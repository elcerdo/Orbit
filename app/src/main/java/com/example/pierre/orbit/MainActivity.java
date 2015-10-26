package com.example.pierre.orbit;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity implements View.OnTouchListener {
    private MainRenderer mainRenderer;
    private MainSurfaceView mainSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Orbit", "creating activity.");
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
        if (info.reqGlEsVersion < 0x20000) {
            Log.e("Orbit", "your device doesn't support opengl es 2.");
            return;
        }

        mainRenderer = new MainRenderer();
        mainSurfaceView = new MainSurfaceView(this);

        mainSurfaceView.setEGLContextClientVersion(2);
        mainSurfaceView.setRenderer(mainRenderer);
        mainSurfaceView.setOnTouchListener(this);
        this.setContentView(mainSurfaceView);
    }

    @Override
    public boolean onTouch(View view, MotionEvent evt) {
        Log.i("Orbit", "prout");
        mainRenderer.counter ++;
        return false;
    }

}
