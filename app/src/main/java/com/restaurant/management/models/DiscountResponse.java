package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DiscountResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("count")
    private int count;
    @SerializedName("data")
    private List<Discount> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<Discount> getData() {
        return data;
    }

    public void setData(List<Discount> data) {
        this.data = data;
    }
}