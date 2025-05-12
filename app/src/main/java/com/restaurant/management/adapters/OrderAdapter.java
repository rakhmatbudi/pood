package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> ordersList;
    private OnOrderClickListener listener;
    private Context context;

    // Interface for click events
    public interface OnOrderClickListener {
        void onOrderClick(Order order, int position);
    }

    // Constructor with List<Order> parameter
    public OrderAdapter(List<Order> ordersList) {
        this.ordersList = ordersList;
    }

    // Set click listener
    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order_activity, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = ordersList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return ordersList != null ? ordersList.size() : 0;
    }

    // ViewHolder class
    class OrderViewHolder extends RecyclerView.ViewHolder {
        CardView orderCard;

        TextView timeTextView;
        TextView tableNumberTextView;
        TextView customerNameTextView;
        TextView itemsTextView;
        TextView totalTextView;
        TextView statusTextView;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views matching your item_order.xml
            orderCard = itemView.findViewById(R.id.order_card);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            tableNumberTextView = itemView.findViewById(R.id.table_number_text_view);
            customerNameTextView = itemView.findViewById(R.id.customer_name_text_view);
            itemsTextView = itemView.findViewById(R.id.items_text_view);
            totalTextView = itemView.findViewById(R.id.total_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);

            // Set click listener for the whole item
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onOrderClick(ordersList.get(position), position);
                }
            });
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

        void bind(Order order) {
            // Bind data from the Order model to the views
            timeTextView.setText(order.getCreatedAt());
            tableNumberTextView.setText("Table: " + order.getTableNumber());

            // Handle customer name (which might be empty in your case)
            String customerName = order.getCustomerName();
            if (customerName == null || customerName.isEmpty()) {
                customerNameTextView.setVisibility(View.GONE);
            } else {
                customerNameTextView.setVisibility(View.VISIBLE);
                customerNameTextView.setText(customerName);
            }

            // Set items text - MODIFIED to display quantity and menu items in a single line
            List<String> items = order.getItems();
            if (items != null && !items.isEmpty()) {
                // Format is typically "1x Menu Item Name (notes)"
                // We want to keep "1x Menu Item Name" but remove the notes and put all items on one line
                StringBuilder itemsText = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    if (i > 0) {
                        itemsText.append(", ");
                    }

                    String itemString = items.get(i);
                    // Remove notes if present
                    int notesIndex = itemString.indexOf(" (");
                    if (notesIndex > 0) {
                        // Remove the notes part
                        itemsText.append(itemString.substring(0, notesIndex));
                    } else {
                        // No notes, keep the full string
                        itemsText.append(itemString);
                    }
                }
                itemsTextView.setText(itemsText.toString());
            } else {
                itemsTextView.setText("No items");
            }

            // Set total with custom formatting (Rp. prefix, format as xxx.xxx.xxx with no decimal)
            String formattedTotal = formatPriceWithCurrency(order.getTotal());
            totalTextView.setText(formattedTotal);

            // Set status
            statusTextView.setText(order.getFormattedStatus());

            // Set status text color based on status
            int colorResId;
            String status = order.getStatus().toLowerCase();
            switch (status) {
                case "pending":
                    colorResId = R.color.status_pending;
                    break;
                case "processing":
                    colorResId = R.color.status_processing;
                    break;
                case "ready":
                    colorResId = R.color.status_ready;
                    break;
                case "completed":
                    colorResId = R.color.status_completed;
                    break;
                case "cancelled":
                    colorResId = R.color.status_cancelled;
                    break;
                default:
                    colorResId = R.color.status_default;
                    break;
            }

            statusTextView.setTextColor(ContextCompat.getColor(context, colorResId));
        }
    }
}