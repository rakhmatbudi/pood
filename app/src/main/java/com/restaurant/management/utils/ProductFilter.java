package com.restaurant.management.utils;

import com.restaurant.management.models.Product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductFilter {
    private List<Product> originalList;
    private String selectedCategory = "All";
    private String searchQuery = "";

    public ProductFilter(List<Product> originalList) {
        this.originalList = originalList != null ? originalList : new ArrayList<>();
    }

    public void setSelectedCategory(String category) {
        this.selectedCategory = category != null ? category : "All";
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
    }

    public List<Product> filter() {
        List<Product> filteredList = new ArrayList<>();

        // Check if "All" category is selected
        boolean includeAllCategories = "All".equals(selectedCategory);

        for (Product product : originalList) {
            // Check if product matches category filter
            boolean matchesCategory = includeAllCategories ||
                    (product.getCategoryName() != null &&
                            selectedCategory.equals(product.getCategoryName()));

            // Check if product matches search query
            boolean matchesSearch = searchQuery.isEmpty() ||
                    (product.getName() != null &&
                            product.getName().toLowerCase().contains(searchQuery));

            // Add product if it matches both filters
            if (matchesCategory && matchesSearch) {
                filteredList.add(product);
            }
        }

        return filteredList;
    }

    // Extract unique categories from product list
    public static Set<String> extractCategories(List<Product> products) {
        Set<String> categories = new HashSet<>();

        if (products != null) {
            for (Product product : products) {
                if (product.getCategoryName() != null && !product.getCategoryName().isEmpty()) {
                    categories.add(product.getCategoryName());
                }
            }
        }

        return categories;
    }
}