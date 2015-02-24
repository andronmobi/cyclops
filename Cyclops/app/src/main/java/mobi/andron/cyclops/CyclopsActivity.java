package mobi.andron.cyclops;

import mobi.andron.cyclops.Cyclops.PreviewOrientation;
import mobi.andron.cyclops.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;


public class CyclopsActivity extends Activity {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final boolean TOGGLE_ON_CLICK = true;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private static final String TAG = "CyclopsActivity";
    private static final boolean DEBUG = true;

    private SystemUiHider mSystemUiHider;
    private CameraLayout mCameraLayout;

    private static final String EXTRA_REARGEAR = "com.parrot.reargear.status";
    private static final String ACTION_REARGEAR = "com.parrot.reargear";

    private boolean mLaunchedByGearStick;
    private boolean mIsStarted = false;
    private boolean mIsRestarted = false;
    private IntentFilter mFilter = new IntentFilter(ACTION_REARGEAR);

    private RelativeLayout mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean busy = SystemProperties.isOverlay0OnTV();
        SystemProperties.setTvBusyByOtherApp(busy);

        mIsRestarted = false;
        mLaunchedByGearStick = isLaunchedByGearStick(getIntent());
        if (DEBUG) Log.d(TAG, "onCreate by GearStick=" + mLaunchedByGearStick);

        setContentView(R.layout.activity_cyclops);
        initSystemUiHider();

        // Start CyclopsService which handles camera commands from the activity
        startService(new Intent(this, CyclopsService.class));

        int camId = 0;
        mCameraLayout = new CameraLayout(CyclopsActivity.this);
        mCameraLayout.setTag(camId);
        mCameraLayout.setCameraId((camId == 0) ? Cyclops.REAR_CAMERA_ID : Cyclops.USB_CAMERA_ID);
        //mCameraLayout.setPreviewOrientation((camId == 0) ? PreviewOrientation.BOTTOM : PreviewOrientation.TOP);
        mCameraLayout.setFullScreen(true);
//        mCameraLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                CameraLayout camLayout = (CameraLayout) v;
//                if (!mFullScreen) {
//                    mCameraPanel.setListener(CyclopsActivity.this, camLayout);
//                    mFullScreen = true;
//                    setFullScreen(camLayout);
//                }
//            }
//        });
//        mCameraLayout.setOnDobleClickListener(this);
        mContentView.addView(mCameraLayout);

        // TODO delete
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
//        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    /**
     * Called if the activity is re-launched and it was not been destroyed.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        /* An activity will always be paused before receiving a new intent,
         so you can count on onResume() being called after this method.
        */
        if (DEBUG) Log.d(TAG, "onNewIntent, mIsStarted = " + mIsStarted);
        if (mIsStarted)
            return; // don't update mLaunchedByGearStick, since the activity is already started

        mLaunchedByGearStick = isLaunchedByGearStick(intent);
        if (mLaunchedByGearStick) {
            // Always start with analog camera
            //mCurrentCameraId = Cyclops.REAR_CAMERA_ID;
        }
        if (DEBUG) Log.d(TAG, "The activity is re-launched by " + (mLaunchedByGearStick ? "Gear stick" : "LAUNCHER"));
    }

    private boolean isLaunchedByGearStick(Intent intent) {
        String action = intent.getAction();
        int flags = intent.getFlags();
        if (Intent.ACTION_MAIN.equals(action) ||
                (flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return false; // don't update mLaunchedByGearStick, since the activity is resumed from launcher
        }
        return intent.getBooleanExtra(EXTRA_REARGEAR, false);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isRearGear = intent.getBooleanExtra(EXTRA_REARGEAR, false);

            if (DEBUG) Log.d(TAG, "com.parrot.reargear intent is received, on = " + isRearGear);
            if (mLaunchedByGearStick && !isRearGear) {
                if (mIsRestarted)
                    moveTaskToBack(true); // Go background
                else
                    finish();
            }
        }
    };

    @Override
    protected void onRestart() {
        super.onRestart();
        mIsRestarted = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (DEBUG) Log.d(TAG, "onStart");
        mIsStarted = true;
        if (SystemProperties.isTvBusyByCyclops()) {
            SystemProperties.setTvBusyByOtherApp(false);
        } else {
            boolean busy = SystemProperties.isOverlay0OnTV();
            SystemProperties.setTvBusyByOtherApp(busy);
        }
        registerReceiver(mReceiver, mFilter);
        mCameraLayout.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(DEBUG)Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DEBUG)Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (DEBUG) Log.d(TAG, "onStop");
        mIsStarted = false;
        SystemProperties.setTvBusyByCyclops(false); // Value will by updated by CameraView if it's on TV
        unregisterReceiver(mReceiver);
        boolean force = isFinishing();
        mCameraLayout.stop(force);
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        //stopService(new Intent(this, CyclopsService.class));
    }

    // ******************* SystemUiHider implementation ***********************

    private void initSystemUiHider() {
        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = (RelativeLayout) findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, mContentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                            // If the ViewPropertyAnimator API is available
                            // (Honeycomb MR2 and later), use it to animate the
                            // in-layout UI controls at the bottom of the
                            // screen.
                            if (mControlsHeight == 0) {
                                mControlsHeight = controlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            controlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            controlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
                        }

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
