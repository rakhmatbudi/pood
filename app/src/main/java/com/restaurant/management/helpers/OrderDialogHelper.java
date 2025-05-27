package com.restaurant.management.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.restaurant.management.R;
import com.restaurant.management.models.OrderType;

import java.util.ArrayList;
import java.util.List;

public class OrderDialogHelper {

    private final Context context;
    private final OrderListApiHelper apiHelper;

    public interface OnOrderCreatedListener {
        void onOrderCreated();
    }

    private OnOrderCreatedListener orderCreatedListener;

    public OrderDialogHelper(Context context, OrderListApiHelper apiHelper) {
        this.context = context;
        this.apiHelper = apiHelper;
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
                    .setPositiveButton("Create Order", null) // Set null listener initially
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            // Show dialog
            AlertDialog dialog = builder.create();
            dialog.show();

            // Fetch order types and populate spinner
            fetchOrderTypes(orderTypeSpinner, orderTypeProgress);

            // Get the positive button and set custom click listener
            // This needs to be done AFTER dialog.show() as buttons are initialized then
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setOnClickListener(v -> {
                    handleCreateOrderClick(dialog, sessionId, orderTypeSpinner,
                            tableNumberEditText, customerNameEditText);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error opening dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleCreateOrderClick(AlertDialog dialog, long sessionId, Spinner orderTypeSpinner,
                                        EditText tableNumberEditText, EditText customerNameEditText) {
        try {
            // Get form values
            String tableNumber = tableNumberEditText.getText().toString().trim();
            String customerName = customerNameEditText.getText().toString().trim();

            // Get selected order type
            OrderType selectedOrderType = null;
            if (orderTypeSpinner.getSelectedItem() != null && orderTypeSpinner.getSelectedItemPosition() > 0) {
                Object selectedItem = orderTypeSpinner.getSelectedItem();
                if (selectedItem instanceof OrderType) {
                    selectedOrderType = (OrderType) selectedItem;
                }
            }

            // Validate required fields
            if (selectedOrderType == null || selectedOrderType.getId() == 0) { // Check ID for placeholder
                Toast.makeText(context, "Please select an order type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tableNumber.isEmpty()) {
                tableNumberEditText.setError("This field is required");
                return;
            }

            // Show loading state
            final Toast loadingToast = Toast.makeText(context, "Creating order...", Toast.LENGTH_LONG);
            loadingToast.show();

            // Disable input during API call
            setDialogEnabled(dialog, orderTypeSpinner, tableNumberEditText, customerNameEditText, false);

            // Create order
            apiHelper.createOrder(sessionId, tableNumber, customerName, selectedOrderType,
                    new OrderListApiHelper.CreateOrderCallback() {
                        @Override
                        public void onSuccess(String message) {
                            // Ensure UI updates happen on main thread
                            if (context instanceof android.app.Activity) {
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    try {
                                        loadingToast.cancel();
                                        dialog.dismiss();
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                        if (orderCreatedListener != null) {
                                            orderCreatedListener.onOrderCreated();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(context, "Error during UI update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            // Ensure UI updates happen on main thread
                            if (context instanceof android.app.Activity) {
                                ((android.app.Activity) context).runOnUiThread(() -> {
                                    try {
                                        loadingToast.cancel();
                                        setDialogEnabled(dialog, orderTypeSpinner, tableNumberEditText, customerNameEditText, true);
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(context, "Error during UI update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchOrderTypes(Spinner orderTypeSpinner, ProgressBar orderTypeProgress) {
        try {
            // Show loading
            orderTypeProgress.setVisibility(View.VISIBLE);
            orderTypeSpinner.setEnabled(false);

            apiHelper.fetchOrderTypes(new OrderListApiHelper.OrderTypesCallback() {
                @Override
                public void onSuccess(List<OrderType> orderTypes) {
                    // Ensure UI updates happen on main thread
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            try {
                                orderTypeProgress.setVisibility(View.GONE);
                                orderTypeSpinner.setEnabled(true);
                                setupOrderTypeSpinner(orderTypeSpinner, orderTypes);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Fallback to default types if UI update fails
                                setupOrderTypeSpinner(orderTypeSpinner, apiHelper.getFallbackOrderTypes());
                                Toast.makeText(context, "Error setting up order types UI, using fallback.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // If context is not an activity, run on current thread (caution: might not be UI thread)
                        try {
                            orderTypeProgress.setVisibility(View.GONE);
                            orderTypeSpinner.setEnabled(true);
                            setupOrderTypeSpinner(orderTypeSpinner, orderTypes);
                        } catch (Exception e) {
                            e.printStackTrace();
                            setupOrderTypeSpinner(orderTypeSpinner, apiHelper.getFallbackOrderTypes());
                            Toast.makeText(context, "Error setting up order types UI, using fallback.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    // Ensure UI updates happen on main thread
                    if (context instanceof android.app.Activity) {
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            try {
                                orderTypeProgress.setVisibility(View.GONE);
                                orderTypeSpinner.setEnabled(true);
                                // Use fallback data immediately
                                setupOrderTypeSpinner(orderTypeSpinner, apiHelper.getFallbackOrderTypes());
                                Toast.makeText(context, "Using offline order types: " + errorMessage, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                                // Emergency fallback
                                setupEmergencySpinner(orderTypeSpinner);
                                Toast.makeText(context, "Critical error setting up order types.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // If context is not an activity, run on current thread (caution: might not be UI thread)
                        try {
                            orderTypeProgress.setVisibility(View.GONE);
                            orderTypeSpinner.setEnabled(true);
                            setupOrderTypeSpinner(orderTypeSpinner, apiHelper.getFallbackOrderTypes());
                            Toast.makeText(context, "Using offline order types: " + errorMessage, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            e.printStackTrace();
                            setupEmergencySpinner(orderTypeSpinner);
                            Toast.makeText(context, "Critical error setting up order types.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            // Add timeout fallback - if API doesn't respond in 10 seconds, use fallback
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (context instanceof android.app.Activity) { // Ensure running on UI thread for UI check/update
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        try {
                            // Check if still loading (progress bar still visible)
                            if (orderTypeProgress.getVisibility() == View.VISIBLE) {
                                orderTypeProgress.setVisibility(View.GONE);
                                orderTypeSpinner.setEnabled(true);
                                setupOrderTypeSpinner(orderTypeSpinner, apiHelper.getFallbackOrderTypes());
                                Toast.makeText(context, "Loading timeout - using offline order types", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            }, 10000); // 10 second timeout

        } catch (Exception e) {
            e.printStackTrace();
            // Emergency fallback
            orderTypeProgress.setVisibility(View.GONE);
            orderTypeSpinner.setEnabled(true);
            setupEmergencySpinner(orderTypeSpinner);
            Toast.makeText(context, "Initial error fetching order types, using emergency fallback.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupOrderTypeSpinner(Spinner orderTypeSpinner, List<OrderType> orderTypes) {
        try {
            // Ensure we have a valid list, use fallback if null or empty
            if (orderTypes == null || orderTypes.isEmpty()) {
                orderTypes = apiHelper.getFallbackOrderTypes();
            }

            // Add a placeholder at the beginning
            List<OrderType> spinnerItems = new ArrayList<>();
            // Use a placeholder with ID 0 to easily identify if user selected it
            spinnerItems.add(new OrderType(0, "Select Order Type"));
            spinnerItems.addAll(orderTypes);

            ArrayAdapter<OrderType> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, spinnerItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            orderTypeSpinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            // Emergency fallback - create a simple adapter with just the placeholder
            setupEmergencySpinner(orderTypeSpinner); // Call the more basic emergency setup
        }
    }

    private void setupEmergencySpinner(Spinner orderTypeSpinner) {
        try {
            // Create the most basic spinner possible
            List<String> basicItems = new ArrayList<>();
            basicItems.add("Select Order Type");
            basicItems.add("Dine In");
            basicItems.add("Take Away");

            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, basicItems);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            orderTypeSpinner.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Could not set up basic spinner, critical UI error.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setDialogEnabled(AlertDialog dialog, Spinner orderTypeSpinner,
                                  EditText tableNumberEditText, EditText customerNameEditText,
                                  boolean enabled) {
        try {
            // These buttons are only available after dialog.show()
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) positiveButton.setEnabled(enabled);
            if (negativeButton != null) negativeButton.setEnabled(enabled);
            if (orderTypeSpinner != null) orderTypeSpinner.setEnabled(enabled);
            if (tableNumberEditText != null) tableNumberEditText.setEnabled(enabled);
            if (customerNameEditText != null) customerNameEditText.setEnabled(enabled);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Error setting dialog enabled state: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}