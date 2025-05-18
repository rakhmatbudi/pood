package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.OrderItem;

import java.util.List;

public class OrderItemCompactAdapter extends RecyclerView.Adapter<OrderItemCompactAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;

    public OrderItemCompactAdapter(List<OrderItem> orderItems, Context context) {
        this.orderItems = orderItems;
        this.context = context;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView itemQuantityTextView;
        private TextView itemNameTextView;
        private TextView itemPriceTextView;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemQuantityTextView = itemView.findViewById(R.id.item_quantity);
            itemNameTextView = itemView.findViewById(R.id.item_name);
            itemPriceTextView = itemView.findViewById(R.id.item_price);
        }

        public void bind(OrderItem item) {
            // Set quantity
            itemQuantityTextView.setText(String.valueOf(item.getQuantity()) + "x");

            // Set item name
            itemNameTextView.setText(item.getMenuItemName());

            // Format and set price
            String formattedPrice = formatPriceWithCurrency(item.getTotalPrice());
            itemPriceTextView.setText(formattedPrice);
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