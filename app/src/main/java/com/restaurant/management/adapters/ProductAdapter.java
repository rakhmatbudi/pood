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

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
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
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        Log.d("ProductAdapter", "Binding product: " + product.getName());
        holder.txtName.setText(product.getName());
        holder.txtDescription.setText(product.getDescription());

        // Handle price as String directly from API
        holder.txtPrice.setText("Rp " + product.getPrice());

        // Or if you prefer to format it:
        // try {
        //     double priceValue = Double.parseDouble(product.getPrice());
        //     // Format without decimal places
        //     String formattedPrice = "Rp " + String.format("%,.0f", priceValue);
        //     holder.txtPrice.setText(formattedPrice);
        // } catch (NumberFormatException e) {
        //     holder.txtPrice.setText("Rp " + product.getPrice());
        // }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void updateList(List<Product> newList) {
        Log.d("ProductAdapter", "updateList called. New list size: " + (newList != null ? newList.size() : "null"));
        this.productList = newList;
        notifyDataSetChanged();
        Log.d("ProductAdapter", "notifyDataSetChanged called");
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtDescription, txtPrice;

        ProductViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtDescription = itemView.findViewById(R.id.txtProductDescription);
            txtPrice = itemView.findViewById(R.id.txtProductPrice);
        }
    }
}