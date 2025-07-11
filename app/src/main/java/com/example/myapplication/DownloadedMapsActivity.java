package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.adapter.MapListAdapter;
import com.example.myapplication.database.MapDatabaseHelper;
import com.example.myapplication.model.DownloadedMap;

import java.util.List;

/**
 * Activity to display the list of downloaded/cached maps with their labels
 */
public class DownloadedMapsActivity extends AppCompatActivity implements MapListAdapter.OnMapClickListener {
    
    private RecyclerView recyclerView;
    private MapListAdapter adapter;
    private TextView emptyStateTextView;
    private MapDatabaseHelper databaseHelper;
    private List<DownloadedMap> downloadedMaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded_maps);
        
        initializeViews();
        setupRecyclerView();
        loadDownloadedMaps();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view_maps);
        emptyStateTextView = findViewById(R.id.tv_empty_state);
        
        // Initialize database helper
        databaseHelper = MapDatabaseHelper.getInstance(this);
        
        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Downloaded Maps");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MapListAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void loadDownloadedMaps() {
        downloadedMaps = databaseHelper.getAllDownloadedMaps();
        
        if (downloadedMaps.isEmpty()) {
            showEmptyState();
        } else {
            showMapsList();
            adapter.setMaps(downloadedMaps);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyStateTextView.setVisibility(View.VISIBLE);
        emptyStateTextView.setText("No downloaded maps found.\n\nGo to Maps and download some areas for offline use!");
    }

    private void showMapsList() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyStateTextView.setVisibility(View.GONE);
    }

    @Override
    public void onMapClick(DownloadedMap map) {
        // Open the map at the saved location
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("OPEN_SAVED_MAP", true);
        intent.putExtra("MAP_CENTER_LAT", map.getCenterLatitude());
        intent.putExtra("MAP_CENTER_LNG", map.getCenterLongitude());
        intent.putExtra("MAP_ZOOM_LEVEL", map.getZoomLevel());
        intent.putExtra("MAP_TYPE", map.getMapType());
        intent.putExtra("MAP_LABEL", map.getLabel());
        startActivity(intent);
    }

    @Override
    public void onMapLongClick(DownloadedMap map) {
        // Show options dialog for long click
        showMapOptionsDialog(map);
    }

    private void showMapOptionsDialog(DownloadedMap map) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(map.getLabel());
        builder.setMessage("Choose an action for this downloaded map:");

        String[] options = {"View Details", "Open Map", "Delete"};
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // View Details
                    showMapDetails(map);
                    break;
                case 1: // Open Map
                    onMapClick(map);
                    break;
                case 2: // Delete
                    confirmDeleteMap(map);
                    break;
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showMapDetails(DownloadedMap map) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Map Details: " + map.getLabel());
        
        StringBuilder details = new StringBuilder();
        details.append("Label: ").append(map.getLabel()).append("\n\n");
        
        if (map.getDescription() != null && !map.getDescription().trim().isEmpty()) {
            details.append("Description: ").append(map.getDescription()).append("\n\n");
        }
        
        details.append("Location: ").append(map.getBoundsDescription()).append("\n\n");
        details.append("Map Type: ").append(map.getMapType()).append("\n\n");
        details.append("Zoom Level: ").append(map.getZoomLevel()).append("\n\n");
        details.append("Downloaded: ").append(android.text.format.DateFormat.format("MMM dd, yyyy 'at' hh:mm a", map.getDownloadDate())).append("\n\n");
        details.append("Size: ").append(map.getFormattedSize()).append("\n\n");
        details.append("Status: ").append(map.isAvailableOffline() ? "Available Offline" : "Not Available Offline");

        builder.setMessage(details.toString());
        builder.setPositiveButton("Open Map", (dialog, which) -> onMapClick(map));
        builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void confirmDeleteMap(DownloadedMap map) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Map");
        builder.setMessage("Are you sure you want to delete \"" + map.getLabel() + "\"?\n\nThis will remove the map from your downloaded list. The cached map data may still be available in Google Maps cache.");
        
        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteMap(map);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        
        // Make dialog not dismissible by clicking outside
        builder.setCancelable(false);
        builder.show();
    }

    private void deleteMap(DownloadedMap map) {
        try {
            databaseHelper.deleteDownloadedMap(map.getId());
            
            // Remove from list and update adapter
            downloadedMaps.remove(map);
            adapter.setMaps(downloadedMaps);
            
            // Show empty state if no maps left
            if (downloadedMaps.isEmpty()) {
                showEmptyState();
            }
            
            Toast.makeText(this, "Map \"" + map.getLabel() + "\" deleted successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error deleting map: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadDownloadedMaps();
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back button in action bar
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to home activity
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
