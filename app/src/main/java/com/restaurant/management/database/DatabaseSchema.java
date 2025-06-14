package com.restaurant.management.database;

/**
 * Contains all database schema definitions and SQL statements
 */
public class DatabaseSchema {

    // Common columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    // Menu Items table columns
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_CATEGORY_ID = "category_id";
    public static final String COLUMN_CATEGORY_NAME = "category_name";
    public static final String COLUMN_CATEGORY_DESCRIPTION = "category_description";

    // Variants table columns
    public static final String COLUMN_VARIANT_ID = "variant_id";
    public static final String COLUMN_MENU_ITEM_ID = "menu_item_id";
    public static final String COLUMN_VARIANT_NAME = "variant_name";
    public static final String COLUMN_VARIANT_PRICE = "variant_price";
    public static final String COLUMN_VARIANT_IS_ACTIVE = "variant_is_active";
    public static final String COLUMN_VARIANT_CREATED_AT = "variant_created_at";
    public static final String COLUMN_VARIANT_UPDATED_AT = "variant_updated_at";

    // Categories table columns
    public static final String COLUMN_CAT_ID = "cat_id";
    public static final String COLUMN_CAT_NAME = "cat_name";
    public static final String COLUMN_CAT_DESCRIPTION = "cat_description";
    public static final String COLUMN_CAT_CREATED_AT = "cat_created_at";
    public static final String COLUMN_CAT_UPDATED_AT = "cat_updated_at";
    public static final String COLUMN_CAT_IS_DISPLAYED = "cat_is_displayed";
    public static final String COLUMN_CAT_DISPLAY_PICTURE = "cat_display_picture";
    public static final String COLUMN_CAT_GROUP = "cat_group";
    public static final String COLUMN_CAT_SKU_ID = "cat_sku_id";
    public static final String COLUMN_CAT_IS_HIGHLIGHT = "cat_is_highlight";
    public static final String COLUMN_CAT_IS_DISPLAY_FOR_SELF_ORDER = "cat_is_display_for_self_order";

    // Promo table columns
    public static final String COLUMN_PROMO_ID = "promo_id";
    public static final String COLUMN_PROMO_NAME = "promo_name";
    public static final String COLUMN_PROMO_DESCRIPTION = "promo_description";
    public static final String COLUMN_PROMO_START_DATE = "start_date";
    public static final String COLUMN_PROMO_END_DATE = "end_date";
    public static final String COLUMN_PROMO_TERM_CONDITION = "term_and_condition";
    public static final String COLUMN_PROMO_PICTURE = "picture";
    public static final String COLUMN_PROMO_TYPE = "type";
    public static final String COLUMN_PROMO_DISCOUNT_TYPE = "discount_type";
    public static final String COLUMN_PROMO_DISCOUNT_AMOUNT = "discount_amount";
    public static final String COLUMN_PROMO_IS_ACTIVE = "is_active";

    // Order table columns
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_TABLE_NUMBER = "table_number";
    public static final String COLUMN_CUSTOMER_NAME = "customer_name";
    public static final String COLUMN_ORDER_TYPE_ID = "order_type_id";
    public static final String COLUMN_SERVER_ORDER_ID = "server_order_id";
    public static final String COLUMN_IS_SYNCED = "is_synced";
    public static final String COLUMN_ORDER_CREATED_AT = "order_created_at";

    // Order Items table columns
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_ORDER_ITEM_ORDER_ID = "order_id";
    public static final String COLUMN_MENU_ITEM_ID_FK = "menu_item_id";
    public static final String COLUMN_VARIANT_ID_FK = "variant_id";
    public static final String COLUMN_ITEM_QUANTITY = "quantity";
    public static final String COLUMN_ITEM_UNIT_PRICE = "unit_price";
    public static final String COLUMN_ITEM_TOTAL_PRICE = "total_price";
    public static final String COLUMN_ITEM_NOTES = "notes";
    public static final String COLUMN_ITEM_STATUS = "status";
    public static final String COLUMN_ITEM_IS_COMPLIMENTARY = "is_complimentary";
    public static final String COLUMN_ITEM_IS_CUSTOM_PRICE = "is_custom_price";
    public static final String COLUMN_ITEM_ORIGINAL_PRICE = "original_price";
    public static final String COLUMN_ITEM_KITCHEN_PRINTED = "kitchen_printed";
    public static final String COLUMN_ITEM_IS_SYNCED = "is_synced";
    public static final String COLUMN_ITEM_SERVER_ID = "server_id";
    public static final String COLUMN_ITEM_CREATED_AT = "item_created_at";

