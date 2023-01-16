package com.github.gabachogabagaba.mytv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class PlayActivity extends AppCompatActivity {
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

    TextView statusTextView;
    Handler handler = new Handler();

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        }
        return true;
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop();");

        playControl.onStop();

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
        this.conf_lirc_remote_name = prefs.getString("lirc_remote_name", "");
        this.conf_lirc_power_button_name = prefs.getString("lirc_power_button_name", "");
        this.conf_latency_target = Integer.parseInt(prefs.getString("latency_target", "200"));
        this.conf_stream_info = prefs.getBoolean("stream_info", false);
        this.conf_auto_power = prefs.getBoolean("auto_power", false);
        this.conf_lirc_chup_name = prefs.getString("lirc_chup_name",  "");
        this.conf_lirc_chdn_name = prefs.getString("lirc_chdn_name",  "");
        playControl = new PlayControl(encoderURL, mediaURL, conf_lirc_ip_address, conf_lirc_port, conf_latency_target, conf_auto_power, this.conf_lirc_remote_name, this.conf_lirc_power_button_name, this.conf_lirc_chup_name, this.conf_lirc_chdn_name, findViewById(R.id.idExoPlayerVIew), handler, conf_stream_info, this);
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

}
