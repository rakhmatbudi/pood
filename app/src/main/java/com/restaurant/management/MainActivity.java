package com.restaurant.management;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;

// JSON imports
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

// Networking imports
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;

// IO imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

// Charset imports
import java.nio.charset.StandardCharsets;

// String handling
import java.lang.StringBuilder;

// Collections (if needed)
import java.util.HashMap;
import java.util.Map;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // Make sure this import is correct
import androidx.appcompat.app.AppCompatActivity;

import com.restaurant.management.database.RestaurantDatabase;
import com.restaurant.management.database.repository.UserRepository;
import com.restaurant.management.models.User;
import com.restaurant.management.utils.PasswordHasher;
import org.json.JSONObject;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "https://api.pood.lol/users/login";

    private static final String TAG = "MainActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private ExecutorService executorService;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        // Make sure these IDs match exactly with your layout XML
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize database and repository
        //RestaurantDatabase database = RestaurantDatabase.getInstance(this);
        //userRepository = new UserRepository(database.userDao());

        executorService = Executors.newSingleThreadExecutor();

        // Check if already logged in
        checkLoginStatus();

        // Set up login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void bypassLogin() {
        // Get a user ID directly from the database
        executorService.execute(() -> {
            bypassLogin();
            RestaurantDatabase database = RestaurantDatabase.getInstance(this);
            User anyUser = database.userDao().getAnyUser(); // You'll need to add this method

            if (anyUser != null) {
                runOnUiThread(() -> {
                    // Save login state
                    SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putInt("userId", anyUser.getId());
                    editor.putString("userName", anyUser.getName());
                    editor.putInt("userRole", anyUser.getRoleId());
                    editor.apply();

                    Toast.makeText(MainActivity.this, "Debug login successful", Toast.LENGTH_SHORT).show();
                    navigateToCashierActivity(anyUser.getId());
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "No users found in database", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void checkLoginStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        int userId = sharedPreferences.getInt("userId", -1);

        if (isLoggedIn && userId != -1) {
            navigateToCashierActivity(userId);
        }
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();


        // Simple validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        executorService.execute(() -> {
            try {
                // Create the API request
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);

                // Convert JSONObject to String
                String jsonInputString = requestBody.toString();

                // Create connection
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                // Write request body
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Get response code
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "API Response Code: " + responseCode);

                // Read response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode == HttpURLConnection.HTTP_OK ?
                                connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                }

                final String responseData = response.toString();
                Log.d(TAG, "API Response: " + responseData);

                // Process response
                final boolean loginSuccess = responseCode == HttpURLConnection.HTTP_OK;

                // Parse user data if login successful
                int userId = -1;
                String userName = "";
                int userRole = -1;

                if (loginSuccess) {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        JSONObject userData = jsonResponse.getJSONObject("data");
                        userId = userData.getInt("id");
                        userName = userData.getString("name");
                        userRole = userData.getInt("role_id");
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response", e);
                    }
                }

                // Store final values for UI thread
                final int finalUserId = userId;
                final String finalUserName = userName;
                final int finalUserRole = userRole;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (loginSuccess) {
                        // Save login state
                        SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putInt("userId", finalUserId);
                        editor.putString("userName", finalUserName);
                        editor.putInt("userRole", finalUserRole);
                        editor.apply();

                        Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToCashierActivity(finalUserId);
                    } else {
                        // Try to extract error message from response
                        String errorMessage = "Invalid email or password";
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            if (jsonResponse.has("message")) {
                                errorMessage = jsonResponse.getString("message");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }

                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error during login", e);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "Network error. Please check your connection and try again.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void navigateToCashierActivity(int userId) {
        Intent intent = new Intent(MainActivity.this, CashierActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }


}