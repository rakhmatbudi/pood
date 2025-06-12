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

import java.util.ArrayList;
import java.util.List;

public class PoodDatabase extends SQLiteOpenHelper {
    private static final String TAG = "PoodDatabase";
    private static final String DATABASE_NAME = "pood.db";
    private static final int DATABASE_VERSION = 2; // Increased version for categories

    // Table names
    private static final String TABLE_MENU_ITEMS = "menu_items";
    private static final String TABLE_VARIANTS = "variants";
    private static final String TABLE_CATEGORIES = "categories";

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

    public PoodDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MENU_ITEMS_TABLE);
        db.execSQL(CREATE_VARIANTS_TABLE);
        db.execSQL(CREATE_CATEGORIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MENU_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VARIANTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        onCreate(db);
    }

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
            db.close();
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
            db.close();
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
        db.close();

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
        db.close();

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
        } finally {
            db.close();
        }
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
        } finally {
            db.close();
        }
    }

    public void clearAllData() {
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            db.delete(TABLE_MENU_ITEMS, null, null);
            db.delete(TABLE_VARIANTS, null, null);
            db.delete(TABLE_CATEGORIES, null, null);
            Log.d(TAG, "Cleared all menu data");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing menu data", e);
        } finally {
            db.close();
        }
    }
}