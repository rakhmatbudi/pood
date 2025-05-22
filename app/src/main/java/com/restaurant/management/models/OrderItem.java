package com.restaurant.management.models;

/**
 * Model class for order items from the API
 */
public class OrderItem {
    private long id;
    private long orderId;
    private long menuItemId;
    private String menuItemName;
    private Long variantId;
    private String variantName;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private String notes;
    private String status;
    private boolean kitchenPrinted;
    private String createdAt;
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

    // Method to get display name with variant
    public String getDisplayName() {
        String name = menuItemName != null ? menuItemName : "";
        if (variantName != null && !variantName.isEmpty()) {
            name += " (" + variantName + ")";
        }
        return name;
    }

    @Override
    public String toString() {
        return quantity + "x " + menuItemName + (notes != null && !notes.isEmpty() ? " (" + notes + ")" : "");
    }
}