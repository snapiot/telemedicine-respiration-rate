package com.example.telemedicinerr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Handler handler =new Handler();
        handler.postDelayed (() -> {
            Intent splash=new Intent(SplashActivity.this, Mainactivity.class);
            startActivity(splash);
            finish();
        },2500);
    }
}
