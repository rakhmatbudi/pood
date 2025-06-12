package com.restaurant.management.helpers;

import android.content.Context;

import com.restaurant.management.models.Discount;
import com.restaurant.management.models.PaymentMethod;
import com.restaurant.management.models.RoundingConfig;

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

public class PaymentApiHelper {
    private static final String ROUNDING_API_URL = "https://api.pood.lol/roundings/values";
    private static final String PAYMENT_MODES_URL = "https://api.pood.lol/payment-modes";
    private static final String CHECKOUT_API_URL = "https://api.pood.lol/payments/checkout/";
    private static final String DISCOUNTS_API_URL = "https://api.pood.lol/discounts/";
    private static final String PAYMENTS_API_URL = "https://api.pood.lol/payments";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final OkHttpClient client;
    private final String authToken;

    public interface RoundingConfigCallback {
        void onSuccess(RoundingConfig config);
        void onError();
    }

    public interface DiscountsCallback {
        void onSuccess(List<Discount> discounts);
        void onError();
    }

    public interface PaymentMethodsCallback {
        void onSuccess(List<PaymentMethod> paymentMethods);
        void onError();
    }

    public interface CheckoutCallback {
        void onSuccess(CheckoutResponse response);
        void onError(String message);
    }

    public interface PaymentCallback {
        void onSuccess();
        void onError(String message);
    }

    public static class CheckoutResponse {
        public String orderNumber;
        public String tableNumber;
        public double finalAmount;
        public double originalAmount;
        public double discountAmount;

        public CheckoutResponse(String orderNumber, String tableNumber, double finalAmount,
                                double originalAmount, double discountAmount) {
            this.orderNumber = orderNumber;
            this.tableNumber = tableNumber;
            this.finalAmount = finalAmount;
            this.originalAmount = originalAmount;
            this.discountAmount = discountAmount;
        }
    }

    public PaymentApiHelper(Context context, String authToken) {
        this.context = context;
        this.authToken = authToken;
        this.client = new OkHttpClient();
    }

