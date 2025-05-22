// Create this new model class: OrderType.java
package com.restaurant.management.models;

public class OrderType {
    private long id;
    private String name;

    public OrderType() {}

    public OrderType(long id, String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public String toString() {
        return name; // This will be displayed in the spinner
    }
}