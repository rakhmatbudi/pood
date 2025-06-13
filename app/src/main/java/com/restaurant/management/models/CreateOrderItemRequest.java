package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class CreateOrderItemRequest {
    @SerializedName("menu_item_id")
    private long menuItemId;

    @SerializedName("variant_id")
    private Long variantId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("unit_price")
    private double unitPrice;

    @SerializedName("total_price")
    private double totalPrice;

    @SerializedName("status")
    private String status;

    @SerializedName("kitchen_printed")
    private boolean kitchenPrinted;

    @SerializedName("notes")
    private String notes;

    // Optional fields for custom functionality
    @SerializedName("is_complimentary")
    private Boolean isComplimentary;

    @SerializedName("is_custom_price")
    private Boolean isCustomPrice;

    @SerializedName("original_price")
    private Double originalPrice;

    // Default constructor
    public CreateOrderItemRequest() {}

    // Getters
    public long getMenuItemId() {
        return menuItemId;
    }

    public Long getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public boolean isKitchenPrinted() {
        return kitchenPrinted;
    }

    public String getNotes() {
        return notes;
    }

    public boolean isComplimentary() {
        return isComplimentary != null ? isComplimentary : false;
    }

    public boolean isCustomPrice() {
        return isCustomPrice != null ? isCustomPrice : false;
    }

    public Double getOriginalPrice() {
        return originalPrice;
    }

    // Setters
    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setKitchenPrinted(boolean kitchenPrinted) {
        this.kitchenPrinted = kitchenPrinted;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setComplimentary(boolean complimentary) {
        this.isComplimentary = complimentary ? true : null;
    }

    public void setCustomPrice(boolean customPrice) {
        this.isCustomPrice = customPrice ? true : null;
    }

    public void setOriginalPrice(Double originalPrice) {
        this.originalPrice = originalPrice;
    }
}