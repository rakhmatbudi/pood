package com.restaurant.management;

import android.app.Application;
import android.util.Log;

import com.restaurant.management.database.MenuItemDatabase;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RestaurantApplication extends Application {
    private static final String TAG = "RestaurantApplication";
    private static final String BASE_API_URL = "https://api.pood.lol/";

    private MenuItemDatabase database;
    private OkHttpClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started - downloading menu");

        database = new MenuItemDatabase(this);
        client = new OkHttpClient();

        // Download menu on every app start
        downloadMenuOnStart();
    }

    private void downloadMenuOnStart() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "No network - skipping download");
            return;
        }

        Log.d(TAG, "Network available - starting download");
        downloadMenuItems();
    }

    private void downloadMenuItems() {
        String apiUrl = BASE_API_URL + "menu-items";
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(apiUrl);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Download failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<ProductItem> items = parseMenuItems(jsonResponse);

                    Log.d(TAG, "Downloaded " + items.size() + " items");

                    // Check Affogato price in downloaded data
                    for (ProductItem item : items) {
                        if ("Affogato".equals(item.getName())) {
                            Log.d(TAG, "Affogato price from API: " + item.getPrice());
                            break;
                        }
                    }

                    database.saveMenuItems(items);
                    Log.d(TAG, "Saved to database");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing download: " + e.getMessage());
                }
            }
        });
    }

    private List<ProductItem> parseMenuItems(JSONObject jsonResponse) throws JSONException {
        List<ProductItem> items = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray itemsArray;

            if (jsonResponse.get("data") instanceof JSONArray) {
                itemsArray = jsonResponse.getJSONArray("data");
            } else {
                JSONObject singleItem = jsonResponse.getJSONObject("data");
                itemsArray = new JSONArray();
                itemsArray.put(singleItem);
            }

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                ProductItem item = new ProductItem();

                item.setId(itemJson.optLong("id", -1));
                item.setName(itemJson.optString("name", ""));
                item.setDescription(itemJson.optString("description", ""));
                item.setPrice(parsePrice(itemJson.optString("price", "0")));

                if (itemJson.has("category_name") && !itemJson.isNull("category_name")) {
                    item.setCategory(itemJson.optString("category_name", ""));
                } else if (itemJson.has("category") && !itemJson.isNull("category")) {
                    Object categoryObj = itemJson.get("category");
                    if (categoryObj instanceof JSONObject) {
                        JSONObject categoryJson = (JSONObject) categoryObj;
                        if (categoryJson.has("name")) {
                            item.setCategory(categoryJson.optString("name", ""));
                        }
                    } else {
                        item.setCategory(itemJson.optString("category", ""));
                    }
                }

                item.setActive(itemJson.optBoolean("is_active", true));
                item.setCreatedAt(itemJson.optString("created_at", ""));
                item.setUpdatedAt(itemJson.optString("updated_at", ""));

                if (itemJson.has("image_url") && !itemJson.isNull("image_url")) {
                    item.setImageUrl(itemJson.optString("image_url", ""));
                } else if (itemJson.has("image_path") && !itemJson.isNull("image_path")) {
                    item.setImageUrl(itemJson.optString("image_path", ""));
                }

                List<Variant> variants = new ArrayList<>();
                if (itemJson.has("variants") && !itemJson.isNull("variants")) {
                    JSONArray variantsArray = itemJson.getJSONArray("variants");

                    for (int j = 0; j < variantsArray.length(); j++) {
                        JSONObject variantJson = variantsArray.getJSONObject(j);
                        Variant variant = new Variant();
                        variant.setId(variantJson.optLong("id", -1));
                        variant.setName(variantJson.optString("name", ""));
                        variant.setPrice(parsePrice(variantJson.optString("price", "0")));
                        variant.setActive(variantJson.optBoolean("is_active", true));
                        variant.setCreatedAt(variantJson.optString("created_at", ""));
                        variant.setUpdatedAt(variantJson.optString("updated_at", ""));
                        variants.add(variant);
                    }
                }

                item.setVariants(variants);
                items.add(item);
            }

            Collections.sort(items, (item1, item2) ->
                    item1.getName().compareToIgnoreCase(item2.getName()));
        }

        return items;
    }

    private double parsePrice(String priceString) {
        try {
            return Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String getAuthToken() {
        try {
            return getSharedPreferences("restaurant_prefs", MODE_PRIVATE)
                    .getString("auth_token", "");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (database != null) {
            database.close();
        }
    }
}