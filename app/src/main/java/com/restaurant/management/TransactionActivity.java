package com.restaurant.management;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.restaurant.management.printing.PrintTemplateManager;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.restaurant.management.adapters.TransactionExpandableListAdapter;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.OrderItem;
import com.restaurant.management.models.Transaction;
import com.restaurant.management.models.SessionPaymentsResponse;
import com.restaurant.management.models.SessionWithPayments;
import com.restaurant.management.models.PaymentData;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call; // Use Retrofit's Call
import retrofit2.Callback; // Use Retrofit's Callback
import retrofit2.Response; // Use Retrofit's Response

// Remove OkHttp specific imports as ApiService handles them
// import okhttp3.Call;
// import okhttp3.Callback;
// import okhttp3.OkHttpClient;
// import okhttp3.Request;
// import okhttp3.Response;

public class TransactionActivity extends AppCompatActivity implements TransactionExpandableListAdapter.OnPrintClickListener {
    private static final String TAG = "TransactionActivity";

    private static final int BLUETOOTH_PERMISSION_REQUEST = 105;
    private static final UUID PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private PrintTemplateManager templateManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private Transaction pendingPrintTransaction = null;
    private ExpandableListView expandableListView;
    private TransactionExpandableListAdapter listAdapter;
    private List<CashierSession> sessionList;
    private Map<CashierSession, List<Transaction>> transactionMap;
    private ProgressBar progressBar;
    private TextView emptyView;

