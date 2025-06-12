package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderStatusesResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("count")
    private int count;

    @SerializedName("data")
    private List<OrderStatus> data;

    public OrderStatusesResponse() {}

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

    public List<OrderStatus> getData() {
        return data;
    }

    public void setData(List<OrderStatus> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}