package com.restaurant.management;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.helpers.PaymentApiHelper;
import com.restaurant.management.helpers.PaymentUIHelper;
import com.restaurant.management.models.Discount;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.RoundingConfig;
import com.restaurant.management.printing.PrintTemplateManager;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class PaymentActivity extends AppCompatActivity implements
        PaymentUIHelper.DiscountSelectionListener,
        PaymentUIHelper.PaymentMethodSelectionListener {

    private static final int BLUETOOTH_PERMISSION_REQUEST = 104;
    private static final String TAG = "PaymentActivity";

    private double taxRate = 0;
    private String taxDescription = "Pajak Restoran (PB1)";
    private double serviceRate = 0;
    private String serviceDescription = "Service Charge";
    private boolean ratesLoaded = false;

    // Thermal printer constants
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int CHAR_WIDTH = 32;

    // ESC/POS Commands
    private static final byte[] ESC_INIT = {0x1B, 0x40};
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] ESC_DOUBLE_HEIGHT = {0x1B, 0x21, 0x10};
    private static final byte[] ESC_NORMAL_SIZE = {0x1B, 0x21, 0x00};
    private static final byte[] ESC_CUT_PAPER = {0x1D, 0x56, 0x42, 0x00};
    private static final byte[] ESC_FEED_LINE = {0x0A};
    private static final String SEPARATOR_LINE = "--------------------------------";

    // Helper classes
    private PaymentApiHelper apiHelper;
    private PaymentUIHelper uiHelper;

    // Printing component
    private PrintTemplateManager templateManager;

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

    // Thermal printer fields
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

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

        // Initialize Bluetooth
        initializeBluetooth();

        // Fetch tax and service rates from API
        fetchTaxAndServiceRates();

        // Initialize template manager
        templateManager = new PrintTemplateManager(this);

        // Get data from intent and validate
        if (!getAndValidateIntentData()) {
            return;
        }

        // Call the checkout API to get the most current order information
        callCheckoutAPI();
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

    private void fetchTaxAndServiceRates() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.pood.lol/taxes/rates");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    parseRatesResponse(response.toString());
                } else {
                    runOnUiThread(() -> {
                        setDefaultRates();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setDefaultRates();
                });
            }
        }).start();
    }

    private void parseRatesResponse(String jsonResponse) {
        try {
            JSONObject root = new JSONObject(jsonResponse);
            String status = root.getString("status");

            if ("success".equals(status)) {
                JSONArray dataArray = root.getJSONArray("data");

                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject item = dataArray.getJSONObject(i);
                    int id = item.getInt("id");
                    String description = item.getString("description");
                    double amount = item.getDouble("amount");

                    if (id == 1) { // Tax
                        taxRate = amount / 100.0; // Convert percentage to decimal
                        taxDescription = description;
                    } else if (id == 2) { // Service Charge
                        serviceRate = amount / 100.0; // Convert percentage to decimal
                        serviceDescription = description;
                    }
                }

                runOnUiThread(() -> {
                    ratesLoaded = true;
                });
            } else {
                runOnUiThread(() -> {
                    setDefaultRates();
                });
            }
        } catch (JSONException e) {
            runOnUiThread(() -> {
                setDefaultRates();
            });
        }
    }

    private void setDefaultRates() {
        taxRate = 0.10; // 10% default
        taxDescription = "Tax";
        serviceRate = 0.02; // 2% default
        serviceDescription = "Service Charge";
        ratesLoaded = true;
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

                    // Update order information from API response
                    if (response.orderNumber != null && !response.orderNumber.isEmpty()) {
                        orderNumber = response.orderNumber;
                    }
                    if (response.tableNumber != null && !response.tableNumber.isEmpty()) {
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

        // Ask user if they want to print receipt
        showReceiptPrintDialog();
    }

    private void showReceiptPrintDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Print Receipt")
                .setMessage("Payment successful! Would you like to print a receipt?")
                .setPositiveButton("Print Receipt", (dialog, which) -> {
                    printReceipt();
                })
                .setNegativeButton("Skip", (dialog, which) -> {
                    finishPaymentActivity();
                })
                .setCancelable(false)
                .show();
    }

    private void printReceipt() {
        // Check if rates are loaded, if not wait a bit or use defaults
        if (!ratesLoaded) {
            setDefaultRates();
        }

        if (!checkBluetoothAndEnable()) {
            // If Bluetooth not available, just finish
            finishPaymentActivity();
            return;
        }

        showPrinterSelection();
    }

    private boolean checkBluetoothAndEnable() {
        try {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check runtime permissions for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasBluetoothPermissions()) {
                    requestBluetoothPermissions();
                    return false;
                }
            } else {
                if (!hasLocationPermission()) {
                    requestLocationPermission();
                    return false;
                }
            }

            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Please enable Bluetooth and try again", Toast.LENGTH_LONG).show();
                return false;
            }

            return true;

        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Toast.makeText(this, "Bluetooth error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    BLUETOOTH_PERMISSION_REQUEST);
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                BLUETOOTH_PERMISSION_REQUEST);
    }

    private void showPrinterSelection() {
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.isEmpty()) {
                Toast.makeText(this, "No paired printers found. Skipping receipt printing.", Toast.LENGTH_LONG).show();
                finishPaymentActivity();
                return;
            }

            String[] deviceNames = new String[pairedDevices.size()];
            BluetoothDevice[] devices = new BluetoothDevice[pairedDevices.size()];

            int i = 0;
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName() != null ? device.getName() : "Unknown Device";
                deviceNames[i] = deviceName + "\n" + device.getAddress();
                devices[i] = device;
                i++;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select Receipt Printer")
                    .setItems(deviceNames, (dialog, which) -> {
                        connectAndPrintReceipt(devices[which]);
                    })
                    .setNegativeButton("Skip", (dialog, which) -> {
                        finishPaymentActivity();
                    })
                    .setCancelable(false)
                    .show();

        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            finishPaymentActivity();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finishPaymentActivity();
        }
    }

    private void connectAndPrintReceipt(BluetoothDevice device) {
        new Thread(() -> {
            try {
                connectToPrinter(device);
                printThermalReceipt();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Receipt printed successfully", Toast.LENGTH_SHORT).show();
                    finishPaymentActivity();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to print receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finishPaymentActivity();
                });
            } finally {
                disconnectPrinter();
            }
        }).start();
    }

    private void connectToPrinter(BluetoothDevice device) throws IOException {
        bluetoothSocket = device.createRfcommSocketToServiceRecord(PRINTER_UUID);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
    }

    private String getPaymentMethodForPrint() {
        // If selectedPaymentMethod is already set from onPaymentMethodSelected, use it
        if (selectedPaymentMethod != null && !selectedPaymentMethod.isEmpty() &&
                !selectedPaymentMethod.equals("cash") && !selectedPaymentMethod.equals("card")) {
            return selectedPaymentMethod;
        }

        // Otherwise, try to find the payment method by ID
        if (paymentMethods != null && selectedPaymentMethodId != null) {
            for (PaymentMethod method : paymentMethods) {
                if (method.getId().equals(selectedPaymentMethodId)) {
                    return method.getDescription();
                }
            }
        }

        return "Cash Sales"; // Ultimate fallback
    }

    private void printThermalReceipt() throws IOException {
        // Check if rates are loaded, if not wait a bit or use defaults
        if (!ratesLoaded) {
            setDefaultRates();
        }

        // Get discount info
        String discountName = null;
        if (selectedDiscount != null && selectedDiscount.getId() != -1) {
            discountName = selectedDiscount.getName();
        }

        // Get the payment method description for printing
        String paymentMethodForPrint = getPaymentMethodForPrint();

        Log.d("PaymentActivity", "Printing receipt with payment method: '" + paymentMethodForPrint + "'");

        // Use template manager to print receipt
        templateManager.printPaymentReceipt(
                outputStream,
                orderNumber,
                tableNumber,
                originalAmount,
                finalAmount,
                discountedAmount,
                discountName,
                paymentMethodForPrint,
                amountPaid,
                taxRate,
                taxDescription,
                serviceRate,
                serviceDescription
        );
    }
    private void printLine(String text) throws IOException {
        outputStream.write(text.getBytes("UTF-8"));
        outputStream.write(ESC_FEED_LINE);
    }

    private String formatTotalLine(String label, String amount) {
        int labelWidth = CHAR_WIDTH - amount.length();
        return String.format("%-" + labelWidth + "s%s", label, amount);
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0f", amount);
    }

    private String formatDateTime(Date date) {
        return new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(date);
    }

    private void disconnectPrinter() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
        } catch (IOException e) {
            // Silent cleanup
        }
    }

    private void finishPaymentActivity() {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                Toast.makeText(this, "Permissions granted. Retrying print...", Toast.LENGTH_SHORT).show();
                printReceipt();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for printing", Toast.LENGTH_LONG).show();
                finishPaymentActivity();
            }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectPrinter();
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
        // Store the description (not the code) for printing
        selectedPaymentMethod = method.getDescription(); // This will be "Cash Sales", "EDC BRI", etc.
        selectedPaymentMethodId = method.getId();

        Log.d("PaymentActivity", "Selected payment method: '" + selectedPaymentMethod + "' (ID: " + selectedPaymentMethodId + ")");

        // Check if it's cash payment (case insensitive)
        String description = method.getDescription().toLowerCase();
        if (description.contains("cash")) {
            amountPaidEditText.setEnabled(true);
            updateAmountPaidWithRounding();
        } else {
            amountPaidEditText.setText(String.valueOf(Math.round(finalAmount)));
            amountPaidEditText.setEnabled(false);
        }

        updateChangeDisplay();
    }
}