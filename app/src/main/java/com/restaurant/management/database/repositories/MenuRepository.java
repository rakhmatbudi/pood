package com.restaurant.management.database.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.restaurant.management.database.PoodDatabaseHelper;
import com.restaurant.management.database.DatabaseSchema;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.models.MenuCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for menu-related database operations
 */
public class MenuRepository {
    private static final String TAG = "MenuRepository";
    private final PoodDatabaseHelper dbHelper;

    public MenuRepository(Context context) {
        this.dbHelper = PoodDatabaseHelper.getInstance(context);
    }

    public void saveMenuItems(List<ProductItem> menuItems) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(PoodDatabaseHelper.TABLE_MENU_ITEMS, null, null);
            db.delete(PoodDatabaseHelper.TABLE_VARIANTS, null, null);

            for (ProductItem item : menuItems) {
                ContentValues values = new ContentValues();
                values.put(DatabaseSchema.COLUMN_ID, item.getId());
                values.put(DatabaseSchema.COLUMN_NAME, item.getName());
                values.put(DatabaseSchema.COLUMN_DESCRIPTION, item.getDescription());
                values.put(DatabaseSchema.COLUMN_PRICE, item.getPrice());
                values.put(DatabaseSchema.COLUMN_IS_ACTIVE, item.isActive() ? 1 : 0);
                values.put(DatabaseSchema.COLUMN_IMAGE_PATH, item.getImageUrl());
                values.put(DatabaseSchema.COLUMN_CREATED_AT, item.getCreatedAt());
                values.put(DatabaseSchema.COLUMN_UPDATED_AT, item.getUpdatedAt());
                values.put(DatabaseSchema.COLUMN_CATEGORY_NAME, item.getCategory());
                db.insert(PoodDatabaseHelper.TABLE_MENU_ITEMS, null, values);

                if (item.getVariants() != null) {
                    saveVariantsForItem(db, item.getId(), item.getVariants());
                }
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving menu items to database", e);
        } finally {
            db.endTransaction();
        }
    }

    private void saveVariantsForItem(SQLiteDatabase db, long menuItemId, List<Variant> variants) {
        for (Variant variant : variants) {
            ContentValues variantValues = new ContentValues();
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_ID, variant.getId());
            variantValues.put(DatabaseSchema.COLUMN_MENU_ITEM_ID, menuItemId);
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_NAME, variant.getName());
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_PRICE, variant.getPrice());
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_IS_ACTIVE, variant.isActive() ? 1 : 0);
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_CREATED_AT, variant.getCreatedAt());
            variantValues.put(DatabaseSchema.COLUMN_VARIANT_UPDATED_AT, variant.getUpdatedAt());
            db.insert(PoodDatabaseHelper.TABLE_VARIANTS, null, variantValues);
        }
    }

    public List<ProductItem> getAllMenuItems() {
        List<ProductItem> menuItems = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + PoodDatabaseHelper.TABLE_MENU_ITEMS +
                " ORDER BY " + DatabaseSchema.COLUMN_NAME + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ProductItem item = new ProductItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_ID)));
                item.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_NAME)));
                item.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_DESCRIPTION)));
                item.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PRICE)));
                item.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IS_ACTIVE)) == 1);
                item.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_IMAGE_PATH)));
                item.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CREATED_AT)));
                item.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_UPDATED_AT)));
                item.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CATEGORY_NAME)));
                item.setVariants(getVariantsForMenuItem(item.getId()));
                menuItems.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return menuItems;
    }

    public List<Variant> getVariantsForMenuItem(long menuItemId) {
        List<Variant> variants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + PoodDatabaseHelper.TABLE_VARIANTS +
                " WHERE " + DatabaseSchema.COLUMN_MENU_ITEM_ID + " = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(menuItemId)});

        if (cursor.moveToFirst()) {
            do {
                Variant variant = new Variant();
                variant.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_ID)));
                variant.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_NAME)));
                variant.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_PRICE)));
                variant.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_IS_ACTIVE)) == 1);
                variant.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_CREATED_AT)));
                variant.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_UPDATED_AT)));
                variants.add(variant);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return variants;
    }

    public List<Variant> getAllVariants() {
        List<Variant> variants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try {
            Cursor cursor = db.query(PoodDatabaseHelper.TABLE_VARIANTS, null, null, null, null, null,
                    DatabaseSchema.COLUMN_VARIANT_NAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Variant variant = new Variant();
                    variant.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_ID)));
                    variant.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_NAME)));
                    variant.setPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_PRICE)));
                    variant.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_VARIANT_IS_ACTIVE)) == 1);
                    variants.add(variant);
                } while (cursor.moveToNext());
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all variants", e);
        }
        return variants;
    }

    public void saveMenuCategories(List<MenuCategory> categories) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(PoodDatabaseHelper.TABLE_CATEGORIES, null, null);
            for (MenuCategory category : categories) {
                ContentValues values = new ContentValues();
                values.put(DatabaseSchema.COLUMN_CAT_ID, category.getId());
                values.put(DatabaseSchema.COLUMN_CAT_NAME, category.getName());
                values.put(DatabaseSchema.COLUMN_CAT_DESCRIPTION, category.getDescription());
                values.put(DatabaseSchema.COLUMN_CAT_CREATED_AT, category.getCreatedAt());
                values.put(DatabaseSchema.COLUMN_CAT_UPDATED_AT, category.getUpdatedAt());
                values.put(DatabaseSchema.COLUMN_CAT_IS_DISPLAYED, category.isDisplayed() ? 1 : 0);
                values.put(DatabaseSchema.COLUMN_CAT_DISPLAY_PICTURE, category.getDisplayPicture());
                values.put(DatabaseSchema.COLUMN_CAT_GROUP, category.getMenuCategoryGroup());
                values.put(DatabaseSchema.COLUMN_CAT_SKU_ID, category.getSkuId());
                values.put(DatabaseSchema.COLUMN_CAT_IS_HIGHLIGHT, category.isHighlight() ? 1 : 0);
                values.put(DatabaseSchema.COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER, category.isDisplayForSelfOrder() ? 1 : 0);
                db.insert(PoodDatabaseHelper.TABLE_CATEGORIES, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving categories to database", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<MenuCategory> getAllMenuCategories() {
        List<MenuCategory> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + PoodDatabaseHelper.TABLE_CATEGORIES +
                " ORDER BY " + DatabaseSchema.COLUMN_CAT_NAME + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                MenuCategory category = new MenuCategory();
                category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_ID)));
                category.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_NAME)));
                category.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_DESCRIPTION)));
                category.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_CREATED_AT)));
                category.setUpdatedAt(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_UPDATED_AT)));
                category.setDisplayed(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_IS_DISPLAYED)) == 1);
                category.setDisplayPicture(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_DISPLAY_PICTURE)));
                category.setMenuCategoryGroup(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_GROUP)));
                category.setSkuId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_SKU_ID)));
                category.setHighlight(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_IS_HIGHLIGHT)) == 1);
                category.setDisplayForSelfOrder(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER)) == 1);
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public boolean hasMenuItems() {
        return getTableCount(PoodDatabaseHelper.TABLE_MENU_ITEMS) > 0;
    }

    public boolean hasMenuCategories() {
        return getTableCount(PoodDatabaseHelper.TABLE_CATEGORIES) > 0;
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