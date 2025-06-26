package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName; // NEW: Import for @SerializedName
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionSummary {
    private long sessionId;
    private String cashierName;
    private double openingAmount;
    private double totalSales;
    private int totalOrders;

    // NEW: Fields for data expected from the API response
    @SerializedName("opened_at")
    private String openedAt;

    @SerializedName("cash_total")
    private double cashTotal;

    @SerializedName("card_total")
    private double cardTotal;

    @SerializedName("mobile_money_total")
    private double mobileMoneyTotal;

    @SerializedName("payment_totals")
    private Map<String, Double> paymentTotals; // This will map the nested "payment_totals" object

    private List<PaymentReconciliation> paymentReconciliations;
    private Map<String, PaymentReconciliation> reconciliationByCode;
    private String notes;

    public SessionSummary(long sessionId, String cashierName, double openingAmount) {
        this.sessionId = sessionId;
        this.cashierName = cashierName;
        this.openingAmount = openingAmount;
        this.totalSales = 0.0;
        this.totalOrders = 0;
        this.paymentReconciliations = new ArrayList<>();
        this.reconciliationByCode = new HashMap<>();
        this.notes = "";
        // Initialize new fields to default values
        this.openedAt = "";
        this.cashTotal = 0.0;
        this.cardTotal = 0.0;
        this.mobileMoneyTotal = 0.0;
        this.paymentTotals = new HashMap<>();
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public double getOpeningAmount() {
        return openingAmount;
    }

    public void setOpeningAmount(double openingAmount) {
        this.openingAmount = openingAmount;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(double totalSales) {
        this.totalSales = totalSales;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public String getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(String openedAt) {
        this.openedAt = openedAt;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(double cardTotal) {
        this.cardTotal = cardTotal;
    }

    public double getMobileMoneyTotal() {
        return mobileMoneyTotal;
    }

    public void setMobileMoneyTotal(double mobileMoneyTotal) {
        this.mobileMoneyTotal = mobileMoneyTotal;
    }

    public Map<String, Double> getPaymentTotals() {
        return paymentTotals;
    }

    public void setPaymentTotals(Map<String, Double> paymentTotals) {
        this.paymentTotals = paymentTotals;
    }


    public List<PaymentReconciliation> getPaymentReconciliations() {
        return paymentReconciliations;
    }

    public void addPaymentReconciliation(PaymentReconciliation reconciliation) {
        this.paymentReconciliations.add(reconciliation);
        this.reconciliationByCode.put(reconciliation.getCode(), reconciliation);
    }

    public PaymentReconciliation getReconciliationByCode(String code) {
        return reconciliationByCode.get(code);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}