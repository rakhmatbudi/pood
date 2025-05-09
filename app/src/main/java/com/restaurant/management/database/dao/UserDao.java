// ========== 5.3.1. UserDao.java ==========

package com.restaurant.management.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

import com.restaurant.management.models.User;

@Dao
public interface UserDao {

    @Insert
    long insertUser(User user);

    @Update
    int updateUser(User user);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(int id);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users LIMIT 1")
    User getAnyUser();

    @Query("SELECT * FROM users")
    List<User> getAllUsers();
}