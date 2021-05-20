package com.example.watchout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.watchout.Camera.DetectorActivity;
import com.example.watchout.Data.DB_Data;
import com.example.watchout.Data.GuardianManagement;
import com.example.watchout.Data.LocationData;
import com.example.watchout.Login.WardLoginActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static com.example.watchout.Data.DB_Data.DB_CHILD_CURRENTLOCATION;
import static com.example.watchout.Data.DB_Data.DB_CHILD_DEST;

import static com.example.watchout.Data.DB_Data.DB_CHILD_EME;
import static com.example.watchout.Data.DB_Data.DB_CHILD_GOAL;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    Intent intent;
    SpeechRecognizer mRecognizer;
    Button btn1, btn2, btn3, btn4, camt; // 화면 타이틀 버튼
    private TextToSpeech tts;
    final int PERMISSION = 1;
    StringBuilder destination = new StringBuilder(" "); //입력받은 목적지 정보 임시 저장
    StringBuilder outString = new StringBuilder(" "); //출력문

    public boolean destCheck = false; //목적지 정보가 입력된경우 true로 상태변경
    private FirebaseAuth firebaseAuth;

    Button temp2; // test

    private GoogleMap mMap;

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 10000;  // 1초 1000
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;

    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    Location mCurrentLocatiion;
    LatLng currentPosition;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;

    private View mLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        //google Map
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mLayout = findViewById(R.id.layout_main);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);


        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //permission check
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }

        //지워도 됨 근데 test편하게 하는 버튼
        temp2 = (Button) findViewById(R.id.test2trans);
        temp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentDest = new Intent(MainActivity.this, GuardianActivity.class);
                intentDest.putExtra("dest", destination.toString());
                startActivity(intentDest);
            }
        });

        //카메라 테스트
        camt = (Button)findViewById(R.id.camtest);
        camt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentDest = new Intent(MainActivity.this, DetectorActivity.class);
                startActivity(intentDest);
            }
        });

        //버튼연동
        btn1 = (Button) findViewById(R.id.btn_Top_left);
        btn2 = (Button) findViewById(R.id.btn_Top_Right);
        btn3 = (Button) findViewById(R.id.btn_Bottom_Left);
        btn4 = (Button) findViewById(R.id.btn_Bottom_Right);

        //tts 초기화
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        //stt 준비
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        LocationData locationData = new LocationData();

        //목적지 설정 버튼
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //destcheck default = false
                if (destCheck == false) {
                    //초기 가이드
                    tts.speak(getString(R.string.Guide), TextToSpeech.QUEUE_FLUSH, null);
                } else if (destCheck == true) {

                    outString.delete(0, outString.length());
                    outString.append(getString(R.string.button_1_1));
                    outString.append(destination);
                    outString.append(getString(R.string.button_1_2));
                    outString.append(getString(R.string.button_1_3));

                    //목적지 확인 로그
                    Log.d("desttest", "input dest : " + destination.toString());

                    locationData.setUser_destination(destination.toString());

                    /*
                    //firebase 추가2 사용자의 목적지 데이터 올리는부분
                    FirebaseDatabase.getInstance().getReference().
                            child(DB_Data.DB_CHILD_USER_WARD).
                            child(DB_Data.DB_CHILD_DEST).push().setValue(locationData);

                     */

                    //파베 올리는거 변경 테스트 이거쓸꺼임 이걸로 올린 데이터 쓰겠음
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference myRef = database.getReference(DB_CHILD_DEST);
                    myRef.setValue(locationData);

                    tts.speak(outString.toString(), TextToSpeech.QUEUE_FLUSH, null);

                }
            }
        });

        btn1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //목적지 초기화
                destination.delete(0, destination.length());

                //인식 시작
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
                //인식 종료

                //도착여부조건발송 미도착
                String GoalMessage ="false";
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
                myGoal.setValue(GoalMessage);
                Log.d("goal","database upload BTN1  : "+GoalMessage);
                // 미도착
                //긴급상황 아님
                String EmeMessage="false";
                DatabaseReference myEme = database.getReference(DB_CHILD_EME);
                myEme.setValue(EmeMessage);
                Log.d("eme","database upload BTN4  : "+EmeMessage);
                //

                //목적지 입력 완료
                destCheck = true;
                //
                return false;
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_2_1), TextToSpeech.QUEUE_FLUSH, null);

                //목적지 도착을 위한 데이터 베이스 신호 전송
                String GoalMessage ="true";
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
                myGoal.setValue(GoalMessage);
                Log.d("goal","database upload value  : "+GoalMessage);

                //긴급상황 아님신호
                String EmeMessage="false";
                DatabaseReference myEme = database.getReference(DB_CHILD_EME);
                myEme.setValue(EmeMessage);
                Log.d("eme","database upload BTN4  : "+EmeMessage);
                //
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_3_1), TextToSpeech.QUEUE_FLUSH, null);
                //도착여부조건발송 미도착
                String GoalMessage ="false";
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
                myGoal.setValue(GoalMessage);
                Log.d("goal","database upload BTN3  : "+GoalMessage);
                // 미도착

                //긴급상황 아님
                String EmeMessage="false";
                DatabaseReference myEme = database.getReference(DB_CHILD_EME);
                myEme.setValue(EmeMessage);
                Log.d("eme","database upload BTN4  : "+EmeMessage);
                //
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_4_1), TextToSpeech.QUEUE_FLUSH, null);
                //도착여부조건발송 미도착
                String GoalMessage ="false";
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myGoal = database.getReference(DB_CHILD_GOAL);
                myGoal.setValue(GoalMessage);
                Log.d("goal","database upload BTN4  : "+GoalMessage);
                // 미도착

                //긴급상황 발생 전송
                String EmeMessage="true";
                DatabaseReference myEme = database.getReference(DB_CHILD_EME);
                myEme.setValue(EmeMessage);
                Log.d("eme","database upload BTN4  : "+EmeMessage);
                //
            }
        });

        //로그아웃 함수
        logOuts();
    }

    //stt를 위한 리스너
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(), "음성 인식 시작", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onEndOfSpeech() {
            Toast.makeText(getApplicationContext(), "인식 종료됨", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {

            ArrayList<String> matches =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for (int i = 0; i < matches.size(); i++) {
                destination.append(matches.get(i));
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    };

    //TTS 종료 안된경우 종료
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");

        mMap = googleMap;

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            startLocationUpdates();

        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latLng) {

                Log.d(TAG, "onMapClick :");
            }
        });
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();
            LocationData locationData = new LocationData();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);

                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());

                String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                        + " 경도:" + String.valueOf(location.getLongitude());

                locationData.setUser_location(markerSnippet);

                //firebase 위치 로그
                FirebaseDatabase.getInstance().getReference().
                        child(DB_Data.DB_CHILD_USER_WARD).
                        child(DB_Data.DB_CHILD_LOCATION).
                        push().setValue(locationData);

                //파베이걸로 바꿈 위에 코드는 이동기록 로그고 예는 실시간 위치
                //파베 올리는거 변경 테스트 이거쓸꺼임 이걸로 올린 데이터 쓰겠음
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference myRef2 = database.getReference(DB_CHILD_CURRENTLOCATION);
                myRef2.setValue(locationData);



                Log.d(TAG, "onLocationResult : " + markerSnippet);

                mCurrentLocatiion = location;
            }
        }
    };

    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {

            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        } else {

            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);

            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) {

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

            if (mMap != null)
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

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private boolean checkPermission() {

        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

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

                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                } else {

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

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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

    //로그아웃
    private void logOuts() {
        Button btnLogOut;

        btnLogOut = findViewById(R.id.ward_logout);

        firebaseAuth = FirebaseAuth.getInstance();

        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOut();
                Intent intent = new Intent(
                        getApplicationContext(), WardLoginActivity.class);

                // 데이터 초기화 및 생성
                GuardianManagement.getInstance().delAllData();
                startActivity(intent);
            }
        });
    }

    private void logOut() {
        firebaseAuth.getInstance().signOut();
    }

}