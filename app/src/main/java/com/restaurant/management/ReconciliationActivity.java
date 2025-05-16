package com.restaurant.management;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.PaymentReconciliation;
import com.restaurant.management.models.SessionSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.view.Gravity;

public class ReconciliationActivity extends AppCompatActivity {

    private static final String TAG = "ReconciliationActivity";
    private static final String API_URL_BASE = "https://api.pood.lol";
    private static final String PAYMENT_MODES_API = API_URL_BASE + "/payment-modes";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client = new OkHttpClient();

    // UI Components
    private Toolbar toolbar;
    private TextView tvSessionId;
    private TextView tvOpenedBy;
    private TextView tvOpenTime;
    private TextView tvOpeningAmount;
    private TextView tvTotalSales;
    private TextView tvTotalOrders;
    private LinearLayout paymentModesContainer;
    private TextInputEditText etNotes;
    private Button btnEndSession;
    private ProgressBar progressBar;

    // Data
    private long sessionId;
    private String cashierName;
    private SessionSummary sessionSummary;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private Map<String, View> paymentModeViews = new HashMap<>();

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
                getSupportActionBar().setTitle(R.string.title_activity_reconciliation);
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

            // Create session summary
            sessionSummary = new SessionSummary(sessionId, cashierName, 0.0);

            // Show progress while loading
            progressBar.setVisibility(View.VISIBLE);

            // First fetch payment modes, then session data
            fetchPaymentModes();

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
            // Session Summary views
            tvSessionId = findViewById(R.id.tvSessionId);
            tvOpenedBy = findViewById(R.id.tvOpenedBy);
            tvOpenTime = findViewById(R.id.tvOpenTime);
            tvOpeningAmount = findViewById(R.id.tvOpeningAmount);
            tvTotalSales = findViewById(R.id.tvTotalSales);
            tvTotalOrders = findViewById(R.id.tvTotalOrders);

            // Payment Modes container
            paymentModesContainer = findViewById(R.id.paymentModesContainer);

