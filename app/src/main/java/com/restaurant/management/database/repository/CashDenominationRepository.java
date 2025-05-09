// ========== 5.4.5. CashDenominationRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.CashDenominationDao;
import com.restaurant.management.models.CashDenomination;

import java.util.List;

public class CashDenominationRepository {

    private final CashDenominationDao cashDenominationDao;

    public CashDenominationRepository(CashDenominationDao cashDenominationDao) {
        this.cashDenominationDao = cashDenominationDao;
    }

    public long insertCashDenomination(CashDenomination cashDenomination) {
        return cashDenominationDao.insertCashDenomination(cashDenomination);
    }

    public int updateCashDenomination(CashDenomination cashDenomination) {
        return cashDenominationDao.updateCashDenomination(cashDenomination);
    }

    public CashDenomination getCashDenominationById(int id) {
        return cashDenominationDao.getCashDenominationById(id);
    }

    public List<CashDenomination> getAllDenominations() {
        return cashDenominationDao.getAllDenominations();
    }
}