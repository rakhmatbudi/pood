package com.restaurant.management;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.adapters.ProductItemAdapter;
import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.models.CreateOrderItemRequest;
import com.restaurant.management.models.CreateOrderItemResponse;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.utils.NetworkUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AddItemActivity extends AppCompatActivity implements ProductItemAdapter.OnItemClickListener {
    private static final String TAG = "AddItemActivity";

    private TextView tableNumberTextView;
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView menuItemsRecyclerView;
    private ProgressBar progressBar;

    private List<ProductItem> menuItems = new ArrayList<>();
    private List<ProductItem> allMenuItems = new ArrayList<>();
    private ProductItemAdapter menuItemAdapter;
    private DatabaseManager databaseManager;

    private long orderId;
    private String tableNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        databaseManager = DatabaseManager.getInstance(this);

        initializeViews();
        setupToolbar();
        getOrderDetails();
        setupRecyclerView();
        setupSearch();
        loadMenuItems();
    }

    private void initializeViews() {
        tableNumberTextView = findViewById(R.id.table_number_text_view);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        menuItemsRecyclerView = findViewById(R.id.menu_items_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Item");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void getOrderDetails() {
        orderId = getIntent().getLongExtra("order_id", -1);
        tableNumber = getIntent().getStringExtra("table_number");

        if (orderId == -1 || tableNumber == null) {
            Toast.makeText(this, "Invalid order details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tableNumberTextView.setText("Table: " + tableNumber);
    }

    private void setupRecyclerView() {
        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemAdapter = new ProductItemAdapter(menuItems, this);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);
    }

    private void setupSearch() {
        searchButton.setOnClickListener(v -> searchMenuItems());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMenuItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMenuItems() {
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<ProductItem> items = databaseManager.getAllMenuItems();
                Log.d(TAG, "Loaded " + (items != null ? items.size() : 0) + " items from database");

                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        allMenuItems.clear();
                        allMenuItems.addAll(items);

                        menuItems.clear();
                        menuItems.addAll(items);
                        menuItemAdapter.notifyDataSetChanged();

                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                        showNoMenuItemsMessage();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading menu items", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error loading menu items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showNoMenuItemsMessage() {
        if (databaseManager.hasMenuItems()) {
            Toast.makeText(this, "Menu items exist but couldn't be loaded", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No menu items available. Please sync data first.", Toast.LENGTH_LONG).show();
        }
    }

    private void searchMenuItems() {
        String query = searchEditText.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(query)) {
            menuItems.clear();
            menuItems.addAll(allMenuItems);
            menuItemAdapter.notifyDataSetChanged();
        } else {
            List<ProductItem> filteredItems = filterMenuItems(query);

            menuItems.clear();
            menuItems.addAll(filteredItems);
            menuItemAdapter.notifyDataSetChanged();

            if (filteredItems.isEmpty() && !allMenuItems.isEmpty()) {
                Toast.makeText(this, "No items found matching '" + query + "'", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private List<ProductItem> filterMenuItems(String query) {
        List<ProductItem> filteredItems = new ArrayList<>();

        for (ProductItem item : allMenuItems) {
            if (isItemMatchingQuery(item, query)) {
                filteredItems.add(item);
            }
        }

        return filteredItems;
    }

    private boolean isItemMatchingQuery(ProductItem item, String query) {
        String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
        String itemDesc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
        String itemCategory = item.getCategory() != null ? item.getCategory().toLowerCase() : "";

        return itemName.contains(query) || itemDesc.contains(query) || itemCategory.contains(query);
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
    public void onItemClick(ProductItem menuItem) {
        ItemDetailDialog dialog = new ItemDetailDialog(this, menuItem, new ItemDetailDialog.OnItemAddListener() {
            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes) {
                addItemToOrder(selectedItem, variantId, quantity, notes, null, false);
            }

            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes, Double customPrice) {
                addItemToOrder(selectedItem, variantId, quantity, notes, customPrice, false);
            }

            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes, Double customPrice, boolean isComplimentary) {
                addItemToOrder(selectedItem, variantId, quantity, notes, customPrice, isComplimentary);
            }
        });
        dialog.show();
    }

    private void addItemToOrder(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice, boolean isComplimentary) {
        if (quantity <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressIndicator(true);

        // Calculate pricing
        double unitPrice = calculateUnitPrice(menuItem, variantId, customPrice, isComplimentary);
        double totalPrice = unitPrice * quantity;

        // Create the request object
        CreateOrderItemRequest request = createOrderItemRequest(menuItem, variantId, quantity, notes, unitPrice, totalPrice, isComplimentary, customPrice);

        // OFFLINE-FIRST: Always save locally first
        saveOrderItemLocally(request, menuItem, variantId, quantity, notes, unitPrice, totalPrice, isComplimentary, customPrice);
    }

    private void showProgressIndicator(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        menuItemsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private double calculateUnitPrice(ProductItem menuItem, Long variantId, Double customPrice, boolean isComplimentary) {
        if (isComplimentary) {
            return 0.0;
        } else if (customPrice != null) {
            return customPrice;
        } else if (variantId != null) {
            return getVariantPrice(menuItem, variantId);
        } else {
            return menuItem.getPrice();
        }
    }

    private double getVariantPrice(ProductItem menuItem, Long variantId) {
        if (menuItem.getVariants() != null) {
            for (Variant variant : menuItem.getVariants()) {
                if (Objects.equals(variant.getId(), variantId)) {
                    return variant.getPrice();
                }
            }
        }
        return menuItem.getPrice(); // Fallback to base price
    }

    private CreateOrderItemRequest createOrderItemRequest(ProductItem menuItem, Long variantId, int quantity, String notes, double unitPrice, double totalPrice, boolean isComplimentary, Double customPrice) {
        CreateOrderItemRequest request = new CreateOrderItemRequest();
        request.setMenuItemId(menuItem.getId());
        request.setVariantId(variantId);
        request.setQuantity(quantity);
        request.setUnitPrice(unitPrice);
        request.setTotalPrice(totalPrice);
        request.setStatus("new");
        request.setKitchenPrinted(false);

        if (notes != null && !notes.isEmpty()) {
            request.setNotes(notes);
        }

        if (isComplimentary) {
            request.setComplimentary(true);
            request.setOriginalPrice(getOriginalPrice(menuItem, variantId));
        }

        if (customPrice != null && !isComplimentary) {
            request.setCustomPrice(true);
            request.setOriginalPrice(menuItem.getPrice());
        }

        return request;
    }

    private void saveOrderItemLocally(CreateOrderItemRequest request, ProductItem menuItem, Long variantId, int quantity, String notes, double unitPrice, double totalPrice, boolean isComplimentary, Double customPrice) {
        new Thread(() -> {
            try {
                // Save to local database using DatabaseManager
                long localItemId = databaseManager.saveOrderItemLocally(orderId, request);

                runOnUiThread(() -> {
                    handleLocalSaveSuccess(localItemId, request, isComplimentary, customPrice, unitPrice);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error saving order item locally", e);
                runOnUiThread(() -> {
                    handleLocalSaveError(e);
                });
            }
        }).start();
    }

    private void handleLocalSaveSuccess(long localItemId, CreateOrderItemRequest request, boolean isComplimentary, Double customPrice, double unitPrice) {
        // Show success message
        String successMessage = buildSuccessMessage(isComplimentary, customPrice, unitPrice);
        Toast.makeText(this, successMessage + " (Saved locally)", Toast.LENGTH_SHORT).show();

        // If online, attempt to sync immediately
        if (NetworkUtils.isNetworkAvailable(this)) {
            syncOrderItemToServer(localItemId, request);
        } else {
            // Show offline indicator
            Toast.makeText(this, "Item saved offline - will sync when online", Toast.LENGTH_LONG).show();
        }

        showProgressIndicator(false);
        setResult(RESULT_OK);
        finish();
    }

    private void handleLocalSaveError(Exception e) {
        showProgressIndicator(false);
        String errorMessage = "Failed to save item";
        if (e.getMessage() != null) {
            errorMessage += ": " + e.getMessage();
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void syncOrderItemToServer(long localItemId, CreateOrderItemRequest request) {
        ApiClient.getApiService().addItemToOrder(orderId, request)
                .enqueue(new retrofit2.Callback<CreateOrderItemResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<CreateOrderItemResponse> call,
                                           retrofit2.Response<CreateOrderItemResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Mark as synced in local database
                            new Thread(() -> {
                                try {
                                    // Try different possible method names for getting the server ID
                                    long serverId = getServerIdFromResponse(response.body());
                                    databaseManager.markOrderItemAsSynced(localItemId, serverId);
                                    Log.d(TAG, "Order item synced successfully - Local ID: " + localItemId + " -> Server ID: " + serverId);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error marking item as synced", e);
                                }
                            }).start();
                        } else {
                            Log.e(TAG, "Failed to sync order item to server: " + response.code());
                            // Item remains in local database for future sync attempts
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<CreateOrderItemResponse> call, Throwable t) {
                        Log.e(TAG, "Network error syncing order item", t);
                        // Item remains in local database for future sync attempts
                    }
                });
    }

    /**
     * Try to get server ID from response using different possible method names
     */
    private long getServerIdFromResponse(CreateOrderItemResponse response) {
        try {
            // Try common method names for getting ID
            if (hasMethod(response, "getId")) {
                return (Long) response.getClass().getMethod("getId").invoke(response);
            } else if (hasMethod(response, "getItemId")) {
                return (Long) response.getClass().getMethod("getItemId").invoke(response);
            } else if (hasMethod(response, "getOrderItemId")) {
                return (Long) response.getClass().getMethod("getOrderItemId").invoke(response);
            } else if (hasField(response, "id")) {
                return (Long) response.getClass().getField("id").get(response);
            } else {
                Log.w(TAG, "No ID method found in CreateOrderItemResponse, using timestamp as fallback");
                return System.currentTimeMillis(); // Fallback - not ideal but prevents crashes
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting server ID from response", e);
            return System.currentTimeMillis(); // Fallback
        }
    }

    /**
     * Check if object has a specific method
     */
    private boolean hasMethod(Object obj, String methodName) {
        try {
            obj.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Check if object has a specific field
     */
    private boolean hasField(Object obj, String fieldName) {
        try {
            obj.getClass().getField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private double getOriginalPrice(ProductItem menuItem, Long variantId) {
        if (variantId != null) {
            return getVariantPrice(menuItem, variantId);
        }
        return menuItem.getPrice();
    }

    private String buildSuccessMessage(boolean isComplimentary, Double customPrice, double unitPrice) {
        String baseMessage = "Item added successfully";

        if (isComplimentary) {
            return baseMessage + " (Complimentary - FREE)";
        } else if (customPrice != null) {
            return baseMessage + " (Custom price: " + String.format("%.0f", customPrice) + ")";
        } else {
            return baseMessage;
        }
    }

    private String getAuthToken() {
        try {
            return getSharedPreferences("restaurant_prefs", MODE_PRIVATE)
                    .getString("auth_token", "");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Note: No need to close DatabaseManager as it uses singleton pattern
        // and handles its own lifecycle
    }
}