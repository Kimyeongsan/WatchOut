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
    private Geocoder geocoder; // 지오코더
    private Marker currentMarker = null;
    Button ward_btn;       // 화면 전환 버튼
    Button temp; //임시test

    private FirebaseAuth firebaseAuth;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 30000;  // 1초 1000
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초

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

    //목적지 구현부분//
    TextView btn_showDestination;
    private String destinationMarkerPoint; //목적지 받아올 스트링 변수

    //DBtestString
    //목적지
    public String testdatabaseString;//받은스트링
    public String testdatabaseString2;//자른스트링
    public String testdatabaseString3;//최종자른스트링
    //실시간
    public String locationString;
    public String locationString2;
    public String locationString3;

    //알림기능 구현관련
    public String Goaltext;//도착정보 알림용
    public String Emetext;//긴급정보 알림용
    NotificationManager manager;//알람바 구현을 위한것
    NotificationCompat.Builder Nbuilder;//알람바 구현을 위한것2
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




/*
        //작업파트2(사용자의 목적지 정보 받아오기)
        //사용자의 목적지 정보 받아와서 destinationMarkerPoint여기에 넣어주기
        //이부분을 인텐트 받는게 아니라 디비에서 받아오는걸로 수정하면됨
        Intent intentDest = getIntent();//날아오는 인텐트 받기

        destinationMarkerPoint=intentDest.getStringExtra("dest");
        Log.d("desttest","dest Come success Guardian Activity : "+destinationMarkerPoint);

         */

        //디비에서 목적지 정보 받기
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(DB_CHILD_DEST);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                testdatabaseString=value.toString();

                //가라컷팅식
                testdatabaseString2= testdatabaseString.substring(18);
                testdatabaseString3=testdatabaseString2.substring(0,testdatabaseString2.length()-1);


                Log.d("desttest","CutdatafromDB : "+testdatabaseString3);
                //데이터베이스에서 전달 및체크완료

                destinationMarkerPoint=testdatabaseString3;//목적지 정보 실행
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
//디비에서 받기1


