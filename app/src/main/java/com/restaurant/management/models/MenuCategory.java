package com.restaurant.management.models;

public class MenuCategory {
    private long id;
    private String name;
    private String description;
    private String createdAt;
    private String updatedAt;
    private boolean isDisplayed;
    private String displayPicture;
    private String menuCategoryGroup;
    private String skuId;
    private boolean isHighlight;
    private boolean isDisplayForSelfOrder;

    public MenuCategory() {
        this.isDisplayed = true;
        this.isHighlight = false;
        this.isDisplayForSelfOrder = true;
    }

    public MenuCategory(long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isDisplayed = true;
        this.isHighlight = false;
        this.isDisplayForSelfOrder = true;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDisplayed() {
        return isDisplayed;
    }

    public String getDisplayPicture() {
        return displayPicture;
    }

    public String getMenuCategoryGroup() {
        return menuCategoryGroup;
    }

    public String getSkuId() {
        return skuId;
    }

    public boolean isHighlight() {
        return isHighlight;
    }

    public boolean isDisplayForSelfOrder() {
        return isDisplayForSelfOrder;
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

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDisplayed(boolean displayed) {
        isDisplayed = displayed;
    }

    public void setDisplayPicture(String displayPicture) {
        this.displayPicture = displayPicture;
    }

    public void setMenuCategoryGroup(String menuCategoryGroup) {
        this.menuCategoryGroup = menuCategoryGroup;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public void setHighlight(boolean highlight) {
        isHighlight = highlight;
    }

    public void setDisplayForSelfOrder(boolean displayForSelfOrder) {
        isDisplayForSelfOrder = displayForSelfOrder;
    }

    @Override
    public String toString() {
        return "MenuCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", menuCategoryGroup='" + menuCategoryGroup + '\'' +
                ", skuId='" + skuId + '\'' +
                ", isDisplayed=" + isDisplayed +
                ", isHighlight=" + isHighlight +
                '}';
    }
}