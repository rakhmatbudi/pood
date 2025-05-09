package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.SessionItem;

import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private final List<SessionItem> sessionList;

    public SessionAdapter(List<SessionItem> sessionList) {
        this.sessionList = sessionList;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        SessionItem session = sessionList.get(position);

        holder.sessionIdTextView.setText(String.format("Session #%d", session.getId()));
        holder.openedAtTextView.setText(String.format("Opened: %s", session.getOpenedAt()));

        // Set closed time or show "Active" for ongoing sessions
        if ("Active".equals(session.getStatus())) {
            holder.closedAtTextView.setText("Status: Active");
            holder.closedAtTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
        } else {
            holder.closedAtTextView.setText(String.format("Closed: %s", session.getClosedAt()));
            holder.closedAtTextView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
        }

        // Set amounts
        holder.openingAmountTextView.setText(String.format("Opening: %s", formatAmount(session.getOpeningAmount())));

        if ("-".equals(session.getClosingAmount())) {
            holder.closingAmountTextView.setVisibility(View.GONE);
        } else {
            holder.closingAmountTextView.setVisibility(View.VISIBLE);
            holder.closingAmountTextView.setText(String.format("Closing: %s", formatAmount(session.getClosingAmount())));
        }
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    /**
     * Format amount for display
     */
    private String formatAmount(String amount) {
        try {
            // Try to parse as double for better formatting if needed
            double amountValue = Double.parseDouble(amount);
            return String.format("%,.0f", amountValue);
        } catch (NumberFormatException e) {
            // If parsing fails, return original string
            return amount;
        }
    }

    /**
     * ViewHolder class for session items
     */
    static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView sessionIdTextView;
        TextView openedAtTextView;
        TextView closedAtTextView;
        TextView openingAmountTextView;
        TextView closingAmountTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionIdTextView = itemView.findViewById(R.id.session_id_text_view);
            openedAtTextView = itemView.findViewById(R.id.opened_at_text_view);
            closedAtTextView = itemView.findViewById(R.id.closed_at_text_view);
            openingAmountTextView = itemView.findViewById(R.id.opening_amount_text_view);
            closingAmountTextView = itemView.findViewById(R.id.closing_amount_text_view);
        }
    }
}