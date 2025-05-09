// ========== 5.4.3. CashierSessionRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.CashierSessionDao;
import com.restaurant.management.models.CashierSession;

import java.util.List;

public class CashierSessionRepository {

    private final CashierSessionDao cashierSessionDao;

    public CashierSessionRepository(CashierSessionDao cashierSessionDao) {
        this.cashierSessionDao = cashierSessionDao;
    }

    public long insertCashierSession(CashierSession cashierSession) {
        return cashierSessionDao.insertCashierSession(cashierSession);
    }

    public int updateCashierSession(CashierSession cashierSession) {
        return cashierSessionDao.updateCashierSession(cashierSession);
    }

    public CashierSession getCashierSessionById(int id) {
        return cashierSessionDao.getCashierSessionById(id);
    }

    public List<CashierSession> getCashierSessionsByUserId(int userId) {
        return cashierSessionDao.getCashierSessionsByUserId(userId);
    }

    public CashierSession getActiveSessionForUser(int userId) {
        return cashierSessionDao.getActiveSessionForUser(userId);
    }
}