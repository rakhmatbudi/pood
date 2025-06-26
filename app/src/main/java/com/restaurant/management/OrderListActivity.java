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

import com.chuckerteam.chucker.api.Chucker;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

public class OrderListActivity extends AppCompatActivity implements OrderAdapter.OnOrderClickListener {
    private static final String TAG = "OrderListActivity";

    private long sessionId = -1;

    private OrderListApiHelper apiHelper;
    private OrderListUiHelper uiHelper;
    private OrderDialogHelper dialogHelper;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);

        // Initialize sensor manager for shake detection
        initializeShakeDetection();

        initializeHelpers();
        setupToolbar();
        validateSession();
        initializeViews();
        setupClickListeners();

        // Start by fetching order statuses, then orders
        setupOrderStatuses();
    }

    private void initializeHelpers() {
        apiHelper = new OrderListApiHelper(this);
        uiHelper = new OrderListUiHelper(this);
        // Updated: No longer pass apiHelper to dialogHelper
        dialogHelper = new OrderDialogHelper(this);
    }

    private void initializeShakeDetection() {
        try {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer != null) {
                    sensorManager.registerListener(shakeListener, accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize shake detection", e);
        }
    }

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    try {
                        startActivity(Chucker.getLaunchIntent(OrderListActivity.this));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to launch Chucker", e);
                    }
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Register the sensor listener when activity resumes
        if (sensorManager != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        fetchOrders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener when activity pauses
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
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
                // Updated: Set listener to refresh orders when order is created
                dialogHelper.setOnOrderCreatedListener(this::fetchOrders);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error setting up click listeners", e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void setupOrderStatuses() {
        // Get statuses from local storage/cache loaded at app start
        List<OrderStatus> orderStatuses = apiHelper.getOrderStatuses(); // or from local cache
        uiHelper.setupStatusFilterSpinner(orderStatuses);
        fetchOrders(); // Then proceed to fetch actual orders
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
                // Updated: No need to pass additional parameters, dialogHelper handles everything
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