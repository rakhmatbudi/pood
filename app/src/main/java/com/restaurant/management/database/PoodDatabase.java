package com.restaurant.management.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.models.Promo;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.models.OrderStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class PoodDatabase extends SQLiteOpenHelper {
    private static final String TAG = "PoodDatabase";
    private static final String DATABASE_NAME = "pood.db";
    private static final int DATABASE_VERSION = 5; // INCREASED VERSION FOR PROMOS TABLE

    // Table names
    private static final String TABLE_MENU_ITEMS = "menu_items";
    private static final String TABLE_VARIANTS = "variants";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PROMOS = "promos";
    private static final String TABLE_ORDER_TYPES = "order_types";
    private static final String TABLE_ORDER_STATUSES = "order_statuses";
    private static final String TABLE_ORDERS = "orders";


    // Menu Items table columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_PRICE = "price";
    private static final String COLUMN_IS_ACTIVE = "is_active";
    private static final String COLUMN_IMAGE_PATH = "image_path";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_CATEGORY_ID = "category_id";
    private static final String COLUMN_CATEGORY_NAME = "category_name";
    private static final String COLUMN_CATEGORY_DESCRIPTION = "category_description";

    // Variants table columns
    private static final String COLUMN_VARIANT_ID = "variant_id";
    private static final String COLUMN_MENU_ITEM_ID = "menu_item_id";
    private static final String COLUMN_VARIANT_NAME = "variant_name";
    private static final String COLUMN_VARIANT_PRICE = "variant_price";
    private static final String COLUMN_VARIANT_IS_ACTIVE = "variant_is_active";
    private static final String COLUMN_VARIANT_CREATED_AT = "variant_created_at";
    private static final String COLUMN_VARIANT_UPDATED_AT = "variant_updated_at";

    // Categories table columns
    private static final String COLUMN_CAT_ID = "cat_id";
    private static final String COLUMN_CAT_NAME = "cat_name";
    private static final String COLUMN_CAT_DESCRIPTION = "cat_description";
    private static final String COLUMN_CAT_CREATED_AT = "cat_created_at";
    private static final String COLUMN_CAT_UPDATED_AT = "cat_updated_at";
    private static final String COLUMN_CAT_IS_DISPLAYED = "cat_is_displayed";
    private static final String COLUMN_CAT_DISPLAY_PICTURE = "cat_display_picture";
    private static final String COLUMN_CAT_GROUP = "cat_group";
    private static final String COLUMN_CAT_SKU_ID = "cat_sku_id";
    private static final String COLUMN_CAT_IS_HIGHLIGHT = "cat_is_highlight";
    private static final String COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER = "cat_is_display_for_self_order";

    // Promo table columns
    private static final String COLUMN_PROMO_ID = "promo_id";
    private static final String COLUMN_PROMO_NAME = "promo_name";
    private static final String COLUMN_PROMO_DESCRIPTION = "promo_description";
    private static final String COLUMN_PROMO_START_DATE = "start_date";
    private static final String COLUMN_PROMO_END_DATE = "end_date";
    private static final String COLUMN_PROMO_TERM_CONDITION = "term_and_condition";
    private static final String COLUMN_PROMO_PICTURE = "picture";
    private static final String COLUMN_PROMO_TYPE = "type";
    private static final String COLUMN_PROMO_DISCOUNT_TYPE = "discount_type";
    private static final String COLUMN_PROMO_DISCOUNT_AMOUNT = "discount_amount";
    private static final String COLUMN_PROMO_IS_ACTIVE = "is_active";

    // Order table columns
    private static final String COLUMN_ORDER_ID = "order_id";
    private static final String COLUMN_SESSION_ID = "session_id";
    private static final String COLUMN_TABLE_NUMBER = "table_number";
    private static final String COLUMN_CUSTOMER_NAME = "customer_name";
    private static final String COLUMN_ORDER_TYPE_ID = "order_type_id";
    private static final String COLUMN_SERVER_ORDER_ID = "server_order_id";
    private static final String COLUMN_IS_SYNCED = "is_synced";
    private static final String COLUMN_ORDER_CREATED_AT = "order_created_at";

    private static final String CREATE_ORDERS_TABLE = "CREATE TABLE " + TABLE_ORDERS + "("
            + COLUMN_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_ID + " INTEGER NOT NULL,"
            + COLUMN_TABLE_NUMBER + " TEXT NOT NULL,"
            + COLUMN_CUSTOMER_NAME + " TEXT,"
            + COLUMN_ORDER_TYPE_ID + " INTEGER NOT NULL,"
            + COLUMN_SERVER_ORDER_ID + " INTEGER,"
            + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0,"
            + COLUMN_ORDER_CREATED_AT + " TEXT NOT NULL"
            + ")";

    // Create table statements
    private static final String CREATE_MENU_ITEMS_TABLE = "CREATE TABLE " + TABLE_MENU_ITEMS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_NAME + " TEXT NOT NULL,"
            + COLUMN_DESCRIPTION + " TEXT,"
            + COLUMN_PRICE + " REAL,"
            + COLUMN_IS_ACTIVE + " INTEGER,"
            + COLUMN_IMAGE_PATH + " TEXT,"
            + COLUMN_CREATED_AT + " TEXT,"
            + COLUMN_UPDATED_AT + " TEXT,"
            + COLUMN_CATEGORY_ID + " INTEGER,"
            + COLUMN_CATEGORY_NAME + " TEXT,"
            + COLUMN_CATEGORY_DESCRIPTION + " TEXT"
            + ")";

    private static final String CREATE_VARIANTS_TABLE = "CREATE TABLE " + TABLE_VARIANTS + "("
            + COLUMN_VARIANT_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_MENU_ITEM_ID + " INTEGER,"
            + COLUMN_VARIANT_NAME + " TEXT,"
            + COLUMN_VARIANT_PRICE + " REAL,"
            + COLUMN_VARIANT_IS_ACTIVE + " INTEGER,"
            + COLUMN_VARIANT_CREATED_AT + " TEXT,"
            + COLUMN_VARIANT_UPDATED_AT + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_MENU_ITEM_ID + ") REFERENCES " + TABLE_MENU_ITEMS + "(" + COLUMN_ID + ")"
            + ")";

    private static final String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + TABLE_CATEGORIES + "("
            + COLUMN_CAT_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_CAT_NAME + " TEXT NOT NULL,"
            + COLUMN_CAT_DESCRIPTION + " TEXT,"
            + COLUMN_CAT_CREATED_AT + " TEXT,"
            + COLUMN_CAT_UPDATED_AT + " TEXT,"
            + COLUMN_CAT_IS_DISPLAYED + " INTEGER,"
            + COLUMN_CAT_DISPLAY_PICTURE + " TEXT,"
            + COLUMN_CAT_GROUP + " TEXT,"
            + COLUMN_CAT_SKU_ID + " TEXT,"
            + COLUMN_CAT_IS_HIGHLIGHT + " INTEGER,"
            + COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER + " INTEGER"
            + ")";

    // FIXED: Properly defined CREATE_PROMOS_TABLE
    private static final String CREATE_PROMOS_TABLE = "CREATE TABLE " + TABLE_PROMOS + "("
            + COLUMN_PROMO_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_PROMO_NAME + " TEXT,"
            + COLUMN_PROMO_DESCRIPTION + " TEXT,"
            + COLUMN_PROMO_START_DATE + " TEXT,"
            + COLUMN_PROMO_END_DATE + " TEXT,"
            + COLUMN_PROMO_TERM_CONDITION + " TEXT,"
            + COLUMN_PROMO_PICTURE + " TEXT,"
            + COLUMN_PROMO_TYPE + " TEXT,"
            + COLUMN_PROMO_DISCOUNT_TYPE + " TEXT,"
            + COLUMN_PROMO_DISCOUNT_AMOUNT + " TEXT,"
            + COLUMN_PROMO_IS_ACTIVE + " INTEGER"
            + ")";

    private static final String CREATE_ORDER_TYPES_TABLE = "CREATE TABLE " + TABLE_ORDER_TYPES + "("
            + "id INTEGER PRIMARY KEY,"
            + "name TEXT NOT NULL"
            + ")";

    private static final String CREATE_ORDER_STATUSES_TABLE = "CREATE TABLE " + TABLE_ORDER_STATUSES + "("
            + "id INTEGER PRIMARY KEY,"
            + "name TEXT NOT NULL"
            + ")";

    public PoodDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MENU_ITEMS_TABLE);
        db.execSQL(CREATE_VARIANTS_TABLE);
        db.execSQL(CREATE_CATEGORIES_TABLE);
        db.execSQL(CREATE_PROMOS_TABLE);
        db.execSQL(CREATE_ORDER_TYPES_TABLE);
        db.execSQL(CREATE_ORDER_STATUSES_TABLE);
        db.execSQL(CREATE_ORDERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        // Handle incremental upgrades
        if (oldVersion < 3) {
            // Add promos table if upgrading from version 2 or earlier
            db.execSQL(CREATE_PROMOS_TABLE);
            Log.d(TAG, "Added promos table in database upgrade");
        }

        if (oldVersion < 4) {
            // Add order types and statuses tables if upgrading from version 3 or earlier
            db.execSQL(CREATE_ORDER_TYPES_TABLE);
            db.execSQL(CREATE_ORDER_STATUSES_TABLE);
            Log.d(TAG, "Added order_types and order_statuses tables in database upgrade");
        }

        if (oldVersion < 5) {
            db.execSQL(CREATE_ORDERS_TABLE); // Add this line
        }

        // For major changes, you can still drop and recreate if needed
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU_ITEMS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_VARIANTS);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROMOS);
        // onCreate(db);
    }

    /**
     * Save promos to database (called from RestaurantApplication)
     */
    public void savePromos(List<Promo> promos) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // Clear existing promos
            db.delete(TABLE_PROMOS, null, null);

            // Insert new promos
            for (Promo promo : promos) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_PROMO_ID, promo.getPromoId());
                values.put(COLUMN_PROMO_NAME, promo.getPromoName());
                values.put(COLUMN_PROMO_DESCRIPTION, promo.getPromoDescription());
                values.put(COLUMN_PROMO_START_DATE, promo.getStartDate());
                values.put(COLUMN_PROMO_END_DATE, promo.getEndDate());
                values.put(COLUMN_PROMO_TERM_CONDITION, promo.getTermAndCondition());
                values.put(COLUMN_PROMO_PICTURE, promo.getPicture());
                values.put(COLUMN_PROMO_TYPE, promo.getType());
                values.put(COLUMN_PROMO_DISCOUNT_TYPE, promo.getDiscountType());
                values.put(COLUMN_PROMO_DISCOUNT_AMOUNT, promo.getDiscountAmount());
                values.put(COLUMN_PROMO_IS_ACTIVE, promo.isActive() ? 1 : 0);

                db.insert(TABLE_PROMOS, null, values);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Saved " + promos.size() + " promos to database");

        } catch (Exception e) {
            Log.e(TAG, "Error saving promos", e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Get all active promos from database (called from PromoRepository)
     */
    public List<Promo> getAllActivePromos() {
        List<Promo> promos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_PROMOS,
                    null,
                    COLUMN_PROMO_IS_ACTIVE + " = ?",
                    new String[]{"1"},
                    null,
                    null,
                    COLUMN_PROMO_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Promo promo = new Promo();

                    promo.setPromoId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_PROMO_ID)));
                    promo.setPromoName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_NAME)));
                    promo.setPromoDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_DESCRIPTION)));
                    promo.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_START_DATE)));
                    promo.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_END_DATE)));
                    promo.setTermAndCondition(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_TERM_CONDITION)));
                    promo.setPicture(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_PICTURE)));
                    promo.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_TYPE)));
                    promo.setDiscountType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_DISCOUNT_TYPE)));
                    promo.setDiscountAmount(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROMO_DISCOUNT_AMOUNT)));
                    promo.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROMO_IS_ACTIVE)) == 1);

                    promos.add(promo);
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error getting promos from database", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "Loaded " + promos.size() + " active promos from database");
        return promos;
    }

    /**
     * Check if we have any promos in the database
     */
    public boolean hasPromos() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String countQuery = "SELECT COUNT(*) FROM " + TABLE_PROMOS;
            Cursor cursor = db.rawQuery(countQuery, null);

            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            cursor.close();
            Log.d(TAG, "Promos count: " + count);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has promos", e);
            return false;
        }
        // DON'T CLOSE db here - let the connection pool handle it
    }

    // ... keep all your existing methods for menu items and categories exactly as they are ...

    public void saveMenuItems(List<ProductItem> menuItems) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // Clear existing data
            db.delete(TABLE_MENU_ITEMS, null, null);
            db.delete(TABLE_VARIANTS, null, null);

            // Insert menu items
            for (ProductItem item : menuItems) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ID, item.getId());
                values.put(COLUMN_NAME, item.getName());
                values.put(COLUMN_DESCRIPTION, item.getDescription());
                values.put(COLUMN_PRICE, item.getPrice());
                values.put(COLUMN_IS_ACTIVE, item.isActive() ? 1 : 0);
                values.put(COLUMN_IMAGE_PATH, item.getImageUrl());
                values.put(COLUMN_CREATED_AT, item.getCreatedAt());
                values.put(COLUMN_UPDATED_AT, item.getUpdatedAt());
                values.put(COLUMN_CATEGORY_NAME, item.getCategory());

                long menuItemId = db.insert(TABLE_MENU_ITEMS, null, values);

                // Insert variants for this menu item
                if (item.getVariants() != null) {
                    for (Variant variant : item.getVariants()) {
                        ContentValues variantValues = new ContentValues();
                        variantValues.put(COLUMN_VARIANT_ID, variant.getId());
                        variantValues.put(COLUMN_MENU_ITEM_ID, item.getId());
                        variantValues.put(COLUMN_VARIANT_NAME, variant.getName());
                        variantValues.put(COLUMN_VARIANT_PRICE, variant.getPrice());
                        variantValues.put(COLUMN_VARIANT_IS_ACTIVE, variant.isActive() ? 1 : 0);
                        variantValues.put(COLUMN_VARIANT_CREATED_AT, variant.getCreatedAt());
                        variantValues.put(COLUMN_VARIANT_UPDATED_AT, variant.getUpdatedAt());

                        db.insert(TABLE_VARIANTS, null, variantValues);
                    }
                }
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Saved " + menuItems.size() + " menu items to database");

        } catch (Exception e) {
            Log.e(TAG, "Error saving menu items to database", e);
        } finally {
            db.endTransaction();
            // REMOVED db.close() - let connection pool handle it
        }
    }

    public void saveMenuCategories(List<MenuCategory> categories) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.beginTransaction();

            // Clear existing categories
            db.delete(TABLE_CATEGORIES, null, null);

            // Insert categories
            for (MenuCategory category : categories) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_CAT_ID, category.getId());
                values.put(COLUMN_CAT_NAME, category.getName());
                values.put(COLUMN_CAT_DESCRIPTION, category.getDescription());
                values.put(COLUMN_CAT_CREATED_AT, category.getCreatedAt());
                values.put(COLUMN_CAT_UPDATED_AT, category.getUpdatedAt());
                values.put(COLUMN_CAT_IS_DISPLAYED, category.isDisplayed() ? 1 : 0);
                values.put(COLUMN_CAT_DISPLAY_PICTURE, category.getDisplayPicture());
                values.put(COLUMN_CAT_GROUP, category.getMenuCategoryGroup());
                values.put(COLUMN_CAT_SKU_ID, category.getSkuId());
                values.put(COLUMN_CAT_IS_HIGHLIGHT, category.isHighlight() ? 1 : 0);
                values.put(COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER, category.isDisplayForSelfOrder() ? 1 : 0);

                db.insert(TABLE_CATEGORIES, null, values);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "Saved " + categories.size() + " categories to database");

        } catch (Exception e) {
            Log.e(TAG, "Error saving categories to database", e);
        } finally {
            db.endTransaction();
            // REMOVED db.close() - let connection pool handle it
        }
    }

    public List<ProductItem> getAllMenuItems() {
        List<ProductItem> menuItems = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_MENU_ITEMS + " ORDER BY " + COLUMN_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ProductItem item = new ProductItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
                item.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)) == 1);
                item.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
                item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)));
                item.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT)));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME)));

                // Load variants for this menu item
                item.setVariants(getVariantsForMenuItem(item.getId()));

                menuItems.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        // REMOVED db.close() - let connection pool handle it

        Log.d(TAG, "Loaded " + menuItems.size() + " menu items from database");
        return menuItems;
    }

    public List<MenuCategory> getAllMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_CATEGORIES + " ORDER BY " + COLUMN_CAT_NAME + " ASC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                MenuCategory category = new MenuCategory();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CAT_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_NAME)));
                category.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_DESCRIPTION)));
                category.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_CREATED_AT)));
                category.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_UPDATED_AT)));
                category.setDisplayed(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_IS_DISPLAYED)) == 1);
                category.setDisplayPicture(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_DISPLAY_PICTURE)));
                category.setMenuCategoryGroup(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_GROUP)));
                category.setSkuId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CAT_SKU_ID)));
                category.setHighlight(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_IS_HIGHLIGHT)) == 1);
                category.setDisplayForSelfOrder(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER)) == 1);

                categories.add(category);
            } while (cursor.moveToNext());
        }

        cursor.close();
        // REMOVED db.close() - let connection pool handle it

        Log.d(TAG, "Loaded " + categories.size() + " categories from database");
        return categories;
    }

    private List<Variant> getVariantsForMenuItem(long menuItemId) {
        List<Variant> variants = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_VARIANTS + " WHERE " + COLUMN_MENU_ITEM_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(menuItemId)});

        if (cursor.moveToFirst()) {
            do {
                Variant variant = new Variant();
                variant.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_ID)));
                variant.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_NAME)));
                variant.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_PRICE)));
                variant.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_IS_ACTIVE)) == 1);
                variant.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_CREATED_AT)));
                variant.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VARIANT_UPDATED_AT)));

                variants.add(variant);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return variants;
    }

    public boolean hasMenuItems() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String countQuery = "SELECT COUNT(*) FROM " + TABLE_MENU_ITEMS;
            Cursor cursor = db.rawQuery(countQuery, null);

            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            cursor.close();
            Log.d(TAG, "Menu items count: " + count);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has menu items", e);
            return false;
        }
        // REMOVED db.close() - let connection pool handle it
    }

    public boolean hasMenuCategories() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            String countQuery = "SELECT COUNT(*) FROM " + TABLE_CATEGORIES;
            Cursor cursor = db.rawQuery(countQuery, null);

            int count = 0;
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }

            cursor.close();
            Log.d(TAG, "Categories count: " + count);
            return count > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has categories", e);
            return false;
        }
        // REMOVED db.close() - let connection pool handle it
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.delete(TABLE_MENU_ITEMS, null, null);
            db.delete(TABLE_VARIANTS, null, null);
            db.delete(TABLE_CATEGORIES, null, null);
            db.delete(TABLE_PROMOS, null, null); // ADDED PROMOS CLEARING
            db.delete(TABLE_ORDER_TYPES, null, null);
            db.delete(TABLE_ORDER_STATUSES, null, null);
            db.delete(TABLE_ORDERS, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing data", e);
        }
        // REMOVED db.close() - let connection pool handle it
    }

    // Add this method to your PoodDatabase class to test exact SQL queries

    public void testPromoRetrieval() {
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Log.d(TAG, "=== TESTING PROMO RETRIEVAL ===");

            // Test 1: Check table name and existence
            Cursor tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='promos'", null);
            if (tableCheck.moveToFirst()) {
                Log.d(TAG, "✓ Table 'promos' exists");
            } else {
                Log.e(TAG, "✗ Table 'promos' does NOT exist");
                // Try different table name
                tableCheck.close();
                tableCheck = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE '%promo%'", null);
                if (tableCheck.moveToFirst()) {
                    do {
                        Log.d(TAG, "Found table with 'promo' in name: " + tableCheck.getString(0));
                    } while (tableCheck.moveToNext());
                }
            }
            tableCheck.close();

            // Test 2: Count all records
            Cursor countAll = db.rawQuery("SELECT COUNT(*) FROM promos", null);
            if (countAll.moveToFirst()) {
                int totalCount = countAll.getInt(0);
                Log.d(TAG, "✓ Total records in promos table: " + totalCount);
            }
            countAll.close();

            // Test 3: Check column names in the actual table
            Cursor columnInfo = db.rawQuery("PRAGMA table_info(promos)", null);
            Log.d(TAG, "Actual columns in promos table:");
            while (columnInfo.moveToNext()) {
                String columnName = columnInfo.getString(1);
                String dataType = columnInfo.getString(2);
                Log.d(TAG, "  - " + columnName + " (" + dataType + ")");
            }
            columnInfo.close();

            // Test 4: Get first few records with all columns
            Cursor sampleData = db.rawQuery("SELECT * FROM promos LIMIT 3", null);
            Log.d(TAG, "Sample data from promos table:");

            if (sampleData.moveToFirst()) {
                String[] columnNames = sampleData.getColumnNames();
                Log.d(TAG, "Column names: " + Arrays.toString(columnNames));

                int recordNum = 1;
                do {
                    Log.d(TAG, "--- Record " + recordNum + " ---");
                    for (int i = 0; i < columnNames.length; i++) {
                        String value = sampleData.getString(i);
                        Log.d(TAG, columnNames[i] + ": '" + value + "'");
                    }
                    recordNum++;
                } while (sampleData.moveToNext());
            } else {
                Log.e(TAG, "✗ No data found in promos table");
            }
            sampleData.close();

            // Test 5: Check is_active values specifically
            Cursor activeCheck = db.rawQuery("SELECT promo_id, promo_name, is_active FROM promos", null);
            Log.d(TAG, "Checking is_active values:");
            while (activeCheck.moveToFirst()) {
                do {
                    long id = activeCheck.getLong(0);
                    String name = activeCheck.getString(1);
                    String isActiveValue = activeCheck.getString(2);
                    Log.d(TAG, "ID: " + id + ", Name: '" + name + "', is_active: '" + isActiveValue + "'");
                } while (activeCheck.moveToNext());
            }
            activeCheck.close();

            // Test 6: Try the exact query used in getAllActivePromos
            String exactQuery = "SELECT * FROM promos WHERE is_active = ? ORDER BY promo_name ASC";
            Cursor exactTest = db.rawQuery(exactQuery, new String[]{"1"});
            Log.d(TAG, "Exact query test: " + exactQuery);
            Log.d(TAG, "Result count: " + exactTest.getCount());
            exactTest.close();

            // Test 7: Try different variations of active check
            String[] activeVariations = {"1", "true", "TRUE", "True"};
            for (String variation : activeVariations) {
                Cursor variationTest = db.rawQuery("SELECT COUNT(*) FROM promos WHERE is_active = ?", new String[]{variation});
                if (variationTest.moveToFirst()) {
                    int count = variationTest.getInt(0);
                    Log.d(TAG, "Active check with '" + variation + "': " + count + " records");
                }
                variationTest.close();
            }

            Log.d(TAG, "=== END TESTING PROMO RETRIEVAL ===");

        } catch (Exception e) {
            Log.e(TAG, "Error in testPromoRetrieval", e);
            e.printStackTrace();
        }
    }

    // Simplified version of getAllActivePromos for testing
    public List<Promo> getAllActivePromosSimple() {
        Log.d(TAG, "getAllActivePromosSimple called");

        // First run the test
        testPromoRetrieval();

        List<Promo> promos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            // Use the simplest possible query first
            Cursor cursor = db.rawQuery("SELECT * FROM promos", null);
            Log.d(TAG, "Simple query returned " + cursor.getCount() + " records");

            if (cursor.moveToFirst()) {
                do {
                    Promo promo = new Promo();

                    // Get column indices safely
                    int promoIdIndex = cursor.getColumnIndex("promo_id");
                    int promoNameIndex = cursor.getColumnIndex("promo_name");
                    int isActiveIndex = cursor.getColumnIndex("is_active");

                    if (promoIdIndex >= 0) {
                        promo.setPromoId(cursor.getLong(promoIdIndex));
                    }

                    if (promoNameIndex >= 0) {
                        promo.setPromoName(cursor.getString(promoNameIndex));
                    }

                    if (isActiveIndex >= 0) {
                        // Check the raw value
                        String rawActiveValue = cursor.getString(isActiveIndex);
                        Log.d(TAG, "Raw is_active value: '" + rawActiveValue + "'");

                        // Try different interpretations
                        boolean isActive = false;
                        if ("1".equals(rawActiveValue) || "true".equalsIgnoreCase(rawActiveValue)) {
                            isActive = true;
                        }
                        promo.setActive(isActive);
                    }

                    // Set other fields safely
                    int descIndex = cursor.getColumnIndex("promo_description");
                    if (descIndex >= 0) {
                        promo.setPromoDescription(cursor.getString(descIndex));
                    }

                    // For now, add ALL promos regardless of active status for testing
                    promos.add(promo);
                    Log.d(TAG, "Added promo: " + promo.getPromoName() + " (Active: " + promo.isActive() + ")");

                } while (cursor.moveToNext());
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "Error in getAllActivePromosSimple", e);
            e.printStackTrace();
        }

        Log.d(TAG, "getAllActivePromosSimple returning " + promos.size() + " promos");
        return promos;
    }

    public void saveOrderTypes(List<OrderType> orderTypes) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Clear existing order types
            db.delete("order_types", null, null);

            // Insert new order types
            for (OrderType orderType : orderTypes) {
                ContentValues values = new ContentValues();
                values.put("id", orderType.getId());
                values.put("name", orderType.getName());
                // Remove is_active field since method doesn't exist

                db.insert("order_types", null, values);
            }

            db.setTransactionSuccessful();
            Log.d("PoodDatabase", "Saved " + orderTypes.size() + " order types");
        } catch (Exception e) {
            Log.e("PoodDatabase", "Error saving order types", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<OrderType> getOrderTypes() {
        List<OrderType> orderTypes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            // Get all order types (no filtering by active since we don't store that field)
            Cursor cursor = db.query("order_types", null, null, null, null, null, "name ASC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    OrderType orderType = new OrderType();
                    orderType.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    orderType.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    // Remove setActive since method doesn't exist

                    orderTypes.add(orderType);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("PoodDatabase", "Error getting order types", e);
        }

        return orderTypes;
    }

    public void saveOrderStatuses(List<OrderStatus> orderStatuses) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            // Clear existing order statuses
            db.delete("order_statuses", null, null);

            // Insert new order statuses
            for (OrderStatus orderStatus : orderStatuses) {
                ContentValues values = new ContentValues();
                values.put("id", orderStatus.getId());
                values.put("name", orderStatus.getName());
                // Remove color and is_active fields

                db.insert("order_statuses", null, values);
            }

            db.setTransactionSuccessful();
            Log.d("PoodDatabase", "Saved " + orderStatuses.size() + " order statuses");
        } catch (Exception e) {
            Log.e("PoodDatabase", "Error saving order statuses", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<OrderStatus> getOrderStatuses() {
        List<OrderStatus> orderStatuses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            // Get all order statuses (no filtering)
            Cursor cursor = db.query("order_statuses", null, null, null, null, null, "name ASC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    OrderStatus orderStatus = new OrderStatus();
                    orderStatus.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
                    orderStatus.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
                    // Remove setColor and setActive since methods might not exist

                    orderStatuses.add(orderStatus);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("PoodDatabase", "Error getting order statuses", e);
        }

        return orderStatuses;
    }

    public long saveOrderLocally(long sessionId, String tableNumber, String customerName, long orderTypeId) {
        SQLiteDatabase db = this.getWritableDatabase();
        long orderId = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SESSION_ID, sessionId);
            values.put(COLUMN_TABLE_NUMBER, tableNumber);
            values.put(COLUMN_CUSTOMER_NAME, customerName);
            values.put(COLUMN_ORDER_TYPE_ID, orderTypeId);
            values.put(COLUMN_IS_SYNCED, 0); // Not synced initially
            values.put(COLUMN_ORDER_CREATED_AT, getCurrentTimestamp());

            orderId = db.insert(TABLE_ORDERS, null, values);

        } catch (Exception e) {
            Log.e(TAG, "Error saving order locally", e);
            orderId = -1;
        }

        return orderId;
    }

    public void markOrderAsSynced(long localOrderId, long serverOrderId) {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_SERVER_ORDER_ID, serverOrderId);
            values.put(COLUMN_IS_SYNCED, 1);

            int rowsUpdated = db.update(TABLE_ORDERS,
                    values,
                    COLUMN_ORDER_ID + " = ?",
                    new String[]{String.valueOf(localOrderId)});

        } catch (Exception e) {
            Log.e(TAG, "Error marking order as synced", e);
        }
    }

    public List<Long> getUnsyncedOrderIds() {
        List<Long> unsyncedIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        try {
            Cursor cursor = db.query(TABLE_ORDERS,
                    new String[]{COLUMN_ORDER_ID},
                    COLUMN_IS_SYNCED + " = ?",
                    new String[]{"0"},
                    null,
                    null,
                    COLUMN_ORDER_CREATED_AT + " ASC");

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long orderId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ORDER_ID));
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
}