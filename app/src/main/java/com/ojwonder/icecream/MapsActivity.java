package com.ojwonder.icecream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    //location var
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location myLocation;
    private LatLng myLatLong;
    private boolean mLocationPermissionGranted = false;
    private static int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //csv var
    private LatLng[] storeArray = new LatLng[1196];
    private String[][] readFileArray = new String[1196][8];
    float[] distanceResults = new float[1196];

    //var
    private static final String TAG = "MyActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setListners();
        setListners();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onStart() {
        requestLocationPermission();

        super.onStart();
    }

    private void setLatLong() {
        double a = myLocation.getLatitude();
        double b = myLocation.getLongitude();
        myLatLong = new LatLng(a, b);
    }

    private void setMyLocation() {
    }

    private void setListners() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        myLocation = location;
                        myLatLong = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                        Log.d(TAG, "setListeners: location != null");


                    } else {
                        Log.d(TAG, "setListeners: location = null");
                    }
                    mMap.setMyLocationEnabled(true);

                }
            });
            //return;
        }

    }


    private void inMyRadius(LatLng mLatLng) {
        for (int i = 0; i <= storeArray.length - 1; i++) {
            Location.distanceBetween(mLatLng.latitude, mLatLng.longitude, storeArray[i].latitude, storeArray[i].longitude, distanceResults);
            if (distanceResults[0] <= 16093.4) {
                if (readFileArray[i][7].length() == 3)
                    mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet(readFileArray[i][7]).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                else
                    mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet(readFileArray[i][7]).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }
        }
    }

    private int getRestaurantIndex(String name) {
        for (int i = 0; i <= readFileArray.length; i++) {
            if (name == readFileArray[i][0]) {
                return i;
            }
        }
        return -1;
    }

    public void readMcdonaldsCSV() {
        InputStream inputStream = getResources().openRawResource(R.raw.mcdonalds);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        LatLng tempLatLng;
        String csvLine = null;
        for (int i = 0; i <= 1100; i++) {
            try {
                csvLine = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String[] row = csvLine.split(",");
            String name = row[0];
            double a = Double.parseDouble(row[1]);
            double b = Double.parseDouble(row[2]);
            tempLatLng = new LatLng(a, b);
            storeArray[i] = tempLatLng;

            readFileArray[i][0] = row[0];
            readFileArray[i][1] = row[1];
            readFileArray[i][2] = row[2];
            readFileArray[i][3] = row[3];
            readFileArray[i][4] = row[4];
            readFileArray[i][5] = row[5];
            readFileArray[i][6] = row[6];
            readFileArray[i][7] = row[7];
        }
    }

    private void drawCircle(LatLng currentLocation) {
        //The radius of the circle, specified in meters. It should be zero or greater.
        Circle circle = mMap.addCircle(new CircleOptions().center(currentLocation).radius(16000).strokeColor(Color.rgb(0, 136, 255)).fillColor(Color.argb(20, 0, 136, 255)));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                mLocationPermissionGranted = true;
            }
        } else {
            //Toast.makeText(this, "permission already granted", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            mLocationPermissionGranted = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestLocationPermission();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                mLocationPermissionGranted = true;
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            mLocationPermissionGranted = true;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mMap = googleMap;

        //setMyLocation();
        //setLatLong();
        readMcdonaldsCSV();
        addMarkers();

        if (! (myLatLong == null)) {
            Toast.makeText(this, "LATLONG NULL", Toast.LENGTH_LONG).show();
        }

        if (myLocation == null) {
            Toast.makeText(this, "LOCATION NULL", Toast.LENGTH_LONG).show();
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                int temp = getRestaurantIndex(marker.getTitle());
                Toast.makeText(MapsActivity.this, Integer.toString(temp), Toast.LENGTH_LONG).show();
                return false;
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        //mMap.addCircle(new CircleOptions().center(new LatLng(myLatLong.latitude, myLatLong.longitude)).radius(32000).strokeColor(Color.GREEN));



        //inMyRadius(myLatLong);
        //setListners();
    }

    private void addMarkers() {
        Log.d(TAG, "addMarker: adding markers");
        for (int i = 0; i <= storeArray.length - 1; i++) {
            if (!(storeArray[i] == null)) {
                switch (readFileArray[i][7]) {
                    case "off":
                        mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet("Status: OFF").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        Log.d(TAG, "addMarker: added OFF marker");
                        break;
                    case "on":
                        mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet("Status: ON").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                        Log.d(TAG, "addMarker: added ON marker");
                        break;
                    default:
                        mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet("Status: Unknown").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                        Log.d(TAG, "addMarker: added ? marker");
                        break;
                }

            }
        }
    }
}
