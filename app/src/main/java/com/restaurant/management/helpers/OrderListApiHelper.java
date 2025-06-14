package com.restaurant.management.helpers;

import android.content.Context;
import android.util.Log;

import com.restaurant.management.R;
import com.restaurant.management.database.DatabaseManager;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;
import com.restaurant.management.models.OrderStatus;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;

public class OrderListApiHelper {
    private static final String TAG = "OrderListApiHelper";
    private static final String ORDER_TYPES_API_URL = "https://api.pood.lol/order-types/";
    private static final String ORDER_STATUSES_API_URL = "https://api.pood.lol/order-statuses";
    private static final String ORDERS_API_URL = "https://api.pood.lol/orders";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient client;
    private final DatabaseManager databaseManager;

    public interface OrdersCallback {
        void onSuccess(List<Order> orders);
        void onError(String errorMessage);
    }

    public interface OrderStatusesCallback {
        void onSuccess(List<OrderStatus> orderStatuses);
        void onError(String errorMessage);
    }

    public interface OrderTypesCallback {
        void onSuccess(List<OrderType> orderTypes);
        void onError(String errorMessage);
    }

    public interface CreateOrderCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public OrderListApiHelper(Context context) {
        this.context = context;
        this.client = createHttpClient(context);
        this.databaseManager = DatabaseManager.getInstance(context);
        Log.d(TAG, "OrderListApiHelper initialized with DatabaseManager");
    }

    private static OkHttpClient createHttpClient(Context context) {
        try {
            Retrofit retrofit = ApiClient.getClient(context);
            return (OkHttpClient) retrofit.callFactory();
        } catch (Exception e) {
            Log.e(TAG, "Error creating HTTP client from ApiClient, using default", e);
            return new OkHttpClient();
        }
    }

    public void fetchOrders(long sessionId, OrdersCallback callback) {
        String apiUrl = ORDERS_API_URL + "/sessions/" + sessionId + "?t=" + System.currentTimeMillis();

        String authToken = getAuthToken();
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        Log.d(TAG, "Fetching orders for session: " + sessionId);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch orders", e);
                callback.onError("Network error occurred: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Order fetch failed with code: " + response.code());
                        callback.onError("Server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (!jsonResponse.has("data")) {
                        Log.e(TAG, "Response missing 'data' field");
                        callback.onError("Invalid response format");
                        return;
                    }

                    JSONArray ordersArray = jsonResponse.getJSONArray("data");
                    List<Order> orders = new ArrayList<>();

                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject orderJson = ordersArray.getJSONObject(i);
                        Order order = parseOrder(orderJson);
                        if (order != null && order.getId() > 0) {
                            orders.add(order);
                        }
                    }

