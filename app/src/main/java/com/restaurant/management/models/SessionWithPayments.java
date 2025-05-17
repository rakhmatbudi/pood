package com.restaurant.management.models;

import com.google.gson.annotations.SerializedName;
import java.util.Date;
import java.util.List;

public class SessionWithPayments {

    @SerializedName("cashier_session_id")
    private int cashierSessionId;

    @SerializedName("cashier_session_opened_at")
    private Date cashierSessionOpenedAt;

    @SerializedName("payments")
    private List<PaymentData> payments;

    public int getCashierSessionId() {
        return cashierSessionId;
    }

    public Date getCashierSessionOpenedAt() {
        return cashierSessionOpenedAt;
    }

    public List<PaymentData> getPayments() {
        return payments;
    }
}