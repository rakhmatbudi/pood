// OrderTypesResponse.java (if API returns wrapped response)
package com.restaurant.management.models;

import java.util.List;

public class OrderTypesResponse {
    private boolean success;
    private String message;
    private List<OrderType> data;

    public OrderTypesResponse() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<OrderType> getData() { return data; }
    public void setData(List<OrderType> data) { this.data = data; }
}