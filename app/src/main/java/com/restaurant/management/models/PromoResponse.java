package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PromoResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<Promo> data;

    // Constructors
    public PromoResponse() {}

    public PromoResponse(String status, String message, List<Promo> data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Getters
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<Promo> getData() {
        return data;
    }

    // Setters
    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(List<Promo> data) {
        this.data = data;
    }

    // Helper methods
    public boolean isSuccess() {
        return "success".equals(status);
    }

    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    public int getPromoCount() {
        return data != null ? data.size() : 0;
    }
}