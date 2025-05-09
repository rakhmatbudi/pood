// ========== 6.3. DatabaseInitializer.java ==========

package com.restaurant.management.utils;

import android.content.Context;
import android.util.Log;

import com.restaurant.management.database.RestaurantDatabase;
import com.restaurant.management.database.repository.CashDenominationRepository;
import com.restaurant.management.database.repository.PaymentModeRepository;
import com.restaurant.management.database.repository.UserRepository;
import com.restaurant.management.database.repository.UserRoleRepository;
import com.restaurant.management.models.CashDenomination;
import com.restaurant.management.models.PaymentMode;
import com.restaurant.management.models.User;
import com.restaurant.management.models.UserRole;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class to initialize the database with sample data
 */
public class DatabaseInitializer {

    private static final String TAG = "DatabaseInitializer";

    /**
     * Initialize the database with sample data
     */
    public static void initializeDatabase(Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            RestaurantDatabase database = RestaurantDatabase.getInstance(context);

            // Initialize repositories
            UserRoleRepository userRoleRepository = new UserRoleRepository(database.userRoleDao());
            UserRepository userRepository = new UserRepository(database.userDao());
            CashDenominationRepository cashDenominationRepository = new CashDenominationRepository(database.cashDenominationDao());
            PaymentModeRepository paymentModeRepository = new PaymentModeRepository(database.paymentModeDao());

            // Check if database is already initialized
            User admin = userRepository.getUserByEmail("admin@restaurant.com");
            if (admin != null) {
                Log.d(TAG, "Database already initialized");
                return;
            }

            // Initialize user roles
            initializeUserRoles(userRoleRepository);

            // Initialize users
            initializeUsers(userRepository);

            // Initialize cash denominations
            initializeCashDenominations(cashDenominationRepository);

            // Initialize payment modes
            initializePaymentModes(paymentModeRepository);

            Log.d(TAG, "Database initialized successfully");
        });

        executor.shutdown();
    }

    private static void initializeUserRoles(UserRoleRepository userRoleRepository) {
        UserRole adminRole = new UserRole();
        adminRole.setRoleName("Admin");
        adminRole.setCreatedAt(new Date());
        adminRole.setUpdatedAt(new Date());
        userRoleRepository.insertUserRole(adminRole);

        UserRole managerRole = new UserRole();
        managerRole.setRoleName("Manager");
        managerRole.setCreatedAt(new Date());
        managerRole.setUpdatedAt(new Date());
        userRoleRepository.insertUserRole(managerRole);

        UserRole cashierRole = new UserRole();
        cashierRole.setRoleName("Cashier");
        cashierRole.setCreatedAt(new Date());
        cashierRole.setUpdatedAt(new Date());
        userRoleRepository.insertUserRole(cashierRole);

        UserRole waiterRole = new UserRole();
        waiterRole.setRoleName("Waiter");
        waiterRole.setCreatedAt(new Date());
        waiterRole.setUpdatedAt(new Date());
        userRoleRepository.insertUserRole(waiterRole);
    }

    private static void initializeUsers(UserRepository userRepository) {
        // Admin user
        User adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@restaurant.com");
        adminUser.setPassword(PasswordHasher.hashPassword("admin123"));
        adminUser.setRoleId(1); // Admin role
        adminUser.setCreatedAt(new Date());
        adminUser.setUpdatedAt(new Date());
        long adminId = userRepository.insertUser(adminUser);
        Log.d("DatabaseInitializer", "Admin user created with ID: " + adminId);

        // Manager user
        User managerUser = new User();
        managerUser.setName("Manager User");
        managerUser.setEmail("manager@restaurant.com");
        managerUser.setPassword(PasswordHasher.hashPassword("manager123"));
        managerUser.setRoleId(2); // Manager role
        managerUser.setCreatedAt(new Date());
        managerUser.setUpdatedAt(new Date());
        userRepository.insertUser(managerUser);

        // Cashier user
        User cashierUser = new User();
        cashierUser.setName("Cashier User");
        cashierUser.setEmail("cashier@restaurant.com");
        cashierUser.setPassword(PasswordHasher.hashPassword("cashier123"));
        cashierUser.setRoleId(3); // Cashier role
        cashierUser.setCreatedAt(new Date());
        cashierUser.setUpdatedAt(new Date());
        userRepository.insertUser(cashierUser);

        // Debug user with simple password for testing
        User debugUser = new User();
        debugUser.setName("Debug User");
        debugUser.setEmail("debug@test.com");
        // Store the password as plain text for debugging
        String plainPassword = "test123";
        debugUser.setPassword(plainPassword);
        debugUser.setRoleId(3); // Cashier role
        debugUser.setCreatedAt(new Date());
        debugUser.setUpdatedAt(new Date());
        long debugId = userRepository.insertUser(debugUser);
        Log.d("DatabaseInitializer", "Debug user created with ID: " + debugId);
    }

    private static void initializeCashDenominations(CashDenominationRepository cashDenominationRepository) {
        // Add common currency denominations
        int[] values = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000};

        for (int value : values) {
            CashDenomination denomination = new CashDenomination();
            denomination.setValue(value);
            denomination.setCreatedAt(new Date());
            denomination.setUpdatedAt(new Date());
            cashDenominationRepository.insertCashDenomination(denomination);
        }
    }

    private static void initializePaymentModes(PaymentModeRepository paymentModeRepository) {
        // Cash payment mode
        PaymentMode cashPayment = new PaymentMode();
        cashPayment.setName("Cash");
        cashPayment.setCreatedAt(new Date());
        cashPayment.setUpdatedAt(new Date());
        paymentModeRepository.insertPaymentMode(cashPayment);

        // Card payment mode
        PaymentMode cardPayment = new PaymentMode();
        cardPayment.setName("Card");
        cardPayment.setCreatedAt(new Date());
        cardPayment.setUpdatedAt(new Date());
        paymentModeRepository.insertPaymentMode(cardPayment);

        // Digital wallet payment mode
        PaymentMode walletPayment = new PaymentMode();
        walletPayment.setName("Digital Wallet");
        walletPayment.setCreatedAt(new Date());
        walletPayment.setUpdatedAt(new Date());
        paymentModeRepository.insertPaymentMode(walletPayment);
    }
}