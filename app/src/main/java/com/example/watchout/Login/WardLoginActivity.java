package com.example.watchout.Login;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.watchout.MainActivity;
import com.example.watchout.R;

public class WardLoginActivity extends AppCompatActivity {
    EditText id, name;
    Button btn;
    String loginId, loginName;
    Button transform_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginward);
        getSupportActionBar().hide();

        id = (EditText)findViewById(R.id.Id);
        name = (EditText)findViewById(R.id.Name);
        btn = (Button)findViewById(R.id.login_summit);
        SharedPreferences auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);

        loginId = auto.getString("inputId",null);
        loginName = auto.getString("inputPwd",null);

        transform_btn= findViewById(R.id.transform_btn);
        // 화면전환 버튼
        transform_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), GuardianLoginActivity.class);
                startActivity(intent);
            }
        });


        if(loginId !=null && loginName != null) {
            if(loginId.equals("HGD") && loginName.equals("홍길동")) {
                Toast.makeText(WardLoginActivity.this, loginId +"님 자동로그인 입니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WardLoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

        else if(loginId == null && loginName == null){
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (id.getText().toString().equals("HGD") && name.getText().toString().equals("홍길동")) {
                        SharedPreferences auto = getSharedPreferences("auto", Activity.MODE_PRIVATE);

                        SharedPreferences.Editor autoLogin = auto.edit();
                        autoLogin.putString("inputId", id.getText().toString());
                        autoLogin.putString("inputPwd", name.getText().toString());

                        autoLogin.commit();
                        Toast.makeText(WardLoginActivity.this, id.getText().toString()+"님 환영합니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(WardLoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

        }
    }
}