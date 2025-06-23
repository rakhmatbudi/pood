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

import com.chuckerteam.chucker.api.Chucker; // Import Chucker
import com.restaurant.management.adapters.TaxAdapter;
import com.restaurant.management.models.Tax;
import com.restaurant.management.models.TaxResponse;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaxListActivity extends AppCompatActivity { // Removed 'implements SensorEventListener' as it's now an anonymous inner class

    private static final String TAG = "TaxListActivity";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noTaxesTextView;
    private TaxAdapter taxAdapter;
    private List<Tax> taxList = new ArrayList<>();

    private ApiService apiService;

    // --- START: Chucker Shake Detection Variables (Copied from DashboardActivity) ---
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600; // Using the same threshold as DashboardActivity
    // --- END: Chucker Shake Detection Variables ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tax_list);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_tax_list);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view_taxes);
        progressBar = findViewById(R.id.progress_bar);
        noTaxesTextView = findViewById(R.id.text_view_no_taxes);

        // Initialize ApiService
        apiService = ApiClient.getApiService(this);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taxAdapter = new TaxAdapter(taxList);
        recyclerView.setAdapter(taxAdapter);

        // Load tax rates
        loadTaxRates();

        // --- START: Chucker Shake Detection Initialization (Copied from DashboardActivity) ---
        initializeShakeDetection();
        // --- END: Chucker Shake Detection Initialization ---
    }

    // --- START: Chucker Shake Detection Methods (Copied from DashboardActivity) ---
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
        // --- START: Sensor registration (Copied from DashboardActivity) ---
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        // --- END: Sensor registration ---
    }

    @Override
    protected void onPause() {
        super.onPause();
        // --- START: Sensor unregistration (Copied from DashboardActivity) ---
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        // --- END: Sensor unregistration ---
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- START: Sensor unregistration (Copied from DashboardActivity) ---
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
        // --- END: Sensor unregistration ---
    }


    private void loadTaxRates() {
        // Show progress and hide other views
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noTaxesTextView.setVisibility(View.GONE);

        // Use ApiService to make the network call
        apiService.getTaxRates().enqueue(new Callback<TaxResponse>() {
            @Override
            public void onResponse(Call<TaxResponse> call, Response<TaxResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TaxResponse taxResponse = response.body();
                    List<Tax> taxes = taxResponse.getData();

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (taxes == null || taxes.isEmpty()) {
                            noTaxesTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            taxList.clear();
                            taxList.addAll(taxes);
                            taxAdapter.notifyDataSetChanged();
                            recyclerView.setVisibility(View.VISIBLE);
                            noTaxesTextView.setVisibility(View.GONE);
                        }
                    });
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        noTaxesTextView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        Toast.makeText(TaxListActivity.this,
                                getString(R.string.error_loading_taxes) + ": HTTP " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<TaxResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load tax rates", t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    noTaxesTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(TaxListActivity.this,
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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