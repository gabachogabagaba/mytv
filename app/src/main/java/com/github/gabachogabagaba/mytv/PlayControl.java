package com.github.gabachogabagaba.mytv;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;


public class PlayControl extends Thread {
    public Handler handler;
    static final int MSG_ON_PAUSE = 0;
    static final int MSG_ON_RESUME = 1;
    static final int MSG_ON_STOP = 2;
    static final int MSG_ON_DESTROY = 2;
    final String TAG = "PlayControl";
    private Handler mainHandler;
    EncoderControl encoderControl;
    ExoControl exoControl;
    Lirc lirc;
    Context context;
    PlayActivity playActivity;
    PlayControlPrintStatus playControlPrintStatus;
    Timer timerPlayControlStatus;
    boolean boolPrintStatus = false;
    boolean autoPower = false;
    String lircRemoteName;
    String lircPowerButtonName;
    String lircChUpName;
    String lircChDnName;
    boolean handlerReady = false;

    PlayControl(String encoderURL, String mediaURL, String lircAddress, int lircPort, int latencyTarget, boolean autoPower, String lircRemoteName, String lircPowerButtonName, String lircChUpName, String lircChDnName, View view, Handler mainHandler, boolean boolPrintStatus, Context context) {
        encoderControl = new EncoderControl(encoderURL);
        exoControl = new ExoControl(mediaURL, latencyTarget, view, mainHandler, context);

        this.mainHandler = mainHandler;
        lirc = new Lirc(lircAddress, lircPort);
        this.context = context;
        playActivity = (PlayActivity)context;
        playControlPrintStatus = new PlayControlPrintStatus();
        timerPlayControlStatus = new Timer();
        this.boolPrintStatus = boolPrintStatus;
        this.autoPower = autoPower;
        this.lircRemoteName = lircRemoteName;
        this.lircPowerButtonName = lircPowerButtonName;
        this.lircChUpName = lircChUpName;
        this.lircChDnName = lircChDnName;
        handlerReady = false;
    }

    @Override
    public void run() {
        Looper.prepare();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg){
                if(msg.what == MSG_ON_RESUME){
                    Log.d(TAG, "handling MSG_ON_RESUME");
                    lirc.start();
//                    Start the encoder
                    encoderControl.setEncoderState(true);

                    if(autoPower) {
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
                    if(boolPrintStatus) {
                        timerPlayControlStatus.schedule(playControlPrintStatus, 1000, 1000);
                    }
                }

                if(msg.what == MSG_ON_PAUSE) {
//                    Stop the encoder
                    if(boolPrintStatus) {
                        timerPlayControlStatus.cancel();
                    }
                    exoControl.stop();
                    if(autoPower) {
                        //                  If HDMI input is up, send power command to the source to shut it down.
                        if (encoderControl.getInputState()) {
                            LircData lircData = new LircData();
                            lircData.remote = lircRemoteName;
                            lircData.button = lircPowerButtonName;
                            lirc.send(lircData);
                        }
                        lirc.quit();
                    }
//                    Shutdown the encoder.
                    encoderControl.setEncoderState(false);
                }
                if(msg.what == MSG_ON_STOP) {
//                    Stop the thread
                    handlerReady = false;
                    Looper.myLooper().quit();
                }
            }
        };
        handlerReady = true;
        Looper.loop();
    }
    public void lircSend(String buttonName) {
        LircData lircData = new LircData();
        lircData.remote = lircRemoteName;
        lircData.button = buttonName;
        lirc.send(lircData);
    }
    public boolean isHandlerReady() {
        return handlerReady;
    }
    public void onPause(){
        Message message = Message.obtain();
        message.what = MSG_ON_PAUSE;
        Log.d("PlayControl", "onPause()");
        handler.sendMessage(message);
    }
    public void onResume(){
        Message message = Message.obtain();
        message.what = MSG_ON_RESUME;
        Log.d("PlayControl", "onResume()");
        handler.sendMessage(message);
    }
    public void onStop(){
        Message message = Message.obtain();
        message.what = MSG_ON_STOP;
        Log.d("PlayControl", "onStop()");
        handler.sendMessage(message);
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
                str = String.format("\n\n Offset: %d ms\n Offset error: %d ms\n Speed %.2f\n Video: %s, %d x %d, %.0f fps, %d kbps\n Audio: %s, %d Hz, %d kbps",
                        exoControlStatus.offset, exoControlStatus.offsetError, exoControlStatus.speed,
                        exoControlStatus.videoFormat.codecs, exoControlStatus.videoFormat.width, exoControlStatus.videoFormat.height, exoControlStatus.videoFormat.frameRate,exoControlStatus.videoFormat.bitrate,
                        exoControlStatus.audioFormat.codecs, exoControlStatus.audioFormat.sampleRate,exoControlStatus.audioFormat.bitrate);
                playActivity.printStatus(str);
            }
        }
    }
}
