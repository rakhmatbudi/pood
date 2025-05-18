package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.Tax;

import java.util.List;

public class TaxAdapter extends RecyclerView.Adapter<TaxAdapter.TaxViewHolder> {

    private List<Tax> taxList;

    public TaxAdapter(List<Tax> taxList) {
        this.taxList = taxList;
    }

    @NonNull
    @Override
    public TaxViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tax_item_layout, parent, false);
        return new TaxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaxViewHolder holder, int position) {
        Tax tax = taxList.get(position);
        holder.nameTextView.setText(tax.getName());
        holder.descriptionTextView.setText(tax.getDescription());
        holder.amountTextView.setText(tax.getAmount() + "%");
    }

    @Override
    public int getItemCount() {
        return taxList.size();
    }

    public void updateTaxList(List<Tax> taxList) {
        this.taxList = taxList;
        notifyDataSetChanged();
    }

    static class TaxViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView descriptionTextView;
        TextView amountTextView;

        TaxViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_tax_name);
            descriptionTextView = itemView.findViewById(R.id.text_tax_description);
            amountTextView = itemView.findViewById(R.id.text_tax_amount);
        }
    }
}