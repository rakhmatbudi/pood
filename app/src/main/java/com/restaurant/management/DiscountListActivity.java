package com.restaurant.management;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.chuckerteam.chucker.api.Chucker;
// No need for ChuckerInterceptor here, as ApiClient handles it
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.restaurant.management.adapters.DiscountAdapter;
import com.restaurant.management.models.Discount;
import com.restaurant.management.models.DiscountResponse; // Import DiscountResponse
import com.restaurant.management.network.ApiClient; // Import ApiClient
import com.restaurant.management.network.ApiService; // Import ApiService

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscountListActivity extends AppCompatActivity implements DiscountAdapter.DiscountClickListener {
    private static final String TAG = "DiscountListActivity";
    // Removed BASE_API_URL as it's handled by Retrofit's base URL

    private RecyclerView discountRecyclerView;
    private ProgressBar progressBar;
    private TextView noDiscountsTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DiscountAdapter discountAdapter;
    private List<Discount> discountList = new ArrayList<>();

    // Removed direct OkHttpClient client
    private ApiService apiService; // Declare ApiService

    // --- START: Chucker Shake Detection Variables ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;
    // --- END: Chucker Shake Detection Variables ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount_list);

        // --- START: Initialize ApiService (Retrofit) ---
        apiService = ApiClient.getApiService(this); // Get the configured ApiService instance
        // --- END: Initialize ApiService ---

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.title_activity_discount_list));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize Views
        discountRecyclerView = findViewById(R.id.discount_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        noDiscountsTextView = findViewById(R.id.no_discounts_text_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        FloatingActionButton fabAddDiscount = findViewById(R.id.fab_add_discount);

        // Setup RecyclerView
        discountRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        discountAdapter = new DiscountAdapter(discountList, this);
        discountRecyclerView.setAdapter(discountAdapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::fetchDiscounts);

        // Setup FAB click listener
        fabAddDiscount.setOnClickListener(v -> {
            // TODO: Implement add discount functionality
            Toast.makeText(DiscountListActivity.this,
                    "Add discount functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        // Load discounts
        fetchDiscounts();

        // --- START: Chucker Shake Detection Initialization ---
        initializeShakeDetection();
        // --- END: Chucker Shake Detection Initialization ---
    }

    // --- START: Chucker Shake Detection Methods (Copied from DashboardActivity) ---
    private void initializeShakeDetection() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
                    launchChucker();
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

    private void launchChucker() {
        try {
            startActivity(Chucker.getLaunchIntent(this));
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Chucker", e);
        }
    }
    // --- END: Chucker Shake Detection Methods ---


    @Override
    protected void onResume() {
        super.onResume();
        // --- START: Sensor registration ---
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        // --- END: Sensor registration ---
    }

    @Override
    protected void onPause() {
        super.onPause();
        // --- START: Sensor unregistration ---
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        // --- END: Sensor unregistration ---
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- START: Sensor unregistration ---
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        // --- END: Sensor unregistration ---
    }

    private void fetchDiscounts() {
        // Show loading
        showLoading(true);

        // Get auth token
        String authToken = getAuthToken();

        // Use ApiService to make the network call
        apiService.getDiscounts("Bearer " + authToken).enqueue(new Callback<DiscountResponse>() {
            @Override
            public void onResponse(Call<DiscountResponse> call, Response<DiscountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DiscountResponse discountResponse = response.body();
                    List<Discount> discounts = discountResponse.getData(); // Get data from the response object

                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        updateUI(discounts);
                    });
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(DiscountListActivity.this,
                                getString(R.string.error_loading_discounts) + ": HTTP " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<DiscountResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load discounts", t);
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(DiscountListActivity.this,
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // This method is no longer needed if Retrofit correctly parses DiscountResponse
    // private List<Discount> parseDiscounts(JSONObject jsonResponse) throws JSONException {
    //     List<Discount> discounts = new ArrayList<>();
    //     // ... existing parsing logic ...
    //     return discounts;
    // }

    private void updateUI(List<Discount> discounts) {
        discountList.clear();
        if (discounts != null) { // Add null check for safety
            discountList.addAll(discounts);
        }
        discountAdapter.notifyDataSetChanged();

        showLoading(false);

        if (discountList.isEmpty()) { // Check discountList, not 'discounts' directly
            noDiscountsTextView.setVisibility(View.VISIBLE);
            discountRecyclerView.setVisibility(View.GONE);
        } else {
            noDiscountsTextView.setVisibility(View.GONE);
            discountRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

        if (isLoading) {
            discountRecyclerView.setVisibility(View.GONE);
            noDiscountsTextView.setVisibility(View.GONE);
        }
    }

    private String getAuthToken() {
        return getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
    }

    @Override
    public void onDiscountClick(Discount discount) {
        // TODO: Implement discount detail view or edit functionality
        Toast.makeText(this, "Selected: " + discount.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}