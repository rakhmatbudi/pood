package com.restaurant.management.helpers;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.restaurant.management.R;
import com.restaurant.management.adapters.OrderAdapter;
import com.restaurant.management.models.Order;
import com.restaurant.management.models.OrderStatus;

import java.util.ArrayList;
import java.util.List;

public class OrderListUiHelper {
    private final Context context;

    // UI Views
    private RecyclerView ordersRecyclerView;
    private OrderAdapter orderAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private AutoCompleteTextView statusAutoComplete;

    // Data
    private List<Order> ordersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private String currentStatusFilter = "open";

    // Listener interface
    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnTitleUpdateListener {
        void onTitleUpdate(String title);
    }

    private OnRefreshListener refreshListener;
    private OnTitleUpdateListener titleUpdateListener;

    public OrderListUiHelper(Context context) {
        this.context = context;
    }

    public void initializeViews(View rootView, OrderAdapter.OnOrderClickListener orderClickListener) {
        ordersRecyclerView = rootView.findViewById(R.id.orders_recycler_view);
        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout);
        progressBar = rootView.findViewById(R.id.progress_bar);
        emptyView = rootView.findViewById(R.id.empty_view);
        searchEditText = rootView.findViewById(R.id.search_edit_text);
        statusAutoComplete = rootView.findViewById(R.id.statusAutoComplete);

        // Set up RecyclerView
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        orderAdapter = new OrderAdapter(filteredOrdersList, orderClickListener, context);
        ordersRecyclerView.setAdapter(orderAdapter);

        // Set up swipe refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (refreshListener != null) {
                refreshListener.onRefresh();
            }
        });

        // Set up search
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void setRefreshListener(OnRefreshListener listener) {
        this.refreshListener = listener;
    }

    public void setTitleUpdateListener(OnTitleUpdateListener listener) {
        this.titleUpdateListener = listener;
    }

    public void setupStatusFilterSpinner(List<OrderStatus> orderStatuses) {
        // Add "All Orders" option at the beginning
        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add("All Orders");
        for (OrderStatus status : orderStatuses) {
            spinnerItems.add(status.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line, spinnerItems);
        statusAutoComplete.setAdapter(adapter);

        // Set default selection to "Open"
        statusAutoComplete.setText("Open", false);
        currentStatusFilter = "open";

        // Set up selection listener
        statusAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String selectedText = (String) parent.getItemAtPosition(position);
            if ("All Orders".equals(selectedText)) {
                currentStatusFilter = "all";
            } else {
                currentStatusFilter = selectedText.toLowerCase();
            }
            applyFilters();
        });
    }

    public void updateOrdersList(List<Order> orders) {
        this.ordersList = orders;
        applyFilters();
    }

    public void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            ordersRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public void showContent() {
        if (filteredOrdersList.isEmpty()) {
            String emptyMessage = getEmptyMessage();
            showEmptyView(emptyMessage);
        } else {
            ordersRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
        updateTitle();
    }

    public void showEmptyView(String message) {
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        ordersRecyclerView.setVisibility(View.GONE);
    }

    private void applyFilters() {
        filteredOrdersList.clear();
        String searchQuery = searchEditText.getText().toString().trim();

        for (Order order : ordersList) {
            // Apply status filter first
            boolean statusMatches = false;
            if ("all".equals(currentStatusFilter)) {
                statusMatches = true;
            } else {
                statusMatches = currentStatusFilter.equalsIgnoreCase(order.getStatus());
            }

            if (!statusMatches) {
                continue;
            }

            // Apply search filter
            if (searchQuery.isEmpty()) {
                filteredOrdersList.add(order);
            } else {
                String lowerQuery = searchQuery.toLowerCase();
                if (order.getTableNumber().toLowerCase().contains(lowerQuery) ||
                        order.getOrderNumber().toLowerCase().contains(lowerQuery)) {
                    filteredOrdersList.add(order);
                }
            }
        }

        // Update adapter
        orderAdapter.updateOrders(filteredOrdersList);

        // Show/hide empty view
        if (filteredOrdersList.isEmpty()) {
            String emptyMessage;
            if (!searchQuery.isEmpty()) {
                String statusName = "all".equals(currentStatusFilter) ? "orders" :
                        (currentStatusFilter + " orders");
                emptyMessage = "No " + statusName + " match your search";
            } else {
                emptyMessage = getEmptyMessage();
            }
            showEmptyView(emptyMessage);
        } else {
            ordersRecyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        updateTitle();
    }

    private String getEmptyMessage() {
        if ("all".equals(currentStatusFilter)) {
            return context.getString(R.string.no_orders_found);
        } else {
            String formattedStatus = currentStatusFilter.substring(0, 1).toUpperCase() +
                    currentStatusFilter.substring(1);
            return "No " + formattedStatus.toLowerCase() + " orders found";
        }
    }

    private void updateTitle() {
        if (titleUpdateListener != null) {
            String baseTitle = context.getString(R.string.orders_list_title);
            String statusDisplay;
            if ("all".equals(currentStatusFilter)) {
                statusDisplay = "All Orders";
            } else {
                statusDisplay = currentStatusFilter.substring(0, 1).toUpperCase() +
                        currentStatusFilter.substring(1) + " Orders";
            }
            String title = baseTitle + " (" + statusDisplay + ")";
            titleUpdateListener.onTitleUpdate(title);
        }
    }
}