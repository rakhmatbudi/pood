package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SessionWithPayments {

    @SerializedName("cashier_session_id")
    private int cashierSessionId;

    @SerializedName("payments")
    private List<PaymentData> payments;

    public int getCashierSessionId() {
        return cashierSessionId;
    }

    public List<PaymentData> getPayments() {
        return payments;
    }
}