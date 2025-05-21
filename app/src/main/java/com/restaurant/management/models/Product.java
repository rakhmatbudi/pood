package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

public class Product {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("price")
    private String price;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("image_path")
    private String imagePath;

    // Replace the separate category fields with a nested Category object
    @SerializedName("category")
    private Category category;

    // Inner class to represent the category structure from the API
    public static class Category {
        @SerializedName("id")
        private int id;

        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        // Getters for Category
        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }

    // Other fields remain the same
    @SerializedName("variants")
    private Object[] variants;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // Updated methods to access category data through the nested object
    public int getCategoryId() {
        return (category != null) ? category.getId() : 0;
    }

    public String getCategoryName() {
        return (category != null) ? category.getName() : null;
    }

    public String getCategoryDescription() {
        return (category != null) ? category.getDescription() : null;
    }

    // We no longer need these setters as they're part of the nested object
    // But we can keep them for backward compatibility if needed
    public void setCategoryId(int categoryId) {
        // No direct implementation needed as it's now in the nested object
    }

    public void setCategoryName(String categoryName) {
        // No direct implementation needed as it's now in the nested object
    }

    public void setCategoryDescription(String categoryDescription) {
        // No direct implementation needed as it's now in the nested object
    }

    // Getter for the Category object if needed
    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}