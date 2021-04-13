package com.example.watchout;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class WardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ward);
        getSupportActionBar().hide();
    }
}