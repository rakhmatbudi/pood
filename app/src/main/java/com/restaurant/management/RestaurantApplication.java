package com.restaurant.management;

import android.app.Application;
import android.util.Log;

import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.models.Promo;
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

    private PoodDatabase database;
    private OkHttpClient client;
    private AtomicInteger pendingRequests = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();

        database = new PoodDatabase(this);
        client = new OkHttpClient();

        // Download menu items, categories, and promos on every app start
        downloadAllDataOnStart();
    }

    private void downloadAllDataOnStart() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        // Download categories, menu items, and promos
        pendingRequests.set(3); // We have 3 API calls to make
        downloadMenuCategories();
        downloadMenuItems();
        downloadPromos();
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

                    database.saveMenuCategories(categories);

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

                    database.saveMenuItems(items);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing menu items download: " + e.getMessage());
                } finally {
                    decrementPendingRequests();
                }
            }
        });
    }

    private void downloadPromos() {
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
                Log.e(TAG, "Promos download failed: " + e.getMessage());
                decrementPendingRequests();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Promos server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<Promo> promos = parsePromos(jsonResponse);

                    database.savePromos(promos);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing promos download: " + e.getMessage());
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

    private List<Promo> parsePromos(JSONObject jsonResponse) throws JSONException {
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

    private void decrementPendingRequests() {
        int remaining = pendingRequests.decrementAndGet();
        if (remaining == 0) {
            onAllDownloadsComplete();
        }
    }

    private void onAllDownloadsComplete() {
        // This method is called when categories, menu items, and promos have been downloaded
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