package com.restaurant.management;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.restaurant.management.adapters.OrderAdapter;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;
import com.restaurant.management.models.OrderType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private static final String ORDER_TYPES_API_URL = "https://api.pood.lol/order-types/";

    private static final String TAG = "OrderListActivity";
    private static final String ORDERS_API_URL = "https://api.pood.lol/orders";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RecyclerView ordersRecyclerView;
    private OrderAdapter orderAdapter;
    private List<Order> ordersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private long sessionId = -1;
    private OkHttpClient client = new OkHttpClient();
    private List<OrderType> orderTypesList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.orders_list_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get session ID from intent
        sessionId = getIntent().getLongExtra("session_id", -1);
        if (sessionId == -1) {
            Toast.makeText(this, R.string.invalid_session, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        ordersRecyclerView = findViewById(R.id.orders_recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);
        searchEditText = findViewById(R.id.search_edit_text);

        // Set up RecyclerView
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create adapter with OnOrderClickListener (this) and context
        orderAdapter = new OrderAdapter(filteredOrdersList, this, this);
        ordersRecyclerView.setAdapter(orderAdapter);

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(this::fetchOrders);

        // Set up search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterOrders(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Set up FAB click listener (if you have one)
        View fabAddOrder = findViewById(R.id.fab_add_order);
        if (fabAddOrder != null) {
            fabAddOrder.setOnClickListener(v -> showNewOrderDialog());
        }

        // Fetch orders
        fetchOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh orders list when coming back to this activity
        fetchOrders();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchOrders() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        ordersRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        // Build the URL with session ID and cache-busting parameter
        String apiUrl = ORDERS_API_URL + "/open/sessions/" + sessionId + "?t=" + System.currentTimeMillis();
        Log.d(TAG, "Fetching orders from: " + apiUrl);

        // Get the auth token
        String authToken = getAuthToken();

        // Create request with token and cache control headers
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache");

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Cancel any ongoing requests
        client.dispatcher().cancelAll();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    progressBar.setVisibility(View.GONE);
                    showEmptyView(getString(R.string.network_error));
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response: " + responseBody);

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray ordersArray = jsonResponse.getJSONArray("data");

                    // Parse orders
                    final List<Order> newOrders = new ArrayList<>();
                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject orderJson = ordersArray.getJSONObject(i);
                        Order order = parseOrder(orderJson);
                        newOrders.add(order);
                    }

                    // Move ALL UI operations to runOnUiThread
                    runOnUiThread(() -> {
                        // Update the master list
                        ordersList = newOrders;

                        // Apply current filter - now on UI thread
                        String currentFilter = searchEditText.getText().toString();
                        filterOrders(currentFilter);

                        // Update UI state
                        swipeRefreshLayout.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);

                        if (filteredOrdersList.isEmpty()) {
                            showEmptyView(getString(R.string.no_orders_found));
                        } else {
                            ordersRecyclerView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        progressBar.setVisibility(View.GONE);
                        showEmptyView(getString(R.string.error_processing_response, e.getMessage()));
                    });
                }
            }
        });
    }

    private Order parseOrder(JSONObject orderJson) {
        try {
            Order order = new Order();

            // Extract basic order info
            order.setId(orderJson.optLong("id", -1));
            order.setTableNumber(orderJson.optString("table_number", ""));
            order.setOrderNumber(String.valueOf(order.getId()));

            // Parse status
            String apiStatus = orderJson.optString("status", "").toLowerCase();
            order.setStatus(apiStatus);

            // Parse total amount
            String totalAmountStr = orderJson.optString("total_amount", "0").replace(",", "");
            try {
                order.setTotalAmount(Double.parseDouble(totalAmountStr));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing total: " + totalAmountStr, e);
                order.setTotalAmount(0.0);
            }

            // Set final amount same as total amount since it's not in the response
            order.setFinalAmount(order.getTotalAmount());

            // Set timestamp
            String createdAt = orderJson.optString("created_at", "");
            order.setCreatedAt(createdAt);

            // Parse customer info
            if (!orderJson.isNull("customer_name")) {
                order.setCustomerName(orderJson.optString("customer_name", ""));
            } else if (!orderJson.isNull("customer_id")) {
                long customerId = orderJson.optLong("customer_id", -1);
                if (customerId > 0) {
                    order.setCustomerName("Customer #" + customerId);
                }
            }

            // Set session ID
            order.setSessionId(orderJson.optLong("cashier_session_id", -1));

            // Set server ID
            order.setServerId(orderJson.optLong("server_id", -1));

            // Extract order items
            if (orderJson.has("order_items")) {
                JSONArray itemsArray = orderJson.getJSONArray("order_items");
                List<OrderItem> orderItems = new ArrayList<>();

                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemJson = itemsArray.getJSONObject(i);
                    OrderItem item = new OrderItem();

                    item.setId(itemJson.optLong("id", -1));
                    item.setOrderId(itemJson.optLong("order_id", -1));
                    item.setMenuItemId(itemJson.optLong("menu_item_id", -1));
                    item.setMenuItemName(itemJson.optString("menu_item_name", ""));

                    if (!itemJson.isNull("variant_id")) {
                        item.setVariantId(itemJson.optLong("variant_id", -1));
                    }

                    // ADD THIS - Parse variant name
                    if (!itemJson.isNull("variant_name")) {
                        item.setVariantName(itemJson.optString("variant_name", ""));
                    } else {
                        item.setVariantName(null);
                    }

                    item.setQuantity(itemJson.optInt("quantity", 0));
                    item.setUnitPrice(itemJson.optDouble("unit_price", 0));
                    item.setTotalPrice(itemJson.optDouble("total_price", 0));

                    if (!itemJson.isNull("notes")) {
                        item.setNotes(itemJson.optString("notes", ""));
                    }

                    item.setStatus(itemJson.optString("status", ""));
                    item.setKitchenPrinted(itemJson.optBoolean("kitchen_printed", false));
                    item.setCreatedAt(itemJson.optString("created_at", ""));
                    item.setUpdatedAt(itemJson.optString("updated_at", ""));

                    orderItems.add(item);
                }

                order.setItems(orderItems);
            }

            return order;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing order", e);
            return new Order(); // Return empty order on error
        }
    }

    private void filterOrders(String query) {
        filteredOrdersList.clear();

        if (query == null || query.isEmpty()) {
            // No filter, show all orders
            filteredOrdersList.addAll(ordersList);
        } else {
            // Filter by table number or order number
            String lowerQuery = query.toLowerCase();
            for (Order order : ordersList) {
                if (order.getTableNumber().toLowerCase().contains(lowerQuery) ||
                        order.getOrderNumber().toLowerCase().contains(lowerQuery)) {
                    filteredOrdersList.add(order);
                }
            }
        }

        // Update adapter
        orderAdapter.updateOrders(filteredOrdersList);

        // Show/hide empty view
        if (filteredOrdersList.isEmpty()) {
            showEmptyView(getString(R.string.no_orders_match_filter));
        } else {
            ordersRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        ordersRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onOrderClick(Order order) {
        // Navigate to order details
        Intent intent = new Intent(this, OrderActivity.class);
        intent.putExtra("order_id", order.getId());
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
    }

    private void showNewOrderDialog() {
        // Create dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_order, null);

        // Get views from dialog layout
        Spinner orderTypeSpinner = dialogView.findViewById(R.id.order_type_spinner);
        ProgressBar orderTypeProgress = dialogView.findViewById(R.id.order_type_progress);
        EditText tableNumberEditText = dialogView.findViewById(R.id.table_number_edit_text);
        EditText customerNameEditText = dialogView.findViewById(R.id.customer_name_edit_text);

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.create_new_order)
                .setView(dialogView)
                .setPositiveButton(R.string.create_order, null) // Set listener later to prevent auto-dismiss
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        // Show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Fetch order types and populate spinner
        fetchOrderTypes(orderTypeSpinner, orderTypeProgress);

        // Get the positive button and set custom click listener
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            // Get form values
            String tableNumber = tableNumberEditText.getText().toString().trim();
            String customerName = customerNameEditText.getText().toString().trim();

            /// Get selected order type
            OrderType selectedOrderType = null;
            if (orderTypeSpinner.getSelectedItem() != null && orderTypeSpinner.getSelectedItemPosition() > 0) {
                selectedOrderType = (OrderType) orderTypeSpinner.getSelectedItem();
            }

            // Validate required fields
            if (selectedOrderType == null) {
                Toast.makeText(this, R.string.please_select_order_type, Toast.LENGTH_SHORT).show();
                return;
            }

            if (tableNumber.isEmpty()) {
                tableNumberEditText.setError(getString(R.string.required_field));
                return;
            }

            // Show loading state
            Toast loadingToast = Toast.makeText(this, R.string.creating_order, Toast.LENGTH_LONG);
            loadingToast.show();

            // Disable input during API call
            positiveButton.setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            orderTypeSpinner.setEnabled(false);
            tableNumberEditText.setEnabled(false);
            customerNameEditText.setEnabled(false);

            // Create order with order type
            createOrderWithType(tableNumber, customerName, selectedOrderType, dialog, loadingToast,
                    positiveButton, dialog.getButton(AlertDialog.BUTTON_NEGATIVE),
                    orderTypeSpinner, tableNumberEditText, customerNameEditText);
        });
    }

    private void fetchOrderTypes(Spinner orderTypeSpinner, ProgressBar orderTypeProgress) {
        // Show loading
        orderTypeProgress.setVisibility(View.VISIBLE);
        orderTypeSpinner.setEnabled(false);

        // Get auth token
        String authToken = getAuthToken();

        // Create request
        Request.Builder requestBuilder = new Request.Builder()
                .url(ORDER_TYPES_API_URL)
                .header("Cache-Control", "no-cache");

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch order types", e);
                runOnUiThread(() -> {
                    orderTypeProgress.setVisibility(View.GONE);
                    orderTypeSpinner.setEnabled(true);

                    // Use fallback data
                    setupOrderTypeSpinner(orderTypeSpinner, getFallbackOrderTypes());
                    Toast.makeText(OrderListActivity.this,
                            "Using offline order types", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Order types response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if ("success".equals(jsonResponse.optString("status"))) {
                            JSONArray dataArray = jsonResponse.getJSONArray("data");
                            List<OrderType> orderTypes = new ArrayList<>();

                            for (int i = 0; i < dataArray.length(); i++) {
                                JSONObject orderTypeJson = dataArray.getJSONObject(i);
                                OrderType orderType = new OrderType();
                                orderType.setId(orderTypeJson.optLong("id"));
                                orderType.setName(orderTypeJson.optString("name"));
                                orderTypes.add(orderType);
                            }

                            runOnUiThread(() -> {
                                orderTypeProgress.setVisibility(View.GONE);
                                orderTypeSpinner.setEnabled(true);
                                setupOrderTypeSpinner(orderTypeSpinner, orderTypes);
                            });
                        } else {
                            throw new IOException("API returned non-success status");
                        }
                    } else {
                        throw new IOException("HTTP error: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing order types response", e);
                    runOnUiThread(() -> {
                        orderTypeProgress.setVisibility(View.GONE);
                        orderTypeSpinner.setEnabled(true);

                        // Use fallback data
                        setupOrderTypeSpinner(orderTypeSpinner, getFallbackOrderTypes());
                        Toast.makeText(OrderListActivity.this,
                                "Using offline order types", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void setupOrderTypeSpinner(Spinner orderTypeSpinner, List<OrderType> orderTypes) {
        orderTypesList = orderTypes;

        // Add a placeholder at the beginning
        List<OrderType> spinnerItems = new ArrayList<>();
        spinnerItems.add(new OrderType(0, "Select Order Type")); // Placeholder
        spinnerItems.addAll(orderTypes);

        ArrayAdapter<OrderType> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orderTypeSpinner.setAdapter(adapter);
    }

    // Add this method to provide fallback data
    private List<OrderType> getFallbackOrderTypes() {
        List<OrderType> fallbackTypes = new ArrayList<>();
        fallbackTypes.add(new OrderType(1, "Dine In"));
        fallbackTypes.add(new OrderType(2, "Take Away"));
        fallbackTypes.add(new OrderType(3, "GoFood"));
        fallbackTypes.add(new OrderType(4, "GrabFood"));
        fallbackTypes.add(new OrderType(5, "ShopeeFood"));
        fallbackTypes.add(new OrderType(6, "Self Order"));
        return fallbackTypes;
    }

    private void createOrderWithType(String tableNumber, String customerName, OrderType orderType,
                                     AlertDialog dialog, Toast loadingToast, Button positiveButton,
                                     Button negativeButton, Spinner orderTypeSpinner,
                                     EditText tableNumberEditText, EditText customerNameEditText) {
        // ... rest of the method remains the same as before
        try {
            // Create JSON payload
            JSONObject orderData = new JSONObject();
            orderData.put("table_number", tableNumber);
            orderData.put("cashier_session_id", sessionId);
            orderData.put("order_type_id", orderType.getId()); // Add order type ID

            // Add customer name if provided
            if (!customerName.isEmpty()) {
                orderData.put("customer_name", customerName);
            }

            // Create request body
            RequestBody body = RequestBody.create(orderData.toString(), JSON);

            // Get auth token
            String authToken = getAuthToken();

            // Create request with token
            Request.Builder requestBuilder = new Request.Builder()
                    .url(ORDERS_API_URL)
                    .post(body);

            // Add authorization header if token is available
            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            // Execute the request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API request failed", e);
                    runOnUiThread(() -> {
                        loadingToast.cancel();
                        restoreDialogStateSpinner(positiveButton, negativeButton, orderTypeSpinner,
                                tableNumberEditText, customerNameEditText);
                        Toast.makeText(OrderListActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response code: " + response.code());
                    Log.d(TAG, "Response body: " + responseBody);

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");

                            if ("success".equals(status)) {
                                runOnUiThread(() -> {
                                    loadingToast.cancel();
                                    dialog.dismiss();
                                    Toast.makeText(OrderListActivity.this,
                                            getString(R.string.order_created_successfully) +
                                                    " (" + orderType.getName() + ")",
                                            Toast.LENGTH_SHORT).show();
                                    fetchOrders();
                                });
                            } else {
                                String message = jsonResponse.optString("message", getString(R.string.order_creation_failed));
                                runOnUiThread(() -> {
                                    loadingToast.cancel();
                                    restoreDialogStateSpinner(positiveButton, negativeButton, orderTypeSpinner,
                                            tableNumberEditText, customerNameEditText);
                                    Toast.makeText(OrderListActivity.this, message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            handleErrorResponseSpinner(responseBody, loadingToast, positiveButton, negativeButton,
                                    orderTypeSpinner, tableNumberEditText, customerNameEditText);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        runOnUiThread(() -> {
                            loadingToast.cancel();
                            restoreDialogStateSpinner(positiveButton, negativeButton, orderTypeSpinner,
                                    tableNumberEditText, customerNameEditText);
                            Toast.makeText(OrderListActivity.this,
                                    getString(R.string.error_processing_response, e.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating order request", e);
            loadingToast.cancel();
            restoreDialogStateSpinner(positiveButton, negativeButton, orderTypeSpinner,
                    tableNumberEditText, customerNameEditText);
            Toast.makeText(this, getString(R.string.error_creating_request, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreDialogStateSpinner(Button positiveButton, Button negativeButton,
                                           Spinner orderTypeSpinner,
                                           EditText tableNumberEditText, EditText customerNameEditText) {
        positiveButton.setEnabled(true);
        negativeButton.setEnabled(true);
        orderTypeSpinner.setEnabled(true);
        tableNumberEditText.setEnabled(true);
        customerNameEditText.setEnabled(true);
    }

    // Add this helper method to handle error responses
    private void handleErrorResponseSpinner(String responseBody, Toast loadingToast,
                                            Button positiveButton, Button negativeButton,
                                            Spinner orderTypeSpinner,
                                            EditText tableNumberEditText, EditText customerNameEditText) {
        String errorMessage;
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            errorMessage = errorJson.optString("message", getString(R.string.order_creation_failed));
        } catch (JSONException e) {
            errorMessage = getString(R.string.order_creation_failed);
        }

        final String finalErrorMessage = errorMessage;
        runOnUiThread(() -> {
            loadingToast.cancel();
            restoreDialogStateSpinner(positiveButton, negativeButton, orderTypeSpinner,
                    tableNumberEditText, customerNameEditText);
            Toast.makeText(OrderListActivity.this, finalErrorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private String getAuthToken() {
        return getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
    }
}