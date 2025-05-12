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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderClickListener listener;

    // Interface for click events
    public interface OnOrderClickListener {
        void onOrderClick(Order order, int position);
    }

    public OrderAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView orderNumberTextView;
        private TextView tableNumberTextView;
        private TextView customerNameTextView;
        private TextView totalTextView;
        private TextView statusTextView;
        private TextView timeTextView;
        private TextView itemsTextView;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.order_card);
            orderNumberTextView = itemView.findViewById(R.id.order_number_text_view);
            tableNumberTextView = itemView.findViewById(R.id.table_number_text_view);
            customerNameTextView = itemView.findViewById(R.id.customer_name_text_view);
            totalTextView = itemView.findViewById(R.id.total_text_view);
            statusTextView = itemView.findViewById(R.id.status_text_view);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            itemsTextView = itemView.findViewById(R.id.items_text_view);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onOrderClick(orderList.get(position), position);
                }
            });
        }

        void bind(Order order) {
            Context context = itemView.getContext();

            // Set order details
            orderNumberTextView.setText(context.getString(R.string.order_number_format, order.getOrderNumber()));

            // Set table number if available
            if (order.getTableNumber() != null && !order.getTableNumber().isEmpty()) {
                tableNumberTextView.setText(context.getString(R.string.table_number_format, order.getTableNumber()));
                tableNumberTextView.setVisibility(View.VISIBLE);
            } else {
                tableNumberTextView.setVisibility(View.GONE);
            }

            // Set customer name if available
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                customerNameTextView.setText(order.getCustomerName());
                customerNameTextView.setVisibility(View.VISIBLE);
            } else {
                customerNameTextView.setVisibility(View.GONE);
            }

            totalTextView.setText(order.getFormattedTotal());
            statusTextView.setText(order.getFormattedStatus());
            itemsTextView.setText(order.getItemsSummary());

            // Format and set time
            timeTextView.setText(formatCreatedAt(order.getCreatedAt()));

            // Set status color based on status
            setStatusColor(order.getStatus(), statusTextView, context);
        }

        private void setStatusColor(String status, TextView statusTextView, Context context) {
            int colorResId;

            if (status == null) {
                colorResId = R.color.status_default;
            } else if (status.equalsIgnoreCase("pending")) {
                colorResId = R.color.status_pending;
            } else if (status.equalsIgnoreCase("processing")) {
                colorResId = R.color.status_processing;
            } else if (status.equalsIgnoreCase("ready")) {
                colorResId = R.color.status_ready;
            } else if (status.equalsIgnoreCase("completed")) {
                colorResId = R.color.status_completed;
            } else if (status.equalsIgnoreCase("cancelled")) {
                colorResId = R.color.status_cancelled;
            } else {
                colorResId = R.color.status_default;
            }

            statusTextView.setTextColor(ContextCompat.getColor(context, colorResId));
        }

        private String formatCreatedAt(String createdAt) {
            if (createdAt == null || createdAt.isEmpty()) {
                return "";
            }

            try {
                // Parse the ISO format date
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = inputFormat.parse(createdAt);

                // Format the date to a more readable format
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return createdAt; // Return the original string if parsing fails
            }
        }
    }
}