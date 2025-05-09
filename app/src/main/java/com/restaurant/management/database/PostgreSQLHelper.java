// ========== 11.1. PostgreSQLHelper.java ==========

package com.restaurant.management.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Helper class for establishing connection to PostgreSQL database
 * This is for future implementation when the app needs to sync with the server
 */
public class PostgreSQLHelper {

    private static final String TAG = "PostgreSQLHelper";

    // Database connection properties
    private static final String HOST = "localhost";
    private static final String PORT = "5432";
    private static final String DATABASE = "komuni40_pood";
    private static final String USER = "komuni40_rakhmat";
    private static final String PASSWORD = "Postgresp3w3d3^_^";

    private Connection connection;

    /**
     * Connect to PostgreSQL database
     */
    public Connection connect() {
        try {
            Class.forName("org.postgresql.Driver");

            String url = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);

            connection = DriverManager.getConnection(url, props);
            Log.d(TAG, "Connected to PostgreSQL database");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "PostgreSQL driver not found", e);
        } catch (SQLException e) {
            Log.e(TAG, "Failed to connect to PostgreSQL database", e);
        }

        return connection;
    }

    /**
     * Disconnect from PostgreSQL database
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                Log.d(TAG, "Disconnected from PostgreSQL database");
            } catch (SQLException e) {
                Log.e(TAG, "Failed to disconnect from PostgreSQL database", e);
            }
        }
    }

    /**
     * AsyncTask for connecting to PostgreSQL database
     */
    public static class ConnectTask extends AsyncTask<Void, Void, Connection> {

        private final Context context;
        private final ConnectionCallback callback;

        public ConnectTask(Context context, ConnectionCallback callback) {
            this.context = context;
            this.callback = callback;
        }

        @Override
        protected Connection doInBackground(Void... voids) {
            PostgreSQLHelper helper = new PostgreSQLHelper();
            return helper.connect();
        }

        @Override
        protected void onPostExecute(Connection connection) {
            if (connection != null) {
                callback.onConnectionSuccess(connection);
            } else {
                callback.onConnectionFailure("Failed to connect to PostgreSQL database");
            }
        }
    }

    /**
     * Callback interface for database connection
     */
    public interface ConnectionCallback {
        void onConnectionSuccess(Connection connection);
        void onConnectionFailure(String error);
    }
}