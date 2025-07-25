package com.restaurant.management.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.restaurant.management.R;
import com.restaurant.management.models.Promo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PromoAdapter extends RecyclerView.Adapter<PromoAdapter.PromoViewHolder> {

    private final Context context;
    private final List<Promo> promos;
    private OnPromoClickListener listener;

    public interface OnPromoClickListener {
        void onPromoClick(Promo promo);
    }

    public PromoAdapter(Context context, List<Promo> promos) {
        this.context = context;
        this.promos = promos;
    }

    public void setOnPromoClickListener(OnPromoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PromoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promo_card, parent, false);
        return new PromoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromoViewHolder holder, int position) {
        Promo promo = promos.get(position);
        holder.bind(promo);
    }

    @Override
    public int getItemCount() {
        return promos.size();
    }

    public void updatePromos(List<Promo> newPromos) {
        this.promos.clear();
        this.promos.addAll(newPromos);
        notifyDataSetChanged();
    }

    class PromoViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardView;
        private final ImageView promoImageView;
        private final TextView promoNameTextView;
        private final TextView promoDescriptionTextView;
        private final TextView promoDiscountTextView;
        private final TextView promoValidityTextView;

        public PromoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.promo_card_view);
            promoImageView = itemView.findViewById(R.id.promo_image_view);
            promoNameTextView = itemView.findViewById(R.id.promo_name_text_view);
            promoDescriptionTextView = itemView.findViewById(R.id.promo_description_text_view);
            promoDiscountTextView = itemView.findViewById(R.id.promo_discount_text_view);
            promoValidityTextView = itemView.findViewById(R.id.promo_validity_text_view);
        }

        public void bind(Promo promo) {
            // Use description as title instead of display name
            String description = promo.getPromoDescription();
            if (description != null && !description.trim().isEmpty()) {
                promoNameTextView.setText(description);
                promoDescriptionTextView.setVisibility(View.GONE);
            } else {
                promoNameTextView.setText(promo.getDisplayName());
                promoDescriptionTextView.setVisibility(View.GONE);
            }

            promoDiscountTextView.setText(promo.getFormattedDiscount());

            // Format validity dates
            String validityText = formatValidityDates(promo.getStartDate(), promo.getEndDate());
            promoValidityTextView.setText(validityText);

            // FIXED: Proper image loading
            handlePromoImage(promo);

            // Set different card colors based on promo type with fallback
            try {
                if ("Bundle".equalsIgnoreCase(promo.getType())) {
                    int colorId = getColorSafely(R.color.bundle_promo_bg, 0xFFE8F5E8);
                    cardView.setCardBackgroundColor(colorId);
                } else if ("Discount".equalsIgnoreCase(promo.getType())) {
                    int colorId = getColorSafely(R.color.discount_promo_bg, 0xFFFFF3E0);
                    cardView.setCardBackgroundColor(colorId);
                } else {
                    int colorId = getColorSafely(R.color.default_promo_bg, 0xFFF5F5F5);
                    cardView.setCardBackgroundColor(colorId);
                }
            } catch (Exception e) {
                cardView.setCardBackgroundColor(0xFFFFFFFF);
            }

            // Show inactive state if promo is not active
            if (!promo.isActive()) {
                cardView.setAlpha(0.6f);
                promoDiscountTextView.setText("EXPIRED");
                promoDiscountTextView.setBackgroundColor(0xFF9E9E9E);
            } else {
                cardView.setAlpha(1.0f);
                promoDiscountTextView.setBackgroundColor(0xFFFF4444);
            }

            // Set click listener
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPromoClick(promo);
                }
            });
        }

        private void handlePromoImage(Promo promo) {
            if (promo.hasImage()) {
                String imageUrl = promo.getPicture();

                if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                    // Load image with Glide
                    Glide.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_promo_placeholder) // Show while loading
                            .error(R.drawable.ic_promo_error) // Show if loading fails
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image
                            .centerCrop() // Scale image to fill the ImageView
                            .into(promoImageView);

                    promoImageView.setVisibility(View.VISIBLE);
                } else {
                    // Has image flag but no URL - show placeholder
                    setPlaceholderImage();
                }
            } else {
                // No image - hide the image view
                promoImageView.setVisibility(View.GONE);
            }
        }

        private void setPlaceholderImage() {
            try {
                // Use Glide to load placeholder drawable
                Glide.with(context)
                        .load(R.drawable.ic_promo_placeholder)
                        .into(promoImageView);
                promoImageView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                // Ultimate fallback - hide the image view
                promoImageView.setVisibility(View.GONE);
            }
        }

        private int getColorSafely(int colorResId, int fallbackColor) {
            try {
                return context.getResources().getColor(colorResId, null);
            } catch (Exception e) {
                return fallbackColor;
            }
        }

        private String formatValidityDates(String startDate, String endDate) {
            try {
                SimpleDateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

                if (startDate != null && !startDate.isEmpty()) {
                    Date start = apiFormat.parse(startDate);
                    String formattedStart = displayFormat.format(start);

                    if (endDate != null && !endDate.isEmpty()) {
                        Date end = apiFormat.parse(endDate);
                        String formattedEnd = displayFormat.format(end);
                        return "Valid: " + formattedStart + " - " + formattedEnd;
                    } else {
                        return "Valid from: " + formattedStart;
                    }
                }

                return "Check validity";
            } catch (ParseException e) {
                return "Check validity";
            }
        }
    }
}