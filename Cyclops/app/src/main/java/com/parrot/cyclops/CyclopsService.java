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

import java.util.HashMap;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CyclopsService extends Service {

    private static final String TAG = "CyclopsService";
    private static final boolean DEBUG = true;

    private final IBinder mBinder = new CyclopsBinder();
    private CameraCtrl[] mCamCtrlArr;
    private HashMap<Integer, CameraCtrl> mCamCtrlMap;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class CyclopsBinder extends Binder {
        CyclopsService getService() {
            return CyclopsService.this;
        }
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate");
        mCamCtrlMap = new HashMap<Integer, CameraCtrl>();
        mCamCtrlArr = new CameraCtrl[Cyclops.MAX_CAMERAS];
        for (int i = 0; i < Cyclops.MAX_CAMERAS; i++) {
            mCamCtrlArr[i] = new CameraCtrl(this, Cyclops.REAR_CAMERA_ID);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand");
        return  START_NOT_STICKY ;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onBind");
        return mBinder;
    }

    CameraCtrl obtainCameraCntrl(int id) {
        CameraCtrl controller = mCamCtrlMap.get(id);
        if (controller == null) {
            controller = new CameraCtrl(this, id);
            mCamCtrlMap.put(id, controller);
        }
        return controller;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onUnbind");
        // TODO if there is no camera in background the service must be stopped.
        return false;
    }

    @Override
    public void onRebind(Intent intent) {
        if (DEBUG) Log.d(TAG, "onRebind");
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
    }

}
