package com.restaurant.management.models;

/**
 * Model class to hold session data for display in the RecyclerView
 */
public class SessionItem {
    private int id;
    private String openedAt;
    private String closedAt;
    private String openingAmount;
    private String closingAmount;
    private String status;

    public SessionItem() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(String openedAt) {
        this.openedAt = openedAt;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(String closedAt) {
        this.closedAt = closedAt;
    }

    public String getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(String openingAmount) {
        this.openingAmount = openingAmount;
    }

    public String getClosingAmount() {
        return closingAmount;
    }

    public void setClosingAmount(String closingAmount) {
        this.closingAmount = closingAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}