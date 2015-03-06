package mobi.andron.cyclops;

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
