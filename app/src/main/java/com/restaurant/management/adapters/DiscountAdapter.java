package com.restaurant.management.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.R;
import com.restaurant.management.models.Discount;

import java.util.List;

public class DiscountAdapter extends RecyclerView.Adapter<DiscountAdapter.DiscountViewHolder> {

    private List<Discount> discounts;
    private DiscountClickListener listener;

    public interface DiscountClickListener {
        void onDiscountClick(Discount discount);
    }

    public DiscountAdapter(List<Discount> discounts, DiscountClickListener listener) {
        this.discounts = discounts;
        this.listener = listener;
    }

    public void updateDiscounts(List<Discount> newDiscounts) {
        this.discounts = newDiscounts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiscountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discount, parent, false);
        return new DiscountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiscountViewHolder holder, int position) {
        Discount discount = discounts.get(position);
        holder.bind(discount);
    }

    @Override
    public int getItemCount() {
        return discounts != null ? discounts.size() : 0;
    }

    class DiscountViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView descriptionTextView;
        private TextView amountTextView;

        public DiscountViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.discount_name_text_view);
            descriptionTextView = itemView.findViewById(R.id.discount_description_text_view);
            amountTextView = itemView.findViewById(R.id.discount_amount_text_view);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDiscountClick(discounts.get(position));
                }
            });
        }

        public void bind(Discount discount) {
            nameTextView.setText(itemView.getContext().getString(R.string.discount_name, discount.getName()));
            descriptionTextView.setText(itemView.getContext().getString(R.string.discount_description, discount.getDescription()));
            amountTextView.setText(itemView.getContext().getString(R.string.discount_amount, discount.getAmount()));
        }
    }
}