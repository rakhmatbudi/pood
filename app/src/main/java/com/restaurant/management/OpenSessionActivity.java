package com.restaurant.management;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenSessionActivity extends AppCompatActivity {

    private static final String TAG = "OpenSessionActivity";
    private static final String API_URL = "https://api.pood.lol/cashier-sessions/open";

    private int userId;
    private LinearLayout denominationsContainer;
    private EditText notesEditText;
    private Button confirmOpenSessionButton;
    private TextView totalAmountTextView;
    private ProgressBar progressBar;
    private ExecutorService executorService;
    private Map<Integer, EditText> denominationInputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_session);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Open Cashier Session");

        // Get userId from intent
        userId = getIntent().getIntExtra("userId", -1);

        // Initialize UI components
        denominationsContainer = findViewById(R.id.denominations_container);
        notesEditText = findViewById(R.id.notes_edit_text);
        confirmOpenSessionButton = findViewById(R.id.confirm_open_session_button);
        totalAmountTextView = findViewById(R.id.total_amount_text_view);
        progressBar = findViewById(R.id.progress_bar);

        executorService = Executors.newSingleThreadExecutor();
        denominationInputs = new HashMap<>();

        // Load denominations
        loadDenominations();

        // Set up button click listener
        confirmOpenSessionButton.setOnClickListener(v -> confirmOpenSession());
    }

    private void loadDenominations() {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                // Create connection to the denominations API
                URL url = new URL("https://api.pood.lol/cash-denominations");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                // Get response code
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Denominations API Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                    }

                    final String responseData = response.toString();
                    Log.d(TAG, "Denominations API Response: " + responseData);

                    // Parse response JSON - UPDATED for nested structure
                    JSONObject jsonResponse = new JSONObject(responseData);

                    // Check if we have data and it contains denominations
                    if (jsonResponse.has("data")) {
                        JSONObject dataObject = jsonResponse.getJSONObject("data");

                        if (dataObject.has("denominations")) {
                            // Get the denominations array from the data object
                            org.json.JSONArray denominationsArray = dataObject.getJSONArray("denominations");

                            // Extract denomination values
                            int[] denominations = new int[denominationsArray.length()];
                            for (int i = 0; i < denominationsArray.length(); i++) {
                                JSONObject denominationObj = denominationsArray.getJSONObject(i);
                                denominations[i] = denominationObj.getInt("value");
                            }

                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                createDenominationInputs(denominations);
                            });
                        } else {
                            Log.e(TAG, "Data object missing 'denominations' array");
                            loadDefaultDenominations();
                        }
                    } else {
                        Log.e(TAG, "API response missing 'data' field");
                        loadDefaultDenominations();
                    }
                } else {
                    // Handle error response
                    loadDefaultDenominations();
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error loading denominations", e);
                loadDefaultDenominations();
            }
        });
    }

    // Extracted method for loading default denominations to avoid code duplication
    private void loadDefaultDenominations() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(OpenSessionActivity.this,
                    "Failed to load denominations. Using default values.",
                    Toast.LENGTH_SHORT).show();
            // Fallback to some default denominations
            int[] defaultDenominations = {100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100};
            createDenominationInputs(defaultDenominations);
        });
    }

    private void createDenominationInputs(int[] denominations) {
        denominationsContainer.removeAllViews();
        denominationInputs.clear();

        for (int denomination : denominations) {
            // Create a horizontal LinearLayout with the same weight distribution as the header
            LinearLayout itemView = new LinearLayout(this);
            itemView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            itemView.setOrientation(LinearLayout.HORIZONTAL);
            itemView.setPadding(8, 16, 8, 16); // Match padding with header

            // Create denomination text view - with layout_weight="1" to match header
            TextView denominationTextView = new TextView(this);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            denominationTextView.setLayoutParams(textParams);
            denominationTextView.setTextSize(16);

            // Create count edit text - with layout_weight="1" to match header
            EditText countEditText = new EditText(this);
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            countEditText.setLayoutParams(editParams);
            countEditText.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            countEditText.setHint("0");

            // Format denomination as currency
            String formattedDenomination = String.format("%,d", denomination);
            denominationTextView.setText(formattedDenomination);

            // Add views to layout
            itemView.addView(denominationTextView);
            itemView.addView(countEditText);

            // Store the EditText for later use
            denominationInputs.put(denomination, countEditText);

            // Add text change listener to update total
            countEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    updateTotalAmount();
                }
            });

            denominationsContainer.addView(itemView);
        }
    }

    private void updateTotalAmount() {
        BigDecimal total = calculateTotalAmount();
        totalAmountTextView.setText(String.format("Total: %,d", total.intValue()));
    }

    private BigDecimal calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Integer, EditText> entry : denominationInputs.entrySet()) {
            int denomination = entry.getKey();
            EditText countEditText = entry.getValue();

            String countText = countEditText.getText().toString().trim();
            if (!countText.isEmpty()) {
                try {
                    int count = Integer.parseInt(countText);
                    BigDecimal amount = BigDecimal.valueOf(denomination).multiply(BigDecimal.valueOf(count));
                    total = total.add(amount);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing count for denomination " + denomination, e);
                }
            }
        }

        return total;
    }

    private void confirmOpenSession() {
        // Calculate the actual amount from user inputs
        BigDecimal openingAmount = calculateTotalAmount();

        if (openingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            Toast.makeText(this, getString(R.string.amount_must_be_greater_than_zero), Toast.LENGTH_SHORT).show();
            return;
        }

        String notes = notesEditText.getText().toString().trim();

        // Show loading indicators
        progressBar.setVisibility(View.VISIBLE);
        confirmOpenSessionButton.setEnabled(false);

        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Create the API request
                JSONObject requestBody = new JSONObject();
                requestBody.put("user_id", userId);
                // Convert BigDecimal to int to avoid potential format issues
                requestBody.put("opening_amount", openingAmount.intValue());
                requestBody.put("notes", notes);

                // Convert JSONObject to String
                String jsonInputString = requestBody.toString();

                // Log the request for debugging
                Log.d(TAG, "API Request URL: " + API_URL);
                Log.d(TAG, "API Request Body: " + jsonInputString);

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
                        responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED ?
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
                final boolean success = responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmOpenSessionButton.setEnabled(true);

                    if (success) {
                        Toast.makeText(OpenSessionActivity.this, getString(R.string.session_opened_successfully), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Try to extract error message from response
                        String errorMessage = getString(R.string.failed_to_open_session);
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);

                            // Log the entire error response for debugging
                            Log.e(TAG, "Error response JSON: " + responseData);

                            if (jsonResponse.has("message")) {
                                errorMessage = jsonResponse.getString("message");
                                Log.e(TAG, "Error message from API: " + errorMessage);
                            }

                            // Also check for other error indicators
                            if (jsonResponse.has("errors")) {
                                Log.e(TAG, "Validation errors: " + jsonResponse.get("errors"));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }

                        // Show error message
                        final String finalErrorMessage = errorMessage;
                        Toast.makeText(OpenSessionActivity.this, finalErrorMessage, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error opening session: " + e.getMessage(), e);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmOpenSessionButton.setEnabled(true);
                    Toast.makeText(OpenSessionActivity.this,
                            getString(R.string.network_error) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}