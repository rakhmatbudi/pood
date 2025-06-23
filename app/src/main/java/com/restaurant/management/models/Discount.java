package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class Discount {
    @SerializedName("id")
    private long id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("amount")
    private int amount;
    @SerializedName("created_at") // Add this field
    private String createdAt;    // Corresponding Java field name
    @SerializedName("updated_at") // Add this field
    private String updatedAt;    // Corresponding Java field name
    @SerializedName("tenant")     // Add this field
    private String tenant;

    public Discount() {
    }

    public Discount(long id, String name, String description, int amount, String createdAt, String updatedAt, String tenant) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tenant = tenant;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    // Add getters and setters for the new fields
    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
}