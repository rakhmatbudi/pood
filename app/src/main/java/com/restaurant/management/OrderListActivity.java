package com.restaurant.management;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.restaurant.management.adapters.OrderAdapter;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.util.concurrent.TimeUnit;
import java.util.Map;
import androidx.appcompat.app.AlertDialog;
import android.os.Handler;

public class OrderListActivity extends AppCompatActivity {
    private static final String TAG = "OrderActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/open/sessions/";

    private RecyclerView recyclerView;
    private OrderAdapter orderAdapter;
    private ProgressBar progressBar;
    private TextView noOrdersTextView;
    private EditText searchEditText;
    private Spinner filterSpinner;
    private FloatingActionButton fabAddOrder; // This is the field declaration
    private SwipeRefreshLayout swipeRefreshLayout;

    private List<Order> allOrdersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();
    private long currentSessionId = -1;

    private long sessionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.orders));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize UI components
        recyclerView = findViewById(R.id.order_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        noOrdersTextView = findViewById(R.id.no_orders_text_view);
        searchEditText = findViewById(R.id.search_edit_text);
        filterSpinner = findViewById(R.id.filter_spinner);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fabAddOrder = findViewById(R.id.fab_add_order); // Initialize the FAB

        // Get current session ID - either from intent or from active session check
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("session_id")) {
            currentSessionId = extras.getLong("session_id");
            Log.d(TAG, "Got session ID directly from intent: " + currentSessionId);
        } else {
            // Get from shared preferences if not passed in intent
            Log.d(TAG, "No session_id in intent extras, checking SharedPreferences...");
            checkForActiveSession();
        }

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderAdapter = new OrderAdapter(filteredOrdersList);
        orderAdapter.setOnOrderClickListener((order, position) -> {
            // Navigate to OrderActivity with just the IDs
            Intent intent = new Intent(OrderListActivity.this, OrderActivity.class);
            intent.putExtra("order_id", order.getId());
            intent.putExtra("session_id", currentSessionId);
            startActivity(intent);
        });
        recyclerView.setAdapter(orderAdapter);

        // Set up filter spinner
        setupFilterSpinner();

        // Set up search functionality
        setupSearch();

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::fetchOrders);

        // Set up FAB click listener
        fabAddOrder.setOnClickListener(v -> navigateToCreateNewOrder());

        // Fetch initial data
        fetchOrders();
    }


    private void checkForActiveSession() {
        Log.d(TAG, "Checking for active session...");

        // Check for active session ID in SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);

        // Log ALL shared preferences for debugging
        Log.d(TAG, "All SharedPreferences in " + getString(R.string.pref_file_name) + ":");
        Map<String, ?> allPrefs = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            Log.d(TAG, entry.getKey() + ": " + entry.getValue());
        }

        // Get the active session ID
        String sessionIdKey = getString(R.string.pref_active_session_id);
        currentSessionId = sharedPreferences.getLong(sessionIdKey, -1);
        Log.d(TAG, "Retrieved session ID: " + currentSessionId + " using key: '" + sessionIdKey + "'");

        if (currentSessionId == -1) {
            // No active session found, show error message
            Log.e(TAG, "No active session ID found in SharedPreferences");
            Toast.makeText(this, getString(R.string.no_active_session_found), Toast.LENGTH_LONG).show();
            finish(); // Close activity and go back
        } else {
            Log.d(TAG, "Active session found with ID: " + currentSessionId);
        }
    }

    private void setupFilterSpinner() {
        // Create an ArrayAdapter using string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.order_filter_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        filterSpinner.setAdapter(adapter);

        // Set listener to handle selection changes
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterOrders();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter orders when text changes
                filterOrders();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
    }

    private void fetchOrders() {
        // Log the current session ID
        Log.d(TAG, "Fetching orders for session ID: " + currentSessionId);

        if (currentSessionId == -1) {
            // No session ID available
            Log.e(TAG, "No session ID available, showing error");
            showErrorState(getString(R.string.no_session_id));
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noOrdersTextView.setVisibility(View.GONE);

        // Build the URL with the session ID
        String apiUrl = BASE_API_URL + currentSessionId;
        Log.d(TAG, "API URL: " + apiUrl);

        // Get the auth token
        String authToken = getAuthToken();
        Log.d(TAG, "Auth token available: " + (authToken != null && !authToken.isEmpty()));

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        Log.d(TAG, "Executing API request...");
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    showErrorState(getString(R.string.network_error));
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Received API response, status code: " + response.code());
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response body: " + responseBody);

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    processOrdersResponse(jsonResponse);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        showErrorState(getString(R.string.error_processing_response, e.getMessage()));
                    });
                }
            }
        });
    }

    private void showCreateOrderDialog() {
        // Create a dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_new_order);

        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_order, null);
        builder.setView(dialogView);

        // Initialize dialog components
        final EditText tableNumberEditText = dialogView.findViewById(R.id.table_number_edit_text);
        final EditText customerNameEditText = dialogView.findViewById(R.id.customer_name_edit_text);

        // Add buttons
        builder.setPositiveButton(R.string.create, null); // Set listener later to prevent dialog from dismissing on validation error
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Set positive button click listener (this way we can prevent dialog from closing on validation errors)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validate inputs
            String tableNumber = tableNumberEditText.getText().toString().trim();
            if (tableNumber.isEmpty()) {
                tableNumberEditText.setError(getString(R.string.table_number_required));
                return; // Don't dismiss dialog
            }

            // Get customer name (optional)
            String customerName = customerNameEditText.getText().toString().trim();

            // Call method to create order
            createNewOrder(tableNumber, customerName);

            // Dismiss the dialog
            dialog.dismiss();
        });
    }

    private void navigateToCreateNewOrder() {
        // Show the Create Order Dialog instead of starting a new activity
        showCreateOrderDialog();
    }

    private long getCurrentServerId() {
        // Option 1: Get from SharedPreferences
        long serverId = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getLong("current_server_id", -1);

        // Option 2: Use a default value if not found in preferences
        if (serverId == -1) {
            serverId = 2; // Default server ID
        }

        return serverId;
    }

    private long getCustomerIdFromName(String customerName) {
        // Just return a hardcoded value for now
        return 2; // Use whatever customer ID is appropriate for your app
    }

    private void createNewOrder(String tableNumber, String customerName) {
        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);

        try {
            // Create JSON request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("table_number", tableNumber);
            requestBody.put("server_id", 2); // Hardcoded for simplicity, consider fetching this dynamically too

            // Use the current session ID instead of hardcoding
            requestBody.put("cashier_session_id", currentSessionId);

            // Only add customer_id if customer name is provided
            if (!customerName.isEmpty()) {
                requestBody.put("customer_id", 2); // Hardcoded for simplicity
            }
            // Note: If customerName is empty, we don't add the customer_id field at all

            // Log the request payload
            String requestPayload = requestBody.toString();
            Log.d("OrderListActivity", "Request payload: " + requestPayload);

            // Create request
            String apiUrl = "https://api.pood.lol/orders"; // Try without trailing slash
            Log.d("OrderListActivity", "Calling API URL: " + apiUrl);

            // Get and log auth token
            String authToken = getAuthToken();
            Log.d("OrderListActivity", "Auth token length: " + authToken.length());

            // Create MediaType object
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            // Create RequestBody
            RequestBody body = RequestBody.create(JSON, requestPayload);

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .build();

            // Configure timeouts if needed
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            // Execute request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("OrderListActivity", "Network failure", e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrderListActivity.this,
                                "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            Log.d("OrderListActivity", "Order created successfully");
                            Log.d("OrderListActivity", "Response: " + responseBody);

                            Toast.makeText(OrderListActivity.this,
                                    "Order created successfully", Toast.LENGTH_SHORT).show();
                            refreshOrdersList();
                        } else {
                            Log.e("OrderListActivity", "Error creating order. Status code: " + response.code());
                            Log.e("OrderListActivity", "Response body: " + responseBody);

                            String errorMsg = "Error creating order (Code: " + response.code() + ")";
                            Toast.makeText(OrderListActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e("OrderListActivity", "JSON exception", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error creating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the list of orders from the API.
     * This method refreshes the order list displayed in the RecyclerView.
     */
    private void loadOrders() {
        // Show a toast for now - replace with actual implementation later
        Toast.makeText(this, "Refreshing orders list...", Toast.LENGTH_SHORT).show();

        // If you're using this method after creating an order successfully,
        // you can simply call a method to refresh your UI with existing code
        refreshOrdersList();
    }

    /**
     * Refresh the orders list in the UI.
     * This method should call your existing code to load and display orders.
     */
    private void refreshOrdersList() {
        // Call fetchOrders to actually refresh the data from the API
        fetchOrders();
    }

    private void processOrdersResponse(JSONObject jsonResponse) throws JSONException {
        allOrdersList.clear();

        // Check status
        String status = jsonResponse.optString("status", "");
        if (!"success".equals(status)) {
            Log.e(TAG, "API returned non-success status: " + status);
            runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                showErrorState(getString(R.string.unexpected_response_code) + status);
            });
            return;
        }

        // Check for orders array
        if (jsonResponse.has("data") && !jsonResponse.isNull("data") &&
                jsonResponse.get("data") instanceof JSONArray) {

            JSONArray ordersArray = jsonResponse.getJSONArray("data");
            int count = jsonResponse.optInt("count", 0);
            Log.d(TAG, "Found " + count + " orders in response");

            // Parse each order
            for (int i = 0; i < ordersArray.length(); i++) {
                JSONObject orderJson = ordersArray.getJSONObject(i);
                Order order = parseOrder(orderJson);
                allOrdersList.add(order);
            }
        }

        // Update UI
        runOnUiThread(() -> {
            swipeRefreshLayout.setRefreshing(false);

            if (allOrdersList.isEmpty()) {
                showEmptyState();
            } else {
                filterOrders();
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                noOrdersTextView.setVisibility(View.GONE);
            }
        });
    }

    private Order parseOrder(JSONObject orderJson) throws JSONException {
        Order order = new Order();

        // Extract order data from the API format
        order.setId(orderJson.optLong("id", -1));
        order.setTableNumber(orderJson.optString("table_number", ""));
        order.setOrderNumber(String.valueOf(order.getId()));

        // Parse total amount
        String totalAmountStr = orderJson.optString("total_amount", "0.0").replace(",", "");
        try {
            order.setTotal(Double.parseDouble(totalAmountStr));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing total: " + totalAmountStr, e);
            order.setTotal(0.0);
        }

        // Map status values
        String apiStatus = orderJson.optString("status", "").toLowerCase();
        order.setStatus("open".equals(apiStatus) ? "pending" : apiStatus);
        order.setOpen(orderJson.optBoolean("is_open", false));
        order.setCreatedAt(orderJson.optString("created_at", ""));

        // Customer information handling - only set a value if customer_name or customer_id is not null
        if (!orderJson.isNull("customer_name")) {
            // Customer name exists in API response
            order.setCustomerName(orderJson.optString("customer_name", ""));
        } else if (!orderJson.isNull("customer_id")) {
            // Only customer_id exists
            long customerId = orderJson.optLong("customer_id", -1);
            if (customerId > 0) {
                order.setCustomerName("Customer #" + customerId);
            } else {
                order.setCustomerName(null);  // Explicitly set to null
            }
        } else {
            // Both customer_name and customer_id are null
            order.setCustomerName(null);  // Explicitly set to null
        }

        // Store IDs
        order.setCashierSessionId(orderJson.optLong("cashier_session_id", -1));
        order.setServerId(orderJson.optLong("server_id", -1));

        // Process order items
        List<String> displayItems = new ArrayList<>();

        // Extract order items
        List<OrderItem> orderItems = new ArrayList<>();
        if (orderJson.has("order_items") && !orderJson.isNull("order_items")) {
            JSONArray itemsArray = orderJson.getJSONArray("order_items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                OrderItem item = parseOrderItem(itemJson);
                orderItems.add(item);

                // Add to display items
                String itemDisplay = item.getQuantity() + "x " + item.getMenuItemName();

                // Only add notes if not null or empty
                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    itemDisplay += " (" + item.getNotes() + ")";
                }

                displayItems.add(itemDisplay);
            }
        }

        // Set items for display
        order.setItems(displayItems);

        // Store the order items in the order object
        order.setOrderItems(orderItems);

        return order;
    }

    private OrderItem parseOrderItem(JSONObject itemJson) {
        OrderItem item = new OrderItem();

        item.setId(itemJson.optLong("id", -1));
        item.setOrderId(itemJson.optLong("order_id", -1));
        item.setMenuItemId(itemJson.optLong("menu_item_id", -1));
        item.setMenuItemName(itemJson.optString("menu_item_name", ""));

        // Check if variant_id is null in the JSON
        if (!itemJson.isNull("variant_id")) {
            item.setVariantId(itemJson.optLong("variant_id", -1));
        } else {
            item.setVariantId(null); // Explicitly set to null if it's null in the JSON
        }

        item.setQuantity(itemJson.optInt("quantity", 0));
        item.setUnitPrice(itemJson.optDouble("unit_price", 0.0));
        item.setTotalPrice(itemJson.optDouble("total_price", 0.0));
        item.setNotes(itemJson.optString("notes", ""));
        item.setStatus(itemJson.optString("status", ""));
        item.setKitchenPrinted(itemJson.optBoolean("kitchen_printed", false));
        item.setCreatedAt(itemJson.optString("created_at", ""));
        item.setUpdatedAt(itemJson.optString("updated_at", ""));

        return item;
    }

    private void filterOrders() {
        filteredOrdersList.clear();

        String searchQuery = searchEditText.getText().toString().toLowerCase().trim();
        int filterPosition = filterSpinner.getSelectedItemPosition();

        for (Order order : allOrdersList) {
            boolean matchesSearch = searchQuery.isEmpty() ||
                    order.getOrderNumber().toLowerCase().contains(searchQuery) ||
                    order.getTableNumber().toLowerCase().contains(searchQuery) ||
                    order.getCustomerName().toLowerCase().contains(searchQuery);

            // Also search in order items (new feature)
            if (!matchesSearch && order.getItems() != null) {
                for (String item : order.getItems()) {
                    if (item.toLowerCase().contains(searchQuery)) {
                        matchesSearch = true;
                        break;
                    }
                }
            }

            boolean matchesFilter = true;

            // Apply filter based on selection
            switch (filterPosition) {
                case 0: // All orders
                    matchesFilter = true;
                    break;
                case 1: // Pending orders
                    matchesFilter = "pending".equalsIgnoreCase(order.getStatus());
                    break;
                case 2: // Processing orders
                    matchesFilter = "processing".equalsIgnoreCase(order.getStatus());
                    break;
                case 3: // Ready orders
                    matchesFilter = "ready".equalsIgnoreCase(order.getStatus());
                    break;
            }

            if (matchesSearch && matchesFilter) {
                filteredOrdersList.add(order);
            }
        }

        // Update adapter and UI
        orderAdapter.notifyDataSetChanged();

        if (filteredOrdersList.isEmpty() && !allOrdersList.isEmpty()) {
            // Has orders but none match filter
            noOrdersTextView.setText(getString(R.string.no_matching_orders));
            noOrdersTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else if (filteredOrdersList.isEmpty()) {
            // No orders at all
            showEmptyState();
        } else {
            // Has filtered orders
            noOrdersTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private String getAuthToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
        return sharedPreferences.getString(getString(R.string.pref_token), "");
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        noOrdersTextView.setText(getString(R.string.no_orders_found));
        noOrdersTextView.setVisibility(View.VISIBLE);
    }

    private void showErrorState(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        noOrdersTextView.setText(errorMessage);
        noOrdersTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}