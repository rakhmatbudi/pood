package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.OrderItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;
    private SimpleDateFormat apiDateFormat;
    private SimpleDateFormat displayDateFormat;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;

        // Initialize date formatters
        apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        displayDateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);
        displayDateFormat.setTimeZone(TimeZone.getDefault());
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
        private TextView itemStatusTextView;
        private TextView itemUnitPriceTextView;
        private TextView kitchenStatusTextView; // Updated this line
        private TextView itemTimestampTextView;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.item_name_text_view);
            itemQuantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
            itemPriceTextView = itemView.findViewById(R.id.item_price_text_view);
            itemNotesTextView = itemView.findViewById(R.id.item_notes_text_view);
            itemStatusTextView = itemView.findViewById(R.id.item_status_text_view);
            itemUnitPriceTextView = itemView.findViewById(R.id.item_unit_price_text_view);
            kitchenStatusTextView = itemView.findViewById(R.id.kitchen_status_text_view); // Updated this line
            itemTimestampTextView = itemView.findViewById(R.id.item_timestamp_text_view);
        }

        public void bind(OrderItem item) {
            // Set item name
            itemNameTextView.setText(item.getMenuItemName());

            // Set quantity
            itemQuantityTextView.setText(String.valueOf(item.getQuantity()) + "x");

            // Format and set unit price
            String formattedUnitPrice = formatPriceWithCurrency(item.getUnitPrice());
            itemUnitPriceTextView.setText(context.getString(R.string.unit_price_format, formattedUnitPrice));

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

            // Set item status with appropriate color
            String status = item.getStatus().toLowerCase();
            itemStatusTextView.setText(status);

            int statusColor;
            switch (status) {
                case "new":
                    statusColor = R.color.status_pending;
                    break;
                case "in_progress":
                case "preparing":
                    statusColor = R.color.status_processing;
                    break;
                case "ready":
                    statusColor = R.color.status_ready;
                    break;
                case "completed":
                case "served":
                    statusColor = R.color.status_completed;
                    break;
                case "cancelled":
                    statusColor = R.color.status_cancelled;
                    break;
                default:
                    statusColor = R.color.status_default;
            }

            itemStatusTextView.setTextColor(ContextCompat.getColor(context, statusColor));

            // Set kitchen printed status
            if (item.isKitchenPrinted()) {
                kitchenStatusTextView.setText(context.getString(R.string.kitchen_printed));
                kitchenStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_completed));
            } else {
                kitchenStatusTextView.setText(context.getString(R.string.kitchen_not_printed));
                kitchenStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_pending));
            }

            // Set timestamp
            try {
                Date date = apiDateFormat.parse(item.getCreatedAt());
                String timestamp = displayDateFormat.format(date);
                itemTimestampTextView.setText(timestamp);
            } catch (ParseException e) {
                // If parsing fails, just display the raw string
                itemTimestampTextView.setText(item.getCreatedAt());
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