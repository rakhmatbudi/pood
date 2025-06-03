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

import com.restaurant.management.models.CashierSession;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EndSessionActivity extends AppCompatActivity {
    private static final String TAG = "EndSessionActivity";

    private int sessionId;
    private int userId;
    private LinearLayout denominationsContainer;
    private LinearLayout paymentModesContainer;
    private EditText notesEditText;
    private Button confirmEndSessionButton;
    private TextView totalAmountTextView;
    private TextView expectedAmountTextView;
    private TextView differenceAmountTextView;
    private ProgressBar progressBar;

    private ExecutorService executorService;
    private ApiService apiService;
    private CashierSession activeSession;

    private Map<Integer, EditText> denominationInputs;
    private double expectedAmount = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_end_session);

            // Set up toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("End Cashier Session");
            }

            // Get sessionId and userId from intent
            sessionId = getIntent().getIntExtra("sessionId", -1);
            userId = getIntent().getIntExtra("userId", -1);

            // Add this before the validation check
            Log.d(TAG, "Received in intent - sessionId: " + getIntent().getIntExtra("sessionId", -1) +
                    ", userId: " + getIntent().getIntExtra("userId", -1));

            // Log all extras for debugging
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, "Extra: " + key + " = " + value +
                            (value != null ? " (type: " + value.getClass().getName() + ")" : ""));
                }
            } else {
                Log.d(TAG, "No extras found in intent");
            }

            if (sessionId == -1 || userId == -1) {
                Toast.makeText(this, "Invalid session or user information", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize UI components
            denominationsContainer = findViewById(R.id.denominations_container);
            paymentModesContainer = findViewById(R.id.payment_modes_container);
            notesEditText = findViewById(R.id.notes_edit_text);
            confirmEndSessionButton = findViewById(R.id.confirm_end_session_button);
            totalAmountTextView = findViewById(R.id.total_amount_text_view);
            expectedAmountTextView = findViewById(R.id.expected_amount_text_view);
            differenceAmountTextView = findViewById(R.id.difference_amount_text_view);
            progressBar = findViewById(R.id.progress_bar);

            executorService = Executors.newSingleThreadExecutor();
            denominationInputs = new HashMap<>();

            // Initialize Retrofit API service
            apiService = ApiClient.getClient(this).create(ApiService.class);

            // Set initial text values
            totalAmountTextView.setText("Total: 0");
            expectedAmountTextView.setText("Expected: 0");
            differenceAmountTextView.setText("Difference: 0");

            // Load session data
            loadSessionData();

            // Set up button click listener
            confirmEndSessionButton.setOnClickListener(v -> confirmEndSession());

        } catch (Exception e) {
            Log.e(TAG, "Error initializing EndSessionActivity", e);
            Toast.makeText(this, "Error initializing end session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadSessionData() {
        progressBar.setVisibility(View.VISIBLE);

        // Fetch the session details
        Call<CashierSession> call = apiService.getSessionById(sessionId);
        call.enqueue(new Callback<CashierSession>() {
            @Override
            public void onResponse(Call<CashierSession> call, Response<CashierSession> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    activeSession = response.body();
                    expectedAmount = activeSession.getStartAmount();

                    // Update the UI with session data
                    expectedAmountTextView.setText(String.format(Locale.getDefault(), "Expected: %.2f", expectedAmount));

                    // Set up the denominations and payment mode views
                    createDenominationInputs();
                    createPaymentModeViews();

                    // Set any existing notes
                    if (activeSession.getUserName() != null) {
                        notesEditText.setText(activeSession.getUserName());
                    }

                    // Update calculations
                    updateTotalAmount();

                } else {
                    Toast.makeText(EndSessionActivity.this, "Failed to load session data", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading session: " + response.code());
                    finish();
                }
            }

            @Override
            public void onFailure(Call<CashierSession> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EndSessionActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error loading session", t);
                finish();
            }
        });
    }

    private void createDenominationInputs() {
        denominationsContainer.removeAllViews();
        denominationInputs.clear();

        // Define common denominations in descending order
        int[] denominations = { 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500, 200, 100 };

        for (int denomination : denominations) {
            // Create a row for each denomination
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            // Create denomination label
            TextView denominationLabel = new TextView(this);
            denominationLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            denominationLabel.setText(String.format(Locale.getDefault(), "%,d", denomination));
            denominationLabel.setTextSize(16);

            // Create quantity input
            EditText quantityInput = new EditText(this);
            quantityInput.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            quantityInput.setHint("0");
            quantityInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            quantityInput.setMinWidth(150);

            // Add text change listener to update totals
            quantityInput.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    updateTotalAmount();
                }
            });

            // Store the input for calculations
            denominationInputs.put(denomination, quantityInput);

            // Add views to row
            row.addView(denominationLabel);
            row.addView(quantityInput);

            // Add row to container
            denominationsContainer.addView(row);
        }
    }

    private void createPaymentModeViews() {
        paymentModesContainer.removeAllViews();

        // For simplicity, we'll just add a title text view
        TextView titleView = new TextView(this);
        titleView.setText("Payment Modes (Coming Soon)");
        titleView.setTextSize(18);
        titleView.setPadding(0, 16, 0, 16);

        paymentModesContainer.addView(titleView);
    }

    private void updateTotalAmount() {
        double total = calculateTotalAmount();
        double difference = total - expectedAmount;

        // Update text views
        totalAmountTextView.setText(String.format(Locale.getDefault(), "Total: %.2f", total));
        differenceAmountTextView.setText(String.format(Locale.getDefault(), "Difference: %.2f", difference));

        // Set text color based on difference
        int color;
        if (difference < 0) {
            color = getResources().getColor(R.color.red);
        } else if (difference > 0) {
            color = getResources().getColor(R.color.green);
        } else {
            color = getResources().getColor(R.color.black);
        }
        differenceAmountTextView.setTextColor(color);
    }

    private double calculateTotalAmount() {
        double total = 0.0;

        // Calculate total from denominations
        for (Map.Entry<Integer, EditText> entry : denominationInputs.entrySet()) {
            int denomination = entry.getKey();
            EditText quantityInput = entry.getValue();

            try {
                String quantityText = quantityInput.getText().toString();
                if (!quantityText.isEmpty()) {
                    int quantity = Integer.parseInt(quantityText);
                    total += denomination * quantity;
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing quantity", e);
            }
        }

        return total / 100.0; // Convert to currency value
    }

    private void confirmEndSession() {
        // Get end amount and notes
        double closingAmount = calculateTotalAmount();
        String notes = notesEditText.getText().toString();

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        confirmEndSessionButton.setEnabled(false);

        try {
            // Create request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("session_id", sessionId);
            requestBody.put("user_id", userId);
            requestBody.put("closing_amount", closingAmount);
            requestBody.put("notes", notes);

            // Create Retrofit call to end session
            // For now, just simulate a successful response

            // In a real implementation, you would make an API call like:
            // Call<ResponseBody> call = apiService.endSession(RequestBody.create(
            //     MediaType.parse("application/json"), requestBody.toString()));
            // call.enqueue(...);

            // Simulate success response after a delay
            executorService.execute(() -> {
                try {
                    Thread.sleep(1500); // Simulate network delay

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        confirmEndSessionButton.setEnabled(true);

                        Toast.makeText(EndSessionActivity.this,
                                "Session ended successfully", Toast.LENGTH_SHORT).show();

                        // Return to previous screen
                        finish();
                    });
                } catch (InterruptedException e) {
                    Log.e(TAG, "Sleep interrupted", e);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        confirmEndSessionButton.setEnabled(true);
                        Toast.makeText(EndSessionActivity.this,
                                "Error: Operation interrupted", Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating request", e);
            progressBar.setVisibility(View.GONE);
            confirmEndSessionButton.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }


}