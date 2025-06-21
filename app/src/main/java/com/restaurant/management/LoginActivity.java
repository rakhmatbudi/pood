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

public class LoginActivity extends AppCompatActivity {
    private static final String API_URL = "https://api.pood.lol/users/login";

    private static final String TAG = "LoginActivity"; // Changed from MainActivity for consistency
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Switch from launch theme to regular theme
        //setTheme(R.style.AppTheme); // Or whatever your main theme is called

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);

        executorService = Executors.newSingleThreadExecutor();

        // Set up login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Simple validation
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_enter_email_password), Toast.LENGTH_SHORT).show();
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
                StringBuilder responseBuilder = new StringBuilder(); // Renamed to avoid clash with method
                try (BufferedReader br = new BufferedReader(new InputStreamReader(
                        responseCode == HttpURLConnection.HTTP_OK ?
                                connection.getInputStream() : connection.getErrorStream(),
                        StandardCharsets.UTF_8))) {
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        responseBuilder.append(responseLine.trim());
                    }
                }

                final String responseData = responseBuilder.toString(); // Use the renamed StringBuilder
                Log.d(TAG, "API Response: " + responseData);

                // --- START OF AUTHORIZATION CHECK FIX ---
                boolean loginSuccessfulBasedOnAPI = false; // Default to false
                int userId = -1;
                String userName = "";
                int userRole = -1;
                String token = "";
                String apiMessage = getString(R.string.network_error); // Default message for general issues

                try {
                    JSONObject jsonResponse = new JSONObject(responseData);

                    // Extract status and message first, as they are usually top-level
                    String status = jsonResponse.optString("status", "error"); // Use optString for safety
                    apiMessage = jsonResponse.optString("message", getString(R.string.invalid_credentials));

                    // Only consider successful if HTTP OK AND API's internal status is "success"
                    if (responseCode == HttpURLConnection.HTTP_OK && "success".equals(status)) {
                        loginSuccessfulBasedOnAPI = true; // Potentially successful, now parse data

                        // Check for "data" field
                        if (jsonResponse.has("data")) {
                            JSONObject data = jsonResponse.getJSONObject("data");

                            // Check for "user" object within "data"
                            if (data.has("user")) {
                                JSONObject userData = data.getJSONObject("user");

                                // Log the full userData for debugging
                                Log.d(TAG, "User data from API: " + userData.toString());

                                // Extract user details using opt methods for robustness
                                userId = userData.optInt("id", -1);
                                userName = userData.optString("name", "");
                                userRole = userData.optInt("role_id", -1);

                                Log.d(TAG, "Extracted user ID: " + userId + ", Name: " + userName + ", Role: " + userRole);

                                // Basic validation for essential user data
                                if (userId == -1 || userName.isEmpty() || userRole == -1) {
                                    Log.e(TAG, "Critical user data missing despite success status.");
                                    loginSuccessfulBasedOnAPI = false; // Mark as failure if critical data is missing
                                    apiMessage = getString(R.string.login_failed_api_issue); // Specific error
                                }

                            } else {
                                Log.e(TAG, "API response missing 'data.user' field for success.");
                                loginSuccessfulBasedOnAPI = false; // Mark as failure if user data is missing
                                apiMessage = getString(R.string.login_failed_api_issue);
                            }

                            // Extract the authentication token
                            token = data.optString("token", "");
                            Log.d(TAG, "Extracted auth token: " + token);

                            if (token.isEmpty()) {
                                Log.e(TAG, "Authentication token missing despite success status.");
                                loginSuccessfulBasedOnAPI = false; // Mark as failure if token is missing
                                apiMessage = getString(R.string.login_failed_api_issue);
                            }

                        } else {
                            Log.e(TAG, "API response missing 'data' field for success.");
                            loginSuccessfulBasedOnAPI = false; // Mark as failure if data object is missing
                            apiMessage = getString(R.string.login_failed_api_issue);
                        }
                    } else {
                        // If HTTP is not 200 OK, or API status is not "success",
                        // apiMessage is already extracted from the JSON response or defaulted.
                        Log.w(TAG, "Login failed. HTTP: " + responseCode + ", API Status: " + status + ", Message: " + apiMessage);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON response for login: " + e.getMessage(), e);
                    loginSuccessfulBasedOnAPI = false; // JSON parsing error means login failed
                    apiMessage = getString(R.string.login_failed_api_issue); // General API issue message
                }
                // --- END OF AUTHORIZATION CHECK FIX ---


                // Store final values for UI thread
                final boolean finalLoginSuccess = loginSuccessfulBasedOnAPI;
                final int finalUserId = userId;
                final String finalUserName = userName;
                final int finalUserRole = userRole;
                final String finalToken = token;
                final String finalApiMessage = apiMessage; // Pass the specific API message

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);

                    if (finalLoginSuccess) {
                        // Save login state
                        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(getString(R.string.pref_is_logged_in), true);
                        editor.putInt(getString(R.string.pref_user_id), finalUserId);
                        editor.putString(getString(R.string.pref_user_name), finalUserName);
                        editor.putInt(getString(R.string.pref_user_role), finalUserRole);
                        editor.putString(getString(R.string.pref_token), finalToken); // Also save the token for future API calls
                        editor.apply();

                        Log.d(TAG, "Saved to SharedPreferences - userId: " + finalUserId +
                                ", userName: " + finalUserName + ", userRole: " + finalUserRole);

                        Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                        navigateToDashboardActivity(finalUserId);
                    } else {
                        // Display the error message obtained from the API or default
                        Toast.makeText(LoginActivity.this, finalApiMessage, Toast.LENGTH_LONG).show(); // Use LONG for error messages
                    }
                });

            } catch (IOException | JSONException e) { // <--- COMBINED CATCH BLOCK
                Log.e(TAG, "Error during login process (IO/JSON): " + e.getMessage(), e);

                final String displayMessage;
                // Determine the specific error message based on the exception type
                if (e instanceof JSONException) {
                    // This covers JSON errors when building requestBody OR parsing responseData
                    displayMessage = getString(R.string.login_failed_api_issue);
                } else {
                    // This covers network/IO errors
                    displayMessage = getString(R.string.network_error);
                }

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    loginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            displayMessage, // Use the determined message
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void navigateToDashboardActivity(int userId) {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        intent.putExtra(getString(R.string.extra_user_id), userId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}