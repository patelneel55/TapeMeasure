package com.tapemeasure;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */

    //for accelerometer values
    TextView outputX;
    TextView outputY;
    SensorManager sensorManager = null;
    StringBuilder builder = new StringBuilder();

    float [] history = null;
    float [] velocity = null;
    float [] position = null;
    String [] direction = {"NONE","NONE"};
    static final float NS2S = 1.0f / 1000000000.0f;
    long last_Time = 0;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        setContentView(R.layout.activity_main);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);

        Sensor accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);


        //just some textviews, for data output
        outputX = (TextView) findViewById(R.id.outputX);
        outputY = (TextView) findViewById(R.id.outputY);


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }


    private static final boolean ADAPTIVE_ACCEL_FILTER = true;
    float lastAccel[] = new float[3];
    float accelFilter[] = new float[3];

    @Override
    public void onSensorChanged(SensorEvent event) {

        final float alpha = 0.8f;
        float []  gravity = new float[]{0f,0f,0f};
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
        history = new float[3];

        history[0] = event.values[0]*0.8f+history[0]*(1.0f-0.8f);
        history[1] = event.values[1]*0.8f+history[1]*(1.0f-0.8f);
        history[2] = event.values[2]*0.8f+history[2]*(1.0f-0.8f);

        history[0] = event.values[0] - history[0];
        history[1] = event.values[1] - history[1];
        history[2] = event.values[2] - history[2];

        // high pass filter
        float updateFreq = 30; // match this to your update speed
        float cutOffFreq = 0.9f;
        float RC = 1.0f / cutOffFreq;
        float dt = 1.0f / updateFreq;
        float filterConstant = RC / (dt + RC);
        float a = filterConstant;
        float kAccelerometerMinStep = 0.033f;
        float kAccelerometerNoiseAttenuation = 3.0f;

        if(ADAPTIVE_ACCEL_FILTER)
        {
            float d = clamp(Math.abs(norm(accelFilter[0], accelFilter[1], accelFilter[2]) - norm(event.values[0], event.values[1], event.values[2])) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
            a = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;
        }

        accelFilter[0] = a * (accelFilter[0] + event.values[0] - lastAccel[0]);
        accelFilter[1] = a * (accelFilter[1] + event.values[1] - lastAccel[1]);
        accelFilter[2] = a * (accelFilter[2] + event.values[2] - lastAccel[2]);

        lastAccel[0] = event.values[0];
        lastAccel[1] = event.values[1];
        lastAccel[2] = event.values[2];

        /*if(history != null)
        {
            float deltaTime = (event.timestamp - last_Time) * NS2S;

            for(int i = 0;i<3;i++)
            {
                velocity[i] += (((int)(event.values[i]-gravity[i])+history[i])/2 * deltaTime);
                position[i] += velocity[i]*deltaTime;
            }
        }
        else
        {
            history = new float[3];
            velocity = new float[3];//{event.values[0], event.values[1], event.values[2]};
            position = new float[3];
            velocity[0] = velocity[1] = velocity[2] = 0f;
            position[0] = position[1] = position[2] = 0f;
        }
        System.arraycopy(velocity, 0, history, 0, 3);
        last_Time = event.timestamp;*/
        //outputX.setText("x, y, z: "+position[0]+", "+position[1]+", "+position[2]);
        outputX.setText("x, y, z: "+(int)accelFilter[0]+", "+(int)accelFilter[1]+", "+(int)accelFilter[2]);
    }
    
    float clamp(float value, float min, float max)
    {
        if(value < min)return min;
        else if(value > max)return max;
        return value;
    }
    float norm(float a, float b, float c)
    {
        return (float) Math.sqrt(a*a+b*b+c*c);
    }
}
