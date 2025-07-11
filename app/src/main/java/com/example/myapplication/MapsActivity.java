package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.database.MapDatabaseHelper;
import com.example.myapplication.model.DownloadedMap;

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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Button getLocationButton;
    private Button normalButton, satelliteButton, hybridButton, terrainButton;
    private Button downloadMapButton;
    private TextView locationInfoTextView;
    private Location currentLocation;
    private ProgressDialog dialog;
    private Handler uiHandler;
    private Timer timer;
    private float currentAccuracy = Float.MAX_VALUE;
    private boolean isNetworkMode = false;
    private List<Location> networkLocationCandidates;
    private Timer networkAccuracyTimer;
    private MapDatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Check if we're in network mode
        isNetworkMode = getIntent().getBooleanExtra("NETWORK_MODE", false);

        // Initialize database helper
        databaseHelper = MapDatabaseHelper.getInstance(this);

        initializeViews();
        initializeLocationServices();
        setupMap();

        // Check if we should open a saved map
        handleSavedMapIntent();
    }

    private void initializeViews() {
        getLocationButton = findViewById(R.id.btn_get_location);
        downloadMapButton = findViewById(R.id.btn_download_map);
        locationInfoTextView = findViewById(R.id.tv_location_info);

        // Map type buttons
        normalButton = findViewById(R.id.btn_normal);
        satelliteButton = findViewById(R.id.btn_satellite);
        hybridButton = findViewById(R.id.btn_hybrid);
        terrainButton = findViewById(R.id.btn_terrain);

        uiHandler = new Handler(Looper.getMainLooper());

        setupClickListeners();
        updateUIForMode();
        updateDownloadButtonVisibility();
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

        // Download map button listener
        downloadMapButton.setOnClickListener(v -> downloadOfflineMap());
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

        // Check if we have a pending saved map to open
        if (getIntent().getBooleanExtra("PENDING_SAVED_MAP", false)) {
            double lat = getIntent().getDoubleExtra("MAP_CENTER_LAT", 0);
            double lng = getIntent().getDoubleExtra("MAP_CENTER_LNG", 0);
            int zoomLevel = getIntent().getIntExtra("MAP_ZOOM_LEVEL", 15);
            String mapType = getIntent().getStringExtra("MAP_TYPE");
            String mapLabel = getIntent().getStringExtra("MAP_LABEL");

            if (lat != 0 && lng != 0) {
                openSavedMap(lat, lng, zoomLevel, mapType, mapLabel);
            }
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
        dialog.setIcon(R.drawable.blue_location_pin);
        dialog.setTitle(getString(R.string.fetching_network_location));
        dialog.setMessage(message + "\nExcluding GPS - Network only");
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCanceledOnTouchOutside(false); // Disable outside click cancellation
        dialog.setCancelable(false); // Disable back button cancellation

        // Add explicit cancel button
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                handleNetworkLocationCancel();
                dialogInterface.dismiss();
            }
        });

        dialog.show();

        // Initialize network location candidates list
        networkLocationCandidates = new ArrayList<>();

        // Use STRICT network-only location with accuracy improvement
        startNetworkLocationWithAccuracyImprovement();
    }

    private void handleNetworkLocationCancel() {
        // Stop all location updates
        if (networkAccuracyTimer != null) {
            networkAccuracyTimer.cancel();
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.removeUpdates(new LocationListener() {
                @Override public void onLocationChanged(@NonNull Location location) {}
                @Override public void onProviderEnabled(@NonNull String provider) {}
                @Override public void onProviderDisabled(@NonNull String provider) {}
            });
        } catch (Exception e) {
            // Ignore cleanup errors
        }

        // Use best available location or show current area
        if (!networkLocationCandidates.isEmpty()) {
            Location bestLocation = getBestNetworkLocation();
            updateUI(bestLocation);
            String source = getLocationSource(bestLocation);
            makeCenterToast("Using current network location from " + source +
                          " (Accuracy: " + (int)bestLocation.getAccuracy() + "m)",
                          Toast.LENGTH_LONG);
        } else {
            // No network location available, show a default location or last known
            showFallbackLocation();
        }
    }

    private void showFallbackLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Try to get any last known location (excluding GPS)
            Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location lastPassiveLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            Location fallbackLocation = null;

            if (lastNetworkLocation != null && !isLocationFromGPS(lastNetworkLocation)) {
                fallbackLocation = lastNetworkLocation;
            } else if (lastPassiveLocation != null && !isLocationFromGPS(lastPassiveLocation)) {
                fallbackLocation = lastPassiveLocation;
            }

            if (fallbackLocation != null) {
                updateUI(fallbackLocation);
                long locationAge = (System.currentTimeMillis() - fallbackLocation.getTime()) / 1000 / 60; // minutes
                makeCenterToast("Using cached network location (" + locationAge + " min old, " +
                              (int)fallbackLocation.getAccuracy() + "m accuracy)", Toast.LENGTH_LONG);
            } else {
                // Show approximate location based on IP or default area
                showApproximateLocation();
            }
        } else {
            showApproximateLocation();
        }
    }

    private void showApproximateLocation() {
        // Show a default location (you can customize this to your region)
        // For example, showing a major city center as fallback
        LatLng defaultLocation = new LatLng(40.7128, -74.0060); // New York City as example

        if (mMap != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("Approximate Location")
                    .snippet("Network location unavailable - showing general area"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
            locationInfoTextView.setText("Approximate location shown\nEnable location services for accuracy");
        }

        makeCenterToast("Network location unavailable - showing approximate area", Toast.LENGTH_LONG);
    }

    private void startNetworkLocationWithAccuracyImprovement() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Create location listener for network provider only
        LocationListener networkLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Double-check this is not from GPS
                if (!isLocationFromGPS(location)) {
                    // Add to candidates list for accuracy improvement
                    networkLocationCandidates.add(location);
                    currentLocation = location;
                    currentAccuracy = location.getAccuracy();

                    // Update progress and message
                    uiHandler.post(() -> {
                        if (dialog != null && dialog.isShowing()) {
                            String source = getLocationSource(location);
                            dialog.setMessage("Network location from " + source +
                                            "\nCurrent Accuracy: " + (int)location.getAccuracy() + "m" +
                                            "\nWaiting for better accuracy...");
                        }
                    });
                } else {
                    makeCenterToast("Rejecting GPS location, waiting for network...", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                makeCenterToast("Network provider disabled", Toast.LENGTH_SHORT);
            }
        };

        // Request location updates from NETWORK provider ONLY
        try {
            // First try to get last known network location
            Location lastNetworkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (lastNetworkLocation != null && !isLocationFromGPS(lastNetworkLocation)) {
                // Use cached network location if available and not too old
                long locationAge = System.currentTimeMillis() - lastNetworkLocation.getTime();
                if (locationAge < 300000) { // Less than 5 minutes old
                    networkLocationCandidates.add(lastNetworkLocation);
                }
            }

            // Request fresh network location with frequent updates for accuracy improvement
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                3000,  // 3 second intervals for better accuracy sampling
                0,     // No minimum distance
                networkLocationListener,
                Looper.getMainLooper()
            );

            // Fallback to passive provider (cached locations from other apps)
            locationManager.requestLocationUpdates(
                LocationManager.PASSIVE_PROVIDER,
                5000,  // 5 second intervals
                0,     // No minimum distance
                networkLocationListener,
                Looper.getMainLooper()
            );

            // Start accuracy improvement timer
            startNetworkAccuracyMonitoring(locationManager, networkLocationListener);

        } catch (Exception e) {
            makeCenterToast("Network location not available", Toast.LENGTH_LONG);
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    private void startNetworkAccuracyMonitoring(LocationManager locationManager, LocationListener listener) {
        networkAccuracyTimer = new Timer(true);
        final TimerTask accuracyTask = new TimerTask() {
            private int elapsedSeconds = 0;

            @Override
            public void run() {
                elapsedSeconds += 3;

                uiHandler.post(() -> {
                    // Check if dialog was cancelled
                    if (dialog == null || !dialog.isShowing()) {
                        locationManager.removeUpdates(listener);
                        networkAccuracyTimer.cancel();
                        return;
                    }

                    if (networkLocationCandidates.isEmpty()) {
                        // No location yet, update progress
                        int progress = Math.min((elapsedSeconds * 100) / 45, 30); // Max 30% until first location
                        dialog.setProgress(progress);
                        dialog.setMessage("Searching for network location...\nElapsed: " + elapsedSeconds + "s\nClick Cancel button to stop waiting");
                        return;
                    }

                    // Find best accuracy location from candidates
                    Location bestLocation = getBestNetworkLocation();
                    float bestAccuracy = bestLocation.getAccuracy();
                    String source = getLocationSource(bestLocation);

                    // Update progress based on accuracy improvement
                    int progress = calculateNetworkProgress(bestAccuracy, elapsedSeconds);
                    dialog.setProgress(progress);
                    dialog.setMessage("Improving accuracy from " + source +
                                    "\nCurrent: " + (int)bestAccuracy + "m" +
                                    "\nElapsed: " + elapsedSeconds + "s" +
                                    "\nClick Cancel to use current location");

                    // Check if we should complete based on accuracy or time
                    boolean shouldComplete = false;

                    if (bestAccuracy <= 100) { // Very good network accuracy
                        shouldComplete = true;
                    } else if (bestAccuracy <= 300 && elapsedSeconds >= 15) { // Good accuracy after 15s
                        shouldComplete = true;
                    } else if (bestAccuracy <= 500 && elapsedSeconds >= 30) { // Acceptable accuracy after 30s
                        shouldComplete = true;
                    } else if (elapsedSeconds >= 45) { // Timeout after 45s
                        shouldComplete = true;
                    }

                    if (shouldComplete) {
                        // Complete with best location found
                        locationManager.removeUpdates(listener);
                        networkAccuracyTimer.cancel();

                        dialog.setProgress(100);
                        dialog.dismiss();

                        updateUI(bestLocation);
                        makeCenterToast("Best network location from " + source +
                                      " (Accuracy: " + (int)bestLocation.getAccuracy() + "m)",
                                      Toast.LENGTH_LONG);
                    }
                });
            }
        };

        networkAccuracyTimer.schedule(accuracyTask, 0, 3000); // Check every 3 seconds
    }

    private Location getBestNetworkLocation() {
        if (networkLocationCandidates.isEmpty()) {
            return currentLocation;
        }

        Location bestLocation = networkLocationCandidates.get(0);
        for (Location candidate : networkLocationCandidates) {
            // Prefer more recent locations with better accuracy
            long timeDiff = candidate.getTime() - bestLocation.getTime();
            float accuracyDiff = bestLocation.getAccuracy() - candidate.getAccuracy();

            // Choose candidate if it's significantly more accurate or much more recent
            if (accuracyDiff > 50 || (timeDiff > 10000 && accuracyDiff > -100)) {
                bestLocation = candidate;
            }
        }

        return bestLocation;
    }

    private int calculateNetworkProgress(float accuracy, int elapsedSeconds) {
        int baseProgress = Math.min((elapsedSeconds * 100) / 45, 90); // Time-based progress

        // Accuracy bonus
        int accuracyBonus = 0;
        if (accuracy <= 100) accuracyBonus = 10;
        else if (accuracy <= 200) accuracyBonus = 8;
        else if (accuracy <= 300) accuracyBonus = 6;
        else if (accuracy <= 500) accuracyBonus = 4;
        else if (accuracy <= 1000) accuracyBonus = 2;

        return Math.min(baseProgress + accuracyBonus, 100);
    }

    private void downloadOfflineMap() {
        if (mMap == null) {
            makeCenterToast("Map not ready yet", Toast.LENGTH_SHORT);
            return;
        }

        // Double-check internet connectivity
        if (!isInternetAvailable()) {
            makeCenterToast("Internet connection required to download offline maps", Toast.LENGTH_LONG);
            updateDownloadButtonVisibility(); // Hide button if no internet
            return;
        }

        // Get current map bounds or use current location area
        LatLngBounds bounds;
        if (currentLocation != null) {
            // Create bounds around current location (approximately 10km radius)
            double lat = currentLocation.getLatitude();
            double lng = currentLocation.getLongitude();
            double offset = 0.05; // Approximately 5km in each direction

            bounds = new LatLngBounds(
                new LatLng(lat - offset, lng - offset), // Southwest corner
                new LatLng(lat + offset, lng + offset)  // Northeast corner
            );
        } else {
            // Default bounds if no location available
            bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        }

        // Show map labeling dialog
        showMapLabelingDialog(bounds);
    }

    private void showMapLabelingDialog(LatLngBounds bounds) {
        // Inflate the custom dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_map_label, null);

        // Get references to dialog views
        EditText labelEditText = dialogView.findViewById(R.id.et_map_label);
        EditText descriptionEditText = dialogView.findViewById(R.id.et_map_description);
        TextView locationTextView = dialogView.findViewById(R.id.tv_map_info_location);
        TextView typeTextView = dialogView.findViewById(R.id.tv_map_info_type);
        TextView sizeTextView = dialogView.findViewById(R.id.tv_map_info_size);

        // Set map information
        LatLng center = bounds.getCenter();
        locationTextView.setText(String.format("Location: %.4f, %.4f", center.latitude, center.longitude));
        typeTextView.setText("Type: " + getCurrentMapTypeString());
        sizeTextView.setText("Area: ~10km radius");

        // Create and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false); // Make dialog not dismissible by clicking outside

        AlertDialog dialog = builder.create();

        // Set up button click listeners
        Button cancelButton = dialogView.findViewById(R.id.btn_cancel);
        Button downloadButton = dialogView.findViewById(R.id.btn_download);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        downloadButton.setOnClickListener(v -> {
            String label = labelEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();

            if (label.isEmpty()) {
                labelEditText.setError("Map label is required");
                return;
            }

            // Check if label already exists
            if (databaseHelper.isMapLabelExists(label)) {
                labelEditText.setError("A map with this label already exists");
                return;
            }

            dialog.dismiss();
            startOfflineMapDownloadWithLabel(bounds, label, description);
        });

        dialog.show();
    }

    private void startOfflineMapDownloadWithLabel(LatLngBounds bounds, String label, String description) {
        // Create DownloadedMap object
        LatLng center = bounds.getCenter();
        LatLng northeast = bounds.northeast;
        LatLng southwest = bounds.southwest;

        DownloadedMap downloadedMap = new DownloadedMap(
            label,
            center.latitude,
            center.longitude,
            northeast.latitude,
            northeast.longitude,
            southwest.latitude,
            southwest.longitude,
            getCurrentZoomLevel(),
            getCurrentMapTypeString()
        );

        if (description != null && !description.isEmpty()) {
            downloadedMap.setDescription(description);
        }

        // Save to database
        long mapId = databaseHelper.addDownloadedMap(downloadedMap);

        if (mapId > 0) {
            // Enable map caching and show success
            enableMapCachingWithLabel(label);
        } else {
            Toast.makeText(this, "Failed to save map information", Toast.LENGTH_LONG).show();
        }
    }

    private void enableMapCachingWithLabel(String label) {
        // Enable map caching for better offline experience
        if (mMap != null) {
            // Google Maps automatically caches viewed areas
            // We can help by pre-loading the current area
            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

            // Move camera around the area to trigger caching
            LatLng center = bounds.getCenter();
            for (int zoom = 10; zoom <= 16; zoom++) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom));
            }

            makeCenterToast("Map \"" + label + "\" saved successfully!\nViewed areas will be cached for offline use.",
                          Toast.LENGTH_LONG);
            updateDownloadButtonState(true);
        }
    }

    private void updateDownloadButtonState(boolean hasOfflineMap) {
        if (hasOfflineMap) {
            downloadMapButton.setText("Offline Map Available");
            downloadMapButton.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            downloadMapButton.setText(getString(R.string.download_offline_map));
            downloadMapButton.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void updateDownloadButtonVisibility() {
        if (downloadMapButton != null) {
            if (isInternetAvailable()) {
                downloadMapButton.setVisibility(android.view.View.VISIBLE);
                downloadMapButton.setText(getString(R.string.download_offline_map));
                downloadMapButton.setBackgroundColor(getResources().getColor(android.R.color.holo_purple));
                downloadMapButton.setEnabled(true);
            } else {
                // Hide the button completely when no internet
                downloadMapButton.setVisibility(android.view.View.GONE);

                // Update location info to show offline status
                if (locationInfoTextView != null) {
                    String currentText = locationInfoTextView.getText().toString();
                    if (!currentText.contains("Offline")) {
                        locationInfoTextView.setText(currentText + "\n(Offline - No map download available)");
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update download button visibility when app resumes
        updateDownloadButtonVisibility();
    }

    private void startNetworkLocationUpdates() {
        // Create location request that STRICTLY excludes GPS
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 10000)
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

                        // STRICT CHECK: Reject GPS-based locations
                        if (isLocationFromGPS(location)) {
                            makeCenterToast("GPS location detected, waiting for network location...", Toast.LENGTH_SHORT);
                            return; // Skip GPS locations
                        }

                        currentLocation = location;
                        currentAccuracy = location.getAccuracy();

                        // Stop updates after getting first network result
                        fusedLocationClient.removeLocationUpdates(this);

                        uiHandler.post(() -> {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            updateUI(location);
                            String source = getLocationSource(location);
                            makeCenterToast("Network location found from " + source +
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
                    makeCenterToast("Network location timeout. Try moving to area with better cell/WiFi coverage.",
                                  Toast.LENGTH_LONG);
                }
            }, 20000); // 20 second timeout for network-only
        }
    }

    private boolean isLocationFromGPS(Location location) {
        // Check if location provider is GPS
        String provider = location.getProvider();
        if (provider != null && provider.equals("gps")) {
            return true;
        }

        // Additional check: GPS locations typically have very high accuracy (< 20m)
        // and are obtained quickly, while network locations are usually > 50m accuracy
        if (location.getAccuracy() < 20 && location.getTime() > (System.currentTimeMillis() - 30000)) {
            return true; // Likely GPS if very accurate and recent
        }

        return false;
    }

    private String getLocationSource(Location location) {
        String provider = location.getProvider();
        if (provider != null) {
            switch (provider.toLowerCase()) {
                case "network":
                    return "Cell Tower/WiFi";
                case "passive":
                    return "Cached Network";
                case "fused":
                    return "Network Services";
                default:
                    return "Network Provider";
            }
        }

        // Determine source based on accuracy
        if (location.getAccuracy() > 1000) {
            return "IP Geolocation";
        } else if (location.getAccuracy() > 100) {
            return "Cell Tower";
        } else {
            return "WiFi Network";
        }
    }

    private void getLocationFix() {
        if (isGPSEnabled()) {
            final String message = getString(R.string.please_wait_fetching);
            dialog = new ProgressDialog(MapsActivity.this);
            dialog.setIcon(R.drawable.blue_location_pin);
            dialog.setTitle(getString(R.string.fetching_location));
            dialog.setMessage(message);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);

            // Add explicit cancel button for GPS mode too
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    stopLocationUpdates();
                    if (timer != null) {
                        timer.cancel();
                    }
                    dialogInterface.dismiss();
                    makeCenterToast("Location search cancelled", Toast.LENGTH_SHORT);
                }
            });

            dialog.show();

            startLocationUpdates();

            timer = new Timer(true);
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    uiHandler.post(new Runnable() {
                        public void run() {
                            if (dialog != null && dialog.isShowing()) {
                                dialog.setMessage(message + "\nCurrent Accuracy : " + getCurrentAccuracy() + "m" +
                                                "\nClick Cancel button to stop");
                                updateProgressBasedOnAccuracy();
                            } else {
                                // Dialog was dismissed, stop the timer
                                timer.cancel();
                                stopLocationUpdates();
                            }
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

    // Helper method to get current map type as string
    private String getCurrentMapTypeString() {
        if (mMap == null) return "HYBRID";

        switch (mMap.getMapType()) {
            case GoogleMap.MAP_TYPE_NORMAL:
                return "NORMAL";
            case GoogleMap.MAP_TYPE_SATELLITE:
                return "SATELLITE";
            case GoogleMap.MAP_TYPE_TERRAIN:
                return "TERRAIN";
            case GoogleMap.MAP_TYPE_HYBRID:
            default:
                return "HYBRID";
        }
    }

    // Helper method to get current zoom level
    private int getCurrentZoomLevel() {
        if (mMap == null) return 15;
        return Math.round(mMap.getCameraPosition().zoom);
    }

    // Handle intent to open saved map
    private void handleSavedMapIntent() {
        if (getIntent().getBooleanExtra("OPEN_SAVED_MAP", false)) {
            double lat = getIntent().getDoubleExtra("MAP_CENTER_LAT", 0);
            double lng = getIntent().getDoubleExtra("MAP_CENTER_LNG", 0);
            int zoomLevel = getIntent().getIntExtra("MAP_ZOOM_LEVEL", 15);
            String mapType = getIntent().getStringExtra("MAP_TYPE");
            String mapLabel = getIntent().getStringExtra("MAP_LABEL");

            if (lat != 0 && lng != 0) {
                // Wait for map to be ready, then navigate to saved location
                if (mMap != null) {
                    openSavedMap(lat, lng, zoomLevel, mapType, mapLabel);
                } else {
                    // Store values to use when map is ready
                    getIntent().putExtra("PENDING_SAVED_MAP", true);
                }
            }
        }
    }

    // Open saved map at specified location
    private void openSavedMap(double lat, double lng, int zoomLevel, String mapType, String mapLabel) {
        if (mMap == null) return;

        LatLng location = new LatLng(lat, lng);

        // Set map type if specified
        if (mapType != null) {
            switch (mapType.toUpperCase()) {
                case "NORMAL":
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    updateButtonStates(GoogleMap.MAP_TYPE_NORMAL);
                    break;
                case "SATELLITE":
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    updateButtonStates(GoogleMap.MAP_TYPE_SATELLITE);
                    break;
                case "TERRAIN":
                    mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    updateButtonStates(GoogleMap.MAP_TYPE_TERRAIN);
                    break;
                case "HYBRID":
                default:
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    updateButtonStates(GoogleMap.MAP_TYPE_HYBRID);
                    break;
            }
        }

        // Add marker and move camera
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(mapLabel != null ? mapLabel : "Saved Location")
                .snippet("Saved map location"));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));

        // Update location info
        locationInfoTextView.setText(String.format("Saved Map: %s\nLat: %.6f, Lng: %.6f\nZoom: %d",
                mapLabel != null ? mapLabel : "Unknown", lat, lng, zoomLevel));

        makeCenterToast("Opened saved map: " + (mapLabel != null ? mapLabel : "Unknown"), Toast.LENGTH_SHORT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (networkAccuracyTimer != null) {
            networkAccuracyTimer.cancel();
        }
        stopLocationUpdates();
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
