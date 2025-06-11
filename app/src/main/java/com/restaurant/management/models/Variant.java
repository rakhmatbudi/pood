package com.restaurant.management.models;

public class Variant {
    private long id;
    private String name;
    private double price;

    // Additional fields for database storage
    private boolean isActive;
    private String createdAt;
    private String updatedAt;

    public Variant() {
        this.isActive = true; // Default to active
    }

    public Variant(long id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.isActive = true;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Variant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", isActive=" + isActive +
                '}';
    }
}