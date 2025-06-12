package com.restaurant.management.network;

import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.SessionPaymentsResponse;
import com.restaurant.management.models.PromoResponse;
import com.restaurant.management.models.MenuCategoryResponse;
import com.restaurant.management.models.MenuItemResponse;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderStatus;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.models.OrderTypesResponse;
import com.restaurant.management.models.OrderStatusesResponse;
import com.restaurant.management.models.CreateOrderRequest;
import com.restaurant.management.models.CreateOrderResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // Existing cashier session methods
    @GET("cashier/sessions/active")
    Call<CashierSession> getActiveSession(@Query("userId") int userId);

    @GET("cashier/sessions")
    Call<List<CashierSession>> getCashierSessions();

    @GET("cashier/sessions/{id}")
    Call<CashierSession> getSessionById(@Path("id") int sessionId);

    @GET("payments/grouped/sessions/details")
    Call<SessionPaymentsResponse> getSessionPayments();

    // Existing promo methods
    @GET("promos")
    Call<PromoResponse> getActivePromos();

    @GET("promos/{id}")
    Call<PromoResponse> getPromoById(@Path("id") long promoId);

    // Existing menu methods
    @GET("menu-categories")
    Call<MenuCategoryResponse> getMenuCategories();

    @GET("menu-items")
    Call<MenuItemResponse> getMenuItems();

    // ORDER-RELATED METHODS:

    /**
     * Get all orders for a session
     * @param sessionId The ID of the session
     * @return Call object with List of Orders
     */
    @GET("sessions/{sessionId}/orders")
    Call<List<Order>> getOrdersBySession(@Path("sessionId") long sessionId);

    /**
     * Create a new order
     * @param request The order creation request
     * @return Call object with CreateOrderResponse
     */
    @POST("orders")
    Call<CreateOrderResponse> createOrder(@Body CreateOrderRequest request);

    /**
     * Get all order types - FIXED to use wrapper response
     * @return Call object with OrderTypesResponse
     */
    @GET("order-types")
    Call<OrderTypesResponse> getOrderTypes();

    /**
     * Get all order statuses - FIXED to use wrapper response
     * @return Call object with OrderStatusesResponse
     */
    @GET("order-statuses")
    Call<OrderStatusesResponse> getOrderStatuses();

    /**
     * Get a specific order by ID
     * @param orderId The ID of the order
     * @return Call object with Order response
     */
    @GET("orders/{id}")
    Call<Order> getOrderById(@Path("id") long orderId);
}