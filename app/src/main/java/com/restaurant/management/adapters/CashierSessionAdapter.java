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

    private List<CashierSession> sessions;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public CashierSessionAdapter(List<CashierSession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cashier_session, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        CashierSession session = sessions.get(position);

        // Set session ID and cashier name
        holder.sessionIdTextView.setText(String.valueOf(session.getId()));
        holder.cashierNameTextView.setText(session.getUserName());

        // Format and set dates
        String startTime = session.getStartTime() != null
                ? dateFormat.format(session.getStartTime())
                : "N/A";
        holder.startTimeTextView.setText(startTime);

        String endTime = session.getEndTime() != null
                ? dateFormat.format(session.getEndTime())
                : "N/A";
        holder.endTimeTextView.setText(endTime);

        // Set amounts
        holder.startAmountTextView.setText(String.format(Locale.getDefault(), "$%.2f", session.getStartAmount()));
        holder.endAmountTextView.setText(String.format(Locale.getDefault(), "$%.2f", session.getEndAmount()));
        holder.totalTextView.setText(String.format(Locale.getDefault(), "$%.2f", session.getSessionTotal()));

        // Set status
        holder.statusTextView.setText(session.getStatus());

        // Change status text color based on status
        int statusColor;
        if ("ACTIVE".equalsIgnoreCase(session.getStatus())) {
            statusColor = holder.itemView.getContext().getResources().getColor(R.color.color_sub_header);
        } else if ("CLOSED".equalsIgnoreCase(session.getStatus())) {
            statusColor = holder.itemView.getContext().getResources().getColor(R.color.blue);
        } else {
            statusColor = holder.itemView.getContext().getResources().getColor(R.color.black);
        }
        holder.statusTextView.setTextColor(statusColor);
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    public class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView sessionIdTextView;
        TextView cashierNameTextView;
        TextView startTimeTextView;
        TextView endTimeTextView;
        TextView startAmountTextView;
        TextView endAmountTextView;
        TextView totalTextView;
        TextView statusTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            sessionIdTextView = itemView.findViewById(R.id.session_id_text_view);
            startTimeTextView = itemView.findViewById(R.id.start_time_text_view);
            totalTextView = itemView.findViewById(R.id.total_text_view);
        }
    }
}