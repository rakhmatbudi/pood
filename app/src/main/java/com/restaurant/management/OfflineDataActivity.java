package com.restaurant.management;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.restaurant.management.R;
import com.restaurant.management.RestaurantApplication;
import com.restaurant.management.adapters.OfflineDataAdapter;
import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.models.OfflineDataItem;
import com.restaurant.management.models.OrderStatus;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Promo;
import com.restaurant.management.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OfflineDataActivity extends AppCompatActivity {
    private static final String TAG = "OfflineDataActivity";

    private RecyclerView recyclerView;
    private OfflineDataAdapter adapter;
    private TextView tvTotalItems;
    private TextView tvLastSync;
    private TextView tvNetworkStatus;
    private Button btnSync;
    private Button btnDeleteAll;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private PoodDatabase database;
    private RestaurantApplication app;
    private ExecutorService executor;

    private List<OfflineDataItem> offlineDataItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_data);

        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Offline Data Management");
        }

        initializeViews();
        initializeDatabase();
        setupRecyclerView();
        setupButtons();
        setupSwipeRefresh();

        loadOfflineData();
        updateNetworkStatus();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewOfflineData);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvLastSync = findViewById(R.id.tvLastSync);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        btnSync = findViewById(R.id.btnSync);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void initializeDatabase() {
        database = new PoodDatabase(this);
        app = (RestaurantApplication) getApplication();
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        adapter = new OfflineDataAdapter(offlineDataItems, this::onItemClick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        btnSync.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(this)) {
                showSyncConfirmationDialog();
            } else {
                Toast.makeText(this, "No internet connection available", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeleteAll.setOnClickListener(v -> showDeleteAllConfirmationDialog());
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadOfflineData();
            updateNetworkStatus();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void loadOfflineData() {
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                List<OfflineDataItem> items = new ArrayList<>();

                Log.d(TAG, "=== LOADING ALL OFFLINE DATA ===");

                // 1. DOWNLOADED DATA (from server)

                // Get cached menu categories
                int categories = database.getMenuCategories().size();
                Log.d(TAG, "Menu categories: " + categories);
                if (categories > 0) {
                    items.add(new OfflineDataItem(
                            "Menu Categories",
                            categories + " categories cached",
                            categories,
                            "categories"
                    ));
                }

                // Get cached menu items
                int menuItems = database.getMenuItems().size();
                Log.d(TAG, "Menu items: " + menuItems);
                if (menuItems > 0) {
                    items.add(new OfflineDataItem(
                            "Menu Items",
                            menuItems + " items cached",
                            menuItems,
                            "menu_items"
                    ));
                }

                // Get cached promos
                int promos = database.getPromos().size();
                Log.d(TAG, "Promos: " + promos);
                if (promos > 0) {
                    items.add(new OfflineDataItem(
                            "Promotions",
                            promos + " promos cached",
                            promos,
                            "promos"
                    ));
                }

                // Get cached order types
                int orderTypes = database.getOrderTypes().size();
                Log.d(TAG, "Order types: " + orderTypes);
                if (orderTypes > 0) {
                    items.add(new OfflineDataItem(
                            "Order Types",
                            orderTypes + " types cached",
                            orderTypes,
                            "order_types"
                    ));
                }

                // Get cached order statuses
                int orderStatuses = database.getOrderStatuses().size();
                Log.d(TAG, "Order statuses: " + orderStatuses);
                if (orderStatuses > 0) {
                    items.add(new OfflineDataItem(
                            "Order Statuses",
                            orderStatuses + " statuses cached",
                            orderStatuses,
                            "order_statuses"
                    ));
                }

                // 2. LOCALLY GENERATED DATA

                // Get all orders (synced + unsynced)
                int totalOrders = database.getAllOrdersCount();
                int unsyncedOrders = database.getUnsyncedOrderItems().size();
                Log.d(TAG, "Total orders: " + totalOrders + ", Unsynced: " + unsyncedOrders);
                if (totalOrders > 0) {
                    String description = totalOrders + " orders total";
                    if (unsyncedOrders > 0) {
                        description += " (" + unsyncedOrders + " unsynced)";
                    }
                    items.add(new OfflineDataItem(
                            "Orders",
                            description,
                            totalOrders,
                            "orders"
                    ));
                }

                // Get all order items
                int totalOrderItems = database.getAllOrderItemsCount();
                Log.d(TAG, "Order items: " + totalOrderItems);
                if (totalOrderItems > 0) {
                    items.add(new OfflineDataItem(
                            "Order Items",
                            totalOrderItems + " order items stored",
                            totalOrderItems,
                            "order_items"
                    ));
                }

                // Get variants (usually generated with menu items)
                int variants = database.getAllVariantsCount();
                Log.d(TAG, "Variants: " + variants);
                if (variants > 0) {
                    items.add(new OfflineDataItem(
                            "Product Variants",
                            variants + " variants cached",
                            variants,
                            "variants"
                    ));
                }

                // Get cashier sessions (if table exists)
                int cashierSessions = database.getCashierSessionsCount();
                Log.d(TAG, "Cashier sessions: " + cashierSessions);
                if (cashierSessions > 0) {
                    items.add(new OfflineDataItem(
                            "Cashier Sessions",
                            cashierSessions + " sessions stored",
                            cashierSessions,
                            "sessions"
                    ));
                }

                Log.d(TAG, "Total items to display: " + items.size());

                runOnUiThread(() -> {
                    offlineDataItems.clear();
                    offlineDataItems.addAll(items);
                    adapter.notifyDataSetChanged();

                    updateSummary();
                    progressBar.setVisibility(View.GONE);

                    // Show message based on what we found
                    if (items.isEmpty()) {
                        Toast.makeText(this, "No offline data found. Use the app to generate some data first.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Loaded " + items.size() + " data tables", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading offline data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading offline data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void updateSummary() {
        int totalItems = 0;
        int unsyncedCount = 0;

        for (OfflineDataItem item : offlineDataItems) {
            totalItems += item.getCount();
            if ("orders".equals(item.getType())) {
                unsyncedCount = item.getCount();
            }
        }

        tvTotalItems.setText("Total Items: " + totalItems);

        if (unsyncedCount > 0) {
            tvLastSync.setText("Unsynced Items: " + unsyncedCount);
            tvLastSync.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            tvLastSync.setText("All items synced");
            tvLastSync.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }

        // Update button states
        btnSync.setEnabled(unsyncedCount > 0 && NetworkUtils.isNetworkAvailable(this));
        btnDeleteAll.setEnabled(totalItems > 0);
    }

    private void updateNetworkStatus() {
        boolean isConnected = NetworkUtils.isNetworkAvailable(this);
        if (isConnected) {
            tvNetworkStatus.setText("Network: Connected");
            tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvNetworkStatus.setText("Network: Disconnected");
            tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        // Update sync button based on network status
        updateSummary();
    }

    private void onItemClick(OfflineDataItem item) {
        // Show detailed preview of the selected data type
        showDataPreviewDialog(item);
    }

    private void showDataPreviewDialog(OfflineDataItem item) {
        // Create a dialog to show data preview
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle() + " - Data Preview");

        // Show loading while fetching data
        ProgressBar progressBar = new ProgressBar(this);
        builder.setView(progressBar);
        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();

        // Fetch data on background thread
        executor.execute(() -> {
            try {
                String dataPreview = getDataPreview(item.getType(), item.getCount());

                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    showDataPreview(item, dataPreview);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error getting data preview", e);
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(this, "Error loading data preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDataPreview(OfflineDataItem item, String dataPreview) {
        // Create dialog with scrollable text view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle() + " (" + item.getCount() + " items)");

        // Create scrollable text view
        TextView textView = new TextView(this);
        textView.setText(dataPreview);
        textView.setPadding(20, 20, 20, 20);
        textView.setTextSize(12);
        textView.setTypeface(android.graphics.Typeface.MONOSPACE); // Use monospace for better formatting

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(textView);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800 // Max height
        ));

        builder.setView(scrollView);

        // Add action buttons
        builder.setPositiveButton("Close", null);

        if ("orders".equals(item.getType())) {
            builder.setNeutralButton("Sync Now", (dialog, which) -> {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    syncData();
                } else {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.setNegativeButton("Export", (dialog, which) -> {
            // TODO: Implement export functionality
            Toast.makeText(this, "Export feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private String getDataPreview(String type, int count) {
        StringBuilder preview = new StringBuilder();

        try {
            switch (type) {
                case "categories":
                    preview.append(getMenuCategoriesPreview());
                    break;
                case "menu_items":
                    preview.append(getMenuItemsPreview());
                    break;
                case "promos":
                    preview.append(getPromosPreview());
                    break;
                case "order_types":
                    preview.append(getOrderTypesPreview());
                    break;
                case "order_statuses":
                    preview.append(getOrderStatusesPreview());
                    break;
                case "orders":
                    preview.append(getOrdersPreview());
                    break;
                case "order_items":
                    preview.append(getOrderItemsPreview());
                    break;
                case "variants":
                    preview.append(getVariantsPreview());
                    break;
                case "sessions":
                    preview.append(getCashierSessionsPreview());
                    break;
                default:
                    preview.append("No preview available for this data type.");
                    break;
            }
        } catch (Exception e) {
            preview.append("Error loading preview: ").append(e.getMessage());
        }

        return preview.toString();
    }

    private String getMenuCategoriesPreview() {
        List<MenuCategory> categories = database.getMenuCategories();
        StringBuilder sb = new StringBuilder();

        sb.append("MENU CATEGORIES\n");
        sb.append("================\n\n");

        for (int i = 0; i < Math.min(categories.size(), 10); i++) {
            MenuCategory category = categories.get(i);
            sb.append(String.format("ID: %d\n", category.getId()));
            sb.append(String.format("Name: %s\n", category.getName()));
            sb.append(String.format("Description: %s\n", category.getDescription()));
            sb.append(String.format("Displayed: %s\n", category.isDisplayed() ? "Yes" : "No"));
            sb.append(String.format("Highlight: %s\n", category.isHighlight() ? "Yes" : "No"));
            sb.append("---\n");
        }

        if (categories.size() > 10) {
            sb.append(String.format("\n... and %d more categories", categories.size() - 10));
        }

        return sb.toString();
    }

    private String getMenuItemsPreview() {
        List<ProductItem> items = database.getMenuItems();
        StringBuilder sb = new StringBuilder();

        sb.append("MENU ITEMS\n");
        sb.append("==========\n\n");

        for (int i = 0; i < Math.min(items.size(), 10); i++) {
            ProductItem item = items.get(i);
            sb.append(String.format("ID: %d\n", item.getId()));
            sb.append(String.format("Name: %s\n", item.getName()));
            sb.append(String.format("Price: $%.2f\n", item.getPrice()));
            sb.append(String.format("Category: %s\n", item.getCategory()));
            sb.append(String.format("Active: %s\n", item.isActive() ? "Yes" : "No"));
            if (item.getVariants() != null && !item.getVariants().isEmpty()) {
                sb.append(String.format("Variants: %d\n", item.getVariants().size()));
            }
            sb.append("---\n");
        }

        if (items.size() > 10) {
            sb.append(String.format("\n... and %d more items", items.size() - 10));
        }

        return sb.toString();
    }

    private String getPromosPreview() {
        List<Promo> promos = database.getPromos();
        StringBuilder sb = new StringBuilder();

        sb.append("PROMOTIONS\n");
        sb.append("==========\n\n");

        for (int i = 0; i < Math.min(promos.size(), 10); i++) {
            Promo promo = promos.get(i);
            sb.append(String.format("ID: %d\n", promo.getPromoId()));
            sb.append(String.format("Name: %s\n", promo.getPromoName()));
            sb.append(String.format("Description: %s\n", promo.getPromoDescription()));
            sb.append(String.format("Type: %s\n", promo.getType()));
            sb.append(String.format("Discount: %s %s\n", promo.getDiscountAmount(), promo.getDiscountType()));
            sb.append(String.format("Active: %s\n", promo.isActive() ? "Yes" : "No"));
            sb.append(String.format("Start: %s\n", promo.getStartDate()));
            sb.append(String.format("End: %s\n", promo.getEndDate()));
            sb.append("---\n");
        }

        if (promos.size() > 10) {
            sb.append(String.format("\n... and %d more promos", promos.size() - 10));
        }

        return sb.toString();
    }

    private String getOrderTypesPreview() {
        List<OrderType> orderTypes = database.getOrderTypes();
        StringBuilder sb = new StringBuilder();

        sb.append("ORDER TYPES\n");
        sb.append("===========\n\n");

        for (OrderType orderType : orderTypes) {
            sb.append(String.format("ID: %d\n", orderType.getId()));
            sb.append(String.format("Name: %s\n", orderType.getName()));
            sb.append("---\n");
        }

        return sb.toString();
    }

    private String getOrderStatusesPreview() {
        List<OrderStatus> orderStatuses = database.getOrderStatuses();
        StringBuilder sb = new StringBuilder();

        sb.append("ORDER STATUSES\n");
        sb.append("==============\n\n");

        for (OrderStatus orderStatus : orderStatuses) {
            sb.append(String.format("ID: %d\n", orderStatus.getId()));
            sb.append(String.format("Name: %s\n", orderStatus.getName()));
            sb.append("---\n");
        }

        return sb.toString();
    }

    private String getOrdersPreview() {
        // Since we don't have Order objects, we'll query the database directly
        StringBuilder sb = new StringBuilder();

        sb.append("ORDERS\n");
        sb.append("======\n\n");

        android.database.Cursor cursor = null;
        try {
            android.database.sqlite.SQLiteDatabase db = database.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM orders ORDER BY order_created_at DESC LIMIT 10", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sb.append(String.format("Order ID: %d\n", cursor.getLong(cursor.getColumnIndexOrThrow("order_id"))));
                    sb.append(String.format("Session ID: %d\n", cursor.getLong(cursor.getColumnIndexOrThrow("session_id"))));
                    sb.append(String.format("Table: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("table_number"))));
                    sb.append(String.format("Customer: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("customer_name"))));
                    sb.append(String.format("Synced: %s\n", cursor.getInt(cursor.getColumnIndexOrThrow("is_synced")) == 1 ? "Yes" : "No"));
                    sb.append(String.format("Created: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("order_created_at"))));
                    sb.append("---\n");
                } while (cursor.moveToNext());
            } else {
                sb.append("No orders found.");
            }
        } catch (Exception e) {
            sb.append("Error loading orders: ").append(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return sb.toString();
    }

    private String getOrderItemsPreview() {
        StringBuilder sb = new StringBuilder();

        sb.append("ORDER ITEMS\n");
        sb.append("===========\n\n");

        android.database.Cursor cursor = null;
        try {
            android.database.sqlite.SQLiteDatabase db = database.getReadableDatabase();
            String query = "SELECT oi.*, mi.name as menu_item_name FROM order_items oi " +
                    "LEFT JOIN menu_items mi ON oi.menu_item_id = mi.id " +
                    "ORDER BY oi.item_created_at DESC LIMIT 10";
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sb.append(String.format("Item ID: %d\n", cursor.getLong(cursor.getColumnIndexOrThrow("item_id"))));
                    sb.append(String.format("Order ID: %d\n", cursor.getLong(cursor.getColumnIndexOrThrow("order_id"))));
                    sb.append(String.format("Item: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("menu_item_name"))));
                    sb.append(String.format("Quantity: %d\n", cursor.getInt(cursor.getColumnIndexOrThrow("quantity"))));
                    sb.append(String.format("Unit Price: $%.2f\n", cursor.getDouble(cursor.getColumnIndexOrThrow("unit_price"))));
                    sb.append(String.format("Total: $%.2f\n", cursor.getDouble(cursor.getColumnIndexOrThrow("total_price"))));
                    sb.append(String.format("Synced: %s\n", cursor.getInt(cursor.getColumnIndexOrThrow("is_synced")) == 1 ? "Yes" : "No"));
                    sb.append("---\n");
                } while (cursor.moveToNext());
            } else {
                sb.append("No order items found.");
            }
        } catch (Exception e) {
            sb.append("Error loading order items: ").append(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return sb.toString();
    }

    private String getVariantsPreview() {
        StringBuilder sb = new StringBuilder();

        sb.append("PRODUCT VARIANTS\n");
        sb.append("================\n\n");

        android.database.Cursor cursor = null;
        try {
            android.database.sqlite.SQLiteDatabase db = database.getReadableDatabase();
            String query = "SELECT v.*, mi.name as menu_item_name FROM variants v " +
                    "LEFT JOIN menu_items mi ON v.menu_item_id = mi.id " +
                    "ORDER BY v.variant_name ASC LIMIT 10";
            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sb.append(String.format("Variant ID: %d\n", cursor.getLong(cursor.getColumnIndexOrThrow("variant_id"))));
                    sb.append(String.format("Name: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("variant_name"))));
                    sb.append(String.format("Menu Item: %s\n", cursor.getString(cursor.getColumnIndexOrThrow("menu_item_name"))));
                    sb.append(String.format("Price: $%.2f\n", cursor.getDouble(cursor.getColumnIndexOrThrow("variant_price"))));
                    sb.append(String.format("Active: %s\n", cursor.getInt(cursor.getColumnIndexOrThrow("variant_is_active")) == 1 ? "Yes" : "No"));
                    sb.append("---\n");
                } while (cursor.moveToNext());
            } else {
                sb.append("No variants found.");
            }
        } catch (Exception e) {
            sb.append("Error loading variants: ").append(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return sb.toString();
    }

    private String getCashierSessionsPreview() {
        StringBuilder sb = new StringBuilder();

        sb.append("CASHIER SESSIONS\n");
        sb.append("================\n\n");

        android.database.Cursor cursor = null;
        try {
            android.database.sqlite.SQLiteDatabase db = database.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM cashier_sessions ORDER BY created_at DESC LIMIT 10", null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    // Add session data based on your actual table structure
                    sb.append("Session found\n");
                    sb.append("---\n");
                } while (cursor.moveToNext());
            } else {
                sb.append("No cashier sessions found.");
            }
        } catch (Exception e) {
            sb.append("Cashier sessions table not available or error: ").append(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return sb.toString();
    }

    private void showSyncConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sync Offline Data")
                .setMessage("This will sync all unsynced orders to the server. Continue?")
                .setPositiveButton("Sync", (dialog, which) -> syncData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAllConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Offline Data")
                .setMessage("This will permanently delete all cached data including menu items, categories, and unsynced orders. This action cannot be undone. Continue?")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllOfflineData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void syncData() {
        progressBar.setVisibility(View.VISIBLE);
        btnSync.setEnabled(false);

        Toast.makeText(this, "Syncing data...", Toast.LENGTH_SHORT).show();

        // Use the app's sync functionality
        app.forceSyncNow();

        // Check sync status after a delay
        new android.os.Handler().postDelayed(() -> {
            loadOfflineData();
            Toast.makeText(this, "Sync initiated", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void deleteAllOfflineData() {
        progressBar.setVisibility(View.VISIBLE);
        btnDeleteAll.setEnabled(false);

        executor.execute(() -> {
            try {
                // Clear all cached data
                database.clearAllCachedData();

                runOnUiThread(() -> {
                    Toast.makeText(this, "All offline data deleted", Toast.LENGTH_SHORT).show();
                    loadOfflineData();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting offline data", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error deleting offline data", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnDeleteAll.setEnabled(true);
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
        loadOfflineData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }
}