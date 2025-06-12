package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class CreateOrderRequest {
    @SerializedName("cashier_session_id")
    private long sessionId;

    @SerializedName("table_number")
    private String tableNumber;

    @SerializedName("customer_id")
    private Long customerId; // Changed from long to Long to allow null

    @SerializedName("order_type_id")
    private long orderTypeId;

    @SerializedName("server_id")
    private Long serverId; // Changed from long to Long to allow null

    public CreateOrderRequest(long sessionId, String tableNumber, Long customerId, long orderTypeId, Long serverId) {
        this.sessionId = sessionId;
        this.tableNumber = tableNumber;
        this.customerId = customerId;
        this.orderTypeId = orderTypeId;
        this.serverId = serverId;
    }

    // Getters and setters
    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public long getOrderTypeId() {
        return orderTypeId;
    }

    public void setOrderTypeId(long orderTypeId) {
        this.orderTypeId = orderTypeId;
    }

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }
}