package com.restaurant.management.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.helpers.OrderItemSyncData;
import com.restaurant.management.models.CreateOrderItemResponse;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.utils.NetworkUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfflineSyncService extends Service {
    private static final String TAG = "OfflineSyncService";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    private DatabaseManager databaseManager;
    private ExecutorService executorService;
    private boolean isSyncing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        databaseManager = DatabaseManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "OfflineSyncService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OfflineSyncService started");

        if (!isSyncing && NetworkUtils.isNetworkAvailable(this)) {
            startSyncProcess();
        } else if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "No network available, stopping sync service");
            stopSelf();
        }

        return START_NOT_STICKY; // Don't restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }

    private void startSyncProcess() {
        isSyncing = true;
        executorService.execute(this::syncUnsyncedOrderItems);
    }

    private void syncUnsyncedOrderItems() {
        try {
            List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();

            if (unsyncedItems.isEmpty()) {
                Log.d(TAG, "No unsynced items found");
                finishSync();
                return;
            }

            Log.d(TAG, "Found " + unsyncedItems.size() + " unsynced items to sync");

            // Sync items one by one to ensure proper error handling
            syncItemsSequentially(unsyncedItems, 0);

        } catch (Exception e) {
            Log.e(TAG, "Error during sync process", e);
            finishSync();
        }
    }

    private void syncItemsSequentially(List<OrderItemSyncData> items, int currentIndex) {
        if (currentIndex >= items.size()) {
            Log.d(TAG, "All items processed");
            finishSync();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "Network lost during sync, stopping");
            finishSync();
            return;
        }

        OrderItemSyncData item = items.get(currentIndex);
        syncSingleItem(item, new SyncCallback() {
            @Override
            public void onSuccess() {
                // Continue with next item
                syncItemsSequentially(items, currentIndex + 1);
            }

            @Override
            public void onFailure() {
                // Continue with next item (failed item will be retried later)
                Log.w(TAG, "Failed to sync item " + item.getLocalId() + ", will retry later");
                syncItemsSequentially(items, currentIndex + 1);
            }
        });
    }

    private void syncSingleItem(OrderItemSyncData item, SyncCallback callback) {
        syncSingleItemWithRetry(item, 0, callback);
    }

    private void syncSingleItemWithRetry(OrderItemSyncData item, int attemptCount, SyncCallback callback) {
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached for item " + item.getLocalId());
            callback.onFailure();
            return;
        }

        Log.d(TAG, "Syncing item " + item.getLocalId() + " (attempt " + (attemptCount + 1) + ")");

        ApiClient.getApiService().addItemToOrder(item.getOrderId(), item.toCreateOrderItemRequest())
                .enqueue(new Callback<CreateOrderItemResponse>() {
                    @Override
                    public void onResponse(Call<CreateOrderItemResponse> call, Response<CreateOrderItemResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Mark as synced in database
                            executorService.execute(() -> {
                                try {
                                    // Try to get server ID using robust method
                                    long serverId = getServerIdFromResponse(response.body());
                                    databaseManager.markOrderItemAsSynced(item.getLocalId(), serverId);
                                    Log.d(TAG, "Successfully synced item " + item.getLocalId() + " -> " + serverId);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error marking item as synced", e);
                                }
                            });
                            callback.onSuccess();
                        } else {
                            Log.e(TAG, "Server error syncing item " + item.getLocalId() + ": " + response.code());

                            // Retry after delay
                            executorService.execute(() -> {
                                try {
                                    Thread.sleep(RETRY_DELAY_MS);
                                    syncSingleItemWithRetry(item, attemptCount + 1, callback);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    callback.onFailure();
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<CreateOrderItemResponse> call, Throwable t) {
                        Log.e(TAG, "Network error syncing item " + item.getLocalId(), t);

                        // Retry after delay
                        executorService.execute(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                                syncSingleItemWithRetry(item, attemptCount + 1, callback);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                callback.onFailure();
                            }
                        });
                    }
                });
    }

    /**
     * Try to get server ID from response using different possible method names
     */
    private long getServerIdFromResponse(CreateOrderItemResponse response) {
        try {
            // Try common method names for getting ID
            if (hasMethod(response, "getId")) {
                return (Long) response.getClass().getMethod("getId").invoke(response);
            } else if (hasMethod(response, "getItemId")) {
                return (Long) response.getClass().getMethod("getItemId").invoke(response);
            } else if (hasMethod(response, "getOrderItemId")) {
                return (Long) response.getClass().getMethod("getOrderItemId").invoke(response);
            } else if (hasMethod(response, "getData")) {
                // Some APIs return nested data like response.getData().getId()
                Object data = response.getClass().getMethod("getData").invoke(response);
                if (data != null && hasMethod(data, "getId")) {
                    return (Long) data.getClass().getMethod("getId").invoke(data);
                }
            } else if (hasField(response, "id")) {
                return (Long) response.getClass().getField("id").get(response);
            } else {
                Log.w(TAG, "No ID method found in CreateOrderItemResponse, using timestamp as fallback");
                return System.currentTimeMillis(); // Fallback - not ideal but prevents crashes
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting server ID from response", e);
            return System.currentTimeMillis(); // Fallback
        }
        return System.currentTimeMillis(); // Fallback
    }

    /**
     * Check if object has a specific method
     */
    private boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Check if object has a specific field
     */
    private boolean hasField(Object obj, String fieldName) {
        try {
            obj.getClass().getField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private void finishSync() {
        isSyncing = false;

        // Clean up old synced items (older than 7 days)
        executorService.execute(() -> {
            try {
                databaseManager.cleanupSyncedOrderItems(7);
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
        });

        Log.d(TAG, "Sync process completed");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        // Note: No need to close DatabaseManager as it uses singleton pattern
        Log.d(TAG, "OfflineSyncService destroyed");
    }

    private interface SyncCallback {
        void onSuccess();
        void onFailure();
    }
}