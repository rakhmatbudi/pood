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

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order_detail, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.itemNameTextView.setText(item.getDisplayName());
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private TextView itemQuantityTextView;
        private TextView itemPriceTextView;
        private TextView itemNotesTextView;
        // Removed unit price, kitchen status, and item status TextView declarations

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.item_name_text_view);
            itemQuantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
            itemPriceTextView = itemView.findViewById(R.id.item_price_text_view);
            itemNotesTextView = itemView.findViewById(R.id.item_notes_text_view);
            // Removed findViewById for unit price, kitchen status, and item status TextViews
        }

        public void bind(OrderItem item) {
            // Set item name with variant - use getDisplayName() instead of getMenuItemName()
            itemNameTextView.setText(item.getDisplayName());

            // Set quantity
            itemQuantityTextView.setText(String.valueOf(item.getQuantity()) + "x");

            // Format and set total price
            String formattedPrice = formatPriceWithCurrency(item.getTotalPrice());
            itemPriceTextView.setText(formattedPrice);

            // Display notes if available
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                itemNotesTextView.setText(item.getNotes());
                itemNotesTextView.setVisibility(View.VISIBLE);
            } else {
                itemNotesTextView.setVisibility(View.GONE);
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

            // Format according to the pattern in strings.xml (allows for different currency placement)
            return context.getString(R.string.currency_format_pattern, currencyPrefix, formattedPrice.toString());
        }
    }
}