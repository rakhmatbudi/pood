package com.restaurant.management.helpers;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.restaurant.management.R;
import com.restaurant.management.adapters.OrderItemAdapter;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class OrderUiHelper {
    private final Context context;
    private final SimpleDateFormat apiDateFormat;
    private final SimpleDateFormat displayDateFormat;

    // UI Views
    private TextView orderNumberTextView;
    private TextView tableNumberTextView;
    private TextView customerNameTextView;
    private TextView orderStatusTextView;
    private TextView orderTotalTextView;
    private TextView orderDateTextView;
    private TextView orderUpdateTextView;
    private TextView orderServerTextView;
    private TextView orderSessionTextView;
    private RecyclerView orderItemsRecyclerView;
    private Button cancelOrderButton;
    private Button paymentButton;
    private FloatingActionButton addItemFab;

    public OrderUiHelper(Context context) {
        this.context = context;
        this.apiDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        this.displayDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);

        apiDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        displayDateFormat.setTimeZone(TimeZone.getDefault());
    }

    public void initializeViews(View rootView) {
        orderNumberTextView = rootView.findViewById(R.id.order_number_text_view);
        tableNumberTextView = rootView.findViewById(R.id.table_number_text_view);
        customerNameTextView = rootView.findViewById(R.id.customer_name_text_view);
        orderStatusTextView = rootView.findViewById(R.id.order_status_text_view);
        orderTotalTextView = rootView.findViewById(R.id.order_total_text_view);
        orderDateTextView = rootView.findViewById(R.id.order_date_text_view);
        orderUpdateTextView = rootView.findViewById(R.id.order_update_text_view);
        orderServerTextView = rootView.findViewById(R.id.order_server_text_view);
        orderSessionTextView = rootView.findViewById(R.id.order_session_text_view);
        orderItemsRecyclerView = rootView.findViewById(R.id.order_items_recycler_view);
        cancelOrderButton = rootView.findViewById(R.id.cancel_order_button);
        paymentButton = rootView.findViewById(R.id.payment_button);
        addItemFab = rootView.findViewById(R.id.add_item_fab);

        // Set up RecyclerView
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public void displayOrderDetails(Order order, String updatedAt) {
        if (order == null) return;

        updateActionButtons(order);

        orderNumberTextView.setText(context.getString(R.string.order_number_format, order.getOrderNumber()));
        tableNumberTextView.setText(context.getString(R.string.table_number_format, order.getTableNumber()));

        if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
            customerNameTextView.setText(context.getString(R.string.customer_name_format, order.getCustomerName()));
            customerNameTextView.setVisibility(View.VISIBLE);
        } else {
            customerNameTextView.setVisibility(View.GONE);
        }

        String formattedStatus = order.getFormattedStatus();
        String statusText;
        if (order.getOrderTypeName() != null && !order.getOrderTypeName().isEmpty()) {
            statusText = context.getString(R.string.order_status_format, formattedStatus) +
                    " • " + order.getOrderTypeName();
        } else {
            statusText = context.getString(R.string.order_status_format, formattedStatus);
        }
        orderStatusTextView.setText(statusText);

        String formattedTotal = formatPriceWithCurrency(order.getTotalAmount());
        orderTotalTextView.setText(context.getString(R.string.order_total_format, formattedTotal));

        String formattedCreatedDate = formatAPIDate(order.getCreatedAt());
        orderDateTextView.setText(context.getString(R.string.order_date_format, formattedCreatedDate));

        String formattedUpdatedDate = formatAPIDate(updatedAt);
        try {
            orderUpdateTextView.setText(context.getString(R.string.order_updated_format, formattedUpdatedDate));
        } catch (Exception e) {
            // Fallback if string resource doesn't exist
            orderUpdateTextView.setText("Updated: " + formattedUpdatedDate);
        }

        try {
            orderServerTextView.setText(context.getString(R.string.order_server_format, order.getServerId()));
        } catch (Exception e) {
            // Fallback if string resource doesn't exist
            orderServerTextView.setText("Server: #" + order.getServerId());
        }

        try {
            orderSessionTextView.setText(context.getString(R.string.order_session_format, order.getSessionId()));
        } catch (Exception e) {
            // Fallback if string resource doesn't exist
            orderSessionTextView.setText("Session: #" + order.getSessionId());
        }

        List<OrderItem> orderItems = order.getItems();
        if (orderItems != null && !orderItems.isEmpty()) {
            OrderItemAdapter adapter = new OrderItemAdapter(orderItems, context);
            orderItemsRecyclerView.setAdapter(adapter);
        }
    }

    public void updateActionButtons(Order order) {
        boolean isOrderOpen = !"closed".equalsIgnoreCase(order.getStatus()) &&
                !"cancelled".equalsIgnoreCase(order.getStatus());

        // Update FAB visibility and state
        if (isOrderOpen) {
            addItemFab.show();
        } else {
            addItemFab.hide();
        }

        // Update regular buttons
        cancelOrderButton.setEnabled(isOrderOpen);
        cancelOrderButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);

        paymentButton.setEnabled(isOrderOpen);
        paymentButton.setAlpha(isOrderOpen ? 1.0f : 0.5f);
    }

    public void setClickListeners(View.OnClickListener addItemListener,
                                  View.OnClickListener paymentListener,
                                  View.OnClickListener cancelListener) {
        if (addItemFab != null) addItemFab.setOnClickListener(addItemListener);
        if (paymentButton != null) paymentButton.setOnClickListener(paymentListener);
        if (cancelOrderButton != null) cancelOrderButton.setOnClickListener(cancelListener);
    }

    private String formatAPIDate(String apiDateStr) {
        if (apiDateStr == null || apiDateStr.isEmpty()) {
            try {
                return context.getString(R.string.not_available);
            } catch (Exception e) {
                return "N/A"; // Fallback if string resource doesn't exist
            }
        }

        try {
            Date date = apiDateFormat.parse(apiDateStr);
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    date.getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);

            return displayDateFormat.format(date) + " (" + timeAgo + ")";
        } catch (ParseException e) {
            return apiDateStr;
        }
    }

    private String formatPriceWithCurrency(double price) {
        long roundedPrice = Math.round(price);
        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        String currencyPrefix = context.getString(R.string.currency_prefix);
        return context.getString(R.string.currency_format_pattern, currencyPrefix, formattedPrice.toString());
    }
}