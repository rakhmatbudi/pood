package com.restaurant.management.models;

import java.util.List;

public class ProductResponse {
    private String status;
    private List<Product> data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Product> getData() {
        return data;
    }

    public void setData(List<Product> data) {
        this.data = data;
    }
}