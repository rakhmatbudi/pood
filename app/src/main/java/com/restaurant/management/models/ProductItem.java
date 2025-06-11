package com.restaurant.management.models;

import java.util.List;

public class ProductItem {
    private long id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String imageUrl;
    private List<Variant> variants;

    // Additional fields for database storage
    private boolean isActive;
    private String createdAt;
    private String updatedAt;

    public ProductItem() {
        this.isActive = true; // Default to active
    }

    public ProductItem(long id, String name, String description, double price, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.isActive = true;
    }

    // Getters
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<Variant> getVariants() {
        return variants;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setVariants(List<Variant> variants) {
        this.variants = variants;
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

    // Helper method to check if item has variants
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    // Helper method to get variant count
    public int getVariantCount() {
        return variants != null ? variants.size() : 0;
    }

    // Helper method to get variant by ID
    public Variant getVariantById(long variantId) {
        if (variants != null) {
            for (Variant variant : variants) {
                if (variant.getId() == variantId) {
                    return variant;
                }
            }
        }
        return null;
    }

    // Helper method to get formatted price
    public String getFormattedPrice() {
        return String.format("%.0f", price);
    }

    @Override
    public String toString() {
        return "ProductItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", isActive=" + isActive +
                ", variants=" + (variants != null ? variants.size() : 0) +
                '}';
    }
}