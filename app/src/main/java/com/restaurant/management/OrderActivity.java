package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.restaurant.management.helpers.OrderApiHelper;
import com.restaurant.management.helpers.OrderUiHelper;
import com.restaurant.management.models.Order;

public class OrderActivity extends AppCompatActivity {
    private static final int CANCEL_ITEM_REQUEST_CODE = 200;
    private static final int ADD_ITEM_REQUEST_CODE = 100;
    private static final int PAYMENT_REQUEST_CODE = 101;
    private static final String TAG = "OrderActivity";

    private ProgressBar progressBar;
    private View contentLayout;

    private Order order;
    private long orderId = -1;
    private long sessionId = -1;

    private OrderApiHelper apiHelper;
    private OrderUiHelper uiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        initializeHelpers();
        setupToolbar();
        initializeViews();
        setupClickListeners();

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

        uiHelper.initializeViews(findViewById(android.R.id.content));
    }

    private void setupClickListeners() {
        uiHelper.setClickListeners(
                v -> navigateToAddItem(),
                v -> navigateToPayment(),
                v -> showCancelOrderDialog()
        );
    }

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
}