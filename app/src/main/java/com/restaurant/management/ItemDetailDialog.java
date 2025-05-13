package com.restaurant.management;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.restaurant.management.models.ProductItem;

public class ItemDetailDialog extends Dialog {
    private static final String TAG = "ItemDetailDialog";

    public interface OnItemAddListener {
        void onItemAdd(ProductItem menuItem, int quantity, String notes);
    }

    private ProductItem menuItem;
    private OnItemAddListener listener;
    private int quantity = 1;

    private TextView itemNameTextView;
    private TextView itemDescriptionTextView;
    private TextView itemPriceTextView;
    private TextView quantityTextView;
    private Button decreaseButton;
    private Button increaseButton;
    private EditText notesEditText;
    private Button addButton;
    private Button cancelButton;

    public ItemDetailDialog(@NonNull Context context, ProductItem menuItem, OnItemAddListener listener) {
        super(context);
        this.menuItem = menuItem;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_item_detail);

        Log.d(TAG, "Creating dialog for item: " + menuItem.getName());

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

        // Set menu item details
        itemNameTextView.setText(menuItem.getName());

        // Set description if available
        if (menuItem.getDescription() != null && !menuItem.getDescription().isEmpty()) {
            itemDescriptionTextView.setText(menuItem.getDescription());
            itemDescriptionTextView.setVisibility(View.VISIBLE);
        } else {
            itemDescriptionTextView.setVisibility(View.GONE);
        }

        // Format price with currency
        String formattedPrice = formatPrice(menuItem.getPrice(),
                getContext().getString(R.string.currency_prefix));
        itemPriceTextView.setText(formattedPrice);

        // Set initial quantity
        updateQuantityDisplay();

        // Set up click listeners
        decreaseButton.setOnClickListener(v -> {
            decreaseQuantity();
            Log.d(TAG, "Decrease button clicked, quantity: " + quantity);
        });

        increaseButton.setOnClickListener(v -> {
            increaseQuantity();
            Log.d(TAG, "Increase button clicked, quantity: " + quantity);
        });

        addButton.setOnClickListener(v -> {
            if (listener != null) {
                String notes = notesEditText.getText().toString().trim();
                Log.d(TAG, "Add button clicked with quantity: " + quantity);
                listener.onItemAdd(menuItem, quantity, notes);
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> dismiss());
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