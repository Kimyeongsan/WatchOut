package com.example.watchout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.watchout.Data.GuardianManagement;
import com.example.watchout.Login.GuardianLoginActivity;
import com.google.firebase.auth.FirebaseAuth;


public class EnrollmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        getSupportActionBar().hide();

        initialize();
    }

    private void initialize() {
        Button btnEnroll;

        btnEnroll = findViewById(R.id.btnEnroll);

        btnEnroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(
                        getApplicationContext(), GuardianLoginActivity.class);

                Toast.makeText(getApplicationContext(), "보호자 등록 완료", Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        });
    }
}


