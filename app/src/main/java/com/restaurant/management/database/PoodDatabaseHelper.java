package com.restaurant.management.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Base database helper class that handles database creation and versioning
 */
public class PoodDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pood.db";
    private static final int DATABASE_VERSION = 6;

    // Table names
    public static final String TABLE_MENU_ITEMS = "menu_items";
    public static final String TABLE_VARIANTS = "variants";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_PROMOS = "promos";
    public static final String TABLE_ORDER_TYPES = "order_types";
    public static final String TABLE_ORDER_STATUSES = "order_statuses";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_ITEMS = "order_items";

    private static PoodDatabaseHelper instance;

    private PoodDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized PoodDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new PoodDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DatabaseSchema.CREATE_MENU_ITEMS_TABLE);
        db.execSQL(DatabaseSchema.CREATE_VARIANTS_TABLE);
        db.execSQL(DatabaseSchema.CREATE_CATEGORIES_TABLE);
        db.execSQL(DatabaseSchema.CREATE_PROMOS_TABLE);
        db.execSQL(DatabaseSchema.CREATE_ORDER_TYPES_TABLE);
        db.execSQL(DatabaseSchema.CREATE_ORDER_STATUSES_TABLE);
        db.execSQL(DatabaseSchema.CREATE_ORDERS_TABLE);
        db.execSQL(DatabaseSchema.CREATE_ORDER_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            db.execSQL(DatabaseSchema.CREATE_PROMOS_TABLE);
        }
        if (oldVersion < 4) {
            db.execSQL(DatabaseSchema.CREATE_ORDER_TYPES_TABLE);
            db.execSQL(DatabaseSchema.CREATE_ORDER_STATUSES_TABLE);
        }
        if (oldVersion < 5) {
            db.execSQL(DatabaseSchema.CREATE_ORDERS_TABLE);
        }
        if (oldVersion < 6) {
            db.execSQL(DatabaseSchema.CREATE_ORDER_ITEMS_TABLE);
        }
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_MENU_ITEMS, null, null);
            db.delete(TABLE_VARIANTS, null, null);
            db.delete(TABLE_CATEGORIES, null, null);
            db.delete(TABLE_PROMOS, null, null);
            db.delete(TABLE_ORDER_TYPES, null, null);
            db.delete(TABLE_ORDER_STATUSES, null, null);
            db.delete(TABLE_ORDERS, null, null);
            db.delete(TABLE_ORDER_ITEMS, null, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            android.util.Log.e("PoodDatabase", "Error clearing data", e);
        } finally {
            db.endTransaction();
        }
    }
}