/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.speaker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * We first get the required permission to use the MIC. If it is granted, then we continue with
 * the application and present the UI with three icons: a MIC icon (if pressed, user can record up
 * to 10 seconds), a Play icon (if clicked, it wil playback the recorded audio file) and a music
 * note icon (if clicked, it plays an MP3 file that is included in the app).
 */
public class MainActivity extends FragmentActivity implements
        AmbientModeSupport.AmbientCallbackProvider {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final long MILLIS_IN_SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final String CADENA100 = "https://cadena100-cope-rrcast.flumotion.com/cope/cadena100.mp3"; // your URL here
    private static final String LOS40 = "http://192.173.31.51/LOS40_SC"; // your URL here
    private static final String COPE = "https://net1-cope-rrcast.flumotion.com/cope/net1.mp3"; // your URL here
    private static final String KISSFM = "http://kissfm.kissfmradio.cires21.com/kissfm.mp3"; // your URL here
    private static final String EUROPAFM = "https://stream.laut.fm/europafm"; // your URL here
    private static int CURRENT_EMISORA = 0;
    Map<Integer, String> emisoras = new HashMap<>();
    private MediaPlayer mMediaPlayer;
    private AppState mState = AppState.READY;

    private RelativeLayout mOuterCircle;
    private View mInnerCircle;

    private ProgressBar mProgressBar;
    private CountDownTimer mCountDownTimer;

    /**
     * Ambient mode controller attached to this display. Used by Activity to see if it is in
     * ambient mode.
     */
    private AmbientModeSupport.AmbientController mAmbientController;

    enum AppState {
        READY, PLAYING_MUSIC, PAUSE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        emisoras.put(0, CADENA100);
        emisoras.put(1, LOS40);
        emisoras.put(2, COPE);
        emisoras.put(3, KISSFM);
        emisoras.put(4, EUROPAFM);

        mOuterCircle = findViewById(R.id.outer_circle);
        mInnerCircle = findViewById(R.id.inner_circle);

        mProgressBar = findViewById(R.id.progress_bar);
        findViewById(R.id.previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous();
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });

        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });
        // Enables Ambient mode.
        mAmbientController = AmbientModeSupport.attach(this);
    }


    private void play() {
        if (mState.equals(AppState.PAUSE)) {
            resumeMusic();
        } else {
            playMusic();
        }
        mState = AppState.PLAYING_MUSIC;
    }

    private void resumeMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
        }
    }

    private void next() {
        mState = AppState.PLAYING_MUSIC;
        CURRENT_EMISORA = (CURRENT_EMISORA + 1) % 5;
        Toast.makeText(this, "CURRENT: " + CURRENT_EMISORA, Toast.LENGTH_LONG).show();
        stopMusic();
        playMusic();
    }
    private void previous() {
        mState = AppState.PLAYING_MUSIC;
        CURRENT_EMISORA = CURRENT_EMISORA > 1 ? Math.abs(CURRENT_EMISORA - 1) % 5 : 0;
        Toast.makeText(this, "CURRENT: " + CURRENT_EMISORA, Toast.LENGTH_LONG).show();
        stopMusic();
        playMusic();
    }

    private void stop (){
           if (mState.equals(AppState.PLAYING_MUSIC) || mState.equals(AppState.PAUSE)) {
               mState = AppState.READY;
               stopMusic();
           }
    }

    private void pause (){
        if (mState.equals(AppState.PLAYING_MUSIC)) {
            mState = AppState.PAUSE;
            pauseMusic();
        }
    }

    private void pauseMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }


    /**
     * Plays back the MP3 file embedded in the application
     */
    private void playMusic() {
        Toast.makeText(this, "Now playing: " + emisoras.get(CURRENT_EMISORA), Toast.LENGTH_LONG).show();
        if (detectSpeaker()) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }
            try {
                mMediaPlayer.setDataSource((String) emisoras.get(CURRENT_EMISORA));
                mMediaPlayer.prepare(); // might take long! (for buffering, etc)
                mMediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Stops the playback of the MP3 file.
     */
    private void stopMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Checks the permission that this app needs and if it has not been granted, it will
     * prompt the user to grant it, otherwise it shuts down the app.
     */
    private void checkPermissions() {
        boolean recordAudioPermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED;

        if (recordAudioPermissionGranted) {
            start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.WAKE_LOCK},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            } else {
                // Permission has been denied before. At this point we should show a dialog to
                // user and explain why this permission is needed and direct him to go to the
                // Permissions settings for the app in the System settings. For this sample, we
                // simply exit to get to the important part.
                Toast.makeText(this, R.string.exiting_for_permissions, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Starts the main flow of the application.
     */
    private void start() {
        int[] thumbResources = new int[]{R.id.play};
        ImageView[] thumbs = new ImageView[1];
        thumbs[0] = findViewById(thumbResources[0]);

        View containerView = findViewById(R.id.container);
        ImageView expandedView = findViewById(R.id.expanded);
        int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (speakerIsSupported()) {
            checkPermissions();
        } else {
            mOuterCircle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, R.string.no_speaker_supported,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onStop();
    }

    /**
     * Determines if the wear device has a built-in speaker and if it is supported. Speaker, even if
     * physically present, is only supported in Android M+ on a wear device..
     */
    public final boolean speakerIsSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager packageManager = getPackageManager();
            // The results from AudioManager.getDevices can't be trusted unless the device
            // advertises FEATURE_AUDIO_OUTPUT.
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean detectSpeaker() {
        Context context = this.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Check whether the device has a speaker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
                packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /**
         * Prepares the UI for ambient mode.
         */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);

            Log.d(TAG, "onEnterAmbient() " + ambientDetails);

            // Changes views to grey scale.
            Context context = getApplicationContext();
            Resources resources = context.getResources();

            mOuterCircle.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.light_grey));
            mInnerCircle.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.grey_circle));

            mProgressBar.setProgressTintList(
                    resources.getColorStateList(R.color.white, context.getTheme()));
            mProgressBar.setProgressBackgroundTintList(
                    resources.getColorStateList(R.color.black, context.getTheme()));
        }

        /**
         * Restores the UI to active (non-ambient) mode.
         */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();

            Log.d(TAG, "onExitAmbient()");

            // Changes views to color.
            Context context = getApplicationContext();
            Resources resources = context.getResources();

            mOuterCircle.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.background_color));
            mInnerCircle.setBackground(
                    ContextCompat.getDrawable(context, R.drawable.color_circle));

            mProgressBar.setProgressTintList(
                    resources.getColorStateList(R.color.progressbar_tint, context.getTheme()));
            mProgressBar.setProgressBackgroundTintList(
                    resources.getColorStateList(
                            R.color.progressbar_background_tint, context.getTheme()));
        }
    }
}
