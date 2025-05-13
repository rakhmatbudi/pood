package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.ProductItem;

import java.util.List;

public class ProductItemAdapter extends RecyclerView.Adapter<ProductItemAdapter.MenuItemViewHolder> {

    private static final String TAG = "MenuItemAdapter";
    private List<ProductItem> menuItems;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ProductItem menuItem);
    }

    public ProductItemAdapter(List<ProductItem> menuItems, OnItemClickListener listener) {
        this.menuItems = menuItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MenuItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new MenuItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MenuItemViewHolder holder, int position) {
        ProductItem menuItem = menuItems.get(position);
        holder.bind(menuItem, listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class MenuItemViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView;
        private TextView categoryTextView;
        private TextView priceTextView;

        public MenuItemViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.menu_item_name);
            descriptionTextView = itemView.findViewById(R.id.menu_item_description);
            categoryTextView = itemView.findViewById(R.id.menu_item_category);
            priceTextView = itemView.findViewById(R.id.menu_item_price);
        }

        public void bind(final ProductItem menuItem, final OnItemClickListener listener) {
            nameTextView.setText(menuItem.getName());

            // Set description if available
            if (menuItem.getDescription() != null && !menuItem.getDescription().isEmpty()) {
                descriptionTextView.setText(menuItem.getDescription());
                descriptionTextView.setVisibility(View.VISIBLE);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }

            // Set category if available
            if (menuItem.getCategory() != null && !menuItem.getCategory().isEmpty()) {
                categoryTextView.setText(menuItem.getCategory());
                categoryTextView.setVisibility(View.VISIBLE);
            } else {
                categoryTextView.setVisibility(View.GONE);
            }

            // Format price with currency
            String formattedPrice = formatPrice(menuItem.getPrice(), itemView.getContext().getString(R.string.currency_prefix));
            priceTextView.setText(formattedPrice);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(menuItem);
                }
            });
        }

        private String formatPrice(double price, String currencyPrefix) {
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

            // Format according to the pattern (allows for different currency placement)
            return String.format(itemView.getContext().getString(R.string.currency_format_pattern),
                    currencyPrefix, formattedPrice.toString());
        }
    }
}