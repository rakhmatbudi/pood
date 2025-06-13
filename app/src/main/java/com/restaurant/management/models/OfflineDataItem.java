package com.restaurant.management.models;

public class OfflineDataItem {
    private String title;
    private String description;
    private int count;
    private String type;

    public OfflineDataItem() {
    }

    public OfflineDataItem(String title, String description, int count, String type) {
        this.title = title;
        this.description = description;
        this.count = count;
        this.type = type;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getCount() {
        return count;
    }

    public String getType() {
        return type;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "OfflineDataItem{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", count=" + count +
                ", type='" + type + '\'' +
                '}';
    }
}