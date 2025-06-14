package com.restaurant.management.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.helpers.OrderItemSyncData;
import com.restaurant.management.models.CreateOrderItemResponse;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.utils.NetworkUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfflineSyncService extends Service {
    private static final String TAG = "OfflineSyncService";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds
    private static final long MAX_RETRY_DELAY_MS = 30000; // 30 seconds max
    private static final String NOTIFICATION_CHANNEL_ID = "sync_channel";
    private static final int NOTIFICATION_ID = 1001;

    // Sync state tracking
    private static volatile boolean isSyncing = false;
    private static final Object syncLock = new Object();

    private DatabaseManager databaseManager;
    private ExecutorService executorService;
    private NotificationManager notificationManager;

    // Progress tracking
    private AtomicInteger totalItems = new AtomicInteger(0);
    private AtomicInteger processedItems = new AtomicInteger(0);
    private AtomicInteger successfulItems = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();
        databaseManager = DatabaseManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        createNotificationChannel();
        Log.d(TAG, "OfflineSyncService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OfflineSyncService started");

        // Prevent multiple sync operations
        synchronized (syncLock) {
            if (isSyncing) {
                Log.d(TAG, "Sync already in progress, ignoring request");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "No network available, stopping sync service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start sync process
        startSyncProcess();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Sync Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Synchronizing offline data");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createSyncNotification(String message, int current, int total) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Syncing Data")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        if (total > 0) {
            builder.setProgress(total, current, false);
        } else {
            builder.setProgress(0, 0, true);
        }

        return builder.build();
    }

    private void updateNotification(String message, int current, int total) {
        // Only show notifications if running as foreground service
        // Comment out for now since we're not using foreground service
        // Notification notification = createSyncNotification(message, current, total);
        // notificationManager.notify(NOTIFICATION_ID, notification);
        Log.d(TAG, message + " (" + current + "/" + total + ")");
    }

    private void startSyncProcess() {
        synchronized (syncLock) {
            isSyncing = true;
        }

        // Reset progress counters
        processedItems.set(0);
        successfulItems.set(0);

        executorService.execute(this::syncUnsyncedData);
    }

    private void syncUnsyncedData() {
        try {
            // First sync order items
            List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();

            if (unsyncedItems.isEmpty()) {
                Log.d(TAG, "No unsynced items found");
                finishSync();
                return;
            }

            totalItems.set(unsyncedItems.size());
            Log.d(TAG, "Found " + unsyncedItems.size() + " unsynced items to sync");

            updateNotification("Syncing items...", 0, totalItems.get());

            // Use CompletableFuture for better async handling
            syncItemsAsync(unsyncedItems)
                    .thenRun(this::finishSync)
                    .exceptionally(throwable -> {
                        Log.e(TAG, "Error during sync process", throwable);
                        finishSync();
                        return null;
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error during sync process", e);
            finishSync();
        }
    }

    private CompletableFuture<Void> syncItemsAsync(List<OrderItemSyncData> items) {
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            final OrderItemSyncData item = items.get(i);

            future = future.thenCompose(ignored -> {
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    Log.d(TAG, "Network lost during sync, stopping");
                    return CompletableFuture.completedFuture(null);
                }

                return syncSingleItemAsync(item, index + 1);
            });
        }

        return future;
    }

    private CompletableFuture<Void> syncSingleItemAsync(OrderItemSyncData item, int itemNumber) {
        return syncSingleItemWithRetryAsync(item, 0, itemNumber);
    }

    private CompletableFuture<Void> syncSingleItemWithRetryAsync(OrderItemSyncData item, int attemptCount, int itemNumber) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached for item " + item.getLocalId());
            processedItems.incrementAndGet();
            updateProgress(itemNumber);
            future.complete(null);
            return future;
        }

        Log.d(TAG, "Syncing item " + item.getLocalId() + " (attempt " + (attemptCount + 1) + ")");

        ApiClient.getApiService().addItemToOrder(item.getOrderId(), item.toCreateOrderItemRequest())
                .enqueue(new Callback<CreateOrderItemResponse>() {
                    @Override
                    public void onResponse(Call<CreateOrderItemResponse> call, Response<CreateOrderItemResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            handleSuccessfulSync(item, response.body());
                            successfulItems.incrementAndGet();
                            processedItems.incrementAndGet();
                            updateProgress(itemNumber);
                            future.complete(null);
                        } else {
                            Log.e(TAG, "Server error syncing item " + item.getLocalId() + ": " + response.code());
                            retryAfterDelay(item, attemptCount, itemNumber, future);
                        }
                    }

                    @Override
                    public void onFailure(Call<CreateOrderItemResponse> call, Throwable t) {
                        Log.e(TAG, "Network error syncing item " + item.getLocalId(), t);
                        retryAfterDelay(item, attemptCount, itemNumber, future);
                    }
                });

        return future;
    }

    private void retryAfterDelay(OrderItemSyncData item, int attemptCount, int itemNumber, CompletableFuture<Void> future) {
        // Exponential backoff with jitter
        long delay = Math.min(RETRY_DELAY_MS * (1L << attemptCount), MAX_RETRY_DELAY_MS);
        long jitter = (long) (Math.random() * 1000); // Add up to 1 second jitter

        executorService.execute(() -> {
            try {
                Thread.sleep(delay + jitter);
                syncSingleItemWithRetryAsync(item, attemptCount + 1, itemNumber)
                        .thenRun(() -> future.complete(null))
                        .exceptionally(throwable -> {
                            future.completeExceptionally(throwable);
                            return null;
                        });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processedItems.incrementAndGet();
                updateProgress(itemNumber);
                future.complete(null);
            }
        });
    }

    private void handleSuccessfulSync(OrderItemSyncData item, CreateOrderItemResponse response) {
        executorService.execute(() -> {
            try {
                long serverId = getServerIdFromResponse(response);
                databaseManager.markOrderItemAsSynced(item.getLocalId(), serverId);
                Log.d(TAG, "Successfully synced item " + item.getLocalId() + " -> " + serverId);
            } catch (Exception e) {
                Log.e(TAG, "Error marking item as synced", e);
            }
        });
    }

    private void updateProgress(int itemNumber) {
        String message = String.format("Syncing item %d of %d (%d successful)",
                itemNumber, totalItems.get(), successfulItems.get());
        updateNotification(message, processedItems.get(), totalItems.get());
    }

    /**
     * Improved server ID extraction with better error handling and logging
     */
    private long getServerIdFromResponse(CreateOrderItemResponse response) {
        if (response == null) {
            Log.w(TAG, "Response is null, using timestamp fallback");
            return System.currentTimeMillis();
        }

        // Define possible method names in order of preference
        String[] methodNames = {"getId", "getItemId", "getOrderItemId", "getServerId"};
        String[] fieldNames = {"id", "itemId", "orderItemId", "serverId"};

        // Try methods first
        for (String methodName : methodNames) {
            try {
                Method method = response.getClass().getMethod(methodName);
                Object result = method.invoke(response);
                if (result != null) {
                    return convertToLong(result);
                }
            } catch (Exception e) {
                Log.v(TAG, "Method " + methodName + " not found or failed: " + e.getMessage());
            }
        }

        // Try nested data object
        try {
            Method dataMethod = response.getClass().getMethod("getData");
            Object data = dataMethod.invoke(response);
            if (data != null) {
                for (String methodName : methodNames) {
                    try {
                        Method method = data.getClass().getMethod(methodName);
                        Object result = method.invoke(data);
                        if (result != null) {
                            return convertToLong(result);
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "Nested method " + methodName + " not found or failed: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.v(TAG, "No getData method found: " + e.getMessage());
        }

        // Try fields
        for (String fieldName : fieldNames) {
            try {
                Field field = response.getClass().getField(fieldName);
                Object result = field.get(response);
                if (result != null) {
                    return convertToLong(result);
                }
            } catch (Exception e) {
                Log.v(TAG, "Field " + fieldName + " not found or failed: " + e.getMessage());
            }
        }

        Log.w(TAG, "No ID found in CreateOrderItemResponse, using timestamp fallback");
        return System.currentTimeMillis();
    }

    private long convertToLong(Object value) throws NumberFormatException {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else {
            throw new NumberFormatException("Cannot convert " + value.getClass().getSimpleName() + " to Long");
        }
    }

    private void finishSync() {
        synchronized (syncLock) {
            isSyncing = false;
        }

        // Final progress log
        String finalMessage = String.format("Sync completed: %d/%d items successful",
                successfulItems.get(), totalItems.get());
        Log.d(TAG, finalMessage);

        // Clean up old synced items
        executorService.execute(() -> {
            try {
                databaseManager.cleanupSyncedOrderItems(7);
                Log.d(TAG, "Cleaned up old synced items");
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
        });

        Log.d(TAG, "Sync process completed - " + finalMessage);

        // Stop service after a short delay
        executorService.execute(() -> {
            try {
                Thread.sleep(1000); // Brief delay before stopping
                stopSelf();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        synchronized (syncLock) {
            isSyncing = false;
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        Log.d(TAG, "OfflineSyncService destroyed");
    }
}