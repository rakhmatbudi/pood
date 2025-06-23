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
import com.restaurant.management.models.CreateOrderItemRequest;
import com.restaurant.management.models.CreateOrderItemResponse;
import com.restaurant.management.models.ApiResponse;
import com.restaurant.management.models.DiscountResponse; // Import the new DiscountResponse model

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

    // UPDATED METHOD: Get current active session for the authenticated user (JWT-based)
    @GET("cashier-sessions/current")
    Call<ApiResponse<CashierSession>> getCurrentCashierSession();

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
    @GET("sessions/{sessionId}/orders")
    Call<List<Order>> getOrdersBySession(@Path("sessionId") long sessionId);

    @POST("orders")
    Call<CreateOrderResponse> createOrder(@Body CreateOrderRequest request);

    @GET("order-types")
    Call<OrderTypesResponse> getOrderTypes();

    @GET("order-statuses")
    Call<OrderStatusesResponse> getOrderStatuses();

    @GET("orders/{id}")
    Call<Order> getOrderById(@Path("id") long orderId);

    @POST("orders/{orderId}/items")
    Call<CreateOrderItemResponse> addItemToOrder(
            @Path("orderId") long orderId,
            @Body CreateOrderItemRequest request
    );

    // METHOD FOR DISCOUNTS
    @GET("discounts") // The endpoint is confirmed as "discounts" relative to the base URL
    Call<DiscountResponse> getDiscounts();
}