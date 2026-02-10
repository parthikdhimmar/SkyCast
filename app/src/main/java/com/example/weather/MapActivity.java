package com.example.weather;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker selectedMarker;
    private GeoPoint selectedPoint;
    private String selectedAddress = "";
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_map);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue));
        }


        // Initialize osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osm_prefs", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Add rotation gesture overlay
        RotationGestureOverlay rotationGestureOverlay = new RotationGestureOverlay(this, mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        // Add compass overlay
        CompassOverlay compassOverlay = new CompassOverlay(this,
                new InternalCompassOrientationProvider(this), mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        // Add my location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Set initial position to current location
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (isFirstLoad) {
                IMapController mapController = mapView.getController();
                GeoPoint startPoint = myLocationOverlay.getMyLocation();
                if (startPoint != null) {
                    mapController.setZoom(12.0);
                    mapController.setCenter(startPoint);
                    isFirstLoad = false;
                }
            }
        }));

        // Set up long press listener
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Get the coordinates where user touched
                    GeoPoint touchedPoint = (GeoPoint) mapView.getProjection().fromPixels(
                            (int) event.getX(),
                            (int) event.getY()
                    );

                    // Get address from coordinates
                    getAddressFromLocation(touchedPoint);
                }
                return false;
            }
        });

        AppCompatButton btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnSelectLocation.setOnClickListener(v -> {
            if (selectedPoint != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("location_query", selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });

        AppCompatButton btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
        AppCompatButton btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            // Reset to current location
            if (myLocationOverlay.getMyLocation() != null) {
                IMapController mapController = mapView.getController();
                mapController.setCenter(myLocationOverlay.getMyLocation());
                mapController.setZoom(12.0);

                // Clear selection
                if (selectedMarker != null) {
                    mapView.getOverlays().remove(selectedMarker);
                    selectedMarker = null;
                    selectedPoint = null;
                    selectedAddress = "";
                }
            }
        });
    }

    private void getAddressFromLocation(GeoPoint point) {
        new Thread(() -> {
            try {
                // Force English locale for geocoder
                Geocoder geocoder = new Geocoder(MapActivity.this, Locale.ENGLISH);
                List<Address> addresses = geocoder.getFromLocation(
                        point.getLatitude(),
                        point.getLongitude(),
                        1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder addressText = new StringBuilder();

                    // Build address in English
                    if (address.getLocality() != null) {
                        addressText.append(address.getLocality());
                    } else if (address.getSubAdminArea() != null) {
                        addressText.append(address.getSubAdminArea());
                    } else if (address.getAdminArea() != null) {
                        addressText.append(address.getAdminArea());
                    } else if (address.getCountryName() != null) {
                        addressText.append(address.getCountryName());
                    }

                    final String finalAddress = addressText.length() > 0 ? addressText.toString() :
                            String.format(Locale.getDefault(), "%.4f,%.4f",
                                    point.getLatitude(),
                                    point.getLongitude());

                    runOnUiThread(() -> {
                        // Remove previous marker if exists
                        if (selectedMarker != null) {
                            mapView.getOverlays().remove(selectedMarker);
                        }

                        // Create new marker
                        selectedPoint = point;
                        selectedMarker = new Marker(mapView);
                        selectedMarker.setPosition(selectedPoint);
                        selectedMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        selectedMarker.setTitle("Selected Location");
                        selectedMarker.setSnippet(finalAddress);
                        mapView.getOverlays().add(selectedMarker);
                        mapView.invalidate();

                        // Set the selected address
                        selectedAddress = finalAddress;

                        Toast.makeText(MapActivity.this,
                                "Location selected: " + selectedAddress,
                                Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MapActivity.this,
                        "Could not get address", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}