package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.restaurant.management.adapters.ProductAdapter; // Added this import
import com.restaurant.management.models.Product;
import com.restaurant.management.models.ProductResponse;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ProductAdapter.OnProductClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ProgressBar progressBar;
    private List<Product> productList = new ArrayList<>();
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add back button to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Products");
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductAdapter(this, productList, this);
        recyclerView.setAdapter(adapter);

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar);

        // Initialize API service
        apiService = ApiClient.getClient().create(ApiService.class);

        // Load products
        loadProducts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the back button
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Navigate back to Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
        startActivity(intent);
        finish();
    }

    private void loadProducts() {
        Log.d("ProductListActivity", "Loading products...");
        showLoading(true);

        Call<ProductResponse> call = apiService.getProducts();
        call.enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                showLoading(false);
                Log.d("ProductListActivity", "API Response Code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    ProductResponse productResponse = response.body();

                    if ("success".equals(productResponse.getStatus()) && productResponse.getData() != null) {
                        productList = productResponse.getData();
                        Log.d("ProductListActivity", "Response data size: " + productList.size());

                        // Log the first few products to verify data
                        int logCount = Math.min(productList.size(), 3);
                        for (int i = 0; i < logCount; i++) {
                            Product product = productList.get(i);
                            Log.d("ProductListActivity", "Product " + i + ": " + product.getName() +
                                    ", Price: " + product.getPrice());
                        }

                        adapter.updateList(productList);
                    } else {
                        Log.e("ProductListActivity", "API returned success=false or null data");
                        showError("Error: API returned invalid data");
                    }

                    // Show a message if the list is empty
                    if (productList.isEmpty()) {
                        TextView emptyView = new TextView(ProductListActivity.this);
                        emptyView.setText("No products available");
                        emptyView.setTextSize(18);
                        emptyView.setGravity(Gravity.CENTER);
                        emptyView.setPadding(16, 100, 16, 16);

                        ViewGroup parent = (ViewGroup) recyclerView.getParent();
                        parent.addView(emptyView);
                    }
                } else {
                    String errorMsg = "API Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e("ProductListActivity", "Error reading error body", e);
                    }
                    Log.e("ProductListActivity", errorMsg);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                showLoading(false);
                Log.e("ProductListActivity", "API call failed", t);
                showError("Network Error: " + t.getMessage());
            }
        });
    }

    private void showLoading(boolean show) {
        Log.d("ProductListActivity", "ShowLoading: " + show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

        // Add explicit logging to debug visibility states
        Log.d("ProductListActivity", "ProgressBar visibility: " +
                (progressBar.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d("ProductListActivity", "RecyclerView visibility: " +
                (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProductClick(Product product) {
        // Handle product click - you could navigate to a detail page
        Toast.makeText(this, "Selected: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Add this log statement
        Log.d("ProductListActivity", "Navigation item selected: " + item.getTitle());

        // Handle navigation view item clicks
        int id = item.getItemId();

        if (id == R.id.nav_dashboard) {
            Log.d("ProductListActivity", "Dashboard selected");
            startActivity(new Intent(this, DashboardActivity.class));
        } else if (id == R.id.nav_orders) {
            Log.d("ProductListActivity", "Orders selected");
            startActivity(new Intent(this, OrderListActivity.class));
        } else if (id == R.id.nav_products) {
            Log.d("ProductListActivity", "Products selected");
            // We are already here
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.nav_transactions) {
            Log.d("ProductListActivity", "Transactions selected");
            startActivity(new Intent(this, TransactionActivity.class));
        } else if (id == R.id.nav_logout) {
            Log.d("ProductListActivity", "Logout selected");
            // Handle logout
            // Example: clear preferences and redirect to login
            // SharedPreferences preferences = getSharedPreferences("USER_PREF", MODE_PRIVATE);
            // preferences.edit().clear().apply();
            // startActivity(new Intent(this, MainActivity.class));
            // finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }


}