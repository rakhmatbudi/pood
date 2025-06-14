package com.restaurant.management.helpers;

import android.content.Context;

import com.restaurant.management.R;
import com.restaurant.management.database.PoodDatabase;
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
    private static final String ORDER_TYPES_API_URL = "https://api.pood.lol/order-types/";
    private static final String ORDER_STATUSES_API_URL = "https://api.pood.lol/order-statuses";
    private static final String ORDERS_API_URL = "https://api.pood.lol/orders";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient client;

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
    }

    private static OkHttpClient createHttpClient(Context context) {
        try {
            Retrofit retrofit = ApiClient.getClient(context);
            return (OkHttpClient) retrofit.callFactory();
        } catch (Exception e) {
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

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error occurred");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        callback.onError("Unexpected response code: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    if (!jsonResponse.has("data")) {
                        callback.onError("Response missing 'data' field");
                        return;
                    }

                    JSONArray ordersArray = jsonResponse.getJSONArray("data");
                    List<Order> orders = new ArrayList<>();

                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject orderJson = ordersArray.getJSONObject(i);
                        Order order = parseOrder(orderJson);
                        orders.add(order);
                    }

                    callback.onSuccess(orders);

                } catch (Exception e) {
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

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to fetch order statuses");
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

                        callback.onSuccess(orderStatuses);
                    } else {
                        callback.onError("HTTP error: " + response.code());
                    }
                } catch (Exception e) {
                    callback.onError("Error parsing order statuses");
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

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null) {
                    callback.onError("Failed to fetch order types: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        if (callback != null) {
                            callback.onError("HTTP error: " + response.code());
                        }
                        return;
                    }

                    String responseBody = response.body().string();

                    // Try to parse as array first (direct array response)
                    try {
                        JSONArray dataArray = new JSONArray(responseBody);
                        List<OrderType> orderTypes = parseOrderTypesFromArray(dataArray);
                        if (callback != null) {
                            callback.onSuccess(orderTypes);
                        }
                        return;
                    } catch (JSONException e) {
                        // Not a direct array, try object format
                    }

                    // Try to parse as object with data field
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if ("success".equals(jsonResponse.optString("status")) && jsonResponse.has("data")) {
                            JSONArray dataArray = jsonResponse.getJSONArray("data");
                            List<OrderType> orderTypes = parseOrderTypesFromArray(dataArray);
                            if (callback != null) {
                                callback.onSuccess(orderTypes);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("API returned non-success status");
                            }
                        }
                    } catch (JSONException e) {
                        if (callback != null) {
                            callback.onError("Error parsing response");
                        }
                    }

                } catch (Exception e) {
                    if (callback != null) {
                        callback.onError("Error processing response");
                    }
                }
            }
        });
    }

    private List<OrderType> parseOrderTypesFromArray(JSONArray dataArray) throws JSONException {
        List<OrderType> orderTypes = new ArrayList<>();
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject orderTypeJson = dataArray.getJSONObject(i);
            OrderType orderType = new OrderType();
            orderType.setId(orderTypeJson.optLong("id"));
            orderType.setName(orderTypeJson.optString("name"));
            orderTypes.add(orderType);
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

            if (!customerName.isEmpty()) {
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

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error occurred");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");

                            if ("success".equals(status)) {
                                callback.onSuccess("Order created successfully (" + orderType.getName() + ")");
                            } else {
                                String message = jsonResponse.optString("message", "Order creation failed");
                                callback.onError(message);
                            }
                        } else {
                            handleErrorResponse(responseBody, callback);
                        }
                    } catch (JSONException e) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    private void handleErrorResponse(String responseBody, CreateOrderCallback callback) {
        String errorMessage;
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            errorMessage = errorJson.optString("message", "Order creation failed");
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

            // Parse amounts
            String totalAmountStr = orderJson.optString("total_amount", "0").replace(",", "");
            try {
                order.setTotalAmount(Double.parseDouble(totalAmountStr));
            } catch (NumberFormatException e) {
                order.setTotalAmount(0.0);
            }

            String finalAmountStr = orderJson.optString("final_amount", "0").replace(",", "");
            try {
                order.setFinalAmount(Double.parseDouble(finalAmountStr));
            } catch (NumberFormatException e) {
                order.setFinalAmount(order.getTotalAmount());
            }

            order.setCreatedAt(orderJson.optString("created_at", ""));

            // Parse customer info
            if (!orderJson.isNull("customer_name") && !orderJson.optString("customer_name", "").isEmpty()) {
                order.setCustomerName(orderJson.optString("customer_name", ""));
            } else if (!orderJson.isNull("customer_id")) {
                long customerId = orderJson.optLong("customer_id", -1);
                if (customerId > 0) {
                    order.setCustomerName("Customer #" + customerId);
                }
            }

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

                    if (itemJson.isNull("id")) {
                        continue;
                    }

                    OrderItem item = parseOrderItem(itemJson);
                    orderItems.add(item);
                }

                order.setItems(orderItems);
            }

            return order;
        } catch (Exception e) {
            return new Order();
        }
    }

    private OrderItem parseOrderItem(JSONObject itemJson) {
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
    }

    public List<OrderStatus> getOrderStatuses() {
        PoodDatabase database = new PoodDatabase(context);
        return database.getOrderStatuses();
    }

    private String getAuthToken() {
        return context.getSharedPreferences(context.getString(R.string.pref_file_name), Context.MODE_PRIVATE)
                .getString(context.getString(R.string.pref_token), "");
    }
}