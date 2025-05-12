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

    // New field for the order items from the API
    private List<OrderItem> orderItems = new ArrayList<>();

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

    // New getter and setter for orderItems
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    // Helper method to add an order item
    public void addOrderItem(OrderItem item) {
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        }
        this.orderItems.add(item);
    }

    // Helper method to get a formatted summary of items
    public String getItemsSummary() {
        StringBuilder builder = new StringBuilder();

        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                String item = items.get(i);

                // Remove any "(null)" occurrences from the item string
                item = item.replace(" (null)", "");

                builder.append(item);

                // Add line break if not the last item
                if (i < items.size() - 1) {
                    builder.append("\n");
                }
            }
        }

        return builder.toString();
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