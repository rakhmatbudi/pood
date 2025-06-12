package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MenuItemResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<ProductItem> data;

    // Constructors
    public MenuItemResponse() {}

    public MenuItemResponse(String status, String message, List<ProductItem> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public List<ProductItem> getData() { return data; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<ProductItem> data) { this.data = data; }

    // Helper methods
    public boolean isSuccess() { return "success".equals(status); }
    public boolean hasData() { return data != null && !data.isEmpty(); }
    public int getItemCount() { return data != null ? data.size() : 0; }
}