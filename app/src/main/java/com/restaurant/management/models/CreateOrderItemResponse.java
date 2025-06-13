package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class CreateOrderItemResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private OrderItem data;

    @SerializedName("error")
    private String error;

    // Default constructor
    public CreateOrderItemResponse() {}

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public OrderItem getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(OrderItem data) {
        this.data = data;
    }

    public void setError(String error) {
        this.error = error;
    }

    // Inner class for the order item data
    public static class OrderItem {
        @SerializedName("id")
        private long id;

        @SerializedName("order_id")
        private long orderId;

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

        @SerializedName("notes")
        private String notes;

        @SerializedName("is_complimentary")
        private boolean isComplimentary;

        @SerializedName("is_custom_price")
        private boolean isCustomPrice;

        @SerializedName("original_price")
        private Double originalPrice;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("updated_at")
        private String updatedAt;

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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public boolean isComplimentary() {
            return isComplimentary;
        }

        public void setComplimentary(boolean complimentary) {
            isComplimentary = complimentary;
        }

        public boolean isCustomPrice() {
            return isCustomPrice;
        }

        public void setCustomPrice(boolean customPrice) {
            isCustomPrice = customPrice;
        }

        public Double getOriginalPrice() {
            return originalPrice;
        }

        public void setOriginalPrice(Double originalPrice) {
            this.originalPrice = originalPrice;
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
    }
}