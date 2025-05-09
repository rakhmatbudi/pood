// ========== 5.4.4. CashierSessionPaymentRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.CashierSessionPaymentDao;
import com.restaurant.management.models.CashierSessionPayment;

import java.math.BigDecimal;
import java.util.List;

public class CashierSessionPaymentRepository {

    private final CashierSessionPaymentDao cashierSessionPaymentDao;

    public CashierSessionPaymentRepository(CashierSessionPaymentDao cashierSessionPaymentDao) {
        this.cashierSessionPaymentDao = cashierSessionPaymentDao;
    }

    public long insertCashierSessionPayment(CashierSessionPayment cashierSessionPayment) {
        return cashierSessionPaymentDao.insertCashierSessionPayment(cashierSessionPayment);
    }

    public int updateCashierSessionPayment(CashierSessionPayment cashierSessionPayment) {
        return cashierSessionPaymentDao.updateCashierSessionPayment(cashierSessionPayment);
    }

    public CashierSessionPayment getCashierSessionPaymentById(int id) {
        return cashierSessionPaymentDao.getCashierSessionPaymentById(id);
    }

    public List<CashierSessionPayment> getPaymentsByCashierSessionId(int cashierSessionId) {
        return cashierSessionPaymentDao.getPaymentsByCashierSessionId(cashierSessionId);
    }

    public BigDecimal getExpectedAmountForPaymentMode(int cashierSessionId, int paymentModeId) {
        return cashierSessionPaymentDao.getExpectedAmountForPaymentMode(cashierSessionId, paymentModeId);
    }
}