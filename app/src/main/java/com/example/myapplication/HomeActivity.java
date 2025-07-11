package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private Button openMapButton;
    private Button openMapNetworkButton;
    private Button viewDownloadedMapsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        openMapButton = findViewById(R.id.btn_open_map);
        openMapNetworkButton = findViewById(R.id.btn_open_map_network);
        viewDownloadedMapsButton = findViewById(R.id.btn_view_downloaded_maps);
    }

    private void setupClickListeners() {
        openMapButton.setOnClickListener(v -> {
            if (checkLocationPermissions()) {
                openMapsActivity(false); // GPS mode
            } else {
                requestLocationPermissions();
            }
        });

        openMapNetworkButton.setOnClickListener(v -> {
            // Network location only requires coarse location permission
            if (checkCoarseLocationPermission()) {
                openMapsActivity(true); // Network mode
            } else {
                requestCoarseLocationPermission();
            }
        });

        viewDownloadedMapsButton.setOnClickListener(v -> {
            openDownloadedMapsActivity();
        });
    }

    private boolean checkLocationPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private boolean checkCoarseLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCoarseLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void openMapsActivity(boolean networkMode) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("NETWORK_MODE", networkMode);
        startActivity(intent);
    }

    private void openDownloadedMapsActivity() {
        Intent intent = new Intent(this, DownloadedMapsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show();
                openMapsActivity(false); // GPS mode
            } else {
                Toast.makeText(this, "Location permissions are required to use this feature", 
                             Toast.LENGTH_LONG).show();
            }
        }
    }
}
