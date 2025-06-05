package com.restaurant.management.helpers;

import android.content.Context;
import android.graphics.Paint;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.restaurant.management.R;
import com.restaurant.management.adapters.DiscountSpinnerAdapter;
import com.restaurant.management.models.Discount;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.RoundingConfig;

import java.util.List;

public class PaymentUIHelper {
    private final Context context;

    public interface DiscountSelectionListener {
        void onDiscountSelected(Discount discount);
        void onDiscountRemoved();
    }

    public interface PaymentMethodSelectionListener {
        void onPaymentMethodSelected(PaymentMethod method);
    }

    public PaymentUIHelper(Context context) {
        this.context = context;
    }

    public void setupDiscountSpinner(Spinner discountSpinner, List<Discount> discounts,
                                     DiscountSelectionListener listener) {
        DiscountSpinnerAdapter adapter = new DiscountSpinnerAdapter(context, discounts);
        discountSpinner.setAdapter(adapter);

        discountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Discount selectedDiscount = (Discount) parent.getItemAtPosition(position);

                if (selectedDiscount != null && selectedDiscount.getId() != -1) {
                    listener.onDiscountSelected(selectedDiscount);
                } else {
                    listener.onDiscountRemoved();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                listener.onDiscountRemoved();
            }
        });
    }

    public void updatePricingDisplay(TextView orderTotalTextView, TextView discountAmountTextView,
                                     TextView discountedTotalTextView, Discount selectedDiscount,
                                     double originalAmount, double finalAmount, double discountedAmount) {
        if (selectedDiscount == null || selectedDiscount.getId() == -1) {
            // No discount selected
            discountAmountTextView.setVisibility(View.GONE);
            discountedTotalTextView.setVisibility(View.GONE);

            String formattedTotal = formatPriceWithCurrency(finalAmount);
            orderTotalTextView.setText(context.getString(R.string.order_total_format, formattedTotal));
            orderTotalTextView.setPaintFlags(orderTotalTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        } else {
            // Discount applied - show original amount with strikethrough and final charged amount
            discountAmountTextView.setVisibility(View.VISIBLE);

            // Show discount info
            String discountText = context.getString(R.string.discount_applied,
                    selectedDiscount.getName(), selectedDiscount.getAmount());
            if (discountedAmount > 0) {
                discountText += " (-" + formatPriceWithCurrency(discountedAmount) + ")";
            }
            discountAmountTextView.setText(discountText);

            String totalLabel = context.getString(R.string.order_total_format, "").replace("%s", "");
            String formattedOriginalPrice = formatPriceWithCurrency(originalAmount);
            String formattedFinalPrice = formatPriceWithCurrency(finalAmount);

            SpannableString spannableString = new SpannableString(
                    totalLabel + formattedOriginalPrice + "  " + formattedFinalPrice);

            spannableString.setSpan(new StrikethroughSpan(),
                    totalLabel.length(),
                    totalLabel.length() + formattedOriginalPrice.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            orderTotalTextView.setText(spannableString);
            discountedTotalTextView.setVisibility(View.GONE);
        }
    }

    public void populatePaymentMethodsUI(RadioGroup paymentMethodRadioGroup,
                                         List<PaymentMethod> paymentMethods,
                                         PaymentMethodSelectionListener listener) {
        paymentMethodRadioGroup.removeAllViews();

        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod method = paymentMethods.get(i);

            RadioButton radioButton = new RadioButton(context);
            radioButton.setId(View.generateViewId());
            radioButton.setText(method.getName());
            radioButton.setTag(method.getId());
            radioButton.setPadding(32, 30, 32, 30);

            paymentMethodRadioGroup.addView(radioButton);

            if (i == 0) {
                radioButton.setChecked(true);
                listener.onPaymentMethodSelected(method);
            }
        }

        paymentMethodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            for (int i = 0; i < paymentMethodRadioGroup.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) paymentMethodRadioGroup.getChildAt(i);
                if (radioButton.getId() == checkedId) {
                    String paymentMethodId = (String) radioButton.getTag();

                    for (PaymentMethod method : paymentMethods) {
                        if (method.getId().equals(paymentMethodId)) {
                            listener.onPaymentMethodSelected(method);
                            break;
                        }
                    }
                    break;
                }
            }
        });
    }

    public void updateChangeDisplay(TextView changeTextView, double amountPaid, double finalAmount,
                                    String selectedPaymentMethod, RoundingConfig roundingConfig,
                                    Context context) {
        double amountToPay = finalAmount;

        double amountToCompare = amountToPay;
        if ("cash".equalsIgnoreCase(selectedPaymentMethod) && roundingConfig != null) {
            amountToCompare = applyRounding(amountToPay, roundingConfig);
        }

        double change = amountPaid - amountToPay;

        String formattedChange = formatPriceWithCurrency(Math.max(0, change));
        changeTextView.setText(context.getString(R.string.change_format, formattedChange));

        if (amountPaid < amountToCompare && "cash".equalsIgnoreCase(selectedPaymentMethod)) {
            changeTextView.setTextColor(context.getResources().getColor(R.color.colorError));
        } else {
            changeTextView.setTextColor(context.getResources().getColor(R.color.colorNormal));
        }
    }

    public boolean isPaymentValid(double amountPaid, double finalAmount, String selectedPaymentMethod,
                                  RoundingConfig roundingConfig) {
        if (!"cash".equalsIgnoreCase(selectedPaymentMethod)) {
            return true;
        }

        double amountToValidate = finalAmount;
        if (roundingConfig != null) {
            amountToValidate = applyRounding(finalAmount, roundingConfig);
        }

        return amountPaid >= amountToValidate;
    }

    public double applyRounding(double amount, RoundingConfig roundingConfig) {
        if (amount < 0 || roundingConfig == null) {
            return amount;
        }

        int roundingBelow = roundingConfig.getRoundingBelow();
        int roundingNumber = roundingConfig.getRoundingNumber();

        long amountInt = Math.round(amount);
        long remainder = amountInt % roundingNumber;
        long roundedAmount;

        if (remainder <= roundingBelow) {
            roundedAmount = amountInt - remainder;
        } else {
            roundedAmount = amountInt + (roundingNumber - remainder);
        }

        return roundedAmount;
    }

    public String formatPriceWithCurrency(double price) {
        long roundedPrice = Math.round(price);

        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        String currencyPrefix = context.getString(R.string.currency_prefix);
        return context.getString(R.string.currency_format_pattern, currencyPrefix, formattedPrice.toString());
    }

    public void resetDiscountSpinner(Spinner discountSpinner) {
        if (discountSpinner != null && discountSpinner.getAdapter() != null) {
            discountSpinner.setOnItemSelectedListener(null);
            discountSpinner.setSelection(0);
        }
    }

    public void setLoadingState(View progressBar, View contentView, View processPaymentButton,
                                View cancelButton, boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (contentView != null) {
            contentView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }

        if (processPaymentButton != null) {
            processPaymentButton.setEnabled(!isLoading);
        }

        if (cancelButton != null) {
            cancelButton.setEnabled(!isLoading);
        }
    }
}