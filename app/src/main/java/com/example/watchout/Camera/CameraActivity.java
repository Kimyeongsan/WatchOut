/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.watchout.Camera;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Trace;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.watchout.Data.DB_Data;
import com.example.watchout.Data.GuardianManagement;
import com.example.watchout.Data.LocationData;
import com.example.watchout.GuardianActivity;
import com.example.watchout.Login.WardLoginActivity;

import com.example.watchout.R;
import com.example.watchout.Tensorflow.env.ImageUtils;
import com.example.watchout.Tensorflow.env.Logger;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;
import static com.example.watchout.Data.DB_Data.DB_CHILD_CURRENTLOCATION;
import static com.example.watchout.Data.DB_Data.DB_CHILD_DEST;
import static com.example.watchout.Data.DB_Data.DB_CHILD_EME;
import static com.example.watchout.Data.DB_Data.DB_CHILD_GOAL;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback{

  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private SwitchCompat apiSwitchCompat;
  private TextView threadsTextView;

  Intent intent;
  SpeechRecognizer mRecognizer;
  Button btn1, btn2, btn3, btn4, camt; // 화면 타이틀 버튼
  private TextToSpeech tts;
  final int PERMISSION = 1;
  StringBuilder destination = new StringBuilder(" "); //입력받은 목적지 정보 임시 저장
  StringBuilder outString = new StringBuilder(" "); //출력문

  public boolean destCheck = false; //목적지 정보가 입력된경우 true로 상태변경
  private FirebaseAuth firebaseAuth;

  private GoogleMap mMap;

  private static final String TAG = "googlemap_example";
  private static final int GPS_ENABLE_REQUEST_CODE = 2001;
  private static final int UPDATE_INTERVAL_MS = 100000;  // 10초 1000
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
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getSupportActionBar().hide();

    setContentView(R.layout.tfe_od_activity_camera);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    //google Map
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
//        temp2 = (Button) findViewById(R.id.test2trans);
//        temp2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intentDest = new Intent(MainActivity.this, GuardianActivity.class);
//                intentDest.putExtra("dest", destination.toString());
//                startActivity(intentDest);
//            }
//        });

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


    //초기 시작 알람음 ok
    MediaPlayer player = MediaPlayer.create(CameraActivity.this,R.raw.goal1);
    player.start();
    //초기 시작 진동 ok
    Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    vibrator.vibrate(500);
    //초기 시작 TTS 소리가 지금 안남 nope
    tts.speak(getString(R.string.initGuide), TextToSpeech.QUEUE_FLUSH, null);
    Log.d("initttS","init tts activate");



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
      }
    });



    //로그아웃 함수
    logOuts();

    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    apiSwitchCompat = findViewById(R.id.api_info_switch);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                  gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                  gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                //                int width = bottomSheetLayout.getMeasuredWidth();
                int height = gestureLayout.getMeasuredHeight();

                sheetBehavior.setPeekHeight(height);
              }
            });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
            new BottomSheetBehavior.BottomSheetCallback() {
              @Override
              public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                  case BottomSheetBehavior.STATE_HIDDEN:
                    break;
                  case BottomSheetBehavior.STATE_EXPANDED:
                  {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                  }
                  break;
                  case BottomSheetBehavior.STATE_COLLAPSED:
                  {
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                  }
                  break;
                  case BottomSheetBehavior.STATE_DRAGGING:
                    break;
                  case BottomSheetBehavior.STATE_SETTLING:
                    bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                    break;
                }
              }

              @Override
              public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
            });

    frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);

    apiSwitchCompat.setOnCheckedChangeListener(this);

    plusImageView.setOnClickListener(this);
    minusImageView.setOnClickListener(this);
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
            new Runnable() {
              @Override
              public void run() {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
              }
            };

    postInferenceCallback =
            new Runnable() {
              @Override
              public void run() {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
              }
            };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
              new Runnable() {
                @Override
                public void run() {
                  ImageUtils.convertYUV420ToARGB8888(
                          yuvBytes[0],
                          yuvBytes[1],
                          yuvBytes[2],
                          previewWidth,
                          previewHeight,
                          yRowStride,
                          uvRowStride,
                          uvPixelStride,
                          rgbBytes);
                }
              };

      postInferenceCallback =
              new Runnable() {
                @Override
                public void run() {
                  image.close();
                  isProcessingFrame = false;
                }
              };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();

    if (checkPermission()) {

      Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
      mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

      if (mMap != null)
        mMap.setMyLocationEnabled(true);
    }
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();

    if (mFusedLocationClient != null) {

      Log.d(TAG, "onStop : call stopLocationUpdates");
      mFusedLocationClient.removeLocationUpdates(locationCallback);
    }
  }

  //TTS 종료 안된경우 종료
  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();

    if (tts != null) {
      tts.stop();
      tts.shutdown();
      tts = null;
    }
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
          final int permsRequestCode, final String[] permissions, final int[] grandResults) {
    super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
    if (permsRequestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grandResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }


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

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
                .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
          CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
                (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                        || isHardwareLevelSupported(
                        characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
              CameraConnectionFragment.newInstance(
                      new CameraConnectionFragment.ConnectionCallback() {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation) {
                          previewHeight = size.getHeight();
                          previewWidth = size.getWidth();
                          CameraActivity.this.onPreviewSizeChosen(size, rotation);
                        }
                      },
                      this,
                      getLayoutId(),
                      getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
              new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    setUseNNAPI(isChecked);
    if (isChecked) apiSwitchCompat.setText("NNAPI");
    else apiSwitchCompat.setText("TFLITE");
  }

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }

  protected void showFrameInfo(String frameInfo) {
    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);

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
            ActivityCompat.requestPermissions(CameraActivity.this, REQUIRED_PERMISSIONS,
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

  private void showDialogForLocationServiceSetting() {

    AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
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