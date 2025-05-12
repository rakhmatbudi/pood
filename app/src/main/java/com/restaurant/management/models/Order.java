package com.restaurant.management.models;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private long id;
    private String orderNumber;
    private String tableNumber;
    private String customerName;
    private double total;
    private String status;
    private String createdAt;
    private List<String> items = new ArrayList<>();
    private boolean isOpen;
    private long cashierSessionId;
    private long serverId;

    // Default constructor
    public Order() {
    }

    // Full constructor
    public Order(long id, String orderNumber, String tableNumber, String customerName,
                 double total, String status, String createdAt, List<String> items,
                 boolean isOpen, long cashierSessionId, long serverId) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.tableNumber = tableNumber;
        this.customerName = customerName;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
        this.isOpen = isOpen;
        this.cashierSessionId = cashierSessionId;
        this.serverId = serverId;
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public long getCashierSessionId() {
        return cashierSessionId;
    }

    public void setCashierSessionId(long cashierSessionId) {
        this.cashierSessionId = cashierSessionId;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    // Helper method to get a formatted summary of items
    public String getItemsSummary() {
        if (items.isEmpty()) {
            return "No items";
        }

        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(3, items.size()); i++) {
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(items.get(i));
        }

        if (items.size() > 3) {
            summary.append(", +").append(items.size() - 3).append(" more");
        }

        return summary.toString();
    }

    // Helper method to get a formatted total
    public String getFormattedTotal() {
        return String.format("$%.2f", total);
    }

    // Helper method to get a formatted status with proper capitalization
    public String getFormattedStatus() {
        if (status == null || status.isEmpty()) {
            return "Unknown";
        }

        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }
}