// ========== 5.4.2. UserRoleRepository.java ==========

package com.restaurant.management.database.repository;

import com.restaurant.management.database.dao.UserRoleDao;
import com.restaurant.management.models.UserRole;

import java.util.List;

public class UserRoleRepository {

    private final UserRoleDao userRoleDao;

    public UserRoleRepository(UserRoleDao userRoleDao) {
        this.userRoleDao = userRoleDao;
    }

    public long insertUserRole(UserRole userRole) {
        return userRoleDao.insertUserRole(userRole);
    }

    public int updateUserRole(UserRole userRole) {
        return userRoleDao.updateUserRole(userRole);
    }

    public UserRole getUserRoleById(int id) {
        return userRoleDao.getUserRoleById(id);
    }

    public List<UserRole> getAllUserRoles() {
        return userRoleDao.getAllUserRoles();
    }
}