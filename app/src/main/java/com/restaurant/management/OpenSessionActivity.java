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

import com.restaurant.management.database.RestaurantDatabase;
import com.restaurant.management.database.repository.CashDenominationRepository;
import com.restaurant.management.database.repository.CashierSessionRepository;
import com.restaurant.management.models.CashDenomination;
import com.restaurant.management.models.CashierSession;

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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    private CashierSessionRepository cashierSessionRepository;
    private CashDenominationRepository cashDenominationRepository;
    private ExecutorService executorService;

    private List<CashDenomination> denominations;
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

        // Initialize database and repositories (still needed for denominations)
        RestaurantDatabase database = RestaurantDatabase.getInstance(this);
        cashierSessionRepository = new CashierSessionRepository(database.cashierSessionDao());
        cashDenominationRepository = new CashDenominationRepository(database.cashDenominationDao());

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
            denominations = cashDenominationRepository.getAllDenominations();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (denominations != null && !denominations.isEmpty()) {
                    createDenominationInputs();
                } else {
                    Toast.makeText(this, "No denominations found", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void createDenominationInputs() {
        denominationsContainer.removeAllViews();

        for (CashDenomination denomination : denominations) {
            View denominationView = getLayoutInflater().inflate(R.layout.item_denomination_input, null);

            TextView denominationValueText = denominationView.findViewById(R.id.denomination_value_text);
            EditText quantityEditText = denominationView.findViewById(R.id.quantity_edit_text);
            TextView amountTextView = denominationView.findViewById(R.id.amount_text_view);

            denominationValueText.setText(String.valueOf(denomination.getValue()));

            // Add text change listener to update totals
            quantityEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    try {
                        int quantity = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                        int amount = quantity * denomination.getValue();
                        amountTextView.setText(String.valueOf(amount));
                        updateTotalAmount();
                    } catch (NumberFormatException e) {
                        amountTextView.setText("0");
                        updateTotalAmount();
                    }
                }
            });

            denominationInputs.put(denomination.getValue(), quantityEditText);
            denominationsContainer.addView(denominationView);
        }
    }

    private void updateTotalAmount() {
        BigDecimal total = calculateTotalAmount();
        totalAmountTextView.setText(String.format("Total: %s", total.toString()));
    }

    private BigDecimal calculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;

        for (CashDenomination denomination : denominations) {
            EditText quantityEditText = denominationInputs.get(denomination.getValue());
            if (quantityEditText != null) {
                String quantityStr = quantityEditText.getText().toString();
                if (!quantityStr.isEmpty()) {
                    try {
                        int quantity = Integer.parseInt(quantityStr);
                        BigDecimal amount = new BigDecimal(denomination.getValue() * quantity);
                        total = total.add(amount);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return total;
    }

    private void confirmOpenSession() {
        BigDecimal openingAmount = calculateTotalAmount();

        if (openingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            Toast.makeText(this, "Opening amount must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        String notes = notesEditText.getText().toString();

        progressBar.setVisibility(View.VISIBLE);
        confirmOpenSessionButton.setEnabled(false);

        executorService.execute(() -> {
            // First check if there's an active session (still using local DB for this check)
            CashierSession activeSession = cashierSessionRepository.getActiveSessionForUser(userId);

            if (activeSession != null) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmOpenSessionButton.setEnabled(true);
                    Toast.makeText(OpenSessionActivity.this, "You already have an active session", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            // Open session via API
            try {
                // Create the API request
                JSONObject requestBody = new JSONObject();
                requestBody.put("user_id", userId);
                requestBody.put("opening_amount", openingAmount);
                requestBody.put("notes", notes);

                // Convert JSONObject to String
                String jsonInputString = requestBody.toString();

                // Log the request for debugging
                Log.d(TAG, "API Request: " + jsonInputString);

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
                        Toast.makeText(OpenSessionActivity.this, "Session opened successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Try to extract error message from response
                        String errorMessage = "Failed to open session";
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            if (jsonResponse.has("message")) {
                                errorMessage = jsonResponse.getString("message");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing error response", e);
                        }

                        Toast.makeText(OpenSessionActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error opening session", e);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    confirmOpenSessionButton.setEnabled(true);
                    Toast.makeText(OpenSessionActivity.this,
                            "Network error. Please check your connection and try again.",
                            Toast.LENGTH_SHORT).show();
                });
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