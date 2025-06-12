package com.restaurant.management.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.restaurant.management.R;
import com.restaurant.management.models.Promo;
import com.restaurant.management.models.PromoResponse;
import com.restaurant.management.network.ApiService;
import com.restaurant.management.network.RetrofitClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PromoRepository {
    private static final String TAG = "PromoRepository";

    private final Context context;
    private final ApiService apiService;

    public interface PromoCallback {
        void onSuccess(List<Promo> promos);
        void onError(String message);
    }

    public PromoRepository(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void fetchActivePromos(PromoCallback callback) {
        Log.d(TAG, "Fetching active promos...");

        Call<PromoResponse> call = apiService.getActivePromos();

        call.enqueue(new Callback<PromoResponse>() {
            @Override
            public void onResponse(Call<PromoResponse> call, Response<PromoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PromoResponse promoResponse = response.body();

                    Log.d(TAG, "API Response Status: " + promoResponse.getStatus());

                    if (promoResponse.isSuccess() && promoResponse.hasData()) {
                        List<Promo> promos = promoResponse.getData();
                        Log.d(TAG, "Successfully loaded " + promos.size() + " promos");

                        // Log image URLs for debugging
                        for (Promo promo : promos) {
                            Log.d(TAG, "Promo: " + promo.getDisplayName() +
                                    " | Has Image: " + promo.hasImage() +
                                    " | Picture URL: " + promo.getPicture());
                        }

                        callback.onSuccess(promos);
                    } else {
                        String errorMsg = "No active promotions available";
                        Log.w(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                } else {
                    String errorMsg = "Failed to load promotions (HTTP " + response.code() + ")";
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<PromoResponse> call, Throwable t) {
                String errorMsg = "Failed to load promotions: " + t.getMessage();
                Log.e(TAG, errorMsg, t);
                callback.onError("Failed to load promotions. Please check your connection.");
            }
        });
    }

    public void fetchPromoById(long promoId, PromoCallback callback) {
        Log.d(TAG, "Fetching promo by ID: " + promoId);

        Call<PromoResponse> call = apiService.getPromoById(promoId);

        call.enqueue(new Callback<PromoResponse>() {
            @Override
            public void onResponse(Call<PromoResponse> call, Response<PromoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PromoResponse promoResponse = response.body();

                    if (promoResponse.isSuccess() && promoResponse.hasData()) {
                        callback.onSuccess(promoResponse.getData());
                    } else {
                        callback.onError("Promo not found");
                    }
                } else {
                    callback.onError("Failed to load promo (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<PromoResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch promo by ID", t);
                callback.onError("Failed to load promo. Please check your connection.");
            }
        });
    }
}