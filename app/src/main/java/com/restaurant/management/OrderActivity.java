package com.restaurant.management;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.restaurant.management.helpers.OrderApiHelper;
import com.restaurant.management.helpers.OrderUiHelper;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;
import com.restaurant.management.printing.PrintTemplateManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class OrderActivity extends AppCompatActivity {
    private static final int CANCEL_ITEM_REQUEST_CODE = 200;
    private static final int ADD_ITEM_REQUEST_CODE = 100;
    private static final int PAYMENT_REQUEST_CODE = 101;
    private static final int BLUETOOTH_ENABLE_REQUEST = 102;
    private static final int BLUETOOTH_PERMISSION_REQUEST = 103;
    private static final String TAG = "OrderActivity";

    private PrintTemplateManager templateManager;
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

    // Existing fields
    private ProgressBar progressBar;
    private View contentLayout;
    private Order order;
    private long orderId = -1;
    private long sessionId = -1;
    private OrderApiHelper apiHelper;
    private OrderUiHelper uiHelper;

    // Thermal printer fields
    private Button printBillButton;
    private Button printCheckerButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    // Track what action to perform after permissions are granted
    private boolean pendingBillPrint = false;
    private boolean pendingCheckerPrint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        initializeHelpers();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        initializeBluetooth();

        // Initialize template manager
        templateManager = new PrintTemplateManager(this);

        // Fetch tax and service rates from API
        fetchTaxAndServiceRates();

        orderId = getIntent().getLongExtra("order_id", -1);
        sessionId = getIntent().getLongExtra("session_id", -1);

        if (orderId == -1 || sessionId == -1) {
            Toast.makeText(this, R.string.order_not_found, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchOrderDetails();
    }

    private void initializeHelpers() {
        apiHelper = new OrderApiHelper(this);
        uiHelper = new OrderUiHelper(this);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.order_details));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        // Initialize print buttons
        printBillButton = findViewById(R.id.print_bill_button);
        printCheckerButton = findViewById(R.id.print_checker_button);

        uiHelper.initializeViews(findViewById(android.R.id.content));
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Disable print buttons if Bluetooth not supported
            if (printBillButton != null) printBillButton.setEnabled(false);
            if (printCheckerButton != null) printCheckerButton.setEnabled(false);
        }
    }

    private void setupClickListeners() {
        uiHelper.setClickListeners(
                v -> navigateToAddItem(),
                v -> navigateToPayment(),
                v -> showCancelOrderDialog()
        );

        // Add print button listeners
        if (printBillButton != null) {
            printBillButton.setOnClickListener(v -> printBill());
        }

        if (printCheckerButton != null) {
            printCheckerButton.setOnClickListener(v -> printChecker());
        }
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
                        // Use default values if API fails
                        setDefaultRates();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    // Use default values if API fails
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

    // Thermal Printing Implementation
    private void printBill() {
        try {
            if (order == null) {
                Toast.makeText(this, "Order data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if rates are loaded, if not wait a bit or use defaults
            if (!ratesLoaded) {
                setDefaultRates();
            }

            pendingBillPrint = true;
            pendingCheckerPrint = false;

            if (!checkBluetoothAndEnable()) {
                return;
            }

            showPrinterSelection(true);

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void printChecker() {
        try {
            if (order == null) {
                Toast.makeText(this, "Order data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set flag for after permissions are granted
            pendingBillPrint = false;
            pendingCheckerPrint = true;

            if (!checkBluetoothAndEnable()) {
                return;
            }

            showPrinterSelection(false); // false for checker

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean checkBluetoothAndEnable() {
        try {
            // Check if Bluetooth is supported
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check runtime permissions for Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasBluetoothPermissions()) {
                    requestBluetoothPermissions();
                    return false;
                }
            } else {
                // For older Android versions, check location permission (required for Bluetooth discovery)
                if (!hasLocationPermission()) {
                    requestLocationPermission();
                    return false;
                }
            }

            // Check if Bluetooth is enabled
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);
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
        return true; // Older versions don't need these permissions
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

    private void showPrinterSelection(boolean isBill) {
        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices.isEmpty()) {
                Toast.makeText(this, "No paired printers found. Please pair a printer first.", Toast.LENGTH_LONG).show();
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

            String title = isBill ? "Select Printer for Bill" : "Select Printer for Kitchen Checker";

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setItems(deviceNames, (dialog, which) -> {
                        if (isBill) {
                            connectAndPrintBill(devices[which]);
                        } else {
                            connectAndPrintChecker(devices[which]);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectAndPrintBill(BluetoothDevice device) {
        new Thread(() -> {
            try {
                connectToPrinter(device);
                printThermalBill();
                runOnUiThread(() -> Toast.makeText(this, "Bill printed successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to print bill: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                disconnectPrinter();
            }
        }).start();
    }

    private void connectAndPrintChecker(BluetoothDevice device) {
        new Thread(() -> {
            try {
                connectToPrinter(device);
                printKitchenChecker();
                runOnUiThread(() -> Toast.makeText(this, "Kitchen checker printed successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to print checker: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void printThermalBill() throws IOException {
        // Check if rates are loaded, if not wait a bit or use defaults
        if (!ratesLoaded) {
            setDefaultRates();
        }

        // Use template manager to print bill
        templateManager.printCustomerBill(outputStream, order, taxRate, taxDescription, serviceRate, serviceDescription);
    }

    private void printKitchenChecker() throws IOException {
        // Use template manager to print kitchen checker
        templateManager.printKitchenChecker(outputStream, order);
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

    // Existing methods (unchanged)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ADD_ITEM_REQUEST_CODE && resultCode == RESULT_OK) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fetchOrderDetails();
                Toast.makeText(this, R.string.order_updated, Toast.LENGTH_SHORT).show();
            }, 500);
        } else if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            fetchOrderDetails();
            handlePaymentResult(data);
        } else if (requestCode == CANCEL_ITEM_REQUEST_CODE && resultCode == RESULT_OK) {
            fetchOrderDetails();
            Toast.makeText(this, "Order updated", Toast.LENGTH_SHORT).show();
        } else if (requestCode == BLUETOOTH_ENABLE_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled. Please try printing again.", Toast.LENGTH_SHORT).show();
                // Retry the pending print action
                retryPendingPrintAction();
            } else {
                Toast.makeText(this, "Bluetooth is required for printing", Toast.LENGTH_SHORT).show();
            }
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
                retryPendingPrintAction();
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for printing", Toast.LENGTH_LONG).show();
                // Reset pending flags
                pendingBillPrint = false;
                pendingCheckerPrint = false;
            }
        }
    }

    private void retryPendingPrintAction() {
        if (pendingBillPrint) {
            pendingBillPrint = false;
            printBill();
        } else if (pendingCheckerPrint) {
            pendingCheckerPrint = false;
            printChecker();
        }
    }

    private void handlePaymentResult(Intent data) {
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
        showLoading(true);

        apiHelper.cancelOrder(orderId, new OrderApiHelper.CancelCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(OrderActivity.this,
                            "Order cancelled successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(OrderActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
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

    private void fetchOrderDetails() {
        showLoading(true);

        apiHelper.fetchOrderDetails(orderId, new OrderApiHelper.OrderCallback() {
            @Override
            public void onSuccess(Order fetchedOrder, String updatedAt) {
                runOnUiThread(() -> {
                    order = fetchedOrder;
                    showLoading(false);
                    uiHelper.displayOrderDetails(order, updatedAt);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(OrderActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            contentLayout.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
        }
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
}