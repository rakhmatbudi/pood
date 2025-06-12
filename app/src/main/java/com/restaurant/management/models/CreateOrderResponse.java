// CreateOrderResponse.java
package com.restaurant.management.models;

public class CreateOrderResponse {
    private boolean success;
    private String message;
    private Order data; // The created order object

    public CreateOrderResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Order getData() { return data; }
    public void setData(Order data) { this.data = data; }
}