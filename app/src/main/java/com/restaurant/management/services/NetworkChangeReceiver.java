package com.restaurant.management.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.services.OfflineSyncService;
import com.restaurant.management.utils.NetworkUtils;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    private static final String PREFS_NAME = "network_state_prefs";
    private static final String KEY_WAS_OFFLINE = "was_offline";
    private static final long SYNC_DELAY_MS = 2000; // 2 seconds delay to avoid rapid toggles

    private static final AtomicBoolean syncInProgress = new AtomicBoolean(false);
    private static Handler delayHandler = new Handler(Looper.getMainLooper());
    private static Runnable pendingSyncRunnable;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        boolean isOnline = NetworkUtils.isNetworkAvailable(context);
        boolean wasOffline = getWasOfflineState(context);

        Log.d(TAG, "Network state changed. Online: " + isOnline + ", Was offline: " + wasOffline);
        logNetworkInfo(context);

        if (isOnline && wasOffline) {
            Log.d(TAG, "Network reconnected, scheduling sync check");
            scheduleDelayedSync(context);
        } else if (!isOnline) {
            Log.d(TAG, "Network disconnected, app will work in offline mode");
            cancelPendingSync();
        }

        // Update stored state
        setWasOfflineState(context, !isOnline);
    }

    /**
     * Schedule sync with delay to avoid rapid network state changes
     */
    private void scheduleDelayedSync(Context context) {
        // Cancel any pending sync
        cancelPendingSync();

        pendingSyncRunnable = () -> {
            if (NetworkUtils.isNetworkAvailable(context)) {
                checkAndStartSync(context);
            } else {
                Log.d(TAG, "Network no longer available, canceling sync");
            }
        };

        delayHandler.postDelayed(pendingSyncRunnable, SYNC_DELAY_MS);
    }

    private void cancelPendingSync() {
        if (pendingSyncRunnable != null) {
            delayHandler.removeCallbacks(pendingSyncRunnable);
            pendingSyncRunnable = null;
            Log.d(TAG, "Cancelled pending sync");
        }
    }

    /**
     * Get the previous offline state from SharedPreferences for persistence across app restarts
     */
    private boolean getWasOfflineState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_WAS_OFFLINE, false);
    }

    /**
     * Store the offline state in SharedPreferences
     */
    private void setWasOfflineState(Context context, boolean wasOffline) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_WAS_OFFLINE, wasOffline).apply();
    }

    private void checkAndStartSync(Context context) {
        // Prevent multiple concurrent sync checks
        if (!syncInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "Sync check already in progress, skipping");
            return;
        }

        // Run database check in background thread
        new Thread(() -> {
            try {
                performSyncCheck(context);
            } finally {
                syncInProgress.set(false);
            }
        }).start();
    }

    private void performSyncCheck(Context context) {
        DatabaseManager databaseManager = null;
        try {
            databaseManager = DatabaseManager.getInstance(context);

            // Check for unsynced order items
            List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();
            int unsyncedItemsCount = unsyncedItems != null ? unsyncedItems.size() : 0;

            // Check for unsynced orders
            List<Long> unsyncedOrderIds = databaseManager.getUnsyncedOrderIds();
            int unsyncedOrdersCount = unsyncedOrderIds != null ? unsyncedOrderIds.size() : 0;

            Log.d(TAG, String.format("Sync check results: %d unsynced items, %d unsynced orders",
                    unsyncedItemsCount, unsyncedOrdersCount));

            if (unsyncedItemsCount > 0 || unsyncedOrdersCount > 0) {
                startSyncService(context, unsyncedItemsCount, unsyncedOrdersCount);
            } else {
                Log.d(TAG, "No unsynced data found, sync not needed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during sync check", e);

            // Fallback: start sync service anyway in case of database issues
            // The sync service has its own error handling
            Log.w(TAG, "Starting sync service as fallback due to check error");
            startSyncService(context, -1, -1); // -1 indicates unknown count

        } finally {
            // No explicit cleanup needed for DatabaseManager singleton
        }
    }

    private void startSyncService(Context context, int unsyncedItemsCount, int unsyncedOrdersCount) {
        try {
            Log.d(TAG, "Starting sync service");

            Intent syncIntent = new Intent(context, OfflineSyncService.class);

            // Add metadata for the sync service
            if (unsyncedItemsCount >= 0) {
                syncIntent.putExtra("unsynced_items_count", unsyncedItemsCount);
            }
            if (unsyncedOrdersCount >= 0) {
                syncIntent.putExtra("unsynced_orders_count", unsyncedOrdersCount);
            }
            syncIntent.putExtra("trigger_source", "network_reconnection");
            syncIntent.putExtra("timestamp", System.currentTimeMillis());

            // Start the service
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(syncIntent);
            } else {
                context.startService(syncIntent);
            }

            Log.d(TAG, "Sync service started successfully");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start sync service", e);
        }
    }

    /**
     * Enhanced network information logging for debugging
     */
    private void logNetworkInfo(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Log.d(TAG, "ConnectivityManager is null");
                return;
            }

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null) {
                Log.d(TAG, String.format("Active network - Type: %s, SubType: %s, State: %s, " +
                                "Connected: %s, Available: %s, Roaming: %s",
                        activeNetwork.getTypeName(),
                        activeNetwork.getSubtypeName(),
                        activeNetwork.getState(),
                        activeNetwork.isConnected(),
                        activeNetwork.isAvailable(),
                        activeNetwork.isRoaming()));
            } else {
                Log.d(TAG, "No active network");
            }

            // Log all network info for debugging
            NetworkInfo[] allNetworks = cm.getAllNetworkInfo();
            if (allNetworks != null) {
                Log.d(TAG, "All networks count: " + allNetworks.length);
                for (int i = 0; i < allNetworks.length; i++) {
                    NetworkInfo info = allNetworks[i];
                    if (info != null) {
                        Log.d(TAG, String.format("Network %d: %s - %s (%s)",
                                i, info.getTypeName(), info.getState(), info.getDetailedState()));
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error logging network info", e);
        }
    }

    /**
     * Static method to manually trigger sync check (useful for testing or manual triggers)
     */
    public static void triggerSyncCheck(Context context) {
        Log.d(TAG, "Manual sync check triggered");
        NetworkChangeReceiver receiver = new NetworkChangeReceiver();
        receiver.checkAndStartSync(context);
    }
}