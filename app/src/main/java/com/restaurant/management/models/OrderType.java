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
        return name; // This is crucial for Spinner display
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderType orderType = (OrderType) obj;
        return id == orderType.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}