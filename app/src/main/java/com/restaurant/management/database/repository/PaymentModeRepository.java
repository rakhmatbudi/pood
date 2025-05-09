// ========== 5.4.6. PaymentModeRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.PaymentModeDao;
import com.restaurant.management.models.PaymentMode;

import java.util.List;

public class PaymentModeRepository {

    private final PaymentModeDao paymentModeDao;

    public PaymentModeRepository(PaymentModeDao paymentModeDao) {
        this.paymentModeDao = paymentModeDao;
    }

    public long insertPaymentMode(PaymentMode paymentMode) {
        return paymentModeDao.insertPaymentMode(paymentMode);
    }

    public int updatePaymentMode(PaymentMode paymentMode) {
        return paymentModeDao.updatePaymentMode(paymentMode);
    }

    public PaymentMode getPaymentModeById(int id) {
        return paymentModeDao.getPaymentModeById(id);
    }

    public List<PaymentMode> getAllPaymentModes() {
        return paymentModeDao.getAllPaymentModes();
    }
}