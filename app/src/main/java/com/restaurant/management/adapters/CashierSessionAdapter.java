package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.CashierSession;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CashierSessionAdapter extends RecyclerView.Adapter<CashierSessionAdapter.SessionViewHolder> {

    private List<CashierSession> cashierSessions;

    public CashierSessionAdapter(List<CashierSession> cashierSessions) {
        this.cashierSessions = cashierSessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cashier_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        CashierSession session = cashierSessions.get(position);

        // Corrected: Use getSessionId() and match the XML ID: session_id_text_view
        holder.sessionIdTextView.setText(session.getSessionId() != null ? String.valueOf(session.getSessionId()) : "N/A");

        // Display start time using getOpenedAt() from your CashierSession model
        // and match the XML ID: start_time_text_view
        if (session.getOpenedAt() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.startTimeTextView.setText(dateFormat.format(session.getOpenedAt()));
        } else {
            holder.startTimeTextView.setText("N/A");
        }

        // Display total (using closing_amount if available, otherwise just opening_amount for open sessions)
        // and match the XML ID: total_text_view
        String totalAmount;
        if (session.getClosedAt() != null && session.getClosingAmount() != null) {
            // If closed, show the final closing amount
            totalAmount = String.format(Locale.getDefault(), "Rp %.0f", Double.parseDouble(session.getClosingAmount()));
        } else if (session.getOpeningAmount() != null) {
            // If open, show the opening amount (or just indicate open)
            totalAmount = String.format(Locale.getDefault(), "Rp %.0f", Double.parseDouble(session.getOpeningAmount()));
        } else {
            totalAmount = "Rp 0";
        }
        holder.totalTextView.setText(totalAmount);


        // --- IMPORTANT NOTE ---
        // Your `item_cashier_session.xml` *only* has TextViews for:
        // - session_id_text_view
        // - start_time_text_view
        // - total_text_view
        //
        // If you intended to display "cashier name", "opening amount", "closing time", or a calculated "session total" (end-start),
        // you MUST add corresponding TextViews with unique IDs into your `item_cashier_session.xml` layout.
        // As your XML stands, these are not available:
        // holder.cashierNameTextView.setText(...);
        // holder.openingAmountTextView.setText(...);
        // holder.closingTimeTextView.setText(...);
        // holder.sessionTotalTextView.setText(...);
    }

    @Override
    public int getItemCount() {
        return cashierSessions.size();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView sessionIdTextView;
        TextView startTimeTextView; // Corresponds to start_time_text_view in XML
        TextView totalTextView;     // Corresponds to total_text_view in XML

        // These TextViews are NOT in your provided item_cashier_session.xml.
        // If you want to use them, uncomment them here AND add them to your XML layout file
        // with the corresponding IDs (e.g., @+id/text_cashier_name).
        // TextView cashierNameTextView;
        // TextView openingAmountTextView;
        // TextView closingTimeTextView;
        // TextView sessionTotalTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            // CORRECTED FINDVIEWBYID CALLS TO MATCH YOUR item_cashier_session.xml
            sessionIdTextView = itemView.findViewById(R.id.session_id_text_view);
            startTimeTextView = itemView.findViewById(R.id.start_time_text_view);
            totalTextView = itemView.findViewById(R.id.total_text_view);

            // Uncomment these only if you add these IDs to your item_cashier_session.xml:
            // cashierNameTextView = itemView.findViewById(R.id.text_cashier_name);
            // openingAmountTextView = itemView.findViewById(R.id.text_opening_amount);
            // closingTimeTextView = itemView.findViewById(R.id.text_closing_time);
            // sessionTotalTextView = itemView.findViewById(R.id.text_session_total);
        }
    }
}