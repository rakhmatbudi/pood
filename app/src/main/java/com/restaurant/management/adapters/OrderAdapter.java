package com.restaurant.management.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.restaurant.management.R;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
    private List<Order> orders;
    private OnOrderClickListener listener;
    private Context context;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener, Context context) {
        this.orders = orders;
        this.listener = listener;
        this.context = context;

        // Set timezone for parsing API dates (UTC)
        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Local timezone for display
        displayDateFormat.setTimeZone(TimeZone.getDefault());
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
        Order order = orders.get(position);
        holder.bind(order);

        // Set up the nested RecyclerView for order items
        List<OrderItem> orderItems = order.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            holder.orderItemsRecyclerView.setVisibility(View.VISIBLE);

            // Configure the RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(context);
            holder.orderItemsRecyclerView.setLayoutManager(layoutManager);



            // Create and set the adapter
            OrderItemCompactAdapter itemsAdapter = new OrderItemCompactAdapter(orderItems, context);
            holder.orderItemsRecyclerView.setAdapter(itemsAdapter);
        } else {
            holder.orderItemsRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private RecyclerView orderItemsRecyclerView;
        private CardView cardView;
        private TextView orderNumberTextView;
        private TextView tableNumberTextView;
        private TextView orderStatusTextView;
        private TextView orderTotalTextView;
        private TextView timeTextView;
        private TextView customerTextView;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.order_card);
            orderNumberTextView = itemView.findViewById(R.id.order_number_text_view);
            tableNumberTextView = itemView.findViewById(R.id.table_number_text_view);
            orderStatusTextView = itemView.findViewById(R.id.order_status_text_view);
            orderTotalTextView = itemView.findViewById(R.id.order_total_text_view);
            timeTextView = itemView.findViewById(R.id.time_text_view);
            customerTextView = itemView.findViewById(R.id.customer_text_view);

            orderItemsRecyclerView = itemView.findViewById(R.id.order_items_recycler_view);

            cardView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onOrderClick(orders.get(position));
                }
            });
        }

        void bind(Order order) {
            // Set order details in views
            orderNumberTextView.setText(context.getString(R.string.order_number_format,
                    order.getOrderNumber()));

            tableNumberTextView.setText(context.getString(R.string.table_number_format,
                    order.getTableNumber()));

            // Format and set total amount
            String formattedTotal = formatPriceWithCurrency(order.getTotalAmount());
            orderTotalTextView.setText(context.getString(R.string.order_total_format, formattedTotal));

            // Format and set time
            String formattedTime = formatAPIDate(order.getCreatedAt());
            timeTextView.setText(formattedTime);

            // Set customer name if available
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                customerTextView.setText(context.getString(R.string.customer_name_format,
                        order.getCustomerName()));
                customerTextView.setVisibility(View.VISIBLE);
            } else {
                customerTextView.setVisibility(View.GONE);
            }
        }

        private String formatAPIDate(String apiDateStr) {
            if (apiDateStr == null || apiDateStr.isEmpty()) {
                return "N/A";
            }

            try {
                // Parse the API date (UTC)
                Date date = apiDateFormat.parse(apiDateStr);

                // Get time ago string for relative time
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                        date.getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS);

                return timeAgo.toString();
            } catch (ParseException e) {
                return apiDateStr; // Return original if parsing fails
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