    // Create table statements
    public static final String CREATE_MENU_ITEMS_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_MENU_ITEMS + "("
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

    public static final String CREATE_VARIANTS_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_VARIANTS + "("
            + COLUMN_VARIANT_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_MENU_ITEM_ID + " INTEGER,"
            + COLUMN_VARIANT_NAME + " TEXT,"
            + COLUMN_VARIANT_PRICE + " REAL,"
            + COLUMN_VARIANT_IS_ACTIVE + " INTEGER,"
            + COLUMN_VARIANT_CREATED_AT + " TEXT,"
            + COLUMN_VARIANT_UPDATED_AT + " TEXT,"
            + "FOREIGN KEY(" + COLUMN_MENU_ITEM_ID + ") REFERENCES " + PoodDatabaseHelper.TABLE_MENU_ITEMS + "(" + COLUMN_ID + ")"
            + ")";

    public static final String CREATE_CATEGORIES_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_CATEGORIES + "("
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

    public static final String CREATE_PROMOS_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_PROMOS + "("
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

    public static final String CREATE_ORDER_TYPES_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_ORDER_TYPES + "("
            + "id INTEGER PRIMARY KEY,"
            + "name TEXT NOT NULL"
            + ")";

    public static final String CREATE_ORDER_STATUSES_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_ORDER_STATUSES + "("
            + "id INTEGER PRIMARY KEY,"
            + "name TEXT NOT NULL"
            + ")";

    public static final String CREATE_ORDERS_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_ORDERS + "("
            + COLUMN_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_SESSION_ID + " INTEGER NOT NULL,"
            + COLUMN_TABLE_NUMBER + " TEXT NOT NULL,"
            + COLUMN_CUSTOMER_NAME + " TEXT,"
            + COLUMN_ORDER_TYPE_ID + " INTEGER NOT NULL,"
            + COLUMN_SERVER_ORDER_ID + " INTEGER,"
            + COLUMN_IS_SYNCED + " INTEGER DEFAULT 0,"
            + COLUMN_ORDER_CREATED_AT + " TEXT NOT NULL"
            + ")";

    public static final String CREATE_ORDER_ITEMS_TABLE = "CREATE TABLE " + PoodDatabaseHelper.TABLE_ORDER_ITEMS + "("
            + COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_ORDER_ITEM_ORDER_ID + " INTEGER NOT NULL,"
            + COLUMN_MENU_ITEM_ID_FK + " INTEGER NOT NULL,"
            + COLUMN_VARIANT_ID_FK + " INTEGER,"
            + COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL,"
            + COLUMN_ITEM_UNIT_PRICE + " REAL NOT NULL,"
            + COLUMN_ITEM_TOTAL_PRICE + " REAL NOT NULL,"
            + COLUMN_ITEM_NOTES + " TEXT,"
            + COLUMN_ITEM_STATUS + " TEXT DEFAULT 'new',"
            + COLUMN_ITEM_IS_COMPLIMENTARY + " INTEGER DEFAULT 0,"
            + COLUMN_ITEM_IS_CUSTOM_PRICE + " INTEGER DEFAULT 0,"
            + COLUMN_ITEM_ORIGINAL_PRICE + " REAL,"
            + COLUMN_ITEM_KITCHEN_PRINTED + " INTEGER DEFAULT 0,"
            + COLUMN_ITEM_IS_SYNCED + " INTEGER DEFAULT 0,"
            + COLUMN_ITEM_SERVER_ID + " INTEGER,"
            + COLUMN_ITEM_CREATED_AT + " TEXT NOT NULL,"
            + "FOREIGN KEY(" + COLUMN_ORDER_ITEM_ORDER_ID + ") REFERENCES " + PoodDatabaseHelper.TABLE_ORDERS + "(" + COLUMN_ORDER_ID + "),"
            + "FOREIGN KEY(" + COLUMN_MENU_ITEM_ID_FK + ") REFERENCES " + PoodDatabaseHelper.TABLE_MENU_ITEMS + "(" + COLUMN_ID + ")"
            + ")";
}