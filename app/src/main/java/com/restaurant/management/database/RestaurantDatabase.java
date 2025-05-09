// ========== 5.1. RestaurantDatabase.java ==========

package com.restaurant.management.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.restaurant.management.database.dao.CashDenominationDao;
import com.restaurant.management.database.dao.CashierSessionDao;
import com.restaurant.management.database.dao.CashierSessionPaymentDao;
import com.restaurant.management.database.dao.PaymentModeDao;
import com.restaurant.management.database.dao.UserDao;
import com.restaurant.management.database.dao.UserRoleDao;
import com.restaurant.management.models.CashDenomination;
import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.CashierSessionPayment;
import com.restaurant.management.models.PaymentMode;
import com.restaurant.management.models.User;
import com.restaurant.management.models.UserRole;
import com.restaurant.management.utils.Converters;

@Database(entities = {User.class, UserRole.class, CashierSession.class, CashierSessionPayment.class, CashDenomination.class, PaymentMode.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class RestaurantDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "restaurant_db";
    private static RestaurantDatabase instance;

    public static synchronized RestaurantDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            RestaurantDatabase.class, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract UserDao userDao();
    public abstract UserRoleDao userRoleDao();
    public abstract CashierSessionDao cashierSessionDao();
    public abstract CashierSessionPaymentDao cashierSessionPaymentDao();
    public abstract CashDenominationDao cashDenominationDao();
    public abstract PaymentModeDao paymentModeDao();
}