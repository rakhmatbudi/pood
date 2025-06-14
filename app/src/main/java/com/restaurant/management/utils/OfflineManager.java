package com.restaurant.management.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.restaurant.management.RestaurantApplication;
import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OfflineManager {
    private static final String TAG = "OfflineManager";
    private static final String PREFS_NAME = "offline_prefs";
    private static final String LAST_SYNC_TIME = "last_sync_time";
    private static final String OFFLINE_MODE_ENABLED = "offline_mode_enabled";
    private static final String LAST_DATA_FETCH_TIME = "last_data_fetch_time";

    private Context context;
    private SharedPreferences prefs;
    private DatabaseManager databaseManager;

    public OfflineManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.databaseManager = DatabaseManager.getInstance(context);
        Log.d(TAG, "OfflineManager initialized with DatabaseManager");
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
        Log.d(TAG, "Forced offline mode " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Update last sync time
     */
    public void updateLastSyncTime() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(LAST_SYNC_TIME, currentTime).apply();
        Log.d(TAG, "Last sync time updated to: " + getLastSyncTimeFormatted());
    }

    /**
     * Update last data fetch time (when data was downloaded from server)
     */
    public void updateLastDataFetchTime() {
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong(LAST_DATA_FETCH_TIME, currentTime).apply();
        Log.d(TAG, "Last data fetch time updated");
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
     * Get last data fetch time as formatted string
     */
    public String getLastDataFetchTimeFormatted() {
        long lastFetch = prefs.getLong(LAST_DATA_FETCH_TIME, 0);
        if (lastFetch == 0) {
            return "Never";
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(lastFetch));
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
     * Get time since last data fetch in minutes
     */
    public long getMinutesSinceLastDataFetch() {
        long lastFetch = prefs.getLong(LAST_DATA_FETCH_TIME, 0);
        if (lastFetch == 0) {
            return Long.MAX_VALUE;
        }

        return (System.currentTimeMillis() - lastFetch) / (60 * 1000);
    }

    /**
     * Check if sync is needed (based on time threshold)
     */
    public boolean isSyncNeeded() {
        return getMinutesSinceLastSync() > 15; // Sync if more than 15 minutes
    }

    /**
     * Check if data refresh is needed (based on time threshold)
     */
    public boolean isDataRefreshNeeded() {
        return getMinutesSinceLastDataFetch() > 60; // Refresh if more than 1 hour
    }

    /**
     * Get comprehensive offline status message for UI
     */
    public void getOfflineStatusMessage(OfflineStatusCallback callback) {
        new Thread(() -> {
            try {
                // Get unsynced order items
                List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();
                int unsyncedItemsCount = unsyncedItems.size();

                // Get unsynced orders
                List<Long> unsyncedOrderIds = databaseManager.getUnsyncedOrderIds();
                int unsyncedOrdersCount = unsyncedOrderIds.size();

                // Calculate total unsynced count
                int totalUnsyncedCount = unsyncedItemsCount + unsyncedOrdersCount;

                String message = buildStatusMessage(unsyncedItemsCount, unsyncedOrdersCount);

                Log.d(TAG, "Offline status - Items: " + unsyncedItemsCount +
                        ", Orders: " + unsyncedOrdersCount + ", Message: " + message);

                callback.onResult(message, totalUnsyncedCount > 0, unsyncedItemsCount, unsyncedOrdersCount);

            } catch (Exception e) {
                Log.e(TAG, "Error getting offline status", e);
                callback.onResult("Status unknown", false, 0, 0);
            }
        }).start();
    }

    private String buildStatusMessage(int unsyncedItemsCount, int unsyncedOrdersCount) {
        int totalUnsynced = unsyncedItemsCount + unsyncedOrdersCount;
        boolean isOnline = NetworkUtils.isNetworkAvailable(context);

        if (totalUnsynced == 0) {
            return isOnline ? "Online - All data synced" : "Offline - No pending items";
        } else {
            if (isOnline) {
                if (unsyncedItemsCount > 0 && unsyncedOrdersCount > 0) {
                    return unsyncedItemsCount + " items & " + unsyncedOrdersCount + " orders syncing...";
                } else if (unsyncedItemsCount > 0) {
                    return unsyncedItemsCount + " items syncing...";
                } else {
                    return unsyncedOrdersCount + " orders syncing...";
                }
            } else {
                if (unsyncedItemsCount > 0 && unsyncedOrdersCount > 0) {
                    return "Offline - " + unsyncedItemsCount + " items & " + unsyncedOrdersCount + " orders pending";
                } else if (unsyncedItemsCount > 0) {
                    return "Offline - " + unsyncedItemsCount + " items pending";
                } else {
                    return "Offline - " + unsyncedOrdersCount + " orders pending";
                }
            }
        }
    }

    /**
     * Get detailed database statistics
     */
    public void getDatabaseStats(DatabaseStatsCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Integer> tableCounts = databaseManager.getAllTableCounts();
                List<OrderItemSyncData> unsyncedItems = databaseManager.getUnsyncedOrderItems();

                DatabaseStats stats = new DatabaseStats();
                stats.menuItemsCount = tableCounts.getOrDefault("menu_items", 0);
                stats.categoriesCount = tableCounts.getOrDefault("menu_categories", 0);
                stats.promosCount = tableCounts.getOrDefault("promos", 0);
                stats.orderTypesCount = tableCounts.getOrDefault("order_types", 0);
                stats.orderStatusesCount = tableCounts.getOrDefault("order_statuses", 0);
                stats.ordersCount = tableCounts.getOrDefault("orders", 0);
                stats.orderItemsCount = tableCounts.getOrDefault("order_items", 0);
                stats.variantsCount = tableCounts.getOrDefault("variants", 0);
                stats.unsyncedItemsCount = unsyncedItems.size();
                stats.lastSyncTime = getLastSyncTimeFormatted();
                stats.lastDataFetchTime = getLastDataFetchTimeFormatted();

                callback.onResult(stats);

            } catch (Exception e) {
                Log.e(TAG, "Error getting database stats", e);
                callback.onResult(new DatabaseStats());
            }
        }).start();
    }

    /**
     * Force sync now if network is available
     */
    public void forceSyncNow() {
        if (NetworkUtils.isNetworkAvailable(context)) {
            Log.d(TAG, "Force sync requested");
            RestaurantApplication app = (RestaurantApplication) context.getApplicationContext();
            app.forceSyncNow();
        } else {
            Log.w(TAG, "Force sync requested but no network available");
        }
    }

    /**
     * Check if app has essential data for offline operation
     */
    public void checkOfflineReadiness(OfflineReadinessCallback callback) {
        new Thread(() -> {
            try {
                boolean hasMenuItems = databaseManager.hasMenuItems();
                boolean hasMenuCategories = databaseManager.hasMenuCategories();
                boolean hasPromos = databaseManager.hasPromos();

                OfflineReadiness readiness = new OfflineReadiness(
                        hasMenuItems,
                        hasMenuCategories,
                        hasPromos,
                        getLastDataFetchTimeFormatted()
                );

                callback.onResult(readiness);

            } catch (Exception e) {
                Log.e(TAG, "Error checking offline readiness", e);
                callback.onResult(new OfflineReadiness(false, false, false, "Unknown"));
            }
        }).start();
    }

    /**
     * Get network status with additional info
     */
    public NetworkStatus getNetworkStatus() {
        boolean isConnected = NetworkUtils.isNetworkAvailable(context);
        boolean isForcedOffline = prefs.getBoolean(OFFLINE_MODE_ENABLED, false);

        return new NetworkStatus(isConnected, isForcedOffline, getLastSyncTimeFormatted(), getLastDataFetchTimeFormatted());
    }

    /**
     * Clear all offline data (use with caution)
     */
    public void clearAllOfflineData() {
        new Thread(() -> {
            try {
                databaseManager.clearAllCachedData();

                // Clear sync timestamps
                prefs.edit()
                        .remove(LAST_SYNC_TIME)
                        .remove(LAST_DATA_FETCH_TIME)
                        .apply();

                Log.d(TAG, "All offline data cleared");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing offline data", e);
            }
        }).start();
    }

    // Callback interfaces
    public interface OfflineStatusCallback {
        void onResult(String message, boolean hasPendingItems, int unsyncedItemsCount, int unsyncedOrdersCount);
    }

    public interface DatabaseStatsCallback {
        void onResult(DatabaseStats stats);
    }

    public interface OfflineReadinessCallback {
        void onResult(OfflineReadiness readiness);
    }

    // Data classes
    public static class NetworkStatus {
        private boolean isConnected;
        private boolean isForcedOffline;
        private String lastSyncTime;
        private String lastDataFetchTime;

        public NetworkStatus(boolean isConnected, boolean isForcedOffline, String lastSyncTime, String lastDataFetchTime) {
            this.isConnected = isConnected;
            this.isForcedOffline = isForcedOffline;
            this.lastSyncTime = lastSyncTime;
            this.lastDataFetchTime = lastDataFetchTime;
        }

        public boolean isConnected() { return isConnected; }
        public boolean isForcedOffline() { return isForcedOffline; }
        public String getLastSyncTime() { return lastSyncTime; }
        public String getLastDataFetchTime() { return lastDataFetchTime; }

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

    public static class DatabaseStats {
        public int menuItemsCount = 0;
        public int categoriesCount = 0;
        public int promosCount = 0;
        public int orderTypesCount = 0;
        public int orderStatusesCount = 0;
        public int ordersCount = 0;
        public int orderItemsCount = 0;
        public int variantsCount = 0;
        public int unsyncedItemsCount = 0;
        public String lastSyncTime = "Never";
        public String lastDataFetchTime = "Never";

        public int getTotalCachedItemsCount() {
            return menuItemsCount + categoriesCount + promosCount + orderTypesCount + orderStatusesCount + variantsCount;
        }

        public int getTotalLocalDataCount() {
            return ordersCount + orderItemsCount;
        }

        public boolean hasEssentialData() {
            return menuItemsCount > 0 && categoriesCount > 0;
        }
    }

    public static class OfflineReadiness {
        private boolean hasMenuItems;
        private boolean hasMenuCategories;
        private boolean hasPromos;
        private String lastDataFetchTime;

        public OfflineReadiness(boolean hasMenuItems, boolean hasMenuCategories, boolean hasPromos, String lastDataFetchTime) {
            this.hasMenuItems = hasMenuItems;
            this.hasMenuCategories = hasMenuCategories;
            this.hasPromos = hasPromos;
            this.lastDataFetchTime = lastDataFetchTime;
        }

        public boolean hasMenuItems() { return hasMenuItems; }
        public boolean hasMenuCategories() { return hasMenuCategories; }
        public boolean hasPromos() { return hasPromos; }
        public String getLastDataFetchTime() { return lastDataFetchTime; }

        public boolean isReadyForOfflineUse() {
            return hasMenuItems && hasMenuCategories;
        }

        public String getReadinessMessage() {
            if (isReadyForOfflineUse()) {
                return "Ready for offline use";
            } else if (!hasMenuItems && !hasMenuCategories) {
                return "No offline data available - please connect to internet first";
            } else if (!hasMenuItems) {
                return "Menu items missing - partial offline capability";
            } else {
                return "Menu categories missing - partial offline capability";
            }
        }
    }
}