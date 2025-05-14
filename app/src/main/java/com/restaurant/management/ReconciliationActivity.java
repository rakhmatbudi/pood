package com.restaurant.management;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.models.PaymentReconciliation;
import com.restaurant.management.models.SessionSummary;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReconciliationActivity extends AppCompatActivity {

    private static final String TAG = "ReconciliationActivity";
    private static final String API_URL_BASE = "https://api.pood.lol";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    // UI Components
    private Toolbar toolbar;
    private TextView tvSessionInfo;
    private TextView tvOpeningAmount;
    private TextView tvTotalSales;
    private TextView tvTotalOrders;

    // Cash
    private TextView tvCashSystemTotal;
    private TextInputEditText etCashCount;
    private TextView tvCashDifference;

    // Card
    private TextView tvCardSystemTotal;
    private TextInputEditText etCardCount;
    private TextView tvCardDifference;

    // Mobile
    private TextView tvMobileSystemTotal;
    private TextInputEditText etMobileCount;
    private TextView tvMobileDifference;

    private TextInputEditText etNotes;
    private Button btnEndSession;
    private ProgressBar progressBar;

    // Data
    private long sessionId;
    private String cashierName;
    private SessionSummary sessionSummary;
    private PaymentReconciliation cashReconciliation;
    private PaymentReconciliation cardReconciliation;
    private PaymentReconciliation mobileReconciliation;

    // Format for currency values
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_reconciliation);

            // Set up toolbar
            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.end_session);
            }

            // Initialize views
            initViews();

            // Get session ID from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            sessionId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);
            cashierName = sharedPreferences.getString(getString(R.string.pref_user_name), "");

            if (sessionId == -1) {
                Toast.makeText(this, R.string.no_active_session_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Create default reconciliation objects
            // (prevent NPE if data loading fails but user interacts with UI)
            cashReconciliation = new PaymentReconciliation(getString(R.string.cash_label), 0.0);
            cardReconciliation = new PaymentReconciliation(getString(R.string.card_label), 0.0);
            mobileReconciliation = new PaymentReconciliation(getString(R.string.mobile_money_label), 0.0);

            // Load session data
            loadSessionData();

            // Setup input listeners
            setupInputListeners();

            // Setup end session button
            btnEndSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    validateAndEndSession();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ReconciliationActivity", e);
            Toast.makeText(this, getString(R.string.error_initializing, e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        try {
            tvSessionInfo = findViewById(R.id.tvSessionInfo);
            tvOpeningAmount = findViewById(R.id.tvOpeningAmount);
            tvTotalSales = findViewById(R.id.tvTotalSales);
            tvTotalOrders = findViewById(R.id.tvTotalOrders);

            tvCashSystemTotal = findViewById(R.id.tvCashSystemTotal);
            etCashCount = findViewById(R.id.etCashCount);
            tvCashDifference = findViewById(R.id.tvCashDifference);

            tvCardSystemTotal = findViewById(R.id.tvCardSystemTotal);
            etCardCount = findViewById(R.id.etCardCount);
            tvCardDifference = findViewById(R.id.tvCardDifference);

            tvMobileSystemTotal = findViewById(R.id.tvMobileSystemTotal);
            etMobileCount = findViewById(R.id.etMobileCount);
            tvMobileDifference = findViewById(R.id.tvMobileDifference);

            etNotes = findViewById(R.id.etNotes);
            btnEndSession = findViewById(R.id.btnEndSession);
            progressBar = findViewById(R.id.progressBar);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e; // rethrow to be caught by the onCreate try-catch
        }
    }

    private void setupInputListeners() {
        etCashCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCashDifference();
            }
        });

        etCardCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCardDifference();
            }
        });

        etMobileCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateMobileDifference();
            }
        });
    }

    private void updateCashDifference() {
        try {
            double actualAmount = 0.0;
            if (etCashCount.getText() != null && !etCashCount.getText().toString().isEmpty()) {
                actualAmount = Double.parseDouble(etCashCount.getText().toString());
            }

            if (cashReconciliation != null) {
                cashReconciliation.setActualAmount(actualAmount);
                tvCashDifference.setText(currencyFormat.format(cashReconciliation.getDifference()));

                // Set text color based on difference
                if (cashReconciliation.getDifference() < 0) {
                    tvCashDifference.setTextColor(getResources().getColor(R.color.red));
                } else if (cashReconciliation.getDifference() > 0) {
                    tvCashDifference.setTextColor(getResources().getColor(R.color.green));
                } else {
                    tvCashDifference.setTextColor(getResources().getColor(R.color.black));
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing cash amount", e);
        }
    }

    private void updateCardDifference() {
        try {
            double actualAmount = 0.0;
            if (etCardCount.getText() != null && !etCardCount.getText().toString().isEmpty()) {
                actualAmount = Double.parseDouble(etCardCount.getText().toString());
            }

            if (cardReconciliation != null) {
                cardReconciliation.setActualAmount(actualAmount);
                tvCardDifference.setText(currencyFormat.format(cardReconciliation.getDifference()));

                // Set text color based on difference
                if (cardReconciliation.getDifference() < 0) {
                    tvCardDifference.setTextColor(getResources().getColor(R.color.red));
                } else if (cardReconciliation.getDifference() > 0) {
                    tvCardDifference.setTextColor(getResources().getColor(R.color.green));
                } else {
                    tvCardDifference.setTextColor(getResources().getColor(R.color.black));
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing card amount", e);
        }
    }

    private void updateMobileDifference() {
        try {
            double actualAmount = 0.0;
            if (etMobileCount.getText() != null && !etMobileCount.getText().toString().isEmpty()) {
                actualAmount = Double.parseDouble(etMobileCount.getText().toString());
            }

            if (mobileReconciliation != null) {
                mobileReconciliation.setActualAmount(actualAmount);
                tvMobileDifference.setText(currencyFormat.format(mobileReconciliation.getDifference()));

                // Set text color based on difference
                if (mobileReconciliation.getDifference() < 0) {
                    tvMobileDifference.setTextColor(getResources().getColor(R.color.red));
                } else if (mobileReconciliation.getDifference() > 0) {
                    tvMobileDifference.setTextColor(getResources().getColor(R.color.green));
                } else {
                    tvMobileDifference.setTextColor(getResources().getColor(R.color.black));
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing mobile amount", e);
        }
    }

    private void loadSessionData() {
        progressBar.setVisibility(View.VISIBLE);

        // Construct the URL for getting session details
        String url = API_URL_BASE + "/cashier-sessions/" + sessionId;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load session data", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReconciliationActivity.this,
                                getString(R.string.failed_to_check_session, e.getMessage()),
                                Toast.LENGTH_LONG).show();
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

                    if ("success".equals(jsonObject.optString("status"))) {
                        JSONObject data = jsonObject.optJSONObject("data");
                        if (data == null) {
                            throw new IOException("Data is null in response");
                        }

                        // Use optDouble and optInt to avoid errors if fields are missing
                        final double openingAmount = data.optDouble("opening_amount", 0.0);
                        final double totalSales = data.optDouble("total_sales", 0.0);
                        final int totalOrders = data.optInt("total_orders", 0);

                        // Get payment totals
                        final double cashTotal = data.optDouble("cash_total", 0.0);
                        final double cardTotal = data.optDouble("card_total", 0.0);
                        final double mobileTotal = data.optDouble("mobile_money_total", 0.0);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Create session summary
                                sessionSummary = new SessionSummary(sessionId, cashierName, openingAmount);
                                sessionSummary.setTotalSales(totalSales);
                                sessionSummary.setTotalOrders(totalOrders);

                                // Create payment reconciliations
                                cashReconciliation = new PaymentReconciliation(getString(R.string.cash_label), cashTotal);
                                cardReconciliation = new PaymentReconciliation(getString(R.string.card_label), cardTotal);
                                mobileReconciliation = new PaymentReconciliation(getString(R.string.mobile_money_label), mobileTotal);

                                sessionSummary.addPaymentReconciliation(cashReconciliation);
                                sessionSummary.addPaymentReconciliation(cardReconciliation);
                                sessionSummary.addPaymentReconciliation(mobileReconciliation);

                                // Update UI
                                updateUI();

                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        throw new IOException(getString(R.string.error_checking_session) + ": " + jsonObject.optString("message", "Unknown error"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    final String errorMessage = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ReconciliationActivity.this,
                                    getString(R.string.error_processing_response, errorMessage),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void updateUI() {
        try {
            // Format date for display
            SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.getDefault());
            String dateString = dateFormat.format(new Date());

            // Update session info
            tvSessionInfo.setText(String.format("Session #%d - %s - %s", sessionId, cashierName, dateString));

            // Format currency values - use currency prefix if available
            String prefix = "";
            try {
                prefix = getString(R.string.currency_prefix) + " ";
            } catch (Exception e) {
                // Currency prefix not defined, use empty string
            }

            // Update summary values
            tvOpeningAmount.setText(prefix + currencyFormat.format(sessionSummary.getOpeningAmount()));
            tvTotalSales.setText(prefix + currencyFormat.format(sessionSummary.getTotalSales()));
            tvTotalOrders.setText(String.valueOf(sessionSummary.getTotalOrders()));

            // Update payment reconciliation values
            tvCashSystemTotal.setText(prefix + currencyFormat.format(cashReconciliation.getSystemAmount()));
            tvCardSystemTotal.setText(prefix + currencyFormat.format(cardReconciliation.getSystemAmount()));
            tvMobileSystemTotal.setText(prefix + currencyFormat.format(mobileReconciliation.getSystemAmount()));

            // Pre-populate the physical count fields with system amounts for easier reconciliation
            etCashCount.setText(String.valueOf(cashReconciliation.getSystemAmount()));
            etCardCount.setText(String.valueOf(cardReconciliation.getSystemAmount()));
            etMobileCount.setText(String.valueOf(mobileReconciliation.getSystemAmount()));

            // Update differences
            updateCashDifference();
            updateCardDifference();
            updateMobileDifference();
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            Toast.makeText(this, "Error updating display: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void validateAndEndSession() {
        // Check if all fields are filled
        if (etCashCount.getText() == null || etCashCount.getText().toString().isEmpty() ||
                etCardCount.getText() == null || etCardCount.getText().toString().isEmpty() ||
                etMobileCount.getText() == null || etMobileCount.getText().toString().isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_all_amounts), Toast.LENGTH_SHORT).show();
            return;
        }

        // Get notes
        if (sessionSummary != null && etNotes.getText() != null) {
            sessionSummary.setNotes(etNotes.getText().toString());
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Create JSON for API request
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("session_id", sessionId);
            requestJson.put("cash_counted", cashReconciliation.getActualAmount());
            requestJson.put("card_counted", cardReconciliation.getActualAmount());
            requestJson.put("mobile_money_counted", mobileReconciliation.getActualAmount());
            requestJson.put("notes", sessionSummary != null ? sessionSummary.getNotes() : "");

            // End session API call
            endSession(requestJson);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.error_creating_request), Toast.LENGTH_SHORT).show();
        }
    }

    private void endSession(JSONObject requestJson) {
        // Construct the URL for ending the session
        String url = API_URL_BASE + "/cashier-sessions/" + sessionId + "/end";

        // Try to create the RequestBody using reflection to handle different OkHttp versions
        RequestBody body;

        try {
            // Try newer OkHttp version method signature (MediaType first, String second)
            body = RequestBody.create(JSON, requestJson.toString());
        } catch (NoSuchMethodError e1) {
            try {
                // Try older OkHttp version method signature (String first, MediaType second)
                body = RequestBody.create(requestJson.toString(), JSON);
            } catch (Exception e2) {
                // Ultimate fallback for any other case
                Log.w(TAG, "Both RequestBody creation methods failed, using null MediaType", e2);
                body = RequestBody.create(null, requestJson.toString());
            }
        }

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to end session", e);
                final String errorMessage = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReconciliationActivity.this,
                                getString(R.string.failed_end_session, errorMessage),
                                Toast.LENGTH_LONG).show();
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

                    if ("success".equals(jsonObject.optString("status"))) {
                        // Session ended successfully
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);

                                // Clear active session ID from SharedPreferences
                                SharedPreferences sharedPreferences = getSharedPreferences(
                                        getString(R.string.pref_file_name), MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.remove(getString(R.string.pref_active_session_id));
                                editor.apply();

                                Toast.makeText(ReconciliationActivity.this,
                                        getString(R.string.session_ended_successfully),
                                        Toast.LENGTH_LONG).show();

                                // Return to Dashboard
                                finish();
                            }
                        });
                    } else {
                        throw new IOException(getString(R.string.error_end_session, jsonObject.optString("message", "Unknown error")));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    final String errorMessage = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ReconciliationActivity.this,
                                    getString(R.string.error_end_session, errorMessage),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
}