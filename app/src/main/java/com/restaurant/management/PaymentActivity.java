package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.models.PaymentMethod;

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

public class PaymentActivity extends AppCompatActivity {
    private static final String TAG = "PaymentActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";
    private static final String PAYMENT_MODES_URL = "https://api.pood.lol/payment-modes";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView orderNumberTextView;
    private TextView tableNumberTextView;
    private TextView orderTotalTextView;
    private TextView changeTextView;
    private RadioGroup paymentMethodRadioGroup;
    private TextInputEditText amountPaidEditText;
    private TextInputEditText paymentNotesEditText;
    private Button cancelButton;
    private Button processPaymentButton;
    private ProgressBar progressBar;
    private View contentView;

    private long orderId;
    private String orderNumber;
    private String tableNumber;
    private double finalAmount; // Only using final amount
    private long sessionId;
    private double amountPaid;
    private String selectedPaymentMethod = "cash"; // Default payment method
    private String selectedPaymentMethodId = "1"; // Default payment method ID
    private OkHttpClient client = new OkHttpClient();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        Log.d(TAG, "PaymentActivity onCreate started");
        Log.d(TAG, "Intent extras: " + (getIntent().getExtras() != null ? getIntent().getExtras().toString() : "null"));

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.payment));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        orderNumberTextView = findViewById(R.id.order_number_text_view);
        tableNumberTextView = findViewById(R.id.table_number_text_view);
        orderTotalTextView = findViewById(R.id.order_total_text_view);
        changeTextView = findViewById(R.id.change_text_view);
        paymentMethodRadioGroup = findViewById(R.id.payment_method_radio_group);
        amountPaidEditText = findViewById(R.id.amount_paid_edit_text);
        paymentNotesEditText = findViewById(R.id.payment_notes_edit_text);
        cancelButton = findViewById(R.id.cancel_button);
        processPaymentButton = findViewById(R.id.process_payment_button);
        progressBar = findViewById(R.id.progress_bar);
        contentView = findViewById(R.id.content_layout);

        if (contentView == null) {
            Log.e(TAG, "Content view not found! Check your layout ID.");
        } else {
            Log.d(TAG, "Content view found successfully");
        }

        // Get data from intent
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderId = intent.getLongExtra("order_id", -1);
        orderNumber = intent.getStringExtra("order_number");
        tableNumber = intent.getStringExtra("table_number");
        finalAmount = intent.getDoubleExtra("final_amount", 0.0);
        sessionId = intent.getLongExtra("session_id", -1);

        // Log all received data for debugging
        Log.d(TAG, "Received from intent: order_id=" + orderId +
                ", order_number=" + orderNumber +
                ", table_number=" + tableNumber +
                ", final_amount=" + finalAmount +
                ", session_id=" + sessionId);

        // Check if we have all required data
        if (orderId == -1 || orderNumber == null || tableNumber == null || finalAmount <= 0) {
            // If we at least have the orderId, try to fetch the order details from API
            if (orderId != -1) {
                Log.d(TAG, "Missing essential order data, will try to fetch from API");
                fetchOrderDetails(orderId);
            } else {
                // Cannot proceed without at least the order ID
                Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Invalid order data, finishing activity. orderId: " + orderId +
                        ", orderNumber: " + orderNumber + ", tableNumber: " + tableNumber +
                        ", finalAmount: " + finalAmount);
                finish();
            }
            return;
        }

        // We have all required data, proceed to setup UI
        setupUI();
        setupListeners();
        fetchPaymentMethods();
    }

    private void fetchOrderDetails(long orderId) {
        // Show loading state
        setLoadingState(true);
        Log.d(TAG, "Fetching order details for order ID: " + orderId);

        // Build the URL for the order
        String orderApiUrl = BASE_API_URL + orderId;
        Log.d(TAG, "Fetching order from: " + orderApiUrl);

        // Get the auth token
        String authToken = getAuthToken();

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(orderApiUrl);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Order details API request failed", e);
                runOnUiThread(() -> {
                    setLoadingState(false);
                    Toast.makeText(PaymentActivity.this,
                            R.string.order_fetch_failed,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Order details response code: " + response.code());
                    Log.d(TAG, "Order details response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Check if status is success
                        String status = jsonResponse.optString("status", "");
                        if (!"success".equals(status)) {
                            throw new JSONException("API returned non-success status: " + status);
                        }

                        // Parse order data
                        JSONObject orderData = jsonResponse.getJSONObject("data");

                        // Get required fields
                        // Convert string amounts to double
                        String finalAmountStr = orderData.optString("final_amount", "0");
                        finalAmount = Double.parseDouble(finalAmountStr.replace(",", ""));
                        tableNumber = orderData.optString("table_number", "");
                        orderNumber = String.valueOf(orderData.optLong("id", -1)); // Use ID as order number if not provided

                        Log.d(TAG, "Parsed order data - Number: " + orderNumber +
                                ", Table: " + tableNumber + ", Final Amount: " + finalAmount);

                        // Check if we now have the required data
                        if (orderNumber == null || tableNumber == null || finalAmount <= 0) {
                            throw new JSONException("Incomplete order data from API");
                        }

                        // Update UI with the fetched data
                        runOnUiThread(() -> {
                            Log.d(TAG, "API call successful, setting up UI");
                            setupUI();
                            setupListeners();
                            fetchPaymentMethods();
                        });
                    } else {
                        // API error response
                        Log.e(TAG, "API returned error: " + response.code());
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(PaymentActivity.this,
                                    R.string.order_fetch_failed,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing order details response", e);
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(PaymentActivity.this,
                                R.string.order_fetch_failed,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
        });
    }

    private void setupUI() {
        // Set initial values
        orderNumberTextView.setText(getString(R.string.order_number_format, orderNumber));
        tableNumberTextView.setText(getString(R.string.table_number_format, tableNumber));

        // Format and display total - using finalAmount
        String formattedTotal = formatPriceWithCurrency(finalAmount);
        orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));

        // Set initial amount paid to match final amount
        amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));

        // Initial change calculation (zero if exact amount)
        updateChangeDisplay();
    }

    private void setupListeners() {
        // Payment method selection
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Log.d(TAG, "Radio button selected with ID: " + checkedId);

            // Find the selected payment method based on the checkedId
            for (int i = 0; i < paymentMethodRadioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) paymentMethodRadioGroup.getChildAt(i);
                if (radioButton.getId() == checkedId) {
                    // Get the tag (payment method id)
                    String paymentMethodId = (String) radioButton.getTag();
                    Log.d(TAG, "Found selected radio button with tag: " + paymentMethodId);

                    // Find the payment method in our list
                    for (PaymentMethod method : paymentMethods) {
                        if (method.getId().equals(paymentMethodId)) {
                            selectedPaymentMethod = method.getCode();
                            selectedPaymentMethodId = method.getId();
                            Log.d(TAG, "Selected payment method: " + method.getName() +
                                    " (Code: " + method.getCode() + ", ID: " + method.getId() + ")");

                            // Handle UI based on payment type
                            if ("cash".equalsIgnoreCase(method.getCode())) {
                                // Cash needs change calculation
                                amountPaidEditText.setEnabled(true);
                                Log.d(TAG, "Enabled amount input for cash payment");
                            } else {
                                // Non-cash payments typically match total exactly
                                amountPaidEditText.setText(String.valueOf(Math.round(finalAmount))); // Use finalAmount
                                amountPaidEditText.setEnabled(false);
                                Log.d(TAG, "Disabled amount input for non-cash payment");
                            }

                            updateChangeDisplay();
                            break;
                        }
                    }
                    break;
                }
            }
        });

        // Amount paid changes
        amountPaidEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateChangeDisplay();
            }
        });

        // Cancel button
        cancelButton.setOnClickListener(v -> finish());

        // Process payment button
        processPaymentButton.setOnClickListener(v -> validateAndProcessPayment());
    }

    private void fetchPaymentMethods() {
        // Show loading state
        setLoadingState(true);
        Log.d(TAG, "Starting to fetch payment methods");

        // Build the URL
        Log.d(TAG, "Fetching payment methods from: " + PAYMENT_MODES_URL);

        // Get the auth token
        String authToken = getAuthToken();
        Log.d(TAG, "Auth token available: " + (authToken != null && !authToken.isEmpty()));

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(PAYMENT_MODES_URL);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Payment methods API request failed", e);
                runOnUiThread(() -> {
                    // If we can't fetch payment methods, use default ones
                    createDefaultPaymentMethods();
                    populatePaymentMethodsUI();
                    setLoadingState(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Payment methods response code: " + response.code());
                    Log.d(TAG, "Payment methods response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Check if status is success
                        String status = jsonResponse.optString("status", "");
                        if (!"success".equals(status)) {
                            throw new JSONException("API returned non-success status: " + status);
                        }

                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        Log.d(TAG, "Found " + dataArray.length() + " payment methods");

                        // Parse payment methods
                        paymentMethods.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject methodJson = dataArray.getJSONObject(i);

                            // Extract values based on actual API response format
                            long id = methodJson.getLong("id");
                            long typeId = methodJson.getLong("payment_mode_type_id");
                            String description = methodJson.getString("description");
                            boolean isActive = methodJson.getBoolean("is_active");

                            // Skip inactive payment methods
                            if (!isActive) {
                                Log.d(TAG, "Skipping inactive payment method: " + description);
                                continue;
                            }

                            // Determine payment code based on type ID
                            String code;
                            switch ((int) typeId) {
                                case 1:
                                    code = "cash"; // Cash type
                                    break;
                                case 2:
                                    code = "card"; // Card type (EDC)
                                    break;
                                case 3:
                                    code = "transfer"; // Bank transfer
                                    break;
                                default:
                                    code = "other"; // Other types
                                    break;
                            }

                            PaymentMethod method = new PaymentMethod(
                                    String.valueOf(id),
                                    description,
                                    code
                            );
                            paymentMethods.add(method);
                            Log.d(TAG, "Added payment method: " + method.getName() +
                                    " (Code: " + method.getCode() + ", ID: " + method.getId() + ")");
                        }

                        // If no active payment methods found, use default ones
                        if (paymentMethods.isEmpty()) {
                            Log.w(TAG, "No active payment methods found, using defaults");
                            runOnUiThread(() -> {
                                createDefaultPaymentMethods();
                                populatePaymentMethodsUI();
                                setLoadingState(false);
                            });
                            return;
                        }

                        runOnUiThread(() -> {
                            Log.d(TAG, "API call successful, populating UI with " + paymentMethods.size() + " methods");
                            // Populate UI with payment methods
                            populatePaymentMethodsUI();
                            setLoadingState(false);
                        });
                    } else {
                        // If API fails, use default payment methods
                        Log.e(TAG, "API returned error: " + response.code());
                        runOnUiThread(() -> {
                            createDefaultPaymentMethods();
                            populatePaymentMethodsUI();
                            setLoadingState(false);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing payment methods response", e);
                    runOnUiThread(() -> {
                        createDefaultPaymentMethods();
                        populatePaymentMethodsUI();
                        setLoadingState(false);
                    });
                }
            }
        });
    }

    private void createDefaultPaymentMethods() {
        // Create default payment methods if API fails
        Log.d(TAG, "Creating default payment methods");
        paymentMethods.clear();
        paymentMethods.add(new PaymentMethod("1", "Cash", "cash"));
        paymentMethods.add(new PaymentMethod("2", "Credit/Debit Card", "card"));
        paymentMethods.add(new PaymentMethod("3", "Mobile Payment", "mobile"));
    }

    private void populatePaymentMethodsUI() {
        Log.d(TAG, "Starting to populate payment methods UI");

        // Clear existing radio buttons
        paymentMethodRadioGroup.removeAllViews();
        Log.d(TAG, "Cleared existing radio buttons");

        // Add radio buttons for each payment method
        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod method = paymentMethods.get(i);
            Log.d(TAG, "Creating radio button for: " + method.getName());

            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(method.getName());
            radioButton.setTag(method.getId()); // Store the ID as a tag
            radioButton.setPadding(32, 30, 32, 30); // Add padding for better touch targets

            // Add radio button to group
            paymentMethodRadioGroup.addView(radioButton);
            Log.d(TAG, "Added radio button to group: " + method.getName());

            // Set the first one as checked by default
            if (i == 0) {
                radioButton.setChecked(true);
                selectedPaymentMethod = method.getCode();
                selectedPaymentMethodId = method.getId();
                Log.d(TAG, "Set first payment method as default: " + method.getName());

                // If it's not cash, disable amount input
                if (!"cash".equalsIgnoreCase(method.getCode())) {
                    amountPaidEditText.setEnabled(false);
                }
            }
        }

        Log.d(TAG, "Finished populating payment methods UI with " + paymentMethods.size() + " methods");

        // Update UI based on initial selection
        updateChangeDisplay();
    }

    private void updateChangeDisplay() {
        try {
            String amountText = amountPaidEditText.getText() != null ?
                    amountPaidEditText.getText().toString() : "0";

            if (amountText.isEmpty()) {
                amountText = "0";
            }

            // Parse amount paid
            amountPaid = Double.parseDouble(amountText);

            // Calculate change based on API-provided finalAmount
            double change = amountPaid - finalAmount;

            // Update change display
            String formattedChange = formatPriceWithCurrency(Math.max(0, change));
            changeTextView.setText(getString(R.string.change_format, formattedChange));

            // Validate if payment is sufficient
            if (amountPaid < finalAmount && "cash".equalsIgnoreCase(selectedPaymentMethod)) {
                changeTextView.setTextColor(getResources().getColor(R.color.colorError));
                processPaymentButton.setEnabled(false);
            } else {
                changeTextView.setTextColor(getResources().getColor(R.color.colorNormal));
                processPaymentButton.setEnabled(true);
            }
        } catch (NumberFormatException e) {
            // Invalid amount format
            changeTextView.setText(getString(R.string.invalid_amount));
            changeTextView.setTextColor(getResources().getColor(R.color.colorError));
            processPaymentButton.setEnabled(false);
        }
    }

    private void validateAndProcessPayment() {
        // Validate payment
        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaid < finalAmount) {
            Toast.makeText(this, R.string.insufficient_payment, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Process payment with API
        processPayment();
    }

    private void processPayment() {
        try {
            // Show loading state
            setLoadingState(true);

            // Create request body with the correct format
            JSONObject paymentData = new JSONObject();
            paymentData.put("order_id", orderId);
            paymentData.put("amount", finalAmount); // Use finalAmount from API
            paymentData.put("payment_mode", Integer.parseInt(selectedPaymentMethodId)); // Use ID as payment_mode
            paymentData.put("transaction_id", null); // Optional: Set to null as mentioned in the required format

            // Add notes as additional info if provided (even though not in the required format)
            String notes = paymentNotesEditText.getText() != null ?
                    paymentNotesEditText.getText().toString().trim() : "";
            if (!notes.isEmpty()) {
                paymentData.put("notes", notes); // Extra field, might be ignored by API
            }

            // Use the correct API endpoint
            String apiUrl = "https://api.pood.lol/payments";
            Log.d(TAG, "Processing payment at: " + apiUrl);
            Log.d(TAG, "Payment data: " + paymentData.toString());

            // Create request body
            RequestBody body = RequestBody.create(paymentData.toString(), JSON);

            // Get the auth token
            String authToken = getAuthToken();

            // Create request with token
            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .post(body);

            // Add authorization header if token is available
            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
                Log.d(TAG, "Added authorization header to payment request");
            } else {
                Log.w(TAG, "No authorization token available for payment request");
            }

            Request request = requestBuilder.build();

            // Execute the request
            Log.d(TAG, "Sending payment request...");
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Payment API request failed", e);
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(PaymentActivity.this,
                                getString(R.string.payment_failed_network),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Payment response code: " + response.code());
                    Log.d(TAG, "Payment response: " + responseBody);

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            // Check for success status in the response
                            String status = jsonResponse.optString("status", "");

                            if ("success".equals(status)) {
                                // Payment successful
                                Log.d(TAG, "Payment processed successfully");

                                runOnUiThread(() -> {
                                    setLoadingState(false);
                                    showPaymentSuccessAndFinish();
                                });
                            } else {
                                // API returned non-success status
                                String message = jsonResponse.optString("message",
                                        getString(R.string.payment_failed_unknown));
                                Log.e(TAG, "Payment failed: " + message);

                                runOnUiThread(() -> {
                                    setLoadingState(false);
                                    Toast.makeText(PaymentActivity.this, message,
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            // HTTP error response
                            Log.e(TAG, "Payment failed with HTTP error: " + response.code());

                            // Try to parse error message from response
                            String errorMessage;
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("message",
                                        getString(R.string.payment_failed_server));
                            } catch (JSONException e) {
                                // Couldn't parse JSON, use response body as error message
                                errorMessage = responseBody;
                            }

                            final String finalErrorMessage = errorMessage;
                            runOnUiThread(() -> {
                                setLoadingState(false);
                                Toast.makeText(PaymentActivity.this, finalErrorMessage,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing payment response", e);
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(PaymentActivity.this,
                                    getString(R.string.payment_failed_parsing),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment request", e);
            setLoadingState(false);
            Toast.makeText(this, R.string.payment_failed_request,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showPaymentSuccessAndFinish() {
        // Show success message
        Toast.makeText(this, R.string.payment_success, Toast.LENGTH_SHORT).show();

        // Return success to previous activity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("payment_method", selectedPaymentMethod);
        resultIntent.putExtra("amount_paid", amountPaid);
        setResult(RESULT_OK, resultIntent);

        // Close activity
        finish();
    }

    private void setLoadingState(boolean isLoading) {
        Log.d(TAG, "Setting loading state: " + isLoading);

        // Toggle loading UI elements
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            contentView.setVisibility(View.GONE);
            processPaymentButton.setEnabled(false);
            cancelButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            contentView.setVisibility(View.VISIBLE);
            processPaymentButton.setEnabled(true);
            cancelButton.setEnabled(true);
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
        String token = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
        Log.d(TAG, "Auth token retrieved: " + (token.isEmpty() ? "EMPTY" : "NOT EMPTY"));
        return token;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}