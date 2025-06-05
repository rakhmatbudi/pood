package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";
    private static final String PAYMENT_MODES_URL = "https://api.pood.lol/payment-modes";
    private static final String CHECKOUT_API_URL = "https://api.pood.lol/payments/checkout/";
    private static final String DISCOUNTS_API_URL = "https://api.pood.lol/discounts/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private RoundingConfig roundingConfig;
    private Spinner discountSpinner;
    private TextView discountAmountTextView;
    private TextView discountedTotalTextView;
    private List<Discount> discountList = new ArrayList<>();
    private Discount selectedDiscount;
    private double discountedAmount = 0.0;

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
    private double originalAmount; // Store original amount before any discounts
    private double finalAmount; // Current final amount (after discounts)
    private long sessionId;
    private double amountPaid;
    private String selectedPaymentMethod = "cash";
    private String selectedPaymentMethodId = "1";
    private OkHttpClient client = new OkHttpClient();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private boolean isUpdatingDiscount = false; // Flag to prevent recursive calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.payment));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        initializeViews();

        // Get data from intent
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        orderId = intent.getLongExtra("order_id", -1);
        orderNumber = intent.getStringExtra("order_number");
        tableNumber = intent.getStringExtra("table_number");
        finalAmount = intent.getDoubleExtra("final_amount", 0.0);
        originalAmount = finalAmount; // Store original amount
        sessionId = intent.getLongExtra("session_id", -1);

        // Check if we have the minimum required data
        if (orderId == -1) {
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Call the checkout API to get the most current order information
        callCheckoutAPI();
    }

    private void initializeViews() {
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
    }

    private void callCheckoutAPI() {
        callCheckoutAPIWithPayload(null);
    }

    private void callCheckoutAPIWithDiscount(long discountId) {
        if (isUpdatingDiscount) {
            return; // Prevent recursive calls
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("discount_id", discountId);
            callCheckoutAPIWithPayload(payload);
        } catch (JSONException e) {
            Toast.makeText(this, "Error creating discount request", Toast.LENGTH_SHORT).show();
            revertToNoDiscount();
        }
    }

    private void callCheckoutAPIToReset() {
        if (isUpdatingDiscount) {
            return; // Prevent recursive calls
        }

        // POST with empty payload to reset/remove discount
        callCheckoutAPIWithPayload(new JSONObject());
    }

    private void callCheckoutAPIWithPayload(JSONObject payload) {
        setLoadingState(true);
        isUpdatingDiscount = true;

        String checkoutUrl = CHECKOUT_API_URL + orderId;
        String authToken = getAuthToken();

        try {
            // Use empty JSON object if payload is null
            if (payload == null) {
                payload = new JSONObject();
            }

            RequestBody body = RequestBody.create(payload.toString(), JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(checkoutUrl)
                    .post(body);

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        isUpdatingDiscount = false;
                        setLoadingState(false);
                        Toast.makeText(PaymentActivity.this,
                                "Network error. Please check your connection.",
                                Toast.LENGTH_SHORT).show();
                        // Try to continue with existing data if available
                        if (finalAmount > 0) {
                            proceedWithSetup();
                        } else {
                            finish();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                JSONObject data = jsonResponse.getJSONObject("data");

                                // Update order information from checkout API
                                if (data.has("order_number")) {
                                    orderNumber = data.optString("order_number");
                                }
                                if (data.has("table_number")) {
                                    tableNumber = data.optString("table_number");
                                }

                                // Use final_charged_amount as the primary amount
                                if (data.has("final_charged_amount")) {
                                    finalAmount = data.optDouble("final_charged_amount", 0.0);
                                } else if (data.has("final_amount")) {
                                    String finalAmountStr = data.optString("final_amount", "0");
                                    finalAmount = Double.parseDouble(finalAmountStr.replace(",", ""));
                                }

                                // Update original amount only on first load (when no discount is applied)
                                if (selectedDiscount == null || selectedDiscount.getId() == -1) {
                                    // Use total_items_amount as original amount if available, otherwise use current final amount
                                    if (data.has("total_items_amount")) {
                                        originalAmount = data.optDouble("total_items_amount", finalAmount);
                                    } else {
                                        originalAmount = finalAmount;
                                    }
                                }

                                // Get discount amount and other breakdown info
                                if (data.has("discount_amount")) {
                                    discountedAmount = data.optDouble("discount_amount", 0.0);
                                } else {
                                    discountedAmount = 0.0;
                                }

                                runOnUiThread(() -> {
                                    isUpdatingDiscount = false;
                                    setLoadingState(false);

                                    // If this is the initial load, proceed with full setup
                                    if (discountSpinner.getAdapter() == null) {
                                        proceedWithSetup();
                                    } else {
                                        // Just update the pricing display
                                        updatePricingDisplay();
                                        updateAmountPaidWithRounding();
                                        updateChangeDisplay();
                                    }
                                });
                            } else {
                                throw new JSONException("API returned non-success status: " + jsonResponse.optString("message"));
                            }
                        } else {
                            throw new IOException("HTTP error: " + response.code());
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            isUpdatingDiscount = false;
                            setLoadingState(false);
                            Toast.makeText(PaymentActivity.this,
                                    "Error loading order details: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();

                            // Try to continue with existing data if available
                            if (finalAmount > 0) {
                                if (discountSpinner.getAdapter() == null) {
                                    proceedWithSetup();
                                } else {
                                    revertToNoDiscount();
                                }
                            } else {
                                finish();
                            }
                        });
                    }
                }
            });

        } catch (Exception e) {
            isUpdatingDiscount = false;
            setLoadingState(false);
            Toast.makeText(this, "Error creating checkout request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (finalAmount > 0) {
                if (discountSpinner.getAdapter() == null) {
                    proceedWithSetup();
                } else {
                    revertToNoDiscount();
                }
            } else {
                finish();
            }
        }
    }

    private void revertToNoDiscount() {
        // Reset discount-related variables
        selectedDiscount = null;
        discountedAmount = 0.0;
        finalAmount = originalAmount; // Reset to original amount

        // Update UI to reflect no discount
        updatePricingDisplay();
        updateAmountPaidWithRounding();
        updateChangeDisplay();

        // Reset spinner to first item (No Discount) without triggering the listener
        if (discountSpinner != null && discountSpinner.getAdapter() != null) {
            discountSpinner.setOnItemSelectedListener(null);
            discountSpinner.setSelection(0);
            setupDiscountSpinnerListener();
        }
    }

    private void proceedWithSetup() {
        setupUI();
        setupListeners();
        fetchDiscounts();
        fetchPaymentMethods();
        fetchRoundingConfig();
    }

    private void updateAmountPaidWithRounding() {
        if (roundingConfig == null) {
            return;
        }

        double amountToPay = finalAmount;
        if (selectedDiscount != null && selectedDiscount.getId() != -1) {
            amountToPay = finalAmount;
        }

        double roundedAmount = applyRounding(amountToPay);
        amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
        updateChangeDisplay();
    }

    private double applyRounding(double amount) {
        if (amount < 0 || roundingConfig == null) {
            return amount;
        }

        int roundingBelow = roundingConfig.getRoundingBelow();
        int roundingNumber = roundingConfig.getRoundingNumber();

        long amountInt = Math.round(amount);
        long remainder = amountInt % roundingNumber;
        long roundedAmount;

        if (remainder <= roundingBelow) {
            roundedAmount = amountInt - remainder;
        } else {
            roundedAmount = amountInt + (roundingNumber - remainder);
        }

        return roundedAmount;
    }

    private void fetchRoundingConfig() {
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(ROUNDING_API_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                    if (amountPaidEditText != null) {
                        updateAmountPaidWithRounding();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject config = dataArray.getJSONObject(0);

                            int roundingBelow = config.optInt("rounding_below", 99);
                            int roundingDigit = config.optInt("rounding_digit", 1);
                            String description = config.optString("rounding_digit_description", "00 - Hundreds");
                            int roundingNumber = config.optInt("rounding_number", 100);

                            roundingConfig = new RoundingConfig(roundingBelow, roundingDigit, description, roundingNumber);
                        } else {
                            roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                        }

                        runOnUiThread(() -> {
                            if (amountPaidEditText != null) {
                                updateAmountPaidWithRounding();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                            if (amountPaidEditText != null) {
                                updateAmountPaidWithRounding();
                            }
                        });
                    }
                } catch (Exception e) {
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

    private void fetchDiscounts() {
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(DISCOUNTS_API_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
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

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        List<Discount> discounts = parseDiscountsFromJson(jsonResponse);

                        runOnUiThread(() -> {
                            setupDiscountSpinner(discounts);
                        });
                    } else {
                        runOnUiThread(() -> {
                            setupDiscountSpinner(new ArrayList<>());
                            Toast.makeText(PaymentActivity.this,
                                    R.string.error_loading_discounts,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
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
            }
        }

        return discounts;
    }

    private void setupDiscountSpinner(List<Discount> discounts) {
        discountList.clear();
        discountList.addAll(discounts);

        DiscountSpinnerAdapter adapter = new DiscountSpinnerAdapter(this, discountList);
        discountSpinner.setAdapter(adapter);

        setupDiscountSpinnerListener();
    }

    private void setupDiscountSpinnerListener() {
        discountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUpdatingDiscount) {
                    return; // Prevent recursive calls during programmatic selection
                }

                Discount newSelectedDiscount = (Discount) parent.getItemAtPosition(position);

                // Check if the selection actually changed
                if ((selectedDiscount == null && newSelectedDiscount != null && newSelectedDiscount.getId() == -1) ||
                        (selectedDiscount != null && newSelectedDiscount != null && selectedDiscount.getId() == newSelectedDiscount.getId())) {
                    return; // No actual change
                }

                selectedDiscount = newSelectedDiscount;

                if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                    // Call checkout API with discount
                    callCheckoutAPIWithDiscount(selectedDiscount.getId());
                } else {
                    // No discount selected - call checkout API with empty payload to reset
                    callCheckoutAPIToReset();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (!isUpdatingDiscount) {
                    selectedDiscount = null;
                    callCheckoutAPIToReset();
                }
            }
        });
    }

    private void updatePricingDisplay() {
        if (selectedDiscount == null || selectedDiscount.getId() == -1) {
            // No discount selected
            discountAmountTextView.setVisibility(View.GONE);
            discountedTotalTextView.setVisibility(View.GONE);

            String formattedTotal = formatPriceWithCurrency(finalAmount);
            orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));
            orderTotalTextView.setPaintFlags(orderTotalTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaidEditText.isEnabled()) {
                if (roundingConfig != null) {
                    double roundedAmount = applyRounding(finalAmount);
                    amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
                } else {
                    amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
                }
            }
        } else {
            // Discount applied - show original amount with strikethrough and final charged amount
            discountAmountTextView.setVisibility(View.VISIBLE);

            // Show discount info if available
            String discountText = getString(R.string.discount_applied,
                    selectedDiscount.getName(), selectedDiscount.getAmount());
            if (discountedAmount > 0) {
                discountText += " (-" + formatPriceWithCurrency(discountedAmount) + ")";
            }
            discountAmountTextView.setText(discountText);

            String totalLabel = getString(R.string.order_total_format, "").replace("%s", "");
            String formattedOriginalPrice = formatPriceWithCurrency(originalAmount);
            String formattedFinalPrice = formatPriceWithCurrency(finalAmount);

            SpannableString spannableString = new SpannableString(
                    totalLabel + formattedOriginalPrice + "  " + formattedFinalPrice);

            spannableString.setSpan(new StrikethroughSpan(),
                    totalLabel.length(),
                    totalLabel.length() + formattedOriginalPrice.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            orderTotalTextView.setText(spannableString);
            discountedTotalTextView.setVisibility(View.GONE);

            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaidEditText.isEnabled()) {
                if (roundingConfig != null) {
                    double roundedAmount = applyRounding(finalAmount);
                    amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
                } else {
                    amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
                }
            }
        }
    }

    private void setupUI() {
        orderNumberTextView.setText(getString(R.string.order_number_format, orderNumber));
        tableNumberTextView.setText(getString(R.string.table_number_format, tableNumber));

        String formattedTotal = formatPriceWithCurrency(finalAmount);
        orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));

        if (roundingConfig != null) {
            double roundedAmount = applyRounding(finalAmount);
            amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
        } else {
            amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
        }

        updateChangeDisplay();
    }

    private void setupListeners() {
        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < paymentMethodRadioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) paymentMethodRadioGroup.getChildAt(i);
                if (radioButton.getId() == checkedId) {
                    String paymentMethodId = (String) radioButton.getTag();

                    for (PaymentMethod method : paymentMethods) {
                        if (method.getId().equals(paymentMethodId)) {
                            selectedPaymentMethod = method.getCode();
                            selectedPaymentMethodId = method.getId();

                            if ("cash".equalsIgnoreCase(method.getCode())) {
                                amountPaidEditText.setEnabled(true);
                                updateAmountPaidWithRounding();
                            } else {
                                double amountToPay = finalAmount;
                                amountPaidEditText.setText(String.valueOf(Math.round(amountToPay)));
                                amountPaidEditText.setEnabled(false);
                            }

                            updateChangeDisplay();
                            break;
                        }
                    }
                    break;
                }
            }
        });

        amountPaidEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateChangeDisplay();
            }
        });

        cancelButton.setOnClickListener(v -> finish());
        processPaymentButton.setOnClickListener(v -> validateAndProcessPayment());
    }

    private void fetchPaymentMethods() {
        setLoadingState(true);

        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(PAYMENT_MODES_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    createDefaultPaymentMethods();
                    populatePaymentMethodsUI();
                    setLoadingState(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        JSONArray dataArray = jsonResponse.getJSONArray("data");

                        paymentMethods.clear();
                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject methodJson = dataArray.getJSONObject(i);

                            long id = methodJson.getLong("id");
                            long typeId = methodJson.getLong("payment_mode_type_id");
                            String description = methodJson.getString("description");
                            boolean isActive = methodJson.getBoolean("is_active");

                            if (!isActive) {
                                continue;
                            }

                            String code;
                            switch ((int) typeId) {
                                case 1:
                                    code = "cash";
                                    break;
                                case 2:
                                    code = "card";
                                    break;
                                case 3:
                                    code = "transfer";
                                    break;
                                default:
                                    code = "other";
                                    break;
                            }

                            PaymentMethod method = new PaymentMethod(String.valueOf(id), description, code);
                            paymentMethods.add(method);
                        }

                        if (paymentMethods.isEmpty()) {
                            runOnUiThread(() -> {
                                createDefaultPaymentMethods();
                                populatePaymentMethodsUI();
                                setLoadingState(false);
                            });
                            return;
                        }

                        runOnUiThread(() -> {
                            populatePaymentMethodsUI();
                            setLoadingState(false);
                        });
                    } else {
                        runOnUiThread(() -> {
                            createDefaultPaymentMethods();
                            populatePaymentMethodsUI();
                            setLoadingState(false);
                        });
                    }
                } catch (Exception e) {
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
        paymentMethods.clear();
        paymentMethods.add(new PaymentMethod("1", "Cash", "cash"));
        paymentMethods.add(new PaymentMethod("2", "Credit/Debit Card", "card"));
        paymentMethods.add(new PaymentMethod("3", "Mobile Payment", "mobile"));
    }

    private void populatePaymentMethodsUI() {
        paymentMethodRadioGroup.removeAllViews();

        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod method = paymentMethods.get(i);

            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(method.getName());
            radioButton.setTag(method.getId());
            radioButton.setPadding(32, 30, 32, 30);

            paymentMethodRadioGroup.addView(radioButton);

            if (i == 0) {
                radioButton.setChecked(true);
                selectedPaymentMethod = method.getCode();
                selectedPaymentMethodId = method.getId();

                if (!"cash".equalsIgnoreCase(method.getCode())) {
                    amountPaidEditText.setEnabled(false);
                }
            }
        }

        updateChangeDisplay();
    }

    private void updateChangeDisplay() {
        try {
            String amountText = amountPaidEditText.getText() != null ?
                    amountPaidEditText.getText().toString() : "0";

            if (amountText.isEmpty()) {
                amountText = "0";
            }

            amountPaid = Double.parseDouble(amountText);

            double amountToPay = finalAmount;

            double amountToCompare = amountToPay;
            if ("cash".equalsIgnoreCase(selectedPaymentMethod) && roundingConfig != null) {
                amountToCompare = applyRounding(amountToPay);
            }

            double change = amountPaid - amountToPay;

            String formattedChange = formatPriceWithCurrency(Math.max(0, change));
            changeTextView.setText(getString(R.string.change_format, formattedChange));

            if (amountPaid < amountToCompare && "cash".equalsIgnoreCase(selectedPaymentMethod)) {
                changeTextView.setTextColor(getResources().getColor(R.color.colorError));
                processPaymentButton.setEnabled(false);
            } else {
                changeTextView.setTextColor(getResources().getColor(R.color.colorNormal));
                processPaymentButton.setEnabled(true);
            }
        } catch (NumberFormatException e) {
            changeTextView.setText(getString(R.string.invalid_amount));
            changeTextView.setTextColor(getResources().getColor(R.color.colorError));
            processPaymentButton.setEnabled(false);
        }
    }

    private void validateAndProcessPayment() {
        double amountToPay = finalAmount;

        double amountToValidate = amountToPay;
        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && roundingConfig != null) {
            amountToValidate = applyRounding(amountToPay);
        }

        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && amountPaid < amountToValidate) {
            Toast.makeText(this, R.string.insufficient_payment, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);
        processPayment();
    }

    private void processPayment() {
        try {
            setLoadingState(true);

            JSONObject paymentData = new JSONObject();
            paymentData.put("order_id", orderId);

            double amountToPay = finalAmount;
            if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                paymentData.put("discount_id", selectedDiscount.getId());
            }

            paymentData.put("amount", amountToPay);
            paymentData.put("payment_mode", Integer.parseInt(selectedPaymentMethodId));
            paymentData.put("transaction_id", null);

            String notes = paymentNotesEditText.getText() != null ?
                    paymentNotesEditText.getText().toString().trim() : "";
            if (!notes.isEmpty()) {
                paymentData.put("notes", notes);
            }

            String apiUrl = "https://api.pood.lol/payments";
            RequestBody body = RequestBody.create(paymentData.toString(), JSON);
            String authToken = getAuthToken();

            Request.Builder requestBuilder = new Request.Builder()
                    .url(apiUrl)
                    .post(body);

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoadingState(false);
                        Toast.makeText(PaymentActivity.this,
                                "Network error. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                runOnUiThread(() -> {
                                    setLoadingState(false);
                                    showPaymentSuccessAndFinish();
                                });
                            } else {
                                String message = jsonResponse.optString("message", "Payment failed");
                                runOnUiThread(() -> {
                                    setLoadingState(false);
                                    Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_SHORT).show();
                                });
                            }
                        } else {
                            String errorMessage;
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("message", "Payment failed");
                            } catch (JSONException e) {
                                errorMessage = "Payment failed";
                            }

                            final String finalErrorMessage = errorMessage;
                            runOnUiThread(() -> {
                                setLoadingState(false);
                                Toast.makeText(PaymentActivity.this, finalErrorMessage, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(PaymentActivity.this,
                                    "Error processing payment response",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        } catch (Exception e) {
            setLoadingState(false);
            Toast.makeText(this, "Error creating payment request", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPaymentSuccessAndFinish() {
        Toast.makeText(this, R.string.payment_success, Toast.LENGTH_SHORT).show();

        try {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("payment_method", selectedPaymentMethod);
            resultIntent.putExtra("amount_paid", amountPaid);

            if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                resultIntent.putExtra("discount_id", selectedDiscount.getId());
                resultIntent.putExtra("discount_name", selectedDiscount.getName());
                resultIntent.putExtra("discount_amount", selectedDiscount.getAmount());
                resultIntent.putExtra("discounted_total", finalAmount);
            }

            setResult(RESULT_OK, resultIntent);

            Intent orderListIntent = new Intent(this, OrderListActivity.class);
            orderListIntent.putExtra("session_id", sessionId);
            startActivity(orderListIntent);
            finish();

        } catch (Exception e) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (contentView != null) {
            contentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }

        if (processPaymentButton != null) {
            processPaymentButton.setEnabled(!isLoading);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(!isLoading);
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