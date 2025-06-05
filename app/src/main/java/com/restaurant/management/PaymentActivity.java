package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.helpers.PaymentApiHelper;
import com.restaurant.management.helpers.PaymentUIHelper;
import com.restaurant.management.models.Discount;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.RoundingConfig;

import java.util.ArrayList;
import java.util.List;

public class PaymentActivity extends AppCompatActivity implements
        PaymentUIHelper.DiscountSelectionListener,
        PaymentUIHelper.PaymentMethodSelectionListener {

    // Helper classes
    private PaymentApiHelper apiHelper;
    private PaymentUIHelper uiHelper;

    // UI Components
    private TextView orderNumberTextView;
    private TextView tableNumberTextView;
    private TextView orderTotalTextView;
    private TextView changeTextView;
    private TextView discountAmountTextView;
    private TextView discountedTotalTextView;
    private RadioGroup paymentMethodRadioGroup;
    private TextInputEditText amountPaidEditText;
    private TextInputEditText paymentNotesEditText;
    private Button cancelButton;
    private Button processPaymentButton;
    private ProgressBar progressBar;
    private View contentView;
    private Spinner discountSpinner;

    // Data variables
    private long orderId;
    private String orderNumber;
    private String tableNumber;
    private double originalAmount;
    private double finalAmount;
    private long sessionId;
    private double amountPaid;
    private String selectedPaymentMethod = "cash";
    private String selectedPaymentMethodId = "1";
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private List<Discount> discountList = new ArrayList<>();
    private Discount selectedDiscount;
    private double discountedAmount = 0.0;
    private RoundingConfig roundingConfig;
    private boolean isUpdatingDiscount = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize helpers
        String authToken = getAuthToken();
        apiHelper = new PaymentApiHelper(this, authToken);
        uiHelper = new PaymentUIHelper(this);

        // Initialize toolbar
        setupToolbar();

        // Initialize views
        initializeViews();

        // Get data from intent and validate
        if (!getAndValidateIntentData()) {
            return;
        }

        // Call the checkout API to get the most current order information
        callCheckoutAPI();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.payment));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        discountSpinner = findViewById(R.id.discount_spinner);
        discountAmountTextView = findViewById(R.id.discount_amount_text_view);
        discountedTotalTextView = findViewById(R.id.discounted_total_text_view);
    }

    private boolean getAndValidateIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }

        orderId = intent.getLongExtra("order_id", -1);
        orderNumber = intent.getStringExtra("order_number");
        tableNumber = intent.getStringExtra("table_number");
        finalAmount = intent.getDoubleExtra("final_amount", 0.0);
        originalAmount = finalAmount;
        sessionId = intent.getLongExtra("session_id", -1);

        if (orderId == -1) {
            Toast.makeText(this, R.string.invalid_order_data, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        return true;
    }

    private void callCheckoutAPI() {
        callCheckoutAPIWithDiscount(null);
    }

    private void callCheckoutAPIWithDiscount(Long discountId) {
        if (isUpdatingDiscount) {
            return;
        }

        setLoadingState(true);
        isUpdatingDiscount = true;

        apiHelper.callCheckout(orderId, discountId, new PaymentApiHelper.CheckoutCallback() {
            @Override
            public void onSuccess(PaymentApiHelper.CheckoutResponse response) {
                runOnUiThread(() -> {
                    isUpdatingDiscount = false;
                    setLoadingState(false);

                    // Update order information
                    if (response.orderNumber != null) {
                        orderNumber = response.orderNumber;
                    }
                    if (response.tableNumber != null) {
                        tableNumber = response.tableNumber;
                    }

                    finalAmount = response.finalAmount;
                    discountedAmount = response.discountAmount;

                    // Update original amount only on first load (when no discount is applied)
                    if (selectedDiscount == null || selectedDiscount.getId() == -1) {
                        originalAmount = response.originalAmount;
                    }

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
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isUpdatingDiscount = false;
                    setLoadingState(false);
                    Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_SHORT).show();

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
        });
    }

    private void revertToNoDiscount() {
        selectedDiscount = null;
        discountedAmount = 0.0;
        finalAmount = originalAmount;

        updatePricingDisplay();
        updateAmountPaidWithRounding();
        updateChangeDisplay();

        uiHelper.resetDiscountSpinner(discountSpinner);
        setupDiscountSpinnerListener();
    }

    private void proceedWithSetup() {
        setupUI();
        setupListeners();
        fetchDiscounts();
        fetchPaymentMethods();
        fetchRoundingConfig();
    }

    private void setupUI() {
        orderNumberTextView.setText(getString(R.string.order_number_format, orderNumber));
        tableNumberTextView.setText(getString(R.string.table_number_format, tableNumber));

        String formattedTotal = uiHelper.formatPriceWithCurrency(finalAmount);
        orderTotalTextView.setText(getString(R.string.order_total_format, formattedTotal));

        updateAmountPaidWithRounding();
        updateChangeDisplay();
    }

    private void setupListeners() {
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

    private void fetchRoundingConfig() {
        apiHelper.fetchRoundingConfig(new PaymentApiHelper.RoundingConfigCallback() {
            @Override
            public void onSuccess(RoundingConfig config) {
                runOnUiThread(() -> {
                    roundingConfig = config;
                    updateAmountPaidWithRounding();
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    roundingConfig = new RoundingConfig(99, 1, "00 - Hundreds", 100);
                    updateAmountPaidWithRounding();
                });
            }
        });
    }

    private void fetchDiscounts() {
        apiHelper.fetchDiscounts(new PaymentApiHelper.DiscountsCallback() {
            @Override
            public void onSuccess(List<Discount> discounts) {
                runOnUiThread(() -> {
                    discountList.clear();
                    discountList.addAll(discounts);
                    setupDiscountSpinner();
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    setupDiscountSpinner();
                    Toast.makeText(PaymentActivity.this, R.string.error_loading_discounts, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchPaymentMethods() {
        setLoadingState(true);

        apiHelper.fetchPaymentMethods(new PaymentApiHelper.PaymentMethodsCallback() {
            @Override
            public void onSuccess(List<PaymentMethod> methods) {
                runOnUiThread(() -> {
                    paymentMethods.clear();
                    paymentMethods.addAll(methods);
                    populatePaymentMethodsUI();
                    setLoadingState(false);
                });
            }

            @Override
            public void onError() {
                runOnUiThread(() -> {
                    paymentMethods.clear();
                    paymentMethods.addAll(PaymentApiHelper.getDefaultPaymentMethods());
                    populatePaymentMethodsUI();
                    setLoadingState(false);
                });
            }
        });
    }

    private void setupDiscountSpinner() {
        uiHelper.setupDiscountSpinner(discountSpinner, discountList, this);
    }

    private void setupDiscountSpinnerListener() {
        uiHelper.setupDiscountSpinner(discountSpinner, discountList, this);
    }

    private void populatePaymentMethodsUI() {
        uiHelper.populatePaymentMethodsUI(paymentMethodRadioGroup, paymentMethods, this);
        updateChangeDisplay();
    }

    private void updatePricingDisplay() {
        uiHelper.updatePricingDisplay(orderTotalTextView, discountAmountTextView, discountedTotalTextView,
                selectedDiscount, originalAmount, finalAmount, discountedAmount);
    }

    private void updateAmountPaidWithRounding() {
        if (roundingConfig == null) {
            return;
        }

        double amountToPay = finalAmount;
        double roundedAmount = uiHelper.applyRounding(amountToPay, roundingConfig);
        amountPaidEditText.setText(String.valueOf(Math.round(roundedAmount)));
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

            uiHelper.updateChangeDisplay(changeTextView, amountPaid, finalAmount,
                    selectedPaymentMethod, roundingConfig, this);

            boolean isValid = uiHelper.isPaymentValid(amountPaid, finalAmount, selectedPaymentMethod, roundingConfig);
            processPaymentButton.setEnabled(isValid);

        } catch (NumberFormatException e) {
            changeTextView.setText(getString(R.string.invalid_amount));
            changeTextView.setTextColor(getResources().getColor(R.color.colorError));
            processPaymentButton.setEnabled(false);
        }
    }

    private void validateAndProcessPayment() {
        boolean isValid = uiHelper.isPaymentValid(amountPaid, finalAmount, selectedPaymentMethod, roundingConfig);

        if (!isValid) {
            Toast.makeText(this, R.string.insufficient_payment, Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);
        processPayment();
    }

    private void processPayment() {
        Long discountId = (selectedDiscount != null && selectedDiscount.getId() != -1)
                ? selectedDiscount.getId() : null;

        String notes = paymentNotesEditText.getText() != null ?
                paymentNotesEditText.getText().toString().trim() : "";

        apiHelper.processPayment(orderId, discountId, finalAmount, selectedPaymentMethodId, notes,
                new PaymentApiHelper.PaymentCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            showPaymentSuccessAndFinish();
                        });
                    }

                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            setLoadingState(false);
                            Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
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
        uiHelper.setLoadingState(progressBar, contentView, processPaymentButton, cancelButton, isLoading);
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

    // PaymentUIHelper.DiscountSelectionListener implementation
    @Override
    public void onDiscountSelected(Discount discount) {
        if (isUpdatingDiscount) {
            return;
        }

        selectedDiscount = discount;
        callCheckoutAPIWithDiscount(discount.getId());
    }

    @Override
    public void onDiscountRemoved() {
        if (isUpdatingDiscount) {
            return;
        }

        selectedDiscount = null;
        callCheckoutAPIWithDiscount(null);
    }

    // PaymentUIHelper.PaymentMethodSelectionListener implementation
    @Override
    public void onPaymentMethodSelected(PaymentMethod method) {
        selectedPaymentMethod = method.getCode();
        selectedPaymentMethodId = method.getId();

        if ("cash".equalsIgnoreCase(method.getCode())) {
            amountPaidEditText.setEnabled(true);
            updateAmountPaidWithRounding();
        } else {
            amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
            amountPaidEditText.setEnabled(false);
        }

        updateChangeDisplay();
    }
}