package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.restaurant.management.adapters.OrderAdapter;
import com.restaurant.management.helpers.OrderDialogHelper;
import com.restaurant.management.helpers.OrderListApiHelper;
import com.restaurant.management.helpers.OrderListUiHelper;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderStatus;

import java.util.List;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private static final String TAG = "OrderListActivity";

    private long sessionId = -1;

    private OrderListApiHelper apiHelper;
    private OrderListUiHelper uiHelper;
    private OrderDialogHelper dialogHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        initializeHelpers();
        setupToolbar();
        validateSession();
        initializeViews();
        setupClickListeners();

        // Start by fetching order statuses, then orders
        fetchOrderStatuses();
    }

    private void initializeHelpers() {
        apiHelper = new OrderListApiHelper(this);
        uiHelper = new OrderListUiHelper(this);
        dialogHelper = new OrderDialogHelper(this, apiHelper);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.orders_list_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void validateSession() {
        sessionId = getIntent().getLongExtra("session_id", -1);
        if (sessionId == -1) {
            Toast.makeText(this, R.string.invalid_session, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            View rootView = findViewById(android.R.id.content);
            uiHelper.initializeViews(rootView, this);

            // Set up listeners
            uiHelper.setRefreshListener(this::fetchOrders);
            uiHelper.setTitleUpdateListener(title -> {
                try {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(title);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupClickListeners() {
        try {
            View fabAddOrder = findViewById(R.id.fab_add_order);
            if (fabAddOrder != null) {
                fabAddOrder.setOnClickListener(v -> showNewOrderDialog());
            }

            if (dialogHelper != null) {
                dialogHelper.setOnOrderCreatedListener(this::fetchOrders);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchOrders();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void fetchOrderStatuses() {
        apiHelper.fetchOrderStatuses(new OrderListApiHelper.OrderStatusesCallback() {
            @Override
            public void onSuccess(List<OrderStatus> orderStatuses) {
                runOnUiThread(() -> {
                    uiHelper.setupStatusFilterSpinner(orderStatuses);
                    fetchOrders();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    // Use fallback data
                    uiHelper.setupStatusFilterSpinner(apiHelper.getFallbackOrderStatuses());
                    Toast.makeText(OrderListActivity.this,
                            "Using offline order statuses", Toast.LENGTH_SHORT).show();
                    fetchOrders();
                });
            }
        });
    }

    private void fetchOrders() {
        uiHelper.showLoading(true);

        apiHelper.fetchOrders(sessionId, new OrderListApiHelper.OrdersCallback() {
            @Override
            public void onSuccess(List<Order> orders) {
                runOnUiThread(() -> {
                    uiHelper.showLoading(false);
                    uiHelper.updateOrdersList(orders);
                    uiHelper.showContent();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    uiHelper.showLoading(false);
                    uiHelper.showEmptyView(errorMessage);
                });
            }
        });
    }

    private void showNewOrderDialog() {
        try {
            if (dialogHelper != null && sessionId != -1) {
                dialogHelper.showNewOrderDialog(sessionId);
            } else {
                Toast.makeText(this, "Unable to create order. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error opening create order dialog", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onOrderClick(Order order) {
        Intent intent = new Intent(this, OrderActivity.class);
        intent.putExtra("order_id", order.getId());
        intent.putExtra("session_id", sessionId);
        startActivity(intent);
    }
}