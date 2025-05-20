package com.restaurant.management;

import android.os.Bundle;
import android.text.TextUtils;
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
    private ProductItemAdapter menuItemAdapter;

    private long orderId;
    private String tableNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Log.d(TAG, "onCreate: Starting AddItemActivity");

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

        //Log.d(TAG, "Views initialized");

        // Get order details from intent
        orderId = getIntent().getLongExtra("order_id", -1);
        tableNumber = getIntent().getStringExtra("table_number");

        //Log.d(TAG, "Order ID: " + orderId + ", Table: " + tableNumber);

        if (orderId == -1 || tableNumber == null) {
            Toast.makeText(this, R.string.invalid_order_details, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set table number
        tableNumberTextView.setText(getString(R.string.table_number_format, tableNumber));

        // Set up RecyclerView
        //Log.d(TAG, "Setting up RecyclerView");
        menuItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuItemAdapter = new ProductItemAdapter(menuItems, this);
        menuItemsRecyclerView.setAdapter(menuItemAdapter);

        // Set up search button
        searchButton.setOnClickListener(v -> searchMenuItems());

        // Load initial menu items
        fetchMenuItems("");
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
        String query = searchEditText.getText().toString().trim();
        fetchMenuItems(query);
    }

    private void fetchMenuItems(String searchQuery) {
        //Log.d(TAG, "fetchMenuItems called with query: " + (searchQuery.isEmpty() ? "empty" : searchQuery));

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        // Build the URL
        String apiUrl = BASE_API_URL + "menu-items";
        if (!TextUtils.isEmpty(searchQuery)) {
            apiUrl += "?search=" + searchQuery;
        }

        //Log.d(TAG, "Fetching menu items from: " + apiUrl);

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
                //Log.e(TAG, "API request failed", e);
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
                    //Log.d(TAG, "API response received: " + response.code());
                    //Log.d(TAG, "Response body length: " + responseBody.length());

                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response.code());
                    }

                    JSONObject jsonResponse = new JSONObject(responseBody);
                    List<ProductItem> items = parseMenuItems(jsonResponse);

                    //Log.d(TAG, "Parsed " + items.size() + " menu items");

                    runOnUiThread(() -> {
                        menuItems.clear();
                        menuItems.addAll(items);
                        menuItemAdapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        menuItemsRecyclerView.setVisibility(View.VISIBLE);
                    });

                } catch (Exception e) {
                    //Log.e(TAG, "Error processing response", e);
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
                //Log.d(TAG, "Parsing array of items: " + itemsArray.length());
            } else {
                // If "data" is a single object, create an array with just that object
                JSONObject singleItem = jsonResponse.getJSONObject("data");
                itemsArray = new JSONArray();
                itemsArray.put(singleItem);
                //Log.d(TAG, "Parsing single item as array");
            }

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemJson = itemsArray.getJSONObject(i);
                ProductItem item = new ProductItem();

                item.setId(itemJson.optLong("id", -1));
                item.setName(itemJson.optString("name", ""));
                item.setDescription(itemJson.optString("description", ""));
                item.setPrice(parsePrice(itemJson.optString("price", "0")));

                // Handle different category field names
                if (itemJson.has("category_name") && !itemJson.isNull("category_name")) {
                    item.setCategory(itemJson.optString("category_name", ""));
                } else if (itemJson.has("category") && !itemJson.isNull("category")) {
                    item.setCategory(itemJson.optString("category", ""));
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
                    //Log.d(TAG, "Found " + variantsArray.length() + " variants for item " + item.getName());

                    for (int j = 0; j < variantsArray.length(); j++) {
                        JSONObject variantJson = variantsArray.getJSONObject(j);
                        Variant variant = new Variant();
                        variant.setId(variantJson.optLong("id", -1));
                        variant.setName(variantJson.optString("name", ""));
                        variant.setPrice(parsePrice(variantJson.optString("price", "0")));
                        variants.add(variant);

                        //Log.d(TAG, "Added variant: " + variant.getName() +
                        //        " (ID: " + variant.getId() + ") with price: " + variant.getPrice());
                    }
                } else {
                    //Log.d(TAG, "No variants found for item " + item.getName());
                }

                if (!variants.isEmpty()) {
                    //Log.d(TAG, "Setting " + variants.size() + " variants on item " + item.getName());
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
            //Log.e(TAG, "Error parsing price: " + priceString, e);
            return 0.0;
        }
    }

    @Override
    public void onItemClick(ProductItem menuItem) {


        // Show item detail dialog with quantity selector and variants if available
        ItemDetailDialog dialog = new ItemDetailDialog(this, menuItem, new ItemDetailDialog.OnItemAddListener() {
            @Override
            public void onItemAdd(ProductItem selectedItem, Long variantId, int quantity, String notes) {
                // Add item to order
                addItemToOrder(selectedItem, variantId, quantity, notes);
            }
        });
        dialog.show();
    }

    private void addItemToOrder(ProductItem menuItem, Long variantId, int quantity, String notes) {


        if (quantity <= 0) {
            Toast.makeText(this, R.string.invalid_quantity, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        menuItemsRecyclerView.setVisibility(View.GONE);

        // Get the unit price based on whether a variant is selected
        double unitPrice = menuItem.getPrice();
        if (variantId != null) {
            // Find the selected variant
            for (Variant variant : menuItem.getVariants()) {
                if (variant.getId() == variantId) {
                    unitPrice = variant.getPrice();
                    Log.d(TAG, "Using variant price: " + unitPrice);
                    break;
                }
            }
        }

        // Calculate the total price
        double totalPrice = unitPrice * quantity;

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
                        Toast.makeText(AddItemActivity.this,
                                getString(R.string.item_added_successfully),
                                Toast.LENGTH_SHORT).show();
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