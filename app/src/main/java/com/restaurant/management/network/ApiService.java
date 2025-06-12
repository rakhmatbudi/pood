package com.restaurant.management.network;

import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.SessionPaymentsResponse;
import com.restaurant.management.models.PromoResponse;
import com.restaurant.management.models.MenuCategoryResponse;
import com.restaurant.management.models.MenuItemResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * Get active cashier session for a user
     * @param userId The ID of the user
     * @return Call object with CashierSession response
     */
    @GET("cashier/sessions/active")
    Call<CashierSession> getActiveSession(@Query("userId") int userId);

    /**
     * Get all cashier sessions
     * @return Call object with List of CashierSession response
     */
    @GET("cashier/sessions")
    Call<List<CashierSession>> getCashierSessions();

    /**
     * Get a specific session by ID
     * @param sessionId The ID of the session
     * @return Call object with CashierSession response
     */
    @GET("cashier/sessions/{id}")
    Call<CashierSession> getSessionById(@Path("id") int sessionId);

    @GET("payments/grouped/sessions/details")
    Call<SessionPaymentsResponse> getSessionPayments();

    /**
     * Get all active promotions
     * @return Call object with PromoResponse containing promos data
     */
    @GET("promos")
    Call<PromoResponse> getActivePromos();

    /**
     * Get a specific promo by ID
     * @param promoId The ID of the promo
     * @return Call object with PromoResponse containing single promo
     */
    @GET("promos/{id}")
    Call<PromoResponse> getPromoById(@Path("id") long promoId);

    /**
     * Get all menu categories
     * @return Call object with MenuCategoryResponse containing categories data
     */
    @GET("menu-categories")
    Call<MenuCategoryResponse> getMenuCategories();

    /**
     * Get all menu items
     * @return Call object with MenuItemResponse containing menu items data
     */
    @GET("menu-items")
    Call<MenuItemResponse> getMenuItems();
}