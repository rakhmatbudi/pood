// ========== 5.2.4. CashierSessionPayment.java ==========

package com.restaurant.management.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.math.BigDecimal;
import java.util.Date;

@Entity(tableName = "cashier_session_payments",
        foreignKeys = {
                @ForeignKey(entity = CashierSession.class,
                        parentColumns = "id",
                        childColumns = "cashier_session_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = PaymentMode.class,
                        parentColumns = "id",
                        childColumns = "payment_mode_id",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index("cashier_session_id"),
                @Index("payment_mode_id")
        })
public class CashierSessionPayment {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "cashier_session_id")
    private int cashierSessionId;

    @ColumnInfo(name = "payment_mode_id")
    private int paymentModeId;

    @ColumnInfo(name = "expected_amount")
    private BigDecimal expectedAmount;

    @ColumnInfo(name = "actual_amount")
    private BigDecimal actualAmount;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "updated_at")
    private Date updatedAt;

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCashierSessionId() {
        return cashierSessionId;
    }

    public void setCashierSessionId(int cashierSessionId) {
        this.cashierSessionId = cashierSessionId;
    }

    public int getPaymentModeId() {
        return paymentModeId;
    }

    public void setPaymentModeId(int paymentModeId) {
        this.paymentModeId = paymentModeId;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}