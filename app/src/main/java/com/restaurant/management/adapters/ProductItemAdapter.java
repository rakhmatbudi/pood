package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.utils.PriceFormatter;

import java.util.List;

public class ProductItemAdapter extends RecyclerView.Adapter<ProductItemAdapter.ViewHolder> {

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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductItem menuItem = menuItems.get(position);
        holder.bind(menuItem, listener);
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView; // We'll hide this
        private TextView priceTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Using the exact IDs from your layout
            nameTextView = itemView.findViewById(R.id.menu_item_name);
            descriptionTextView = itemView.findViewById(R.id.menu_item_description);
            priceTextView = itemView.findViewById(R.id.menu_item_price);
        }

        public void bind(final ProductItem menuItem, final OnItemClickListener listener) {
            nameTextView.setText(menuItem.getName());

            // Hide the description view to effectively remove the category display
            descriptionTextView.setVisibility(View.GONE);

            // Format and set the price
            String formattedPrice = PriceFormatter.format(
                    menuItem.getPrice(),
                    itemView.getContext().getString(R.string.currency_prefix)
            );
            priceTextView.setText(formattedPrice);

            // Set click listener on the whole item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(menuItem);
                }
            });
        }
    }
}