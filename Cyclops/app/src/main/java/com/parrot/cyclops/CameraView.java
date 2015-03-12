/**
 * Copyright (C) 2013-2015 Parrot
 *
 * authors: Andrei Mandychev <andrei.mandychev@parrot.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

package com.parrot.cyclops;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewParent;

public class CameraView extends SurfaceView implements CameraCtrl.SyncCallback {

    public interface OnCameraStateListener extends CameraCtrl.SyncCallback {
        public void onOpen(boolean opened);
        public void onUpdateDisplay(int displayId);
        public void onClose();
    }

    private static final String TAG = "CameraView";
    private static final boolean DEBUG = true;

    private int mCameraId = Cyclops.REAR_CAMERA_ID;
    private Context mContext = null;
    private boolean mBound = false;
    private CyclopsService mCyclopsService = null;
    private CameraCtrl mCameraCtrl = null;
    private SurfaceHolder mSurfaceHolder = null; // Don't delete

    private OnCameraStateListener mCameraStateListener = null;

    private Method mSetTitleMethod = null;
    private boolean mExtDisplay = false;

    private boolean mIsCameraMirrored = false;
    private boolean mIsCameraRotatedBy180 = false;

    public CameraView(Context context) {
        super(context);
        initCameraView(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCameraView(context);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initCameraView(context);
    }

    private void initCameraView(Context context) {
        if (DEBUG) Log.d(TAG, "initCameraView");
        mContext = context;
        getHolder().addCallback(mSHCallback);
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Use reflection to call a hidden method of SurfaceView added by Parrot.
        Class<?> params[] = new Class[1];
        params[0] = String.class;
        try {
            mSetTitleMethod = getClass().getSuperclass().getDeclaredMethod("setTitle", params);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, e.toString());
            mSetTitleMethod = null;
        }
    }

    public void setCameraId(int cameraId) {
        mCameraId = cameraId;
    }

    public void setCameraStateListener(OnCameraStateListener listener) {
        mCameraStateListener = listener;
    }

    public int setCameraDisplay(int displayId) {
        mExtDisplay = (displayId == Cyclops.DISPLAY_TYPE_HDMI_TV) ? true : false;
        setVoutVideoView(mExtDisplay);
        int res = mCameraCtrl.setCameraDisplayId(displayId);
        if (mCameraStateListener != null && res == 0) {
            mCameraStateListener.onUpdateDisplay(displayId);
        }
        return res;
    }

    public int getCameraDisplay() {
        return mCameraCtrl.getCameraDisplayId();
    }

    public void start() {
        logdebug("start");
        // Bind to CyclopsService
        Intent intent = new Intent(mContext, CyclopsService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void stop(boolean force) {
        logdebug("stop");
        if (mBound) {
            if (force) {
                if (mCameraCtrl.isCameraOpened()) {
                    mCameraCtrl.setSyncCallback(null);
                    mCameraCtrl.closeCamera();
                    if (mCameraStateListener != null) {
                        mCameraStateListener.onClose();
                    }
                }
            } else {
                if (mCameraCtrl.getCameraDisplayId() == Cyclops.DISPLAY_TYPE_HDMI_TV) {
                    startCyclopsServiceForeground();
                }
            }
            mBound = false;
            mContext.unbindService(mConnection);
        }
    }

    public void setCameraMirroring(boolean mirrored) { mIsCameraMirrored = mirrored; }
    public void setCameraRotationBy180(boolean rotatedBy180) { mIsCameraRotatedBy180 = rotatedBy180; }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        logdebug("onTouchEvent");
        ViewParent parent =this.getParent();
        if (parent != null) {
            ViewGroup vg = (ViewGroup) parent;
            return vg.onTouchEvent(ev);
        }
        return super.onTouchEvent(ev);
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            logdebug("surfaceChanged");
        }

        public void surfaceCreated(SurfaceHolder holder) {
            logdebug("surfaceCreated");
            mSurfaceHolder = holder;
            //setBackgroundDrawable(getResources().getDrawable(R.drawable.bg));
            if (mCyclopsService == null) {
                return;
            }
            mCameraCtrl = mCyclopsService.obtainCameraCntrl(mCameraId);
            mCameraCtrl.setSyncCallback(CameraView.this);
            if (mCameraCtrl.isCameraOpened()) {
                logdebug("Camera is already opened, resume from background");
            } else {
                boolean opened = mCameraCtrl.openCamera(holder, mIsCameraMirrored, mIsCameraRotatedBy180);
                if (mCameraStateListener != null) {
                    mCameraStateListener.onOpen(opened);
                }
                if (opened) {
                    int displayId = getCameraDisplay();
                    mExtDisplay = (displayId == Cyclops.DISPLAY_TYPE_HDMI_TV) ? true : false;
                    // Set a title for SurfaceView if the camera was started on TV
                    // from the beginning and don't have updated value
                    setVoutVideoView(mExtDisplay);
                    if (mCameraStateListener != null) {
                        mCameraStateListener.onUpdateDisplay(displayId);
                    }
                }
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            logdebug("surfaceDestroyed");
            if (mCyclopsService == null) {
                // If mCyclopsService is null it means that we didn't bind with 
                // CyclopsService and we didn't do anything with a camera
                // so we just return without any actions.
                return;
            }
            if (mExtDisplay) {
                logdebug("Camera is not closed, keep in background");
            } else {
                mCameraCtrl.setSyncCallback(null);
                mCameraCtrl.closeCamera();
                if (mCameraStateListener != null) {
                    mCameraStateListener.onClose();
                }
            }
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            logdebug("onServiceConnected");
            CyclopsService.CyclopsBinder binder = (CyclopsService.CyclopsBinder) service;
            mCyclopsService = binder.getService();
            mBound = true;
            if (mCameraCtrl != null) {
                if (mCameraCtrl.isCameraOpened()) {
                    logdebug("stopForeground");
                    mCyclopsService.stopForeground(true);
                    // Update mirroring and rotation for camera which is on TV
                    if (mIsCameraMirrored == mCameraCtrl.isCameraMirrored() ||
                        mIsCameraRotatedBy180 == mCameraCtrl.isCameraRotatedBy180()) {
                        mCameraCtrl.setCameraTransformation(mIsCameraMirrored, mIsCameraRotatedBy180);
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            logdebug("onServiceDisconnected");
            mCyclopsService = null;
        }
    };

    private void startCyclopsServiceForeground() {
        logdebug("startCyclopsServiceForeground");
        SystemProperties.setTvBusyByCyclops(true);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(mContext.getString(R.string.notif_title))
                        .setContentText(mContext.getString(R.string.notif_text));
        PendingIntent pendingIntent;
        Intent intent = new Intent(mContext, CyclopsActivity.class);
        pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        builder.setContentIntent(pendingIntent);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        Notification notif = builder.build();
        notif.flags = Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_NO_CLEAR;
        mCyclopsService.startForeground(Cyclops.CYCLOPS_FOREGROUND_NOTIFICATION, notif);
    }

    private void setVoutVideoView(boolean vout) {
        try {
            // If a window of SurfaceView has a title VoutVideoView
            // than the surface wont be destroyed by WindowManagerService
            // while video is playing on external screen in background mode.
            String title = vout ? "VoutVideoView" : "VideoView";
            logdebug("set title to SurfaceView: " + title);
            mSetTitleMethod.invoke(this, title);
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.toString());
        } catch (InvocationTargetException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void onSyncLost() {
        if (mCameraStateListener != null) {
            mCameraStateListener.onSyncLost();
        }
    }

    @Override
    public void onSyncFound() {
        if (mCameraStateListener != null) {
            mCameraStateListener.onSyncFound();
        }
    }

    public void takePicture() {
        if (mCameraCtrl != null) {
            mCameraCtrl.takePicture();
        }
    }

    public Uri getLastUriOfTakenPicture() {
        return (mCameraCtrl != null) ? mCameraCtrl.getLastUriOfTakenPicture() : null;
    }

    private void logdebug(String msg) {
        if (DEBUG) Log.d(TAG, "camId=" + mCameraId + ", " + msg);
    }

}
