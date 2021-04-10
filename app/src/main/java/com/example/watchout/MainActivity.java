package com.example.watchout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class MainActivity extends AppCompatActivity {

    Intent intent;
    SpeechRecognizer mRecognizer;
    Button btn1,btn2,btn3,btn4;
    private TextToSpeech tts;
    final int PERMISSION = 1;
    StringBuilder destination = new StringBuilder(" "); //입력받은 목적지 정보 임시 저장
    StringBuilder outString = new StringBuilder(" "); //출력문


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        //permission check
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET,
                    Manifest.permission.RECORD_AUDIO}, PERMISSION);
        }
        //

        //버튼연동
        btn1=(Button)findViewById(R.id.btn_Top_left);
        btn2=(Button)findViewById(R.id.btn_Top_Right);
        btn3=(Button)findViewById(R.id.btn_Bottom_Left);
        btn4=(Button)findViewById(R.id.btn_Bottom_Right);
        //

        //tts 초기화
        tts =new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status!=ERROR){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });
        //


        //stt 준비
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");
        //

        //앱 시작하자마자 출력할 TTS <아직 작동안함!>
        tts.speak(getString(R.string.Guide),TextToSpeech.QUEUE_FLUSH,null);

        //목적지 설정 버튼 < 인식, 출력 동시에 되고 있는거 수정해야함>
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //인식 시작
                mRecognizer=SpeechRecognizer.createSpeechRecognizer(getBaseContext());
                mRecognizer.setRecognitionListener(listener);
                mRecognizer.startListening(intent);
                //

                //목적지 인식후 출려될 출력문
                // 합치고 출력
                //<인식이 된 이후 출력되게 수정 필요>
                outString.append(getString(R.string.button_1_1));
                outString.append(destination);
                outString.append(getString(R.string.button_1_2));
                outString.append(getString(R.string.button_1_3));

                tts.speak(outString.toString(),TextToSpeech.QUEUE_FLUSH,null);
                //
            }
        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_2_1),TextToSpeech.QUEUE_FLUSH,null);
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_3_1),TextToSpeech.QUEUE_FLUSH,null);
            }
        });

        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts.speak(getString(R.string.button_4_1),TextToSpeech.QUEUE_FLUSH,null);
            }
        });


    }
    //stt를 위한 리스너
    private RecognitionListener listener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Toast.makeText(getApplicationContext(),"음성 인식 시작",Toast.LENGTH_SHORT).show();

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

        }

        @Override
        public void onError(int error) {

        }

        @Override
        public void onResults(Bundle results) {

            //ArrayList에 단어를 넣고 textview에 단어를 하나씩 이어주기
            ArrayList<String> matches=
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            for(int i = 0; i< matches.size();i++){
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
        if(tts!=null){
            tts.stop();
            tts.shutdown();
            tts=null;
        }
    }

    //
}