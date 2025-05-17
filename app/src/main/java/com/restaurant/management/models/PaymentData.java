package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PaymentData {

    @SerializedName("payment_id")
    private int paymentId;

    @SerializedName("order_id")
    private int orderId;

    @SerializedName("order_table_number")
    private String orderTableNumber;

    @SerializedName("payment_amount")
    private double paymentAmount;

    @SerializedName("payment_mode")
    private int paymentMode;

    @SerializedName("payment_mode_name")
    private String paymentModeName;

    @SerializedName("payment_date")
    private String paymentDate;

    @SerializedName("transaction_id")
    private String transactionId;

    @SerializedName("order_items")
    private List<OrderItemData> orderItems;

    public int getPaymentId() {
        return paymentId;
    }

    public int getOrderId() {
        return orderId;
    }

    public String getOrderTableNumber() {
        return orderTableNumber;
    }

    public double getPaymentAmount() {
        return paymentAmount;
    }

    public int getPaymentMode() {
        return paymentMode;
    }

    public String getPaymentModeName() {
        return paymentModeName;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public List<OrderItemData> getOrderItems() {
        return orderItems;
    }

    public static class OrderItemData {

        @SerializedName("item_id")
        private int itemId;

        @SerializedName("menu_item_id")
        private int menuItemId;

        @SerializedName("menu_item_name")
        private String menuItemName;

        @SerializedName("variant_id")
        private Long variantId;

        @SerializedName("quantity")
        private int quantity;

        @SerializedName("unit_price")
        private double unitPrice;

        @SerializedName("total_price")
        private double totalPrice;

        @SerializedName("notes")
        private String notes;

        public int getItemId() {
            return itemId;
        }

        public int getMenuItemId() {
            return menuItemId;
        }

        public String getMenuItemName() {
            return menuItemName;
        }

        public Long getVariantId() {
            return variantId;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public String getNotes() {
            return notes;
        }
    }
}