//디비에서 받기 2 사용자 현재위치 받기
        DatabaseReference myRef2 = database.getReference(DB_CHILD_CURRENTLOCATION);
        myRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                locationString=value.toString();
                //가라컷팅식
                locationString2= locationString.substring(18);
                locationString3=locationString2.substring(0,locationString2.length()-1);

                Log.d("locationDB","datafromDB : "+locationString);
                Log.d("locationDB","CutdatafromDB : "+locationString3);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //디비서 받기 2 끝

        //디비서 받기 3 도착 정보 받기
        DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
        myGoal.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                Goaltext=value.toString();
                Log.d("goal","database value  : "+Goaltext);
                //디비3정보 도착여부 확인 //작업3 진동과 알림넣기
                if(Goaltext.equals("true")){
                    Toast.makeText(GuardianActivity.this, "도착", Toast.LENGTH_LONG).show();
                    showNotificationGoal();
                }
                //여기에 알람바 구현
                //

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //디비받기 3 끝

        //디비받기4
        DatabaseReference myEme = database.getReference(DB_CHILD_EME);
        myEme.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue(Object.class);
                Emetext=value.toString();
                Log.d("Eme","database value  : "+Emetext);
                //디비3정보 도착여부 확인 //작업3 진동과 알림넣기
                if(Emetext.equals("true")){

                    Toast.makeText(GuardianActivity.this, "긴급상황 발생", Toast.LENGTH_LONG).show();
                    showNotificationEmergency();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //


        //피보호자에게 전화걸기
        btnCallUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //번호 디비로 @@@@@@
                //디비 연동 안해서 테스트로 내번호
                Intent tt = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+"01077675517"));
                startActivity(tt);
            }
        });






        // 피보호자 등록 화면
        ward_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EnrollmentActivity.class);
                startActivity(intent);
            }
        });


        initialize();
    }

    //도착 알람바 코드
    public void showNotificationGoal(){
        Nbuilder = null;
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //버전별 나눔
        //버전 오레오 이상일 경우
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            );

            Nbuilder = new NotificationCompat.Builder(this,CHANNEL_ID);

            //하위 버전일 경우
        }else{
            Nbuilder = new NotificationCompat.Builder(this); }

        //진동
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(500);

        //알람음
        MediaPlayer player = MediaPlayer.create(GuardianActivity.this,R.raw.goal1);
        player.start();

        //알림바
        Nbuilder.setContentTitle("목적지 정상 도착");
        Nbuilder.setContentText("사용자가 목적지에 도착하였습니다.");
        Nbuilder.setSmallIcon(R.drawable.smile);

        Notification notification = Nbuilder.build();
        manager.notify(1,notification);

    }
    //

    //긴급알람 알람바 코드
    public void showNotificationEmergency(){
        Nbuilder = null;
        manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        //버전별 나눔
        //버전 오레오 이상일 경우
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID2, CHANNEL_NAME2, NotificationManager.IMPORTANCE_DEFAULT)
            );

            Nbuilder = new NotificationCompat.Builder(this,CHANNEL_ID2);

            //하위 버전일 경우
        }else{
            Nbuilder = new NotificationCompat.Builder(this); }

        //진동
        Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(5000);

        //알림음
        MediaPlayer player = MediaPlayer.create(GuardianActivity.this,R.raw.warning1);
        player.start();

        //알랍바
        Nbuilder.setContentTitle("긴급 상황 발생 즉시 확인 요망");
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

            startLocationUpdates(); // 3. 위치 업데이트 시작

        }else {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

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
        //피보호자 목적지 확인하기
        btn_showDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //여기서 메인에서 입력받은 목적지 ->체크완료
                Log.d("desttest","using DestData from MainActivity : "+destinationMarkerPoint);
                //
                String str = destinationMarkerPoint;

                List<Address>addressList=null;
                try {
                    //지오코딩 을 통한 스트링 내 정보 (주소 지역 장소등) 변환
                    addressList = geocoder.getFromLocationName(str,5);
                }catch (IOException e){
                    e.printStackTrace();
                }
                System.out.println(addressList.get(0).toString());
                // 콤마를 기준으로 split
                String []splitStr = addressList.get(0).toString().split(",");
                String address = splitStr[0].substring(splitStr[0].indexOf("\"") + 1,splitStr[0].length() - 2); // 주소
                System.out.println(address);

                String latitude = splitStr[10].substring(splitStr[10].indexOf("=") + 1); // 위도
                String longitude = splitStr[12].substring(splitStr[12].indexOf("=") + 1); // 경도
                System.out.println(latitude);
                System.out.println(longitude);

                // 좌표(위도, 경도) 생성
                LatLng point = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                // 마커 생성
                MarkerOptions mOptions2 = new MarkerOptions();
                mOptions2.title("search result");
                mOptions2.snippet(address);
                mOptions2.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                mOptions2.position(point);

                // 마커 추가
                mMap.addMarker(mOptions2);
                // 해당 좌표로 화면 줌
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point,15));

            }
        });

        //현재위치 마커 찍기
        currentlocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("marker","버튼 돌아는 가는중 : "+ " ");
                MarkerOptions currentmOption = new MarkerOptions();
                currentmOption.title("currentLocation");
                Log.d("marker","위도 경도 그대로 오는지 테스트 :"+locationString3);

                String lan = locationString3.substring(0,10);
                String lut = locationString3.substring(14);
                Log.d("marker","위도 :"+lan);
                Log.d("marker","경도 :"+lut);
                //존나 잘짤랐어 기특해
               currentmOption.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                // 좌표(위도, 경도) 생성
                LatLng point = new LatLng(Double.parseDouble(lan), Double.parseDouble(lut));

                currentmOption.position(point);
                mMap.addMarker(currentmOption);
                //포지션 넣기
                //추가하기

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
//작업포인트
                String markerTitle = getCurrentAddress(currentPosition);
                /*
                String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                        + " 경도:" + String.valueOf(location.getLongitude());

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

                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
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
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

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
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";

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

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            boolean check_result = true;

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if ( check_result ) {
                startLocationUpdates();
            }
            else {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();

                }else {

                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

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
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
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

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {
                        Log.d(TAG, "onActivityResult : GPS 활성화 되있음");

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

                // 데이터 초기화 및 생성
                GuardianManagement.getInstance().delAllData();

                startActivity(intent);

            }
        });
    }

    private void logOut () {
        firebaseAuth.getInstance().signOut();
    }

}