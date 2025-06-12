package com.restaurant.management;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.restaurant.management.models.OrderItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CancelOrderItemActivity extends AppCompatActivity {
    private static final String TAG = "CancelOrderItemActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView itemNameTextView;
    private TextView itemVariantTextView;
    private TextView itemQuantityTextView;
    private TextView itemPriceTextView;
    private TextView itemNotesTextView;
    private TextView itemStatusTextView;
    private Button cancelItemButton;
    private Button backButton;

    private OrderItem orderItem;
    private long orderId;
    private long itemId;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel_order_item);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Cancel Order Item");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        itemNameTextView = findViewById(R.id.item_name_text_view);
        itemVariantTextView = findViewById(R.id.item_variant_text_view);
        itemQuantityTextView = findViewById(R.id.item_quantity_text_view);
        itemPriceTextView = findViewById(R.id.item_price_text_view);
        itemNotesTextView = findViewById(R.id.item_notes_text_view);
        itemStatusTextView = findViewById(R.id.item_status_text_view);
        cancelItemButton = findViewById(R.id.cancel_item_button);
        backButton = findViewById(R.id.back_button);

        // Get data from intent
        orderId = getIntent().getLongExtra("order_id", -1);
        itemId = getIntent().getLongExtra("item_id", -1);
        String itemName = getIntent().getStringExtra("item_name");
        String itemVariant = getIntent().getStringExtra("item_variant");
        int quantity = getIntent().getIntExtra("quantity", 0);
        double unitPrice = getIntent().getDoubleExtra("unit_price", 0.0);
        double totalPrice = getIntent().getDoubleExtra("total_price", 0.0);
        String notes = getIntent().getStringExtra("notes");
        String status = getIntent().getStringExtra("status");

        if (orderId == -1 || itemId == -1) {
            Toast.makeText(this, "Invalid order item data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create OrderItem object
        orderItem = new OrderItem();
        orderItem.setId(itemId);
        orderItem.setOrderId(orderId);
        orderItem.setMenuItemName(itemName);
        orderItem.setVariantName(itemVariant);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(totalPrice);
        orderItem.setNotes(notes);
        orderItem.setStatus(status);

        // Display item details
        displayItemDetails();

        // Set up button click listeners
        cancelItemButton.setOnClickListener(v -> showCancelConfirmationDialog());
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void displayItemDetails() {
        itemNameTextView.setText(orderItem.getMenuItemName());

        if (orderItem.getVariantName() != null && !orderItem.getVariantName().isEmpty()) {
            itemVariantTextView.setText("Variant: " + orderItem.getVariantName());
            itemVariantTextView.setVisibility(TextView.VISIBLE);
        } else {
            itemVariantTextView.setVisibility(TextView.GONE);
        }

        itemQuantityTextView.setText("Quantity: " + orderItem.getQuantity());

        String formattedPrice = formatPriceWithCurrency(orderItem.getTotalPrice());
        itemPriceTextView.setText("Total: " + formattedPrice);

        if (orderItem.getNotes() != null && !orderItem.getNotes().isEmpty()) {
            itemNotesTextView.setText("Notes: " + orderItem.getNotes());
            itemNotesTextView.setVisibility(TextView.VISIBLE);
        } else {
            itemNotesTextView.setVisibility(TextView.GONE);
        }

        String formattedStatus = formatStatus(orderItem.getStatus());
        itemStatusTextView.setText("Status: " + formattedStatus);

        // Disable cancel button if already cancelled
        if ("cancelled".equalsIgnoreCase(orderItem.getStatus())) {
            cancelItemButton.setEnabled(false);
            cancelItemButton.setText("Already Cancelled");
            cancelItemButton.setAlpha(0.5f);
        }
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Order Item")
                .setMessage("Are you sure you want to cancel this item?\n\n" +
                        orderItem.getMenuItemName() +
                        (orderItem.getVariantName() != null ? " (" + orderItem.getVariantName() + ")" : "") +
                        "\nQuantity: " + orderItem.getQuantity())
                .setPositiveButton("Yes, Cancel Item", (dialog, which) -> cancelOrderItem())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void cancelOrderItem() {
        // Show loading state
        cancelItemButton.setEnabled(false);
        cancelItemButton.setText("Cancelling...");

        try {
            // Create JSON payload
            JSONObject statusData = new JSONObject();
            statusData.put("status", "cancelled");

            // Create request body
            RequestBody body = RequestBody.create(statusData.toString(), JSON);

            // Build API URL: /orders/{orderId}/items/{itemId}/status
            String apiUrl = BASE_API_URL + orderId + "/items/" + itemId + "/status";
            Log.d(TAG, "Updating item status at: " + apiUrl);

            // Get auth token
            String authToken = getAuthToken();

            // Create request
            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .put(body)
                    .header("Content-Type", "application/json");

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
                        cancelItemButton.setEnabled(true);
                        cancelItemButton.setText("Cancel Item");
                        Toast.makeText(CancelOrderItemActivity.this,
                                "Network error. Please try again.",
                                Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(CancelOrderItemActivity.this,
                                            "Item cancelled successfully",
                                            Toast.LENGTH_SHORT).show();

                                    // Update the item status locally
                                    orderItem.setStatus("cancelled");
                                    displayItemDetails();

                                    // Set result to indicate success and finish
                                    setResult(RESULT_OK);
                                    finish();
                                });
                            } else {
                                String message = jsonResponse.optString("message", "Failed to cancel item");
                                runOnUiThread(() -> {
                                    cancelItemButton.setEnabled(true);
                                    cancelItemButton.setText("Cancel Item");
                                    Toast.makeText(CancelOrderItemActivity.this, message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            handleErrorResponse(responseBody);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        runOnUiThread(() -> {
                            cancelItemButton.setEnabled(true);
                            cancelItemButton.setText("Cancel Item");
                            Toast.makeText(CancelOrderItemActivity.this,
                                    "Error processing response",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating cancel request", e);
            cancelItemButton.setEnabled(true);
            cancelItemButton.setText("Cancel Item");
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleErrorResponse(String responseBody) {
        String errorMessage;
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            errorMessage = errorJson.optString("message", "Failed to cancel item");
        } catch (JSONException e) {
            errorMessage = "Failed to cancel item";
        }

        final String finalErrorMessage = errorMessage;
        runOnUiThread(() -> {
            cancelItemButton.setEnabled(true);
            cancelItemButton.setText("Cancel Item");
            Toast.makeText(CancelOrderItemActivity.this, finalErrorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "Unknown";
        }
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
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

        // Format according to the pattern in strings.xml
        return getString(R.string.currency_format_pattern, currencyPrefix, formattedPrice.toString());
    }

    private String getAuthToken() {
        return getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
    }
}