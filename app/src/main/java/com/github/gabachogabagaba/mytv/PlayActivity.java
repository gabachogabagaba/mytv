package com.github.gabachogabagaba.mytv;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.preference.PreferenceManager;

public class PlayActivity extends AppCompatActivity implements GestureDetector.OnGestureListener{
    final String mediaPath = "live/stream0";
    String encoderURL = "";
    String mediaURL = "";

    final String TAG = "PlayActivity";
    static final int MSG_EXO_START = 0;
    static final int MSG_EXO_STOP = 1;

    String conf_streamer_ip_address = "";
    int conf_streamer_http_port = -1;
    String conf_lirc_ip_address = "";
    int conf_lirc_port = -1;
    int conf_latency_target = -1;
    String conf_lirc_remote_name;
    String conf_lirc_power_button_name;
    boolean conf_auto_power = false;
    boolean conf_stream_info = false;
    String conf_lirc_chdn_name;
    String conf_lirc_chup_name;
    private float x1, x2;
    static final int SWIPE_MIN_DIST = 300;
    static final int SWIPE_MAX_DIST = 1200;
    static final int SWIPE_MIN_VELO = 100;
    TextView statusTextView;
    Handler handler = new Handler();
    private GestureDetectorCompat mDetector;

    PlayControl playControl;
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "Keycode: " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.i(TAG, conf_lirc_chup_name);
                playControl.lircSend(conf_lirc_chup_name);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                playControl.lircSend(conf_lirc_chdn_name);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
                playControl.togglePrintStatus();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_play);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Get TextView for printing status
        statusTextView = (findViewById(R.id.idTextViewStatus));
        mDetector = new GestureDetectorCompat(this,this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop();");

        Log.d(TAG, "Stopping PlayerControl thread.");
        try {
            playControl.join();
        } catch (Exception e) {
        }
        Log.d(TAG, "PlayerControl thread stopped.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        playControl.onPause();
        Log.d(TAG, "onPause();");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume();");
        printStatus("");
        // Read prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.conf_streamer_ip_address = prefs.getString("streamer_ip_address", "192.168.1.1");
        this.conf_streamer_http_port = Integer.parseInt(prefs.getString("streamer_port", "80"));
        this.conf_lirc_ip_address = prefs.getString("lirc_ip_address", "");
        this.conf_lirc_port = Integer.parseInt(prefs.getString("lirc_port", "8765"));
        this.encoderURL = String.format("http://%s:%d", this.conf_streamer_ip_address, this.conf_streamer_http_port);
        this.mediaURL = String.format("http://%s:%d/%s", this.conf_streamer_ip_address, this.conf_streamer_http_port, this.mediaPath);
//        this.mediaURL = String.format("http://%s/hls/stream0.m3u8", this.conf_streamer_ip_address);
//        this.mediaURL = String.format("rtsp://%s/stream0", this.conf_streamer_ip_address);
        this.conf_lirc_remote_name = prefs.getString("lirc_remote_name", "");
        this.conf_lirc_power_button_name = prefs.getString("lirc_power_button_name", "");
        this.conf_latency_target = Integer.parseInt(prefs.getString("latency_target", "200"));
        this.conf_stream_info = prefs.getBoolean("stream_info", false);
        this.conf_auto_power = prefs.getBoolean("auto_power", false);
        this.conf_lirc_chup_name = prefs.getString("lirc_chup_name",  "");
        this.conf_lirc_chdn_name = prefs.getString("lirc_chdn_name",  "");
        playControl = new PlayControl("PlayControl", encoderURL, mediaURL, conf_lirc_ip_address, conf_lirc_port, conf_latency_target, conf_auto_power, this.conf_lirc_remote_name, this.conf_lirc_power_button_name, this.conf_lirc_chup_name, this.conf_lirc_chdn_name, findViewById(R.id.idExoPlayerVIew), handler, conf_stream_info, this);
        playControl.start();
// Wait for the playControl's message handler to be ready after the thread start
        while(playControl.isHandlerReady() == false) {
        }
        playControl.onResume();
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy();");
    }

    public void printStatus(String str){
        handler.post(new Runnable() {
            @Override
            public void run(){
                statusTextView.setText(str);
            }
        });
    }

    @Override
    public boolean onDown(@NonNull MotionEvent motionEvent) {
        Log.d(TAG, "onDown()");
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
        Log.d(TAG, "onSingleTapUp()");
        playControl.togglePrintStatus();
        return true;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        final float xDistance = Math.abs(e1.getX() - e2.getX());
        final float yDistance = Math.abs(e1.getY() - e2.getY());

        if (xDistance > SWIPE_MAX_DIST
                || yDistance > SWIPE_MAX_DIST)
            return false;

        velocityX = Math.abs(velocityX);
        velocityY = Math.abs(velocityY);
        boolean result = false;

        if (velocityX > SWIPE_MIN_VELO
                && xDistance > SWIPE_MIN_DIST) {
            if (e1.getX() > e2.getX()) // right to left
                onSwipeLeft();
            else
                onSwipeRight();

            result = true;
        } else if (velocityY > SWIPE_MIN_VELO
                && yDistance > SWIPE_MIN_DIST) {
            if (e1.getY() > e2.getY()) // bottom to up
                onSwipeUp();
            else
                onSwipeDown();

            result = true;
        }
        return result;
    }

    void onSwipeUp(){
        Log.d(TAG, "onSwipeUp()");
    }

    void onSwipeDown(){
        Log.d(TAG, "onSwipeDown()");
    }

    void onSwipeLeft(){
        Log.d(TAG, "onSwipeLeft()");
        playControl.lircSend(conf_lirc_chdn_name);
    }

    void onSwipeRight(){
        Log.d(TAG, "onSwipeRight()");
        playControl.lircSend(conf_lirc_chup_name);
    }
}
