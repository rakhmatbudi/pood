// ========== 5.3.2. UserRoleDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.restaurant.management.models.UserRole;

import java.util.List;


@Dao
public interface UserRoleDao {

    @Insert
    long insertUserRole(UserRole userRole);

    @Update
    int updateUserRole(UserRole userRole);

    @Query("SELECT * FROM user_role WHERE id = :id")
    UserRole getUserRoleById(int id);

    @Query("SELECT * FROM user_role")
    List<UserRole> getAllUserRoles();
}