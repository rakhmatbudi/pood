package com.restaurant.management;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.restaurant.management.models.OfflineDataItem;
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
                int totalOrders = database.getAllOrders().size();
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
                int totalOrderItems = database.getAllOrderItems().size();
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
                int variants = database.getAllVariants().size();
                Log.d(TAG, "Variants: " + variants);
                if (variants > 0) {
                    items.add(new OfflineDataItem(
                            "Product Variants",
                            variants + " variants cached",
                            variants,
                            "variants"
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
        // Show detailed information about the selected item
        showItemDetailsDialog(item);
    }

    private void showItemDetailsDialog(OfflineDataItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getTitle());
        builder.setMessage(item.getDescription() + "\n\nType: " + item.getType().toUpperCase());
        builder.setPositiveButton("OK", null);

        if ("orders".equals(item.getType())) {
            builder.setNeutralButton("Sync Now", (dialog, which) -> {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    syncData();
                } else {
                    Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            });
        }

        builder.show();
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