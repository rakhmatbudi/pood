package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.OfflineDataItem;

import java.util.List;

public class OfflineDataAdapter extends RecyclerView.Adapter<OfflineDataAdapter.ViewHolder> {

    private List<OfflineDataItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(OfflineDataItem item);
    }

    public OfflineDataAdapter(List<OfflineDataItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.offline_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OfflineDataItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvDescription;
        private TextView tvCount;
        private ImageView ivIcon;
        private View itemView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCount = itemView.findViewById(R.id.tvCount);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }

        void bind(OfflineDataItem item, OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvDescription.setText(item.getDescription());
            tvCount.setText(String.valueOf(item.getCount()));

            // Set icon and background color based on type
            setIconForType(item.getType());

            // Set background color for count badge based on type
            setCountBadgeColor(item.getType());

            // Set background color based on type
            setBackgroundForType(item.getType());

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private void setCountBadgeColor(String type) {
            // Find the FrameLayout parent of tvCount
            android.view.View parent = (android.view.View) tvCount.getParent().getParent();
            if (parent instanceof android.widget.FrameLayout) {
                int badgeColor;
                switch (type) {
                    case "orders":
                        badgeColor = 0xFFFFCDD2; // Lighter red
                        break;
                    case "categories":
                        badgeColor = 0xFFE3F2FD; // Light blue
                        break;
                    case "menu_items":
                        badgeColor = 0xFFC8E6C9; // Light green
                        break;
                    case "promos":
                        badgeColor = 0xFFFFE0B2; // Light orange
                        break;
                    case "order_types":
                        badgeColor = 0xFFE1BEE7; // Light purple
                        break;
                    case "order_statuses":
                        badgeColor = 0xFFB2DFDB; // Light teal
                        break;
                    default:
                        badgeColor = 0xFFE3F2FD; // Default blue
                        break;
                }
                ((android.widget.FrameLayout) parent).setBackgroundColor(badgeColor);
            }
        }

        private void setIconForType(String type) {
            int iconRes;
            int backgroundColor;

            switch (type) {
                case "orders":
                    iconRes = android.R.drawable.ic_menu_recent_history;
                    backgroundColor = 0xFFFFEBEE; // Light red
                    break;
                case "categories":
                    iconRes = android.R.drawable.ic_menu_sort_by_size;
                    backgroundColor = 0xFFE3F2FD; // Light blue
                    break;
                case "menu_items":
                    iconRes = android.R.drawable.ic_menu_agenda;
                    backgroundColor = 0xFFE8F5E8; // Light green
                    break;
                case "promos":
                    iconRes = android.R.drawable.ic_menu_today;
                    backgroundColor = 0xFFFFF3E0; // Light orange
                    break;
                case "order_types":
                    iconRes = android.R.drawable.ic_menu_preferences;
                    backgroundColor = 0xFFF3E5F5; // Light purple
                    break;
                case "order_statuses":
                    iconRes = android.R.drawable.ic_menu_info_details;
                    backgroundColor = 0xFFE0F2F1; // Light teal
                    break;
                default:
                    iconRes = android.R.drawable.ic_menu_manage;
                    backgroundColor = 0xFFE0E0E0; // Light gray
                    break;
            }

            ivIcon.setImageResource(iconRes);
            ivIcon.getParent(); // Get the FrameLayout parent
            if (ivIcon.getParent() instanceof android.widget.FrameLayout) {
                ((android.widget.FrameLayout) ivIcon.getParent()).setBackgroundColor(backgroundColor);
            }
        }

        private void setBackgroundForType(String type) {
            int backgroundColor;
            int textColor;

            switch (type) {
                case "orders":
                    // Red tint for unsynced orders (important)
                    backgroundColor = 0xFFFFEBEE; // Light red
                    textColor = 0xFFD32F2F; // Dark red
                    break;
                case "categories":
                    backgroundColor = 0xFFE3F2FD; // Light blue
                    textColor = 0xFF1976D2; // Dark blue
                    break;
                case "menu_items":
                    backgroundColor = 0xFFE8F5E8; // Light green
                    textColor = 0xFF388E3C; // Dark green
                    break;
                case "promos":
                    backgroundColor = 0xFFFFF3E0; // Light orange
                    textColor = 0xFFF57C00; // Dark orange
                    break;
                case "order_types":
                    backgroundColor = 0xFFF3E5F5; // Light purple
                    textColor = 0xFF7B1FA2; // Dark purple
                    break;
                case "order_statuses":
                    backgroundColor = 0xFFE0F2F1; // Light teal
                    textColor = 0xFF00796B; // Dark teal
                    break;
                default:
                    backgroundColor = 0xFFF5F5F5; // Light gray
                    textColor = 0xFF424242; // Dark gray
                    break;
            }

            itemView.setBackgroundColor(backgroundColor);
            tvTitle.setTextColor(textColor);
            tvCount.setTextColor(textColor);
        }
    }
}