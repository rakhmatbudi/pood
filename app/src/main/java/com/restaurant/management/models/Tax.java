package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class Tax {
    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("amount")
    private String amount; // Keep as String if the API sends it that way (e.g., "10.00")

    public Tax(int id, String name, String description, String amount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.amount = amount;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAmount() {
        return amount;
    }

    // You might want setters if you modify these objects after creation,
    // but for simple display, getters are enough.
}