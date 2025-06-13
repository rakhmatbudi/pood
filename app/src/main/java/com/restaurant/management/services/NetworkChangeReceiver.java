package com.restaurant.management.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.services.OfflineSyncService;
import com.restaurant.management.utils.NetworkUtils;

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
                checkAndStartSync(context);
            }

            wasOffline = !isOnline;
        }
    }

    private void checkAndStartSync(Context context) {
        new Thread(() -> {
            try {
                PoodDatabase database = new PoodDatabase(context);
                int unsyncedCount = database.getUnsyncedOrderItems().size();
                database.close();

                if (unsyncedCount > 0) {
                    Log.d(TAG, "Found " + unsyncedCount + " unsynced items, starting sync service");
                    Intent syncIntent = new Intent(context, OfflineSyncService.class);
                    context.startService(syncIntent);
                } else {
                    Log.d(TAG, "No unsynced items found");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error checking unsynced items", e);
            }
        }).start();
    }
}