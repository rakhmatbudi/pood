// ========== 5.3.5. CashDenominationDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.restaurant.management.models.CashDenomination;

import java.util.List;

@Dao
public interface CashDenominationDao {

    @Insert
    long insertCashDenomination(CashDenomination cashDenomination);

    @Update
    int updateCashDenomination(CashDenomination cashDenomination);

    @Query("SELECT * FROM cash_denominations WHERE id = :id")
    CashDenomination getCashDenominationById(int id);

    @Query("SELECT * FROM cash_denominations ORDER BY value DESC")
    List<CashDenomination> getAllDenominations();
}