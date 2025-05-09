// ========== 5.3.3. CashierSessionDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.restaurant.management.models.CashierSession;

import java.util.List;

@Dao
public interface CashierSessionDao {

    @Insert
    long insertCashierSession(CashierSession cashierSession);

    @Update
    int updateCashierSession(CashierSession cashierSession);

    @Query("SELECT * FROM cashier_sessions WHERE id = :id")
    CashierSession getCashierSessionById(int id);

    @Query("SELECT * FROM cashier_sessions WHERE user_id = :userId ORDER BY opened_at DESC")
    List<CashierSession> getCashierSessionsByUserId(int userId);

    @Query("SELECT * FROM cashier_sessions WHERE user_id = :userId AND closed_at IS NULL LIMIT 1")
    CashierSession getActiveSessionForUser(int userId);
}