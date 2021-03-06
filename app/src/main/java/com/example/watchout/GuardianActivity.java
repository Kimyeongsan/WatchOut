package com.example.watchout;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.watchout.Data.DB_Data;
import com.example.watchout.Data.GuardianManagement;
import com.example.watchout.Login.GuardianLoginActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static com.example.watchout.Data.DB_Data.DB_CHILD_CURRENTLOCATION;
import static com.example.watchout.Data.DB_Data.DB_CHILD_CURRENTLOCATION;
import static com.example.watchout.Data.DB_Data.DB_CHILD_DEST;
import static com.example.watchout.Data.DB_Data.DB_CHILD_EME;
import static com.example.watchout.Data.DB_Data.DB_CHILD_GOAL;

public class GuardianActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback{

    private GoogleMap mMap;
    private Geocoder geocoder; // ????????????
    private Marker currentMarker = null;
    Button ward_btn;       // ?????? ?????? ??????
    Button temp; //??????test

    private FirebaseAuth firebaseAuth;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 30000;  // 1??? 1000
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5???

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;

    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    Location mCurrentLocatiion;
    LatLng currentPosition;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;
    private View mLayout;
    TextView currentlocation;

    //????????? ????????????//
    TextView btn_showDestination;
    private String destinationMarkerPoint; //????????? ????????? ????????? ??????

    //DBtestString
    //?????????
    public String testdatabaseString;//???????????????
    public String testdatabaseString2;//???????????????
    public String testdatabaseString3;//?????????????????????
    //?????????
    public String locationString;
    public String locationString2;
    public String locationString3;

    //???????????? ????????????
    public String Goaltext;//???????????? ?????????
    public String Emetext;//???????????? ?????????
    NotificationManager manager;//????????? ????????? ?????????
    NotificationCompat.Builder Nbuilder;//????????? ????????? ?????????2
    private String CHANNEL_ID="channel1";
    private String CHANNEL_NAME="Channel1";
    private String CHANNEL_ID2="channel2";
    private String CHANNEL_NAME2="Channel2";

    TextView btnCallUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_guardian);
        currentlocation = findViewById(R.id.btn_currentLocation);
        mLayout = findViewById(R.id.layout_main);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ward_btn = findViewById(R.id.ward_btn);
        btn_showDestination=findViewById(R.id.btn_showDestination);
        btnCallUser=(TextView)findViewById(R.id.btnCalltoUser);

        //???????????? ????????? ?????? ??????
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(DB_CHILD_DEST);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                testdatabaseString=value.toString();

                //???????????????
                testdatabaseString2= testdatabaseString.substring(18);
                testdatabaseString3=testdatabaseString2.substring(0,testdatabaseString2.length()-1);


                Log.d("desttest","CutdatafromDB : "+testdatabaseString3);
                //???????????????????????? ?????? ???????????????

                destinationMarkerPoint=testdatabaseString3;//????????? ?????? ??????
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
//???????????? ??????1


