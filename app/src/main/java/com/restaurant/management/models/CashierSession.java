package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

/**
 * Model class for Cashier Session, aligned with the API response for /cashier-sessions/open
 */
public class CashierSession {

    // Matches "id" from API response, typically the session ID
    @SerializedName("id")
    private Long sessionId; // Changed to Long, as IDs are often large and can be null if not present

    // Matches "user_id" from API response
    @SerializedName("user_id")
    private Integer userId; // Changed to Integer, if it could be null (though usually not for an active session)

    // API response showed "opening_amount": "100000.00". It's a string in the JSON.
    // Parsing as String avoids potential precision issues with doubles if not careful.
    @SerializedName("opening_amount")
    private String openingAmount;

    // Matches "closing_amount" from API response, which can be null. Keep as String.
    @SerializedName("closing_amount")
    private String closingAmount;

    // Matches "expected_amount" from API response, which can be null. Keep as String.
    @SerializedName("expected_amount")
    private String expectedAmount;

    // Matches "difference" from API response, which can be null. Keep as String.
    @SerializedName("difference")
    private String difference;

    // Matches "notes" from API response, which can be null.
    @SerializedName("notes")
    private String notes;

    // Matches "opened_at" from API response. Date objects work well with Gson's date format.
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

    // --- IMPORTANT NOTE ON 'user_name' and 'cashier_name' ---
    // The provided API response for /cashier-sessions/open DOES NOT contain "user_name" or "cashier_name".
    // If you need the cashier's name here, you'll either:
    // 1. Need another API call to get user details based on 'user_id'.
    // 2. Have the user's name stored locally (e.g., in SharedPreferences) after login.
    // For direct API mapping, this field should ideally be removed or set separately.
    // I'm commenting it out as it doesn't directly map to the JSON provided for this endpoint.
    // private String userName; // Not directly in the API response data object you provided

    // Default constructor (necessary for Gson deserialization)
    public CashierSession() {
    }

    // --- Getters and Setters ---

    public Long getSessionId() { // Changed return type to Long
        return sessionId;
    }

    public void setSessionId(Long sessionId) { // Changed parameter type to Long
        this.sessionId = sessionId;
    }

    public Integer getUserId() { // Changed return type to Integer
        return userId;
    }

    public void setUserId(Integer userId) { // Changed parameter type to Integer
        this.userId = userId;
    }

    // Removed userName getter/setter as it's not directly mapped from this API response

    public String getOpeningAmount() { // Changed return type to String
        return openingAmount;
    }

    public void setOpeningAmount(String openingAmount) { // Changed parameter type to String
        this.openingAmount = openingAmount;
    }

    public String getClosingAmount() { // New getter for closing_amount
        return closingAmount;
    }

    public void setClosingAmount(String closingAmount) { // New setter for closing_amount
        this.closingAmount = closingAmount;
    }

    public String getExpectedAmount() { // New getter for expected_amount
        return expectedAmount;
    }

    public void setExpectedAmount(String expectedAmount) { // New setter for expected_amount
        this.expectedAmount = expectedAmount;
    }

    public String getDifference() { // New getter for difference
        return difference;
    }

    public void setDifference(String difference) { // New setter for difference
        this.difference = difference;
    }

    public String getNotes() { // New getter for notes
        return notes;
    }

    public void setNotes(String notes) { // New setter for notes
        this.notes = notes;
    }

    public Date getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Date openedAt) {
        this.openedAt = openedAt;
    }

    public Date getClosedAt() { // New getter for closed_at
        return closedAt;
    }

    public void setClosedAt(Date closedAt) { // New setter for closed_at
        this.closedAt = closedAt;
    }

    public Date getCreatedAt() { // New getter for created_at
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) { // New setter for created_at
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() { // New getter for updated_at
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) { // New setter for updated_at
        this.updatedAt = updatedAt;
    }

    public String getTenant() { // New getter for tenant
        return tenant;
    }

    public void setTenant(String tenant) { // New setter for tenant
        this.tenant = tenant;
    }

    // Removed status getter/setter as it's not directly in the data object from the API response

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
            // Handle parsing error, e.g., log it or return 0.0
            System.err.println("Error parsing amount: " + e.getMessage());
        }
        return end - start;
    }

    @Override
    public String toString() {
        return "CashierSession{" +
                "sessionId=" + sessionId +
                ", userId=" + userId +
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