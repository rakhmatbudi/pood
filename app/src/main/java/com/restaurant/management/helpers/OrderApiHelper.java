package com.restaurant.management.helpers;

import android.content.Context;
import android.util.Log;

import com.restaurant.management.R;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderItem;

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

public class OrderApiHelper {
    private static final String TAG = "OrderApiHelper";
    private static final String BASE_API_URL = "https://api.pood.lol/orders/";

    private final Context context;
    private final OkHttpClient client;

    public interface OrderCallback {
        void onSuccess(Order order, String updatedAt);
        void onError(String errorMessage);
    }

    public interface CancelCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public OrderApiHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void fetchOrderDetails(long orderId, OrderCallback callback) {
        String apiUrl = BASE_API_URL + orderId + "?t=" + System.currentTimeMillis();
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
                Log.e(TAG, "API request failed", e);
                callback.onError("Network error: " + e.getMessage());
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
                    JSONObject orderData = jsonResponse.getJSONObject("data");
                    Order order = parseOrder(orderData);
                    String updatedAt = orderData.optString("update_at", "");

                    callback.onSuccess(order, updatedAt);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    callback.onError("Error processing response: " + e.getMessage());
                }
            }
        });
    }

    public void cancelOrder(long orderId, CancelCallback callback) {
        String cancelUrl = BASE_API_URL + orderId + "/cancel";
        String authToken = getAuthToken();

        RequestBody emptyBody = RequestBody.create(new byte[0], MediaType.parse("application/json"));
        Request.Builder requestBuilder = new Request.Builder()
                .url(cancelUrl)
                .put(emptyBody);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Cancel order request failed", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "No response body";

                    if (response.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String errorMessage = "Failed to cancel order (Code: " + response.code() + ")";
                        try {
                            if (responseBody.contains("message")) {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("message", errorMessage);
                            } else if (responseBody.contains("error")) {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("error", errorMessage);
                            }
                        } catch (Exception e) {
                            // Use default error message
                        }
                        callback.onError(errorMessage);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing cancel response", e);
                    callback.onError("Error cancelling order: " + e.getMessage());
                }
            }
        });
    }

    private Order parseOrder(JSONObject orderJson) throws JSONException {
        Order order = new Order();

        order.setId(orderJson.optLong("id", -1));
        order.setTableNumber(orderJson.optString("table_number", ""));
        order.setOrderNumber(String.valueOf(order.getId()));

        String totalAmountStr = orderJson.optString("total_amount", "0.0").replace(",", "");
        try {
            order.setTotalAmount(Double.parseDouble(totalAmountStr));
        } catch (NumberFormatException e) {
            order.setTotalAmount(0.0);
        }

        String finalAmountStr = orderJson.optString("final_amount", "0.0").replace(",", "");
        try {
            order.setFinalAmount(Double.parseDouble(finalAmountStr));
        } catch (NumberFormatException e) {
            double serviceCharge = orderJson.optDouble("service_charge", 0.0);
            double taxAmount = orderJson.optDouble("tax_amount", 0.0);
            double discountAmount = orderJson.optDouble("discount_amount", 0.0);
            double finalAmount = order.getTotalAmount() + serviceCharge + taxAmount - discountAmount;
            order.setFinalAmount(finalAmount);
        }

        // Parse status - use order_status_name instead of status
        String orderStatusName = orderJson.optString("order_status_name", "").toLowerCase();
        order.setStatus(orderStatusName);

        order.setCreatedAt(orderJson.optString("created_at", ""));

        if (!orderJson.isNull("customer_name")) {
            order.setCustomerName(orderJson.optString("customer_name", ""));
        } else if (!orderJson.isNull("customer_id")) {
            long customerId = orderJson.optLong("customer_id", -1);
            if (customerId > 0) {
                order.setCustomerName("Customer #" + customerId);
            } else {
                order.setCustomerName(null);
            }
        } else {
            order.setCustomerName(null);
        }

        order.setServerId(orderJson.optLong("server_id", -1));
        order.setSessionId(orderJson.optLong("cashier_session_id", -1));

        if (!orderJson.isNull("order_type_id")) {
            order.setOrderTypeId(orderJson.optLong("order_type_id", -1));
        }

        if (!orderJson.isNull("order_type_name")) {
            order.setOrderTypeName(orderJson.optString("order_type_name", ""));
        }

        List<OrderItem> orderItems = new ArrayList<>();
        if (orderJson.has("order_items") && !orderJson.isNull("order_items")) {
            JSONArray itemsArray = orderJson.getJSONArray("order_items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                OrderItem item = parseOrderItem(itemJson);
                orderItems.add(item);
            }
        }

        order.setItems(orderItems);
        return order;
    }

    private OrderItem parseOrderItem(JSONObject itemJson) {
        OrderItem item = new OrderItem();

        item.setId(itemJson.optLong("id", -1));
        item.setOrderId(itemJson.optLong("order_id", -1));
        item.setMenuItemId(itemJson.optLong("menu_item_id", -1));
        item.setMenuItemName(itemJson.optString("menu_item_name", ""));

        if (!itemJson.isNull("variant_id")) {
            item.setVariantId(itemJson.optLong("variant_id", -1));
        } else {
            item.setVariantId(null);
        }

        if (!itemJson.isNull("variant_name")) {
            item.setVariantName(itemJson.optString("variant_name", ""));
        } else {
            item.setVariantName(null);
        }

        item.setQuantity(itemJson.optInt("quantity", 0));
        item.setUnitPrice(itemJson.optDouble("unit_price", 0.0));
        item.setTotalPrice(itemJson.optDouble("total_price", 0.0));
        item.setNotes(itemJson.optString("notes", ""));
        item.setStatus(itemJson.optString("status", ""));
        item.setKitchenPrinted(itemJson.optBoolean("kitchen_printed", false));
        item.setCreatedAt(itemJson.optString("created_at", ""));
        item.setUpdatedAt(itemJson.optString("updated_at", ""));

        return item;
    }

    private String getAuthToken() {
        return context.getSharedPreferences(context.getString(R.string.pref_file_name), Context.MODE_PRIVATE)
                .getString(context.getString(R.string.pref_token), "");
    }
}