package com.restaurant.management.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.restaurant.management.R;
import com.restaurant.management.RestaurantApplication;
import com.restaurant.management.models.CreateOrderRequest;
import com.restaurant.management.models.CreateOrderResponse;
import com.restaurant.management.models.OrderType;
import com.restaurant.management.network.ApiService;
import com.restaurant.management.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDialogHelper {
    private static final String TAG = "OrderDialogHelper";

    private final Context context;
    private final ApiService apiService;

    public interface OnOrderCreatedListener {
        void onOrderCreated();
    }

    private OnOrderCreatedListener orderCreatedListener;

    public OrderDialogHelper(Context context) {
        this.context = context;
        this.apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void setOnOrderCreatedListener(OnOrderCreatedListener listener) {
        this.orderCreatedListener = listener;
    }

    public void showNewOrderDialog(long sessionId) {
        try {
            // Create dialog view
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_order, null);

            // Get views from dialog layout
            Spinner orderTypeSpinner = dialogView.findViewById(R.id.order_type_spinner);
            ProgressBar orderTypeProgress = dialogView.findViewById(R.id.order_type_progress);
            EditText tableNumberEditText = dialogView.findViewById(R.id.table_number_edit_text);
            EditText customerNameEditText = dialogView.findViewById(R.id.customer_name_edit_text);

            // Create dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Create New Order")
                    .setView(dialogView)
                    .setPositiveButton("Create Order", null)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            // Show dialog
            AlertDialog dialog = builder.create();
            dialog.show();

            // Load order types from offline cache
            loadOrderTypesFromCache(orderTypeSpinner, orderTypeProgress);

            // Set up create order button click listener
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> createOrder(dialog, sessionId, orderTypeSpinner,
                        tableNumberEditText, customerNameEditText));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening dialog", e);
            Toast.makeText(context, "Error opening dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadOrderTypesFromCache(Spinner orderTypeSpinner, ProgressBar orderTypeProgress) {
        try {
            // Show loading briefly for UI consistency
            orderTypeProgress.setVisibility(View.VISIBLE);
            orderTypeSpinner.setEnabled(false);

            // Get cached order types from application
            RestaurantApplication app = (RestaurantApplication) context.getApplicationContext();
            List<OrderType> cachedOrderTypes = app.getCachedOrderTypes();

            // Hide loading
            orderTypeProgress.setVisibility(View.GONE);
            orderTypeSpinner.setEnabled(true);

            if (cachedOrderTypes != null && !cachedOrderTypes.isEmpty()) {
                setupOrderTypeSpinner(orderTypeSpinner, cachedOrderTypes);
            } else {
                // No cached data - use fallback
                setupOrderTypeSpinner(orderTypeSpinner, getFallbackOrderTypes());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading order types from cache", e);
            orderTypeProgress.setVisibility(View.GONE);
            orderTypeSpinner.setEnabled(true);
            setupOrderTypeSpinner(orderTypeSpinner, getFallbackOrderTypes());
        }
    }

    private void setupOrderTypeSpinner(Spinner orderTypeSpinner, List<OrderType> orderTypes) {
        try {
            // Create spinner items with placeholder
            List<OrderType> spinnerItems = new ArrayList<>();
            spinnerItems.add(new OrderType(0, "Select Order Type")); // Placeholder
            spinnerItems.addAll(orderTypes);

            // Set up adapter
            ArrayAdapter<OrderType> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            orderTypeSpinner.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up order type spinner", e);

            // Emergency fallback with basic string items
            List<String> basicItems = new ArrayList<>();
            basicItems.add("Select Order Type");
            basicItems.add("Dine In");
            basicItems.add("Take Away");
            basicItems.add("Delivery");

            ArrayAdapter<String> basicAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, basicItems);
            basicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            orderTypeSpinner.setAdapter(basicAdapter);
        }
    }

    private List<OrderType> getFallbackOrderTypes() {
        List<OrderType> fallbackTypes = new ArrayList<>();
        fallbackTypes.add(new OrderType(1, "Dine In"));
        fallbackTypes.add(new OrderType(2, "Take Away"));
        fallbackTypes.add(new OrderType(3, "Delivery"));
        fallbackTypes.add(new OrderType(4, "GoFood"));
        fallbackTypes.add(new OrderType(5, "GrabFood"));
        fallbackTypes.add(new OrderType(6, "ShopeeFood"));
        return fallbackTypes;
    }

    private void createOrder(AlertDialog dialog, long sessionId, Spinner orderTypeSpinner,
                             EditText tableNumberEditText, EditText customerNameEditText) {
        try {
            // Get form values
            String tableNumber = tableNumberEditText.getText().toString().trim();
            String customerName = customerNameEditText.getText().toString().trim();

            // Validate input
            if (!validateInput(orderTypeSpinner, tableNumberEditText, tableNumber)) {
                return;
            }

            // Get selected order type
            OrderType selectedOrderType = getSelectedOrderType(orderTypeSpinner);
            if (selectedOrderType == null) {
                Toast.makeText(context, "Please select an order type", Toast.LENGTH_SHORT).show();
                return;
            }

            // Disable UI during API call
            setDialogControlsEnabled(dialog, false);
            Toast loadingToast = Toast.makeText(context, "Creating order...", Toast.LENGTH_LONG);
            loadingToast.show();

            // Create order request
            CreateOrderRequest request = new CreateOrderRequest(
                    sessionId,                    // long sessionId
                    tableNumber,                  // String tableNumber
                    null,                        // Long customerId - using null since we don't have customer ID
                    selectedOrderType.getId(),   // long orderTypeId
                    null                         // Long serverId - using null as default
            );

            // Make API call
            apiService.createOrder(request).enqueue(new Callback<CreateOrderResponse>() {
                @Override
                public void onResponse(Call<CreateOrderResponse> call, Response<CreateOrderResponse> response) {
                    handleCreateOrderResponse(dialog, loadingToast, response);
                }

                @Override
                public void onFailure(Call<CreateOrderResponse> call, Throwable t) {
                    handleCreateOrderFailure(dialog, loadingToast, t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating order", e);
            Toast.makeText(context, "Error creating order", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(Spinner orderTypeSpinner, EditText tableNumberEditText, String tableNumber) {
        if (tableNumber.isEmpty()) {
            tableNumberEditText.setError("Table number is required");
            return false;
        }

        if (orderTypeSpinner.getSelectedItemPosition() <= 0) {
            Toast.makeText(context, "Please select an order type", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private OrderType getSelectedOrderType(Spinner orderTypeSpinner) {
        try {
            Object selectedItem = orderTypeSpinner.getSelectedItem();

            if (selectedItem instanceof OrderType) {
                OrderType selected = (OrderType) selectedItem;
                return selected.getId() > 0 ? selected : null; // Ignore placeholder
            } else if (selectedItem instanceof String) {
                // Handle emergency fallback case where we used strings
                String selectedString = (String) selectedItem;
                if (!"Select Order Type".equals(selectedString)) {
                    // Create a basic OrderType for fallback
                    int id = getFallbackOrderTypeId(selectedString);
                    return new OrderType(id, selectedString);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting selected order type", e);
        }

        return null;
    }

    private int getFallbackOrderTypeId(String orderTypeName) {
        // Map string names to IDs for emergency fallback
        switch (orderTypeName) {
            case "Dine In": return 1;
            case "Take Away": return 2;
            case "Delivery": return 3;
            case "GoFood": return 4;
            case "GrabFood": return 5;
            case "ShopeeFood": return 6;
            default: return 1; // Default to Dine In
        }
    }

    private void handleCreateOrderResponse(AlertDialog dialog, Toast loadingToast,
                                           Response<CreateOrderResponse> response) {
        runOnUiThread(() -> {
            try {
                loadingToast.cancel();

                if (response.isSuccessful() && response.body() != null) {
                    CreateOrderResponse orderResponse = response.body();

                    if (orderResponse.isSuccess()) {
                        // Success - close dialog and notify listener
                        dialog.dismiss();

                        String successMessage = "Order created successfully";
                        if (orderResponse.getData() != null) {
                            successMessage += " (Order #" + orderResponse.getOrderId() + ")";
                        }

                        Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();

                        if (orderCreatedListener != null) {
                            orderCreatedListener.onOrderCreated();
                        }
                    } else {
                        // API error in response body
                        setDialogControlsEnabled(dialog, true);
                        String errorMessage = "Failed to create order";
                        if (orderResponse.getStatus() != null && !orderResponse.getStatus().isEmpty()) {
                            errorMessage += ": " + orderResponse.getStatus();
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // HTTP error
                    handleHttpError(dialog, response);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling response", e);
                setDialogControlsEnabled(dialog, true);
                Toast.makeText(context, "Error processing response", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCreateOrderFailure(AlertDialog dialog, Toast loadingToast, Throwable t) {
        Log.e(TAG, "Order creation failed", t);

        runOnUiThread(() -> {
            try {
                loadingToast.cancel();
                setDialogControlsEnabled(dialog, true);
                Toast.makeText(context, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error handling failure", e);
            }
        });
    }

    private void handleHttpError(AlertDialog dialog, Response<CreateOrderResponse> response) {
        setDialogControlsEnabled(dialog, true);
        String errorMessage = "Server error: " + response.code();

        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                errorMessage = errorBody;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading error body", e);
        }

        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void setDialogControlsEnabled(AlertDialog dialog, boolean enabled) {
        try {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) positiveButton.setEnabled(enabled);
            if (negativeButton != null) negativeButton.setEnabled(enabled);

            // Enable/disable all input controls in the dialog
            View dialogView = dialog.findViewById(android.R.id.content);
            if (dialogView != null) {
                setViewGroupEnabled(dialogView, enabled);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting dialog controls enabled state", e);
        }
    }

    private void setViewGroupEnabled(View view, boolean enabled) {
        if (view instanceof Spinner || view instanceof EditText) {
            view.setEnabled(enabled);
        } else if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setViewGroupEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }

    private void runOnUiThread(Runnable runnable) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(runnable);
        } else {
            // Fallback - run immediately (may not be on UI thread)
            runnable.run();
        }
    }
}