package com.restaurant.management;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class TransactionActivity extends AppCompatActivity {

    private static final String TAG = "TransactionActivity";

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
        apiService = ApiClient.getClient().create(ApiService.class);

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
            // Show order details when clicked
            showOrderDetails(transaction);
            return true;
        });
    }

    private void showOrderDetails(Transaction transaction) {
        // Open OrderActivity with the order ID
        String message = String.format(Locale.getDefault(),
                "Order #%d - Table %s - %s",
                transaction.getOrderId(),
                transaction.getTableNumber(),
                formatCurrency(transaction.getAmount()));

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
            // Extract data using the correct field names based on API response
            int sessionId = sessionData.getCashierSessionId(); // or sessionData.id based on your model

            // Create a new CashierSession object
            CashierSession session = new CashierSession();
            session.setId(sessionId);

            // Use the actual cashier name from the API
            //session.setUserName(sessionData.getCashierName());

            // Use the actual opening amount from the API
            //double openingAmount = 0;
            //try {
            //    openingAmount = Double.parseDouble(sessionData.getOpeningAmount());
            //} catch (NumberFormatException e) {
            //    Log.e(TAG, "Error parsing opening amount: " + e.getMessage());
            //}
            //session.setStartAmount(openingAmount);

            // Use the actual closing amount from the API
            //double closingAmount = 0;
            //try {
            //    closingAmount = Double.parseDouble(sessionData.getClosingAmount());
            //} catch (NumberFormatException e) {
            //    Log.e(TAG, "Error parsing closing amount: " + e.getMessage());
            //}
            //session.setEndAmount(closingAmount);

            // Parse the session start time (opened_at)


            if (sessionData.getCashierSessionOpenedAt() != null) {
                Date startTime = sessionData.getCashierSessionOpenedAt();
                session.setStartTime(startTime);
                Log.d(TAG, "Parsed session opened time: " + startTime);
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
                        Log.e(TAG, "Date parsing error: " + e.getMessage());
                        paymentDate = new Date();
                    }

                    // Parse order items
                    List<PaymentData.OrderItemData> itemsData = payment.getOrderItems();
                    List<OrderItem> orderItems = new ArrayList<>();

                    if (itemsData != null) {
                        for (PaymentData.OrderItemData itemData : itemsData) {
                            OrderItem orderItem = new OrderItem();

                            // Set the basic properties
                            orderItem.setId(itemData.getItemId());
                            orderItem.setMenuItemId(itemData.getMenuItemId());
                            orderItem.setQuantity(itemData.getQuantity());
                            orderItem.setUnitPrice(itemData.getUnitPrice());
                            orderItem.setTotalPrice(itemData.getTotalPrice());

                            // Explicitly set the menu item name from API
                            if (itemData.getMenuItemName() != null) {
                                orderItem.setMenuItemName(itemData.getMenuItemName());
                            } else {
                                // Fallback
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

                    // Create the transaction with all the data
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

            // Add transactions list to map
            transactionMap.put(session, transactions);
        }
    }

    private void setupExpandableListView() {
        if (sessionList.isEmpty()) {
            showEmptyView(true);
            return;
        }

        showEmptyView(false);
        listAdapter = new TransactionExpandableListAdapter(this, sessionList, transactionMap);
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
        Log.e(TAG, message);
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
}