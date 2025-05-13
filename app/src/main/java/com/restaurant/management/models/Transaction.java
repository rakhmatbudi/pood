package com.restaurant.management.models;

import java.util.Date;
import java.util.List;

/**
 * Model class for Transaction that represents a payment
 */
public class Transaction {
    private int id;
    private int orderId;
    private String tableNumber;
    private double amount;
    private String paymentMethod;
    private Date paymentDate;
    private List<OrderItem> orderItems;

    /**
     * Constructor for Transaction
     *
     * @param id The transaction/payment ID
     * @param orderId The associated order ID
     * @param tableNumber The table number for the order
     * @param amount The payment amount
     * @param paymentMethod The payment method (Cash, Card, Digital Wallet)
     * @param paymentDate The date and time of the payment
     * @param orderItems The list of order items included in this transaction
     */
    public Transaction(int id, int orderId, String tableNumber, double amount,
                       String paymentMethod, Date paymentDate, List<OrderItem> orderItems) {
        this.id = id;
        this.orderId = orderId;
        this.tableNumber = tableNumber;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.orderItems = orderItems;
    }

    /**
     * Get the transaction ID
     *
     * @return The transaction/payment ID
     */
    public int getId() {
        return id;
    }

    /**
     * Get the order ID
     *
     * @return The associated order ID
     */
    public int getOrderId() {
        return orderId;
    }

    /**
     * Get the table number
     *
     * @return The table number for the order
     */
    public String getTableNumber() {
        return tableNumber;
    }

    /**
     * Get the payment amount
     *
     * @return The payment amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Get the payment method
     *
     * @return The payment method (Cash, Card, Digital Wallet)
     */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * Get the payment date
     *
     * @return The date and time of the payment
     */
    public Date getPaymentDate() {
        return paymentDate;
    }

    /**
     * Get the order items
     *
     * @return The list of order items included in this transaction
     */
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    /**
     * Get the total number of items in this transaction
     * Calculates by summing the quantities of all order items
     *
     * @return The total item count
     */
    public int getItemCount() {
        int count = 0;
        for (OrderItem item : orderItems) {
            count += item.getQuantity();
        }
        return count;
    }

    /**
     * Returns a string representation of the transaction
     *
     * @return String representation of the transaction
     */
    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", tableNumber='" + tableNumber + '\'' +
                ", amount=" + amount +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", paymentDate=" + paymentDate +
                ", itemCount=" + getItemCount() +
                '}';
    }
}