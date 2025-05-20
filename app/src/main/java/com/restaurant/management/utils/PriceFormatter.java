package com.restaurant.management.utils;

/**
 * Utility class for formatting prices with currency symbol and thousands separators
 */
public class PriceFormatter {

    /**
     * Format a price with currency symbol and thousands separators
     *
     * @param price The price value to format
     * @param currencyPrefix The currency symbol/prefix to use
     * @return The formatted price string
     */
    public static String format(double price, String currencyPrefix) {
        // Round to the nearest integer (no decimal)
        long roundedPrice = Math.round(price);

        // Format as xxx.xxx.xxx
        String priceStr = String.valueOf(roundedPrice);
        StringBuilder formattedPrice = new StringBuilder();

        int length = priceStr.length();
        for (int i = 0; i < length; i++) {
            formattedPrice.append(priceStr.charAt(i));
            // Add dot after every 3 digits from the right, but not at the end
            if ((length - i - 1) % 3 == 0 && i < length - 1) {
                formattedPrice.append('.');
            }
        }

        // Return formatted price with currency prefix
        return currencyPrefix + " " + formattedPrice.toString();
    }
}