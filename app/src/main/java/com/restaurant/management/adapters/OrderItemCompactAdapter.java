package com.restaurant.management.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.OrderItem;

import java.util.List;

public class OrderItemCompactAdapter extends RecyclerView.Adapter<OrderItemCompactAdapter.ViewHolder> {
    private static final String TAG = "OrderItemCompact";
    private List<OrderItem> orderItems;
    private Context context;

    public OrderItemCompactAdapter(List<OrderItem> orderItems, Context context) {
        this.orderItems = orderItems;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item_compact, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private TextView quantityTextView;
        private TextView priceTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.item_name_text_view);
            quantityTextView = itemView.findViewById(R.id.quantity_text_view);
            priceTextView = itemView.findViewById(R.id.price_text_view);
        }

        void bind(OrderItem item) {
            // Check if item is cancelled
            boolean isCancelled = item.getStatus() != null &&
                    item.getStatus().equalsIgnoreCase("cancelled");

            // Format item name with variant if available using getDisplayName()
            String displayName = item.getDisplayName();

            if (itemNameTextView != null) {
                itemNameTextView.setText(displayName);
                applyStrikethroughEffect(itemNameTextView, isCancelled);
            }

            // Set quantity
            if (quantityTextView != null) {
                quantityTextView.setText(String.valueOf(item.getQuantity()));
                applyStrikethroughEffect(quantityTextView, isCancelled);
            }

            // Format and set price
            if (priceTextView != null) {
                String formattedPrice = formatPriceWithCurrency(item.getTotalPrice());
                priceTextView.setText(formattedPrice);
                applyStrikethroughEffect(priceTextView, isCancelled);
            }

            // Apply overall styling to the entire item view
            if (isCancelled) {
                itemView.setAlpha(0.6f); // Make entire item semi-transparent
            } else {
                itemView.setAlpha(1.0f); // Full opacity for active items
            }
        }

        /**
         * Apply or remove strikethrough effect and color changes to a TextView
         */
        private void applyStrikethroughEffect(TextView textView, boolean isCancelled) {
            if (isCancelled) {
                // Apply strikethrough
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                // Make text gray
                if (context != null) {
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                } else {
                    textView.setTextColor(0xFF757575); // Fallback gray color
                }
            } else {
                // Remove strikethrough
                textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

                // Restore normal text color
                if (context != null) {
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                } else {
                    textView.setTextColor(0xFF000000); // Fallback black color
                }
            }
        }

        private String formatPriceWithCurrency(double price) {
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

            // Get currency prefix from strings.xml
            String currencyPrefix = context.getString(R.string.currency_prefix);

            // Format according to the pattern in strings.xml
            return context.getString(R.string.currency_format_pattern,
                    currencyPrefix, formattedPrice.toString());
        }
    }
}