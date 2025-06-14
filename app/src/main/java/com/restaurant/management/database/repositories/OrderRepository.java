package com.restaurant.management.database.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.restaurant.management.database.PoodDatabaseHelper;
import com.restaurant.management.database.DatabaseSchema;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.models.OrderStatus;
import com.restaurant.management.models.CreateOrderItemRequest;
import com.restaurant.management.helpers.OrderItemSyncData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Repository for order-related database operations
 */
public class OrderRepository {
    private static final String TAG = "OrderRepository";
    private final PoodDatabaseHelper dbHelper;

    public OrderRepository(Context context) {
        this.dbHelper = PoodDatabaseHelper.getInstance(context);
    }

    public void saveOrderTypes(List<OrderType> orderTypes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(PoodDatabaseHelper.TABLE_ORDER_TYPES, null, null);
            for (OrderType orderType : orderTypes) {
                ContentValues values = new ContentValues();
                values.put("id", orderType.getId());
                values.put("name", orderType.getName());
                db.insert(PoodDatabaseHelper.TABLE_ORDER_TYPES, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving order types", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<OrderType> getOrderTypes() {
        List<OrderType> orderTypes = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(PoodDatabaseHelper.TABLE_ORDER_TYPES, null, null, null, null, null, "name ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    OrderType orderType = new OrderType();
                    orderType.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    orderType.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    orderTypes.add(orderType);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting order types", e);
        }
        return orderTypes;
    }

    public void saveOrderStatuses(List<OrderStatus> orderStatuses) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(PoodDatabaseHelper.TABLE_ORDER_STATUSES, null, null);
            for (OrderStatus orderStatus : orderStatuses) {
                ContentValues values = new ContentValues();
                values.put("id", orderStatus.getId());
                values.put("name", orderStatus.getName());
                db.insert(PoodDatabaseHelper.TABLE_ORDER_STATUSES, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving order statuses", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<OrderStatus> getOrderStatuses() {
        List<OrderStatus> orderStatuses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(PoodDatabaseHelper.TABLE_ORDER_STATUSES, null, null, null, null, null, "name ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    OrderStatus orderStatus = new OrderStatus();
                    orderStatus.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    orderStatus.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    orderStatuses.add(orderStatus);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting order statuses", e);
        }
        return orderStatuses;
    }

    public long saveOrderLocally(long sessionId, String tableNumber, String customerName, long orderTypeId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long orderId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.COLUMN_SESSION_ID, sessionId);
            values.put(DatabaseSchema.COLUMN_TABLE_NUMBER, tableNumber);
            values.put(DatabaseSchema.COLUMN_CUSTOMER_NAME, customerName);
            values.put(DatabaseSchema.COLUMN_ORDER_TYPE_ID, orderTypeId);
            values.put(DatabaseSchema.COLUMN_IS_SYNCED, 0);
            values.put(DatabaseSchema.COLUMN_ORDER_CREATED_AT, getCurrentTimestamp());
            orderId = db.insert(PoodDatabaseHelper.TABLE_ORDERS, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error saving order locally", e);
            orderId = -1;
        }
        return orderId;
    }

    public void markOrderAsSynced(long localOrderId, long serverOrderId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseSchema.COLUMN_SERVER_ORDER_ID, serverOrderId);
            values.put(DatabaseSchema.COLUMN_IS_SYNCED, 1);
            db.update(PoodDatabaseHelper.TABLE_ORDERS, values,
                    DatabaseSchema.COLUMN_ORDER_ID + " = ?",
                    new String[]{String.valueOf(localOrderId)});
        } catch (Exception e) {
            Log.e(TAG, "Error marking order as synced", e);
        }
    }

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(PoodDatabaseHelper.TABLE_ORDERS, null, null, null, null, null,
                    DatabaseSchema.COLUMN_ORDER_CREATED_AT + " DESC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Order order = new Order();
                    // Set order properties from cursor
                    orders.add(order);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all orders", e);
        }
        return orders;
    }

    public List<Long> getUnsyncedOrderIds() {
        List<Long> unsyncedIds = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(PoodDatabaseHelper.TABLE_ORDERS,
                    new String[]{DatabaseSchema.COLUMN_ORDER_ID},
                    DatabaseSchema.COLUMN_IS_SYNCED + " = ?", new String[]{"0"},
                    null, null, DatabaseSchema.COLUMN_ORDER_CREATED_AT + " ASC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long orderId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ORDER_ID));
                    unsyncedIds.add(orderId);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting unsynced order IDs", e);
        }
        return unsyncedIds;
    }

    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
    }

    public int getAllOrdersCount() {
        return getTableCount(PoodDatabaseHelper.TABLE_ORDERS);
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