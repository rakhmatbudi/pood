package com.restaurant.management.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.services.OfflineSyncService;
import com.restaurant.management.utils.NetworkUtils;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    private static boolean wasOffline = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isOnline = NetworkUtils.isNetworkAvailable(context);

            Log.d(TAG, "Network state changed. Online: " + isOnline + ", Was offline: " + wasOffline);

            if (isOnline && wasOffline) {
                // Network just became available, check if we have unsynced items
                Log.d(TAG, "Network reconnected, checking for unsynced data");
                checkAndStartSync(context);
            } else if (!isOnline) {
                Log.d(TAG, "Network disconnected, app will work in offline mode");
            }

            wasOffline = !isOnline;
        }
    }

    private void checkAndStartSync(Context context) {
        new Thread(() -> {
            try {
                DatabaseManager databaseManager = DatabaseManager.getInstance(context);

                // Get unsynced order items
                List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();
                int unsyncedCount = unsyncedItems.size();

                // Get unsynced orders
                List<Long> unsyncedOrderIds = databaseManager.getUnsyncedOrderIds();
                int unsyncedOrdersCount = unsyncedOrderIds.size();

                Log.d(TAG, "Found " + unsyncedCount + " unsynced order items and " +
                        unsyncedOrdersCount + " unsynced orders");

                if (unsyncedCount > 0 || unsyncedOrdersCount > 0) {
                    Log.d(TAG, "Starting sync service to sync pending data");

                    Intent syncIntent = new Intent(context, OfflineSyncService.class);
                    // Add extra information about what needs to be synced
                    syncIntent.putExtra("unsynced_items_count", unsyncedCount);
                    syncIntent.putExtra("unsynced_orders_count", unsyncedOrdersCount);

                    context.startService(syncIntent);
                } else {
                    Log.d(TAG, "No unsynced data found, sync not needed");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error checking unsynced items", e);

                // Even if there's an error, try to start sync service as a fallback
                // The sync service itself will handle any database errors
                try {
                    Log.d(TAG, "Starting sync service as fallback due to error");
                    Intent syncIntent = new Intent(context, OfflineSyncService.class);
                    context.startService(syncIntent);
                } catch (Exception fallbackError) {
                    Log.e(TAG, "Failed to start sync service as fallback", fallbackError);
                }
            }
        }).start();
    }

    /**
     * Get network status information for debugging
     */
    private void logNetworkInfo(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork != null) {
                    Log.d(TAG, "Active network - Type: " + activeNetwork.getTypeName() +
                            ", State: " + activeNetwork.getState() +
                            ", Connected: " + activeNetwork.isConnected());
                } else {
                    Log.d(TAG, "No active network");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting network info", e);
        }
    }
}