package mobi.andron.cyclops;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class CameraPanel {

    public interface Listener {
        public void onSwitchDisplay(CameraLayout camLayout);
        public void onTakePicture(CameraLayout camLayout);
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
    }

    public void setListener(Listener listener, CameraLayout camLayout) {
        if (DEBUG) Log.d(TAG, "setListener");
        mListener = listener;
        mCameraLayout = camLayout;
    }
}
