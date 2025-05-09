// ========== 5.3.4. CashierSessionPaymentDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.restaurant.management.models.CashierSessionPayment;

import java.math.BigDecimal;
import java.util.List;

@Dao
public interface CashierSessionPaymentDao {

    @Insert
    long insertCashierSessionPayment(CashierSessionPayment cashierSessionPayment);

    @Update
    int updateCashierSessionPayment(CashierSessionPayment cashierSessionPayment);

    @Query("SELECT * FROM cashier_session_payments WHERE id = :id")
    CashierSessionPayment getCashierSessionPaymentById(int id);

    @Query("SELECT * FROM cashier_session_payments WHERE cashier_session_id = :cashierSessionId")
    List<CashierSessionPayment> getPaymentsByCashierSessionId(int cashierSessionId);

    @Query("SELECT expected_amount FROM cashier_session_payments WHERE cashier_session_id = :cashierSessionId AND payment_mode_id = :paymentModeId")
    BigDecimal getExpectedAmountForPaymentMode(int cashierSessionId, int paymentModeId);
}