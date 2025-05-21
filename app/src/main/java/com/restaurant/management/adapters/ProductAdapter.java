package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<Product> productList;
    private Context context;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
        Log.d("ProductAdapter", "Adapter created with " + (productList != null ? productList.size() : 0) + " items");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            Log.d("ProductAdapter", "Creating header view holder");
            View headerView = LayoutInflater.from(context).inflate(R.layout.item_product_table_header, parent, false);
            return new HeaderViewHolder(headerView);
        } else {
            Log.d("ProductAdapter", "Creating product view holder");
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_product_table, parent, false);
            return new ProductViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            // Header doesn't need data binding
            Log.d("ProductAdapter", "Binding header view");
        } else if (holder instanceof ProductViewHolder) {
            // Adjust position to account for header
            int productPosition = position - 1;
            Product product = productList.get(productPosition);

            Log.d("ProductAdapter", "Binding product at position " + productPosition + ": " + product.getName());

            ProductViewHolder productHolder = (ProductViewHolder) holder;
            productHolder.txtName.setText(product.getName());

            // Set category name
            productHolder.txtCategory.setText(product.getCategoryName());

            // Format price to remove decimal places
            try {
                double priceValue = Double.parseDouble(product.getPrice());
                // Format without decimal places
                String formattedPrice = String.format("%,.0f", priceValue);
                productHolder.txtPrice.setText(formattedPrice);
            } catch (NumberFormatException e) {
                // Fallback if parsing fails
                productHolder.txtPrice.setText("Rp " + product.getPrice());
                Log.e("ProductAdapter", "Error formatting price: " + e.getMessage());
            }

            productHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // Add 1 for the header
        int count = (productList != null ? productList.size() : 0) + 1;
        Log.d("ProductAdapter", "getItemCount: " + count);
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_ITEM;
    }

    public void updateList(List<Product> newList) {
        this.productList.clear();
        this.productList.addAll(newList);
        notifyDataSetChanged();
        //Log.d("ProductAdapter", "notifyDataSetChanged called");
    }

    // Header ViewHolder
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    // Product ViewHolder
    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtCategory, txtPrice;

        ProductViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtCategory = itemView.findViewById(R.id.txtProductCategory);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
        }
    }
}