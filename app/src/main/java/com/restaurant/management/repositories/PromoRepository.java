package com.restaurant.management.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.models.Promo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PromoRepository {
    private static final String TAG = "PromoRepository";
    private static final String BASE_API_URL = "https://api.pood.lol/";

    private PoodDatabase database;
    private Context context;
    private ExecutorService executor;
    private Handler mainHandler;
    private OkHttpClient client;

    public interface PromoCallback {
        void onSuccess(List<Promo> promos);
        void onError(String message);
    }

    public PromoRepository(Context context) {
        this.context = context;
        this.database = new PoodDatabase(context);
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient();
    }

    /**
     * Get promos from offline database
     * This is the primary method that should be called from the UI
     */
    public void getOfflinePromos(PromoCallback callback) {
        executor.execute(() -> {
            try {
                List<Promo> promos = database.getAllActivePromos();

                // Switch back to main thread for callback
                mainHandler.post(() -> {
                    Log.d(TAG, "Retrieved " + promos.size() + " promos from offline database");
                    callback.onSuccess(promos);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error getting offline promos", e);

                // Switch back to main thread for callback
                mainHandler.post(() -> {
                    callback.onError("Failed to retrieve promos from database: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Fetch promos from API and save to database
     * This method can be used for manual refresh scenarios
     */
    public void fetchActivePromos(PromoCallback callback) {
        String apiUrl = BASE_API_URL + "promos";
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(apiUrl);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API fetch failed, falling back to offline data", e);

                // Fall back to offline data when API fails
                getOfflinePromos(callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + response.code() + ", falling back to offline data");
                        getOfflinePromos(callback);
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<Promo> promos = parsePromos(jsonResponse);

                    Log.d(TAG, "Fetched " + promos.size() + " promos from API");

                    // Save to database for future offline use
                    executor.execute(() -> {
                        try {
                            database.savePromos(promos);
                            Log.d(TAG, "API promos saved to database");
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving API promos to database", e);
                        }
                    });

                    // Return the fresh data
                    mainHandler.post(() -> {
                        callback.onSuccess(promos);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing API response, falling back to offline data", e);
                    getOfflinePromos(callback);
                }
            }
        });
    }

    /**
     * Parse promos from JSON response
     */
    private List<Promo> parsePromos(JSONObject jsonResponse) throws Exception {
        List<Promo> promos = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray promosArray;

            if (jsonResponse.get("data") instanceof JSONArray) {
                promosArray = jsonResponse.getJSONArray("data");
            } else {
                JSONObject singlePromo = jsonResponse.getJSONObject("data");
                promosArray = new JSONArray();
                promosArray.put(singlePromo);
            }

            for (int i = 0; i < promosArray.length(); i++) {
                JSONObject promoJson = promosArray.getJSONObject(i);
                Promo promo = new Promo();

                // Map API fields to your Promo class fields
                promo.setPromoId(promoJson.optLong("promo_id", -1));
                promo.setPromoName(promoJson.optString("promo_name", ""));
                promo.setPromoDescription(promoJson.optString("promo_description", ""));
                promo.setStartDate(promoJson.optString("start_date", ""));
                promo.setEndDate(promoJson.optString("end_date", ""));
                promo.setTermAndCondition(promoJson.optString("term_and_condition", ""));
                promo.setType(promoJson.optString("type", ""));
                promo.setDiscountType(promoJson.optString("discount_type", ""));
                promo.setDiscountAmount(promoJson.optString("discount_amount", ""));
                promo.setActive(promoJson.optBoolean("is_active", false));

                // Handle image/picture field - check multiple possible field names
                if (promoJson.has("picture") && !promoJson.isNull("picture")) {
                    promo.setPicture(promoJson.optString("picture", ""));
                } else if (promoJson.has("image_url") && !promoJson.isNull("image_url")) {
                    promo.setPicture(promoJson.optString("image_url", ""));
                } else if (promoJson.has("image_path") && !promoJson.isNull("image_path")) {
                    promo.setPicture(promoJson.optString("image_path", ""));
                }

                // Handle promo items if they exist in the API response
                if (promoJson.has("promo_items") && !promoJson.isNull("promo_items")) {
                    JSONArray itemsArray = promoJson.getJSONArray("promo_items");
                    List<Promo.PromoItem> promoItems = new ArrayList<>();

                    for (int j = 0; j < itemsArray.length(); j++) {
                        JSONObject itemJson = itemsArray.getJSONObject(j);
                        Promo.PromoItem item = new Promo.PromoItem(
                                itemJson.optLong("id", -1),
                                itemJson.optLong("item_id", -1),
                                itemJson.optString("item_name", "")
                        );
                        promoItems.add(item);
                    }
                    promo.setPromoItems(promoItems);
                }

                // Only add active promos
                if (promo.isActive()) {
                    promos.add(promo);
                }
            }

            // Sort promos by name
            Collections.sort(promos, (promo1, promo2) ->
                    promo1.getPromoName().compareToIgnoreCase(promo2.getPromoName()));
        }

        return promos;
    }

    public void testDirectDatabaseAccess(PromoCallback callback) {
        Log.d(TAG, "testDirectDatabaseAccess called");

        try {
            // Call the simple database method directly
            List<Promo> promos = database.getAllActivePromosSimple();

            Log.d(TAG, "Direct database access returned " + promos.size() + " promos");

            // Call callback on main thread
            mainHandler.post(() -> {
                callback.onSuccess(promos);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in testDirectDatabaseAccess", e);

            mainHandler.post(() -> {
                callback.onError("Direct access error: " + e.getMessage());
            });
        }
    }

    /**
     * Get authentication token from shared preferences
     */
    private String getAuthToken() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("restaurant_prefs", Context.MODE_PRIVATE);
            return prefs.getString("auth_token", "");
        } catch (Exception e) {
            Log.e(TAG, "Error getting auth token", e);
            return "";
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }
}