package com.ojwonder.icecream;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //location var
    private GoogleMap mMap;
    private FusedLocationProviderClient locationProvider;
    private Location myLocation;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private LatLng myLatLong = new LatLng(-5.55555, 5.55555);
    private boolean locationPermissionGranted = false;


    //csv var
    private LatLng[] storeArray = new LatLng[1196];
    private String[][] readFileArray = new String[1196][8];
    private float[] distanceResults = new float[1196];
    private Marker[] markerArray = new Marker[100];


    private Date todaysDate;
    private int finished = 0;

    //var
    private static final String TAG = "MyActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final float DEFAULT_ZOOM = (float) 9.0293;


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        StyleableToast.makeText(this, "current location: "+ myLatLong.toString(), Toast.LENGTH_LONG, R.style.GreenToast).show();
        Log.d(TAG, "onMapReady");
        mMap = googleMap;

        if(locationPermissionGranted == true){
            @SuppressLint("MissingPermission") Task<Location> locationResult = locationProvider.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        myLocation = task.getResult();
                        mMap.setMyLocationEnabled(true); //set my location on the map
                        myLatLong = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(myLatLong, DEFAULT_ZOOM);
                        mMap.moveCamera(update);
                        drawCircle(myLatLong);
                        StyleableToast.makeText(MapsActivity.this, "current location: "+ myLatLong.toString(), Toast.LENGTH_LONG, R.style.GreenToast).show();
                        finished = 1;


                    } else {
                        StyleableToast.makeText(MapsActivity.this, "Please turn on your location", Toast.LENGTH_LONG, R.style.RedToast).show();
                        finished = 1;
                        //myLatLong
                    }
                }
            });

        }
        //TODO: Replace reading from CSV with API call to server
        readMcdonaldsCSV(); //load data from file


        //below code is run after 1 second to allow for the Location task to finish and not freeze UI thread
        Handler handler = new Handler();
        Runnable r = new Runnable() {
            public void run() {
                if (!(myLatLong == null)){
                    addMarkersInMyRadius(myLatLong);
                } else {
                    StyleableToast.makeText(MapsActivity.this, "Latitude and Longitude are NULL. There was a problem retrieving your current location.", Toast.LENGTH_LONG, R.style.RedToast).show();
                }

            }
        };
        handler.postDelayed(r, 1000);




    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        locationProvider = LocationServices.getFusedLocationProviderClient(this);

        getLocationPermission(); //makes sure the user has given permission to location
        //isServicesOK();


    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onStart() {
        getLocationPermission();

        super.onStart();
    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (available == ConnectionResult.SUCCESS){
            //everything is okay user can perfom map requests
            Log.d(TAG, "Google play service working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but can be resolved
            Log.d(TAG, "An error occured but can be fixed");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available,ERROR_DIALOG_REQUEST);
        }else{
            StyleableToast.makeText(this, "You cannot make map requests :(", Toast.LENGTH_LONG, R.style.RedToast);
        }

        return false;
}

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true; //if permission already granted
                initMap(); //initialise the map
            }else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
                initMap();
            }

        }else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            initMap();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            locationPermissionGranted = false;
                            return;
                        }
                    }
                    locationPermissionGranted = true;
                }
                locationPermissionGranted = true;
            }
        }
    }

    private void addMarkersInMyRadius(LatLng mLatLng) {
        for (int i = 0; i <= storeArray.length - 1; i++) {
            if (!(storeArray[i] == null)) {
                Location.distanceBetween(mLatLng.latitude, mLatLng.longitude, storeArray[i].latitude, storeArray[i].longitude, distanceResults);
                if (distanceResults[0] <= 16093.4) {
                    switch (readFileArray[i][7]) {
                        case "off":
                            Marker marker = mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet("Status: OFF").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                            marker.setTag(storeArray[i]);

                            Log.d(TAG, "addMarker: added OFF marker");
                            break;
                        case "on":
                            Marker marker1 = mMap.addMarker(new MarkerOptions().position(storeArray[i]).title(readFileArray[i][0]).snippet("Status: ON").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            marker1.setTag(storeArray[i]);
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
        mMap.addCircle(new CircleOptions().center(currentLocation).radius(16000).strokeColor(Color.rgb(0, 136, 255)));
    }

    private int requestStores() throws  IOException, MalformedURLException{
        String sql = "";
        URL url = new URL("https://icecream.olliejones.tech");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //start HTTPS connection

        sql = "SELECT * FROM TABLE mcdonalds_uk";

        return 1;
    }

    private int updateStoreStatus(String name, String status) throws  IOException, MalformedURLException{
        String sql = "";
        URL url = new URL("https://icecream.olliejones.tech");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(); //start HTTPS connection

        switch (status) {
            case "off":
                sql = "UPDATE mcdonalds_uk SET status = on WHERE name = " + name +";";

            case "on":
                sql = "UPDATE mcdonalds_uk SET status = off WHERE name = " + name +";";
        }


        return 1;
    }







}


