package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.adapters.OrderItemAdapter;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrderActivity extends AppCompatActivity {
    private static final int ADD_ITEM_REQUEST_CODE = 100;
    private static final int PAYMENT_REQUEST_CODE = 101;
    private static final String TAG = "OrderActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";

    private TextView orderNumberTextView;
    private TextView tableNumberTextView;
    private TextView customerNameTextView;
    private TextView orderStatusTextView;
    private TextView orderTotalTextView;
    private TextView orderDateTextView;
    private TextView orderUpdateTextView;
    private TextView orderServerTextView;
    private TextView orderSessionTextView;
    private RecyclerView orderItemsRecyclerView;
    private ProgressBar progressBar;
    private View contentLayout;
    private Button addItemButton;
    private Button paymentButton;

    private Order order;
    private String updatedAt; // Store updatedAt as a separate variable
    private long orderId = -1;
    private long sessionId = -1;
    private OkHttpClient client = new OkHttpClient();
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        // Set timezone for parsing API dates (UTC)
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Local timezone for display
        displayDateFormat.setTimeZone(TimeZone.getDefault());

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.order_details));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        orderNumberTextView = findViewById(R.id.order_number_text_view);
        tableNumberTextView = findViewById(R.id.table_number_text_view);
        customerNameTextView = findViewById(R.id.customer_name_text_view);
        orderStatusTextView = findViewById(R.id.order_status_text_view);
        orderTotalTextView = findViewById(R.id.order_total_text_view);
        orderDateTextView = findViewById(R.id.order_date_text_view);
        orderUpdateTextView = findViewById(R.id.order_update_text_view);
        orderServerTextView = findViewById(R.id.order_server_text_view);
        orderSessionTextView = findViewById(R.id.order_session_text_view);
        orderItemsRecyclerView = findViewById(R.id.order_items_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);
        addItemButton = findViewById(R.id.add_item_button);
        paymentButton = findViewById(R.id.payment_button);

        // Set up button click listeners
        addItemButton.setOnClickListener(v -> navigateToAddItem());
        paymentButton.setOnClickListener(v -> navigateToPayment());

        // Setup RecyclerView
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get order ID and session ID from intent
        orderId = getIntent().getLongExtra("order_id", -1);
        sessionId = getIntent().getLongExtra("session_id", -1);

        if (orderId == -1 || sessionId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch order details
        fetchOrderDetails(orderId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_ITEM_REQUEST_CODE && resultCode == RESULT_OK) {
            // Add a small delay to ensure the server has processed the new item
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Refresh order details when an item is added
                fetchOrderDetails(orderId);
                Toast.makeText(this, R.string.order_updated, Toast.LENGTH_SHORT).show();
            }, 500); // 500ms delay
        } else if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            // Payment was successful, refresh order to show closed status
            fetchOrderDetails(orderId);

            // Show success message with payment method
            if (data != null) {
                String paymentMethod = data.getStringExtra("payment_method");
                String formattedMethod = paymentMethod != null ?
                        paymentMethod.substring(0, 1).toUpperCase() + paymentMethod.substring(1) :
                        getString(R.string.unknown);

                Toast.makeText(this,
                        getString(R.string.payment_completed_format, formattedMethod),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.payment_completed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToAddItem() {
        // Navigate to add item screen
        Intent intent = new Intent(OrderActivity.this, AddItemActivity.class);
        intent.putExtra("order_id", orderId);
        intent.putExtra("table_number", order.getTableNumber());
        startActivityForResult(intent, ADD_ITEM_REQUEST_CODE);
    }

    private void navigateToPayment() {
        // Check if order is eligible for payment
        if (order == null) {
            Toast.makeText(this, R.string.order_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if (order.getTotalAmount() <= 0) {
            Toast.makeText(this, R.string.zero_amount_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if ("closed".equalsIgnoreCase(order.getStatus())) {
            Toast.makeText(this, R.string.order_already_closed, Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to payment screen
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("order_id", order.getId());
        intent.putExtra("order_number", order.getOrderNumber());
        intent.putExtra("table_number", order.getTableNumber());
        intent.putExtra("final_amount", order.getFinalAmount()); // This will work now
        intent.putExtra("session_id", order.getSessionId());

        // Log the values being sent for debugging
        Log.d(TAG, "Sending to PaymentActivity - " +
                "order_id: " + order.getId() +
                ", order_number: " + order.getOrderNumber() +
                ", table_number: " + order.getTableNumber() +
                ", final_amount: " + order.getFinalAmount() +
                ", session_id: " + order.getSessionId());

        startActivityForResult(intent, PAYMENT_REQUEST_CODE);
    }

    private void fetchOrderDetails(long orderId) {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        // Build the URL with cache-busting parameter
        String apiUrl = BASE_API_URL + orderId + "?t=" + System.currentTimeMillis();
        Log.d(TAG, "Fetching order details from: " + apiUrl);

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

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrderActivity.this,
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response body: " + responseBody);

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);

                    // Process the order details
                    JSONObject orderData = jsonResponse.getJSONObject("data");
                    order = parseOrder(orderData);

                    // Store updated_at separately
                    updatedAt = orderData.optString("updated_at", "");

                    runOnUiThread(() -> displayOrderDetails());

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrderActivity.this,
                                getString(R.string.error_processing_response, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
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
            order.setTotalAmount(Double.parseDouble(totalAmountStr));
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing total: " + totalAmountStr, e);
            order.setTotalAmount(0.0);
        }

        // Parse final amount (with tax and service charge)
        String finalAmountStr = orderJson.optString("final_amount", "0.0").replace(",", "");
        try {
            order.setFinalAmount(Double.parseDouble(finalAmountStr));
            Log.d(TAG, "Successfully parsed final amount: " + order.getFinalAmount());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing final amount: " + finalAmountStr, e);
            // If final amount parsing fails, calculate it from total amount
            double serviceCharge = orderJson.optDouble("service_charge", 0.0);
            double taxAmount = orderJson.optDouble("tax_amount", 0.0);
            double discountAmount = orderJson.optDouble("discount_amount", 0.0);
            double finalAmount = order.getTotalAmount() + serviceCharge + taxAmount - discountAmount;
            order.setFinalAmount(finalAmount);
            Log.d(TAG, "Calculated final amount: " + order.getFinalAmount() +
                    " (from total: " + order.getTotalAmount() +
                    ", service: " + serviceCharge +
                    ", tax: " + taxAmount +
                    ", discount: " + discountAmount + ")");
        }

        // Map status values
        String apiStatus = orderJson.optString("status", "").toLowerCase();
        order.setStatus("open".equals(apiStatus) ? "pending" : apiStatus);

        // Set timestamps
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

        // Set server ID
        order.setServerId(orderJson.optLong("server_id", -1));

        // Set session ID
        order.setSessionId(orderJson.optLong("cashier_session_id", -1));

        // Parse order type information - API provides both ID and name directly
        if (!orderJson.isNull("order_type_id")) {
            order.setOrderTypeId(orderJson.optLong("order_type_id", -1));
        }

        if (!orderJson.isNull("order_type_name")) {
            order.setOrderTypeName(orderJson.optString("order_type_name", ""));
        }

        // Process order items
        List<OrderItem> orderItems = new ArrayList<>();
        if (orderJson.has("order_items") && !orderJson.isNull("order_items")) {
            JSONArray itemsArray = orderJson.getJSONArray("order_items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                OrderItem item = parseOrderItem(itemJson);
                orderItems.add(item);
            }
        }

        // Store the order items in the order object
        order.setItems(orderItems);

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

        // Parse variant name directly from the API response
        if (!itemJson.isNull("variant_name")) {
            item.setVariantName(itemJson.optString("variant_name", ""));
        } else {
            item.setVariantName(null);
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

    private void displayOrderDetails() {
        if (order == null) return;

        // Hide loading, show content
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);

        // Enable/disable buttons based on order status
        updateActionButtons();

        // Set order details in views
        orderNumberTextView.setText(getString(R.string.order_number_format, order.getOrderNumber()));
        tableNumberTextView.setText(getString(R.string.table_number_format, order.getTableNumber()));

        // Handle customer name (which might be null)
        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            customerNameTextView.setText(getString(R.string.customer_name_format, order.getCustomerName()));
            customerNameTextView.setVisibility(View.VISIBLE);
        } else {
            customerNameTextView.setVisibility(View.GONE);
        }

        // Display status with order type (if available)
        String formattedStatus = order.getFormattedStatus();
        String statusText;
        if (order.getOrderTypeName() != null && !order.getOrderTypeName().isEmpty()) {
            statusText = getString(R.string.order_status_format, formattedStatus) +
                    " • " + order.getOrderTypeName();
        } else {
            statusText = getString(R.string.order_status_format, formattedStatus);
        }
        orderStatusTextView.setText(statusText);

        // Format the total price
        String formattedTotal = formatPriceWithCurrency(order.getTotalAmount());
        orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));

        // Format and display dates
        String formattedCreatedDate = formatAPIDate(order.getCreatedAt());
        orderDateTextView.setText(getString(R.string.order_date_format, formattedCreatedDate));

        // Format and display updated date (using the separate variable)
        String formattedUpdatedDate = formatAPIDate(updatedAt);
        orderUpdateTextView.setText("Updated: " + formattedUpdatedDate);

        // Show server ID and session ID
        orderServerTextView.setText("Server: #" + order.getServerId());
        orderSessionTextView.setText("Session: #" + order.getSessionId());

        // Set up recycler view for order items
        List<OrderItem> orderItems = order.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(orderItems);
            orderItemsRecyclerView.setAdapter(adapter);
        }
    }

    private void updateActionButtons() {
        // Only enable buttons if order is "pending" or "open" (not closed)
        boolean isOrderOpen = !"closed".equalsIgnoreCase(order.getStatus());

        addItemButton.setEnabled(isOrderOpen);
        addItemButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);

        paymentButton.setEnabled(isOrderOpen);
        paymentButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);
    }

    private String formatAPIDate(String apiDateStr) {
        if (apiDateStr == null || apiDateStr.isEmpty()) {
            return "N/A";
        }

        try {
            // Parse the API date (UTC)
            Date date = apiDateFormat.parse(apiDateStr);

            // Get time ago string for relative time
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    date.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);

            // Return formatted date with time ago
            return displayDateFormat.format(date) + " (" + timeAgo + ")";
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + apiDateStr, e);
            return apiDateStr; // Return original if parsing fails
        }
    }

    private String formatPriceWithCurrency(double price) {
        // Round to the nearest integer (no decimal)
        long roundedPrice = Math.round(price);

        // Format as xxx.xxx.xxx
        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            // Add dot after every 3 digits from the right, but not at the end
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        // Get currency prefix from strings.xml
        String currencyPrefix = getString(R.string.currency_prefix);

        // Format according to the pattern in strings.xml (allows for different currency placement)
        return getString(R.string.currency_format_pattern, currencyPrefix, formattedPrice.toString());
    }

    private String getAuthToken() {
        return getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}