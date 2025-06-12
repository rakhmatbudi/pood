package com.restaurant.management.repositories;

import android.content.Context;
import android.util.Log;

import com.restaurant.management.models.MenuCategory;
import com.restaurant.management.models.MenuCategoryResponse;
import com.restaurant.management.models.MenuItemResponse;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.network.ApiService;
import com.restaurant.management.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuRepository {
    private static final String TAG = "MenuRepository";

    private final Context context;
    private final ApiService apiService;

    public interface MenuCategoryCallback {
        void onSuccess(List<MenuCategory> categories);
        void onError(String message);
    }

    public interface MenuItemCallback {
        void onSuccess(List<ProductItem> items);
        void onError(String message);
    }

    public MenuRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void fetchMenuCategories(MenuCategoryCallback callback) {
        Log.d(TAG, "Fetching menu categories...");

        Call<MenuCategoryResponse> call = apiService.getMenuCategories();

        call.enqueue(new Callback<MenuCategoryResponse>() {
            @Override
            public void onResponse(Call<MenuCategoryResponse> call, Response<MenuCategoryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MenuCategoryResponse categoryResponse = response.body();

                    Log.d(TAG, "API Response Status: " + categoryResponse.getStatus());

                    if (categoryResponse.isSuccess() && categoryResponse.hasData()) {
                        List<MenuCategory> categories = categoryResponse.getData();
                        Log.d(TAG, "Successfully loaded " + categories.size() + " categories");
                        callback.onSuccess(categories);
                    } else {
                        String errorMsg = "No menu categories available";
                        Log.w(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                } else {
                    String errorMsg = "Failed to load menu categories (HTTP " + response.code() + ")";
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<MenuCategoryResponse> call, Throwable t) {
                String errorMsg = "Failed to load menu categories: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError("Failed to load menu categories. Please check your connection.");
            }
        });
    }

    public void fetchMenuItems(MenuItemCallback callback) {
        Log.d(TAG, "Fetching menu items...");

        Call<MenuItemResponse> call = apiService.getMenuItems();

        call.enqueue(new Callback<MenuItemResponse>() {
            @Override
            public void onResponse(Call<MenuItemResponse> call, Response<MenuItemResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    MenuItemResponse itemResponse = response.body();

                    Log.d(TAG, "API Response Status: " + itemResponse.getStatus());

                    if (itemResponse.isSuccess() && itemResponse.hasData()) {
                        List<ProductItem> items = itemResponse.getData();
                        Log.d(TAG, "Successfully loaded " + items.size() + " menu items");

                        // Log specific item for debugging (like your Affogato check)
                        for (ProductItem item : items) {
                            if ("Affogato".equals(item.getName())) {
                                Log.d(TAG, "Affogato price from API: " + item.getPrice());
                                break;
                            }
                        }

                        callback.onSuccess(items);
                    } else {
                        String errorMsg = "No menu items available";
                        Log.w(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                } else {
                    String errorMsg = "Failed to load menu items (HTTP " + response.code() + ")";
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<MenuItemResponse> call, Throwable t) {
                String errorMsg = "Failed to load menu items: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError("Failed to load menu items. Please check your connection.");
            }
        });
    }
}