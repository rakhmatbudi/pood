package com.restaurant.management.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
            // Format item name with variant if available using getDisplayName()
            String displayName = item.getDisplayName();
            Log.d(TAG, "Binding item: " + item.getMenuItemName() + " with variant: " + item.getVariantName() + " = " + displayName);
            itemNameTextView.setText(displayName);

            // Set quantity
            quantityTextView.setText(String.valueOf(item.getQuantity()));

            // Format and set price
            String formattedPrice = formatPriceWithCurrency(item.getTotalPrice());
            priceTextView.setText(formattedPrice);
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