                    Log.d(TAG, "Successfully parsed " + orders.size() + " orders");
                    callback.onSuccess(orders);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing orders response", e);
                    callback.onError("Error processing response: " + e.getMessage());
                }
            }
        });
    }

    public void fetchOrderStatuses(OrderStatusesCallback callback) {
        String authToken = getAuthToken();
        Request.Builder requestBuilder = new Request.Builder()
                .url(ORDER_STATUSES_API_URL)
                .header("Cache-Control", "no-cache");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        Log.d(TAG, "Fetching order statuses from API");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch order statuses", e);

                // Fall back to cached data
                Log.d(TAG, "Falling back to cached order statuses");
                List<OrderStatus> cachedStatuses = getOrderStatuses();
                if (!cachedStatuses.isEmpty()) {
                    callback.onSuccess(cachedStatuses);
                } else {
                    callback.onError("Failed to fetch order statuses and no cached data available");
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONArray statusesArray = new JSONArray(responseBody);
                        List<OrderStatus> orderStatuses = new ArrayList<>();

                        for (int i = 0; i < statusesArray.length(); i++) {
                            JSONObject statusJson = statusesArray.getJSONObject(i);
                            OrderStatus orderStatus = new OrderStatus();
                            orderStatus.setId(statusJson.optLong("id"));
                            orderStatus.setName(statusJson.optString("name"));
                            orderStatus.setDescription(statusJson.optString("description"));
                            orderStatuses.add(orderStatus);
                        }

                        // Cache the fresh data
                        cacheFreshOrderStatuses(orderStatuses);

                        Log.d(TAG, "Successfully fetched " + orderStatuses.size() + " order statuses");
                        callback.onSuccess(orderStatuses);
                    } else {
                        Log.e(TAG, "Order statuses fetch failed with code: " + response.code());

                        // Fall back to cached data
                        List<OrderStatus> cachedStatuses = getOrderStatuses();
                        if (!cachedStatuses.isEmpty()) {
                            Log.d(TAG, "Using cached order statuses due to API error");
                            callback.onSuccess(cachedStatuses);
                        } else {
                            callback.onError("HTTP error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing order statuses", e);

                    // Fall back to cached data
                    List<OrderStatus> cachedStatuses = getOrderStatuses();
                    if (!cachedStatuses.isEmpty()) {
                        Log.d(TAG, "Using cached order statuses due to parsing error");
                        callback.onSuccess(cachedStatuses);
                    } else {
                        callback.onError("Error parsing order statuses");
                    }
                }
            }
        });
    }

    public void fetchOrderTypes(OrderTypesCallback callback) {
        String authToken = getAuthToken();
        Request.Builder requestBuilder = new Request.Builder()
                .url(ORDER_TYPES_API_URL)
                .header("Cache-Control", "no-cache");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        Log.d(TAG, "Fetching order types from API");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch order types", e);

                // Fall back to cached data
                Log.d(TAG, "Falling back to cached order types");
                List<OrderType> cachedTypes = getOrderTypes();
                if (!cachedTypes.isEmpty()) {
                    callback.onSuccess(cachedTypes);
                } else {
                    callback.onError("Failed to fetch order types: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Order types fetch failed with code: " + response.code());

                        // Fall back to cached data
                        List<OrderType> cachedTypes = getOrderTypes();
                        if (!cachedTypes.isEmpty()) {
                            Log.d(TAG, "Using cached order types due to API error");
                            callback.onSuccess(cachedTypes);
                        } else {
                            callback.onError("HTTP error: " + response.code());
                        }
                        return;
                    }

                    String responseBody = response.body().string();
                    List<OrderType> orderTypes = parseOrderTypesResponse(responseBody);

                    if (orderTypes != null && !orderTypes.isEmpty()) {
                        // Cache the fresh data
                        cacheFreshOrderTypes(orderTypes);

                        Log.d(TAG, "Successfully fetched " + orderTypes.size() + " order types");
                        callback.onSuccess(orderTypes);
                    } else {
                        // Fall back to cached data
                        List<OrderType> cachedTypes = getOrderTypes();
                        if (!cachedTypes.isEmpty()) {
                            Log.d(TAG, "Using cached order types due to empty API response");
                            callback.onSuccess(cachedTypes);
                        } else {
                            callback.onError("No order types found");
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing order types response", e);

                    // Fall back to cached data
                    List<OrderType> cachedTypes = getOrderTypes();
                    if (!cachedTypes.isEmpty()) {
                        Log.d(TAG, "Using cached order types due to processing error");
                        callback.onSuccess(cachedTypes);
                    } else {
                        callback.onError("Error processing response");
                    }
                }
            }
        });
    }

    private List<OrderType> parseOrderTypesResponse(String responseBody) {
        try {
            // Try to parse as array first (direct array response)
            try {
                JSONArray dataArray = new JSONArray(responseBody);
                return parseOrderTypesFromArray(dataArray);
            } catch (JSONException e) {
                // Not a direct array, try object format
            }

            // Try to parse as object with data field
            JSONObject jsonResponse = new JSONObject(responseBody);
            if ("success".equals(jsonResponse.optString("status")) && jsonResponse.has("data")) {
                JSONArray dataArray = jsonResponse.getJSONArray("data");
                return parseOrderTypesFromArray(dataArray);
            } else {
                Log.e(TAG, "API returned non-success status or missing data field");
                return null;
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing order types response", e);
            return null;
        }
    }

    private List<OrderType> parseOrderTypesFromArray(JSONArray dataArray) throws JSONException {
        List<OrderType> orderTypes = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject orderTypeJson = dataArray.getJSONObject(i);
            OrderType orderType = new OrderType();
            orderType.setId(orderTypeJson.optLong("id"));
            orderType.setName(orderTypeJson.optString("name"));

            if (orderType.getId() > 0 && orderType.getName() != null && !orderType.getName().isEmpty()) {
                orderTypes.add(orderType);
            }
        }
        return orderTypes;
    }

    public void createOrder(long sessionId, String tableNumber, String customerName,
                            OrderType orderType, CreateOrderCallback callback) {
        try {
            JSONObject orderData = new JSONObject();
            orderData.put("table_number", tableNumber);
            orderData.put("cashier_session_id", sessionId);
            orderData.put("order_type_id", orderType.getId());

            if (customerName != null && !customerName.trim().isEmpty()) {
                orderData.put("customer_name", customerName);
            }

            RequestBody body = RequestBody.create(orderData.toString(), JSON);
            String authToken = getAuthToken();

            Request.Builder requestBuilder = new Request.Builder()
                    .url(ORDERS_API_URL)
                    .post(body);

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            Log.d(TAG, "Creating order: Table " + tableNumber + ", Type: " + orderType.getName());

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Order creation failed", e);
                    callback.onError("Network error occurred: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");

                            if ("success".equals(status)) {
                                String successMessage = "Order created successfully (" + orderType.getName() + ")";
                                Log.d(TAG, successMessage);
                                callback.onSuccess(successMessage);
                            } else {
                                String message = jsonResponse.optString("message", "Order creation failed");
                                Log.e(TAG, "Order creation failed: " + message);
                                callback.onError(message);
                            }
                        } else {
                            Log.e(TAG, "Order creation failed with code: " + response.code());
                            handleErrorResponse(responseBody, callback);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error processing order creation response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating order request", e);
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    private void handleErrorResponse(String responseBody, CreateOrderCallback callback) {
        String errorMessage;
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            errorMessage = errorJson.optString("message", "Order creation failed");

            // Log additional error details if available
            if (errorJson.has("errors")) {
                Log.e(TAG, "Order creation errors: " + errorJson.getJSONObject("errors").toString());
            }
        } catch (JSONException e) {
            errorMessage = "Order creation failed";
        }
        callback.onError(errorMessage);
    }

    private Order parseOrder(JSONObject orderJson) {
        try {
            Order order = new Order();
            order.setId(orderJson.optLong("id", -1));
            order.setTableNumber(orderJson.optString("table_number", ""));
            order.setOrderNumber(String.valueOf(order.getId()));

            // Parse status - use order_status_name instead of status
            String orderStatusName = orderJson.optString("order_status_name", "").toLowerCase();
            order.setStatus(orderStatusName);

            // Parse amounts with better error handling
            order.setTotalAmount(parseAmount(orderJson.optString("total_amount", "0")));
            order.setFinalAmount(parseAmount(orderJson.optString("final_amount",
                    String.valueOf(order.getTotalAmount()))));

            order.setCreatedAt(orderJson.optString("created_at", ""));

            // Parse customer info
            parseCustomerInfo(orderJson, order);

            order.setSessionId(orderJson.optLong("cashier_session_id", -1));
            order.setServerId(orderJson.optLong("server_id", -1));
            order.setOrderTypeId(orderJson.optLong("order_type_id", -1));
            order.setOrderTypeName(orderJson.optString("order_type_name", ""));

            // Parse order items
            if (orderJson.has("order_items")) {
                JSONArray itemsArray = orderJson.getJSONArray("order_items");
                List<OrderItem> orderItems = new ArrayList<>();

                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject itemJson = itemsArray.getJSONObject(i);

                    if (!itemJson.isNull("id")) {
                        OrderItem item = parseOrderItem(itemJson);
                        if (item != null) {
                            orderItems.add(item);
                        }
                    }
                }

                order.setItems(orderItems);
            }

            return order;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing order", e);
            return null;
        }
    }

    private double parseAmount(String amountStr) {
        try {
            return Double.parseDouble(amountStr.replace(",", ""));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to parse amount: " + amountStr);
            return 0.0;
        }
    }

    private void parseCustomerInfo(JSONObject orderJson, Order order) {
        if (!orderJson.isNull("customer_name") && !orderJson.optString("customer_name", "").isEmpty()) {
            order.setCustomerName(orderJson.optString("customer_name", ""));
        } else if (!orderJson.isNull("customer_id")) {
            long customerId = orderJson.optLong("customer_id", -1);
            if (customerId > 0) {
                order.setCustomerName("Customer #" + customerId);
            }
        }
    }

    private OrderItem parseOrderItem(JSONObject itemJson) {
        try {
            OrderItem item = new OrderItem();
            item.setId(itemJson.optLong("id", -1));
            item.setOrderId(itemJson.optLong("order_id", -1));
            item.setMenuItemId(itemJson.optLong("menu_item_id", -1));
            item.setMenuItemName(itemJson.optString("menu_item_name", ""));

            if (!itemJson.isNull("variant_id")) {
                item.setVariantId(itemJson.optLong("variant_id", -1));
            }

            if (!itemJson.isNull("variant_name") && !itemJson.optString("variant_name", "").isEmpty()) {
                item.setVariantName(itemJson.optString("variant_name", ""));
            } else {
                item.setVariantName(null);
            }

            item.setQuantity(itemJson.optInt("quantity", 0));
            item.setUnitPrice(itemJson.optDouble("unit_price", 0.0));
            item.setTotalPrice(itemJson.optDouble("total_price", 0.0));

            if (!itemJson.isNull("notes") && !itemJson.optString("notes", "").isEmpty()) {
                item.setNotes(itemJson.optString("notes", ""));
            } else {
                item.setNotes(null);
            }

            item.setStatus(itemJson.optString("status", ""));
            item.setKitchenPrinted(itemJson.optBoolean("kitchen_printed", false));
            item.setCreatedAt(itemJson.optString("created_at", ""));
            item.setUpdatedAt(itemJson.optString("updated_at", ""));

            return item;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing order item", e);
            return null;
        }
    }

    // Cache management methods
    private void cacheFreshOrderStatuses(List<OrderStatus> orderStatuses) {
        new Thread(() -> {
            try {
                databaseManager.saveOrderStatuses(orderStatuses);
                Log.d(TAG, "Cached " + orderStatuses.size() + " order statuses");
            } catch (Exception e) {
                Log.e(TAG, "Error caching order statuses", e);
            }
        }).start();
    }

    private void cacheFreshOrderTypes(List<OrderType> orderTypes) {
        new Thread(() -> {
            try {
                databaseManager.saveOrderTypes(orderTypes);
                Log.d(TAG, "Cached " + orderTypes.size() + " order types");
            } catch (Exception e) {
                Log.e(TAG, "Error caching order types", e);
            }
        }).start();
    }

    // Public methods for accessing cached data
    public List<OrderStatus> getOrderStatuses() {
        try {
            List<OrderStatus> statuses = databaseManager.getOrderStatuses();
            Log.d(TAG, "Retrieved " + statuses.size() + " cached order statuses");
            return statuses;
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached order statuses", e);
            return new ArrayList<>();
        }
    }

    public List<OrderType> getOrderTypes() {
        try {
            List<OrderType> types = databaseManager.getOrderTypes();
            Log.d(TAG, "Retrieved " + types.size() + " cached order types");
            return types;
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached order types", e);
            return new ArrayList<>();
        }
    }

    private String getAuthToken() {
        try {
            return context.getSharedPreferences(context.getString(R.string.pref_file_name), Context.MODE_PRIVATE)
                    .getString(context.getString(R.string.pref_token), "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting auth token", e);
            return "";
        }
    }
}