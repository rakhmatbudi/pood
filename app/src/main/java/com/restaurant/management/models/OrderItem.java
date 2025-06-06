package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for order items from the API
 */
public class OrderItem {
    private long id;

    @SerializedName("order_id")
    private long orderId;

    @SerializedName("menu_item_id")
    private long menuItemId;

    @SerializedName("menu_item_name")
    private String menuItemName;

    @SerializedName("variant_id")
    private Long variantId;

    @SerializedName("variant_name")
    private String variantName;

    private int quantity;

    @SerializedName("unit_price")
    private double unitPrice;

    @SerializedName("total_price")
    private double totalPrice;

    private String notes;
    private String status;

    @SerializedName("kitchen_printed")
    private boolean kitchenPrinted;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public OrderItem() {
        // Default constructor
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public void setMenuItemName(String menuItemName) {
        this.menuItemName = menuItemName;
    }

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isKitchenPrinted() {
        return kitchenPrinted;
    }

    public void setKitchenPrinted(boolean kitchenPrinted) {
        this.kitchenPrinted = kitchenPrinted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    // Method to get display name with variant - handles null properly
    public String getDisplayName() {
        String name = menuItemName != null ? menuItemName : "Unknown Item";

        // Only add variant if it exists and is not null/empty
        if (variantName != null && !variantName.trim().isEmpty()) {
            name += " (" + variantName + ")";
        }

        return name;
    }

    @Override
    public String toString() {
        String result = quantity + "x " + menuItemName;

        // Only add notes if they exist and are not null/empty
        if (notes != null && !notes.trim().isEmpty()) {
            result += " (" + notes + ")";
        }

        return result;
    }
}