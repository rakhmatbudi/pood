package com.restaurant.management.models;

import java.util.List;

public class Promo {
    private long promoId;
    private String promoName;
    private String promoDescription;
    private String startDate;
    private String endDate;
    private String termAndCondition;
    private String picture;
    private String type;
    private String discountType;
    private String discountAmount;
    private boolean isActive;
    private List<PromoItem> promoItems;

    public static class PromoItem {
        private long id;
        private long itemId;
        private String itemName;

        public PromoItem(long id, long itemId, String itemName) {
            this.id = id;
            this.itemId = itemId;
            this.itemName = itemName;
        }

        // Getters
        public long getId() { return id; }
        public long getItemId() { return itemId; }
        public String getItemName() { return itemName; }

        // Setters
        public void setId(long id) { this.id = id; }
        public void setItemId(long itemId) { this.itemId = itemId; }
        public void setItemName(String itemName) { this.itemName = itemName; }
    }

    public Promo() {
    }

    public Promo(long promoId, String promoName, String promoDescription, String startDate,
                 String endDate, String termAndCondition, String picture, String type,
                 String discountType, String discountAmount, boolean isActive, List<PromoItem> promoItems) {
        this.promoId = promoId;
        this.promoName = promoName;
        this.promoDescription = promoDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.termAndCondition = termAndCondition;
        this.picture = picture;
        this.type = type;
        this.discountType = discountType;
        this.discountAmount = discountAmount;
        this.isActive = isActive;
        this.promoItems = promoItems;
    }

    // Getters
    public long getPromoId() {
        return promoId;
    }

    public String getPromoName() {
        return promoName;
    }

    public String getPromoDescription() {
        return promoDescription;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getTermAndCondition() {
        return termAndCondition;
    }

    public String getPicture() {
        return picture;
    }

    public String getType() {
        return type;
    }

    public String getDiscountType() {
        return discountType;
    }

    public String getDiscountAmount() {
        return discountAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<PromoItem> getPromoItems() {
        return promoItems;
    }

    // Setters
    public void setPromoId(long promoId) {
        this.promoId = promoId;
    }

    public void setPromoName(String promoName) {
        this.promoName = promoName;
    }

    public void setPromoDescription(String promoDescription) {
        this.promoDescription = promoDescription;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setTermAndCondition(String termAndCondition) {
        this.termAndCondition = termAndCondition;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public void setDiscountAmount(String discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setPromoItems(List<PromoItem> promoItems) {
        this.promoItems = promoItems;
    }

    /**
     * Get formatted discount display text
     */
    public String getFormattedDiscount() {
        if (discountAmount != null && !discountAmount.isEmpty()) {
            if ("percentage".equalsIgnoreCase(discountType)) {
                return discountAmount + "% OFF";
            } else if ("Fixed".equalsIgnoreCase(discountType)) {
                return "$" + discountAmount + " OFF";
            }
        }

        if ("Bundle".equalsIgnoreCase(type)) {
            return "Bundle Deal";
        }

        return "Special Offer";
    }

    /**
     * Check if promo has an image
     */
    public boolean hasImage() {
        return picture != null && !picture.trim().isEmpty();
    }

    /**
     * Get display name for UI
     */
    public String getDisplayName() {
        return promoName != null ? promoName.replace("_", " ").toUpperCase() : "";
    }

    /**
     * Check if promo has items
     */
    public boolean hasItems() {
        return promoItems != null && !promoItems.isEmpty();
    }

    /**
     * Get items count
     */
    public int getItemsCount() {
        return promoItems != null ? promoItems.size() : 0;
    }

    @Override
    public String toString() {
        return "Promo{" +
                "promoId=" + promoId +
                ", promoName='" + promoName + '\'' +
                ", promoDescription='" + promoDescription + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", type='" + type + '\'' +
                ", discountType='" + discountType + '\'' +
                ", discountAmount='" + discountAmount + '\'' +
                ", isActive=" + isActive +
                ", picture='" + picture + '\'' +
                '}';
    }
}