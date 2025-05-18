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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TaxListActivity extends AppCompatActivity {

    private static final String TAG = "TaxListActivity";
    private static final String API_URL = "https://api.pood.lol/taxes/rates";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView noTaxesTextView;
    private TaxAdapter taxAdapter;
    private List<Tax> taxList = new ArrayList<>();
    private OkHttpClient client = new OkHttpClient();

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

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to load tax rates", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(TaxListActivity.this,
                            getString(R.string.error_loading_taxes) + ": " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response);
                    }

                    String responseBody = response.body().string();
                    Log.d(TAG, "API Response: " + responseBody);

                    JSONObject jsonObject = new JSONObject(responseBody);

                    if ("success".equals(jsonObject.getString("status")) &&
                            jsonObject.has("data")) {

                        JSONArray dataArray = jsonObject.getJSONArray("data");
                        List<Tax> taxes = new ArrayList<>();

                        for (int i = 0; i < dataArray.length(); i++) {
                            JSONObject taxObject = dataArray.getJSONObject(i);
                            int id = taxObject.getInt("id");
                            String name = taxObject.getString("name");
                            String description = taxObject.getString("description");
                            String amount = taxObject.getString("amount");

                            taxes.add(new Tax(id, name, description, amount));
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            if (taxes.isEmpty()) {
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
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            noTaxesTextView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            Toast.makeText(TaxListActivity.this,
                                    R.string.error_loading_taxes,
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing response", e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TaxListActivity.this,
                                getString(R.string.error_loading_taxes) + ": " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
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