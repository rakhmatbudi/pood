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
import com.restaurant.management.models.ApiResponse; // Import the ApiResponse wrapper
import com.restaurant.management.network.ApiClient; // Import ApiClient
import com.restaurant.management.network.ApiService; // Import ApiService

import com.chuckerteam.chucker.api.Chucker;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call; // Retrofit Call
import retrofit2.Callback; // Retrofit Callback
import retrofit2.Response; // Retrofit Response

// No longer need these OkHttp specific imports since we're using Retrofit's Callbacks
// import okhttp3.Call;
// import okhttp3.Callback;
// import okhttp3.OkHttpClient;
// import okhttp3.Request;
// import okhttp3.Response;
// import org.json.JSONException; // No longer needed for parsing
// import org.json.JSONObject; // No longer needed for parsing
// import java.util.Iterator; // No longer needed for parsing

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "DashboardActivity";
    // private static final String API_URL = "https://api.pood.lol/cashier-sessions/current"; // REMOVED: Handled by ApiService

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
    private String userName; // This will be used to display the cashier's name if not provided by API
    private List<CashierSession> cashierSessions = new ArrayList<>();

    // CHANGED: Use ApiService for network calls
    private ApiService apiService;

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

            // CHANGED: Initialize ApiService using ApiClient
            apiService = ApiClient.getApiService(this);

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
            userName = sharedPreferences.getString(getString(R.string.pref_user_name), ""); // Get user name for display

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
                        // No need to pass userId if OpenSessionActivity can read from SharedPreferences
                        startActivity(intent);

                        // After returning from OpenSessionActivity, onResume will handle session check
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

    // REMOVED: initializeHttpClient() method is no longer needed as ApiClient handles it

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

    // REMOVED: testChuckerRequest() is not needed as ApiService handles network calls

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

        // Load from offline database instead of API
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

    private void initializePromoComponents() {
        // Initialize repository instead of API helper
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
     * Checks if there's an active cashier session by calling the API using ApiService.
     * The Authorization header is handled by ApiClient's interceptor.
     */
    private void checkActiveCashierSession() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        sessionStatusTextView.setText(getString(R.string.checking_session_status));

        // Use ApiService to make the call to the /cashier-sessions/open endpoint
        apiService.getCurrentCashierSession().enqueue(new Callback<ApiResponse<CashierSession>>() {
            @Override
            public void onResponse(Call<ApiResponse<CashierSession>> call, Response<ApiResponse<CashierSession>> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingProgressBar != null) {
                            loadingProgressBar.setVisibility(View.GONE);
                        }

                        if (response.isSuccessful()) { // HTTP 200-299 status code
                            ApiResponse<CashierSession> apiResponse = response.body();

                            // Check if the API response itself indicates success and has data
                            if (apiResponse != null && apiResponse.isSuccess() && apiResponse.getData() != null) {
                                CashierSession sessionData = apiResponse.getData(); // Get the actual session object

                                if (sessionData.getSessionId() != null) {
                                    // Active session found
                                    Log.d(TAG, "Active session found: ID=" + sessionData.getSessionId() + ", User ID=" + sessionData.getUserId());

                                    // Store the session ID
                                    long activeSessionId = sessionData.getSessionId();
                                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putLong(getString(R.string.pref_active_session_id), activeSessionId);
                                    editor.apply();

                                    // Display user name from SharedPreferences as API response doesn't provide it directly
                                    sessionStatusTextView.setText(getString(R.string.active_session_cashier, userName));

                                    openSessionButton.setVisibility(View.GONE);
                                    endSessionButton.setVisibility(View.VISIBLE);
                                } else {
                                    // API responded successfully, but data is empty/null, or session ID is null
                                    Log.d(TAG, "API Response indicates no active session data or null session ID: " + apiResponse.getMessage());
                                    sessionStatusTextView.setText(getString(R.string.no_active_session));
                                    openSessionButton.setVisibility(View.VISIBLE);
                                    endSessionButton.setVisibility(View.GONE);

                                    // Clear potentially stale active session ID from SharedPreferences
                                    SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(getString(R.string.pref_active_session_id));
                                    editor.apply();
                                }
                            } else {
                                // API responded successfully (e.g., 200 OK) but internal status is 'error'
                                // or 'data' is explicitly null indicating no active session via the API's logic.
                                String message = apiResponse != null && apiResponse.getMessage() != null
                                        ? apiResponse.getMessage()
                                        : getString(R.string.no_active_session); // Default message if API message is null
                                Log.d(TAG, "API success response, but no active session or status not 'success': " + message);
                                sessionStatusTextView.setText(getString(R.string.no_active_session));
                                Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                                openSessionButton.setVisibility(View.VISIBLE);
                                endSessionButton.setVisibility(View.GONE);

                                // Clear potentially stale active session ID
                                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.remove(getString(R.string.pref_active_session_id));
                                editor.apply();
                            }
                        } else {
                            // HTTP error (e.g., 400, 401, 500)
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (IOException e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                            Log.e(TAG, "HTTP Error checking active session. Code: " + response.code() + ", Message: " + response.message() + ", Error Body: " + errorBody);
                            sessionStatusTextView.setText(getString(R.string.error_checking_session));
                            Toast.makeText(DashboardActivity.this,
                                    getString(R.string.failed_to_check_session, response.message()),
                                    Toast.LENGTH_SHORT).show();

                            // Handle 401 Unauthorized specifically
                            if (response.code() == 401) {
                                Toast.makeText(DashboardActivity.this, getString(R.string.session_expired_relogin), Toast.LENGTH_LONG).show();
                                // Clear session data and force logout
                                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                editor.apply();
                                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                openSessionButton.setVisibility(View.VISIBLE);
                                endSessionButton.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<CashierSession>> call, Throwable t) {
                Log.e(TAG, "Network error checking active session: " + t.getMessage(), t);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loadingProgressBar != null) {
                            loadingProgressBar.setVisibility(View.GONE);
                        }
                        sessionStatusTextView.setText(getString(R.string.error_checking_session));
                        Toast.makeText(DashboardActivity.this,
                                getString(R.string.failed_to_check_session, t.getMessage()),
                                Toast.LENGTH_SHORT).show();

                        openSessionButton.setVisibility(View.VISIBLE);
                        endSessionButton.setVisibility(View.GONE);
                    }
                });
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

                // Get session ID from SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                long sessionId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);

                // If no session ID found, inform the user or handle appropriately
                if (sessionId == -1) {
                    Toast.makeText(DashboardActivity.this, R.string.error_no_active_session, Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Attempted to navigate to Orders without an active session ID.");
                    return true; // Stay on dashboard
                } else {
                    Log.d(TAG, "Using session ID from SharedPreferences: " + sessionId);
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
        } else if (id == R.id.nav_offline_data) {
            // Launch Offline Data Management Activity
            Intent intent = new Intent(this, OfflineDataActivity.class);
            startActivity(intent);
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