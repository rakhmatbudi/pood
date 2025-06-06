package com.restaurant.management.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        holder.bind(item, context);

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

        private boolean hasValidNotes(OrderItem item) {
            String notes = item.getNotes();

            if (notes == null) {
                return false;
            }

            notes = notes.trim();

            if (notes.isEmpty()) {
                return false;
            }

            if ("null".equalsIgnoreCase(notes)) {
                return false;
            }

            return true;
        }

        void bind(OrderItem item, Context context) {
            // Check if item is cancelled
            boolean isCancelled = item.getStatus() != null &&
                    item.getStatus().equalsIgnoreCase("cancelled");

            if (itemNameTextView != null) {
                itemNameTextView.setText(item.getMenuItemName());
                applyStrikethroughEffect(itemNameTextView, isCancelled, context);
            }

            if (itemVariantTextView != null) {
                if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
                    itemVariantTextView.setText(item.getVariantName());
                    itemVariantTextView.setVisibility(View.VISIBLE);
                    applyStrikethroughEffect(itemVariantTextView, isCancelled, context);
                } else {
                    itemVariantTextView.setVisibility(View.GONE);
                }
            }

            if (itemQuantityTextView != null) {
                itemQuantityTextView.setText(String.valueOf(item.getQuantity()));
                applyStrikethroughEffect(itemQuantityTextView, isCancelled, context);
            }

            if (itemPriceTextView != null) {
                itemPriceTextView.setText(formatPrice(item.getTotalPrice()));
                applyStrikethroughEffect(itemPriceTextView, isCancelled, context);
            }

            if (itemNotesTextView != null) {
                if (hasValidNotes(item)) {
                    itemNotesTextView.setText(item.getNotes());
                    itemNotesTextView.setVisibility(View.VISIBLE);
                    applyStrikethroughEffect(itemNotesTextView, isCancelled, context);
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

                // Don't apply strikethrough to status text, but make it more prominent for cancelled items
                if (isCancelled) {
                    itemStatusTextView.setTextColor(0xFFE53935); // Red for cancelled
                }
            }

            // Apply overall styling to the entire item view
            if (isCancelled) {
                itemView.setAlpha(0.6f); // Make entire item semi-transparent
            } else {
                itemView.setAlpha(1.0f); // Full opacity for active items
            }
        }

        /**
         * Apply or remove strikethrough effect and color changes to a TextView
         */
        private void applyStrikethroughEffect(TextView textView, boolean isCancelled, Context context) {
            if (isCancelled) {
                // Apply strikethrough
                textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                // Make text gray
                if (context != null) {
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
                } else {
                    textView.setTextColor(0xFF757575); // Fallback gray color
                }
            } else {
                // Remove strikethrough
                textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

                // Restore normal text color
                if (context != null) {
                    textView.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                } else {
                    textView.setTextColor(0xFF000000); // Fallback black color
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