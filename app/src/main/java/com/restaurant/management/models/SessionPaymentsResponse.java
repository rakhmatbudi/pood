package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SessionPaymentsResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private List<SessionWithPayments> data;

    @SerializedName("message") // <--- ADD THIS LINE
    private String message;   // <--- ADD THIS LINE

    public String getStatus() {
        return status;
    }

    public List<SessionWithPayments> getData() {
        return data;
    }

    public String getMessage() { // <--- ADD THIS METHOD
        return message;
    }

    // Optional: Add setters if you ever need to manually set these values
    public void setStatus(String status) {
        this.status = status;
    }

    public void setData(List<SessionWithPayments> data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}