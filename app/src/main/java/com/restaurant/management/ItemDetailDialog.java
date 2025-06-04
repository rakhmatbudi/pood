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
import android.widget.CheckBox;
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

        // New method to handle custom price items and complimentary items
        default void onItemAdd(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice, boolean isComplimentary) {
            // Default implementation calls the original method for backward compatibility
            onItemAdd(menuItem, variantId, quantity, notes);
        }

        // Overloaded method for custom price without complimentary flag (backward compatibility)
        default void onItemAdd(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice) {
            // Call the new method with complimentary flag set to false
            onItemAdd(menuItem, variantId, quantity, notes, customPrice, false);
        }
    }

    private ProductItem menuItem;
    private OnItemAddListener listener;
    private int quantity = 1;
    private Long selectedVariantId = null;
    private double currentPrice;
    private double originalPrice; // Store original price for when complimentary is unchecked
    private boolean isCustomItem = false;
    private boolean isComplimentary = false;

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

    // New views for complimentary checkbox
    private CheckBox complimentaryCheckBox;

    public ItemDetailDialog(@NonNull Context context, ProductItem menuItem, OnItemAddListener listener) {
        super(context);
        this.menuItem = menuItem;
        this.listener = listener;
        this.currentPrice = menuItem.getPrice();
        this.originalPrice = menuItem.getPrice(); // Store original price

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

        // Find complimentary checkbox
        complimentaryCheckBox = findViewById(R.id.complimentary_checkbox);

        // Set menu item details
        itemNameTextView.setText(menuItem.getName());

        // Set description if available
        if (menuItem.getDescription() != null && !menuItem.getDescription().isEmpty()) {
            itemDescriptionTextView.setText(menuItem.getDescription());
            itemDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            itemDescriptionTextView.setVisibility(View.GONE);
        }

        // Set up complimentary checkbox
        setupComplimentaryCheckbox();

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
                                originalPrice = menuItem.getPrice();
                            } else {
                                // Variant selected (position - 1 because we added "Regular" as first item)
                                Variant selectedVariant = menuItem.getVariants().get(position - 1);
                                selectedVariantId = selectedVariant.getId();
                                originalPrice = selectedVariant.getPrice();
                            }

                            // Update current price based on complimentary status
                            if (!isComplimentary) {
                                currentPrice = originalPrice;
                            }
                            updatePriceDisplay();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            // If nothing is selected, use regular price
                            selectedVariantId = null;
                            originalPrice = menuItem.getPrice();
                            if (!isComplimentary) {
                                currentPrice = originalPrice;
                            }
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
            if (isCustomItem && !isComplimentary && !isValidCustomPrice()) {
                // Show error or prevent adding if custom price is invalid (unless complimentary)
                return;
            }

            if (listener != null) {
                String notes = notesEditText.getText().toString().trim();

                if (isCustomItem || isComplimentary) {
                    // For custom items or complimentary items, pass the custom price and complimentary status
                    Double customPrice = currentPrice;
                    // Call the new method with custom price and complimentary flag
                    listener.onItemAdd(menuItem, selectedVariantId, quantity, notes, customPrice, isComplimentary);
                } else {
                    // For regular items, use the original method
                    listener.onItemAdd(menuItem, selectedVariantId, quantity, notes);
                }
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }

    private void setupComplimentaryCheckbox() {
        if (complimentaryCheckBox != null) {
            complimentaryCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isComplimentary = isChecked;

                if (isChecked) {
                    // When complimentary is checked, set price to 0
                    currentPrice = 0.0;

                    // Disable custom price input and variant selection for complimentary items
                    if (customPriceContainer != null && isCustomItem) {
                        customPriceEditText.setEnabled(false);
                        customPriceEditText.setText("0");
                    }
                    if (variantSpinner != null) {
                        variantSpinner.setEnabled(false);
                    }
                } else {
                    // When complimentary is unchecked, restore original price
                    if (isCustomItem) {
                        // For custom items, restore to 0 and let user enter price
                        currentPrice = 0.0;
                        if (customPriceEditText != null) {
                            customPriceEditText.setEnabled(true);
                            customPriceEditText.setText("");
                        }
                    } else {
                        // For regular items, restore original price
                        currentPrice = originalPrice;
                    }

                    // Re-enable variant selection
                    if (variantSpinner != null) {
                        variantSpinner.setEnabled(true);
                    }
                }

                updatePriceDisplay();
                updateAddButtonState();
            });
        }
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
                        if (isComplimentary) {
                            // Don't process price changes when complimentary is checked
                            return;
                        }

                        String priceText = s.toString().trim();
                        if (!priceText.isEmpty()) {
                            try {
                                double customPrice = Double.parseDouble(priceText);
                                if (customPrice >= 0) {
                                    currentPrice = customPrice;
                                    originalPrice = customPrice; // Update original price for custom items
                                    updatePriceDisplay();
                                    updateAddButtonState();
                                } else {
                                    updateAddButtonState();
                                }
                            } catch (NumberFormatException e) {
                                updateAddButtonState();
                            }
                        } else {
                            currentPrice = 0;
                            updatePriceDisplay();
                            updateAddButtonState();
                        }
                    }
                });

                // Set hint text
                customPriceEditText.setHint("Enter price");

                // Initially disable add button until valid price is entered
                updateAddButtonState();
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

    private void updateAddButtonState() {
        boolean enabled = true;

        if (isCustomItem && !isComplimentary) {
            // For custom items that are not complimentary, require valid price
            enabled = isValidCustomPrice();
        }
        // For complimentary items or regular items, always enable the button

        enableAddButton(enabled);
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

        if (isComplimentary) {
            // Show "FREE" or "Complimentary" text for complimentary items
            itemPriceTextView.setText("FREE (" + formattedPrice + ")");
        } else {
            itemPriceTextView.setText(formattedPrice);
        }
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