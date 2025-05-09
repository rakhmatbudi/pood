// ========== 5.4.1. UserRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.UserDao;
import com.restaurant.management.models.User;
import java.util.List;

public class UserRepository {

    private final UserDao userDao;

    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }

    public UserRepository(UserDao userDao) {
        this.userDao = userDao;
    }

    public long insertUser(User user) {
        return userDao.insertUser(user);
    }

    public int updateUser(User user) {
        return userDao.updateUser(user);
    }

    public User getUserById(int id) {
        return userDao.getUserById(id);
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }
}