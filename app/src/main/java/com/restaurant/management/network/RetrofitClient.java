package com.restaurant.management.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.restaurant.management.R;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://api.pood.lol/";
    private static RetrofitClient instance;
    private Retrofit retrofit;
    private Context context;

    private RetrofitClient(Context context) {
        this.context = context.getApplicationContext();

        // Create HTTP logging interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Create auth interceptor to add Bearer token
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();

                // Get auth token from SharedPreferences
                String authToken = getAuthToken();

                // Add Authorization header if token exists
                if (authToken != null && !authToken.isEmpty()) {
                    Request authenticatedRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + authToken)
                            .build();
                    return chain.proceed(authenticatedRequest);
                }

                return chain.proceed(originalRequest);
            }
        };

        // Create OkHttpClient with interceptors and timeouts
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        // Create Retrofit instance
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }

    private String getAuthToken() {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(
                    context.getString(R.string.pref_file_name), Context.MODE_PRIVATE);
            return sharedPreferences.getString(context.getString(R.string.pref_token), "");
        } catch (Exception e) {
            return "";
        }
    }

    // Method to update base URL if needed
    public static void resetInstance() {
        instance = null;
    }
}