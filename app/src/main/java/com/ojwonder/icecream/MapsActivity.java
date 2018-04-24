package com.ojwonder.icecream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
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

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LatLng[] storeArray = new LatLng[1196];
    private String[][] readFileArray = new String[1196][8];
    private List<Marker> markers = new ArrayList<>();
    private Location myLocation;
    private boolean mLocationPermissionGranted = false;
    private static int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onStart() {
        requestLocationPermission();
        storeArray = new LatLng[1196];
        super.onStart();
    }

    private void setListners() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        myLocation = location;
                    } else {
                        Toast.makeText(MapsActivity.this, "Location FAILED", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                int temp = findRestaurantIndex(marker.getTitle());
                Toast.makeText(MapsActivity.this, Integer.toString(temp), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    private int findRestaurantIndex(String name){
        for ( int i = 0; i <= readFileArray.length; i++){
            if ( name == readFileArray[i][0] ){
                return i;
            }
            else
                return -1;
        }
        return -1;
    }

    public void readMcdonaldsCSV(){
        InputStream inputStream = getResources().openRawResource(R.raw.mcdonalds);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        LatLng tempLatLng;
        String csvLine = null;
        for( int i = 0; i <= 1100; i++){
            try {
                csvLine = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String [] row = csvLine.split(",");
            String name = row[0];
            double a = Double.parseDouble(row[1]);
            double b = Double.parseDouble(row[2]);
            tempLatLng = new LatLng(a, b);
            storeArray[i] = tempLatLng;
            if ( row[7] == )
                mMap.addMarker(new MarkerOptions().position(tempLatLng).title(name).snippet(row[7]).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            else
                mMap.addMarker(new MarkerOptions().position(tempLatLng).title(name).snippet(row[7]).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

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

    private void drawCircle(LatLng currentLocation){
        //The radius of the circle, specified in meters. It should be zero or greater.
        Circle circle = mMap.addCircle(new CircleOptions().center(currentLocation).radius(16000).strokeColor(Color.rgb(0, 136, 255)).fillColor(Color.argb(20, 0, 136, 255)));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestLocationPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
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
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                requestLocationPermission();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                mLocationPermissionGranted = true;
            }
        } else {
            //Toast.makeText(this, "permission already granted", Toast.LENGTH_SHORT).show();
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            mLocationPermissionGranted = true;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setListners();
        readMcdonaldsCSV();
        //LatLng mLatLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        //drawCircle(mLatLng);

    }
}