    // Use ApiService instead of raw OkHttpClient
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.transactions);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize API service using ApiClient
        apiService = ApiClient.getApiService(this); // Correctly initialize ApiService with context

        // Initialize printing components
        templateManager = new PrintTemplateManager(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Initialize views
        expandableListView = findViewById(R.id.transactionExpandableListView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Initialize data structures
        sessionList = new ArrayList<>();
        transactionMap = new HashMap<>();

        // Fetch data
        fetchTransactions();

        // Set up expandable list view click listeners
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> false);

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            Transaction transaction = transactionMap.get(sessionList.get(groupPosition)).get(childPosition);
            showOrderDetails(transaction);
            return true;
        });
    }

    private void showOrderDetails(Transaction transaction) {
        String message = String.format(Locale.getDefault(),
                "Order #%d - Table %s - %s",
                transaction.getOrderId(),
                transaction.getTableNumber(),
                formatCurrency(transaction.getAmount()));

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPrintClick(Transaction transaction) {
        pendingPrintTransaction = transaction;
        reprintReceipt(transaction);
    }

    private void reprintReceipt(Transaction transaction) {
        if (transaction == null) {
            Toast.makeText(this, "Transaction data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkBluetoothAndEnable()) {
            return;
        }

        showPrinterSelection(transaction);
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

    private void showPrinterSelection(Transaction transaction) {
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

            new AlertDialog.Builder(this)
                    .setTitle("Select Printer for Receipt Reprint")
                    .setItems(deviceNames, (dialog, which) -> {
                        connectAndPrintReceipt(devices[which], transaction);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectAndPrintReceipt(BluetoothDevice device, Transaction transaction) {
        new Thread(() -> {
            try {
                connectToPrinter(device);
                printTransactionReceipt(transaction);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Receipt reprinted successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to reprint receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void printTransactionReceipt(Transaction transaction) throws IOException {
        // Use the template manager to print receipt
        // We'll use default tax and service rates, or you could fetch them from API
        double taxRate = 0.10; // 10% default
        String taxDescription = "Tax";
        double serviceRate = 0.02; // 2% default
        String serviceDescription = "Service Charge";

        // Calculate original amount (before discount if any)
        double originalAmount = transaction.getAmount();
        double finalAmount = transaction.getAmount();
        double discountAmount = 0.0;
        String discountName = null;

        templateManager.printPaymentReceipt(
                outputStream,
                String.valueOf(transaction.getOrderId()),
                transaction.getTableNumber(),
                originalAmount,
                finalAmount,
                discountAmount,
                discountName,
                transaction.getPaymentMethod(),
                transaction.getAmount(), // Amount paid same as final amount
                taxRate,
                taxDescription,
                serviceRate,
                serviceDescription
        );
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
                if (pendingPrintTransaction != null) {
                    reprintReceipt(pendingPrintTransaction);
                    pendingPrintTransaction = null;
                }
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for printing", Toast.LENGTH_LONG).show();
                pendingPrintTransaction = null;
            }
        }
    }

    private void fetchTransactions() {
        showLoading(true);

        // Use ApiService to make the call to get session payments
        apiService.getSessionPayments().enqueue(new Callback<SessionPaymentsResponse>() {
            @Override
            public void onResponse(Call<SessionPaymentsResponse> call, Response<SessionPaymentsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SessionPaymentsResponse data = response.body();
                    if ("success".equals(data.getStatus())) {
                        parseSessionsAndTransactions(data);
                        setupExpandableListView();
                    } else {
                        handleError("API returned non-success status: " + data.getMessage()); // Log API message
                    }
                } else {
                    // Log HTTP error details
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    handleError("HTTP error: " + response.code() + " " + response.message() + " - " + errorBody);
                    // Handle 401 Unauthorized specifically
                    if (response.code() == 401) {
                        Toast.makeText(TransactionActivity.this, getString(R.string.session_expired_relogin), Toast.LENGTH_LONG).show();
                        // Clear session data and force logout
                        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.apply();
                        Intent intent = new Intent(TransactionActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
                showLoading(false);
            }

            @Override
            public void onFailure(Call<SessionPaymentsResponse> call, Throwable t) {
                handleError("Network error: " + t.getMessage());
                showLoading(false);
            }
        });
    }

    private void parseSessionsAndTransactions(SessionPaymentsResponse response) {
        List<SessionWithPayments> sessionsData = response.getData();

        if (sessionsData == null || sessionsData.isEmpty()) {
            showEmptyView(true);
            return;
        }

        // Clear existing data
        sessionList.clear();
        transactionMap.clear();

        // Use the Gson converter in ApiClient; manual SimpleDateFormat parsing
        // for model fields should ideally be avoided if models are correctly mapped to Date.
        // If SessionWithPayments.getCashierSessionOpenedAt() returns a Date, use it directly.
        // If PaymentData.getPaymentDate() returns a Date, use it directly.
        // Assuming your models (SessionWithPayments, PaymentData) are updated to use java.util.Date.

        for (SessionWithPayments sessionData : sessionsData) {
            // Get ID and OpenedAt directly from SessionWithPayments if they are Date/Long
            Long sessionId = (long) sessionData.getCashierSessionId(); // Ensure this returns Long
            Date sessionOpenedAt = sessionData.getCashierSessionOpenedAt(); // Ensure this returns Date

            CashierSession session = new CashierSession();
            session.setSessionId(sessionId); // Use setSessionId (expects Long)
            session.setOpenedAt(sessionOpenedAt); // Use setOpenedAt (expects Date)

            // As per CashierSession model, if you want to set the userId, you'd get it from sessionData if available
            // session.setUserId(sessionData.getUserId()); // Add this if SessionWithPayments has getUserId()


            sessionList.add(session);

            // Parse payments for this session
            List<PaymentData> payments = sessionData.getPayments();
            List<Transaction> transactions = new ArrayList<>();

            if (payments != null) {
                // If payment.getPaymentDate() returns Date, this manual parsing is not needed.
                SimpleDateFormat paymentDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
                paymentDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                for (PaymentData payment : payments) {
                    int paymentId = payment.getPaymentId();
                    int orderId = payment.getOrderId();
                    String tableNumber = payment.getOrderTableNumber();
                    double amount = payment.getPaymentAmount();
                    // int paymentMode = payment.getPaymentMode(); // Not directly used in Transaction constructor
                    String paymentModeText = payment.getPaymentModeName();

                    Date paymentDate = null; // Initialize to null
                    try {
                        String dateStr = payment.getPaymentDate(); // This returns a String
                        if (dateStr != null && !dateStr.isEmpty()) {
                            paymentDate = paymentDateFormat.parse(dateStr); // <--- PARSE THE STRING TO DATE
                        }
                    } catch (ParseException e) {
                        // Handle parsing error, e.g., log it and use current date or null
                        Log.e(TAG, "Error parsing payment date: " + payment.getPaymentDate(), e);
                        paymentDate = new Date(); // Fallback to current date or handle as error
                    }

                    // Parse order items
                    List<PaymentData.OrderItemData> itemsData = payment.getOrderItems();
                    List<OrderItem> orderItems = new ArrayList<>();

                    if (itemsData != null) {
                        for (PaymentData.OrderItemData itemData : itemsData) {
                            OrderItem orderItem = new OrderItem();

                            orderItem.setId(itemData.getItemId());
                            orderItem.setMenuItemId(itemData.getMenuItemId());
                            orderItem.setQuantity(itemData.getQuantity());
                            orderItem.setUnitPrice(itemData.getUnitPrice());
                            orderItem.setTotalPrice(itemData.getTotalPrice());

                            if (itemData.getMenuItemName() != null) {
                                orderItem.setMenuItemName(itemData.getMenuItemName());
                            } else {
                                orderItem.setMenuItemName("Item #" + itemData.getMenuItemId());
                            }

                            if (itemData.getNotes() != null) {
                                orderItem.setNotes(itemData.getNotes());
                            }

                            orderItem.setOrderId(orderId);

                            if (itemData.getVariantId() != null) {
                                orderItem.setVariantId(itemData.getVariantId());
                            }

                            orderItems.add(orderItem);
                        }
                    }

                    String customerName = payment.getCustomerName();

                    Transaction transaction = new Transaction(
                            paymentId,
                            orderId,
                            tableNumber,
                            amount,
                            paymentModeText,
                            paymentDate,
                            orderItems,
                            customerName
                    );

                    transactions.add(transaction);
                }
            }

            transactionMap.put(session, transactions);
        }
    }

    private void setupExpandableListView() {
        if (sessionList.isEmpty()) {
            showEmptyView(true);
            return;
        }

        showEmptyView(false);

        // Updated constructor call - pass 'this' as the print click listener
        listAdapter = new TransactionExpandableListAdapter(this, sessionList, transactionMap, this);
        expandableListView.setAdapter(listAdapter);

        // Expand all groups initially
        for (int i = 0; i < listAdapter.getGroupCount(); i++) {
            expandableListView.expandGroup(i);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            expandableListView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            // Only make expandableListView visible if it's not empty
            if (!sessionList.isEmpty()) {
                expandableListView.setVisibility(View.VISIBLE);
            } else {
                showEmptyView(true); // If list is empty, show empty view
            }
        }
    }

    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        expandableListView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void handleError(String message) {
        Toast.makeText(this, "Error loading transactions: " + message, Toast.LENGTH_LONG).show(); // Show detailed error
        showLoading(false);
        showEmptyView(true);
        emptyView.setText(getString(R.string.error_loading_transactions) + "\n" + message); // Show error message in UI
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "Rp %,.0f", amount);
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