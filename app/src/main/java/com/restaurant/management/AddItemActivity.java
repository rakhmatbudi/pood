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
import com.restaurant.management.models.ProductItem;
import com.restaurant.management.models.Variant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
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
    private View contentLayout;

    private OkHttpClient client = new OkHttpClient();
    private List<ProductItem> menuItems = new ArrayList<>();
    private List<ProductItem> allMenuItems = new ArrayList<>(); // Store all items for filtering
    private ProductItemAdapter menuItemAdapter;

    private long orderId;
    private String tableNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_item);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.add_item));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize views
        tableNumberTextView = findViewById(R.id.table_number_text_view);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        menuItemsRecyclerView = findViewById(R.id.menu_items_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        // Get order details from intent
        orderId = getIntent().getLongExtra("order_id", -1);
        tableNumber = getIntent().getStringExtra("table_number");

        if (orderId == -1 || tableNumber == null) {
            Toast.makeText(this, R.string.invalid_order_details, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set table number
        tableNumberTextView.setText(getString(R.string.table_number_format, tableNumber));

        // Set up RecyclerView
        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemAdapter = new ProductItemAdapter(menuItems, this);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);

        // Set up search button for immediate search
        searchButton.setOnClickListener(v -> searchMenuItems());

        // Optional: Add text change listener for real-time search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Implement search as user types
                searchMenuItems();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Load all menu items once
        fetchMenuItems();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchMenuItems() {
        String query = searchEditText.getText().toString().trim().toLowerCase();

        if (TextUtils.isEmpty(query)) {
            // If query is empty, show all items
            menuItems.clear();
            menuItems.addAll(allMenuItems);
            menuItemAdapter.notifyDataSetChanged();
        } else {
            // Filter items based on name, description, or category
            List<ProductItem> filteredItems = new ArrayList<>();

            for (ProductItem item : allMenuItems) {
                String itemName = item.getName() != null ? item.getName().toLowerCase() : "";
                String itemDesc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
                String itemCategory = item.getCategory() != null ? item.getCategory().toLowerCase() : "";

                if (itemName.contains(query) || itemDesc.contains(query) || itemCategory.contains(query)) {
                    filteredItems.add(item);
                }
            }

            // Update adapter with filtered results
            menuItems.clear();
            menuItems.addAll(filteredItems);
            menuItemAdapter.notifyDataSetChanged();

            // Show message if no results found
            if (filteredItems.isEmpty() && !allMenuItems.isEmpty()) {
                Toast.makeText(this, "No items found matching '" + query + "'", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchMenuItems() {
        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        // Simple URL to fetch all menu items
        String apiUrl = BASE_API_URL + "menu-items";
        Log.d(TAG, "Fetching all menu items from: " + apiUrl);

        // Get the auth token
        String authToken = getAuthToken();

        // Create request with token
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl);

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(AddItemActivity.this,
                            getString(R.string.network_error),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "API response received: " + response.code());

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<ProductItem> items = parseMenuItems(jsonResponse);
                    Log.d(TAG, "Parsed " + items.size() + " menu items");

                    runOnUiThread(() -> {
                        // Store in both lists for filtering
                        allMenuItems.clear();
                        allMenuItems.addAll(items);

                        menuItems.clear();
                        menuItems.addAll(items);
                        menuItemAdapter.notifyDataSetChanged();

                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                        Toast.makeText(AddItemActivity.this,
                                getString(R.string.error_processing_response, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private List<ProductItem> parseMenuItems(JSONObject jsonResponse) throws JSONException {
        List<ProductItem> items = new ArrayList<>();

        if (jsonResponse.has("data") && !jsonResponse.isNull("data")) {
            JSONArray itemsArray;

            // Check if "data" is an array or an object
            if (jsonResponse.get("data") instanceof JSONArray) {
                itemsArray = jsonResponse.getJSONArray("data");
                Log.d(TAG, "Parsing array of items: " + itemsArray.length());
            } else {
                // If "data" is a single object, create an array with just that object
                JSONObject singleItem = jsonResponse.getJSONObject("data");
                itemsArray = new JSONArray();
                itemsArray.put(singleItem);
                Log.d(TAG, "Parsing single item as array");
            }

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                ProductItem item = new ProductItem();

                item.setId(itemJson.optLong("id", -1));
                item.setName(itemJson.optString("name", ""));
                item.setDescription(itemJson.optString("description", ""));
                item.setPrice(parsePrice(itemJson.optString("price", "0")));

                // Handle different category field structures
                if (itemJson.has("category_name") && !itemJson.isNull("category_name")) {
                    // Direct category_name field
                    item.setCategory(itemJson.optString("category_name", ""));
                } else if (itemJson.has("category") && !itemJson.isNull("category")) {
                    // Check if "category" is a string or an object
                    Object categoryObj = itemJson.get("category");
                    if (categoryObj instanceof JSONObject) {
                        // Handle nested category object
                        JSONObject categoryJson = (JSONObject) categoryObj;
                        if (categoryJson.has("name")) {
                            item.setCategory(categoryJson.optString("name", ""));
                        }
                    } else {
                        // Handle string category
                        item.setCategory(itemJson.optString("category", ""));
                    }
                }

                // Handle image URL - check for both image_url and image_path
                if (itemJson.has("image_url") && !itemJson.isNull("image_url")) {
                    item.setImageUrl(itemJson.optString("image_url", ""));
                } else if (itemJson.has("image_path") && !itemJson.isNull("image_path")) {
                    item.setImageUrl(itemJson.optString("image_path", ""));
                }

                // Parse variants if available
                List<Variant> variants = new ArrayList<>();
                if (itemJson.has("variants") && !itemJson.isNull("variants")) {
                    JSONArray variantsArray = itemJson.getJSONArray("variants");

                    for (int j = 0; j < variantsArray.length(); j++) {
                        JSONObject variantJson = variantsArray.getJSONObject(j);
                        Variant variant = new Variant();
                        variant.setId(variantJson.optLong("id", -1));
                        variant.setName(variantJson.optString("name", ""));
                        variant.setPrice(parsePrice(variantJson.optString("price", "0")));
                        variants.add(variant);
                    }
                }

                item.setVariants(variants);
                items.add(item);
            }

            // Sort the items alphabetically by name
            Collections.sort(items, (item1, item2) ->
                    item1.getName().compareToIgnoreCase(item2.getName()));
        }

        return items;
    }

    private double parsePrice(String priceString) {
        try {
            return Double.parseDouble(priceString);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing price: " + priceString, e);
            return 0.0;
        }
    }

    @Override
    public void onItemClick(ProductItem menuItem) {
        // Show item detail dialog with quantity selector and variants if available
        ItemDetailDialog dialog = new ItemDetailDialog(this, menuItem, new ItemDetailDialog.OnItemAddListener() {
            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes) {
                // Handle regular items (backward compatibility)
                Log.d(TAG, "Adding regular item: " + selectedItem.getName());
                addItemToOrder(selectedItem, variantId, quantity, notes, null);
            }

            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes, Double customPrice) {
                // Handle custom price items
                Log.d(TAG, "Adding custom item: " + selectedItem.getName() + " with custom price: " + customPrice);
                addItemToOrder(selectedItem, variantId, quantity, notes, customPrice);
            }
        });
        dialog.show();
    }

    private void addItemToOrder(ProductItem menuItem, Long variantId, int quantity, String notes, Double customPrice) {
        if (quantity <= 0) {
            Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        // Get the unit price based on custom price, variant, or default price
        double unitPrice;
        String priceSource;

        if (customPrice != null) {
            // Use custom price for custom items
            unitPrice = customPrice;
            priceSource = "custom price";
            Log.d(TAG, "Using custom price: " + unitPrice);
        } else if (variantId != null) {
            // Find the selected variant for regular items
            unitPrice = menuItem.getPrice(); // fallback to default
            priceSource = "variant price";
            for (Variant variant : menuItem.getVariants()) {
                if (Objects.equals(variant.getId(), variantId)) {
                    unitPrice = variant.getPrice();
                    Log.d(TAG, "Using variant price: " + unitPrice + " for variant: " + variant.getName());
                    break;
                }
            }
        } else {
            // Use default item price
            unitPrice = menuItem.getPrice();
            priceSource = "default price";
            Log.d(TAG, "Using default price: " + unitPrice);
        }

        // Calculate the total price
        double totalPrice = unitPrice * quantity;

        Log.d(TAG, String.format("Price calculation: %s (%.2f) x %d = %.2f",
                priceSource, unitPrice, quantity, totalPrice));

        // Prepare the request body according to the specified structure
        JSONObject requestJson = new JSONObject();
        try {
            requestJson.put("menu_item_id", menuItem.getId());

            if (variantId != null) {
                requestJson.put("variant_id", variantId);
            } else {
                requestJson.put("variant_id", JSONObject.NULL);
            }

            requestJson.put("quantity", quantity);
            requestJson.put("unit_price", unitPrice);
            requestJson.put("total_price", totalPrice);
            requestJson.put("status", "new");
            requestJson.put("kitchen_printed", false);

            // Only add notes if not empty
            if (notes != null && !notes.isEmpty()) {
                requestJson.put("notes", notes);
            }

            // Add custom price flag if this is a custom item
            if (customPrice != null) {
                requestJson.put("is_custom_price", true);
                requestJson.put("original_price", menuItem.getPrice());
            }

            // Log the complete request JSON
            Log.d(TAG, "Request JSON: " + requestJson.toString());

        } catch (JSONException e) {
            Log.e(TAG, "Error creating request JSON", e);
            Toast.makeText(this, R.string.error_creating_request, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            menuItemsRecyclerView.setVisibility(View.VISIBLE);
            return;
        }

        // Build the URL for adding an item to the specified order
        String apiUrl = BASE_API_URL + "orders/" + orderId + "/items";
        Log.d(TAG, "Sending request to: " + apiUrl);

        // Get the auth token
        String authToken = getAuthToken();
        Log.d(TAG, "Using auth token: " + (authToken != null && !authToken.isEmpty() ? "Valid token present" : "No token or empty token"));

        // Create request with token and proper content type
        RequestBody body = RequestBody.create(JSON, requestJson.toString());
        Request.Builder requestBuilder = new Request.Builder()
                .url(apiUrl)
                .post(body)
                .header("Content-Type", "application/json");

        // Add authorization header if token is available
        if (authToken != null && !authToken.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + authToken);
        }

        Request request = requestBuilder.build();
        Log.d(TAG, "Request headers: " + request.headers().toString());

        // Execute the request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(AddItemActivity.this,
                            getString(R.string.network_error) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "Add item response received: " + response.code());
                Log.d(TAG, "Response body: " + responseBody);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    menuItemsRecyclerView.setVisibility(View.VISIBLE);

                    if (response.isSuccessful()) {
                        String successMessage;
                        if (customPrice != null) {
                            successMessage = getString(R.string.item_added_successfully) +
                                    " (Custom price: " + String.format("%.0f", customPrice) + ")";
                        } else {
                            successMessage = getString(R.string.item_added_successfully);
                        }

                        Toast.makeText(AddItemActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        try {
                            // Try to parse the error message from the response
                            JSONObject errorJson = new JSONObject(responseBody);

                            // Look for message in different standard formats
                            String errorMessage;
                            if (errorJson.has("message")) {
                                errorMessage = errorJson.getString("message");
                            } else if (errorJson.has("error")) {
                                errorMessage = errorJson.getString("error");
                            } else if (errorJson.has("error_message")) {
                                errorMessage = errorJson.getString("error_message");
                            } else {
                                // If no standard error field, use the entire JSON
                                errorMessage = errorJson.toString();
                            }

                            Log.e(TAG, "Error from server: " + errorMessage);

                            // Show a more detailed error message to help debugging
                            String displayMessage = getString(R.string.error_adding_item) +
                                    " (" + response.code() + "): " + errorMessage;
                            Toast.makeText(AddItemActivity.this, displayMessage, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            Log.e(TAG, "Could not parse error response", e);

                            // Show HTTP status code for non-JSON responses
                            String displayMessage = getString(R.string.error_adding_item) +
                                    " (HTTP " + response.code() + ")";
                            Toast.makeText(AddItemActivity.this, displayMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private String getAuthToken() {
        return getSharedPreferences(getString(R.string.pref_file_name), MODE_PRIVATE)
                .getString(getString(R.string.pref_token), "");
    }
}