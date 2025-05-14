package com.restaurant.management.models;

public class PaymentReconciliation {
    private String paymentType;
    private double systemAmount;
    private double actualAmount;
    private double difference;

    public PaymentReconciliation(String paymentType, double systemAmount) {
        this.paymentType = paymentType;
        this.systemAmount = systemAmount;
        this.actualAmount = 0.0;
        this.difference = 0.0;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public double getSystemAmount() {
        return systemAmount;
    }

    public void setSystemAmount(double systemAmount) {
        this.systemAmount = systemAmount;
    }

    public double getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(double actualAmount) {
        this.actualAmount = actualAmount;
        calculateDifference();
    }

    public double getDifference() {
        return difference;
    }

    private void calculateDifference() {
        this.difference = actualAmount - systemAmount;
    }
}