package com.restaurant.management;

import android.app.Application;
import android.util.Log;

import com.restaurant.management.database.MenuItemDatabase;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.utils.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
    private AtomicInteger pendingRequests = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App started - downloading menu data");

        database = new MenuItemDatabase(this);
        client = new OkHttpClient();

        // Download both menu items and categories on every app start
        downloadMenuDataOnStart();
    }

    private void downloadMenuDataOnStart() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Log.d(TAG, "No network - skipping download");
            return;
        }

        Log.d(TAG, "Network available - starting downloads");

        // Download both categories and menu items
        pendingRequests.set(2); // We have 2 API calls to make
        downloadMenuCategories();
        downloadMenuItems();
    }

    private void downloadMenuCategories() {
        String apiUrl = BASE_API_URL + "menu-categories";
        String authToken = getAuthToken();

        Request.Builder requestBuilder = new Request.Builder().url(apiUrl);

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Categories download failed: " + e.getMessage());
                decrementPendingRequests();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Categories server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<MenuCategory> categories = parseMenuCategories(jsonResponse);

                    Log.d(TAG, "Downloaded " + categories.size() + " categories");

                    database.saveMenuCategories(categories);
                    Log.d(TAG, "Categories saved to database");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing categories download: " + e.getMessage());
                } finally {
                    decrementPendingRequests();
                }
            }
        });
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
                Log.e(TAG, "Menu items download failed: " + e.getMessage());
                decrementPendingRequests();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Menu items server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<ProductItem> items = parseMenuItems(jsonResponse);

                    Log.d(TAG, "Downloaded " + items.size() + " menu items");

                    // Check Affogato price in downloaded data
                    for (ProductItem item : items) {
                        if ("Affogato".equals(item.getName())) {
                            Log.d(TAG, "Affogato price from API: " + item.getPrice());
                            break;
                        }
                    }

                    database.saveMenuItems(items);
                    Log.d(TAG, "Menu items saved to database");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing menu items download: " + e.getMessage());
                } finally {
                    decrementPendingRequests();
                }
            }
        });
    }

    private List<MenuCategory> parseMenuCategories(JSONObject jsonResponse) throws JSONException {
        List<MenuCategory> categories = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray categoriesArray;

            if (jsonResponse.get("data") instanceof JSONArray) {
                categoriesArray = jsonResponse.getJSONArray("data");
            } else {
                JSONObject singleCategory = jsonResponse.getJSONObject("data");
                categoriesArray = new JSONArray();
                categoriesArray.put(singleCategory);
            }

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryJson = categoriesArray.getJSONObject(i);
                MenuCategory category = new MenuCategory();

                category.setId(categoryJson.optLong("id", -1));
                category.setName(categoryJson.optString("name", ""));
                category.setDescription(categoryJson.optString("description", ""));
                category.setCreatedAt(categoryJson.optString("created_at", ""));
                category.setUpdatedAt(categoryJson.optString("updated_at", ""));

                // Map API fields to your MenuCategory fields
                category.setDisplayed(categoryJson.optBoolean("is_displayed", true));
                category.setHighlight(categoryJson.optBoolean("is_highlight", false));
                category.setDisplayForSelfOrder(categoryJson.optBoolean("is_display_for_self_order", true));
                category.setSkuId(categoryJson.optString("sku_id", ""));
                category.setMenuCategoryGroup(categoryJson.optString("menu_category_group", ""));

                // Handle display picture - check multiple possible field names
                if (categoryJson.has("display_picture") && !categoryJson.isNull("display_picture")) {
                    category.setDisplayPicture(categoryJson.optString("display_picture", ""));
                } else if (categoryJson.has("image_url") && !categoryJson.isNull("image_url")) {
                    category.setDisplayPicture(categoryJson.optString("image_url", ""));
                } else if (categoryJson.has("image_path") && !categoryJson.isNull("image_path")) {
                    category.setDisplayPicture(categoryJson.optString("image_path", ""));
                }

                categories.add(category);
            }

            // Sort categories by name
            Collections.sort(categories, (cat1, cat2) ->
                    cat1.getName().compareToIgnoreCase(cat2.getName()));
        }

        return categories;
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

    private void decrementPendingRequests() {
        int remaining = pendingRequests.decrementAndGet();
        if (remaining == 0) {
            Log.d(TAG, "All menu data downloads completed");
            onAllDownloadsComplete();
        }
    }

    private void onAllDownloadsComplete() {
        // This method is called when both categories and menu items have been downloaded
        // You can add any additional logic here that should run after all data is loaded
        Log.d(TAG, "Menu initialization complete - app ready to use");
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