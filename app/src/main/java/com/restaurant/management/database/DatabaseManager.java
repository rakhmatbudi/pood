package com.restaurant.management.database;

import android.content.Context;
import android.util.Log;

import com.restaurant.management.database.repositories.MenuRepository;
import com.restaurant.management.database.repositories.PromoRepository;
import com.restaurant.management.database.repositories.OrderRepository;
import com.restaurant.management.database.repositories.OrderItemRepository;
import com.restaurant.management.models.*;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main database manager class that provides a unified interface to all repositories
 * Uses Facade pattern to simplify database operations
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";
    private static DatabaseManager instance;

    private final PoodDatabaseHelper dbHelper;
    private final MenuRepository menuRepository;
    private final PromoRepository promoRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private DatabaseManager(Context context) {
        this.dbHelper = PoodDatabaseHelper.getInstance(context);
        this.menuRepository = new MenuRepository(context);
        this.promoRepository = new PromoRepository(context);
        this.orderRepository = new OrderRepository(context);
        this.orderItemRepository = new OrderItemRepository(context);
    }

    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    // Menu Operations
    public void saveMenuItems(List<ProductItem> menuItems) {
        menuRepository.saveMenuItems(menuItems);
    }

    public List<ProductItem> getAllMenuItems() {
        return menuRepository.getAllMenuItems();
    }

    public List<ProductItem> getMenuItems() {
        return menuRepository.getAllMenuItems();
    }

    public void saveMenuCategories(List<MenuCategory> categories) {
        menuRepository.saveMenuCategories(categories);
    }

    public List<MenuCategory> getAllMenuCategories() {
        return menuRepository.getAllMenuCategories();
    }

    public List<MenuCategory> getMenuCategories() {
        return menuRepository.getAllMenuCategories();
    }

    public List<Variant> getAllVariants() {
        return menuRepository.getAllVariants();
    }

    public boolean hasMenuItems() {
        return menuRepository.hasMenuItems();
    }

    public boolean hasMenuCategories() {
        return menuRepository.hasMenuCategories();
    }

    // Promo Operations
    public void savePromos(List<Promo> promos) {
        promoRepository.savePromos(promos);
    }

    public List<Promo> getAllActivePromos() {
        return promoRepository.getAllActivePromos();
    }

    public List<Promo> getPromos() {
        return promoRepository.getAllPromos();
    }

    public boolean hasPromos() {
        return promoRepository.hasPromos();
    }

    // Order Operations
    public void saveOrderTypes(List<OrderType> orderTypes) {
        orderRepository.saveOrderTypes(orderTypes);
    }

    public List<OrderType> getOrderTypes() {
        return orderRepository.getOrderTypes();
    }

    public void saveOrderStatuses(List<OrderStatus> orderStatuses) {
        orderRepository.saveOrderStatuses(orderStatuses);
    }

    public List<OrderStatus> getOrderStatuses() {
        return orderRepository.getOrderStatuses();
    }

    public long saveOrderLocally(long sessionId, String tableNumber, String customerName, long orderTypeId) {
        return orderRepository.saveOrderLocally(sessionId, tableNumber, customerName, orderTypeId);
    }

    public void markOrderAsSynced(long localOrderId, long serverOrderId) {
        orderRepository.markOrderAsSynced(localOrderId, serverOrderId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.getAllOrders();
    }

    public List<Long> getUnsyncedOrderIds() {
        return orderRepository.getUnsyncedOrderIds();
    }

    public int getAllOrdersCount() {
        return orderRepository.getAllOrdersCount();
    }

    // Order Item Operations
    public long saveOrderItemLocally(long orderId, CreateOrderItemRequest request) {
        return orderItemRepository.saveOrderItemLocally(orderId, request);
    }

    public void markOrderItemAsSynced(long localItemId, long serverItemId) {
        orderItemRepository.markOrderItemAsSynced(localItemId, serverItemId);
    }

    public List<OrderItemSyncData> getUnsyncedOrderItems() {
        return orderItemRepository.getUnsyncedOrderItems();
    }

    public List<OrderItemSyncData> getAllOrderItems() {
        return orderItemRepository.getAllOrderItems();
    }

    public List<OrderItemSyncData> getOrderItems(long orderId) {
        return orderItemRepository.getOrderItems(orderId);
    }

    public void cleanupSyncedOrderItems(int daysOld) {
        orderItemRepository.cleanupSyncedOrderItems(daysOld);
    }

    public int getAllOrderItemsCount() {
        return orderItemRepository.getAllOrderItemsCount();
    }

    public int getAllVariantsCount() {
        return menuRepository.getAllVariants().size();
    }

    // Database Utility Operations
    public void clearAllData() {
        dbHelper.clearAllData();
    }

    public void clearAllCachedData() {
        try {
            dbHelper.getWritableDatabase().beginTransaction();
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_CATEGORIES, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_MENU_ITEMS, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_VARIANTS, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_PROMOS, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_ORDER_TYPES, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_ORDER_STATUSES, null, null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_ORDERS,
                    DatabaseSchema.COLUMN_IS_SYNCED + " = 0", null);
            dbHelper.getWritableDatabase().delete(PoodDatabaseHelper.TABLE_ORDER_ITEMS,
                    DatabaseSchema.COLUMN_ITEM_IS_SYNCED + " = 0", null);
            dbHelper.getWritableDatabase().setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cached data", e);
        } finally {
            dbHelper.getWritableDatabase().endTransaction();
        }
    }

    public Map<String, Integer> getAllTableCounts() {
        Map<String, Integer> counts = new HashMap<>();
        try {
            counts.put("menu_categories", getTableCount(PoodDatabaseHelper.TABLE_CATEGORIES));
            counts.put("menu_items", getTableCount(PoodDatabaseHelper.TABLE_MENU_ITEMS));
            counts.put("variants", getTableCount(PoodDatabaseHelper.TABLE_VARIANTS));
            counts.put("promos", getTableCount(PoodDatabaseHelper.TABLE_PROMOS));
            counts.put("order_types", getTableCount(PoodDatabaseHelper.TABLE_ORDER_TYPES));
            counts.put("order_statuses", getTableCount(PoodDatabaseHelper.TABLE_ORDER_STATUSES));
            counts.put("orders", getTableCount(PoodDatabaseHelper.TABLE_ORDERS));
            counts.put("order_items", getTableCount(PoodDatabaseHelper.TABLE_ORDER_ITEMS));
        } catch (Exception e) {
            Log.e(TAG, "Error getting table counts", e);
        }
        return counts;
    }

    public int getCashierSessionsCount() {
        int count = 0;
        try {
            count = getTableCount("cashier_sessions");
        } catch (Exception e) {
            // Table doesn't exist
        }
        return count;
    }

    private int getTableCount(String tableName) {
        int count = 0;
        try {
            android.database.Cursor cursor = dbHelper.getReadableDatabase()
                    .rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e(TAG, "Error counting table " + tableName, e);
        }
        return count;
    }
}