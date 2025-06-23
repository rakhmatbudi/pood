package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TaxResponse {
    @SerializedName("status")
    private String status;
    @SerializedName("data")
    private List<Tax> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Tax> getData() {
        return data;
    }

    public void setData(List<Tax> data) {
        this.data = data;
    }
}