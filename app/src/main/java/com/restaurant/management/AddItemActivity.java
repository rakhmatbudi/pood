package com.restaurant.management;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.adapters.ProductItemAdapter;
import com.restaurant.management.database.PoodDatabase;
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;
import com.restaurant.management.utils.NetworkUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddItemActivity extends AppCompatActivity implements ProductItemAdapter.OnItemClickListener {
    private static final String TAG = "AddItemActivity";
    private static final String BASE_API_URL = "https://api.pood.lol/";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private TextView tableNumberTextView;
    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView menuItemsRecyclerView;
    private ProgressBar progressBar;

    private OkHttpClient client = new OkHttpClient();
    private List<ProductItem> menuItems = new ArrayList<>();
    private List<ProductItem> allMenuItems = new ArrayList<>();
    private ProductItemAdapter menuItemAdapter;
    private PoodDatabase database;

    private long orderId;
    private String tableNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        database = new PoodDatabase(this);

        initializeViews();
        setupToolbar();
        getOrderDetails();
        setupRecyclerView();
        setupSearch();
        loadMenuItems();
    }

    private void initializeViews() {
        tableNumberTextView = findViewById(R.id.table_number_text_view);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        menuItemsRecyclerView = findViewById(R.id.menu_items_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Item");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void getOrderDetails() {
        orderId = getIntent().getLongExtra("order_id", -1);
        tableNumber = getIntent().getStringExtra("table_number");

        if (orderId == -1 || tableNumber == null) {
            Toast.makeText(this, "Invalid order details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tableNumberTextView.setText("Table: " + tableNumber);
    }

    private void setupRecyclerView() {
        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemAdapter = new ProductItemAdapter(menuItems, this);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);
    }

    private void setupSearch() {
        searchButton.setOnClickListener(v -> searchMenuItems());

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMenuItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMenuItems() {
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<ProductItem> items = database.getAllMenuItems();
                Log.d(TAG, "Loaded " + (items != null ? items.size() : 0) + " items from database");

                // Check Affogato price in database
                if (items != null) {
                    for (ProductItem item : items) {
                        if ("Affogato".equals(item.getName())) {
                            Log.d(TAG, "Affogato price from database: " + item.getPrice());
                            break;
                        }
                    }
                }

                runOnUiThread(() -> {
                    if (items != null && !items.isEmpty()) {
                        allMenuItems.clear();
                        allMenuItems.addAll(items);

                        menuItems.clear();
                        menuItems.addAll(items);
                        menuItemAdapter.notifyDataSetChanged();

                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "No menu items available", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading menu items", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error loading menu items", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void searchMenuItems() {
        String query = searchEditText.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(query)) {
            menuItems.clear();
            menuItems.addAll(allMenuItems);
            menuItemAdapter.notifyDataSetChanged();
        } else {
            List<ProductItem> filteredItems = new ArrayList<>();

            for (ProductItem item : allMenuItems) {
                String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
                String itemDesc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                String itemCategory = item.getCategory() != null ? item.getCategory().toLowerCase() : "";

                if (itemName.contains(query) || itemDesc.contains(query) || itemCategory.contains(query)) {
                    filteredItems.add(item);
                }
            }

            menuItems.clear();
            menuItems.addAll(filteredItems);
            menuItemAdapter.notifyDataSetChanged();

            if (filteredItems.isEmpty() && !allMenuItems.isEmpty()) {
                Toast.makeText(this, "No items found matching '" + query + "'", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(ProductItem menuItem) {
        ItemDetailDialog dialog = new ItemDetailDialog(this, menuItem, new ItemDetailDialog.OnItemAddListener() {
            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes) {
                addItemToOrder(selectedItem, variantId, quantity, notes, null, false);
            }

            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes, Double customPrice) {
                addItemToOrder(selectedItem, variantId, quantity, notes, customPrice, false);
            }

            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes, Double customPrice, boolean isComplimentary) {
                addItemToOrder(selectedItem, variantId, quantity, notes, customPrice, isComplimentary);
            }
        });
        dialog.show();
    }

    private void addItemToOrder(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice, boolean isComplimentary) {
        if (quantity <= 0) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Internet connection required to add items to order.", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        double unitPrice;
        if (isComplimentary) {
            unitPrice = 0.0;
        } else if (customPrice != null) {
            unitPrice = customPrice;
        } else if (variantId != null) {
            unitPrice = menuItem.getPrice();
            for (Variant variant : menuItem.getVariants()) {
                if (Objects.equals(variant.getId(), variantId)) {
                    unitPrice = variant.getPrice();
                    break;
                }
            }
        } else {
            unitPrice = menuItem.getPrice();
        }

        double totalPrice = unitPrice * quantity;
        final double finalUnitPrice = unitPrice;

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("menu_item_id", menuItem.getId());
            requestJson.put("variant_id", variantId != null ? variantId : JSONObject.NULL);
            requestJson.put("quantity", quantity);
            requestJson.put("unit_price", finalUnitPrice);
            requestJson.put("total_price", totalPrice);
            requestJson.put("status", "new");
            requestJson.put("kitchen_printed", false);

            if (notes != null && !notes.isEmpty()) {
                requestJson.put("notes", notes);
            }

            if (isComplimentary) {
                requestJson.put("is_complimentary", true);
                requestJson.put("original_price", getOriginalPrice(menuItem, variantId));
            }

            if (customPrice != null && !isComplimentary) {
                requestJson.put("is_custom_price", true);
                requestJson.put("original_price", menuItem.getPrice());
            }

        } catch (JSONException e) {
            Toast.makeText(this, "Error creating request", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            menuItemsRecyclerView.setVisibility(View.VISIBLE);
            return;
        }

        String apiUrl = BASE_API_URL + "orders/" + orderId + "/items";
        String authToken = getAuthToken();

        RequestBody body = RequestBody.create(JSON, requestJson.toString());
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .header("Content-Type", "application/json");

        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(AddItemActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);

                    if (response.isSuccessful()) {
                        String successMessage = buildSuccessMessage(isComplimentary, customPrice, finalUnitPrice);
                        Toast.makeText(AddItemActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMessage = errorJson.optString("message", "Server error");
                            Toast.makeText(AddItemActivity.this, "Failed to add item: " + errorMessage, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Toast.makeText(AddItemActivity.this, "Failed to add item", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private double getOriginalPrice(ProductItem menuItem, Long variantId) {
        if (variantId != null) {
            for (Variant variant : menuItem.getVariants()) {
                if (Objects.equals(variant.getId(), variantId)) {
                    return variant.getPrice();
                }
            }
        }
        return menuItem.getPrice();
    }

    private String buildSuccessMessage(boolean isComplimentary, Double customPrice, double unitPrice) {
        String baseMessage = "Item added successfully";

        if (isComplimentary) {
            return baseMessage + " (Complimentary - FREE)";
        } else if (customPrice != null) {
            return baseMessage + " (Custom price: " + String.format("%.0f", customPrice) + ")";
        } else {
            return baseMessage;
        }
    }

    private String getAuthToken() {
        try {
            return getSharedPreferences("restaurant_prefs", MODE_PRIVATE)
                    .getString("auth_token", "");
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
}