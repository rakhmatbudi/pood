package com.restaurant.management;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.util.Log;

import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.models.Promo;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.models.OrderStatus;
import com.restaurant.management.utils.NetworkUtils;
import com.restaurant.management.receivers.NetworkChangeReceiver;
import com.restaurant.management.services.OfflineSyncService;

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

    private NetworkChangeReceiver networkChangeReceiver;
    private static final long SYNC_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes

    private PoodDatabase database;
    private OkHttpClient client;
    private AtomicInteger pendingRequests = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();

        database = new PoodDatabase(this);
        client = new OkHttpClient();

        // Register network change receiver
        registerNetworkChangeReceiver();

        // Schedule periodic sync
        schedulePeriodicSync();

        // Download all data on every app start
        downloadAllDataOnStart();

        // Start initial sync if network is available
        if (NetworkUtils.isNetworkAvailable(this)) {
            startSyncService();
        }
    }

    private void registerNetworkChangeReceiver() {
        networkChangeReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
        Log.d(TAG, "Network change receiver registered");
    }

    private void schedulePeriodicSync() {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, OfflineSyncService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Schedule repeating sync every 30 minutes
            alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + SYNC_INTERVAL_MS,
                    SYNC_INTERVAL_MS,
                    pendingIntent
            );

            Log.d(TAG, "Periodic sync scheduled");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling periodic sync", e);
        }
    }

    /**
     * Start sync service manually
     */
    public void startSyncService() {
        try {
            Intent syncIntent = new Intent(this, OfflineSyncService.class);
            startService(syncIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting sync service", e);
        }
    }

    /**
     * Get count of unsynced order items
     */
    public void getUnsyncedItemsCount(UnsyncedCountCallback callback) {
        new Thread(() -> {
            try {
                int count = database.getUnsyncedOrderItems().size();
                callback.onResult(count);
            } catch (Exception e) {
                Log.e(TAG, "Error getting unsynced items count", e);
                callback.onResult(0);
            }
        }).start();
    }

    /**
     * Force sync all unsynced items
     */
    public void forceSyncNow() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            startSyncService();
        } else {
            Log.w(TAG, "Cannot force sync - no network available");
        }
    }

    /**
     * Check if there are pending offline items
     */
    public void hasPendingOfflineItems(PendingItemsCallback callback) {
        new Thread(() -> {
            try {
                boolean hasPending = !database.getUnsyncedOrderItems().isEmpty();
                callback.onResult(hasPending);
            } catch (Exception e) {
                Log.e(TAG, "Error checking pending offline items", e);
                callback.onResult(false);
            }
        }).start();
    }

    // Callback interfaces
    public interface UnsyncedCountCallback {
        void onResult(int count);
    }

    public interface PendingItemsCallback {
        void onResult(boolean hasPending);
    }

    private void downloadAllDataOnStart() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            return;
        }

        // Download categories, menu items, promos, order types, and order statuses
        pendingRequests.set(5);
        downloadMenuCategories();
        downloadMenuItems();
        downloadPromos();
        downloadOrderTypes();
        downloadOrderStatuses();
    }

    private void downloadMenuCategories() {
        String apiUrl = BASE_API_URL + "menu-categories";

        Request request = new Request.Builder().url(apiUrl).build();

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

        Request request = new Request.Builder().url(apiUrl).build();

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

        Request request = new Request.Builder().url(apiUrl).build();

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

    private void downloadOrderTypes() {
        String apiUrl = BASE_API_URL + "order-types";

        Request request = new Request.Builder().url(apiUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Order types download failed: " + e.getMessage());
                decrementPendingRequests();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Order types server error: " + response.code());
                        return;
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<OrderType> orderTypes = parseOrderTypes(jsonResponse);

                    database.saveOrderTypes(orderTypes);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing order types download: " + e.getMessage());
                } finally {
                    decrementPendingRequests();
                }
            }
        });
    }

    private void downloadOrderStatuses() {
        String apiUrl = BASE_API_URL + "order-statuses";

        Request request = new Request.Builder().url(apiUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Order statuses download failed: " + e.getMessage());
                decrementPendingRequests();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Order statuses server error: " + response.code());
                        return;
                    }

                    // Try parsing as direct array first, then as object
                    List<OrderStatus> orderStatuses;
                    try {
                        orderStatuses = parseOrderStatusesFromArray(responseBody);
                    } catch (Exception e) {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        orderStatuses = parseOrderStatuses(jsonResponse);
                    }

                    database.saveOrderStatuses(orderStatuses);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing order statuses download: " + e.getMessage());
                } finally {
                    decrementPendingRequests();
                }
            }
        });
    }

    private List<OrderType> parseOrderTypes(JSONObject jsonResponse) throws JSONException {
        List<OrderType> orderTypes = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray typesArray;

            if (jsonResponse.get("data") instanceof JSONArray) {
                typesArray = jsonResponse.getJSONArray("data");
            } else {
                JSONObject singleType = jsonResponse.getJSONObject("data");
                typesArray = new JSONArray();
                typesArray.put(singleType);
            }

            for (int i = 0; i < typesArray.length(); i++) {
                JSONObject typeJson = typesArray.getJSONObject(i);
                OrderType orderType = new OrderType();

                orderType.setId(typeJson.optLong("id", -1));
                orderType.setName(typeJson.optString("name", ""));

                if (orderType.getId() > 0 && !orderType.getName().isEmpty()) {
                    orderTypes.add(orderType);
                }
            }

            Collections.sort(orderTypes, (type1, type2) ->
                    type1.getName().compareToIgnoreCase(type2.getName()));
        }

        return orderTypes;
    }

    private List<OrderStatus> parseOrderStatuses(JSONObject jsonResponse) throws JSONException {
        List<OrderStatus> orderStatuses = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray statusesArray;

            if (jsonResponse.get("data") instanceof JSONArray) {
                statusesArray = jsonResponse.getJSONArray("data");
            } else {
                JSONObject singleStatus = jsonResponse.getJSONObject("data");
                statusesArray = new JSONArray();
                statusesArray.put(singleStatus);
            }

            for (int i = 0; i < statusesArray.length(); i++) {
                JSONObject statusJson = statusesArray.getJSONObject(i);
                OrderStatus orderStatus = new OrderStatus();

                orderStatus.setId(statusJson.optLong("id", -1));
                orderStatus.setName(statusJson.optString("name", ""));

                if (orderStatus.getId() > 0 && !orderStatus.getName().isEmpty()) {
                    orderStatuses.add(orderStatus);
                }
            }
        }

        Collections.sort(orderStatuses, (status1, status2) ->
                status1.getName().compareToIgnoreCase(status2.getName()));

        return orderStatuses;
    }

    private List<OrderStatus> parseOrderStatusesFromArray(String responseBody) throws JSONException {
        List<OrderStatus> orderStatuses = new ArrayList<>();

        JSONArray statusesArray = new JSONArray(responseBody);

        for (int i = 0; i < statusesArray.length(); i++) {
            JSONObject statusJson = statusesArray.getJSONObject(i);
            OrderStatus orderStatus = new OrderStatus();

            orderStatus.setId(statusJson.optLong("id", -1));
            orderStatus.setName(statusJson.optString("name", ""));

            if (orderStatus.getId() > 0 && !orderStatus.getName().isEmpty()) {
                orderStatuses.add(orderStatus);
            }
        }

        Collections.sort(orderStatuses, (status1, status2) ->
                status1.getName().compareToIgnoreCase(status2.getName()));

        return orderStatuses;
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

                category.setDisplayed(categoryJson.optBoolean("is_displayed", true));
                category.setHighlight(categoryJson.optBoolean("is_highlight", false));
                category.setDisplayForSelfOrder(categoryJson.optBoolean("is_display_for_self_order", true));
                category.setSkuId(categoryJson.optString("sku_id", ""));
                category.setMenuCategoryGroup(categoryJson.optString("menu_category_group", ""));

                if (categoryJson.has("display_picture") && !categoryJson.isNull("display_picture")) {
                    category.setDisplayPicture(categoryJson.optString("display_picture", ""));
                } else if (categoryJson.has("image_url") && !categoryJson.isNull("image_url")) {
                    category.setDisplayPicture(categoryJson.optString("image_url", ""));
                } else if (categoryJson.has("image_path") && !categoryJson.isNull("image_path")) {
                    category.setDisplayPicture(categoryJson.optString("image_path", ""));
                }

                categories.add(category);
            }

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

                if (promoJson.has("picture") && !promoJson.isNull("picture")) {
                    promo.setPicture(promoJson.optString("picture", ""));
                } else if (promoJson.has("image_url") && !promoJson.isNull("image_url")) {
                    promo.setPicture(promoJson.optString("image_url", ""));
                } else if (promoJson.has("image_path") && !promoJson.isNull("image_path")) {
                    promo.setPicture(promoJson.optString("image_path", ""));
                }

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

                if (promo.isActive()) {
                    promos.add(promo);
                }
            }

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
        Log.d(TAG, "All downloads completed successfully");
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

    public List<OrderType> getCachedOrderTypes() {
        return database.getOrderTypes();
    }

    public List<OrderStatus> getCachedOrderStatuses() {
        return database.getOrderStatuses();
    }

    public long saveOrderLocally(long sessionId, String tableNumber, String customerName, long orderTypeId) {
        return database.saveOrderLocally(sessionId, tableNumber, customerName, orderTypeId);
    }

    public void markOrderAsSynced(long localOrderId, long serverOrderId) {
        database.markOrderAsSynced(localOrderId, serverOrderId);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Unregister network receiver
        if (networkChangeReceiver != null) {
            try {
                unregisterReceiver(networkChangeReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network receiver", e);
            }
        }

        if (database != null) {
            database.close();
        }
    }
}