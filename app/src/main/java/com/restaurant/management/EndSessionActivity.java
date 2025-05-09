package com.restaurant.management;

import android.os.Bundle;
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
import com.restaurant.management.database.repository.CashierSessionPaymentRepository;
import com.restaurant.management.database.repository.CashierSessionRepository;
import com.restaurant.management.database.repository.PaymentModeRepository;
import com.restaurant.management.models.CashDenomination;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.CashierSessionPayment;
import com.restaurant.management.models.PaymentMode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EndSessionActivity extends AppCompatActivity {

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

    private CashierSessionRepository cashierSessionRepository;
    private CashDenominationRepository cashDenominationRepository;
    private PaymentModeRepository paymentModeRepository;
    private CashierSessionPaymentRepository cashierSessionPaymentRepository;
    private ExecutorService executorService;

    private CashierSession activeSession;
    private List<CashDenomination> denominations;
    private List<PaymentMode> paymentModes;
    private Map<Integer, EditText> denominationInputs;
    private Map<Integer, TextView> paymentModeAmounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end_session);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("End Cashier Session");

        // Get sessionId and userId from intent
        sessionId = getIntent().getIntExtra("sessionId", -1);
        userId = getIntent().getIntExtra("userId", -1);

        // Initialize UI components
        denominationsContainer = findViewById(R.id.denominations_container);
        paymentModesContainer = findViewById(R.id.payment_modes_container);
        notesEditText = findViewById(R.id.notes_edit_text);
        confirmEndSessionButton = findViewById(R.id.confirm_end_session_button);
        totalAmountTextView = findViewById(R.id.total_amount_text_view);
        expectedAmountTextView = findViewById(R.id.expected_amount_text_view);
        differenceAmountTextView = findViewById(R.id.difference_amount_text_view);
        progressBar = findViewById(R.id.progress_bar);

        // Initialize database and repositories
        RestaurantDatabase database = RestaurantDatabase.getInstance(this);
        cashierSessionRepository = new CashierSessionRepository(database.cashierSessionDao());
        cashDenominationRepository = new CashDenominationRepository(database.cashDenominationDao());
        paymentModeRepository = new PaymentModeRepository(database.paymentModeDao());
        cashierSessionPaymentRepository = new CashierSessionPaymentRepository(database.cashierSessionPaymentDao());

        executorService = Executors.newSingleThreadExecutor();
        denominationInputs = new HashMap<>();
        paymentModeAmounts = new HashMap<>();

        // Load session data
        loadSessionData();

        // Set up button click listener
        confirmEndSessionButton.setOnClickListener(v -> confirmEndSession());
    }

    private void loadSessionData() {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            // Load session, denominations, payment modes
            activeSession = cashierSessionRepository.getCashierSessionById(sessionId);
            denominations = cashDenominationRepository.getAllDenominations();
            paymentModes = paymentModeRepository.getAllPaymentModes();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (activeSession != null) {
                    expectedAmountTextView.setText(String.format("Expected: %s", activeSession.getOpeningAmount().toString()));
                    createDenominationInputs();
                    createPaymentModeViews();
                } else {
                    Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
                    finish();
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

    private void createPaymentModeViews() {
        paymentModesContainer.removeAllViews();

        for (PaymentMode paymentMode : paymentModes) {
            View paymentModeView = getLayoutInflater().inflate(R.layout.item_payment_mode, null);

            TextView paymentModeNameText = paymentModeView.findViewById(R.id.payment_mode_name_text);
            TextView amountTextView = paymentModeView.findViewById(R.id.amount_text_view);

            paymentModeNameText.setText(paymentMode.getName());

            // Load expected amount for this payment mode
            executorService.execute(() -> {
                BigDecimal expectedAmount = cashierSessionPaymentRepository.getExpectedAmountForPaymentMode(sessionId, paymentMode.getId());
                if (expectedAmount == null) {
                    expectedAmount = BigDecimal.ZERO;
                }

                final BigDecimal finalExpectedAmount = expectedAmount;
                runOnUiThread(() -> {
                    amountTextView.setText(finalExpectedAmount.toString());
                });
            });

            paymentModeAmounts.put(paymentMode.getId(), amountTextView);
            paymentModesContainer.addView(paymentModeView);
        }
    }

    private void updateTotalAmount() {
        BigDecimal total = calculateTotalAmount();
        BigDecimal expected = activeSession.getOpeningAmount();
        BigDecimal difference = total.subtract(expected);

        totalAmountTextView.setText(String.format("Total: %s", total.toString()));
        differenceAmountTextView.setText(String.format("Difference: %s", difference.toString()));

        // Set text color based on difference
        int color;
        if (difference.compareTo(BigDecimal.ZERO) < 0) {
            color = getResources().getColor(R.color.red);
        } else if (difference.compareTo(BigDecimal.ZERO) > 0) {
            color = getResources().getColor(R.color.green);
        } else {
            color = getResources().getColor(R.color.black);
        }
        differenceAmountTextView.setTextColor(color);
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

    private void confirmEndSession() {
        BigDecimal closingAmount = calculateTotalAmount();
        BigDecimal expectedAmount = activeSession.getOpeningAmount();
        BigDecimal difference = closingAmount.subtract(expectedAmount);
        String notes = notesEditText.getText().toString();

        progressBar.setVisibility(View.VISIBLE);
        confirmEndSessionButton.setEnabled(false);

        executorService.execute(() -> {
            // Update session
            activeSession.setClosingAmount(closingAmount);
            activeSession.setExpectedAmount(expectedAmount);
            activeSession.setDifference(difference);
            if (activeSession.getNotes() != null && !activeSession.getNotes().isEmpty()) {
                activeSession.setNotes(activeSession.getNotes() + "\n\nClosing notes: " + notes);
            } else {
                activeSession.setNotes(notes);
            }
            activeSession.setClosedAt(new Date());

            int updated = cashierSessionRepository.updateCashierSession(activeSession);

            // Process payment modes
            boolean allPaymentModesProcessed = true;
            for (PaymentMode paymentMode : paymentModes) {
                TextView amountTextView = paymentModeAmounts.get(paymentMode.getId());
                if (amountTextView != null) {
                    try {
                        String amountStr = amountTextView.getText().toString();
                        BigDecimal paymentAmount = new BigDecimal(amountStr);

                        // Check if payment entry already exists
                        List<CashierSessionPayment> existingPayments =
                                cashierSessionPaymentRepository.getPaymentsByCashierSessionId(sessionId);

                        boolean paymentExists = false;
                        for (CashierSessionPayment existingPayment : existingPayments) {
                            if (existingPayment.getPaymentModeId() == paymentMode.getId()) {
                                // Update existing payment
                                existingPayment.setExpectedAmount(paymentAmount);
                                existingPayment.setActualAmount(paymentAmount); // For simplicity, actual = expected
                                cashierSessionPaymentRepository.updateCashierSessionPayment(existingPayment);
                                paymentExists = true;
                                break;
                            }
                        }

                        if (!paymentExists) {
                            // Create new payment
                            CashierSessionPayment payment = new CashierSessionPayment();
                            payment.setCashierSessionId(sessionId);
                            payment.setPaymentModeId(paymentMode.getId());
                            payment.setExpectedAmount(paymentAmount);
                            payment.setActualAmount(paymentAmount); // For simplicity, actual = expected

                            long insertResult = cashierSessionPaymentRepository.insertCashierSessionPayment(payment);
                            if (insertResult <= 0) {
                                allPaymentModesProcessed = false;
                            }
                        }
                    } catch (NumberFormatException e) {
                        allPaymentModesProcessed = false;
                    }
                }
            }

            final boolean finalAllPaymentModesProcessed = allPaymentModesProcessed;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                confirmEndSessionButton.setEnabled(true);

                if (updated > 0 && finalAllPaymentModesProcessed) {
                    Toast.makeText(EndSessionActivity.this, "Session closed successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EndSessionActivity.this, "Failed to close session", Toast.LENGTH_SHORT).show();
                }
            });
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