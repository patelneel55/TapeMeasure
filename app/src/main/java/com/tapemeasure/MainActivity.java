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
import android.widget.Button;
import android.widget.TextView;

import static android.R.attr.gravity;


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

    //Velocity Integral
    float [] integralV = new float[3];
    //Distance Integral
    float [] integralD = new float[3];
    //History for Velocity
    float[] historyV = new float[3];

    float [] history = new float[3];
    float [] cache = null;
    float [] velocity = null;
    float [] position = null;
    private static final boolean ADAPTIVE_ACCEL_FILTER = true;
    float accelFilter[] = new float[3];
    String [] direction = {"NONE","NONE"};
    static final float NS2S = 1.0f / 1000000000.0f;
    long last_Time = 0;
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    public boolean start = true;

    private final float NOISE = (float)0.05;
    private boolean mInitialized = false;

    private float mLastX, mLastY, mLastZ;
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

        //just some textviews, for data output
        outputX = (TextView) findViewById(R.id.outputX);
        outputY = (TextView) findViewById(R.id.outputY);

        Button start_btn = (Button) findViewById(R.id.start_btn);
        Button reset_btn = (Button) findViewById(R.id.reset_btn);

        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start)
                {
                    Button btn = (Button) findViewById(R.id.start_btn);
                    outputY.setVisibility(View.VISIBLE);
                    outputX.setVisibility(View.VISIBLE);
                    startSensors();
                    btn.setText("Stop");
                    start = false;
                }
                else
                {
                    Button btn = (Button) findViewById(R.id.start_btn);
                    outputY.setVisibility(View.VISIBLE);
                    outputX.setVisibility(View.VISIBLE);
                    stopSensors();
                    btn.setText("Start");
                    start = true;
                }
            }
        });

        reset_btn.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v)
          {
              Button btn = (Button) findViewById(R.id.start_btn);
              stopSensors();
              btn.setText("Start");
              start = true;

              //Velocity Integral
              integralV = new float[3];
              //Distance Integral
              integralD = new float[3];
              //History for Velocity
              historyV = new float[3];

              history = new float[3];
              cache = new float[3];
              velocity = new float[3];
              position = new float[3];

              outputY.setText("xDis " + integralD[0] + " | yDis" + integralD[1] + " | zDis " + integralD[2]);
              outputX.setText("xAcc, yAcc, zAcc: "+0+", "+0+", "+0+"\n"+"xVel, yVel, zVel: "+velocity[0]+", "+velocity[1]+", "+velocity[2]+"\n"+"xPos, yPos, zPos: "+position[0]+", "+position[1]+", "+position[2]);
          }
        });
    }

    public void stopSensors()
    {
        sensorManager.unregisterListener(this);
    }
    public void startSensors()
    {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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

    @Override
    public void onSensorChanged(SensorEvent event) {


        //highPassRampingFilter(event.values[0], event.values[1], event.values[2]);
        //highPassFilter(event.values[0], event.values[1], event.values[2]);


        float x = Math.abs(event.values[0]);
        float y = Math.abs(event.values[1]);
        float z = Math.abs(event.values[2]);

        highPassRampingFilter(x, y, z);
        float deltaTime = (event.timestamp - last_Time)*NS2S;

        if (!mInitialized) {
            mLastX = x;
            mLastY = y;
            mLastZ = z;
            velocity = new float[] {0f, 0f, 0f};
            position = new float[] {0f, 0f, 0f};
            historyV[0] = 0;
            historyV[1] = 0;
            historyV[2] = 0;
            outputX.setText("xAcc, yAcc, zAcc: 0,0,0");
            mInitialized = true;
        } else {
            float deltaX = x-mLastX;
            float deltaY = y-mLastY;
            float deltaZ = z-mLastZ;
            if (Math.abs(deltaX) < NOISE) deltaX = (float) 0.0;
            if (Math.abs(deltaY) < NOISE) deltaY = (float) 0.0;
            if (Math.abs(deltaZ) < NOISE) deltaZ = (float) 0.0;

            velocity[0] = ( deltaX* deltaTime);
            velocity[1] = ( deltaY* deltaTime);
            velocity[2] = ( deltaZ* deltaTime);
            position[0] += velocity[0] * deltaTime;
            position[1] += velocity[1] * deltaTime;
            position[2] += velocity[2] * deltaTime;


            //Riemonns Sum Test Code

                //X
                integralV[0] += trapArea(0, deltaX, deltaTime);

                if(integralV[0] < 0 || deltaX == 0)integralV[0] = 0;
                integralD[0] += trapArea(historyV[0], integralV[0], deltaTime);
                historyV[0] = integralV[0];
                //integralD[0] += (integralV[0] *deltaTime);
                System.out.println("SensAccel: " + x + " FilterAccel: " + accelFilter[0] + " deltaX: " + deltaX + " Velocity: "+ integralV[0]+" Distance: "+ integralD[0]);

                //Y
                integralV[1] += trapArea(0, deltaY, deltaTime);
                historyV[1] = integralV[1];
                integralD[1] += trapArea(historyV[1],integralV[1], deltaTime);

                //Z
                integralV[2] += trapArea(0, deltaZ, deltaTime);
                historyV[2] = integralV[2];
                integralD[2] += trapArea(historyV[2], integralV[2], deltaTime);

            /*else if(deltaX == 0.0f || deltaY == 0.0f || deltaZ == 0.0f) {
                historyV[0] = historyV[1] = historyV[2] = 0;
            }*/

            outputY.setText("xDis " + integralD[0] + " | yDis" + integralD[1] + " | zDis " + integralD[2]);
            outputX.setText("xAcc, yAcc, zAcc: "+deltaX+", "+deltaY+", "+deltaZ+"\n"+"xVel, yVel, zVel: "+velocity[0]+", "+velocity[1]+", "+velocity[2]+"\n"+"xPos, yPos, zPos: "+position[0]+", "+position[1]+", "+position[2]);
        }

        last_Time = event.timestamp;
        mLastX = x;
        mLastY = y;
        mLastZ = z;
        /*
        if(cache != null)
        {

            outputY.setText(event.timestamp+"\n"+last_Time+"\n"+deltaTime+"");

                for (int i = 0; i < 3; i++) {
                    velocity[i] = (event.values[i]+cache[i])/2 * deltaTime;
                    position[i] += velocity[i] * deltaTime; // Position in meters
                }
        }
        else
        {
            cache = new float[3];
            velocity = new float[3];//{event.values[0], event.values[1], event.values[2]};
            position = new float[3];
            velocity[0] = velocity[1] = velocity[2] = 0f;
            position[0] = position[1] = position[2] = 0f;
        }
        System.arraycopy(event.values, 0, cache, 0, 3);
        last_Time = event.timestamp;

        int distance = (int)Math.sqrt(position[0]*position[0]+position[1]*position[1]+position[2]*position[2])*100;
        //outputX.setText("x, y, z: "+position[0]+", "+position[1]+", "+position[2]);
        outputX.setText("xAcc, yAcc, zAcc: "+(int)event.values[0]+", "+(int)accelFilter[1]+", "+(int)accelFilter[2]+"\n"+"xVel, yVel, zVel: "+(int)velocity[0]+", "+(int)velocity[1]+", "+(int)velocity[2]+"\n"+"xPos, yPos, zPos: "+(int)position[0]+", "+(int)position[1]+", "+(int)position[2]+"\nDistance: "+distance);
        */
    }

    private void highPassFilter(float aX, float aY, float aZ)
    {
        float updateFreq = 30; // match this to your update speed
        float cutOffFreq = 0.9f;
        float RC = 1.0f / cutOffFreq;
        float dt = 1.0f / updateFreq;
        float filterConstant = RC / (dt + RC);
        float alpha = filterConstant;
        float kAccelerometerMinStep = 0.033f;
        float kAccelerometerNoiseAttenuation = 3.0f;

        if(ADAPTIVE_ACCEL_FILTER)
        {
            float d = clamp(Math.abs(norm(accelFilter[0], accelFilter[1], accelFilter[2]) - norm(aX, aY, aZ)) / kAccelerometerMinStep - 1.0f, 0.0f, 1.0f);
            alpha = d * filterConstant / kAccelerometerNoiseAttenuation + (1.0f - d) * filterConstant;
        }

        accelFilter[0] = (float) (alpha * (accelFilter[0] + aX - history[0]));
        accelFilter[1] = (float) (alpha * (accelFilter[1] + aY - history[1]));
        accelFilter[2] = (float) (alpha * (accelFilter[2] + aZ - history[2]));

        history[0] = aX;
        history[1] = aY;
        history[2] = aZ;

    }

    private void highPassRampingFilter(float aX, float aY, float aZ)
    {
        final float filterFactor = 0.9f;
        accelFilter[0] = aX*filterFactor+accelFilter[0]*(1.0f-filterFactor);
        accelFilter[1] = aY*filterFactor+accelFilter[1]*(1.0f-filterFactor);
        accelFilter[2] = aZ*filterFactor+accelFilter[2]*(1.0f-filterFactor);

        accelFilter[0] = aX - accelFilter[0];
        accelFilter[1] = aY - accelFilter[1];
        accelFilter[2] = aZ - accelFilter[2];
    }

    private void lowPassFilter(float aX, float aY, float aZ)
    {
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

    float trapArea(float past, float current, float dT)
    {
        return 0.5f*dT*(past+current);
    }

    float rou(float n)
    {
        return Math.round(n*100)/100;
    }
}
