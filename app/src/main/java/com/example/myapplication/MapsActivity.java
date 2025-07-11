package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Button getLocationButton;
    private Button normalButton, satelliteButton, hybridButton, terrainButton;
    private TextView locationInfoTextView;
    private Location currentLocation;
    private ProgressDialog dialog;
    private Handler uiHandler;
    private Timer timer;
    private float currentAccuracy = Float.MAX_VALUE;
    private boolean isNetworkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Check if we're in network mode
        isNetworkMode = getIntent().getBooleanExtra("NETWORK_MODE", false);

        initializeViews();
        initializeLocationServices();
        setupMap();
    }

    private void initializeViews() {
        getLocationButton = findViewById(R.id.btn_get_location);
        locationInfoTextView = findViewById(R.id.tv_location_info);

        // Map type buttons
        normalButton = findViewById(R.id.btn_normal);
        satelliteButton = findViewById(R.id.btn_satellite);
        hybridButton = findViewById(R.id.btn_hybrid);
        terrainButton = findViewById(R.id.btn_terrain);

        uiHandler = new Handler(Looper.getMainLooper());

        setupClickListeners();
        updateUIForMode();
    }

    private void updateUIForMode() {
        if (isNetworkMode) {
            getLocationButton.setText("Get Network Location");
            locationInfoTextView.setText("Network mode: Using cell towers, WiFi, and IP location");
        } else {
            getLocationButton.setText(getString(R.string.get_current_location));
            locationInfoTextView.setText(getString(R.string.location_status));
        }
    }

    private void setupClickListeners() {
        getLocationButton.setOnClickListener(v -> {
            if (isNetworkMode) {
                getNetworkLocation();
            } else {
                getLocationFix();
            }
        });

        // Map type button listeners
        normalButton.setOnClickListener(v -> changeMapType(GoogleMap.MAP_TYPE_NORMAL));
        satelliteButton.setOnClickListener(v -> changeMapType(GoogleMap.MAP_TYPE_SATELLITE));
        hybridButton.setOnClickListener(v -> changeMapType(GoogleMap.MAP_TYPE_HYBRID));
        terrainButton.setOnClickListener(v -> changeMapType(GoogleMap.MAP_TYPE_TERRAIN));
    }

    private void changeMapType(int mapType) {
        if (mMap != null) {
            mMap.setMapType(mapType);
            updateButtonStates(mapType);

            // Show appropriate features based on map type
            switch (mapType) {
                case GoogleMap.MAP_TYPE_HYBRID:
                case GoogleMap.MAP_TYPE_SATELLITE:
                    mMap.setTrafficEnabled(true);
                    mMap.setBuildingsEnabled(true);
                    break;
                case GoogleMap.MAP_TYPE_NORMAL:
                case GoogleMap.MAP_TYPE_TERRAIN:
                    mMap.setTrafficEnabled(false);
                    mMap.setBuildingsEnabled(true);
                    break;
            }
        }
    }

    private void updateButtonStates(int currentMapType) {
        // Reset all buttons to default color
        int defaultColor = getResources().getColor(android.R.color.holo_blue_light);
        int selectedColor = getResources().getColor(android.R.color.holo_orange_light);

        normalButton.setBackgroundColor(defaultColor);
        satelliteButton.setBackgroundColor(defaultColor);
        hybridButton.setBackgroundColor(defaultColor);
        terrainButton.setBackgroundColor(defaultColor);

        // Highlight the selected button
        switch (currentMapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
                normalButton.setBackgroundColor(selectedColor);
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                satelliteButton.setBackgroundColor(selectedColor);
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                hybridButton.setBackgroundColor(selectedColor);
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                terrainButton.setBackgroundColor(selectedColor);
                break;
        }
    }

    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    currentAccuracy = location.getAccuracy();
                }
            }
        };
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Configure map settings for satellite view with labels
        configureMapSettings();

        // Enable location layer if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void configureMapSettings() {
        if (mMap != null) {
            // Set map type to hybrid (satellite with street names and labels)
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            // Enable all map controls and features
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.getUiSettings().setScrollGesturesEnabled(true);
            mMap.getUiSettings().setTiltGesturesEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);

            // Enable traffic layer for additional street information
            mMap.setTrafficEnabled(true);

            // Enable indoor maps for building details
            mMap.setIndoorEnabled(true);

            // Enable buildings layer for 3D buildings
            mMap.setBuildingsEnabled(true);

            // Update button states to show hybrid as selected
            updateButtonStates(GoogleMap.MAP_TYPE_HYBRID);
        }
    }

    private void getNetworkLocation() {
        final String message = getString(R.string.please_wait_network);
        dialog = new ProgressDialog(MapsActivity.this);
        dialog.setIcon(R.drawable.zona);
        dialog.setTitle(getString(R.string.fetching_network_location));
        dialog.setMessage(message);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        // Use network-based location (cell towers, WiFi, IP)
        startNetworkLocationUpdates();
    }

    private void startNetworkLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                        Location location = locationResult.getLocations().get(0);
                        currentLocation = location;
                        currentAccuracy = location.getAccuracy();

                        // Stop updates after getting first result
                        fusedLocationClient.removeLocationUpdates(this);

                        uiHandler.post(() -> {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            updateUI(location);
                            makeCenterToast(getString(R.string.network_location_found) +
                                          " (Accuracy: " + (int)location.getAccuracy() + "m)",
                                          Toast.LENGTH_LONG);
                        });
                    }
                }
            }, Looper.getMainLooper());

            // Set timeout for network location
            uiHandler.postDelayed(() -> {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    makeCenterToast(getString(R.string.network_location_failed), Toast.LENGTH_LONG);
                }
            }, 15000); // 15 second timeout
        }
    }

    private void getLocationFix() {
        if (isGPSEnabled()) {
            final String message = getString(R.string.please_wait_fetching);
            dialog = new ProgressDialog(MapsActivity.this);
            dialog.setIcon(R.drawable.zona);
            dialog.setTitle(getString(R.string.fetching_location));
            dialog.setMessage(message);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            startLocationUpdates();

            timer = new Timer(true);
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    uiHandler.post(new Runnable() {
                        public void run() {
                            dialog.setMessage(message + "\nCurrent Accuracy : " + getCurrentAccuracy() + "m");
                            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    stopLocationUpdates();
                                    timer.cancel();
                                    makeCenterToast("Please Wait ...", Toast.LENGTH_SHORT);
                                }
                            });

                            updateProgressBasedOnAccuracy();
                        }
                    });
                }
            };
            timer.schedule(task, 0, 5000);
        } else {
            uiHandler.post(new Runnable() {
                public void run() {
                    makeCenterToast(getString(R.string.gps_disabled), Toast.LENGTH_LONG);
                }
            });
        }
    }

    private void updateProgressBasedOnAccuracy() {
        int accuracy = (int) getCurrentAccuracy();
        
        if (accuracy <= 1000) {
            dialog.setProgress(20);
            if (accuracy <= 500) {
                dialog.setProgress(30);
                if (accuracy <= 100) {
                    dialog.setProgress(40);
                    if (accuracy <= 50) {
                        dialog.setProgress(50);
                        if (accuracy <= 40) {
                            dialog.setProgress(70);
                            if (accuracy <= 30) {
                                dialog.setProgress(80);
                                dialog.setProgress(100);
                                dialog.dismiss();
                                timer.cancel();
                                stopLocationUpdates();
                                updateUI(getCurrentLocation());
                            }
                        }
                    }
                }
            }
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdateDelayMillis(10000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private boolean isGPSEnabled() {
        // For simplicity, returning true. In production, check LocationManager
        return true;
    }

    private float getCurrentAccuracy() {
        return currentAccuracy == Float.MAX_VALUE ? 9999 : currentAccuracy;
    }

    private Location getCurrentLocation() {
        return currentLocation;
    }

    private void updateUI(Location location) {
        if (location != null && mMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            // Add marker and move camera
            mMap.clear();
            String markerTitle = isNetworkMode ? "Network Location" : "GPS Location";
            String markerSnippet = String.format("Accuracy: %.0fm | Source: %s",
                    location.getAccuracy(),
                    isNetworkMode ? "Cell/WiFi/IP" : "GPS/GNSS");

            mMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title(markerTitle)
                    .snippet(markerSnippet));

            // Use higher zoom level for better street detail visibility
            // Network location typically less accurate, so zoom out a bit more
            int zoomLevel = isNetworkMode ? 16 : 18;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, zoomLevel));

            // Update info text
            String locationSource = isNetworkMode ? "Network" : "GPS";
            String locationInfo = String.format("%s Location:\nLat: %.6f, Lng: %.6f\nAccuracy: %.1fm",
                    locationSource, location.getLatitude(), location.getLongitude(), location.getAccuracy());
            locationInfoTextView.setText(locationInfo);

            String toastMessage = isNetworkMode ?
                    "Network location found with accuracy: " + (int)location.getAccuracy() + "m" :
                    "GPS location updated with accuracy: " + (int)location.getAccuracy() + "m";
            makeCenterToast(toastMessage, Toast.LENGTH_SHORT);
        }
    }

    private void makeCenterToast(String message, int duration) {
        Toast toast = Toast.makeText(getApplicationContext(), message, duration);
        toast.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        stopLocationUpdates();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
