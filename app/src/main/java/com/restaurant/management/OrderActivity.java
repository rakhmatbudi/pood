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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrderActivity extends AppCompatActivity {
    private static final int CANCEL_ITEM_REQUEST_CODE = 200;
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
    private Button cancelOrderButton;
    private Button paymentButton;
    private FloatingActionButton addItemFab;

    private Order order;
    private String updatedAt;
    private long orderId = -1;
    private long sessionId = -1;
    private OkHttpClient client = new OkHttpClient();
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US); // Corrected pattern for 'yyyy'

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormat.setTimeZone(TimeZone.getDefault());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.order_details));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeViews();
        setupClickListeners();
        setupRecyclerView();

        orderId = getIntent().getLongExtra("order_id", -1);
        sessionId = getIntent().getLongExtra("session_id", -1);

        if (orderId == -1 || sessionId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchOrderDetails(orderId);
    }

    private void initializeViews() {
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
        cancelOrderButton = findViewById(R.id.cancel_order_button);
        paymentButton = findViewById(R.id.payment_button);
        addItemFab = findViewById(R.id.add_item_fab);
    }

    private void setupClickListeners() {
        addItemFab.setOnClickListener(v -> navigateToAddItem());
        paymentButton.setOnClickListener(v -> navigateToPayment());
        cancelOrderButton.setOnClickListener(v -> showCancelOrderDialog());
    }

    private void setupRecyclerView() {
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_ITEM_REQUEST_CODE && resultCode == RESULT_OK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fetchOrderDetails(orderId);
                Toast.makeText(this, R.string.order_updated, Toast.LENGTH_SHORT).show();
            }, 500);
        } else if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            fetchOrderDetails(orderId);

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
        } else if (requestCode == CANCEL_ITEM_REQUEST_CODE && resultCode == RESULT_OK) {
            fetchOrderDetails(orderId);
            Toast.makeText(this, "Order updated", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCancelOrderDialog() {
        if (order == null) {
            Toast.makeText(this, R.string.order_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        if ("closed".equalsIgnoreCase(order.getStatus()) || "cancelled".equalsIgnoreCase(order.getStatus())) {
            Toast.makeText(this, "Order is already closed or cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order? This action cannot be undone.")
                .setPositiveButton("Cancel Order", (dialog, which) -> cancelOrder())
                .setNegativeButton("Keep Order", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void cancelOrder() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        String cancelUrl = BASE_API_URL + orderId + "/cancel";
        String authToken = getAuthToken();

        RequestBody emptyBody = RequestBody.create(new byte[0], MediaType.parse("application/json")); // Corrected emptyBody
        Request.Builder requestBuilder = new Request.Builder()
                .url(cancelUrl)
                .put(emptyBody);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    contentLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(OrderActivity.this,
                            "Network error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";

                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(OrderActivity.this,
                                    "Order cancelled successfully",
                                    Toast.LENGTH_SHORT).show();

                            finish(); // Or fetchOrderDetails(orderId); to update UI
                        });
                    } else {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            contentLayout.setVisibility(View.VISIBLE);

                            String errorMessage = "Failed to cancel order (Code: " + response.code() + ")";
                            try {
                                if (responseBody.contains("message")) {
                                    JSONObject errorJson = new JSONObject(responseBody);
                                    errorMessage = errorJson.optString("message", errorMessage);
                                } else if (responseBody.contains("error")) {
                                    JSONObject errorJson = new JSONObject(responseBody);
                                    errorMessage = errorJson.optString("error", errorMessage);
                                }
                            } catch (Exception e) {
                                // Use default error message
                            }

                            Toast.makeText(OrderActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        contentLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(OrderActivity.this,
                                "Error cancelling order: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void navigateToAddItem() {
        Intent intent = new Intent(OrderActivity.this, AddItemActivity.class);
        intent.putExtra("order_id", orderId);
        intent.putExtra("table_number", order.getTableNumber());
        startActivityForResult(intent, ADD_ITEM_REQUEST_CODE);
    }

    private void navigateToPayment() {
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

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("order_id", order.getId());
        intent.putExtra("order_number", order.getOrderNumber());
        intent.putExtra("table_number", order.getTableNumber());
        intent.putExtra("final_amount", order.getFinalAmount());
        intent.putExtra("session_id", order.getSessionId());

        startActivityForResult(intent, PAYMENT_REQUEST_CODE);
    }

    private void fetchOrderDetails(long orderId) {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        String apiUrl = BASE_API_URL + orderId + "?t=" + System.currentTimeMillis();
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
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

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONObject orderData = jsonResponse.getJSONObject("data");
                    order = parseOrder(orderData);
                    updatedAt = orderData.optString("update_at", "");

                    runOnUiThread(() -> displayOrderDetails());

                } catch (Exception e) {
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

        order.setId(orderJson.optLong("id", -1));
        order.setTableNumber(orderJson.optString("table_number", ""));
        order.setOrderNumber(String.valueOf(order.getId()));

        String totalAmountStr = orderJson.optString("total_amount", "0.0").replace(",", "");
        try {
            order.setTotalAmount(Double.parseDouble(totalAmountStr));
        } catch (NumberFormatException e) {
            order.setTotalAmount(0.0);
        }

        String finalAmountStr = orderJson.optString("final_amount", "0.0").replace(",", "");
        try {
            order.setFinalAmount(Double.parseDouble(finalAmountStr));
        } catch (NumberFormatException e) {
            double serviceCharge = orderJson.optDouble("service_charge", 0.0);
            double taxAmount = orderJson.optDouble("tax_amount", 0.0);
            double discountAmount = orderJson.optDouble("discount_amount", 0.0);
            double finalAmount = order.getTotalAmount() + serviceCharge + taxAmount - discountAmount;
            order.setFinalAmount(finalAmount);
        }

        // Parse status - use order_status_name instead of status
        String orderStatusName = orderJson.optString("order_status_name", "").toLowerCase();
        order.setStatus(orderStatusName);

        order.setCreatedAt(orderJson.optString("created_at", ""));

        if (!orderJson.isNull("customer_name")) {
            order.setCustomerName(orderJson.optString("customer_name", ""));
        } else if (!orderJson.isNull("customer_id")) {
            long customerId = orderJson.optLong("customer_id", -1);
            if (customerId > 0) {
                order.setCustomerName("Customer #" + customerId);
            } else {
                order.setCustomerName(null);
            }
        } else {
            order.setCustomerName(null);
        }

        order.setServerId(orderJson.optLong("server_id", -1));
        order.setSessionId(orderJson.optLong("cashier_session_id", -1));

        if (!orderJson.isNull("order_type_id")) {
            order.setOrderTypeId(orderJson.optLong("order_type_id", -1));
        }

        if (!orderJson.isNull("order_type_name")) {
            order.setOrderTypeName(orderJson.optString("order_type_name", ""));
        }

        List<OrderItem> orderItems = new ArrayList<>();
        if (orderJson.has("order_items") && !orderJson.isNull("order_items")) {
            JSONArray itemsArray = orderJson.getJSONArray("order_items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                OrderItem item = parseOrderItem(itemJson);
                orderItems.add(item);
            }
        }

        order.setItems(orderItems);
        return order;
    }

    private OrderItem parseOrderItem(JSONObject itemJson) {
        OrderItem item = new OrderItem();

        item.setId(itemJson.optLong("id", -1));
        item.setOrderId(itemJson.optLong("order_id", -1));
        item.setMenuItemId(itemJson.optLong("menu_item_id", -1));
        item.setMenuItemName(itemJson.optString("menu_item_name", ""));

        if (!itemJson.isNull("variant_id")) {
            item.setVariantId(itemJson.optLong("variant_id", -1));
        } else {
            item.setVariantId(null);
        }

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

        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);

        updateActionButtons();

        orderNumberTextView.setText(getString(R.string.order_number_format, order.getOrderNumber()));
        tableNumberTextView.setText(getString(R.string.table_number_format, order.getTableNumber()));

        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            customerNameTextView.setText(getString(R.string.customer_name_format, order.getCustomerName()));
            customerNameTextView.setVisibility(View.VISIBLE);
        } else {
            customerNameTextView.setVisibility(View.GONE);
        }

        String formattedStatus = order.getFormattedStatus();
        String statusText;
        if (order.getOrderTypeName() != null && !order.getOrderTypeName().isEmpty()) {
            statusText = getString(R.string.order_status_format, formattedStatus) +
                    " • " + order.getOrderTypeName();
        } else {
            statusText = getString(R.string.order_status_format, formattedStatus);
        }
        orderStatusTextView.setText(statusText);

        String formattedTotal = formatPriceWithCurrency(order.getTotalAmount());
        orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));

        String formattedCreatedDate = formatAPIDate(order.getCreatedAt());
        orderDateTextView.setText(getString(R.string.order_date_format, formattedCreatedDate));

        String formattedUpdatedDate = formatAPIDate(updatedAt);
        orderUpdateTextView.setText("Updated: " + formattedUpdatedDate);

        orderServerTextView.setText("Server: #" + order.getServerId());
        orderSessionTextView.setText("Session: #" + order.getSessionId());

        List<OrderItem> orderItems = order.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(orderItems, this);
            orderItemsRecyclerView.setAdapter(adapter);
        }
    }

    private void updateActionButtons() {
        boolean isOrderOpen = !"closed".equalsIgnoreCase(order.getStatus()) &&
                !"cancelled".equalsIgnoreCase(order.getStatus());

        // Update FAB visibility and state
        if (isOrderOpen) {
            addItemFab.show();
        } else {
            addItemFab.hide();
        }

        // Update regular buttons
        cancelOrderButton.setEnabled(isOrderOpen);
        cancelOrderButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);

        paymentButton.setEnabled(isOrderOpen);
        paymentButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);
    }

    private String formatAPIDate(String apiDateStr) {
        if (apiDateStr == null || apiDateStr.isEmpty()) {
            return "N/A";
        }

        try {
            Date date = apiDateFormat.parse(apiDateStr);
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    date.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);

            return displayDateFormat.format(date) + " (" + timeAgo + ")";
        } catch (ParseException e) {
            return apiDateStr;
        }
    }

    private String formatPriceWithCurrency(double price) {
        long roundedPrice = Math.round(price);
        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        String currencyPrefix = getString(R.string.currency_prefix);
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