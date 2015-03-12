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

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class CameraPanel {

    public interface Listener {
        public void onSwitchDisplay(CameraLayout camLayout);
        public void onTakePicture(CameraLayout camLayout);
        public void onOpenImageFolder(CameraLayout camLayout);
    }

    private static final String TAG = "CameraPanel";
    private static final boolean DEBUG = true;

    private Listener mListener = null;
    private CameraLayout mCameraLayout;

    public CameraPanel(ViewGroup layoutControl) {
        layoutControl.findViewById(R.id.button_switch_to_tv)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DEBUG) Log.d(TAG, "onSwitchDisplay");
                        if (mListener != null) {
                            mListener.onSwitchDisplay(mCameraLayout);
                        }
                    }
                });
        layoutControl.findViewById(R.id.button_take_photo)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DEBUG) Log.d(TAG, "onTakePicture");
                        if (mListener != null) {
                            mListener.onTakePicture(mCameraLayout);
                        }
                    }
                });
        layoutControl.findViewById(R.id.button_go_to_gallery)
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DEBUG) Log.d(TAG, "onOpenImageFolder");
                        if (mListener != null) {
                            mListener.onOpenImageFolder(mCameraLayout);
                        }
                    }
                });
    }

    public void setListener(Listener listener, CameraLayout camLayout) {
        if (DEBUG) Log.d(TAG, "setListener");
        mListener = listener;
        mCameraLayout = camLayout;
    }
}
