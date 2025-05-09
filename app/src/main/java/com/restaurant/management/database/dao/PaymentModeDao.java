// ========== 5.3.6. PaymentModeDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.restaurant.management.models.PaymentMode;

import java.util.List;

@Dao
public interface PaymentModeDao {

    @Insert
    long insertPaymentMode(PaymentMode paymentMode);

    @Update
    int updatePaymentMode(PaymentMode paymentMode);

    @Query("SELECT * FROM payment_modes WHERE id = :id")
    PaymentMode getPaymentModeById(int id);

    @Query("SELECT * FROM payment_modes")
    List<PaymentMode> getAllPaymentModes();
}