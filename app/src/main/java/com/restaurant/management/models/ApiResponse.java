package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

/**
 * A generic wrapper class to handle common API response structure:
 * {
 * "status": "success" | "error",
 * "data": <T>, // Can be an object, list, or null
 * "message": "..."
 * }
 * @param <T> The type of the data payload.
 */
public class ApiResponse<T> {
    @SerializedName("status")
    private String status;

    @SerializedName("data")
    private T data; // Generic type for the actual data payload (e.g., CashierSession, List<Order>, etc.)

    @SerializedName("message")
    private String message;

    public String getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }

    // You can add setters if needed, but for response parsing, getters are usually sufficient.
}