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
import android.util.Log;
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

    // Test mode - set to true to simulate printing without real printer
    private static final boolean TEST_MODE = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        initializeHelpers();
        setupToolbar();
        initializeViews();
        setupClickListeners();
        initializeBluetooth();

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

        // Initialize print buttons with null checks
        printBillButton = findViewById(R.id.print_bill_button);
        printCheckerButton = findViewById(R.id.print_checker_button);

        // Log button initialization for debugging
        Log.d(TAG, "Print Bill Button: " + (printBillButton != null ? "Found" : "NOT FOUND"));
        Log.d(TAG, "Print Checker Button: " + (printCheckerButton != null ? "Found" : "NOT FOUND"));

        uiHelper.initializeViews(findViewById(android.R.id.content));
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Disable print buttons if Bluetooth not supported
            if (printBillButton != null) printBillButton.setEnabled(false);
            if (printCheckerButton != null) printCheckerButton.setEnabled(false);
            Log.w(TAG, "Bluetooth not supported on this device");
        }
    }

    private void setupClickListeners() {
        uiHelper.setClickListeners(
                v -> navigateToAddItem(),
                v -> navigateToPayment(),
                v -> showCancelOrderDialog()
        );

        // Add print button listeners with null checks
        if (printBillButton != null) {
            Log.d(TAG, "Setting up Print Bill button listener");
            printBillButton.setOnClickListener(v -> {
                Log.d(TAG, "Print Bill button clicked");
                printBill();
            });
        } else {
            Log.w(TAG, "Print Bill button not found in layout");
        }

        if (printCheckerButton != null) {
            Log.d(TAG, "Setting up Print Checker button listener");
            printCheckerButton.setOnClickListener(v -> {
                Log.d(TAG, "Print Checker button clicked");
                printChecker();
            });
        } else {
            Log.w(TAG, "Print Checker button not found in layout");
        }
    }

    // Thermal Printing Implementation
    private void printBill() {
        Log.d(TAG, "=== Print Bill Started ===");

        try {
            Log.d(TAG, "Step 1: Checking if order is null");
            if (order == null) {
                Log.e(TAG, "Order is null - returning");
                Toast.makeText(this, "Order data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Step 2: Order available: " + order.getOrderNumber());

            // Set flag for after permissions are granted
            pendingBillPrint = true;
            pendingCheckerPrint = false;

            Log.d(TAG, "Step 3: Calling checkBluetoothAndEnable()");
            boolean bluetoothResult = checkBluetoothAndEnable();
            Log.d(TAG, "Step 4: Bluetooth check result: " + bluetoothResult);

            if (!bluetoothResult) {
                Log.d(TAG, "Bluetooth check failed - will retry after permissions");
                return;
            }

            Log.d(TAG, "Step 5: Bluetooth check passed, calling showPrinterSelection");
            showPrinterSelection(true); // true for bill
            Log.d(TAG, "Step 6: showPrinterSelection completed");

        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION in printBill: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "=== Print Bill Ended ===");
    }

    private void printChecker() {
        Log.d(TAG, "=== Print Checker Started ===");

        try {
            Log.d(TAG, "Step 1: Checking if order is null");
            if (order == null) {
                Log.e(TAG, "Order is null - returning");
                Toast.makeText(this, "Order data not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Step 2: Order available: " + order.getOrderNumber());

            // Set flag for after permissions are granted
            pendingBillPrint = false;
            pendingCheckerPrint = true;

            Log.d(TAG, "Step 3: Calling checkBluetoothAndEnable()");
            boolean bluetoothResult = checkBluetoothAndEnable();
            Log.d(TAG, "Step 4: Bluetooth check result: " + bluetoothResult);

            if (!bluetoothResult) {
                Log.d(TAG, "Bluetooth check failed - will retry after permissions");
                return;
            }

            Log.d(TAG, "Step 5: Bluetooth check passed, calling showPrinterSelection");
            showPrinterSelection(false); // false for checker
            Log.d(TAG, "Step 6: showPrinterSelection completed");

        } catch (Exception e) {
            Log.e(TAG, "EXCEPTION in printChecker: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "=== Print Checker Ended ===");
    }

    private boolean checkBluetoothAndEnable() {
        Log.d(TAG, "Checking Bluetooth availability");

        try {
            // Check if Bluetooth is supported
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter is null");
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
                return false;
            }

            // Check runtime permissions for Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!hasBluetoothPermissions()) {
                    Log.d(TAG, "Missing Bluetooth permissions for Android 12+");
                    requestBluetoothPermissions();
                    return false;
                }
            } else {
                // For older Android versions, check location permission (required for Bluetooth discovery)
                if (!hasLocationPermission()) {
                    Log.d(TAG, "Missing location permission for Bluetooth");
                    requestLocationPermission();
                    return false;
                }
            }

            // Check if Bluetooth is enabled
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth not enabled, requesting enable");
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, BLUETOOTH_ENABLE_REQUEST);
                return false;
            }

            Log.d(TAG, "Bluetooth is available and enabled");
            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception - missing Bluetooth permissions", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error checking Bluetooth", e);
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
        Log.d(TAG, "Showing printer selection for: " + (isBill ? "Bill" : "Checker"));

        try {
            // TEST MODE: Simulate successful printing
            if (TEST_MODE) {
                Log.d(TAG, "TEST MODE: Simulating printer selection and printing");

                new AlertDialog.Builder(this)
                        .setTitle("TEST MODE")
                        .setMessage("Simulate printing " + (isBill ? "Bill" : "Kitchen Checker") + "?")
                        .setPositiveButton("Yes, Print", (dialog, which) -> {
                            if (isBill) {
                                simulateBillPrinting();
                            } else {
                                simulateCheckerPrinting();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            Log.d(TAG, "Found " + pairedDevices.size() + " paired devices");

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
                Log.d(TAG, "Device " + i + ": " + deviceNames[i]);
                i++;
            }

            String title = isBill ? "Select Printer for Bill" : "Select Printer for Kitchen Checker";

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setItems(deviceNames, (dialog, which) -> {
                        Log.d(TAG, "Selected device: " + deviceNames[which]);
                        if (isBill) {
                            connectAndPrintBill(devices[which]);
                        } else {
                            connectAndPrintChecker(devices[which]);
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        Log.d(TAG, "User cancelled printer selection");
                    })
                    .show();

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception in printer selection", e);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing printer selection", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Test mode simulation methods
    private void simulateBillPrinting() {
        Log.d(TAG, "=== SIMULATING BILL PRINTING ===");

        // Simulate printing delay
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate 2 second printing time

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ TEST: Bill printed successfully!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "TEST MODE: Bill printing simulation completed");
                });

            } catch (InterruptedException e) {
                Log.e(TAG, "Simulation interrupted", e);
            }
        }).start();

        // Show immediate feedback
        Toast.makeText(this, "🖨️ TEST: Printing bill...", Toast.LENGTH_SHORT).show();

        // Log what would be printed
        logSimulatedBillContent();
    }

    private void simulateCheckerPrinting() {
        Log.d(TAG, "=== SIMULATING CHECKER PRINTING ===");

        // Simulate printing delay
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate 1.5 second printing time

                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ TEST: Kitchen checker printed successfully!", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "TEST MODE: Checker printing simulation completed");
                });

            } catch (InterruptedException e) {
                Log.e(TAG, "Simulation interrupted", e);
            }
        }).start();

        // Show immediate feedback
        Toast.makeText(this, "🖨️ TEST: Printing kitchen checker...", Toast.LENGTH_SHORT).show();

        // Log what would be printed
        logSimulatedCheckerContent();
    }

    private void logSimulatedBillContent() {
        Log.d(TAG, "========== SIMULATED BILL CONTENT ==========");
        Log.d(TAG, "              RESTAURANT BILL");
        Log.d(TAG, "");
        Log.d(TAG, "          Your Restaurant Name");
        Log.d(TAG, "        Jl. Restaurant Address");
        Log.d(TAG, "         Jakarta, Indonesia");
        Log.d(TAG, "        Phone: +62-21-1234567");
        Log.d(TAG, "");
        Log.d(TAG, "Order #: " + (order != null ? order.getOrderNumber() : "N/A"));
        Log.d(TAG, "Table: " + (order != null ? order.getTableNumber() : "N/A"));
        if (order != null && order.getCustomerName() != null) {
            Log.d(TAG, "Customer: " + order.getCustomerName());
        }
        Log.d(TAG, "Date: " + formatDateTime(new Date()));
        Log.d(TAG, "");
        Log.d(TAG, "ITEMS:");
        Log.d(TAG, "--------------------------------");

        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Log.d(TAG, item.getDisplayName());
                Log.d(TAG, String.format("  %d x Rp.%,.0f = Rp.%,.0f",
                        item.getQuantity(), item.getUnitPrice(), item.getTotalPrice()));
                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    Log.d(TAG, "  Note: " + item.getNotes());
                }
            }
        } else {
            Log.d(TAG, "No items available");
        }

        Log.d(TAG, "--------------------------------");
        if (order != null) {
            Log.d(TAG, String.format("Subtotal: Rp.%,.0f", order.getTotalAmount()));
            double additionalCharges = order.getFinalAmount() - order.getTotalAmount();
            if (additionalCharges > 0) {
                Log.d(TAG, String.format("Additional Charges: Rp.%,.0f", additionalCharges));
            }
            Log.d(TAG, String.format("TOTAL: Rp.%,.0f", order.getFinalAmount()));
        }
        Log.d(TAG, "");
        Log.d(TAG, "     Thank you for dining with us!");
        Log.d(TAG, "=========================================");
    }

    private void logSimulatedCheckerContent() {
        Log.d(TAG, "======== SIMULATED CHECKER CONTENT =========");
        Log.d(TAG, "           KITCHEN CHECKER");
        Log.d(TAG, "");
        Log.d(TAG, "Order #: " + (order != null ? order.getOrderNumber() : "N/A"));
        Log.d(TAG, "Table: " + (order != null ? order.getTableNumber() : "N/A"));
        Log.d(TAG, "Time: " + formatDateTime(new Date()));
        Log.d(TAG, "");
        Log.d(TAG, "ITEMS TO PREPARE:");
        Log.d(TAG, "--------------------------------");

        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                Log.d(TAG, item.getQuantity() + "x " + item.getDisplayName());
                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    Log.d(TAG, "Note: " + item.getNotes());
                }
                Log.d(TAG, "");
            }
        } else {
            Log.d(TAG, "No items to prepare");
        }

        Log.d(TAG, "** KITCHEN COPY **");
        Log.d(TAG, "=========================================");
    }

    private void connectAndPrintBill(BluetoothDevice device) {
        new Thread(() -> {
            try {
                connectToPrinter(device);
                printThermalBill();
                runOnUiThread(() -> Toast.makeText(this, "Bill printed successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "Error printing bill", e);
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
                Log.e(TAG, "Error printing checker", e);
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
        // Initialize printer
        outputStream.write(ESC_INIT);

        // Print header
        printBillHeader();

        // Print restaurant info
        printRestaurantInfo();

        // Print order info
        printOrderInfo();

        // Print items
        printOrderItems();

        // Print totals
        printBillTotals();

        // Print footer
        printBillFooter();

        // Cut paper
        outputStream.write(ESC_FEED_LINE);
        outputStream.write(ESC_FEED_LINE);
        outputStream.write(ESC_CUT_PAPER);
        outputStream.flush();
    }

    private void printKitchenChecker() throws IOException {
        // Initialize printer
        outputStream.write(ESC_INIT);

        // Print header
        outputStream.write(ESC_ALIGN_CENTER);
        outputStream.write(ESC_DOUBLE_HEIGHT);
        outputStream.write(ESC_BOLD_ON);
        printLine("KITCHEN CHECKER");
        outputStream.write(ESC_NORMAL_SIZE);
        outputStream.write(ESC_BOLD_OFF);
        outputStream.write(ESC_FEED_LINE);

        // Print order info
        outputStream.write(ESC_ALIGN_LEFT);
        printLine("Order #: " + order.getOrderNumber());
        printLine("Table: " + order.getTableNumber());
        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            printLine("Customer: " + order.getCustomerName());
        }
        printLine("Time: " + formatDateTime(new Date()));
        outputStream.write(ESC_FEED_LINE);
        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_FEED_LINE);

        // Print items for kitchen
        outputStream.write(ESC_BOLD_ON);
        printLine("ITEMS TO PREPARE:");
        outputStream.write(ESC_BOLD_OFF);
        printLine(SEPARATOR_LINE);

        List<OrderItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                outputStream.write(ESC_BOLD_ON);
                printLine(item.getQuantity() + "x " + item.getDisplayName());
                outputStream.write(ESC_BOLD_OFF);

                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    printLine("Note: " + item.getNotes());
                }
                outputStream.write(ESC_FEED_LINE);
            }
        } else {
            printLine("No items in this order");
        }

        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_ALIGN_CENTER);
        printLine("** KITCHEN COPY **");

        // Cut paper
        outputStream.write(ESC_FEED_LINE);
        outputStream.write(ESC_FEED_LINE);
        outputStream.write(ESC_CUT_PAPER);
        outputStream.flush();
    }

    private void printBillHeader() throws IOException {
        outputStream.write(ESC_ALIGN_CENTER);
        outputStream.write(ESC_DOUBLE_HEIGHT);
        outputStream.write(ESC_BOLD_ON);
        printLine("CUSTOMER BILL");
        outputStream.write(ESC_NORMAL_SIZE);
        outputStream.write(ESC_BOLD_OFF);
        outputStream.write(ESC_FEED_LINE);
    }

    private void printRestaurantInfo() throws IOException {
        outputStream.write(ESC_ALIGN_CENTER);
        printLine("Serendipity");
        printLine("Jalan Durian Barat III no 10");
        printLine("Jakarta, Indonesia");
        printLine("Phone: +62821234568276");
        printLine("@cafeserendipityjagakarsa");
        outputStream.write(ESC_FEED_LINE);
        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_FEED_LINE);
    }

    private void printOrderInfo() throws IOException {
        outputStream.write(ESC_ALIGN_LEFT);
        printLine("Order #: " + order.getOrderNumber());
        printLine("Table: " + order.getTableNumber());

        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            printLine("Customer: " + order.getCustomerName());
        }

        if (order.getCreatedAt() != null && !order.getCreatedAt().isEmpty()) {
            printLine("Date: " + order.getCreatedAt());
        }

        printLine("Server ID: " + order.getServerId());

        if (order.getOrderTypeName() != null && !order.getOrderTypeName().isEmpty()) {
            printLine("Type: " + order.getOrderTypeName());
        }

        outputStream.write(ESC_FEED_LINE);
        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_FEED_LINE);
    }

    private void printOrderItems() throws IOException {
        outputStream.write(ESC_ALIGN_LEFT);
        outputStream.write(ESC_BOLD_ON);
        printLine("ITEMS:");
        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_BOLD_OFF);

        List<OrderItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (OrderItem item : items) {
                // Item name with variant
                String itemName = item.getDisplayName();
                if (itemName.length() > CHAR_WIDTH) {
                    for (int i = 0; i < itemName.length(); i += CHAR_WIDTH) {
                        int end = Math.min(i + CHAR_WIDTH, itemName.length());
                        printLine(itemName.substring(i, end));
                    }
                } else {
                    printLine(itemName);
                }

                // Quantity and price
                String qtyPrice = String.format("%d x Rp.%,.0f = Rp.%,.0f",
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getTotalPrice());

                outputStream.write(ESC_ALIGN_RIGHT);
                printLine(qtyPrice);
                outputStream.write(ESC_ALIGN_LEFT);

                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    printLine("Note: " + item.getNotes());
                }

                outputStream.write(ESC_FEED_LINE);
            }
        } else {
            printLine("No items in this order");
            outputStream.write(ESC_FEED_LINE);
        }

        printLine(SEPARATOR_LINE);
    }

    private void printBillTotals() throws IOException {
        outputStream.write(ESC_ALIGN_LEFT);

        double subtotal = order.getTotalAmount();
        double finalAmount = order.getFinalAmount();

        // Calculate tax and service charge from the difference
        double additionalCharges = finalAmount - subtotal;

        printLine(formatTotalLine("Subtotal:", formatCurrency(subtotal)));

        if (additionalCharges > 0) {
            printLine(formatTotalLine("Additional Charges:", formatCurrency(additionalCharges)));
        }

        printLine(SEPARATOR_LINE);

        outputStream.write(ESC_BOLD_ON);
        outputStream.write(ESC_DOUBLE_HEIGHT);
        printLine(formatTotalLine("TOTAL:", formatCurrency(finalAmount)));
        outputStream.write(ESC_NORMAL_SIZE);
        outputStream.write(ESC_BOLD_OFF);
        outputStream.write(ESC_FEED_LINE);
    }

    private void printBillFooter() throws IOException {
        outputStream.write(ESC_ALIGN_CENTER);
        printLine(SEPARATOR_LINE);
        outputStream.write(ESC_FEED_LINE);
        printLine("This bill is not receipt.");
        printLine("Receipt printed after payment");
        outputStream.write(ESC_FEED_LINE);
        printLine("Follow us on social media:");
        printLine("@cafeserendipityjagakarsa");
        outputStream.write(ESC_FEED_LINE);
        printLine("Printed: " + formatDateTime(new Date()));
    }

    // Helper methods
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
            Log.e(TAG, "Error disconnecting printer", e);
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
                Log.d(TAG, "Bluetooth permissions granted, retrying print action");
                Toast.makeText(this, "Permissions granted. Retrying print...", Toast.LENGTH_SHORT).show();
                retryPendingPrintAction();
            } else {
                Log.d(TAG, "Bluetooth permissions denied");
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