package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Model class for Cashier Session, aligned with the API response for /cashier-sessions/open
 */
public class CashierSession {

    // Matches "session_id" from API response, typically the session ID
    @SerializedName("session_id") // Ensure this is "session_id"
    private Long sessionId;

    // Matches "user_id" from API response
    @SerializedName("user_id")
    private Integer userId;

    // --- ADD THIS FIELD AND ITS GETTERS/SETTERS ---
    // Matches "cashier_name" from API response
    @SerializedName("cashier_name")
    private String cashierName;
    // --- END ADDITION ---

    // API response showed "opening_amount": "100000.00". It's a string in the JSON.
    @SerializedName("opening_amount")
    private String openingAmount;

    // Matches "closing_amount" from API response, which can be null.
    @SerializedName("closing_amount")
    private String closingAmount;

    // Matches "expected_amount" from API response, which can be null.
    @SerializedName("expected_amount")
    private String expectedAmount;

    // Matches "difference" from API response, which can be null.
    @SerializedName("difference")
    private String difference;

    // Matches "notes" from API response, which can be null.
    @SerializedName("notes")
    private String notes;

    // Matches "opened_at" from API response.
    @SerializedName("opened_at")
    private Date openedAt;

    // Matches "closed_at" from API response, which can be null.
    @SerializedName("closed_at")
    private Date closedAt;

    // Matches "created_at" from API response.
    @SerializedName("created_at")
    private Date createdAt;

    // Matches "updated_at" from API response.
    @SerializedName("updated_at")
    private Date updatedAt;

    // Matches "tenant" from API response.
    @SerializedName("tenant")
    private String tenant;

    // Default constructor (necessary for Gson deserialization)
    public CashierSession() {
    }

    // --- Getters and Setters ---

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    // --- ADD THESE GETTER AND SETTER ---
    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }
    // --- END ADDITION ---

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

    public String getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(String expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public String getDifference() {
        return difference;
    }

    public void setDifference(String difference) {
        this.difference = difference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Date openedAt) {
        this.openedAt = openedAt;
    }

    public Date getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Date closedAt) {
        this.closedAt = closedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    /**
     * Calculate the session total (end amount - start amount)
     * Note: Requires converting String amounts to double for calculation.
     * @return the session total amount or 0.0 if amounts are invalid.
     */
    public double getSessionTotal() {
        double start = 0.0;
        double end = 0.0;
        try {
            if (openingAmount != null) {
                start = Double.parseDouble(openingAmount);
            }
            if (closingAmount != null) {
                end = Double.parseDouble(closingAmount);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing amount: " + e.getMessage());
        }
        return end - start;
    }

    @Override
    public String toString() {
        return "CashierSession{" +
                "sessionId=" + sessionId +
                ", userId=" + userId +
                ", cashierName='" + cashierName + '\'' + // Include cashierName in toString
                ", openingAmount='" + openingAmount + '\'' +
                ", closingAmount='" + closingAmount + '\'' +
                ", expectedAmount='" + expectedAmount + '\'' +
                ", difference='" + difference + '\'' +
                ", notes='" + notes + '\'' +
                ", openedAt=" + openedAt +
                ", closedAt=" + closedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", tenant='" + tenant + '\'' +
                '}';
    }
}