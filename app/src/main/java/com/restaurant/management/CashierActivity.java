package com.restaurant.management;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CashierActivity extends AppCompatActivity {

    private static final String TAG = "CashierActivity";
    private static final String API_URL_CHECK_SESSION = "https://api.pood.lol/cashier-sessions/current";
    private static final String API_URL_TRANSACTION = "https://api.pood.lol/cashier-sessions/%d/transaction";

    private TextView sessionStatusTextView;
    private TextView currentBalanceTextView;
    private LinearLayout cashierOperationsLayout;
    private Button openCloseSessionButton;
    private Button withdrawMoneyButton;
    private Button addMoneyButton;
    private ProgressBar loadingProgressBar;

    private OkHttpClient client = new OkHttpClient();
    private boolean hasActiveSession = false;
    private long activeSessionId = -1;
    private double currentBalance = 0.0;
    private DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier);

        initializeViews();
        setupToolbar();
        setupClickListeners();
        checkCashierSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkCashierSession();
    }

    private void initializeViews() {
        sessionStatusTextView = findViewById(R.id.session_status_text_view);
        currentBalanceTextView = findViewById(R.id.current_balance_text_view);
        cashierOperationsLayout = findViewById(R.id.cashier_operations_layout);
        openCloseSessionButton = findViewById(R.id.open_close_session_button);
        withdrawMoneyButton = findViewById(R.id.withdraw_money_button);
        addMoneyButton = findViewById(R.id.add_money_button);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);

        // Initially hide operations until we know session status
        cashierOperationsLayout.setVisibility(View.GONE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cashier Management");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        openCloseSessionButton.setOnClickListener(v -> handleOpenCloseSession());
        withdrawMoneyButton.setOnClickListener(v -> showWithdrawDialog());
        addMoneyButton.setOnClickListener(v -> showAddMoneyDialog());
    }

    private void checkCashierSession() {
        loadingProgressBar.setVisibility(View.VISIBLE);
        sessionStatusTextView.setText("Checking session status...");

        Request request = new Request.Builder()
                .url(API_URL_CHECK_SESSION)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to check cashier session", e);
                runOnUiThread(() -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    sessionStatusTextView.setText("Error checking session status");
                    updateUIForSessionStatus(false);
                    Toast.makeText(CashierActivity.this,
                            "Failed to check session: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Session check response: " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);
                    final boolean sessionActive = "success".equals(jsonObject.getString("status")) &&
                            jsonObject.has("data") && !jsonObject.isNull("data");

                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);

                        if (sessionActive) {
                            try {
                                JSONObject sessionData = jsonObject.getJSONObject("data");
                                String cashierName = sessionData.getString("cashier_name");
                                activeSessionId = sessionData.getLong("session_id");

                                // Get current balance if available
                                if (sessionData.has("current_balance")) {
                                    currentBalance = sessionData.getDouble("current_balance");
                                } else if (sessionData.has("opening_amount")) {
                                    currentBalance = sessionData.getDouble("opening_amount");
                                }

                                sessionStatusTextView.setText("Active Session - Cashier: " + cashierName);
                                currentBalanceTextView.setText("Current Balance: " + currencyFormat.format(currentBalance));
                                currentBalanceTextView.setVisibility(View.VISIBLE);

                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing session data", e);
                                sessionStatusTextView.setText("Active Session - Details unavailable");
                                currentBalanceTextView.setVisibility(View.GONE);
                            }
                        } else {
                            sessionStatusTextView.setText("No Active Session");
                            currentBalanceTextView.setVisibility(View.GONE);
                        }

                        updateUIForSessionStatus(sessionActive);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing session response", e);
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        sessionStatusTextView.setText("Error checking session status");
                        updateUIForSessionStatus(false);
                    });
                }
            }
        });
    }

    private void updateUIForSessionStatus(boolean sessionActive) {
        hasActiveSession = sessionActive;

        if (sessionActive) {
            openCloseSessionButton.setText("Close Session");
            cashierOperationsLayout.setVisibility(View.VISIBLE);
            withdrawMoneyButton.setVisibility(View.VISIBLE);
            addMoneyButton.setVisibility(View.VISIBLE);
        } else {
            openCloseSessionButton.setText("Open Session");
            cashierOperationsLayout.setVisibility(View.GONE);
            withdrawMoneyButton.setVisibility(View.GONE);
            addMoneyButton.setVisibility(View.GONE);
        }
    }

    private void handleOpenCloseSession() {
        if (hasActiveSession) {
            // Navigate to reconciliation activity to close session
            Intent intent = new Intent(this, ReconciliationActivity.class);
            startActivity(intent);
        } else {
            // Navigate to open session activity
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE);
            int userId = sharedPreferences.getInt(getString(R.string.pref_user_id), -1);

            Intent intent = new Intent(this, OpenSessionActivity.class);
            intent.putExtra(getString(R.string.extra_user_id), userId);
            startActivity(intent);
        }
    }

    private void showWithdrawDialog() {
        if (!hasActiveSession) {
            Toast.makeText(this, "No active session to withdraw from", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_money_transaction, null);

        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        EditText reasonEditText = dialogView.findViewById(R.id.reason_edit_text);
        TextView titleTextView = dialogView.findViewById(R.id.dialog_title);

        titleTextView.setText("Withdraw Money");
        reasonEditText.setHint("Reason for withdrawal (required)");

        builder.setView(dialogView)
                .setTitle("Withdraw from Cashier")
                .setPositiveButton("Withdraw", null) // Set to null initially
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString().trim();
            String reason = reasonEditText.getText().toString().trim();

            if (amountStr.isEmpty()) {
                amountEditText.setError("Amount is required");
                return;
            }

            if (reason.isEmpty()) {
                reasonEditText.setError("Reason is required");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountEditText.setError("Amount must be greater than 0");
                    return;
                }

                if (amount > currentBalance) {
                    amountEditText.setError("Amount exceeds current balance");
                    return;
                }

                performWithdrawal(amount, reason);
                dialog.dismiss();

            } catch (NumberFormatException e) {
                amountEditText.setError("Invalid amount format");
            }
        });
    }

    private void showAddMoneyDialog() {
        if (!hasActiveSession) {
            Toast.makeText(this, "No active session to add money to", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_money_transaction, null);

        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        EditText reasonEditText = dialogView.findViewById(R.id.reason_edit_text);
        TextView titleTextView = dialogView.findViewById(R.id.dialog_title);

        titleTextView.setText("Add Money");
        reasonEditText.setHint("Reason for adding money (required)");

        builder.setView(dialogView)
                .setTitle("Add Money to Cashier")
                .setPositiveButton("Add", null) // Set to null initially
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the positive button to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString().trim();
            String reason = reasonEditText.getText().toString().trim();

            if (amountStr.isEmpty()) {
                amountEditText.setError("Amount is required");
                return;
            }

            if (reason.isEmpty()) {
                reasonEditText.setError("Reason is required");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountEditText.setError("Amount must be greater than 0");
                    return;
                }

                performAddMoney(amount, reason);
                dialog.dismiss();

            } catch (NumberFormatException e) {
                amountEditText.setError("Invalid amount format");
            }
        });
    }

    private void performWithdrawal(double amount, String reason) {
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("type", "withdrawal");
            requestJson.put("amount", (int)(amount * 100)); // Convert to cents/smallest currency unit
            requestJson.put("description", reason);

            String apiUrl = String.format(API_URL_TRANSACTION, activeSessionId);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Withdrawal request URL: " + apiUrl);
            Log.d(TAG, "Withdrawal request body: " + requestJson.toString());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Withdrawal request failed", e);
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(CashierActivity.this,
                                "Withdrawal failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Withdrawal response: " + responseBody);

                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    String message = jsonResponse.optString("message", "Transaction completed successfully");
                                    Toast.makeText(CashierActivity.this,
                                            message,
                                            Toast.LENGTH_SHORT).show();

                                    // Update local balance and refresh session info
                                    currentBalance -= amount;
                                    currentBalanceTextView.setText("Current Balance: " + currencyFormat.format(currentBalance));
                                    checkCashierSession(); // Refresh to get updated balance from server
                                } else {
                                    String message = jsonResponse.optString("message", "Withdrawal failed");
                                    Toast.makeText(CashierActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing withdrawal response", e);
                                Toast.makeText(CashierActivity.this,
                                        "Error processing withdrawal response",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            try {
                                JSONObject errorResponse = new JSONObject(responseBody);
                                String errorMessage = errorResponse.optString("message", "Withdrawal failed");
                                Toast.makeText(CashierActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(CashierActivity.this,
                                        "Withdrawal failed with code: " + response.code(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating withdrawal request", e);
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error creating withdrawal request", Toast.LENGTH_SHORT).show();
        }
    }

    private void performAddMoney(double amount, String reason) {
        loadingProgressBar.setVisibility(View.VISIBLE);

        try {
            JSONObject requestJson = new JSONObject();
            requestJson.put("type", "deposit");
            requestJson.put("amount", (int)(amount * 100)); // Convert to cents/smallest currency unit
            requestJson.put("description", reason);

            String apiUrl = String.format(API_URL_TRANSACTION, activeSessionId);

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    requestJson.toString()
            );

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(requestBody)
                    .build();

            Log.d(TAG, "Add money request URL: " + apiUrl);
            Log.d(TAG, "Add money request body: " + requestJson.toString());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Add money request failed", e);
                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        Toast.makeText(CashierActivity.this,
                                "Add money failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Add money response: " + responseBody);

                    runOnUiThread(() -> {
                        loadingProgressBar.setVisibility(View.GONE);

                        if (response.isSuccessful()) {
                            try {
                                JSONObject jsonResponse = new JSONObject(responseBody);
                                if ("success".equals(jsonResponse.getString("status"))) {
                                    String message = jsonResponse.optString("message", "Transaction completed successfully");
                                    Toast.makeText(CashierActivity.this,
                                            message,
                                            Toast.LENGTH_SHORT).show();

                                    // Update local balance and refresh session info
                                    currentBalance += amount;
                                    currentBalanceTextView.setText("Current Balance: " + currencyFormat.format(currentBalance));
                                    checkCashierSession(); // Refresh to get updated balance from server
                                } else {
                                    String message = jsonResponse.optString("message", "Add money failed");
                                    Toast.makeText(CashierActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing add money response", e);
                                Toast.makeText(CashierActivity.this,
                                        "Error processing add money response",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            try {
                                JSONObject errorResponse = new JSONObject(responseBody);
                                String errorMessage = errorResponse.optString("message", "Add money failed");
                                Toast.makeText(CashierActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                Toast.makeText(CashierActivity.this,
                                        "Add money failed with code: " + response.code(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Error creating add money request", e);
            loadingProgressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error creating add money request", Toast.LENGTH_SHORT).show();
        }
    }
}