//???????????? ?????? 2 ????????? ???????????? ??????
        DatabaseReference myRef2 = database.getReference(DB_CHILD_CURRENTLOCATION);
        myRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                locationString=value.toString();
                //???????????????
                locationString2= locationString.substring(18);
                locationString3=locationString2.substring(0,locationString2.length()-1);

                Log.d("locationDB","datafromDB : "+locationString);
                Log.d("locationDB","CutdatafromDB : "+locationString3);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //????????? ?????? 2 ???

        //????????? ?????? 3 ?????? ?????? ??????
        DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
        myGoal.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                Goaltext=value.toString();
                Log.d("goal","database value  : "+Goaltext);
                //??????3?????? ???????????? ?????? //??????3 ????????? ????????????
                if(Goaltext.equals("true")){
                    Toast.makeText(GuardianActivity.this, "??????", Toast.LENGTH_LONG).show();
                    showNotificationGoal();
                }
                //????????? ????????? ??????
                //

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //???????????? 3 ???

        //????????????4
        DatabaseReference myEme = database.getReference(DB_CHILD_EME);
        myEme.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                Emetext=value.toString();
                Log.d("Eme","database value  : "+Emetext);
                //??????3?????? ???????????? ?????? //??????3 ????????? ????????????
                if(Emetext.equals("true")){

                    Toast.makeText(GuardianActivity.this, "???????????? ??????", Toast.LENGTH_LONG).show();
                    showNotificationEmergency();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //


        //?????????????????? ????????????
        btnCallUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //?????? ????????? @@@@@@
                //?????? ?????? ????????? ???????????? ?????????
                Intent tt = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+"01077675517"));
                startActivity(tt);
            }
        });


        // ???????????? ?????? ??????
        ward_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EnrollmentActivity.class);
                startActivity(intent);
            }
        });


        initialize();
    }

    //?????? ????????? ??????
    public void showNotificationGoal(){
        Nbuilder = null;
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //????????? ??????
        //?????? ????????? ????????? ??????
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            );

            Nbuilder = new NotificationCompat.Builder(this,CHANNEL_ID);

            //?????? ????????? ??????
        }else{
            Nbuilder = new NotificationCompat.Builder(this); }

        //??????
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);

        //?????????
        MediaPlayer player = MediaPlayer.create(GuardianActivity.this,R.raw.goal1);
        player.start();

        //?????????
        Nbuilder.setContentTitle("????????? ?????? ??????");
        Nbuilder.setContentText("???????????? ???????????? ?????????????????????.");
        Nbuilder.setSmallIcon(R.drawable.smile);

        Notification notification = Nbuilder.build();
        manager.notify(1,notification);

    }
    //

    //???????????? ????????? ??????
    public void showNotificationEmergency(){
        Nbuilder = null;
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //????????? ??????
        //?????? ????????? ????????? ??????
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID2, CHANNEL_NAME2, NotificationManager.IMPORTANCE_DEFAULT)
            );

            Nbuilder = new NotificationCompat.Builder(this,CHANNEL_ID2);

            //?????? ????????? ??????
        }else{
            Nbuilder = new NotificationCompat.Builder(this); }

        //??????
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(5000);

        //?????????
        MediaPlayer player = MediaPlayer.create(GuardianActivity.this,R.raw.warning1);
        player.start();

        //?????????
        Nbuilder.setContentTitle("?????? ?????? ?????? ?????? ?????? ??????");
        Nbuilder.setContentText(locationString);
        Nbuilder.setSmallIcon(R.drawable.warning);

        Notification notification = Nbuilder.build();
        manager.notify(1,notification);

    }
    //





    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");

        mMap = googleMap;
        geocoder = new Geocoder(this);

        setDefaultLocation();

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {

            startLocationUpdates(); // 3. ?????? ???????????? ??????

        }else {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                Snackbar.make(mLayout, "??? ?????? ??????????????? ?????? ?????? ????????? ???????????????.",
                        Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions( GuardianActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                Log.d( TAG, "onMapClick :");
            }
        });
        //???????????? ????????? ????????????
        btn_showDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //????????? ???????????? ???????????? ????????? ->????????????
                Log.d("desttest","using DestData from MainActivity : "+destinationMarkerPoint);
                //
                String str = destinationMarkerPoint;

                List<Address>addressList=null;
                try {
                    //???????????? ??? ?????? ????????? ??? ?????? (?????? ?????? ?????????) ??????
                    addressList = geocoder.getFromLocationName(str,5);
                }catch (IOException e){
                    e.printStackTrace();
                }
                System.out.println(addressList.get(0).toString());
                // ????????? ???????????? split
                String []splitStr = addressList.get(0).toString().split(",");
                String address = splitStr[0].substring(splitStr[0].indexOf("\"") + 1,splitStr[0].length() - 2); // ??????
                System.out.println(address);

                String latitude = splitStr[10].substring(splitStr[10].indexOf("=") + 1); // ??????
                String longitude = splitStr[12].substring(splitStr[12].indexOf("=") + 1); // ??????
                System.out.println(latitude);
                System.out.println(longitude);

                // ??????(??????, ??????) ??????
                LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                // ?????? ??????
                MarkerOptions mOptions2 = new MarkerOptions();
                mOptions2.title("search result");
                mOptions2.snippet(address);
                mOptions2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                mOptions2.position(point);

                // ?????? ??????
                mMap.addMarker(mOptions2);
                // ?????? ????????? ?????? ???
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point,15));

            }
        });

        //???????????? ?????? ??????
        currentlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("marker","?????? ????????? ????????? : "+ " ");
                MarkerOptions currentmOption = new MarkerOptions();
                currentmOption.title("currentLocation");
                Log.d("marker","?????? ?????? ????????? ????????? ????????? :"+locationString3);

                String lan = locationString3.substring(0,10);
                String lut = locationString3.substring(14);
                Log.d("marker","?????? :"+lan);
                Log.d("marker","?????? :"+lut);
                //?????? ???????????? ?????????
               currentmOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                // ??????(??????, ??????) ??????
                LatLng point = new LatLng(Double.parseDouble(lan), Double.parseDouble(lut));

                currentmOption.position(point);
                mMap.addMarker(currentmOption);
                //????????? ??????
                //????????????

            }
        });

    }



    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();




            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());
//???????????????
                String markerTitle = getCurrentAddress(currentPosition);
                /*
                String markerSnippet = "??????:" + String.valueOf(location.getLatitude())
                        + " ??????:" + String.valueOf(location.getLongitude());

                 */
                String markerSnippet=locationString3;

                Log.d(TAG, "onLocationResult : " + markerSnippet);



                setCurrentLocation(location, markerTitle, markerSnippet);

                mCurrentLocatiion = location;
            }
        }
    };

    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        }else {

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED   ) {

                Log.d(TAG, "startLocationUpdates : ????????? ???????????? ??????");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if (checkPermission())
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");

        if (checkPermission()) {

            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            if (mMap!=null)
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStop() {

        super.onStop();

        if (mFusedLocationClient != null) {

            Log.d(TAG, "onStop : call stopLocationUpdates");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public String getCurrentAddress(LatLng latlng) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {

        if (currentMarker != null) currentMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);
    }

    public void setDefaultLocation() {
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "???????????? ????????? ??? ??????";
        String markerSnippet = "?????? ???????????? GPS ?????? ?????? ???????????????";

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);
    }

    private boolean checkPermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            boolean check_result = true;

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                startLocationUpdates();
            } else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();

                } else {

                    Snackbar.make(mLayout, "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("??????", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }

        }
    }

    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(GuardianActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n"
                + "?????? ????????? ???????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //???????????? GPS ?????? ???????????? ??????
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : GPS ????????? ?????????");

                        needRequest = true;

                        return;
                    }
                }
                break;
        }
    }
    private void initialize() {
        Button btnLogOut;

        btnLogOut = findViewById(R.id.guardian_logout);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
                Intent intent = new Intent(
                        getApplicationContext(), GuardianLoginActivity.class);

                // ????????? ????????? ??? ??????
                GuardianManagement.getInstance().delAllData();

                startActivity(intent);

            }
        });
    }

    private void logOut () {
        firebaseAuth.getInstance().signOut();
    }

}