package com.example.telemedicinerr;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.widget.Button;

import android.os.Handler;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

import static java.lang.String.*;
//import java.util.Locale;

public class Mainactivity extends AppCompatActivity {

    Button button1;
    private FileWriter Writer;
    int RRCounter=0;
    Handler handler = new Handler();

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainactivity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        button1 = findViewById(R.id.button1);
        RRCounter = (int) CalActivity.mRRCounter;
        startRepeating();
        button1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onClick(View v) {
                Toast.makeText(Mainactivity.this, "START", Toast.LENGTH_SHORT).show();
                Intent intentdata= new Intent(Mainactivity.this, CalActivity.class);
                startActivity(intentdata);
                finish();
                startActivity(getParentActivityIntent());

            }
        });


    }
    private void updateView() {
        if(CalActivity.mRRCounter>RRCounter){
            TextView RRString= findViewById(R.id.text1);
            RRString.setText(new String(String.valueOf(RRCounter)));

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        final Context context = this;

        //noinspection SimplifiableIfStatement
        if (id == R.id.activity_cal) {
            Intent intent = new Intent(context, CalActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onResume(){
        super.onResume();
        try {
            Writer=new FileWriter("Sensors.txt",true);
            } catch (IOException e)
            {
            e.printStackTrace();
            }
        }
    @Override
    public void onPause() {
        super.onPause();
        if (Writer != null) {
            try {
                Writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeating();
    }
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            try {
                updateView();
            } finally {
                handler.postDelayed(runnable, 500);
            }
        }
    };



    void startRepeating() {
        runnable.run();
    }

    void stopRepeating() {
        handler.removeCallbacks(runnable);
    }
}