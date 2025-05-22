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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.models.Discount;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.RoundingConfig;
import com.restaurant.management.adapters.DiscountSpinnerAdapter;

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
    private static final String ROUNDING_API_URL = "https://api.pood.lol/roundings/values";
    private RoundingConfig roundingConfig;
    private static final String TAG = "PaymentActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";
    private static final String PAYMENT_MODES_URL = "https://api.pood.lol/payment-modes";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private Spinner discountSpinner;
    private TextView discountAmountTextView;
    private TextView discountedTotalTextView;
    private List<Discount> discountList = new ArrayList<>();
    private Discount selectedDiscount;
    private double discountedAmount = 0.0;
    private static final String DISCOUNTS_API_URL = "https://api.pood.lol/discounts/";
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
        //Log.d(TAG, "=================== PaymentActivity onCreate START ===================");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        //Log.d(TAG, "PaymentActivity onCreate started");
        //Log.d(TAG, "Intent extras: " + (getIntent().getExtras() != null ? getIntent().getExtras().toString() : "null"));

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

        // Initialize the discount views
        discountSpinner = findViewById(R.id.discount_spinner);
        discountAmountTextView = findViewById(R.id.discount_amount_text_view);
        discountedTotalTextView = findViewById(R.id.discounted_total_text_view);

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

        // Check if we have the minimum required data
        if (orderId == -1) {
            // We absolutely need the order ID
            Log.e(TAG, "CRITICAL ERROR: Missing order ID, finishing activity");
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_LONG).show();
            // Sleep for 2 seconds to ensure the toast is visible before finishing
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // Ignore
            }
            finish();
            return;
        }

        // If we're missing some non-essential data but have the order ID, fetch from API
        if (orderNumber == null || tableNumber == null || finalAmount <= 0) {
            Log.d(TAG, "Missing some order data, will try to fetch from API");
            fetchOrderDetails(orderId);
            return;
        }

        // We have all required data, proceed to setup UI
        setupUI();
        setupListeners();

        fetchDiscounts();
        fetchPaymentMethods();
        fetchRoundingConfig();
    }

    private void updateAmountPaidWithRounding() {
        // Skip if no rounding config is available yet
        if (roundingConfig == null) {
            Log.d(TAG, "Skipping amount rounding, config not loaded yet");
            return;
        }

        // Calculate the actual amount to pay after discount
        double amountToPay = finalAmount;
        if (selectedDiscount != null && selectedDiscount.getId() != -1) {
            amountToPay = finalAmount - discountedAmount;
        }

        // Apply rounding logic
        double roundedAmount = applyRounding(amountToPay);
        Log.d(TAG, "Rounded amount: " + roundedAmount + " (from " + amountToPay + ")");

        // Update the amount paid field with the rounded value
        amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));

        // Update the change calculation
        updateChangeDisplay();
    }

    private double applyRounding(double amount) {
        // If amount is negative or rounding config is not available, return the original amount
        if (amount < 0 || roundingConfig == null) {
            Log.d(TAG, "Skipping rounding: amount=" + amount +
                    ", config=" + (roundingConfig == null ? "null" : "available"));
            return amount;
        }

        int roundingBelow = roundingConfig.getRoundingBelow();
        int roundingNumber = roundingConfig.getRoundingNumber();
        int roundingDigit = roundingConfig.getRoundingDigit();

        // Convert amount to integer (removing decimal part)
        long amountInt = Math.round(amount);

        // Apply rounding based on the configuration
        long remainder = amountInt % roundingNumber;
        long roundedAmount;

        if (remainder <= roundingBelow) {
            // Round down to the nearest multiple of roundingNumber
            roundedAmount = amountInt - remainder;
            Log.d(TAG, "Rounding down: " + amountInt + " -> " + roundedAmount +
                    " (remainder " + remainder + " <= " + roundingBelow + ")");
        } else {
            // Round up to the nearest multiple of roundingNumber
            roundedAmount = amountInt + (roundingNumber - remainder);
            Log.d(TAG, "Rounding up: " + amountInt + " -> " + roundedAmount +
                    " (remainder " + remainder + " > " + roundingBelow + ")");
        }

        return roundedAmount;
    }

    private void fetchRoundingConfig() {
        Log.d(TAG, "Fetching rounding configuration");

        // Get the auth token
        String authToken = getAuthToken();

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(ROUNDING_API_URL);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch rounding configuration", e);
                // Default to standard rounding if API fails
                runOnUiThread(() -> {
                    roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                    // If we already loaded the UI, update the amount paid with rounding
                    if (amountPaidEditText != null) {
                        updateAmountPaidWithRounding();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Rounding config response code: " + response.code());
                    Log.d(TAG, "Rounding config response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Check if status is success
                        String status = jsonResponse.optString("status", "");
                        if (!"success".equals(status)) {
                            throw new JSONException("API returned non-success status: " + status);
                        }

                        // Parse rounding config
                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject config = dataArray.getJSONObject(0);

                            int roundingBelow = config.optInt("rounding_below", 99);
                            int roundingDigit = config.optInt("rounding_digit", 1);
                            String description = config.optString("rounding_digit_description", "00 - Hundreds");
                            int roundingNumber = config.optInt("rounding_number", 100);

                            Log.d(TAG, "Parsed rounding config: below=" + roundingBelow +
                                    ", digit=" + roundingDigit +
                                    ", number=" + roundingNumber);

                            roundingConfig = new RoundingConfig(
                                    roundingBelow,
                                    roundingDigit,
                                    description,
                                    roundingNumber
                            );
                        } else {
                            // No config found, use default
                            roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                        }

                        runOnUiThread(() -> {
                            // Update the amount paid with rounding if UI is already loaded
                            if (amountPaidEditText != null) {
                                updateAmountPaidWithRounding();
                            }
                        });
                    } else {
                        // API error, use default
                        Log.e(TAG, "API returned error: " + response.code());
                        runOnUiThread(() -> {
                            roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                            if (amountPaidEditText != null) {
                                updateAmountPaidWithRounding();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing rounding config response", e);
                    runOnUiThread(() -> {
                        roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                        if (amountPaidEditText != null) {
                            updateAmountPaidWithRounding();
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=================== PaymentActivity onResume ===================");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=================== PaymentActivity onDestroy ===================");
    }

    private void fetchDiscounts() {
        Log.d(TAG, "Fetching available discounts");

        // Get the auth token
        String authToken = getAuthToken();

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(DISCOUNTS_API_URL);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch discounts", e);
                runOnUiThread(() -> {
                    // Set up discount spinner with empty list
                    setupDiscountSpinner(new ArrayList<>());
                    Toast.makeText(PaymentActivity.this,
                            R.string.error_loading_discounts,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Discounts response code: " + response.code());
                    Log.d(TAG, "Discounts response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        // Check if status is success
                        String status = jsonResponse.optString("status", "");
                        if (!"success".equals(status)) {
                            throw new JSONException("API returned non-success status: " + status);
                        }

                        // Parse discounts
                        List<Discount> discounts = parseDiscountsFromJson(jsonResponse);

                        runOnUiThread(() -> {
                            // Set up discount spinner with fetched discounts
                            setupDiscountSpinner(discounts);
                        });
                    } else {
                        Log.e(TAG, "API returned error: " + response.code());
                        runOnUiThread(() -> {
                            setupDiscountSpinner(new ArrayList<>());
                            Toast.makeText(PaymentActivity.this,
                                    R.string.error_loading_discounts,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing discounts response", e);
                    runOnUiThread(() -> {
                        setupDiscountSpinner(new ArrayList<>());
                        Toast.makeText(PaymentActivity.this,
                                R.string.error_loading_discounts,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private List<Discount> parseDiscountsFromJson(JSONObject jsonResponse) throws JSONException {
        List<Discount> discounts = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray discountsArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < discountsArray.length(); i++) {
                JSONObject discountJson = discountsArray.getJSONObject(i);

                long id = discountJson.optLong("id", -1);
                String name = discountJson.optString("name", "");
                String description = discountJson.optString("description", "");
                int amount = discountJson.optInt("amount", 0);

                Discount discount = new Discount(id, name, description, amount);
                discounts.add(discount);
                Log.d(TAG, "Parsed discount: " + name + " (" + amount + "%)");
            }
        }

        return discounts;
    }

    private void setupDiscountSpinner(List<Discount> discounts) {
        discountList.clear();
        discountList.addAll(discounts);

        // Create and set adapter
        DiscountSpinnerAdapter adapter = new DiscountSpinnerAdapter(this, discountList);
        discountSpinner.setAdapter(adapter);

        // Set selection listener
        discountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDiscount = (Discount) parent.getItemAtPosition(position);
                Log.d(TAG, "Selected discount: " + selectedDiscount.getName() +
                        ", amount: " + selectedDiscount.getAmount() + "%");

                // Calculate and display the discount
                applyDiscount();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDiscount = null;
                discountAmountTextView.setVisibility(View.GONE);
                discountedTotalTextView.setVisibility(View.GONE);

                // Reset to original total
                updateChangeDisplay();
            }
        });
    }

    private void applyDiscount() {
        if (selectedDiscount == null || selectedDiscount.getId() == -1) {
            // No discount selected or "No Discount" option selected
            discountAmountTextView.setVisibility(View.GONE);
            discountedTotalTextView.setVisibility(View.GONE);
            discountedAmount = 0.0;

            // Display only the original total without strikethrough
            String formattedTotal = formatPriceWithCurrency(finalAmount);
            orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));
            orderTotalTextView.setPaintFlags(orderTotalTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            // If this is a cash payment, update the amount paid field to match the total with rounding
            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaidEditText.isEnabled()) {
                if (roundingConfig != null) {
                    double roundedAmount = applyRounding(finalAmount);
                    amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
                } else {
                    amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
                }
            }
        } else {
            // Calculate discount amount
            double discountPercentage = selectedDiscount.getAmount();
            discountedAmount = (finalAmount * discountPercentage) / 100.0;
            double newTotal = finalAmount - discountedAmount;

            // Format and display discount info
            discountAmountTextView.setVisibility(View.VISIBLE);
            discountAmountTextView.setText(getString(R.string.discount_applied,
                    selectedDiscount.getName(), selectedDiscount.getAmount()));

            // Get the label part of the total string
            String totalLabel = getString(R.string.order_total_format, "").replace("%s", "");

            // Create the prices part
            String formattedOriginalPrice = formatPriceWithCurrency(finalAmount);
            String formattedNewPrice = formatPriceWithCurrency(newTotal);

            // Create a spannable string with the label + crossed out original price + new price
            SpannableString spannableString = new SpannableString(
                    totalLabel + formattedOriginalPrice + "  " + formattedNewPrice);

            // Apply strikethrough only to the original price part, not the label
            spannableString.setSpan(new StrikethroughSpan(),
                    totalLabel.length(),
                    totalLabel.length() + formattedOriginalPrice.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the combined text
            orderTotalTextView.setText(spannableString);

            // Hide the now-redundant discounted total text view
            discountedTotalTextView.setVisibility(View.GONE);

            // If this is a cash payment, update the amount paid field to match the new total with rounding
            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaidEditText.isEnabled()) {
                if (roundingConfig != null) {
                    double roundedAmount = applyRounding(newTotal);
                    amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
                } else {
                    amountPaidEditText.setText(String.valueOf(Math.round(newTotal)));
                }
            }
        }

        // Update the change calculation
        updateChangeDisplay();
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

                    // Try to continue with just order ID if available
                    if (orderId > 0) {
                        Log.d(TAG, "Attempting to continue with just order ID");
                        orderNumber = String.valueOf(orderId); // Use order ID as order number
                        tableNumber = "Unknown"; // Default table number
                        finalAmount = 0.0; // Will be updated once payment process begins

                        setupUI();
                        setupListeners();
                        fetchDiscounts();
                        fetchPaymentMethods();
                    } else {
                        Toast.makeText(PaymentActivity.this,
                                R.string.order_fetch_failed,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
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
                        tableNumber = orderData.optString("table_number", "Unknown");
                        orderNumber = String.valueOf(orderData.optLong("id", orderId)); // Use ID as order number if not provided

                        Log.d(TAG, "Parsed order data - Number: " + orderNumber +
                                ", Table: " + tableNumber + ", Final Amount: " + finalAmount);

                        // Update UI with the fetched data
                        runOnUiThread(() -> {
                            Log.d(TAG, "API call successful, setting up UI");
                            setLoadingState(false);
                            setupUI();
                            setupListeners();
                            fetchDiscounts();
                            fetchPaymentMethods();
                        });
                    } else {
                        // API error response
                        Log.e(TAG, "API returned error: " + response.code());
                        runOnUiThread(() -> {
                            setLoadingState(false);

                            // Try to continue with just order ID if available
                            if (orderId > 0) {
                                Log.d(TAG, "Attempting to continue with just order ID");
                                orderNumber = String.valueOf(orderId); // Use order ID as order number
                                tableNumber = "Unknown"; // Default table number
                                finalAmount = 0.0; // Will be updated once payment process begins

                                setupUI();
                                setupListeners();
                                fetchDiscounts();
                                fetchPaymentMethods();
                            } else {
                                Toast.makeText(PaymentActivity.this,
                                        R.string.order_fetch_failed,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing order details response", e);
                    runOnUiThread(() -> {
                        setLoadingState(false);

                        // Try to continue with just order ID if available
                        if (orderId > 0) {
                            Log.d(TAG, "Attempting to continue with just order ID despite parsing error");
                            orderNumber = String.valueOf(orderId); // Use order ID as order number
                            tableNumber = "Unknown"; // Default table number
                            finalAmount = 0.0; // Will be updated once payment process begins

                            setupUI();
                            setupListeners();
                            fetchDiscounts();
                            fetchPaymentMethods();
                        } else {
                            Toast.makeText(PaymentActivity.this,
                                    R.string.order_fetch_failed,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
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

        // If rounding config is available, use rounded amount
        // Otherwise, use regular amount and update later when config is loaded
        if (roundingConfig != null) {
            double roundedAmount = applyRounding(finalAmount);
            amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
        } else {
            amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
        }

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

                                // Update with rounded amount for cash
                                updateAmountPaidWithRounding();
                            } else {
                                // Non-cash payments typically match total exactly (no rounding)
                                double amountToPay = finalAmount;
                                if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                                    amountToPay = finalAmount - discountedAmount;
                                }
                                amountPaidEditText.setText(String.valueOf(Math.round(amountToPay)));
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
        processPaymentButton.setOnClickListener(v -> {
            Log.d(TAG, "Process Payment button clicked");
            validateAndProcessPayment();
        });
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

            // Calculate the actual amount to pay after discount
            double amountToPay = finalAmount;

            // Apply discount if selected
            if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                amountToPay = finalAmount - discountedAmount;
            }

            // For cash payments, we need to consider the rounded amount for validation
            double amountToCompare = amountToPay;
            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && roundingConfig != null) {
                amountToCompare = applyRounding(amountToPay);
            }

            // Calculate change based on the appropriate amount
            double change = amountPaid - amountToPay;

            // Update change display
            String formattedChange = formatPriceWithCurrency(Math.max(0, change));
            changeTextView.setText(getString(R.string.change_format, formattedChange));

            // Validate if payment is sufficient - using the appropriate amount for comparison
            if (amountPaid < amountToCompare && "cash".equalsIgnoreCase(selectedPaymentMethod)) {
                changeTextView.setTextColor(getResources().getColor(R.color.colorError));
                processPaymentButton.setEnabled(false);
                Log.d(TAG, "Payment button disabled: amount paid (" + amountPaid +
                        ") is less than amount to pay after rounding (" + amountToCompare + ")");
            } else {
                changeTextView.setTextColor(getResources().getColor(R.color.colorNormal));
                processPaymentButton.setEnabled(true);
                Log.d(TAG, "Payment button enabled: amount paid (" + amountPaid +
                        ") is sufficient for amount to pay (" + amountToCompare + ")");
            }
        } catch (NumberFormatException e) {
            // Invalid amount format
            changeTextView.setText(getString(R.string.invalid_amount));
            changeTextView.setTextColor(getResources().getColor(R.color.colorError));
            processPaymentButton.setEnabled(false);
            Log.e(TAG, "Invalid amount format", e);
        }
    }

    private void validateAndProcessPayment() {
        Log.d(TAG, "validateAndProcessPayment called");
        // Calculate the actual amount to pay after discount
        double amountToPay = finalAmount;
        if (selectedDiscount != null && selectedDiscount.getId() != -1) {
            amountToPay = finalAmount - discountedAmount;
        }

        // For cash payments, use the rounded amount for validation
        double amountToValidate = amountToPay;
        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && roundingConfig != null) {
            amountToValidate = applyRounding(amountToPay);
        }

        Log.d(TAG, "Amount to pay: " + amountToPay +
                ", Amount after rounding: " + amountToValidate +
                ", Amount paid: " + amountPaid);

        // Validate payment
        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaid < amountToValidate) {
            Toast.makeText(this, R.string.insufficient_payment, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        setLoadingState(true);
        Log.d(TAG, "Loading state set to true, about to process payment");

        // Process payment with API
        processPayment();
    }

    private void processPayment() {
        Log.d(TAG, "processPayment method called");
        try {
            // Show loading state
            setLoadingState(true);

            // Create request body with the correct format
            JSONObject paymentData = new JSONObject();
            paymentData.put("order_id", orderId);

            // Include the discounted amount and discount ID if a discount is applied
            double amountToPay = finalAmount;
            if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                amountToPay = finalAmount - discountedAmount;
                paymentData.put("discount_id", selectedDiscount.getId());
            }

            paymentData.put("amount", amountToPay);
            paymentData.put("payment_mode", Integer.parseInt(selectedPaymentMethodId));
            paymentData.put("transaction_id", null);

            // Add notes as additional info if provided
            String notes = paymentNotesEditText.getText() != null ?
                    paymentNotesEditText.getText().toString().trim() : "";
            if (!notes.isEmpty()) {
                paymentData.put("notes", notes);
            }

            Log.d(TAG, "Payment data prepared: " + paymentData.toString());

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
                            Log.d(TAG, "Payment response status: " + status);

                            if ("success".equals(status)) {
                                // Payment successful
                                Log.d(TAG, "Payment processed successfully");

                                runOnUiThread(() -> {
                                    Log.d(TAG, "Running success UI updates on UI thread");
                                    setLoadingState(false);
                                    showPaymentSuccessAndFinish();
                                });
                            } else {
                                // API returned non-success status
                                String message = jsonResponse.optString("message",
                                        getString(R.string.payment_failed_unknown));
                                Log.e(TAG, "Payment failed with status: " + status + ", message: " + message);

                                runOnUiThread(() -> {
                                    Log.d(TAG, "Running failure UI updates on UI thread");
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
                                Log.d(TAG, "Running HTTP error UI updates on UI thread");
                                setLoadingState(false);
                                Toast.makeText(PaymentActivity.this, finalErrorMessage,
                                        Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing payment response", e);
                        runOnUiThread(() -> {
                            Log.d(TAG, "Running JSON parse error UI updates on UI thread");
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

        try {
            // Return success to previous activity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("payment_method", selectedPaymentMethod);
            resultIntent.putExtra("amount_paid", amountPaid);

            // Add discount information if a discount was applied
            if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                resultIntent.putExtra("discount_id", selectedDiscount.getId());
                resultIntent.putExtra("discount_name", selectedDiscount.getName());
                resultIntent.putExtra("discount_amount", selectedDiscount.getAmount());
                resultIntent.putExtra("discounted_total", finalAmount - discountedAmount);
            }

            // Set the result and finish THIS activity first
            setResult(RESULT_OK, resultIntent);

            // Create an intent specifically for OrderListActivity
            Intent orderListIntent = new Intent(this, OrderListActivity.class);

            // Add session ID to the intent to maintain session
            orderListIntent.putExtra("session_id", sessionId);

            // Log for debugging
            Log.d(TAG, "Navigating to OrderListActivity with session_id: " + sessionId);

            // Start the OrderListActivity
            startActivity(orderListIntent);

            // THEN finish this activity
            finish();

        } catch (Exception e) {
            Log.e(TAG, "Error in showPaymentSuccessAndFinish", e);
            // Fallback to just finishing if there's an error
            setResult(RESULT_OK);
            finish();
        }
    }

    private void setLoadingState(boolean isLoading) {
        Log.d(TAG, "Setting loading state: " + isLoading);

        // Toggle loading UI elements
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        } else {
            Log.e(TAG, "progressBar is null in setLoadingState");
        }

        if (contentView != null) {
            contentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        } else {
            Log.e(TAG, "contentView is null in setLoadingState");
        }

        if (processPaymentButton != null) {
            processPaymentButton.setEnabled(!isLoading);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(!isLoading);
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