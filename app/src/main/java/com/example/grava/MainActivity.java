package com.example.grava;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import static android.os.Environment.DIRECTORY_MOVIES;

public class MainActivity extends AppCompatActivity {
    private Button imageButton;
    private static final String LOG_TAG = "AudioRecordTest";
    private static final int CAST_PERMISSION_CODE = 22;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;
    private Surface mInputSurface;
    private final static String TAG = MainActivity.class.getSimpleName();
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder recorder = null;
    private boolean recording = false;
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private long pauseOffset;
    private Chronometer chronometer;
    private boolean running;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.activity_main);
        imageButton = findViewById(R.id.custom_button);
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("Tempo de Gravação: %s");
        chronometer.setBase(SystemClock.elapsedRealtime());
//        ActivityCompat.requestPermissions(this, new String[]{}, 1);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                recording = !recording;
                if (recording) {
                    imageButton.setBackgroundResource(R.drawable.recordp);
                    startChronometer();
                    startRecording();
                } else {
                    imageButton.setBackgroundResource(R.drawable.nonrecordp);
                    pauseChronometer();
                    stopRecording();
                    resetChronometer();
                }
            }
        });
    }

    public void startChronometer() {
        if (!running) {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            running = true;
        }
    }

    public void pauseChronometer() {
        if (running) {
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            running = false;
        }
    }

    public void resetChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void preparandoStarting() {
        // If mMediaProjection is null that means we didn't get a context, lets ask the user
        if (mMediaProjection == null) {
            // This asks for user permissions to capture the screen
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = getVirtualDisplay();
        recorder.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkPermission(){
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
            //File write logic here
            return true;
        }
        return false;
    }

    private void scanner(String path) {
        MediaScannerConnection.scanFile(this,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startRecording() {



        // Record to the external cache directory for visibility
        final File f = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES), "Gravação Audios");

        if (!f.exists()) {
            Log.d(TAG, "Folder doesn't exist, creating it...");
            boolean rv = f.mkdir();
            Log.d(TAG, "Folder creation " + (rv ? "success" : "failed"));
        } else {
            Log.d(TAG, "Folder already exists.");
        }

        Date currentTime = Calendar.getInstance().getTime();

        fileName = f.getAbsolutePath() + "/" + currentTime + ".mp4";

        recorder = new MediaRecorder();
        recorder.setPreviewDisplay(mInputSurface);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        recorder.setVideoSize(40, 40);
        recorder.setVideoFrameRate(30);
        recorder.setOutputFile(fileName);


        try {
            recorder.prepare();
            preparandoStarting();
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAST_PERMISSION_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied :(", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
        // TODO Register a callback that will listen onStop and release & prepare the recorder for next recording
        // mMediaProjection.registerCallback(callback, null);
        mVirtualDisplay = getVirtualDisplay();
        recorder.start();
    }

    private VirtualDisplay getVirtualDisplay() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenDensity = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        return mMediaProjection.createVirtualDisplay(this.getClass().getSimpleName(),
                width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
                recorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        mVirtualDisplay.release();
        scanner(fileName);
    }

    class RecordButton extends androidx.appcompat.widget.AppCompatButton {
        boolean mStartRecording = true;

        OnClickListener clicker = new OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void onClick(View v) {
                onRecord(mStartRecording);
                if (mStartRecording) {
                    setText("Stop recording");
                } else {
                    setText("Start recording");
                }
                mStartRecording = !mStartRecording;
            }
        };

        public RecordButton(Context ctx) {
            super(ctx);
            setText("Start recording");
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }
}