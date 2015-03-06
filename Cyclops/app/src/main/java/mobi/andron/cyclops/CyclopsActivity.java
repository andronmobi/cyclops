package mobi.andron.cyclops;

import mobi.andron.cyclops.Cyclops.PreviewOrientation;
import mobi.andron.cyclops.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class CyclopsActivity extends Activity implements CameraPanel.Listener {

    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final boolean TOGGLE_ON_CLICK = true;
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    private static final String TAG = "CyclopsActivity";
    private static final boolean DEBUG = true;

    private SystemUiHider mSystemUiHider;
    private CameraLayout mCameraLayout;
    private View mControlsView;
    private CameraPanel mCameraPanel;
    private int mCurrentCameraId = -1;

    private static final String EXTRA_REARGEAR = "com.parrot.reargear.status";
    private static final String ACTION_REARGEAR = "com.parrot.reargear";

    private static final int REQUEST_SETTINGS_CODE = 1;

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
        mCameraPanel = new CameraPanel((ViewGroup) mControlsView);

        // Start CyclopsService which handles camera commands from the activity
        startService(new Intent(this, CyclopsService.class));
    }

    void updateCameraSettings() {
        // Check shared prefs to obtain default settings for camera
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mCurrentCameraId = Integer.parseInt(sharedPrefs.getString("camera_type",
                Integer.toString(Cyclops.REAR_CAMERA_ID)));
    }

    void createCameraLayout() {
        mCameraLayout = new CameraLayout(CyclopsActivity.this);
        mCameraLayout.setTag(mCurrentCameraId);
        mCameraLayout.setCameraId(mCurrentCameraId);
        //mCameraLayout.setPreviewOrientation((camId == 0) ? PreviewOrientation.BOTTOM : PreviewOrientation.TOP);
        mCameraLayout.setFullScreen(true);
        /*mCameraLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraLayout camLayout = (CameraLayout) v;
                if (!mFullScreen) {
                    mCameraPanel.setListener(CyclopsActivity.this, camLayout);
                    mFullScreen = true;
                    setFullScreen(camLayout);
                }
            }
        });
        mCameraLayout.setOnDobleClickListener(this);*/
        mContentView.addView(mCameraLayout);
        mCameraPanel.setListener(CyclopsActivity.this, mCameraLayout);
    }

    void removeCameraLayout() {
        if (mCameraLayout != null) {
            mContentView.removeView(mCameraLayout);
            mCameraLayout.stop(true);
            mCameraLayout = null;
        }
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
        boolean camOnTv = false;
        if (SystemProperties.isTvBusyByCyclops()) {
            SystemProperties.setTvBusyByOtherApp(false);
            camOnTv = true;
        } else {
            boolean busy = SystemProperties.isOverlay0OnTV();
            SystemProperties.setTvBusyByOtherApp(busy);
        }
        registerReceiver(mReceiver, mFilter);
        int camId = mCurrentCameraId;
        updateCameraSettings();
        // Check that type of camera used by default is changed and
        // it's not currently on TV (don't use two overlays with two cameras)
        if (camId != mCurrentCameraId && !camOnTv) {
            removeCameraLayout();
            createCameraLayout();
        }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                i.putExtra(Cyclops.CAMERA_ID, mCurrentCameraId);
                startActivityForResult(i, REQUEST_SETTINGS_CODE);
                break;
        }
        return true;
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

    private boolean isTvDisplayBusy(CameraLayout camLayout) {
        if (SystemProperties.isOverlay0OnTV() || mCameraLayout.getCameraDisplay() == Cyclops.DISPLAY_TYPE_HDMI_TV)
            return true;
        return false;
    }

    // ******************* CameraPanel.Listener implementation ****************

    @Override
    public void onSwitchDisplay(CameraLayout camLayout) {
        int display = camLayout.getCameraDisplay();
        switch (display) {
            case Cyclops.DISPLAY_TYPE_LCD_PRIMARY:
                if (isTvDisplayBusy(camLayout)) {
                    Toast toast = Toast.makeText(this, R.string.toast_tv_busy, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    camLayout.setCameraDisplay(Cyclops.DISPLAY_TYPE_HDMI_TV);
                }
                break;
            case Cyclops.DISPLAY_TYPE_HDMI_TV:
                camLayout.setCameraDisplay(Cyclops.DISPLAY_TYPE_LCD_PRIMARY);
                break;
            case Cyclops.DISPLAY_TYPE_UNDEFINED:
                Log.e(TAG, "Camera display is undefined");
                break;
        }
    }

    @Override
    public void onTakePicture(CameraLayout camLayout) {
        Log.d(TAG, "onTakePicture");
        camLayout.takePicture();
    }

    @Override
    public void onOpenImageFolder(CameraLayout camLayout) {
        Log.d(TAG, "onOpenImageFolder");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.cooliris.media", "com.cooliris.media.Gallery"));
        Uri uri = camLayout.getLastUriOfTakenPicture();
        if (uri != null) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
        } else {
            uri = Uri.parse(Cyclops.CYCLOPS_DIR);
            intent.setDataAndType(uri , "image/*");
        }
        startActivity(intent);
    }

    // ******************* SystemUiHider implementation ***********************

    private void initSystemUiHider() {
        mControlsView = findViewById(R.id.fullscreen_content_controls);
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
                                mControlsHeight = mControlsView.getHeight();
                            }
                            if (mShortAnimTime == 0) {
                                mShortAnimTime = getResources().getInteger(
                                        android.R.integer.config_shortAnimTime);
                            }
                            mControlsView.animate()
                                    .translationY(visible ? 0 : mControlsHeight)
                                    .setDuration(mShortAnimTime);
                        } else {
                            // If the ViewPropertyAnimator APIs aren't
                            // available, simply show or hide the in-layout UI
                            // controls.
                            mControlsView.setVisibility(visible ? View.VISIBLE : View.GONE);
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
