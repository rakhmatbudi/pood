package com.restaurant.management;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.chuckerteam.chucker.api.Chucker;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.PaymentReconciliation;
import com.restaurant.management.models.SessionSummary;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;
import com.restaurant.management.models.ApiResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.view.Gravity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;


public class ReconciliationActivity extends AppCompatActivity {

    private static final String TAG = "ReconciliationActivity";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private ApiService apiService;


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

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 600;



    // Data
    private long sessionId;
    private String cashierName;
    private SessionSummary sessionSummary;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private Map<String, View> paymentModeViews = new HashMap<>();

    // Format for currency values
    private DecimalFormat currencyFormat = new DecimalFormat("#,##0.00");

    private int pendingPaymentMethodRequests = 0;
    private double totalSalesAmount = 0.0;
    private Set<String> uniqueOrderIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_reconciliation);

            // Initialize sensor manager for shake detection
            //sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            //if (sensorManager != null) {
            //    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //}
            initializeShakeDetection();

            apiService = ApiClient.getApiService(this);

            toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(R.string.title_activity_reconciliation);
            }

            initViews();

            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            sessionId = sharedPreferences.getLong(getString(R.string.pref_active_session_id), -1);
            cashierName = sharedPreferences.getString(getString(R.string.pref_user_name), "");
            String authToken = sharedPreferences.getString(getString(R.string.pref_token), "");


            if (sessionId == -1) {
                Toast.makeText(this, R.string.no_active_session_found, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (authToken.isEmpty()) {
                Toast.makeText(this, R.string.session_expired_login_again, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            sessionSummary = new SessionSummary(sessionId, cashierName, 0.0);

            progressBar.setVisibility(View.VISIBLE);

            fetchPaymentModes();

            btnEndSession.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkForActiveOrders();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ReconciliationActivity", e);
            Toast.makeText(this, getString(R.string.error_initializing, e.getMessage()), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeShakeDetection() {
        try {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            if (sensorManager != null) {
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer != null) {
                    sensorManager.registerListener(shakeListener, accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize shake detection", e);
        }
    }

    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    try {
                        // Use ReconciliationActivity.this instead of just this
                        startActivity(Chucker.getLaunchIntent(ReconciliationActivity.this));
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to launch Chucker", e);
                    }
                }

                last_x = x;
                last_y = y;
                last_z = z;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Register the sensor listener when activity resumes
        if (sensorManager != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listener when activity pauses
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
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
            tvSessionId = findViewById(R.id.tvSessionId);
            tvOpenedBy = findViewById(R.id.tvOpenedBy);
            tvOpenTime = findViewById(R.id.tvOpenTime);
            tvOpeningAmount = findViewById(R.id.tvOpeningAmount);
            tvTotalSales = findViewById(R.id.tvTotalSales);
            tvTotalOrders = findViewById(R.id.tvTotalOrders);

            paymentModesContainer = findViewById(R.id.paymentModesContainer);

            etNotes = findViewById(R.id.etNotes);
            btnEndSession = findViewById(R.id.btnEndSession);
            progressBar = findViewById(R.id.progressBar);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void updateSystemTotal(String paymentModeId, double systemAmount) {
        View itemView = paymentModeViews.get(paymentModeId);
        if (itemView == null) {
            Log.w(TAG, "View for payment mode " + paymentModeId + " not found");
            return;
        }

        TextView tvSystemTotal = itemView.findViewById(R.id.tvPaymentModeSystemTotal);
        if (tvSystemTotal == null) {
            Log.w(TAG, "System total view not found for payment mode " + paymentModeId);
            return;
        }

        String prefix = "";
        try {
            prefix = getString(R.string.currency_prefix) + " ";
        } catch (Exception e) {
            // Currency prefix not defined, use empty string
        }

        tvSystemTotal.setText(prefix + currencyFormat.format(systemAmount));
    }

    private void fetchPaymentModes() {
        apiService.getPaymentModes().enqueue(new Callback<ApiResponse<List<PaymentMethod>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<PaymentMethod>>> call, Response<ApiResponse<List<PaymentMethod>>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        List<PaymentMethod> fetchedMethods = response.body().getData();
                        if (fetchedMethods != null) {
                            paymentMethods.clear();
                            paymentMethods.addAll(fetchedMethods);
                            Log.d(TAG, "Successfully fetched " + paymentMethods.size() + " payment modes.");
                        } else {
                            Log.w(TAG, "Payment modes data is null in API response.");
                        }
                    } else {
                        String errorMessage = response.message();
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyString = response.errorBody().string();
                                JSONObject errorJson = new JSONObject(errorBodyString);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body for payment modes: " + e.getMessage());
                            }
                        }
                        final String finalErrorMessage = errorMessage; // Make it effectively final
                        Log.e(TAG, "Failed to fetch payment modes: " + response.code() + " - " + finalErrorMessage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReconciliationActivity.this,
                                        "Failed to fetch payment modes: " + finalErrorMessage,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing payment modes response", e);
                    final String finalErrorMessage = e.getMessage(); // Make it effectively final
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReconciliationActivity.this,
                                    "Error processing payment modes: " + finalErrorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    loadSessionData();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<PaymentMethod>>> call, Throwable t) {
                Log.e(TAG, "Network error fetching payment modes", t);
                final String finalErrorMessage = t.getMessage(); // Make it effectively final
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReconciliationActivity.this,
                                "Network error fetching payment modes: " + finalErrorMessage,
                                Toast.LENGTH_LONG).show();
                        loadSessionData();
                    }
                });
            }
        });
    }

    private void fetchPaymentMethodTransactions(long sessionId, String paymentModeId) {
        apiService.getPaymentMethodTransactions(sessionId, paymentModeId).enqueue(new Callback<ApiResponse<List<JsonElement>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<JsonElement>>> call, Response<ApiResponse<List<JsonElement>>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                        List<JsonElement> data = response.body().getData();
                        Log.d(TAG, "Processing transactions for paymentModeId: " + paymentModeId); // Log the mode ID being processed

                        if (data != null) {
                            double totalAmount = 0.0;
                            Set<String> orderIds = new HashSet<>();

                            Log.d(TAG, "Data list size for " + paymentModeId + ": " + data.size()); // Check if data is empty

                            for (JsonElement element : data) {
                                if (element.isJsonObject()) {
                                    JsonObject transaction = element.getAsJsonObject();
                                    if (transaction.has("amount")) {
                                        JsonElement amountElement = transaction.get("amount");
                                        if (amountElement != null && !amountElement.isJsonNull() && amountElement.isJsonPrimitive()) {
                                            try {
                                                double currentAmount = Double.parseDouble(amountElement.getAsString()); // Get as string then parse
                                                totalAmount += currentAmount;
                                                Log.d(TAG, "  Adding amount " + currentAmount + " to total for " + paymentModeId + ". Current total: " + totalAmount);
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Invalid 'amount' format in transaction for mode " + paymentModeId + ": " + amountElement.toString(), e);
                                            }
                                        } else {
                                            Log.w(TAG, "'amount' field is null, not primitive, or invalid type in transaction for mode " + paymentModeId + ": " + (amountElement != null ? amountElement.toString() : "null"));
                                        }
                                    } else {
                                        Log.w(TAG, "'amount' field missing in transaction for mode " + paymentModeId);
                                    }

                                    if (transaction.has("order_id") && !transaction.get("order_id").isJsonNull() && transaction.get("order_id").isJsonPrimitive()) {
                                        String orderId = transaction.get("order_id").getAsString();
                                        if (!orderId.isEmpty()) {
                                            orderIds.add(orderId);
                                        }
                                    }
                                }
                            }

                            final double finalTotalAmount = totalAmount;
                            Log.d(TAG, "Final calculated total for " + paymentModeId + ": " + finalTotalAmount); // Log the final total

                            final Set<String> finalOrderIds = orderIds;

                            Log.d(TAG, "Attempting to match reconciliation for " + paymentModeId + " (fetched ID)");
                            Log.d(TAG, "Total PaymentReconciliations in sessionSummary: " + sessionSummary.getPaymentReconciliations().size());

                            boolean matchFound = false;

                            for (final PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
                                Log.d(TAG, "  Comparing reconciliation code '" + reconciliation.getId() + "' with fetched paymentModeId '" + paymentModeId + "'");

                                if (reconciliation.getId().equals(paymentModeId)) {
                                    Log.d(TAG, "  MATCH FOUND for reconciliation code: " + reconciliation.getId());

                                    reconciliation.setSystemAmount(finalTotalAmount);
                                    matchFound = true;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateSystemTotal(paymentModeId, finalTotalAmount);
                                            View itemView = paymentModeViews.get(paymentModeId);
                                            if (itemView != null) {
                                                TextInputEditText etPhysicalCount = itemView.findViewById(R.id.etPaymentModeCount);
                                                if (etPhysicalCount != null) {
                                                    etPhysicalCount.setText(String.valueOf(finalTotalAmount));
                                                    etPhysicalCount.getEditableText().clear();
                                                    etPhysicalCount.append(String.valueOf(finalTotalAmount));
                                                }
                                            }
                                            processCompletedPaymentRequest(finalTotalAmount, finalOrderIds);
                                        }
                                    });
                                    break;
                                }
                            }
                            if (!matchFound) {
                                Log.e(TAG, "ERROR: No matching PaymentReconciliation found for paymentModeId: " + paymentModeId + ". System total will remain 0.");
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processCompletedPaymentRequest(0.0, new HashSet<String>());
                                }
                            });
                        }
                    } else {
                        String errorMessage = response.message();
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyString = response.errorBody().string();
                                JSONObject errorJson = new JSONObject(errorBodyString);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body for transactions: " + e.getMessage());
                            }
                        }
                        final String finalErrorMessage = errorMessage; // Make it effectively final
                        Log.w(TAG, "Failed to get transactions for mode " + paymentModeId + ": " + response.code() + " - " + finalErrorMessage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReconciliationActivity.this,
                                        "Error fetching transactions for " + paymentModeId + ": " + finalErrorMessage,
                                        Toast.LENGTH_SHORT).show();
                                processCompletedPaymentRequest(0.0, new HashSet<String>());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing transaction data for payment mode " + paymentModeId, e);
                    final String finalErrorMessage = e.getMessage(); // Make it effectively final
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReconciliationActivity.this,
                                    "Error processing transactions for " + paymentModeId + ": " + finalErrorMessage,
                                    Toast.LENGTH_SHORT).show();
                            processCompletedPaymentRequest(0.0, new HashSet<String>());
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<JsonElement>>> call, Throwable t) {
                Log.e(TAG, "Network error fetching transactions for payment mode " + paymentModeId, t);
                final String finalErrorMessage = t.getMessage(); // Make it effectively final
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReconciliationActivity.this,
                                "Network error fetching transactions: " + finalErrorMessage,
                                Toast.LENGTH_SHORT).show();
                        processCompletedPaymentRequest(0.0, new HashSet<String>());
                    }
                });
            }
        });
    }

    private synchronized void processCompletedPaymentRequest(double amount, Set<String> orderIds) {
        totalSalesAmount += amount;
        uniqueOrderIds.addAll(orderIds);
        pendingPaymentMethodRequests--;

        if (pendingPaymentMethodRequests <= 0) {
            updateTotalSalesAndOrders(totalSalesAmount, uniqueOrderIds.size());
            progressBar.setVisibility(View.GONE);
        }
    }

    private void updateTotalSalesAndOrders(double totalAmount, int orderCount) {
        sessionSummary.setTotalSales(totalAmount);
        sessionSummary.setTotalOrders(orderCount);

        String prefix = "";
        try {
            prefix = getString(R.string.currency_prefix) + " ";
        } catch (Exception e) {
            // Currency prefix not defined, use empty string
        }

        tvTotalSales.setText(prefix + currencyFormat.format(totalAmount));
        tvTotalOrders.setText(String.valueOf(orderCount));

        Log.i(TAG, "Total sales updated to: " + totalAmount);
        Log.i(TAG, "Total orders updated to: " + orderCount);
    }

    private void updateTotalSales(double totalAmount) {
        sessionSummary.setTotalSales(totalAmount);

        String prefix = "";
        try {
            prefix = getString(R.string.currency_prefix) + " ";
        } catch (Exception e) {
            // Currency prefix not defined, use empty string
        }

        tvTotalSales.setText(prefix + currencyFormat.format(totalAmount));

        Log.i(TAG, "Total sales updated to: " + totalAmount);
    }

    private void loadSessionData() {
        apiService.getSessionDetails(sessionId).enqueue(new Callback<ApiResponse<SessionSummary>>() {
            @Override
            public void onResponse(Call<ApiResponse<SessionSummary>> call, Response<ApiResponse<SessionSummary>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {

                        SessionSummary fetchedSummary = response.body().getData();
                        if (fetchedSummary == null) {
                            throw new IOException("Session summary data is null in response");
                        }

                        sessionSummary.setOpeningAmount(fetchedSummary.getOpeningAmount());
                        sessionSummary.setTotalSales(fetchedSummary.getTotalSales());
                        sessionSummary.setTotalOrders(fetchedSummary.getTotalOrders());

                        final String openedBy = fetchedSummary.getCashierName() != null ? fetchedSummary.getCashierName() : cashierName;
                        final String openTime = fetchedSummary.getOpenedAt() != null ? fetchedSummary.getOpenedAt() : "";

                        final Map<String, Double> paymentTotals = new HashMap<>();
                        if (fetchedSummary.getPaymentTotals() != null) {
                            paymentTotals.putAll(fetchedSummary.getPaymentTotals());
                        } else {
                            paymentTotals.put("cash", fetchedSummary.getCashTotal());
                            paymentTotals.put("card", fetchedSummary.getCardTotal());
                            paymentTotals.put("mobile", fetchedSummary.getMobileMoneyTotal());
                            Log.w(TAG, "No 'payment_totals' map found in SessionSummary, attempting to use individual totals.");
                        }


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateSessionSummaryUI(openedBy, openTime);
                                createPaymentModesUI(paymentTotals);
                            }
                        });
                    } else {
                        String errorMessage = response.message();
                        if (response.errorBody() != null) {
                            try {
                                String errorBodyString = response.errorBody().string();
                                JSONObject errorJson = new JSONObject(errorBodyString);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error body for session data: " + e.getMessage());
                            }
                        }
                        final String finalErrorMessage = errorMessage; // Make it effectively final
                        Log.e(TAG, "Error loading session data: " + response.code() + " - " + finalErrorMessage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(ReconciliationActivity.this,
                                        getString(R.string.error_processing_response, finalErrorMessage),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing session data response", e);
                    final String finalErrorMessage = e.getMessage(); // Make it effectively final
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ReconciliationActivity.this,
                                    getString(R.string.error_processing_response, finalErrorMessage),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SessionSummary>> call, Throwable t) {
                Log.e(TAG, "Failed to load session data", t);
                final String finalErrorMessage = t.getMessage(); // Make it effectively final
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ReconciliationActivity.this,
                                getString(R.string.failed_to_check_session, finalErrorMessage),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void updateSessionSummaryUI(String openedBy, String openTime) {
        // ... (existing method, no changes in logic, only updated to use passed parameters)
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
        // Clear container first (existing code, no changes)
        paymentModesContainer.removeAllViews();
        paymentModeViews.clear();

        // Reset total sales tracking (existing code, no changes)
        totalSalesAmount = 0.0;
        uniqueOrderIds.clear(); // Clear the set of order IDs

        // If no payment methods, display a message and return (existing code, no changes)
        if (paymentMethods.isEmpty()) {
            TextView emptyMessage = new TextView(this);
            emptyMessage.setText(getString(R.string.no_payment_methods_available));
            emptyMessage.setTextSize(16);
            emptyMessage.setPadding(16, 16, 16, 16);
            emptyMessage.setGravity(Gravity.CENTER);
            paymentModesContainer.addView(emptyMessage);
            progressBar.setVisibility(View.GONE); // Hide progress if no payment methods
            return;
        }

        // Set the number of pending requests to the number of payment methods (existing code, no changes)
        pendingPaymentMethodRequests = paymentMethods.size();

        // Get LayoutInflater (existing code, no changes)
        LayoutInflater inflater = LayoutInflater.from(this);

        // For each payment method, create a UI element (existing code, no changes)
        for (final PaymentMethod method : paymentMethods) {
            Log.d(TAG, "Creating payment mode UI for: " + method.getName());

            // Get system amount for this payment method (or 0 if not found)
            Double systemAmount = paymentTotals.get(method.getId()); // MODIFIED: Uses passed paymentTotals map
            if (systemAmount == null) {
                systemAmount = 0.0;
            }

            // Create reconciliation object (existing code, no changes)
            final PaymentReconciliation reconciliation = new PaymentReconciliation(method, systemAmount);
            sessionSummary.addPaymentReconciliation(reconciliation);

            // Inflate the payment mode item layout (existing code, no changes)
            View itemView = inflater.inflate(R.layout.item_payment_mode, paymentModesContainer, false);

            // Get UI elements (existing code, no changes)
            TextView tvTitle = itemView.findViewById(R.id.tvPaymentModeTitle);
            TextView tvSystemTotal = itemView.findViewById(R.id.tvPaymentModeSystemTotal);
            final TextInputEditText etPhysicalCount = itemView.findViewById(R.id.etPaymentModeCount);
            final TextView tvDifference = itemView.findViewById(R.id.tvPaymentModeDifference);

            // Set title (existing code, no changes)
            tvTitle.setText(method.getName());

            // Format currency (existing code, no changes)
            String prefix = "";
            try {
                prefix = getString(R.string.currency_prefix) + " ";
            } catch (Exception e) {
                // Currency prefix not defined, use empty string
            }

            // Set initial values (existing code, no changes)
            tvSystemTotal.setText(prefix + currencyFormat.format(reconciliation.getSystemAmount()));
            etPhysicalCount.setText(String.valueOf(reconciliation.getSystemAmount()));  // Pre-fill with system amount
            tvDifference.setText(prefix + currencyFormat.format(0.00));  // Initially difference is 0

            // Setup TextWatcher for physical count (existing code, no changes)
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

            // Store view for later reference (existing code, no changes)
            paymentModeViews.put(method.getId(), itemView);

            // Add view to container (existing code, no changes)
            paymentModesContainer.addView(itemView);

            // Fetch transaction details for this payment method (existing code, no changes)
            fetchPaymentMethodTransactions(sessionId, method.getId());
        }
    }

    private void validateAndEndSession() {
        // ... (existing method, only the endSession API call section modified)
        boolean allFieldsFilled = true;

        // Check if all payment reconciliations have physical counts (existing code, no changes)
        for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
            View itemView = paymentModeViews.get(reconciliation.getId());
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

        // Get notes (existing code, no changes)
        String notes = "";
        if (etNotes.getText() != null) {
            notes = etNotes.getText().toString();
        }

        // Show progress (existing code, no changes)
        progressBar.setVisibility(View.VISIBLE);

        // Create payload for API request (existing code, no changes)
        try {
            // Create the main JSON object
            JSONObject requestJson = new JSONObject();

            // Calculate total amounts
            double totalClosingAmount = 0.0;
            double totalExpectedAmount = 0.0;

            // Create payment_mode_amounts and expected_payment_mode_amounts objects
            JSONObject paymentModeAmounts = new JSONObject();
            JSONObject expectedPaymentModeAmounts = new JSONObject();

            // Add all payment reconciliations
            for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
                // Get the actual (counted) amount
                double actualAmount = reconciliation.getActualAmount();
                // Get the system amount
                double systemAmount = reconciliation.getSystemAmount();

                // Add to payment_mode_amounts
                paymentModeAmounts.put(reconciliation.getName(), actualAmount); // NOTE: Your API expects `code` or `id`, but here you use `name`. Please ensure this matches your backend.
                // Assuming `reconciliation.getId()` should be used if API expects code, not name.
                // paymentModeAmounts.put(reconciliation.getId(), actualAmount);

                // Add to expected_payment_mode_amounts
                expectedPaymentModeAmounts.put(reconciliation.getName(), systemAmount); // Same note as above
                // expectedPaymentModeAmounts.put(reconciliation.getId(), systemAmount);


                // Add to totals
                totalClosingAmount += actualAmount;
                totalExpectedAmount += systemAmount;
            }

            // Add totals to request
            requestJson.put("closing_amount", totalClosingAmount);
            requestJson.put("expected_amount", totalExpectedAmount);

            // Add payment mode JSONObjects
            requestJson.put("payment_mode_amounts", paymentModeAmounts);
            requestJson.put("expected_payment_mode_amounts", expectedPaymentModeAmounts);

            // Add notes
            requestJson.put("notes", notes);

            // Log the request for debugging (existing code, no changes)
            Log.d(TAG, "Created request payload: " + requestJson.toString());

            // MODIFIED: Call new proceedWithEndSession to use Retrofit ApiService
            proceedWithEndSession(requestJson);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.error_creating_request), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForActiveOrders() {
        // Show confirmation dialog asking if all orders are closed (existing code, no changes)
        new AlertDialog.Builder(ReconciliationActivity.this)
                .setTitle(R.string.confirm_session_closing) // Using string resource
                .setMessage(R.string.confirm_session_closing_message) // Using string resource
                .setPositiveButton(R.string.yes_close_session, new DialogInterface.OnClickListener() { // Using string resource
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        proceedWithSessionClosing();
                    }
                })
                .setNegativeButton(R.string.cancel, null) // Using string resource
                .show();
    }

    private void proceedWithSessionClosing() {
        // Original validateAndEndSession logic, now integrated here after confirmation
        boolean allFieldsFilled = true;

        // Check if all payment reconciliations have physical counts (existing code, no changes)
        for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
            View itemView = paymentModeViews.get(reconciliation.getId());
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

        // Get notes (existing code, no changes)
        String notes = "";
        if (etNotes.getText() != null) {
            notes = etNotes.getText().toString();
        }

        // Show progress (existing code, no changes)
        progressBar.setVisibility(View.VISIBLE);

        // Create payload for API request (existing code, no changes)
        try {
            // Create the main JSON object
            JSONObject requestJson = new JSONObject();

            // Calculate total amounts
            double totalClosingAmount = 0.0;
            double totalExpectedAmount = 0.0;

            // Create payment_mode_amounts and expected_payment_mode_amounts objects
            JSONObject paymentModeAmounts = new JSONObject();
            JSONObject expectedPaymentModeAmounts = new JSONObject();

            // Add all payment reconciliations
            for (PaymentReconciliation reconciliation : sessionSummary.getPaymentReconciliations()) {
                // Get the actual (counted) amount
                double actualAmount = reconciliation.getActualAmount();
                // Get the system amount
                double systemAmount = reconciliation.getSystemAmount();

                // Add to payment_mode_amounts
                // Consider using reconciliation.getId() here if your backend expects the payment mode code/ID
                paymentModeAmounts.put(reconciliation.getName(), actualAmount);

                // Add to expected_payment_mode_amounts
                // Consider using reconciliation.getId() here if your backend expects the payment mode code/ID
                expectedPaymentModeAmounts.put(reconciliation.getName(), systemAmount);

                // Add to totals
                totalClosingAmount += actualAmount;
                totalExpectedAmount += systemAmount;
            }

            // Add totals to request
            requestJson.put("closing_amount", totalClosingAmount);
            requestJson.put("expected_amount", totalExpectedAmount);

            // Add payment mode JSONObjects
            requestJson.put("payment_mode_amounts", paymentModeAmounts);
            requestJson.put("expected_payment_mode_amounts", expectedPaymentModeAmounts);

            // Add notes
            requestJson.put("notes", notes);

            // NEW: Call the Retrofit-based end session method
            proceedWithEndSession(requestJson);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request", e);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, getString(R.string.error_creating_request), Toast.LENGTH_SHORT).show();
        }
    }

    // NEW: Renamed and adjusted to fit Retrofit call
    private void proceedWithEndSession(JSONObject requestJson) {
        // Original logic from endSession, adapted for Retrofit
        // Use the correct endpoint with PUT method
        // String url = API_URL_BASE + "/cashier-sessions/" + sessionId + "/close"; // Base URL is now handled by ApiClient


        // Create the RequestBody (existing code, no changes)
        RequestBody body = RequestBody.create(JSON, requestJson.toString());

        // REPLACED: Old OkHttpClient request
        // Request request = new Request.Builder().url(url).put(body).build();
        // client.newCall(request).enqueue(new Callback() { ... });

        // NEW: Use apiService for closing the session
        apiService.closeSession(sessionId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        String responseBody = "No response body"; // Default in case of no body
                        try {
                            if (response.errorBody() != null) {
                                responseBody = response.errorBody().string();
                            }

                            if (response.isSuccessful() && response.body() != null && "success".equals(response.body().getStatus())) {
                                // Session ended successfully
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
                            } else {
                                // API returned an error or non-successful status
                                String errorMessage = response.message();
                                if (response.errorBody() != null) {
                                    try {
                                        JSONObject errorJson = new JSONObject(responseBody);
                                        errorMessage = errorJson.optString("message", errorMessage);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error parsing error body for end session: " + e.getMessage());
                                    }
                                }
                                final String finalErrorMessage = errorMessage; // Make it effectively final
                                final String finalResponseBody = responseBody; // Make it effectively final
                                Toast.makeText(ReconciliationActivity.this,
                                        "API Error: " + finalErrorMessage,
                                        Toast.LENGTH_LONG).show();

                                // Create an alert dialog with more details
                                new AlertDialog.Builder(ReconciliationActivity.this)
                                        .setTitle("API Error")
                                        .setMessage("Status code: " + response.code() + "\n\nResponse: " + finalResponseBody)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        } catch (IOException e) { // MODIFIED: Removed JSONException from here
                            final String errorMessage = e.getMessage(); // This 'errorMessage' is a new declaration within this catch
                            final String finalResponseBody = responseBody; // Make it effectively final

                            Toast.makeText(ReconciliationActivity.this,
                                    "Processing error: " + errorMessage,
                                    Toast.LENGTH_LONG).show();

                            // Create an alert dialog with more details
                            new AlertDialog.Builder(ReconciliationActivity.this)
                                    .setTitle("Processing Error")
                                    .setMessage("Error: " + errorMessage + "\n\nResponse: " + finalResponseBody)
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        final String finalErrorMessage = t.getMessage(); // Make it effectively final
                        Toast.makeText(ReconciliationActivity.this,
                                getString(R.string.failed_end_session, finalErrorMessage),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}