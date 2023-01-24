package com.github.gabachogabagaba.mytv;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class PlayControl extends HandlerThread {
    static final int MSG_ON_PAUSE = 0;
    static final int MSG_ON_RESUME = 1;
    static final int MSG_ON_STOP = 2;
    static final int MSG_ON_DESTROY = 2;
    static final int MSG_TOGGLE_STATUS = 3;
    final String TAG = "PlayControl";
    private Handler mainHandler;
    EncoderControl encoderControl;
    ExoControl exoControl;
    Lirc lirc;
    Context context;
    PlayActivity playActivity;
    PlayControlPrintStatus playControlPrintStatus;
    Timer timerPlayControlStatus = null;
    boolean boolPrintStatus = false;
    boolean autoPower = false;
    String lircRemoteName;
    String lircPowerButtonName;
    String lircChUpName;
    String lircChDnName;
    PlayControlHandler mHandler = null;

    PlayControl(String name, String encoderURL, String mediaURL, String lircAddress, int lircPort, int latencyTarget, boolean autoPower, String lircRemoteName, String lircPowerButtonName, String lircChUpName, String lircChDnName, View view, Handler mainHandler, boolean boolPrintStatus, Context context) {
        super(name);
        encoderControl = new EncoderControl(encoderURL);
        exoControl = new ExoControl(mediaURL, latencyTarget, view, mainHandler, context);

        this.mainHandler = mainHandler;
        lirc = new Lirc("Lirc", lircAddress, lircPort);
        this.context = context;
        playActivity = (PlayActivity)context;
        this.boolPrintStatus = boolPrintStatus;
        this.autoPower = autoPower;
        this.lircRemoteName = lircRemoteName;
        this.lircPowerButtonName = lircPowerButtonName;
        this.lircChUpName = lircChUpName;
        this.lircChDnName = lircChDnName;
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();
        mHandler = new PlayControlHandler(getLooper());
    }

    private class PlayControlHandler extends Handler {
        public PlayControlHandler(Looper looper){
            super(looper);
        }
        public void handleMessage(Message msg) {
            if (msg.what == MSG_ON_RESUME) {
                Log.d(TAG, "handling MSG_ON_RESUME");
                lirc.start();
//                    Start the encoder
                encoderControl.setEncoderState(true);

                if (autoPower) {
                    // Start the lirc client
                    lirc.connect();
                    // If encder's HDMI input is not active send an IR power command once.
                    if (!encoderControl.getInputState()) {
                        Log.d(TAG, "HDMI state: no signal");
                        LircData lircData = new LircData();
                        lircData.remote = lircRemoteName;
                        lircData.button = lircPowerButtonName;
                        lirc.send(lircData);
                    }
                }
//                    Start the playback.
                exoControl.start();
//                    Start status printing
                if (boolPrintStatus) {
                    enablePrintStatus();
                }
            }

            if (msg.what == MSG_ON_PAUSE) {
//                    Stop the encoder
                if (boolPrintStatus) {
                    timerPlayControlStatus.cancel();
                }
                exoControl.stop();
                if (autoPower) {
                    //                  If HDMI input is up, send power command to the source to shut it down.
                    if (encoderControl.getInputState()) {
                        LircData lircData = new LircData();
                        lircData.remote = lircRemoteName;
                        lircData.button = lircPowerButtonName;
                        lirc.send(lircData);
                    }
                    lircStop();
                }
//                    Shutdown the encoder.
                encoderControl.setEncoderState(false);
                getLooper().quit();
            }
            if(msg.what == MSG_TOGGLE_STATUS){
                togglePrintStatus();
            }
        }
    }

    public void togglePrintStatus(){
        boolPrintStatus = !boolPrintStatus;
        if(boolPrintStatus){
            enablePrintStatus();
        }
        else{
            disablePrintStatus();
        }
    }
    private void enablePrintStatus(){
        playControlPrintStatus = new PlayControlPrintStatus();
        timerPlayControlStatus = new Timer("PlayControlStatus");
        timerPlayControlStatus.schedule(playControlPrintStatus, 0, 1000);
    }

    private void disablePrintStatus(){
        timerPlayControlStatus.cancel();
        timerPlayControlStatus = null;
        playControlPrintStatus = null;
        playActivity.printStatus("");
    }

    private void lircStop() {
        if (lirc != null) {
            lirc.disconnect();
            lirc = null;
        }
    }

    public void lircSend(String buttonName) {
        LircData lircData = new LircData();
        lircData.remote = lircRemoteName;
        lircData.button = buttonName;
        lirc.send(lircData);
    }
    public boolean isHandlerReady() {
        return mHandler != null;
    }
    public void onPause(){
        Message message = Message.obtain();
        message.what = MSG_ON_PAUSE;
        Log.d("PlayControl", "onPause()");
        mHandler.sendMessage(message);
    }
    public void onResume(){
        Message message = Message.obtain();
        message.what = MSG_ON_RESUME;
        Log.d("PlayControl", "onResume()");
        mHandler.sendMessage(message);
    }

    class PlayControlPrintStatus extends TimerTask {
        @Override
        public void run() {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    printStatus();
                }
            });
        }

        private void printStatus(){
            String str;
            ExoControlStatus exoControlStatus;
            exoControlStatus = exoControl.getStatus();
            if(exoControlStatus != null){
                str = String.format("\n\n Offset: %d ms\n Offset error: %d ms\n Speed %.2f\n %s\n %s",
                        exoControlStatus.offset, exoControlStatus.offsetError, exoControlStatus.speed,
                        exoControlStatus.videoFormat.toString(),
                        exoControlStatus.audioFormat.toString());
                playActivity.printStatus(str);
            }
        }
    }
}
