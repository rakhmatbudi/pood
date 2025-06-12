package com.restaurant.management.network;

import android.content.Context;
import android.util.Log;

import com.chuckerteam.chucker.api.ChuckerCollector;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.chuckerteam.chucker.api.RetentionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://api.pood.lol/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            // Add logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);

            // Try to add Chucker interceptor
            try {
                ChuckerCollector chuckerCollector = new ChuckerCollector(
                        context,
                        true, // Show notification
                        RetentionManager.Period.ONE_HOUR // Keep data for 1 hour
                );

                ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor.Builder(context)
                        .collector(chuckerCollector)
                        .maxContentLength(250_000L) // Max content length to display
                        .redactHeaders("Authorization", "Cookie") // Hide sensitive headers in UI
                        .alwaysReadResponseBody(true)
                        .createShortcut(true) // Create app shortcut
                        .build();

                clientBuilder.addInterceptor(chuckerInterceptor);

            } catch (Exception e) {
                Log.e(TAG, "Failed to create/add Chucker: " + e.getMessage());
            }

            OkHttpClient client = clientBuilder.build();

            // Create Gson converter that properly handles Date objects
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create();

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        try {
            return getClient(context).create(ApiService.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create ApiService: " + e.getMessage());
            throw e;
        }
    }

    // Backward compatibility methods (without Chucker)
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}