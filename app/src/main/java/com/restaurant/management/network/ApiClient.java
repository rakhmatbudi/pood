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
    private static final String TAG = "ApiClient_Debug";
    private static final String BASE_URL = "https://api.pood.lol/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient(Context context) {
        Log.d(TAG, "=== getClient() called ===");
        Log.d(TAG, "Context: " + (context != null ? context.getClass().getSimpleName() : "NULL"));

        if (retrofit == null) {
            Log.d(TAG, "Creating new Retrofit client...");

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            // Add logging interceptor
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);
            Log.d(TAG, "✅ Added HttpLoggingInterceptor");

            // Try to add Chucker interceptor
            try {
                Log.d(TAG, "Attempting to create Chucker components...");

                ChuckerCollector chuckerCollector = new ChuckerCollector(
                        context,
                        true, // Show notification
                        RetentionManager.Period.ONE_HOUR // Keep data for 1 hour
                );
                Log.d(TAG, "✅ ChuckerCollector created successfully");

                ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor.Builder(context)
                        .collector(chuckerCollector)
                        .maxContentLength(250_000L) // Max content length to display
                        .redactHeaders("Authorization", "Cookie") // Hide sensitive headers in UI
                        .alwaysReadResponseBody(true)
                        .createShortcut(true) // Create app shortcut
                        .build();
                Log.d(TAG, "✅ ChuckerInterceptor created successfully");

                clientBuilder.addInterceptor(chuckerInterceptor);
                Log.d(TAG, "✅ ChuckerInterceptor added to OkHttp client");

            } catch (Exception e) {
                Log.e(TAG, "❌ FAILED to create/add Chucker: " + e.getMessage());
                Log.e(TAG, "❌ Exception type: " + e.getClass().getSimpleName());
                e.printStackTrace();
            }

            OkHttpClient client = clientBuilder.build();
            Log.d(TAG, "OkHttpClient built with " + client.interceptors().size() + " interceptors");

            // Create Gson converter that properly handles Date objects
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                    .create();
            Log.d(TAG, "Gson converter created");

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            Log.d(TAG, "✅ Retrofit instance created with base URL: " + BASE_URL);
        } else {
            Log.d(TAG, "Using existing Retrofit instance");
        }

        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        Log.d(TAG, "=== getApiService() called ===");
        Log.d(TAG, "Context: " + (context != null ? context.getClass().getSimpleName() : "NULL"));

        try {
            ApiService service = getClient(context).create(ApiService.class);
            Log.d(TAG, "✅ ApiService created successfully");
            return service;
        } catch (Exception e) {
            Log.e(TAG, "❌ FAILED to create ApiService: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Backward compatibility methods (without Chucker) - WITH STACK TRACES
    public static Retrofit getClient() {
        Log.d(TAG, "=== getClient() called (NO CONTEXT - NO CHUCKER) ===");

        // ✅ ADD STACK TRACE TO SEE WHO'S CALLING THIS
        Log.d(TAG, "📍 STACK TRACE - WHO IS CALLING getClient() WITHOUT CONTEXT:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stackTrace.length, 8); i++) { // Skip first 3 (getStackTrace, getClient, etc)
            StackTraceElement element = stackTrace[i];
            Log.d(TAG, "   at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.d(TAG, "📍 END STACK TRACE");

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
        Log.d(TAG, "=== getApiService() called (NO CONTEXT - NO CHUCKER) ===");

        // ✅ ADD STACK TRACE TO SEE WHO'S CALLING THIS
        Log.d(TAG, "📍 STACK TRACE - WHO IS CALLING getApiService() WITHOUT CONTEXT:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stackTrace.length, 8); i++) { // Skip first 3
            StackTraceElement element = stackTrace[i];
            Log.d(TAG, "   at " + element.getClassName() + "." + element.getMethodName() +
                    "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
        }
        Log.d(TAG, "📍 END STACK TRACE");

        return getClient().create(ApiService.class);
    }
}