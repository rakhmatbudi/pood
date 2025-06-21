package com.restaurant.management.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.chuckerteam.chucker.api.ChuckerCollector;
import com.chuckerteam.chucker.api.ChuckerInterceptor;
import com.chuckerteam.chucker.api.RetentionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.restaurant.management.R; // Import your R class to access string resources for shared preferences keys

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

            // Add logging interceptor for general request/response logging
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addInterceptor(loggingInterceptor);

            // Add the Authorization header interceptor FIRST, so it's applied before Chucker/other logging.
            // This ensures the token is present for other interceptors if they need it (like Chucker for redaction).
            clientBuilder.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request(); // Get the original request

                    // Retrieve token from SharedPreferences
                    SharedPreferences sharedPreferences = context.getSharedPreferences(
                            context.getString(R.string.pref_file_name), Context.MODE_PRIVATE);
                    String token = sharedPreferences.getString(context.getString(R.string.pref_token), null);

                    Request.Builder requestBuilder = originalRequest.newBuilder();

                    // If a token exists, add the Authorization header
                    if (token != null && !token.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + token);
                        // Log a partial token for debugging, full token is sensitive
                        Log.d(TAG, "Adding Authorization header with token: Bearer " + token.substring(0, Math.min(token.length(), 20)) + "...");
                    } else {
                        Log.w(TAG, "No JWT token found in SharedPreferences for request to: " + originalRequest.url());
                    }

                    // Build the new request with the added header (if any)
                    Request newRequest = requestBuilder.method(originalRequest.method(), originalRequest.body()).build();

                    // Proceed with the request
                    return chain.proceed(newRequest);
                }
            });


            // Try to add Chucker interceptor (for in-app network inspection)
            try {
                // ChuckerCollector setup
                ChuckerCollector chuckerCollector = new ChuckerCollector(
                        context,
                        true, // Show notification
                        RetentionManager.Period.ONE_HOUR // Keep data for 1 hour
                );

                // ChuckerInterceptor setup
                ChuckerInterceptor chuckerInterceptor = new ChuckerInterceptor.Builder(context)
                        .collector(chuckerCollector)
                        .maxContentLength(250_000L) // Max content length to display
                        .redactHeaders("Authorization", "Cookie") // Hide sensitive headers in UI (Chucker will redact this)
                        .alwaysReadResponseBody(true)
                        .createShortcut(true) // Create app shortcut
                        .build();

                clientBuilder.addInterceptor(chuckerInterceptor);
                Log.d(TAG, "Chucker interceptor added successfully.");

            } catch (NoClassDefFoundError e) { // Catch NoClassDefFoundError if Chucker isn't in release build
                Log.e(TAG, "Chucker library not found. Skipping Chucker interceptor. Error: " + e.getMessage());
            } catch (Exception e) { // Catch any other exceptions during Chucker setup
                Log.e(TAG, "Failed to create/add Chucker interceptor: " + e.getMessage(), e);
            }

            OkHttpClient client = clientBuilder.build();

            // Create Gson converter that properly handles Date objects
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Ensure this matches your API's date format
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
            Log.e(TAG, "Failed to create ApiService: " + e.getMessage(), e); // Log full exception
            throw e; // Re-throw to indicate failure
        }
    }

    // Backward compatibility methods (without Chucker or Authorization - generally NOT RECOMMENDED for authenticated calls)
    // These methods should ideally be removed or marked deprecated if all calls are authenticated.
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