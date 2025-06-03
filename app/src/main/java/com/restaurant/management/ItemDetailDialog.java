package com.restaurant.management;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;

import java.util.ArrayList;
import java.util.List;

public class ItemDetailDialog extends Dialog {
    private static final String TAG = "ItemDetailDialog";

    public interface OnItemAddListener {
        void onItemAdd(ProductItem menuItem, Long variantId, int quantity, String notes);

        // New method to handle custom price items
        default void onItemAdd(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice) {
            // Default implementation calls the original method for backward compatibility
            onItemAdd(menuItem, variantId, quantity, notes);
        }
    }

    private ProductItem menuItem;
    private OnItemAddListener listener;
    private int quantity = 1;
    private Long selectedVariantId = null;
    private double currentPrice;
    private boolean isCustomItem = false;

    private TextView itemNameTextView;
    private TextView itemDescriptionTextView;
    private TextView itemPriceTextView;
    private TextView quantityTextView;
    private Button decreaseButton;
    private Button increaseButton;
    private EditText notesEditText;
    private Button addButton;
    private Button cancelButton;
    private Spinner variantSpinner;
    private View variantContainer;

    // New views for custom price input
    private View customPriceContainer;
    private EditText customPriceEditText;
    private TextView customPriceLabel;

    public ItemDetailDialog(@NonNull Context context, ProductItem menuItem, OnItemAddListener listener) {
        super(context);
        this.menuItem = menuItem;
        this.listener = listener;
        this.currentPrice = menuItem.getPrice();

        // Check if this is a custom item
        this.isCustomItem = "Custom".equalsIgnoreCase(menuItem.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_item_detail);

        // Make dialog background transparent
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Initialize views
        itemNameTextView = findViewById(R.id.item_name_text_view);
        itemDescriptionTextView = findViewById(R.id.item_description_text_view);
        itemPriceTextView = findViewById(R.id.item_price_text_view);
        quantityTextView = findViewById(R.id.quantity_text_view);
        decreaseButton = findViewById(R.id.decrease_button);
        increaseButton = findViewById(R.id.increase_button);
        notesEditText = findViewById(R.id.notes_edit_text);
        addButton = findViewById(R.id.add_button);
        cancelButton = findViewById(R.id.cancel_button);

        // Find variant views
        variantContainer = findViewById(R.id.variant_container);
        variantSpinner = findViewById(R.id.variant_spinner);

        // Find custom price views
        customPriceContainer = findViewById(R.id.custom_price_container);
        customPriceEditText = findViewById(R.id.custom_price_edit_text);
        customPriceLabel = findViewById(R.id.custom_price_label);

        // Set menu item details
        itemNameTextView.setText(menuItem.getName());

        // Set description if available
        if (menuItem.getDescription() != null && !menuItem.getDescription().isEmpty()) {
            itemDescriptionTextView.setText(menuItem.getDescription());
            itemDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            itemDescriptionTextView.setVisibility(View.GONE);
        }

        // Handle custom item price input
        if (isCustomItem) {
            if (customPriceContainer != null) {
                setupCustomPriceInput();
            } else {
                // Fallback: treat as regular item for now
                isCustomItem = false;
            }
        } else {
            // Hide custom price container for non-custom items
            if (customPriceContainer != null) {
                customPriceContainer.setVisibility(View.GONE);
            }

            // Set up variant spinner if there are variants
            if (menuItem.hasVariants() && menuItem.getVariants().size() > 0) {
                if (variantContainer != null) {
                    variantContainer.setVisibility(View.VISIBLE);

                    // Create the adapter for variants
                    List<String> variantLabels = new ArrayList<>();
                    variantLabels.add("Regular - " + formatPrice(menuItem.getPrice(),
                            getContext().getString(R.string.currency_prefix))); // Default option

                    List<Variant> variants = menuItem.getVariants();
                    for (Variant variant : variants) {
                        String formattedPrice = formatPrice(variant.getPrice(),
                                getContext().getString(R.string.currency_prefix));
                        variantLabels.add(variant.getName() + " - " + formattedPrice);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_item,
                            variantLabels
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    variantSpinner.setAdapter(adapter);

                    // Handle variant selection
                    variantSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                // Regular price option
                                selectedVariantId = null;
                                currentPrice = menuItem.getPrice();
                            } else {
                                // Variant selected (position - 1 because we added "Regular" as first item)
                                Variant selectedVariant = menuItem.getVariants().get(position - 1);
                                selectedVariantId = selectedVariant.getId();
                                currentPrice = selectedVariant.getPrice();
                            }
                            updatePriceDisplay();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // If nothing is selected, use regular price
                            selectedVariantId = null;
                            currentPrice = menuItem.getPrice();
                            updatePriceDisplay();
                        }
                    });
                }
            } else {
                if (variantContainer != null) {
                    variantContainer.setVisibility(View.GONE);
                }
            }
        }

        // Format price with currency
        updatePriceDisplay();

        // Set initial quantity
        updateQuantityDisplay();

        // Set up click listeners
        decreaseButton.setOnClickListener(v -> {
            decreaseQuantity();
            updatePriceDisplay();
        });

        increaseButton.setOnClickListener(v -> {
            increaseQuantity();
            updatePriceDisplay();
        });

        addButton.setOnClickListener(v -> {
            if (isCustomItem && !isValidCustomPrice()) {
                // Show error or prevent adding if custom price is invalid
                return;
            }

            if (listener != null) {
                String notes = notesEditText.getText().toString().trim();

                if (isCustomItem) {
                    // For custom items, pass the custom price
                    Double customPrice = currentPrice;
                    // Call the new method with custom price
                    listener.onItemAdd(menuItem, selectedVariantId, quantity, notes, customPrice);
                } else {
                    // For regular items, use the original method
                    listener.onItemAdd(menuItem, selectedVariantId, quantity, notes);
                }
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupCustomPriceInput() {
        if (customPriceContainer != null) {
            customPriceContainer.setVisibility(View.VISIBLE);

            // Hide variant container for custom items
            if (variantContainer != null) {
                variantContainer.setVisibility(View.GONE);
            }

            // Set up the custom price EditText
            if (customPriceEditText != null) {
                customPriceEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        String priceText = s.toString().trim();
                        if (!priceText.isEmpty()) {
                            try {
                                double customPrice = Double.parseDouble(priceText);
                                if (customPrice >= 0) {
                                    currentPrice = customPrice;
                                    updatePriceDisplay();
                                    enableAddButton(true);
                                } else {
                                    enableAddButton(false);
                                }
                            } catch (NumberFormatException e) {
                                enableAddButton(false);
                            }
                        } else {
                            currentPrice = 0;
                            updatePriceDisplay();
                            enableAddButton(false);
                        }
                    }
                });

                // Set hint text
                customPriceEditText.setHint("Enter price");

                // Initially disable add button until valid price is entered
                enableAddButton(false);
            }

            // Set label text
            if (customPriceLabel != null) {
                customPriceLabel.setText("Custom Price:");
            }
        }
    }

    private boolean isValidCustomPrice() {
        if (!isCustomItem) return true;

        String priceText = customPriceEditText != null ? customPriceEditText.getText().toString().trim() : "";
        if (priceText.isEmpty()) return false;

        try {
            double price = Double.parseDouble(priceText);
            return price >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void enableAddButton(boolean enabled) {
        if (addButton != null) {
            addButton.setEnabled(enabled);
            addButton.setAlpha(enabled ? 1.0f : 0.5f);
        }
    }

    private void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
            updateQuantityDisplay();
        }
    }

    private void increaseQuantity() {
        quantity++;
        updateQuantityDisplay();
    }

    private void updateQuantityDisplay() {
        quantityTextView.setText(String.valueOf(quantity));

        // Disable decrease button if quantity is 1
        decreaseButton.setEnabled(quantity > 1);
        decreaseButton.setAlpha(quantity > 1 ? 1.0f : 0.5f);
    }

    private void updatePriceDisplay() {
        double totalPrice = currentPrice * quantity;
        String formattedPrice = formatPrice(totalPrice, getContext().getString(R.string.currency_prefix));
        itemPriceTextView.setText(formattedPrice);
    }

    private String formatPrice(double price, String currencyPrefix) {
        // Round to the nearest integer (no decimal)
        long roundedPrice = Math.round(price);

        // Format as xxx.xxx.xxx
        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            // Add dot after every 3 digits from the right, but not at the end
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        // Format according to the pattern (allows for different currency placement)
        return String.format(getContext().getString(R.string.currency_format_pattern),
                currencyPrefix, formattedPrice.toString());
    }
}