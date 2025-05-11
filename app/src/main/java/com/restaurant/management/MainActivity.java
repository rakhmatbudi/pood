package com.restaurant.management;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

// JSON imports
import org.json.JSONObject;
import org.json.JSONException;

// Networking imports
import java.net.URL;
import java.net.HttpURLConnection;

// IO imports
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

// Charset imports
import java.nio.charset.StandardCharsets;

// String handling
import java.lang.StringBuilder;

// Collections (if needed)
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String API_URL = "https://api.pood.lol/users/login";

    private static final String TAG = "MainActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);

        executorService = Executors.newSingleThreadExecutor();

        // Check if already logged in
        checkLoginStatus();

        // Set up login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());
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
            HttpURLConnection connection = null;
            try {
                // Create the API request
                JSONObject requestBody = new JSONObject();
                requestBody.put("email", email);
                requestBody.put("password", password);

                // Convert JSONObject to String
                String jsonInputString = requestBody.toString();

                // Log the request for debugging
                Log.d(TAG, "Sending login request to: " + API_URL);
                Log.d(TAG, "Request body: " + jsonInputString);

                // Create connection
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000); // 15 seconds timeout
                connection.setReadTimeout(15000);    // 15 seconds read timeout

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

                // Initialize user data variables
                int userId = -1;
                String userName = "";
                int userRole = -1;
                String token = "";

                if (loginSuccess) {
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);

                        // Check if the response has a "data" field
                        if (jsonResponse.has("data")) {
                            JSONObject data = jsonResponse.getJSONObject("data");

                            // The user data is now nested under "data.user"
                            if (data.has("user")) {
                                JSONObject userData = data.getJSONObject("user");

                                // Log the full userData for debugging
                                Log.d(TAG, "User data from API: " + userData.toString());

                                // Extract user ID
                                if (userData.has("id")) {
                                    userId = userData.getInt("id");
                                    Log.d(TAG, "Extracted user ID: " + userId);
                                } else {
                                    Log.e(TAG, "User data missing 'id' field");
                                }

                                // Extract user name
                                if (userData.has("name")) {
                                    userName = userData.getString("name");
                                    Log.d(TAG, "Extracted user name: " + userName);
                                } else {
                                    Log.e(TAG, "User data missing 'name' field");
                                }

                                // Extract user role
                                if (userData.has("role_id")) {
                                    userRole = userData.getInt("role_id");
                                    Log.d(TAG, "Extracted user role: " + userRole);
                                } else {
                                    Log.e(TAG, "User data missing 'role_id' field");
                                }
                            } else {
                                Log.e(TAG, "API response missing 'data.user' field");
                            }

                            // Extract the authentication token
                            if (data.has("token")) {
                                token = data.getString("token");
                                Log.d(TAG, "Extracted auth token: " + token);
                            } else {
                                Log.e(TAG, "API response missing 'data.token' field");
                            }
                        } else {
                            Log.e(TAG, "API response missing 'data' field");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage(), e);
                    }
                }

                // Store final values for UI thread
                final int finalUserId = userId;
                final String finalUserName = userName;
                final int finalUserRole = userRole;
                final String finalToken = token;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (loginSuccess) {
                        // Validate that we got proper user data
                        if (finalUserId == -1) {
                            Toast.makeText(MainActivity.this,
                                    "Login successful but user ID not found in response",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Save login state
                        SharedPreferences sharedPreferences = getSharedPreferences("RestaurantApp", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putInt("userId", finalUserId);
                        editor.putString("userName", finalUserName);
                        editor.putInt("userRole", finalUserRole);
                        editor.putString("token", finalToken); // Also save the token for future API calls
                        editor.apply();

                        Log.d(TAG, "Saved to SharedPreferences - userId: " + finalUserId +
                                ", userName: " + finalUserName + ", userRole: " + finalUserRole);

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
                Log.e(TAG, "Error during login: " + e.getMessage(), e);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    Toast.makeText(MainActivity.this,
                            "Network error. Please check your connection and try again.",
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
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