    public void fetchRoundingConfig(RoundingConfigCallback callback) {
        Request.Builder requestBuilder = new Request.Builder().url(ROUNDING_API_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        if (dataArray.length() > 0) {
                            JSONObject config = dataArray.getJSONObject(0);

                            int roundingBelow = config.optInt("rounding_below", 99);
                            int roundingDigit = config.optInt("rounding_digit", 1);
                            String description = config.optString("rounding_digit_description", "00 - Hundreds");
                            int roundingNumber = config.optInt("rounding_number", 100);

                            RoundingConfig roundingConfig = new RoundingConfig(roundingBelow, roundingDigit, description, roundingNumber);
                            callback.onSuccess(roundingConfig);
                        } else {
                            callback.onError();
                        }
                    } else {
                        callback.onError();
                    }
                } catch (Exception e) {
                    callback.onError();
                }
            }
        });
    }

    public void fetchDiscounts(DiscountsCallback callback) {
        Request.Builder requestBuilder = new Request.Builder().url(DISCOUNTS_API_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        List<Discount> discounts = parseDiscountsFromJson(jsonResponse);
                        callback.onSuccess(discounts);
                    } else {
                        callback.onError();
                    }
                } catch (Exception e) {
                    callback.onError();
                }
            }
        });
    }

    public void fetchPaymentMethods(PaymentMethodsCallback callback) {
        Request.Builder requestBuilder = new Request.Builder().url(PAYMENT_MODES_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        JSONArray dataArray = jsonResponse.getJSONArray("data");
                        List<PaymentMethod> paymentMethods = new ArrayList<>();

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject methodJson = dataArray.getJSONObject(i);

                            long id = methodJson.getLong("id");
                            long typeId = methodJson.getLong("payment_mode_type_id");
                            String description = methodJson.getString("description");
                            boolean isActive = methodJson.getBoolean("is_active");

                            if (!isActive) {
                                continue;
                            }

                            String code = getPaymentMethodCode((int) typeId);
                            PaymentMethod method = new PaymentMethod(String.valueOf(id), description, code);
                            paymentMethods.add(method);
                        }

                        if (paymentMethods.isEmpty()) {
                            callback.onError();
                        } else {
                            callback.onSuccess(paymentMethods);
                        }
                    } else {
                        callback.onError();
                    }
                } catch (Exception e) {
                    callback.onError();
                }
            }
        });
    }

    public void callCheckout(long orderId, Long discountId, CheckoutCallback callback) {
        String checkoutUrl = CHECKOUT_API_URL + orderId;

        try {
            JSONObject payload = new JSONObject();
            if (discountId != null && discountId != -1) {
                payload.put("discount_id", discountId);
            }

            RequestBody body = RequestBody.create(payload.toString(), JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(checkoutUrl)
                    .post(body);

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error. Please check your connection.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();

                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                JSONObject data = jsonResponse.getJSONObject("data");

                                String orderNumber = data.optString("order_number");
                                String tableNumber = data.optString("table_number");

                                double finalAmount = 0.0;
                                if (data.has("final_charged_amount")) {
                                    finalAmount = data.optDouble("final_charged_amount", 0.0);
                                } else if (data.has("final_amount")) {
                                    String finalAmountStr = data.optString("final_amount", "0");
                                    finalAmount = Double.parseDouble(finalAmountStr.replace(",", ""));
                                }

                                double originalAmount = finalAmount;
                                if (data.has("total_items_amount")) {
                                    originalAmount = data.optDouble("total_items_amount", finalAmount);
                                }

                                double discountAmount = data.optDouble("discount_amount", 0.0);

                                CheckoutResponse checkoutResponse = new CheckoutResponse(
                                        orderNumber, tableNumber, finalAmount, originalAmount, discountAmount);
                                callback.onSuccess(checkoutResponse);
                            } else {
                                callback.onError("API returned non-success status: " + jsonResponse.optString("message"));
                            }
                        } else {
                            callback.onError("HTTP error: " + response.code());
                        }
                    } catch (Exception e) {
                        callback.onError("Error loading order details: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            callback.onError("Error creating checkout request: " + e.getMessage());
        }
    }

    public void processPayment(long orderId, Long discountId, double amount,
                               String paymentMethodId, String notes, PaymentCallback callback) {
        try {
            JSONObject paymentData = new JSONObject();
            paymentData.put("order_id", orderId);

            if (discountId != null && discountId != -1) {
                paymentData.put("discount_id", discountId);
            }

            paymentData.put("amount", amount);
            paymentData.put("payment_mode", Integer.parseInt(paymentMethodId));
            paymentData.put("transaction_id", null);

            if (notes != null && !notes.trim().isEmpty()) {
                paymentData.put("notes", notes.trim());
            }

            RequestBody body = RequestBody.create(paymentData.toString(), JSON);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(PAYMENTS_API_URL)
                    .post(body);

            if (authToken != null && !authToken.isEmpty()) {
                requestBuilder.header("Authorization", "Bearer " + authToken);
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Network error. Please try again.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();

                    try {
                        if (response.isSuccessful()) {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            if ("success".equals(jsonResponse.optString("status"))) {
                                callback.onSuccess();
                            } else {
                                String message = jsonResponse.optString("message", "Payment failed");
                                callback.onError(message);
                            }
                        } else {
                            String errorMessage;
                            try {
                                JSONObject errorJson = new JSONObject(responseBody);
                                errorMessage = errorJson.optString("message", "Payment failed");
                            } catch (JSONException e) {
                                errorMessage = "Payment failed";
                            }
                            callback.onError(errorMessage);
                        }
                    } catch (JSONException e) {
                        callback.onError("Error processing payment response");
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Error creating payment request");
        }
    }

    private List<Discount> parseDiscountsFromJson(JSONObject jsonResponse) throws JSONException {
        List<Discount> discounts = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray discountsArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < discountsArray.length(); i++) {
                JSONObject discountJson = discountsArray.getJSONObject(i);

                long id = discountJson.optLong("id", -1);
                String name = discountJson.optString("name", "");
                String description = discountJson.optString("description", "");
                int amount = discountJson.optInt("amount", 0);

                Discount discount = new Discount(id, name, description, amount);
                discounts.add(discount);
            }
        }

        return discounts;
    }

    private String getPaymentMethodCode(int typeId) {
        switch (typeId) {
            case 1:
                return "cash";
            case 2:
                return "card";
            case 3:
                return "transfer";
            default:
                return "other";
        }
    }

    public static List<PaymentMethod> getDefaultPaymentMethods() {
        List<PaymentMethod> methods = new ArrayList<>();
        methods.add(new PaymentMethod("1", "Cash", "cash"));
        methods.add(new PaymentMethod("2", "Credit/Debit Card", "card"));
        methods.add(new PaymentMethod("3", "Mobile Payment", "mobile"));
        return methods;
    }
}