package com.restaurant.management.models;

/**
 * Extends PaymentMethod with reconciliation-specific functionality
 */
public class PaymentReconciliation {
    private PaymentMethod paymentMethod;
    private double systemAmount;
    private double actualAmount;
    private double difference;

    public PaymentReconciliation(PaymentMethod paymentMethod, double systemAmount) {
        this.paymentMethod = paymentMethod;
        this.systemAmount = systemAmount;
        this.actualAmount = 0.0;
        this.difference = -systemAmount; // Initially the difference is negative (shortage)
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getName() {
        return paymentMethod.getName();
    }

    public String getCode() {
        return paymentMethod.getCode();
    }

    public String getId() {
        return paymentMethod.getId();
    }

    public double getSystemAmount() {
        return systemAmount;
    }

    public void setSystemAmount(double systemAmount) {
        this.systemAmount = systemAmount;
        calculateDifference();
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