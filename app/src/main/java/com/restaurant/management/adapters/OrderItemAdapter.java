package com.restaurant.management.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.CancelOrderItemActivity;
import com.restaurant.management.R;
import com.restaurant.management.models.OrderItem;

import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {
    private List<OrderItem> orderItems;
    private Context context;

    public OrderItemAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public OrderItemAdapter(List<OrderItem> orderItems, Context context) {
        this.orderItems = orderItems;
        this.context = context;
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_item, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        holder.bind(item);

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CancelOrderItemActivity.class);
            intent.putExtra("order_id", item.getOrderId());
            intent.putExtra("item_id", item.getId());
            intent.putExtra("item_name", item.getMenuItemName());
            intent.putExtra("item_variant", item.getVariantName());
            intent.putExtra("quantity", item.getQuantity());
            intent.putExtra("unit_price", item.getUnitPrice());
            intent.putExtra("total_price", item.getTotalPrice());
            intent.putExtra("notes", item.getNotes());
            intent.putExtra("status", item.getStatus());

            // Start activity for result if context is an Activity
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).startActivityForResult(intent, 200);
            } else {
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public void updateItems(List<OrderItem> newItems) {
        this.orderItems = newItems;
        notifyDataSetChanged();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private TextView itemNameTextView;
        private TextView itemVariantTextView;
        private TextView itemQuantityTextView;
        private TextView itemPriceTextView;
        private TextView itemNotesTextView;
        private TextView itemStatusTextView;

        OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.item_name_text_view);
            itemVariantTextView = itemView.findViewById(R.id.item_variant_text_view);
            itemQuantityTextView = itemView.findViewById(R.id.item_quantity_text_view);
            itemPriceTextView = itemView.findViewById(R.id.item_price_text_view);
            itemNotesTextView = itemView.findViewById(R.id.item_notes_text_view);
            itemStatusTextView = itemView.findViewById(R.id.item_status_text_view);
        }

        void bind(OrderItem item) {
            if (itemNameTextView != null) {
                itemNameTextView.setText(item.getMenuItemName());
            }

            if (itemVariantTextView != null) {
                if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
                    itemVariantTextView.setText(item.getVariantName());
                    itemVariantTextView.setVisibility(View.VISIBLE);
                } else {
                    itemVariantTextView.setVisibility(View.GONE);
                }
            }

            if (itemQuantityTextView != null) {
                itemQuantityTextView.setText(String.valueOf(item.getQuantity()));
            }

            if (itemPriceTextView != null) {
                itemPriceTextView.setText(formatPrice(item.getTotalPrice()));
            }

            if (itemNotesTextView != null) {
                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    itemNotesTextView.setText(item.getNotes());
                    itemNotesTextView.setVisibility(View.VISIBLE);
                } else {
                    itemNotesTextView.setVisibility(View.GONE);
                }
            }

            if (itemStatusTextView != null) {
                String status = item.getStatus();
                if (status != null) {
                    itemStatusTextView.setText(formatStatus(status));

                    // Set different colors based on status
                    switch (status.toLowerCase()) {
                        case "cancelled":
                            itemStatusTextView.setTextColor(0xFFE53935); // Red
                            break;
                        case "preparing":
                            itemStatusTextView.setTextColor(0xFFFB8C00); // Orange
                            break;
                        case "ready":
                            itemStatusTextView.setTextColor(0xFF43A047); // Green
                            break;
                        default:
                            itemStatusTextView.setTextColor(0xFF757575); // Gray
                            break;
                    }
                } else {
                    itemStatusTextView.setText("New");
                    itemStatusTextView.setTextColor(0xFF757575);
                }
            }
        }

        private String formatStatus(String status) {
            if (status == null || status.isEmpty()) {
                return "New";
            }
            return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }

        private String formatPrice(double price) {
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

            return "Rp " + formattedPrice.toString();
        }
    }
}