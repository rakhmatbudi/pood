package com.restaurant.management.helpers;

import com.restaurant.management.models.CreateOrderItemRequest;

public class OrderItemSyncData {
    private long localId;
    private long orderId;
    private long menuItemId;
    private Long variantId;
    private int quantity;
    private double unitPrice;
    private double totalPrice;
    private String notes;
    private String status;
    private boolean isComplimentary;
    private boolean isCustomPrice;
    private double originalPrice;
    private boolean kitchenPrinted;
    private boolean isSynced;
    private Long serverId;
    private String createdAt;
    private String menuItemName;
    private String variantName;

    // Constructors
    public OrderItemSyncData() {}

    // Getters and Setters
    public long getLocalId() { return localId; }
    public void setLocalId(long localId) { this.localId = localId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public long getMenuItemId() { return menuItemId; }
    public void setMenuItemId(long menuItemId) { this.menuItemId = menuItemId; }

    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isComplimentary() { return isComplimentary; }
    public void setComplimentary(boolean complimentary) { isComplimentary = complimentary; }

    public boolean isCustomPrice() { return isCustomPrice; }
    public void setCustomPrice(boolean customPrice) { isCustomPrice = customPrice; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public boolean isKitchenPrinted() { return kitchenPrinted; }
    public void setKitchenPrinted(boolean kitchenPrinted) { this.kitchenPrinted = kitchenPrinted; }

    public boolean isSynced() { return isSynced; }
    public void setSynced(boolean synced) { isSynced = synced; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getMenuItemName() { return menuItemName; }
    public void setMenuItemName(String menuItemName) { this.menuItemName = menuItemName; }

    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }

    // Convert to CreateOrderItemRequest for API sync
    public CreateOrderItemRequest toCreateOrderItemRequest() {
        CreateOrderItemRequest request = new CreateOrderItemRequest();
        request.setMenuItemId(this.menuItemId);
        request.setVariantId(this.variantId);
        request.setQuantity(this.quantity);
        request.setUnitPrice(this.unitPrice);
        request.setTotalPrice(this.totalPrice);
        request.setNotes(this.notes);
        request.setStatus(this.status);
        request.setComplimentary(this.isComplimentary);
        request.setCustomPrice(this.isCustomPrice);
        request.setOriginalPrice(this.originalPrice);
        request.setKitchenPrinted(this.kitchenPrinted);
        return request;
    }
}