package com.github.gabachogabagaba.mytv;

import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.PlaybackStatsListener;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.Format;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ExoControl {
    ExoPlayer exoPlayer;
    StyledPlayerView playerView;
    Uri mediaUri;
    DefaultLoadControl loadControl;
    MediaItem mediaItem;
    Handler mainHandler;
    ExoOffsetControl offsetControl;
    Timer timerOffsetControl;
    final static String TAG = "ExoControl";
    RenderersFactory renderersFactory;

    ExoControl(String mediaURL, int latencyTarget, View view, Handler mainHandler, Context context) {
        this.mainHandler = mainHandler;
        playerView = (StyledPlayerView)view;

        playerView.setUseController(false);
        mediaUri = Uri.parse(mediaURL);
        Log.d(TAG, String.format("medialURL: %s", mediaURL));

        loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(500, 5000, 500, 500)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        renderersFactory = new DefaultRenderersFactory(context)
                .setMediaCodecSelector(
                        (mimeType, requiresSecureDecoder, requiresTunnelingDecoder) -> {
                            boolean sec;
                            if(mimeType == "video/hevc") {
                                sec = false;
                            }
                            else{
                                sec = requiresSecureDecoder;
                            }
                            List<MediaCodecInfo> codecs = MediaCodecSelector.DEFAULT.getDecoderInfos(
                                    mimeType,
//                                    requiresSecureDecoder,
                                    sec,
                                    requiresTunnelingDecoder
                            );
                            if (codecs.isEmpty()) {
                                return Collections.emptyList();
                            }
                            // Only consider the first decoder, or iterate the list and select the decoder
                            // that is hardwareAccelerated.
                            for(MediaCodecInfo mediaCodecInfo : codecs){
                                Log.d(TAG, mimeType + mediaCodecInfo.toString());
                            }

                            return Collections.singletonList(codecs.get(0));
                        }
                );

        exoPlayer = new ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setRenderersFactory(renderersFactory)
//                .setMediaSourceFactory(
//                new DefaultMediaSourceFactory(context).setLiveTargetOffsetMs(latencyTarget))
                .build();
        mediaItem = new MediaItem.Builder()
                .setUri(mediaUri)
//                .setLiveConfiguration(
//                        new MediaItem.LiveConfiguration.Builder()
//                                .setMaxPlaybackSpeed(1.10f)
//                                .build())
                .build();

        exoPlayer.setMediaItem(mediaItem);
        playerView.setPlayer(exoPlayer);
        AnalyticsListener analyticsListener = new PlaybackStatsListener(true, null);
        exoPlayer.addAnalyticsListener(analyticsListener);
        offsetControl = new ExoOffsetControl();
        offsetControl.init(mainHandler, exoPlayer, latencyTarget);

        timerOffsetControl = new Timer();
    }
    public void start() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(true);
                timerOffsetControl.schedule(offsetControl, 0, 50);
            }
        });
    }

    public void stop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                timerOffsetControl.cancel();
                exoPlayer.stop();
            }
        });
    }
    public ExoControlStatus getStatus(){
        ExoControlStatus exoControlStatus = offsetControl.getStatus();
        return exoControlStatus;
    }
}

class ExoOffsetControl extends TimerTask {
    long offsetTarget;
    Handler mainHandler;
    ExoPlayer exoPlayer;
    String TAG = "ExoOffsetControl";
//    Number of latency queries to average per playback speed update
    static final int NAVG = 10;
    long offsetInt;
    int i;
    int state;
    float speedPrev;
    float speed = 1f;
    ExoControlStatus exoControlStatus = new ExoControlStatus();
    Handler handler;
    Format videoFormat = null;
    Format audioFormat = null;

    public void init(Handler mainHandler, ExoPlayer exoPlayer, long offsetTarget) {
        this.mainHandler = mainHandler;
        this.exoPlayer = exoPlayer;
        this.offsetTarget = offsetTarget;
        i = 0;
        offsetInt = 0;
        state = 0;
        speedPrev = 0;
        handler = new Handler();
    };

    public void setLatencyTarget(long offsetTarget) {
        this.offsetTarget = offsetTarget;
    };
    public ExoControlStatus getStatus(){
        if(exoControlStatus.videoFormat != null && exoControlStatus.audioFormat != null){
        return exoControlStatus;
        }
        else{
            return null;
        }
    };

    @Override
    public void run() {
//      Obtain playback status
        long offsetAvg;
        long offsetError;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                long offset;
                Format fa, fv;
                offset = exoPlayer.getTotalBufferedDuration();
                offsetInt += offset;
                videoFormat = exoPlayer.getVideoFormat();
                audioFormat = exoPlayer.getAudioFormat();
            }
        });

        if (i >= NAVG - 1){
            offsetAvg = offsetInt / NAVG;
            offsetError = offsetAvg - offsetTarget;

//            if(offsetError > 2000) {
//                mainHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        exoPlayer.seekTo(exoPlayer.getCurrentPosition() + offsetError - 2000);
//                    }
//                });
//            }
            if(offsetError > -10 && offsetError < 10 && state == 0) {
                state = 1;
                speed = 1f;
            }
            else if(offsetError < 150 && offsetError > -150 && state == 1) {
                speed = 1f;
            }
            else {
                speed = (float) (1.0D + offsetError / 10000D);
                state = 0;
            }

            if(speed > 1.5) {
                speed = 1.5f;
            }
            else if (speed < 0.8) {
                speed = 0.8f;
            }
            if(speed != speedPrev) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        exoPlayer.setPlaybackSpeed(speed);
                    }
                });

                speedPrev = speed;
            }
            setStatus(offsetAvg, offsetError, speed, videoFormat, audioFormat);
            Log.d(TAG, String.format("offsetAvg: %d, offsetError: %d, speed: %f", offsetAvg, offsetError, speed));
            i = 0;
            offsetInt = 0;
        }
        else {
            i ++;
        }
    }
    private void setStatus(long offset, long offsetError, float speed, Format videoFormat, Format audioFormat){
        exoControlStatus.offset= offset;
        exoControlStatus.offsetError= offsetError;
        exoControlStatus.speed = speed;
        exoControlStatus.videoFormat = videoFormat;
        exoControlStatus.audioFormat = audioFormat;
    }
}

class ExoControlStatus{
    long offset = 0;
    long offsetError = 0;
    float speed = 0f;
    Format videoFormat = null;
    Format audioFormat = null;
}