package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MenuCategoryResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<MenuCategory> data;

    // Constructors
    public MenuCategoryResponse() {}

    public MenuCategoryResponse(String status, String message, List<MenuCategory> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public List<MenuCategory> getData() { return data; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
    public void setData(List<MenuCategory> data) { this.data = data; }

    // Helper methods
    public boolean isSuccess() { return "success".equals(status); }
    public boolean hasData() { return data != null && !data.isEmpty(); }
    public int getCategoryCount() { return data != null ? data.size() : 0; }
}