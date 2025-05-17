package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Model class for Cashier Session
 */
public class CashierSession {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("start_amount")
    private double startAmount;

    @SerializedName("end_amount")
    private double endAmount;

    @SerializedName("start_time")
    private Date startTime;

    @SerializedName("end_time")
    private Date endTime;

    @SerializedName("opened_at")
    private Date openedAt;

    @SerializedName("status")
    private String status;

    // Default constructor
    public CashierSession() {
    }

    // Constructor with all fields
    public CashierSession(int id, int userId, String userName, double startAmount, double endAmount,
                          Date startTime, Date endTime, Date openedAt, String status) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.startAmount = startAmount;
        this.endAmount = endAmount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.openedAt = openedAt;
        this.status = status;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public double getStartAmount() {
        return startAmount;
    }

    public void setStartAmount(double startAmount) {
        this.startAmount = startAmount;
    }

    public double getEndAmount() {
        return endAmount;
    }

    public void setEndAmount(double endAmount) {
        this.endAmount = endAmount;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Date openedAt) {
        this.openedAt = openedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Calculate the session total (end amount - start amount)
     * @return the session total amount
     */
    public double getSessionTotal() {
        return endAmount - startAmount;
    }

    @Override
    public String toString() {
        return "CashierSession{" +
                "id=" + id +
                ", userId=" + userId +
                ", userName='" + userName + '\'' +
                ", startAmount=" + startAmount +
                ", endAmount=" + endAmount +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", openedAt=" + openedAt +
                ", status='" + status + '\'' +
                '}';
    }
}