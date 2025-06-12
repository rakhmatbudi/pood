package com.restaurant.management;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionActivity extends AppCompatActivity implements TransactionExpandableListAdapter.OnPrintClickListener {

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

        // Initialize API service
        apiService = ApiClient.getClient(this).create(ApiService.class);

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

        Call<SessionPaymentsResponse> call = apiService.getSessionPayments();
        call.enqueue(new Callback<SessionPaymentsResponse>() {
            @Override
            public void onResponse(Call<SessionPaymentsResponse> call, Response<SessionPaymentsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SessionPaymentsResponse data = response.body();
                    if ("success".equals(data.getStatus())) {
                        parseSessionsAndTransactions(data);
                        setupExpandableListView();
                    } else {
                        handleError("API returned non-success status");
                    }
                } else {
                    handleError("API error: " + response.code());
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

        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (SessionWithPayments sessionData : sessionsData) {
            int sessionId = sessionData.getCashierSessionId();

            CashierSession session = new CashierSession();
            session.setId(sessionId);

            if (sessionData.getCashierSessionOpenedAt() != null) {
                Date startTime = sessionData.getCashierSessionOpenedAt();
                session.setStartTime(startTime);
            }

            sessionList.add(session);

            // Parse payments for this session
            List<PaymentData> payments = sessionData.getPayments();
            List<Transaction> transactions = new ArrayList<>();

            if (payments != null) {
                SimpleDateFormat paymentDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
                paymentDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                for (PaymentData payment : payments) {
                    int paymentId = payment.getPaymentId();
                    int orderId = payment.getOrderId();
                    String tableNumber = payment.getOrderTableNumber();
                    double amount = payment.getPaymentAmount();
                    int paymentMode = payment.getPaymentMode();
                    String paymentModeText = payment.getPaymentModeName();

                    Date paymentDate = null;
                    try {
                        String dateStr = payment.getPaymentDate();
                        paymentDate = paymentDateFormat.parse(dateStr);
                    } catch (ParseException e) {
                        paymentDate = new Date();
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
            expandableListView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyView(boolean show) {
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
        expandableListView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void handleError(String message) {
        Toast.makeText(this, "Error loading transactions", Toast.LENGTH_SHORT).show();
        showLoading(false);
        showEmptyView(true);
        emptyView.setText(R.string.error_loading_transactions);
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