package com.parrot.cyclops;

import com.parrot.cyclops.R;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class CameraLayout extends RelativeLayout implements CameraView.OnCameraStateListener {

    public interface OnDoubleClickListener {
        public void onExitFullScreen(CameraLayout camLayout);
    }

    private static final String TAG = "CameraLayout";
    private static final boolean DEBUG = true;

    private Context mContext = null;

    private int mCameraId = Cyclops.REAR_CAMERA_ID;
    private CameraView mCameraView = null;
    private TextView mTextInfo = null;

    private OnDoubleClickListener mDoubleClickListener = null;
    private int mPreviewOrientation = Cyclops.PreviewOrientation.TOP;
    private long mLastTouchTime = -1;
    private boolean mFullScreen = false;
    private int mLeftMargin;
    private int mTopMargin;
    private int mRightMargin;
    private int mBottomMargin;

    public CameraLayout(Context context) {
        super(context);
        initCameraLayout(context);
    }

    public CameraLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCameraLayout(context);
    }

    public CameraLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCameraLayout(context);
    }

    private void initCameraLayout(Context context) {
        if (DEBUG) Log.d(TAG, "initCameraLayout");
        mContext = context;
        setBackgroundColor(Color.BLUE);

        {
            mCameraView = new CameraView(mContext);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            mCameraView.setLayoutParams(params);
            addView(mCameraView);
        }

        {
            mTextInfo = new TextView(mContext);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            mTextInfo.setLayoutParams(params);
            mTextInfo.setTextColor(Color.parseColor("#33b5e5"));
            mTextInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            //mTextInfo.setPadding(0, 0, 60, 0); TODO remove if it's OK
            mTextInfo.setText("");
            mTextInfo.setVisibility(View.GONE);
            addView(mTextInfo);
        }

        mCameraView.setCameraStateListener(this);
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
        mCameraView.setCameraId(cameraId);
    }

//    public void setOnDobleClickListener(OnDoubleClickListener listener) {
//        mDoubleClickListener = listener;
//    }
//
//    public void setPreviewOrientation(int orientation) {
//        mPreviewOrientation = orientation;
//        switch (mPreviewOrientation) {
//            case PreviewOrientation.TOP:
//                mLeftMargin = 250;
//                mTopMargin = 60;
//                mRightMargin = 0;
//                mBottomMargin = 0;
//                break;
//            case PreviewOrientation.BOTTOM:
//                mLeftMargin = 250;
//                mTopMargin = 60 + 160 + 60;
//                mRightMargin = 0;
//                mBottomMargin = 0;
//                break;
//            default:
//                break;
//        }
//    }

    public void setFullScreen(boolean enable) {
        mFullScreen = enable;
        if (mFullScreen) {
            mTextInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            //mTextInfo.setPadding(0, 0, 0, 0);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            setLayoutParams(params);
        } else {
            mTextInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            //mTextInfo.setPadding(0, 0, 60, 0);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(300, 160);
            params.setMargins(mLeftMargin, mTopMargin, mRightMargin, mBottomMargin);
            setLayoutParams(params);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mCameraView.setVisibility(visibility);
        mTextInfo.setVisibility(visibility);
    }

    public int setCameraDisplay(int displayId) {
        return mCameraView.setCameraDisplay(displayId);
    }

    public int getCameraDisplay() {
        return mCameraView.getCameraDisplay();
    }

    public void start() {
        mCameraView.start();
    }

    public void stop(boolean force) {
        mCameraView.stop(force);
    }

    public void setCameraMirroring(boolean mirrored) { mCameraView.setCameraMirroring(mirrored);}
    public void setCameraRotationBy180(boolean rotatedBy180) { mCameraView.setCameraRotationBy180(rotatedBy180); }

    public void takePicture() { mCameraView.takePicture(); }

    public Uri getLastUriOfTakenPicture() { return mCameraView.getLastUriOfTakenPicture(); }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        logdebug("onTouchEvent");
        if (mFullScreen) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                long thisTime = System.currentTimeMillis();
                if (thisTime - mLastTouchTime < 500) {
                    if (DEBUG) Log.d(TAG, "exit from full screen");
                    if (mDoubleClickListener != null) {
                        mDoubleClickListener.onExitFullScreen(this);
                    }
                    mLastTouchTime = -1;
                    return true;
                } else {
                    mLastTouchTime = thisTime;
                }
            }
            ViewParent parent =this.getParent();
            if (parent != null) {
                ViewGroup vg = (ViewGroup) parent;
                return vg.onTouchEvent(ev);
            }
        }
        return super.onTouchEvent(ev);
    }

    private void logdebug(String msg) {
        if (DEBUG) Log.d(TAG, "camId=" + mCameraId + ", " + msg);
    }

    // ******************* CameraView.OnCameraStateListener implementation ****************

    @Override
    public void onOpen(boolean opened) {
        logdebug("onOpen opened=" + opened);
        if (opened) {
            mTextInfo.setText("");
            mTextInfo.setVisibility(View.GONE);
            // TODO enable panel control
        } else {
            if (SystemProperties.isTvBusyByOtherApp())
                mTextInfo.setText(R.string.camera_is_not_opened_or_no_overlay);
            else
                mTextInfo.setText(R.string.camera_is_not_opened);
            mTextInfo.setVisibility(View.VISIBLE);
            // TODO disable panel control
        }
    }

    @Override
    public void onUpdateDisplay(int displayId) {
        logdebug("onUpdateDisplay displayId=" + displayId);
        if (displayId == Cyclops.DISPLAY_TYPE_HDMI_TV) {
            mTextInfo.setText(R.string.camera_is_on_ext_display);
            mTextInfo.setVisibility(View.VISIBLE);
        } else {
            mTextInfo.setText("");
            mTextInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClose() {
        logdebug("onClose");
    }

    @Override
    public void onSyncFound() {
        logdebug("onSyncFound");
        if (mCameraId == Cyclops.REAR_CAMERA_ID) {
            mTextInfo.setText("");
            mTextInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSyncLost() {
        logdebug("onSyncLost");
        if (mCameraId == Cyclops.REAR_CAMERA_ID) {
            mTextInfo.setText(R.string.camera_no_signal);
            mTextInfo.setVisibility(View.VISIBLE);
        }
    }

}
