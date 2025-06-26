package com.restaurant.management.network;
import com.google.gson.JsonElement;

import com.restaurant.management.models.CashierSession;
import com.restaurant.management.models.PaymentMethod;
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
import com.restaurant.management.models.DiscountResponse;
import com.restaurant.management.models.SessionSummary;
import com.restaurant.management.models.TaxResponse;

import java.util.List;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Header;

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
    @GET("discounts") // Assuming the endpoint is /discounts
    Call<DiscountResponse> getDiscounts();

    // NEW METHOD FOR TAX RATES
    @GET("taxes/rates") // Endpoint for tax rates
    Call<TaxResponse> getTaxRates();

    @GET("payment-modes")
    Call<ApiResponse<List<PaymentMethod>>> getPaymentModes();

    @GET("payments/session/{sessionId}/mode/{paymentModeId}")
    Call<ApiResponse<List<JsonElement>>> getPaymentMethodTransactions(
            @Path("sessionId") long sessionId,
            @Path("paymentModeId") String paymentModeId
    );

    @GET("cashier-sessions/{sessionId}")
    Call<ApiResponse<SessionSummary>> getSessionDetails(
            @Path("sessionId") long sessionId
    );

    @PUT("cashier-sessions/{sessionId}/close")
    Call<ApiResponse<Void>> closeSession(
            @Path("sessionId") long sessionId,
            @Body RequestBody requestBody
    );
}