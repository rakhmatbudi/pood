package com.restaurant.management;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.restaurant.management.database.RestaurantDatabase;
import com.restaurant.management.database.repository.CashierSessionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Absolute minimal version of CashierActivity with no API calls
 * to quickly fix any spinning icon issues
 */
public class CashierActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "CashierActivity";

    private DrawerLayout drawerLayout;
    private TextView userNameTextView;
    private TextView dateTimeTextView;
    private TextView sessionStatusTextView;
    private Button openSessionButton;
    private Button endSessionButton;

    private int userId;
    private String userName;
    private CashierSessionRepository cashierSessionRepository;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

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

        // Set up navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // Get user info from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        userName = sharedPreferences.getString("userName", "");

        userNameTextView.setText(userName);
        updateDateTime();

        // Initialize database and repository
        RestaurantDatabase database = RestaurantDatabase.getInstance(this);
        cashierSessionRepository = new CashierSessionRepository(database.cashierSessionDao());

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // Set up button click listeners
        openSessionButton.setOnClickListener(v -> navigateToOpenSession());
        endSessionButton.setOnClickListener(v -> navigateToEndSession());

        // Set initial UI state without making API calls
        sessionStatusTextView.setText("Status Unknown");
        sessionStatusTextView.setTextColor(getResources().getColor(R.color.black));
        openSessionButton.setEnabled(true);
        endSessionButton.setEnabled(false);
    }

    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date currentDate = new Date();

        String dateTimeString = dateFormat.format(currentDate) + " " + timeFormat.format(currentDate);
        dateTimeTextView.setText(dateTimeString);
    }

    private void navigateToOpenSession() {
        Intent intent = new Intent(CashierActivity.this, OpenSessionActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void navigateToEndSession() {
        // Use a fixed session ID of 1 for testing - replace this with your actual session ID logic
        Intent intent = new Intent(CashierActivity.this, EndSessionActivity.class);
        intent.putExtra("sessionId", 1);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void logout() {
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Clear session data
                    SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.clear();
                    editor.apply();

                    // Navigate to login screen
                    Intent intent = new Intent(CashierActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_cashier) {
            // Already in cashier activity
        } else if (id == R.id.nav_logout) {
            logout();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Don't allow back press to exit the app without logout
            new AlertDialog.Builder(this)
                    .setTitle("Exit")
                    .setMessage("Do you want to logout and exit?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        logout();
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDateTime();
        // Don't make API calls here to avoid spinning icons
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
}