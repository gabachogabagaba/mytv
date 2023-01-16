package com.github.gabachogabagaba.mytv;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    Button buttonPlay;
    Button buttonSetting;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        buttonPlay = (findViewById(R.id.idButtonPlay));
        buttonSetting = (findViewById(R.id.idButtonSetting));

        buttonPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                play();
            }
        });

        buttonSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                settings();
            }
        });
    }

    public void settings() {
        Intent intent = new Intent(this, PreferenceActivity.class);
        startActivity(intent);
    }

    public void play(){
        Intent intent = new Intent(this, PlayActivity.class);
        startActivity(intent);
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop();");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause();");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume();");
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy();");
    }
}