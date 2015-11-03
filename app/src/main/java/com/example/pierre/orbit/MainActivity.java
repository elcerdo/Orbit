package com.example.pierre.orbit;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MainActivity extends Activity {
    private MainRenderer mainRenderer;
    private MainSurfaceView mainSurfaceView;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("Orbit", "create activity");
        ActivityManager activityManager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = activityManager.getDeviceConfigurationInfo();
        if (info.reqGlEsVersion < 0x20000) {
            Log.e("Orbit", "your device doesn't support opengl es 2");
            return;
        }

        mainSurfaceView = new MainSurfaceView(this);
        mainSurfaceView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mainSurfaceView.setEGLContextClientVersion(2);
        this.setContentView(mainSurfaceView);

        mainRenderer = new MainRenderer(this);
        mainSurfaceView.setRenderer(mainRenderer);

        gestureDetector = new GestureDetector(this, mainRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        return gestureDetector.onTouchEvent(evt);
    }

}
