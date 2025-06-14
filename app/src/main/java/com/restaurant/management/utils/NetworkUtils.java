package com.restaurant.management.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

/**
 * Modern NetworkUtils with support for both legacy and current Android APIs
 * Handles network connectivity detection across different Android versions
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * Check if any network connection is available
     * Uses modern API (API 23+) when available, falls back to legacy for older devices
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isNetworkAvailableModern(connectivityManager);
        } else {
            return isNetworkAvailableLegacy(connectivityManager);
        }
    }

    /**
     * Modern network check for API 23+
     */
    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isNetworkAvailableModern(ConnectivityManager connectivityManager) {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Legacy network check for API < 23
     */
    @SuppressWarnings("deprecation")
    private static boolean isNetworkAvailableLegacy(ConnectivityManager connectivityManager) {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Check if WiFi is connected
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isWifiConnectedModern(connectivityManager);
        } else {
            return isWifiConnectedLegacy(connectivityManager);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isWifiConnectedModern(ConnectivityManager connectivityManager) {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    @SuppressWarnings("deprecation")
    private static boolean isWifiConnectedLegacy(ConnectivityManager connectivityManager) {
        NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo != null && wifiInfo.isConnected();
    }

    /**
     * Check if mobile data is connected
     */
    public static boolean isMobileDataConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isMobileDataConnectedModern(connectivityManager);
        } else {
            return isMobileDataConnectedLegacy(connectivityManager);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static boolean isMobileDataConnectedModern(ConnectivityManager connectivityManager) {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    @SuppressWarnings("deprecation")
    private static boolean isMobileDataConnectedLegacy(ConnectivityManager connectivityManager) {
        NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobileInfo != null && mobileInfo.isConnected();
    }

    /**
     * Get current network type
     */
    public static NetworkType getNetworkType(Context context) {
        if (!isNetworkAvailable(context)) {
            return NetworkType.NONE;
        }

        if (isWifiConnected(context)) {
            return NetworkType.WIFI;
        } else if (isMobileDataConnected(context)) {
            return NetworkType.MOBILE;
        } else {
            return NetworkType.OTHER; // Ethernet, VPN, etc.
        }
    }

    /**
     * Check if network has internet capability (not just connected to router)
     * Note: This only works on API 23+ and requires INTERNET capability check
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static boolean hasInternetCapability(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Fallback to basic connectivity check
            return isNetworkAvailable(context);
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /**
     * Check if connection is metered (limited data plan)
     */
    public static boolean isMeteredConnection(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return connectivityManager.isActiveNetworkMetered();
        } else {
            // On older versions, assume mobile data is metered
            return isMobileDataConnected(context);
        }
    }

    /**
     * Get network strength/quality indicator
     * Returns values from 0-4 (similar to WiFi signal strength bars)
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static int getNetworkStrength(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return isNetworkAvailable(context) ? 3 : 0; // Fallback estimation
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return 0;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return 0;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return 0;
        }

        // This is a simplified strength indicator
        // For more accurate readings, you'd need to use WifiManager for WiFi
        // and TelephonyManager for cellular
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return 4; // Assume good WiFi strength
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return 3; // Assume decent cellular strength
        } else {
            return 2; // Other connection types
        }
    }

    /**
     * Register for network connectivity changes
     * Use this for real-time network monitoring
     */
    public static void registerNetworkCallback(Context context, ConnectivityManager.NetworkCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "Network callbacks not supported on this Android version");
            return;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return;
        }

        // For API 24+ (Android N), use the simpler approach
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback);
        } else {
            // For API 21-23, create a basic NetworkRequest
            // Note: addTransport may not be available on all configurations
            // Use a simple NetworkRequest instead
            NetworkRequest networkRequest = new NetworkRequest.Builder().build();
            connectivityManager.registerNetworkCallback(networkRequest, callback);
        }
    }

    /**
     * Unregister network callback
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void unregisterNetworkCallback(Context context, ConnectivityManager.NetworkCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(callback);
        }
    }

    /**
     * Get detailed network information for debugging
     */
    public static NetworkStatus getDetailedNetworkInfo(Context context) {
        NetworkType type = getNetworkType(context);
        boolean isMetered = isMeteredConnection(context);
        boolean hasInternet = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                hasInternetCapability(context) : isNetworkAvailable(context);
        int strength = getNetworkStrength(context);

        return new NetworkStatus(type, isMetered, hasInternet, strength);
    }

    // Enum for network types
    public enum NetworkType {
        WIFI,
        MOBILE,
        ETHERNET,
        OTHER,
        NONE
    }

    // Data class for detailed network information
    public static class NetworkStatus {
        private final NetworkType type;
        private final boolean isMetered;
        private final boolean hasInternet;
        private final int strength;

        public NetworkStatus(NetworkType type, boolean isMetered, boolean hasInternet, int strength) {
            this.type = type;
            this.isMetered = isMetered;
            this.hasInternet = hasInternet;
            this.strength = strength;
        }

        public NetworkType getType() { return type; }
        public boolean isMetered() { return isMetered; }
        public boolean hasInternet() { return hasInternet; }
        public int getStrength() { return strength; }

        public boolean isGoodForSync() {
            return hasInternet && strength >= 2;
        }

        public boolean isGoodForLargeDownloads() {
            return hasInternet && !isMetered && strength >= 3;
        }

        @Override
        public String toString() {
            return String.format("NetworkStatus{type=%s, metered=%s, internet=%s, strength=%d}",
                    type, isMetered, hasInternet, strength);
        }
    }
}