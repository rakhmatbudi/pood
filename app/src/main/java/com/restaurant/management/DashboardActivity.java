package com.restaurant.management;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.restaurant.management.adapters.CashierSessionAdapter;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.adapters.PromoAdapter;
import com.restaurant.management.repositories.PromoRepository;
import com.restaurant.management.models.Promo;
import com.chuckerteam.chucker.api.ChuckerCollector;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.chuckerteam.chucker.api.RetentionManager;
import com.chuckerteam.chucker.api.Chucker;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Iterator;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DashboardActivity";
    private static final String API_URL = "https://api.pood.lol/cashier-sessions/current";

    private DrawerLayout drawerLayout;
    private TextView userNameTextView;
    private TextView dateTimeTextView;
    private TextView sessionStatusTextView;
    private Button openSessionButton;
    private Button endSessionButton;

    // These may be null since they're not in your layout
    private RecyclerView pastSessionsRecyclerView;
    private ProgressBar loadingProgressBar;
    private TextView noPastSessionsTextView;

    private RecyclerView promosRecyclerView;
    private TextView noPromosTextView;
    private ProgressBar promosProgressBar;
    private TextView promosHeaderTextView;
    private List<Promo> promos = new ArrayList<>();
    private PromoRepository promoRepository;
    private PromoAdapter promoAdapter;

    private int userId;
    private String userName;
    private List<CashierSession> cashierSessions = new ArrayList<>();

    // CHANGED: Use ApiClient to get OkHttpClient with Chucker integration
    private OkHttpClient client;

    // NEW: Shake detection fields
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_dashboard);

            // CHANGED: Initialize OkHttpClient using ApiClient for Chucker integration
            initializeHttpClient();

            // NEW: Initialize shake detection for Chucker
            initializeShakeDetection();

            // Initialize toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Initialize UI components
            drawerLayout = findViewById(R.id.drawer_layout);
            NavigationView navigationView = findViewById(R.id.nav_view);
            userNameTextView = findViewById(R.id.user_name_text_view);
            dateTimeTextView = findViewById(R.id.date_time_text_view);
            sessionStatusTextView = findViewById(R.id.session_status_text_view);
            openSessionButton = findViewById(R.id.open_session_button);
            endSessionButton = findViewById(R.id.end_session_button);

            initializePromoComponents();

            // NEW: Setup debug options (long press to open Chucker)
            setupDebugOptions();

            // Try to find the RecyclerView components, but don't crash if not found
            pastSessionsRecyclerView = findViewById(R.id.past_sessions_recycler_view);
            noPastSessionsTextView = findViewById(R.id.no_past_sessions_text_view);
            loadingProgressBar = findViewById(R.id.loading_progress_bar);

            // Initial button visibility setup
            openSessionButton.setVisibility(View.VISIBLE);
            endSessionButton.setVisibility(View.GONE);

            // Set up navigation drawer
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
            navigationView.setNavigationItemSelectedListener(this);

            // Get user info from shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            userId = sharedPreferences.getInt(getString(R.string.pref_user_id), -1);
            userName = sharedPreferences.getString(getString(R.string.pref_user_name), "");

            userNameTextView.setText(userName);
            updateDateTime();

            // Setup RecyclerView ONLY if it's not null
            if (pastSessionsRecyclerView != null) {
                pastSessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                pastSessionsRecyclerView.setAdapter(new CashierSessionAdapter(cashierSessions));
                pastSessionsRecyclerView.setVisibility(View.GONE);

                if (noPastSessionsTextView != null) {
                    noPastSessionsTextView.setVisibility(View.VISIBLE);
                }
            }

            // Set initial UI state
            sessionStatusTextView.setText(getString(R.string.checking_session_status));

            // Check if there's an active cashier session
            checkActiveCashierSession();

            // Set button click listeners
            openSessionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        // Navigate to OpenSessionActivity
                        Intent intent = new Intent(DashboardActivity.this, OpenSessionActivity.class);
                        intent.putExtra(getString(R.string.extra_user_id), userId);
                        startActivity(intent);

                        // After returning from OpenSessionActivity, check session status again
                        checkActiveCashierSession();
                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to OpenSessionActivity", e);
                        Toast.makeText(DashboardActivity.this,
                                getString(R.string.navigation_error, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

            endSessionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Log.d(TAG, getString(R.string.navigating_to_end_session));

                        // Check if we have a session ID before navigating
                        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                        long sessionId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);

                        if (sessionId == -1) {
                            Toast.makeText(DashboardActivity.this,
                                    R.string.error_no_active_session,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Navigate to ReconciliationActivity
                        Intent intent = new Intent(DashboardActivity.this, ReconciliationActivity.class);
                        startActivity(intent);

                    } catch (Exception e) {
                        Log.e(TAG, "Error navigating to ReconciliationActivity", e);
                        Toast.makeText(DashboardActivity.this,
                                getString(R.string.navigation_error, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in DashboardActivity.onCreate", e);
            Toast.makeText(this,
                    getString(R.string.error_initializing, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    // NEW METHOD: Initialize HTTP client with Chucker integration
    private void initializeHttpClient() {
        try {
            ChuckerCollector chuckerCollector = new ChuckerCollector(
                    this,
                    true, // Show notification
                    RetentionManager.Period.ONE_HOUR
            );

            ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor.Builder(this)
                    .collector(chuckerCollector)
                    .maxContentLength(250_000L)
                    .redactHeaders("Authorization", "Cookie")
                    .alwaysReadResponseBody(true)
                    .createShortcut(true)
                    .build();

            // Add logging interceptor too
            okhttp3.logging.HttpLoggingInterceptor loggingInterceptor =
                    new okhttp3.logging.HttpLoggingInterceptor();
            loggingInterceptor.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);

            client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(chuckerInterceptor)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize HTTP client with Chucker, falling back to basic client", e);
            // Fallback to basic OkHttpClient if ApiClient fails
            client = new OkHttpClient();
        }
    }

    // NEW METHOD: Initialize shake detection
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

    // NEW: Shake detection listener
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

    // NEW METHOD: Launch Chucker manually
    private void launchChucker() {
        try {
            startActivity(Chucker.getLaunchIntent(this));
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Chucker", e);
        }
    }

    // NEW METHOD: Setup debug options (long press to open Chucker)
    private void setupDebugOptions() {
        // Long press on user name to open Chucker (alternative to shake)
        if (userNameTextView != null) {
            userNameTextView.setOnLongClickListener(v -> {
                launchChucker();
                return true;
            });
        }
    }

    // NEW METHOD: Create test network request to verify Chucker
    private void testChuckerRequest() {
        Request testRequest = new Request.Builder()
                .url("https://httpbin.org/get") // Simple test endpoint
                .addHeader("X-Test-Header", "Chucker-Test")
                .build();

        client.newCall(testRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Test request failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.body().close();
            }
        });
    }

    // UPDATED: Using PromoRepository instead of PromoApiHelper
    private void loadPromos() {
        if (promosProgressBar != null) {
            promosProgressBar.setVisibility(View.VISIBLE);
        }

        if (promosRecyclerView != null) {
            promosRecyclerView.setVisibility(View.GONE);
        }

        if (noPromosTextView != null) {
            noPromosTextView.setVisibility(View.GONE);
        }

        // CHANGED: Load from offline database instead of API
        promoRepository.getOfflinePromos(new PromoRepository.PromoCallback() {
            @Override
            public void onSuccess(List<Promo> fetchedPromos) {
                runOnUiThread(() -> {
                    if (promosProgressBar != null) {
                        promosProgressBar.setVisibility(View.GONE);
                    }

                    promos.clear();
                    promos.addAll(fetchedPromos);

                    if (promos.isEmpty()) {
                        if (promosRecyclerView != null) {
                            promosRecyclerView.setVisibility(View.GONE);
                        }
                        if (noPromosTextView != null) {
                            noPromosTextView.setVisibility(View.VISIBLE);
                            noPromosTextView.setText("No promotions available");
                        }
                    } else {
                        if (promosRecyclerView != null) {
                            promosRecyclerView.setVisibility(View.VISIBLE);
                            promoAdapter.notifyDataSetChanged();
                        }
                        if (noPromosTextView != null) {
                            noPromosTextView.setVisibility(View.GONE);
                        }
                    }

                    // Update header text with count
                    if (promosHeaderTextView != null) {
                        String headerText = "Promotions (" + promos.size() + ")";
                        promosHeaderTextView.setText(headerText);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (promosProgressBar != null) {
                        promosProgressBar.setVisibility(View.GONE);
                    }

                    if (promosRecyclerView != null) {
                        promosRecyclerView.setVisibility(View.GONE);
                    }

                    if (noPromosTextView != null) {
                        noPromosTextView.setVisibility(View.VISIBLE);
                        noPromosTextView.setText("No promotions available");
                    }

                    Log.e(TAG, "Failed to load offline promos: " + message);
                });
            }
        });
    }

    private void testPromoDatabase() {
        Log.d(TAG, "testPromoDatabase called");

        // Test direct database access
        promoRepository.testDirectDatabaseAccess(new PromoRepository.PromoCallback() {
            @Override
            public void onSuccess(List<Promo> fetchedPromos) {
                Log.d(TAG, "testPromoDatabase SUCCESS: Got " + fetchedPromos.size() + " promos");

                for (Promo promo : fetchedPromos) {
                    Log.d(TAG, "Test Promo: ID=" + promo.getPromoId() +
                            ", Name='" + promo.getPromoName() + "'" +
                            ", Active=" + promo.isActive() +
                            ", DisplayName='" + promo.getDisplayName() + "'");
                }

                // Show a toast with the count
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this,
                            "Database test: Found " + fetchedPromos.size() + " promos",
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "testPromoDatabase ERROR: " + message);

                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this,
                            "Database test error: " + message,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // UPDATED: Using PromoRepository instead of PromoApiHelper
    private void initializePromoComponents() {
        // CHANGED: Initialize repository instead of API helper
        promoRepository = new PromoRepository(this);

        // Find promo views (only if they exist in your layout)
        promosRecyclerView = findViewById(R.id.promos_recycler_view);
        noPromosTextView = findViewById(R.id.no_promos_text_view);
        promosProgressBar = findViewById(R.id.promos_progress_bar);
        promosHeaderTextView = findViewById(R.id.promos_header_text_view);

        // Setup promos RecyclerView if it exists
        if (promosRecyclerView != null) {
            promosRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            promoAdapter = new PromoAdapter(this, promos);
            promoAdapter.setOnPromoClickListener(promo -> {
                // UPDATED: Handle promo click - use description as the display text
                String displayText = (promo.getPromoDescription() != null && !promo.getPromoDescription().trim().isEmpty())
                        ? promo.getPromoDescription()
                        : promo.getDisplayName();
                Toast.makeText(this, "Clicked on: " + displayText, Toast.LENGTH_SHORT).show();
            });
            promosRecyclerView.setAdapter(promoAdapter);

            // Load promos
            loadPromos();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for active session when activity resumes
        checkActiveCashierSession();

        // Re-register shake detection if needed
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister shake detection to save battery
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up shake detection
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    /**
     * Checks if there's an active cashier session by calling the API
     * NOW USES CHUCKER-ENABLED HTTP CLIENT
     */
    private void checkActiveCashierSession() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        sessionStatusTextView.setText(getString(R.string.checking_session_status));

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("User-Agent", "RestaurantApp/1.0")
                .addHeader("X-Debug", "DashboardActivity")
                .build();

        // This call will now be visible in Chucker! 🎉
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check active session", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingProgressBar != null) {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                        sessionStatusTextView.setText(getString(R.string.error_checking_session));
                        Toast.makeText(DashboardActivity.this,
                                getString(R.string.failed_to_check_session, e.getMessage()),
                                Toast.LENGTH_SHORT).show();

                        // Show open session button when there's an error
                        openSessionButton.setVisibility(View.VISIBLE);
                        endSessionButton.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException(getString(R.string.unexpected_response_code) + response);
                    }

                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);

                    final boolean hasActiveSession = "success".equals(jsonObject.getString("status")) &&
                            jsonObject.has("data") &&
                            !jsonObject.isNull("data");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (loadingProgressBar != null) {
                                loadingProgressBar.setVisibility(View.GONE);
                            }

                            if (hasActiveSession) {
                                try {
                                    JSONObject sessionData = jsonObject.getJSONObject("data");
                                    String cashierName = sessionData.getString("cashier_name");
                                    String openingAmount = sessionData.getString("opening_amount");

                                    // Get the full response data for debugging
                                    Log.d(TAG, "Full session data: " + sessionData);

                                    // Store the session ID from the new API response format
                                    if (sessionData.has("session_id")) {
                                        long activeSessionId = sessionData.getLong("session_id");
                                        Log.d(TAG, "Extracted session ID: " + activeSessionId);

                                        // Store in SharedPreferences
                                        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putLong(getString(R.string.pref_active_session_id), activeSessionId);
                                        editor.apply();

                                        // Verify it was saved correctly
                                        long savedId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);
                                        Log.d(TAG, "Verified saved session ID: " + savedId);
                                    } else {
                                        Log.e(TAG, "Session data does not contain 'session_id' field!");
                                        // Log the keys present in sessionData for debugging
                                        Iterator<String> keys = sessionData.keys();
                                        StringBuilder keysBuilder = new StringBuilder("Available keys: ");
                                        while (keys.hasNext()) {
                                            keysBuilder.append(keys.next()).append(", ");
                                        }
                                        Log.d(TAG, keysBuilder.toString());
                                    }

                                    sessionStatusTextView.setText(getString(R.string.active_session_cashier, cashierName));

                                    // Update button visibility
                                    openSessionButton.setVisibility(View.GONE);
                                    endSessionButton.setVisibility(View.VISIBLE);

                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing session data", e);
                                }
                            } else {
                                // No active session found in the response
                                Log.d(TAG, "No active session found in API response");

                                sessionStatusTextView.setText(getString(R.string.no_active_session));

                                // Show open session button when no active session
                                openSessionButton.setVisibility(View.VISIBLE);
                                endSessionButton.setVisibility(View.GONE);
                            }
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (loadingProgressBar != null) {
                                loadingProgressBar.setVisibility(View.GONE);
                            }
                            sessionStatusTextView.setText(getString(R.string.error_checking_session));
                            Toast.makeText(DashboardActivity.this,
                                    getString(R.string.error_processing_response, e.getMessage()),
                                    Toast.LENGTH_SHORT).show();

                            // Show open session button in case of error
                            openSessionButton.setVisibility(View.VISIBLE);
                            endSessionButton.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat(getString(R.string.time_format), Locale.getDefault());
        Date currentDate = new Date();

        String dateTimeString = dateFormat.format(currentDate) + " " + timeFormat.format(currentDate);
        dateTimeTextView.setText(dateTimeString);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            // Already in dashboard activity
            Toast.makeText(this, getString(R.string.already_in_dashboard), Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_orders) {
            try {
                // Navigate to OrderActivity
                Intent intent = new Intent(DashboardActivity.this, OrderListActivity.class);

                // Get session ID from SharedPreferences (now updated with session_id from API)
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                long sessionId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);

                // If no session ID found, fall back to the default from API example
                if (sessionId == -1) {
                    sessionId = 16; // Fallback value if not found in SharedPreferences
                    Log.d(TAG, "No session ID in SharedPreferences, using default: " + sessionId);
                } else {
                    Log.d(TAG, "Using session ID from API: " + sessionId);
                }

                // Pass the session ID to OrderActivity
                intent.putExtra("session_id", sessionId);

                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting OrderActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.nav_transactions) {
            // Handle the new transaction menu item
            Intent intent = new Intent(this, TransactionActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_products) {
            // Handle the new transaction menu item
            Intent intent = new Intent(this, ProductListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_taxes) {
            // Handle the new tax menu item
            try {
                Intent intent = new Intent(this, TaxListActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting TaxListActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.nav_cashier) {
            // Navigate to CashierActivity
            try {
                Intent intent = new Intent(DashboardActivity.this, CashierActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting CashierActivity", e);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.nav_discounts) {
            // Handle discounts navigation
            Intent intent = new Intent(this, DiscountListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_logout) {
            // Clear session data
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Navigate to login screen
            Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}