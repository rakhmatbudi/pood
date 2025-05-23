package com.restaurant.management.models;

public class OrderStatus {
    private long id;
    private String name;
    private String description;

    // Default constructor
    public OrderStatus() {}

    // Constructor with parameters
    public OrderStatus(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // Getters and setters
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

    // For display in spinner
    @Override
    public String toString() {
        // Capitalize first letter for display
        if (name == null || name.isEmpty()) {
            return "Unknown";
        }
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    // Helper method to get formatted name
    public String getFormattedName() {
        return toString();
    }
}