package com.restaurant.management;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.restaurant.management.adapters.TaxAdapter;
import com.restaurant.management.models.Tax;
import com.restaurant.management.models.TaxResponse; // Import TaxResponse
import com.restaurant.management.network.ApiClient; // Import ApiClient
import com.restaurant.management.network.ApiService; // Import ApiService

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TaxListActivity extends AppCompatActivity {

    private static final String TAG = "TaxListActivity";
    // Removed API_URL constant as it's now handled by ApiService and ApiClient

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noTaxesTextView;
    private TaxAdapter taxAdapter;
    private List<Tax> taxList = new ArrayList<>();

    // Retrofit ApiService instance
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tax_list);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_activity_tax_list);
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_view_taxes);
        progressBar = findViewById(R.id.progress_bar);
        noTaxesTextView = findViewById(R.id.text_view_no_taxes);

        // Initialize ApiService
        apiService = ApiClient.getApiService(this); // Pass context for token

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taxAdapter = new TaxAdapter(taxList);
        recyclerView.setAdapter(taxAdapter);

        // Load tax rates
        loadTaxRates();
    }

    private void loadTaxRates() {
        // Show progress and hide other views
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noTaxesTextView.setVisibility(View.GONE);

        // Use ApiService to make the network call
        apiService.getTaxRates().enqueue(new Callback<TaxResponse>() {
            @Override
            public void onResponse(Call<TaxResponse> call, Response<TaxResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TaxResponse taxResponse = response.body();
                    List<Tax> taxes = taxResponse.getData(); // Get the list of Tax objects

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (taxes == null || taxes.isEmpty()) {
                            noTaxesTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            taxList.clear();
                            taxList.addAll(taxes);
                            taxAdapter.notifyDataSetChanged();
                            recyclerView.setVisibility(View.VISIBLE);
                            noTaxesTextView.setVisibility(View.GONE);
                        }
                    });
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        noTaxesTextView.setVisibility(View.VISIBLE); // Show no taxes text on error
                        recyclerView.setVisibility(View.GONE);
                        Toast.makeText(TaxListActivity.this,
                                getString(R.string.error_loading_taxes) + ": HTTP " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(Call<TaxResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load tax rates", t);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    noTaxesTextView.setVisibility(View.VISIBLE); // Show no taxes text on network failure
                    recyclerView.setVisibility(View.GONE);
                    Toast.makeText(TaxListActivity.this,
                            getString(R.string.network_error), // Use generic network error string
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}