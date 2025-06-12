package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class CreateOrderResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private OrderData data;

    public static class OrderData {
        @SerializedName("id")
        private long id;

        @SerializedName("table_number")
        private String tableNumber;

        @SerializedName("server_id")
        private long serverId;

        @SerializedName("cashier_session_id")
        private long cashierSessionId;

        @SerializedName("status")
        private String status;

        @SerializedName("is_open")
        private boolean isOpen;

        @SerializedName("total_amount")
        private String totalAmount;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("updated_at")
        private String updatedAt;

        @SerializedName("customer_id")
        private long customerId;

        @SerializedName("discount_amount")
        private String discountAmount;

        @SerializedName("service_charge")
        private String serviceCharge;

        @SerializedName("tax_amount")
        private String taxAmount;

        @SerializedName("order_type_id")
        private long orderTypeId;

        @SerializedName("order_status")
        private int orderStatus;

        @SerializedName("charged_amount")
        private String chargedAmount;

        @SerializedName("promo_amount")
        private String promoAmount;

        // Getters
        public long getId() { return id; }
        public String getTableNumber() { return tableNumber; }
        public long getServerId() { return serverId; }
        public long getCashierSessionId() { return cashierSessionId; }
        public String getStatus() { return status; }
        public boolean isOpen() { return isOpen; }
        public String getTotalAmount() { return totalAmount; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public long getCustomerId() { return customerId; }
        public String getDiscountAmount() { return discountAmount; }
        public String getServiceCharge() { return serviceCharge; }
        public String getTaxAmount() { return taxAmount; }
        public long getOrderTypeId() { return orderTypeId; }
        public int getOrderStatus() { return orderStatus; }
        public String getChargedAmount() { return chargedAmount; }
        public String getPromoAmount() { return promoAmount; }

        // Setters
        public void setId(long id) { this.id = id; }
        public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
        public void setServerId(long serverId) { this.serverId = serverId; }
        public void setCashierSessionId(long cashierSessionId) { this.cashierSessionId = cashierSessionId; }
        public void setStatus(String status) { this.status = status; }
        public void setOpen(boolean open) { isOpen = open; }
        public void setTotalAmount(String totalAmount) { this.totalAmount = totalAmount; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public void setCustomerId(long customerId) { this.customerId = customerId; }
        public void setDiscountAmount(String discountAmount) { this.discountAmount = discountAmount; }
        public void setServiceCharge(String serviceCharge) { this.serviceCharge = serviceCharge; }
        public void setTaxAmount(String taxAmount) { this.taxAmount = taxAmount; }
        public void setOrderTypeId(long orderTypeId) { this.orderTypeId = orderTypeId; }
        public void setOrderStatus(int orderStatus) { this.orderStatus = orderStatus; }
        public void setChargedAmount(String chargedAmount) { this.chargedAmount = chargedAmount; }
        public void setPromoAmount(String promoAmount) { this.promoAmount = promoAmount; }
    }

    // Main getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OrderData getData() {
        return data;
    }

    public void setData(OrderData data) {
        this.data = data;
    }

    // Convenience methods
    public boolean isSuccess() {
        return "success".equals(status);
    }

    public long getOrderId() {
        return data != null ? data.getId() : -1;
    }
}