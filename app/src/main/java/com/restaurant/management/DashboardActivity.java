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

    private int userId;
    private String userName;
    private List<CashierSession> cashierSessions = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_dashboard);

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

            // Try to find the RecyclerView components, but don't crash if not found
            pastSessionsRecyclerView = findViewById(R.id.past_sessions_recycler_view);
            noPastSessionsTextView = findViewById(R.id.no_past_sessions_text_view);
            loadingProgressBar = findViewById(R.id.loading_progress_bar);

            // Make sure button is visible and enabled
            if (openSessionButton != null) {
                openSessionButton.setVisibility(View.VISIBLE);
                openSessionButton.setEnabled(true);
            }

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
            sessionStatusTextView.setTextColor(getResources().getColor(R.color.black));

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
                    Toast.makeText(DashboardActivity.this,
                            getString(R.string.end_session_clicked),
                            Toast.LENGTH_SHORT).show();

                    // Here you would add code to end the session via API
                    // After ending session, check session status again
                    checkActiveCashierSession();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in DashboardActivity.onCreate", e);
            Toast.makeText(this,
                    getString(R.string.error_initializing, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for active session when activity resumes
        checkActiveCashierSession();
    }

    /**
     * Checks if there's an active cashier session by calling the API
     */
    private void checkActiveCashierSession() {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        sessionStatusTextView.setText(getString(R.string.checking_session_status));

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

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
                        sessionStatusTextView.setTextColor(getResources().getColor(R.color.red));
                        Toast.makeText(DashboardActivity.this,
                                getString(R.string.failed_to_check_session, e.getMessage()),
                                Toast.LENGTH_SHORT).show();

                        // Default to allowing open session when we can't check
                        openSessionButton.setEnabled(true);
                        endSessionButton.setEnabled(false);
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

                                    sessionStatusTextView.setText(getString(R.string.active_session_cashier, cashierName));
                                    sessionStatusTextView.setTextColor(getResources().getColor(R.color.green));

                                    // Disable open session button, enable end session button
                                    openSessionButton.setEnabled(false);
                                    endSessionButton.setEnabled(true);

                                    Toast.makeText(DashboardActivity.this,
                                            getString(R.string.session_opened_by_with_amount, cashierName, openingAmount),
                                            Toast.LENGTH_SHORT).show();

                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing session data", e);
                                }
                            } else {
                                sessionStatusTextView.setText(getString(R.string.no_active_session));
                                sessionStatusTextView.setTextColor(getResources().getColor(R.color.red));

                                // Enable open session button, disable end session button
                                openSessionButton.setEnabled(true);
                                endSessionButton.setEnabled(false);
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
                            sessionStatusTextView.setTextColor(getResources().getColor(R.color.red));
                            Toast.makeText(DashboardActivity.this,
                                    getString(R.string.error_processing_response, e.getMessage()),
                                    Toast.LENGTH_SHORT).show();
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
        } else if (id == R.id.nav_logout) {
            // Clear session data
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            // Navigate to login screen
            Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
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