            // Notes and button
            etNotes = findViewById(R.id.etNotes);
            btnEndSession = findViewById(R.id.btnEndSession);
            progressBar = findViewById(R.id.progressBar);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e; // rethrow to be caught by the onCreate try-catch
        }
    }

    private void fetchPaymentModes() {
        Request request = new Request.Builder()
                .url(PAYMENT_MODES_API)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch payment modes", e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReconciliationActivity.this,
                                "Failed to fetch payment modes: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        // Continue with session data WITHOUT adding default payment methods
                        loadSessionData();
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
                        JSONArray data = jsonObject.optJSONArray("data");
                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject methodJson = data.getJSONObject(i);
                                String id = methodJson.optString("id");
                                String name = methodJson.optString("description");
                                String code = methodJson.optString("id");

                                PaymentMethod method = new PaymentMethod(id, name, code);
                                paymentMethods.add(method);
                            }
                        }
                    } else {
                        Log.w(TAG, "Failed to get payment modes: " + jsonObject.optString("message"));
                        // Do NOT call setupDefaultPaymentMethods() here
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing payment modes response", e);
                    // Do NOT call setupDefaultPaymentMethods() here
                } finally {
                    // If payment methods is empty, do NOT fill it with defaults
                    loadSessionData();
                }
            }
        });
    }

    private void setupDefaultPaymentMethods() {
        // This method is now empty - we no longer add default payment methods
        // Instead, we'll handle the case when no payment methods are available
        Log.d(TAG, "No payment methods available from API");
    }

    private void loadSessionData() {
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
                        final String openedBy = data.optString("cashier_name", cashierName);
                        final String openTime = data.optString("opened_at", "");

                        // Create a map of payment totals by code
                        final Map<String, Double> paymentTotals = new HashMap<>();

                        // Add standard payment types that might be in the API
                        paymentTotals.put("cash", data.optDouble("cash_total", 0.0));
                        paymentTotals.put("card", data.optDouble("card_total", 0.0));
                        paymentTotals.put("mobile", data.optDouble("mobile_money_total", 0.0));

                        // Try to get payment totals from API response if any exist
                        if (data.has("payment_totals")) {
                            JSONObject totals = data.getJSONObject("payment_totals");
                            Iterator<String> keys = totals.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                paymentTotals.put(key, totals.optDouble(key, 0.0));
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Update session summary
                                sessionSummary.setOpeningAmount(openingAmount);
                                sessionSummary.setTotalSales(totalSales);
                                sessionSummary.setTotalOrders(totalOrders);

                                // Update session summary UI
                                updateSessionSummaryUI(openedBy, openTime);

                                // Create payment mode UI
                                createPaymentModesUI(paymentTotals);

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

    private void updateSessionSummaryUI(String openedBy, String openTime) {
        try {
            // Format date for display
            String formattedOpenTime = openTime;
            if (openTime == null || openTime.isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                formattedOpenTime = dateFormat.format(new Date());
            }

            // Format currency values - use currency prefix if available
            String prefix = "";
            try {
                prefix = getString(R.string.currency_prefix) + " ";
            } catch (Exception e) {
                // Currency prefix not defined, use empty string
            }

            // Update session summary details
            tvSessionId.setText(String.valueOf(sessionId));
            tvOpenedBy.setText(openedBy);
            tvOpenTime.setText(formattedOpenTime);
            tvOpeningAmount.setText(prefix + currencyFormat.format(sessionSummary.getOpeningAmount()));
            tvTotalSales.setText(prefix + currencyFormat.format(sessionSummary.getTotalSales()));
            tvTotalOrders.setText(String.valueOf(sessionSummary.getTotalOrders()));

        } catch (Exception e) {
            Log.e(TAG, "Error updating Session Summary UI", e);
            Toast.makeText(this, "Error updating display: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createPaymentModesUI(Map<String, Double> paymentTotals) {
        // Clear container first
        paymentModesContainer.removeAllViews();
        paymentModeViews.clear();

        // If no payment methods, display a message and return
        if (paymentMethods.isEmpty()) {
            TextView emptyMessage = new TextView(this);
            emptyMessage.setText(getString(R.string.no_payment_methods_available));
            emptyMessage.setTextSize(16);
            emptyMessage.setPadding(16, 16, 16, 16);
            emptyMessage.setGravity(Gravity.CENTER);
            paymentModesContainer.addView(emptyMessage);
            return;
        }

        // Get LayoutInflater
        LayoutInflater inflater = LayoutInflater.from(this);

        // For each payment method, create a UI element
        for (final PaymentMethod method : paymentMethods) {
            Log.d(TAG, "Creating payment mode UI for: " + method.getName());

            // Get system amount for this payment method (or 0 if not found)
            Double systemAmount = paymentTotals.get(method.getCode());
            if (systemAmount == null) {
                systemAmount = 0.0;
            }

            // Create reconciliation object
            final PaymentReconciliation reconciliation = new PaymentReconciliation(method, systemAmount);
            sessionSummary.addPaymentReconciliation(reconciliation);

            // Inflate the payment mode item layout
            View itemView = inflater.inflate(R.layout.item_payment_mode, paymentModesContainer, false);

            // Get UI elements
            TextView tvTitle = itemView.findViewById(R.id.tvPaymentModeTitle);
            TextView tvSystemTotal = itemView.findViewById(R.id.tvPaymentModeSystemTotal);
            final TextInputEditText etPhysicalCount = itemView.findViewById(R.id.etPaymentModeCount);
            final TextView tvDifference = itemView.findViewById(R.id.tvPaymentModeDifference);

            // Set title
            tvTitle.setText(method.getName());

            // Format currency
            String prefix = "";
            try {
                prefix = getString(R.string.currency_prefix) + " ";
            } catch (Exception e) {
                // Currency prefix not defined, use empty string
            }

            // Set initial values
            tvSystemTotal.setText(prefix + currencyFormat.format(reconciliation.getSystemAmount()));
            etPhysicalCount.setText(String.valueOf(reconciliation.getSystemAmount()));  // Pre-fill with system amount
            tvDifference.setText(prefix + currencyFormat.format(0.00));  // Initially difference is 0

            // Setup TextWatcher for physical count
            etPhysicalCount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        double actualAmount = 0.0;
                        if (s != null && !s.toString().isEmpty()) {
                            actualAmount = Double.parseDouble(s.toString());
                        }

                        // Update reconciliation object
                        reconciliation.setActualAmount(actualAmount);

                        // Format with currency prefix
                        String prefix = "";
                        try {
                            prefix = getString(R.string.currency_prefix) + " ";
                        } catch (Exception e) {
                            // Ignore if not found
                        }

                        // Update difference display
                        tvDifference.setText(prefix + currencyFormat.format(reconciliation.getDifference()));

                        // Set text color based on difference
                        if (reconciliation.getDifference() < 0) {
                            tvDifference.setTextColor(getResources().getColor(R.color.red));
                        } else if (reconciliation.getDifference() > 0) {
                            tvDifference.setTextColor(getResources().getColor(R.color.green));
                        } else {
                            tvDifference.setTextColor(getResources().getColor(R.color.black));
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing amount for " + reconciliation.getName(), e);
                    }
                }
            });

            // Store view for later reference
            paymentModeViews.put(method.getCode(), itemView);

            // Add view to container
            paymentModesContainer.addView(itemView);
        }
    }

    private void validateAndEndSession() {
        boolean allFieldsFilled = true;

        // Check if all payment reconciliations have physical counts
        for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
            View itemView = paymentModeViews.get(reconciliation.getCode());
            if (itemView != null) {
                TextInputEditText etPhysicalCount = itemView.findViewById(R.id.etPaymentModeCount);
                if (etPhysicalCount.getText() == null || etPhysicalCount.getText().toString().isEmpty()) {
                    allFieldsFilled = false;
                    break;
                }
            }
        }

        if (!allFieldsFilled) {
            Toast.makeText(this, getString(R.string.enter_all_amounts), Toast.LENGTH_SHORT).show();
            return;
        }

        // Get notes
        if (etNotes.getText() != null) {
            sessionSummary.setNotes(etNotes.getText().toString());
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Create JSON for API request
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("session_id", sessionId);

            // Add all payment reconciliations
            for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
                requestJson.put(reconciliation.getCode() + "_counted", reconciliation.getActualAmount());
            }

            // Add notes
            requestJson.put("notes", sessionSummary.getNotes());

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
            // Try newer OkHttp version method signature
            body = RequestBody.create(JSON, requestJson.toString());
        } catch (NoSuchMethodError e1) {
            try {
                // Try older OkHttp version method signature
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