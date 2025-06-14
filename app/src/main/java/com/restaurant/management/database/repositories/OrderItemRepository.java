package com.restaurant.management.database.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.restaurant.management.database.PoodDatabaseHelper;
import com.restaurant.management.database.DatabaseSchema;
import com.restaurant.management.models.CreateOrderItemRequest;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Repository for order item-related database operations
 */
public class OrderItemRepository {
    private static final String TAG = "OrderItemRepository";
    private final PoodDatabaseHelper dbHelper;

    public OrderItemRepository(Context context) {
        this.dbHelper = PoodDatabaseHelper.getInstance(context);
    }

    public long saveOrderItemLocally(long orderId, CreateOrderItemRequest request) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long itemId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.COLUMN_ORDER_ITEM_ORDER_ID, orderId);
            values.put(DatabaseSchema.COLUMN_MENU_ITEM_ID_FK, request.getMenuItemId());
            values.put(DatabaseSchema.COLUMN_VARIANT_ID_FK, request.getVariantId());
            values.put(DatabaseSchema.COLUMN_ITEM_QUANTITY, request.getQuantity());
            values.put(DatabaseSchema.COLUMN_ITEM_UNIT_PRICE, request.getUnitPrice());
            values.put(DatabaseSchema.COLUMN_ITEM_TOTAL_PRICE, request.getTotalPrice());
            values.put(DatabaseSchema.COLUMN_ITEM_NOTES, request.getNotes());
            values.put(DatabaseSchema.COLUMN_ITEM_STATUS, request.getStatus());
            values.put(DatabaseSchema.COLUMN_ITEM_IS_COMPLIMENTARY, request.isComplimentary() ? 1 : 0);
            values.put(DatabaseSchema.COLUMN_ITEM_IS_CUSTOM_PRICE, request.isCustomPrice() ? 1 : 0);
            values.put(DatabaseSchema.COLUMN_ITEM_ORIGINAL_PRICE, request.getOriginalPrice());
            values.put(DatabaseSchema.COLUMN_ITEM_KITCHEN_PRINTED, request.isKitchenPrinted() ? 1 : 0);
            values.put(DatabaseSchema.COLUMN_ITEM_IS_SYNCED, 0);
            values.put(DatabaseSchema.COLUMN_ITEM_CREATED_AT, getCurrentTimestamp());
            itemId = db.insert(PoodDatabaseHelper.TABLE_ORDER_ITEMS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error saving order item locally", e);
            itemId = -1;
        }
        return itemId;
    }

    public void markOrderItemAsSynced(long localItemId, long serverItemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.COLUMN_ITEM_SERVER_ID, serverItemId);
            values.put(DatabaseSchema.COLUMN_ITEM_IS_SYNCED, 1);
            db.update(PoodDatabaseHelper.TABLE_ORDER_ITEMS, values,
                    DatabaseSchema.COLUMN_ITEM_ID + " = ?",
                    new String[]{String.valueOf(localItemId)});
        } catch (Exception e) {
            Log.e(TAG, "Error marking order item as synced", e);
        }
    }

    public List<OrderItemSyncData> getUnsyncedOrderItems() {
        List<OrderItemSyncData> unsyncedItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String selectQuery = "SELECT * FROM " + PoodDatabaseHelper.TABLE_ORDER_ITEMS +
                    " WHERE " + DatabaseSchema.COLUMN_ITEM_IS_SYNCED + " = 0" +
                    " ORDER BY " + DatabaseSchema.COLUMN_ITEM_CREATED_AT + " ASC";
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    OrderItemSyncData item = createOrderItemFromCursor(cursor);
                    unsyncedItems.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting unsynced order items", e);
        }
        return unsyncedItems;
    }

    public List<OrderItemSyncData> getAllOrderItems() {
        List<OrderItemSyncData> orderItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String selectQuery = "SELECT * FROM " + PoodDatabaseHelper.TABLE_ORDER_ITEMS +
                    " ORDER BY " + DatabaseSchema.COLUMN_ITEM_CREATED_AT + " DESC";
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    OrderItemSyncData item = createBasicOrderItemFromCursor(cursor);
                    orderItems.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all order items", e);
        }
        return orderItems;
    }

    public List<OrderItemSyncData> getOrderItems(long orderId) {
        List<OrderItemSyncData> orderItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            String selectQuery = "SELECT oi.*, mi.name as menu_item_name, v.name as variant_name " +
                    "FROM " + PoodDatabaseHelper.TABLE_ORDER_ITEMS + " oi " +
                    "LEFT JOIN " + PoodDatabaseHelper.TABLE_MENU_ITEMS + " mi ON oi." +
                    DatabaseSchema.COLUMN_MENU_ITEM_ID_FK + " = mi." + DatabaseSchema.COLUMN_ID + " " +
                    "LEFT JOIN " + PoodDatabaseHelper.TABLE_VARIANTS + " v ON oi." +
                    DatabaseSchema.COLUMN_VARIANT_ID_FK + " = v." + DatabaseSchema.COLUMN_VARIANT_ID + " " +
                    "WHERE oi." + DatabaseSchema.COLUMN_ORDER_ITEM_ORDER_ID + " = ? " +
                    "ORDER BY oi." + DatabaseSchema.COLUMN_ITEM_CREATED_AT + " ASC";
            Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(orderId)});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    OrderItemSyncData item = createDetailedOrderItemFromCursor(cursor);
                    orderItems.add(item);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting order items", e);
        }
        return orderItems;
    }

    private OrderItemSyncData createOrderItemFromCursor(Cursor cursor) {
        OrderItemSyncData item = new OrderItemSyncData();
        item.setLocalId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_ID)));
        item.setOrderId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ORDER_ITEM_ORDER_ID)));
        item.setMenuItemId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MENU_ITEM_ID_FK)));
        item.setVariantId(cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_ID_FK)) ?
                null : cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_ID_FK)));
        item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_QUANTITY)));
        item.setUnitPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_UNIT_PRICE)));
        item.setTotalPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_TOTAL_PRICE)));
        item.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_NOTES)));
        item.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_STATUS)));
        item.setComplimentary(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_IS_COMPLIMENTARY)) == 1);
        item.setCustomPrice(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_IS_CUSTOM_PRICE)) == 1);
        item.setOriginalPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_ORIGINAL_PRICE)));
        item.setKitchenPrinted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_KITCHEN_PRINTED)) == 1);
        item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_CREATED_AT)));
        return item;
    }

    private OrderItemSyncData createBasicOrderItemFromCursor(Cursor cursor) {
        OrderItemSyncData item = new OrderItemSyncData();
        item.setLocalId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_ID)));
        item.setOrderId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ORDER_ITEM_ORDER_ID)));
        item.setMenuItemId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_MENU_ITEM_ID_FK)));
        item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_QUANTITY)));
        item.setTotalPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_TOTAL_PRICE)));
        item.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_IS_SYNCED)) == 1);
        item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_CREATED_AT)));
        return item;
    }

    private OrderItemSyncData createDetailedOrderItemFromCursor(Cursor cursor) {
        OrderItemSyncData item = createOrderItemFromCursor(cursor);
        item.setSynced(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_IS_SYNCED)) == 1);
        item.setServerId(cursor.isNull(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_SERVER_ID)) ?
                null : cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ITEM_SERVER_ID)));
        item.setMenuItemName(cursor.getString(cursor.getColumnIndexOrThrow("menu_item_name")));
        item.setVariantName(cursor.getString(cursor.getColumnIndexOrThrow("variant_name")));
        return item;
    }

    public void cleanupSyncedOrderItems(int daysOld) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -daysOld);
            String dateThreshold = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(calendar.getTime());
            db.delete(PoodDatabaseHelper.TABLE_ORDER_ITEMS,
                    DatabaseSchema.COLUMN_ITEM_IS_SYNCED + " = 1 AND " +
                            DatabaseSchema.COLUMN_ITEM_CREATED_AT + " < ?",
                    new String[]{dateThreshold});
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up synced order items", e);
        }
    }

    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    public int getAllOrderItemsCount() {
        return getTableCount(PoodDatabaseHelper.TABLE_ORDER_ITEMS);
    }

    private int getTableCount(String tableName) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int count = 0;
        try {
            String countQuery = "SELECT COUNT(*) FROM " + tableName;
            Cursor cursor = db.rawQuery(countQuery, null);
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