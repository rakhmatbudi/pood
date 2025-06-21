package com.restaurant.management.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.restaurant.management.R;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.Transaction;
import com.restaurant.management.models.OrderItem;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<CashierSession> sessionList;
    private Map<CashierSession, List<Transaction>> transactionMap;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat timeOnlyFormat; // Add a new format for time only
    private OnPrintClickListener printClickListener;

    // Interface for print button clicks
    public interface OnPrintClickListener {
        void onPrintClick(Transaction transaction);
    }

    // Updated constructor to include print click listener
    public TransactionExpandableListAdapter(Context context,
                                            List<CashierSession> sessionList,
                                            Map<CashierSession, List<Transaction>> transactionMap,
                                            OnPrintClickListener printClickListener) {
        this.context = context;
        this.sessionList = sessionList;
        this.transactionMap = transactionMap;
        this.printClickListener = printClickListener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        this.timeOnlyFormat = new SimpleDateFormat("HH:mm", Locale.getDefault()); // Format for time only
    }

    // Keep existing constructor for backward compatibility
    public TransactionExpandableListAdapter(Context context,
                                            List<CashierSession> sessionList,
                                            Map<CashierSession, List<Transaction>> transactionMap) {
        this(context, sessionList, transactionMap, null);
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
        TextView startTimeTextView = convertView.findViewById(R.id.start_time_text_view); // This ID is now present in XML
        TextView totalTextView = convertView.findViewById(R.id.total_text_view);

        // Get total transactions amount for this session
        List<Transaction> transactions = transactionMap.get(session);
        double totalAmount = 0;
        if (transactions != null) { // Add null check for transactions list
            for (Transaction transaction : transactions) {
                totalAmount += transaction.getAmount();
            }
        }


        // Set session information
        // Corrected: Use getSessionId() instead of getId()
        sessionIdTextView.setText(session.getSessionId() != null ? String.valueOf(session.getSessionId()) : "N/A");

        // Format and set date using 'openedAt' from the CashierSession model
        startTimeTextView.setText(session.getOpenedAt() != null ?
                timeFormat.format(session.getOpenedAt()) : "N/A");

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

        // Try to find print button - it might not exist in your current layout
        ImageButton printButton = convertView.findViewById(R.id.printButton);

        // Update this line to include customer name if available
        String orderInfo;
        if (transaction.getCustomerName() != null && !transaction.getCustomerName().isEmpty()) {
            orderInfo = String.format(Locale.getDefault(), "#%d • Tbl %s • %s",
                    transaction.getOrderId(), transaction.getTableNumber(), transaction.getCustomerName());
        } else {
            orderInfo = String.format(Locale.getDefault(), "#%d • Tbl %s",
                    transaction.getOrderId(), transaction.getTableNumber());
        }
        tvOrderInfo.setText(orderInfo);

        tvAmount.setText(formatCurrency(transaction.getAmount()));
        tvPaymentMethod.setText(transaction.getPaymentMethod());

        // Use the time-only format for payment time
        tvPaymentTime.setText(timeOnlyFormat.format(transaction.getPaymentDate()));

        // Format and display order items
        String formattedItems = formatOrderItems(transaction.getOrderItems());
        tvOrderItems.setText(formattedItems);

        // Set up print button if it exists and listener is available
        if (printButton != null && printClickListener != null) {
            printButton.setVisibility(View.VISIBLE);
            printButton.setOnClickListener(v -> printClickListener.onPrintClick(transaction));
        } else if (printButton != null) {
            // Hide print button if no listener is set
            printButton.setVisibility(View.GONE);
        }

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

        // Add more debugging
        Log.d("TransactionAdapter", "Formatting " + orderItems.size() + " order items");

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);

            // Log each item being formatted
            Log.d("TransactionAdapter", "Item " + i + ": ID=" + item.getMenuItemId() +
                    ", Name=" + item.getMenuItemName() + ", Qty=" + item.getQuantity());

            // Use the actual menu item name
            sb.append(item.getQuantity()).append("x ");
            sb.append(item.getMenuItemName());

            if (i < orderItems.size() - 1) {
                sb.append(", ");
            }
        }

        String result = sb.toString();

        // For debugging, log the final formatted string
        Log.d("TransactionAdapter", "Final formatted string: " + result);

        // Truncate if longer than 30 characters
        if (result.length() > 40) {
            result = result.substring(0, 37) + "...";
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