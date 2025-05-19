package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.restaurant.management.R;
import com.restaurant.management.models.Discount;

import java.util.List;

public class DiscountSpinnerAdapter extends ArrayAdapter<Discount> {
    private final LayoutInflater inflater;

    public DiscountSpinnerAdapter(@NonNull Context context, @NonNull List<Discount> discounts) {
        super(context, R.layout.item_discount_spinner, discounts);
        this.inflater = LayoutInflater.from(context);

        // Add "No Discount" option as the first item
        discounts.add(0, new Discount(-1, context.getString(R.string.no_discount), "", 0));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_discount_spinner, parent, false);
        }

        Discount discount = getItem(position);
        if (discount != null) {
            TextView nameTextView = view.findViewById(R.id.discount_name);
            TextView amountTextView = view.findViewById(R.id.discount_amount);

            nameTextView.setText(discount.getName());

            if (discount.getId() == -1) {
                // This is the "No Discount" option
                amountTextView.setVisibility(View.GONE);
            } else {
                amountTextView.setVisibility(View.VISIBLE);
                amountTextView.setText(discount.getAmount() + "%");
            }
        }

        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }
}