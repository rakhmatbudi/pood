// CreateOrderRequest.java
package com.restaurant.management.models;

public class CreateOrderRequest {
    private long sessionId;
    private String tableNumber;
    private String customerName;
    private long orderTypeId; // Changed from int to long

    public CreateOrderRequest(long sessionId, String tableNumber, String customerName, long orderTypeId) {
        this.sessionId = sessionId;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.orderTypeId = orderTypeId;
    }

    // Getters and setters
    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }

    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public long getOrderTypeId() { return orderTypeId; } // Changed return type to long
    public void setOrderTypeId(long orderTypeId) { this.orderTypeId = orderTypeId; } // Changed parameter to long
}