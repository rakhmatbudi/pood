package com.restaurant.management;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.restaurant.management.adapters.DiscountAdapter;
import com.restaurant.management.models.Discount;

// --- START: Chucker Shake Imports ---
import com.chuckerteam.chucker.api.Chucker;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context; // Needed for getSystemService
// --- END: Chucker Shake Imports ---

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DiscountListActivity extends AppCompatActivity implements DiscountAdapter.DiscountClickListener {
    private static final String TAG = "DiscountListActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/discounts/";

    private RecyclerView discountRecyclerView;
    private ProgressBar progressBar;
    private TextView noDiscountsTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DiscountAdapter discountAdapter;
    private List<Discount> discountList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient(); // Consider migrating this to ApiService/Retrofit later

    // --- START: Chucker Shake Detection Variables ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600; // Using the same threshold as DashboardActivity
    // --- END: Chucker Shake Detection Variables ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discount_list);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) { // Added null check for robustness
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
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Use Context.SENSOR_SERVICE
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

            if ((curTime - lastUpdate) > 100) { // Using 100ms interval as in DashboardActivity
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Using the exact speed calculation from DashboardActivity
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
            // Using the exact launch method from DashboardActivity
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

        // Build request
        Request.Builder requestBuilder = new Request.Builder()
                .url(BASE_API_URL);

        // Add auth header if available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(DiscountListActivity.this,
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Response body: " + responseBody);

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<Discount> discounts = parseDiscounts(jsonResponse);

                    runOnUiThread(() -> {
                        swipeRefreshLayout.setRefreshing(false);
                        updateUI(discounts);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(DiscountListActivity.this,
                                getString(R.string.error_processing_response, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private List<Discount> parseDiscounts(JSONObject jsonResponse) throws JSONException {
        List<Discount> discounts = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray discountsArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < discountsArray.length(); i++) {
                JSONObject discountJson = discountsArray.getJSONObject(i);
                Discount discount = new Discount();

                discount.setId(discountJson.optLong("id", -1));
                discount.setName(discountJson.optString("name", ""));
                discount.setDescription(discountJson.optString("description", ""));
                discount.setAmount(discountJson.optInt("amount", 0));

                discounts.add(discount);
            }
        }

        return discounts;
    }

    private void updateUI(List<Discount> discounts) {
        discountList.clear();
        discountList.addAll(discounts);
        discountAdapter.notifyDataSetChanged();

        showLoading(false);

        if (discounts.isEmpty()) {
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