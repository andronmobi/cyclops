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

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {

    private static final String TAG = "SettingsActivity";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int cameraId = getIntent().getIntExtra(Cyclops.CAMERA_ID, Cyclops.REAR_CAMERA_ID);
        Log.i(TAG, "cameraId = " + cameraId);
        addPreferencesFromResource(R.xml.settings);
    }

}
