package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SessionPaymentsResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<SessionWithPayments> data;

    public String getStatus() {
        return status;
    }

    public List<SessionWithPayments> getData() {
        return data;
    }
}