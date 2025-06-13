package com.restaurant.management.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.restaurant.management.RestaurantApplication;
import com.restaurant.management.database.PoodDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OfflineManager {
    private static final String TAG = "OfflineManager";
    private static final String PREFS_NAME = "offline_prefs";
    private static final String LAST_SYNC_TIME = "last_sync_time";
    private static final String OFFLINE_MODE_ENABLED = "offline_mode_enabled";

    private Context context;
    private SharedPreferences prefs;

    public OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if device is currently in offline mode
     */
    public boolean isOfflineMode() {
        return !NetworkUtils.isNetworkAvailable(context) || prefs.getBoolean(OFFLINE_MODE_ENABLED, false);
    }

    /**
     * Enable/disable forced offline mode (for testing)
     */
    public void setOfflineMode(boolean enabled) {
        prefs.edit().putBoolean(OFFLINE_MODE_ENABLED, enabled).apply();
    }

    /**
     * Update last sync time
     */
    public void updateLastSyncTime() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(LAST_SYNC_TIME, currentTime).apply();
        Log.d(TAG, "Last sync time updated");
    }

    /**
     * Get last sync time as formatted string
     */
    public String getLastSyncTimeFormatted() {
        long lastSync = prefs.getLong(LAST_SYNC_TIME, 0);
        if (lastSync == 0) {
            return "Never";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastSync));
    }

    /**
     * Get time since last sync in minutes
     */
    public long getMinutesSinceLastSync() {
        long lastSync = prefs.getLong(LAST_SYNC_TIME, 0);
        if (lastSync == 0) {
            return Long.MAX_VALUE;
        }

        return (System.currentTimeMillis() - lastSync) / (60 * 1000);
    }

    /**
     * Check if sync is needed (based on time threshold)
     */
    public boolean isSyncNeeded() {
        return getMinutesSinceLastSync() > 15; // Sync if more than 15 minutes
    }

    /**
     * Get offline status message for UI
     */
    public void getOfflineStatusMessage(OfflineStatusCallback callback) {
        new Thread(() -> {
            try {
                PoodDatabase database = new PoodDatabase(context);
                int unsyncedCount = database.getUnsyncedOrderItems().size();
                database.close();

                String message;
                if (unsyncedCount == 0) {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        message = "Online - All data synced";
                    } else {
                        message = "Offline - No pending items";
                    }
                } else {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        message = unsyncedCount + " items syncing...";
                    } else {
                        message = "Offline - " + unsyncedCount + " items pending";
                    }
                }

                callback.onResult(message, unsyncedCount > 0);

            } catch (Exception e) {
                Log.e(TAG, "Error getting offline status", e);
                callback.onResult("Status unknown", false);
            }
        }).start();
    }

    /**
     * Force sync now if network is available
     */
    public void forceSyncNow() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            RestaurantApplication app = (RestaurantApplication) context.getApplicationContext();
            app.forceSyncNow();
        }
    }

    /**
     * Get network status with additional info
     */
    public NetworkStatus getNetworkStatus() {
        boolean isConnected = NetworkUtils.isNetworkAvailable(context);
        boolean isForcedOffline = prefs.getBoolean(OFFLINE_MODE_ENABLED, false);

        return new NetworkStatus(isConnected, isForcedOffline, getLastSyncTimeFormatted());
    }

    // Callback interface
    public interface OfflineStatusCallback {
        void onResult(String message, boolean hasPendingItems);
    }

    // Network status data class
    public static class NetworkStatus {
        private boolean isConnected;
        private boolean isForcedOffline;
        private String lastSyncTime;

        public NetworkStatus(boolean isConnected, boolean isForcedOffline, String lastSyncTime) {
            this.isConnected = isConnected;
            this.isForcedOffline = isForcedOffline;
            this.lastSyncTime = lastSyncTime;
        }

        public boolean isConnected() { return isConnected; }
        public boolean isForcedOffline() { return isForcedOffline; }
        public String getLastSyncTime() { return lastSyncTime; }

        public boolean isEffectivelyOffline() {
            return !isConnected || isForcedOffline;
        }

        public String getStatusMessage() {
            if (isForcedOffline) {
                return "Forced offline mode";
            } else if (isConnected) {
                return "Online";
            } else {
                return "No internet connection";
            }
        }
    }
}