// ========== 11.2. DatabaseSyncService.java ==========

package com.restaurant.management.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.restaurant.management.database.PostgreSQLHelper;
import com.restaurant.management.database.RestaurantDatabase;
import com.restaurant.management.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for syncing data between local Room database and remote PostgreSQL database
 * This is for future implementation when the app needs to sync with the server
 */
public class DatabaseSyncService extends Service {

    private static final String TAG = "DatabaseSyncService";

    private ExecutorService executorService;
    private PostgreSQLHelper postgreSQLHelper;
    private RestaurantDatabase roomDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
        postgreSQLHelper = new PostgreSQLHelper();
        roomDatabase = RestaurantDatabase.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executorService.execute(this::performSync);
        return START_STICKY;
    }

    private void performSync() {
        // Connect to PostgreSQL database
        Connection connection = postgreSQLHelper.connect();

        if (connection != null) {
            try {
                // Sync users
                syncUsers(connection);

                // Sync other data (to be implemented)
                // syncCashierSessions(connection);
                // syncCashierSessionPayments(connection);

                // Disconnect from PostgreSQL database
                postgreSQLHelper.disconnect();
            } catch (SQLException e) {
                Log.e(TAG, "Failed to sync data", e);
            }
        }
    }

    private void syncUsers(Connection connection) throws SQLException {
        // Example of syncing users from PostgreSQL to Room
        String query = "SELECT id, name, email, password, role_id FROM users";
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String name = resultSet.getString("name");
            String email = resultSet.getString("email");
            String password = resultSet.getString("password");
            int roleId = resultSet.getInt("role_id");

            // Create user object
            User user = new User();
            user.setId(id);
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRoleId(roleId);

            // Insert or update user in Room database
            // For illustration only - actual implementation would handle conflicts
            roomDatabase.userDao().insertUser(user);
        }

        resultSet.close();
        statement.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}