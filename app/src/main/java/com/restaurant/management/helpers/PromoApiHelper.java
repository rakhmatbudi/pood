package com.restaurant.management.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.restaurant.management.R;
import com.restaurant.management.models.Promo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PromoApiHelper {
    private static final String TAG = "PromoApiHelper";
    private static final String PROMOS_API_URL = "https://api.pood.lol/promos";

    private final Context context;
    private final OkHttpClient client;

    public interface PromoCallback {
        void onSuccess(List<Promo> promos);
        void onError(String message);
    }

    public PromoApiHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void fetchActivePromos(PromoCallback callback) {
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(PROMOS_API_URL);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch promos", e);
                callback.onError("Failed to load promotions. Please check your connection.");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Promos API Response: " + responseBody);

                    if (response.isSuccessful()) {
                        JSONObject jsonResponse = new JSONObject(responseBody);

                        if (!"success".equals(jsonResponse.optString("status"))) {
                            throw new JSONException("API returned non-success status");
                        }

                        List<Promo> promos = parsePromosFromJson(jsonResponse);
                        callback.onSuccess(promos);
                    } else {
                        callback.onError("Failed to load promotions (HTTP " + response.code() + ")");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing promos response", e);
                    callback.onError("Error processing promotions data");
                }
            }
        });
    }

    private List<Promo> parsePromosFromJson(JSONObject jsonResponse) throws JSONException {
        List<Promo> promos = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray promosArray = jsonResponse.getJSONArray("data");

            for (int i = 0; i < promosArray.length(); i++) {
                JSONObject promoJson = promosArray.getJSONObject(i);

                long promoId = promoJson.optLong("promo_id", -1);
                String promoName = promoJson.optString("promo_name", "");
                String promoDescription = promoJson.optString("promo_description", "");
                String startDate = promoJson.optString("start_date", "");
                String endDate = promoJson.optString("end_date", "");
                String termAndCondition = promoJson.optString("term_and_condition", "");
                String picture = promoJson.optString("picture", "");
                String type = promoJson.optString("type", "");
                String discountType = promoJson.optString("discount_type", "");
                String discountAmount = promoJson.optString("discount_amount", "");
                boolean isActive = promoJson.optBoolean("is_active", false);

                // Parse promo items
                List<Promo.PromoItem> promoItems = new ArrayList<>();
                if (promoJson.has("promo_items") && !promoJson.isNull("promo_items")) {
                    JSONArray itemsArray = promoJson.getJSONArray("promo_items");
                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemJson = itemsArray.getJSONObject(j);
                        long id = itemJson.optLong("id", -1);
                        long itemId = itemJson.optLong("item_id", -1);
                        String itemName = itemJson.optString("item_name", "");

                        if (id != -1 && itemId != -1 && !itemName.isEmpty()) {
                            promoItems.add(new Promo.PromoItem(id, itemId, itemName));
                        }
                    }
                }

                // Only include active promos or all promos based on your requirement
                if (promoId != -1 && !promoName.isEmpty()) {
                    Promo promo = new Promo(promoId, promoName, promoDescription, startDate,
                            endDate, termAndCondition, picture, type, discountType,
                            discountAmount, isActive, promoItems);
                    promos.add(promo);
                }
            }
        }

        return promos;
    }

    private String getAuthToken() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                context.getString(R.string.pref_file_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.pref_token), "");
    }
}