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
                .inflate(R.layout.item_order, parent, false);
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
        TextView orderNumberTextView;
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
            orderNumberTextView = itemView.findViewById(R.id.order_number_text_view);
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

        void bind(Order order) {
            // Bind data from the Order model to the views
            orderNumberTextView.setText("Order #" + order.getOrderNumber());
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

            // Set items text
            itemsTextView.setText(order.getItemsSummary());

            // Set total
            totalTextView.setText(order.getFormattedTotal());

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