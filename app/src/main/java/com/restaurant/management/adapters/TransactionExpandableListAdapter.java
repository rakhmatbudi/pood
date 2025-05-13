package com.restaurant.management.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.restaurant.management.R;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.Transaction;
import com.restaurant.management.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<CashierSession> sessionList;
    private Map<CashierSession, List<Transaction>> transactionMap;
    private SimpleDateFormat dateFormat;

    public TransactionExpandableListAdapter(Context context,
                                            List<CashierSession> sessionList,
                                            Map<CashierSession, List<Transaction>> transactionMap) {
        this.context = context;
        this.sessionList = sessionList;
        this.transactionMap = transactionMap;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @Override
    public int getGroupCount() {
        return sessionList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return transactionMap.get(sessionList.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return sessionList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return transactionMap.get(sessionList.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        CashierSession session = (CashierSession) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_cashier_session, parent, false);
        }

        // Find all views from your existing layout
        TextView sessionIdTextView = convertView.findViewById(R.id.session_id_text_view);
        TextView startTimeTextView = convertView.findViewById(R.id.start_time_text_view);
        TextView totalTextView = convertView.findViewById(R.id.total_text_view);

        // Get total transactions amount for this session
        List<Transaction> transactions = transactionMap.get(session);
        double totalAmount = 0;
        for (Transaction transaction : transactions) {
            totalAmount += transaction.getAmount();
        }

        // Set session information
        sessionIdTextView.setText(String.valueOf(session.getId()));

        // Format and set date
        startTimeTextView.setText(session.getStartTime() != null ?
                dateFormat.format(session.getStartTime()) : "N/A");

        // Set the total amount
        totalTextView.setText(formatCurrency(totalAmount));

        return convertView;
    }
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Transaction transaction = (Transaction) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.item_transaction, parent, false);
        }

        TextView tvOrderInfo = convertView.findViewById(R.id.tvOrderInfo);
        TextView tvAmount = convertView.findViewById(R.id.tvAmount);
        TextView tvPaymentMethod = convertView.findViewById(R.id.tvPaymentMethod);
        TextView tvPaymentTime = convertView.findViewById(R.id.tvPaymentTime);
        TextView tvOrderItems = convertView.findViewById(R.id.tvOrderItems);

        // Display order info (Order ID and table number)
        tvOrderInfo.setText(String.format(Locale.getDefault(), "Order #%d • Table %s",
                transaction.getOrderId(), transaction.getTableNumber()));

        // Display amount
        tvAmount.setText(formatCurrency(transaction.getAmount()));

        // Display payment method
        tvPaymentMethod.setText(transaction.getPaymentMethod());

        // Display payment time
        tvPaymentTime.setText(dateFormat.format(transaction.getPaymentDate()));

        // Format and display order items
        String orderItemsText = formatOrderItems(transaction.getOrderItems());
        tvOrderItems.setText(orderItemsText);

        return convertView;
    }

    /**
     * Format order items as a comma-separated list with quantities
     * @param orderItems List of order items
     * @return Formatted string of order items
     */
    private String formatOrderItems(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);

            // Use the actual menu item name from the API
            String itemName = item.getMenuItemName();

            sb.append(item.getQuantity()).append("x ");
            sb.append(itemName);

            if (i < orderItems.size() - 1) {
                sb.append(", ");
            }
        }

        String result = sb.toString();

        // Truncate if longer than 30 characters
        if (result.length() > 35) {
            result = result.substring(0, 32) + "...";
        }

        return result;
    }
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "Rp %,.0f", amount);
    }
}