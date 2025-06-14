package com.restaurant.management.database.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.restaurant.management.database.PoodDatabaseHelper;
import com.restaurant.management.database.DatabaseSchema;
import com.restaurant.management.models.Promo;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for promo-related database operations
 */
public class PromoRepository {
    private static final String TAG = "PromoRepository";
    private final PoodDatabaseHelper dbHelper;

    public PromoRepository(Context context) {
        this.dbHelper = PoodDatabaseHelper.getInstance(context);
    }

    public void savePromos(List<Promo> promos) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(PoodDatabaseHelper.TABLE_PROMOS, null, null);
            for (Promo promo : promos) {
                ContentValues values = new ContentValues();
                values.put(DatabaseSchema.COLUMN_PROMO_ID, promo.getPromoId());
                values.put(DatabaseSchema.COLUMN_PROMO_NAME, promo.getPromoName());
                values.put(DatabaseSchema.COLUMN_PROMO_DESCRIPTION, promo.getPromoDescription());
                values.put(DatabaseSchema.COLUMN_PROMO_START_DATE, promo.getStartDate());
                values.put(DatabaseSchema.COLUMN_PROMO_END_DATE, promo.getEndDate());
                values.put(DatabaseSchema.COLUMN_PROMO_TERM_CONDITION, promo.getTermAndCondition());
                values.put(DatabaseSchema.COLUMN_PROMO_PICTURE, promo.getPicture());
                values.put(DatabaseSchema.COLUMN_PROMO_TYPE, promo.getType());
                values.put(DatabaseSchema.COLUMN_PROMO_DISCOUNT_TYPE, promo.getDiscountType());
                values.put(DatabaseSchema.COLUMN_PROMO_DISCOUNT_AMOUNT, promo.getDiscountAmount());
                values.put(DatabaseSchema.COLUMN_PROMO_IS_ACTIVE, promo.isActive() ? 1 : 0);
                db.insert(PoodDatabaseHelper.TABLE_PROMOS, null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error saving promos", e);
        } finally {
            db.endTransaction();
        }
    }

    public List<Promo> getAllActivePromos() {
        List<Promo> promos = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(PoodDatabaseHelper.TABLE_PROMOS, null,
                    DatabaseSchema.COLUMN_PROMO_IS_ACTIVE + " = ?",
                    new String[]{"1"}, null, null,
                    DatabaseSchema.COLUMN_PROMO_NAME + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Promo promo = createPromoFromCursor(cursor);
                    promos.add(promo);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting promos from database", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return promos;
    }

    public List<Promo> getAllPromos() {
        List<Promo> promos = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(PoodDatabaseHelper.TABLE_PROMOS, null, null, null, null, null,
                DatabaseSchema.COLUMN_PROMO_NAME + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Promo promo = createPromoFromCursor(cursor);
                promo.setPromoItems(new ArrayList<>());
                promos.add(promo);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return promos;
    }

    private Promo createPromoFromCursor(Cursor cursor) {
        Promo promo = new Promo();
        promo.setPromoId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_ID)));
        promo.setPromoName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_NAME)));
        promo.setPromoDescription(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_DESCRIPTION)));
        promo.setStartDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_START_DATE)));
        promo.setEndDate(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_END_DATE)));
        promo.setTermAndCondition(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_TERM_CONDITION)));
        promo.setPicture(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_PICTURE)));
        promo.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_TYPE)));
        promo.setDiscountType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_DISCOUNT_TYPE)));
        promo.setDiscountAmount(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_DISCOUNT_AMOUNT)));
        promo.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseSchema.COLUMN_PROMO_IS_ACTIVE)) == 1);
        return promo;
    }

    public boolean hasPromos() {
        return getTableCount(PoodDatabaseHelper.TABLE_PROMOS) > 0;
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