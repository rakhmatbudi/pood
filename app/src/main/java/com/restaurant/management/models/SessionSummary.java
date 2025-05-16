package com.restaurant.management.models;

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