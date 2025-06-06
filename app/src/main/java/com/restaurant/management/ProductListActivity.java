package com.restaurant.management;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.restaurant.management.adapters.ProductAdapter;
import com.restaurant.management.models.Product;
import com.restaurant.management.models.ProductResponse;
import com.restaurant.management.network.ApiClient;
import com.restaurant.management.network.ApiService;
import com.restaurant.management.utils.ProductFilter;
import com.restaurant.management.utils.TableItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, ProductAdapter.OnProductClickListener {

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private ProgressBar progressBar;
    private List<Product> productList = new ArrayList<>();
    private List<Product> filteredProductList = new ArrayList<>();
    private ApiService apiService;
    private TextView emptyView;

    // Filter components
    private TextInputEditText searchEditText;
    private AutoCompleteTextView categoryAutoComplete;
    private ProductFilter productFilter;

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

        // Initialize drawer if available
        initializeDrawer(toolbar);

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add table row spacing
        int dividerSpace = getResources().getDimensionPixelSize(R.dimen.table_row_spacing);
        recyclerView.addItemDecoration(new TableItemDecoration(dividerSpace));

        adapter = new ProductAdapter(this, filteredProductList, this);
        recyclerView.setAdapter(adapter);

        // Initialize filter components
        searchEditText = findViewById(R.id.searchEditText);
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete);

        // Initialize filter mechanics
        initializeFilterListeners();

        // Initialize API service
        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Load products
        loadProducts();
    }

    private void initializeDrawer(Toolbar toolbar) {
        drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = findViewById(R.id.nav_view);
            if (navigationView != null) {
                navigationView.setNavigationItemSelectedListener(this);
            }
        }
    }

    private void initializeFilterListeners() {
        // Setup search text listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter products as text changes
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });

        // Handle search action from keyboard
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                applyFilter();
                return true;
            }
            return false;
        });

        // Setup category dropdown selection listener
        categoryAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = (String) parent.getItemAtPosition(position);
            if (productFilter != null) {
                productFilter.setSelectedCategory(selectedCategory);
                applyFilter();
            }
        });
    }

    private void setupCategoryDropdown(Set<String> categories) {
        // Create sorted list of categories with "All" at the top
        List<String> categoryList = new ArrayList<>();
        categoryList.add("All");

        // Add categories in sorted order
        TreeSet<String> sortedCategories = new TreeSet<>(categories);
        categoryList.addAll(sortedCategories);

        // Create and set adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                categoryList);

        categoryAutoComplete.setAdapter(adapter);

        // Set "All" as default selection
        categoryAutoComplete.setText("All", false);
    }

    private void applyFilter() {
        if (productFilter == null || productList.isEmpty()) {
            return;
        }

        // Update filter parameters
        productFilter.setSearchQuery(searchEditText.getText().toString());

        // Apply filters
        filteredProductList = productFilter.filter();

        // Update adapter
        adapter.updateList(filteredProductList);

        // Show empty view if no results
        if (filteredProductList.isEmpty()) {
            showEmptyView(true);
            emptyView.setText("No products match your filters");
        } else {
            showEmptyView(false);
        }
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
        // Close drawer if open
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Navigate back to Dashboard
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
            startActivity(intent);
            finish();
        }
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

                        // Initialize filter with full product list
                        productFilter = new ProductFilter(productList);

                        // Extract and populate category dropdown
                        Set<String> categories = ProductFilter.extractCategories(productList);
                        setupCategoryDropdown(categories);

                        // Apply initial filter (show all)
                        applyFilter();

                        // Show empty view if the list is empty
                        if (productList.isEmpty()) {
                            showEmptyView(true);
                            emptyView.setText(R.string.no_products);
                        } else {
                            showEmptyView(false);
                        }
                    } else {
                        Log.e("ProductListActivity", "API returned success=false or null data");
                        showError("Error: API returned invalid data");
                        showEmptyView(true);
                        emptyView.setText(R.string.no_products);
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
                    showEmptyView(true);
                    emptyView.setText(R.string.no_products);
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                showLoading(false);
                showEmptyView(true);
                emptyView.setText(R.string.no_products);
                Log.e("ProductListActivity", "API call failed", t);
                showError("Network Error: " + t.getMessage());
            }
        });
    }

    private void showLoading(boolean show) {
        Log.d("ProductListActivity", "ShowLoading: " + show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(View.GONE); // Always hide empty view when loading

        // Add explicit logging to debug visibility states
        Log.d("ProductListActivity", "ProgressBar visibility: " +
                (progressBar.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d("ProductListActivity", "RecyclerView visibility: " +
                (recyclerView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
        Log.d("ProductListActivity", "EmptyView visibility: " +
                (emptyView.getVisibility() == View.VISIBLE ? "VISIBLE" : "GONE"));
    }

    private void showEmptyView(boolean show) {
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
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
            if (drawerLayout != null) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
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

        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}