package com.restaurant.management.models;

public class Tax {
    private int id;
    private String name;
    private String description;
    private String amount;

    public Tax(int id, String name, String description, String amount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.amount = amount;
    }

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
}