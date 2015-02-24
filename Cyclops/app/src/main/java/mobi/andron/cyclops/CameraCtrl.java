package mobi.andron.cyclops;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.ti.omap.omap_mm_library.OmapMMLibrary;

import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraCtrl implements Camera.PreviewCallback {

    public interface CameraState {
        int ERROR = -1;
        int IDLE = 0;
        int OPENED = 1;
    }

    public interface SyncCallback {
        void onSyncFound();
        void onSyncLost();
    }

    private static final String TAG = "CameraCtrl";
    private static final boolean DEBUG = true;

    private static OmapMMLibrary mOmapMMHandle;

    private Camera mCamera;
    private int mCameraId;
    private Camera.Size mCameraSize;
    private int mCameraDisplayId;
    private int mCameraState;
    private SurfaceHolder mCameraSurfaceHolder;
    private boolean mCameraMirroring;
    private boolean mCameraRotation180;
    private SyncCallback mSyncCallback;

    static {
        mOmapMMHandle = new OmapMMLibrary();
        mOmapMMHandle.native_init();
    }

    public CameraCtrl(int cameraId) {
        mCameraId = cameraId;
        mCameraDisplayId = -1;
        mCameraState = CameraState.IDLE;
        mCameraMirroring = false;
        mCameraRotation180 = false;
    }

    public boolean openCamera(SurfaceHolder holder, boolean mirroring, boolean rotation180) {
        mCameraSurfaceHolder = holder;
        mCameraMirroring = mirroring;
        mCameraRotation180 = rotation180;
        try {
            if(DEBUG)Log.d(TAG, "Openinig camera");
            // Open camera
            mCamera = Camera.open(mCameraId);
            //mCamera.setSyncCallback(this); TODO
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, cameraInfo);
            /* TODO replace by setDisplayOrientation
            int transform = 0;
            if (mirroring) {
                transform = CameraTransformation.CAMERA_TRANSFORM_FLIP_V;
            }
            if (rotation180) {
                transform = CameraTransformation.CAMERA_TRANSFORM_ROT_180;
            }
            if (mirroring && rotation180) {
                transform = CameraTransformation.CAMERA_TRANSFORM_FLIP_H;
            }
            mCamera.setDisplayTransformation(transform);*/

            /* Initialize preview callback
            Parameters params = mCamera.getParameters();
            mCameraSize = params.getPreviewSize();
            mCamera.addCallbackBuffer(new byte[mCameraSize.width * mCameraSize.height * 4]);
            mCamera.setPreviewCallbackWithBuffer(this);
            */

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            if (SystemProperties.isTvBusyByOtherApp()) {
                setDisplayId(holder, Cyclops.DISPLAY_TYPE_LCD_PRIMARY);
            }
            mCameraState = CameraState.OPENED;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            mCameraState = CameraState.ERROR;
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
            mCameraState = CameraState.ERROR;
        }
        return (mCameraState == CameraState.OPENED);
    }

    public void closeCamera() {
        if(DEBUG)Log.d(TAG, "Closing camera");
        if (mCamera != null) {
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
        mCamera = null;
        mCameraSurfaceHolder = null;
        mCameraState = CameraState.IDLE;
    }

    public int getCameraState() {
        return mCameraState;
    }

    public boolean isCameraOpened() {
        return (mCameraState == CameraState.OPENED);
    }

    public boolean isCameraOnTV() {
        if (mCameraDisplayId == Cyclops.DISPLAY_TYPE_UNDEFINED) {
            mCameraDisplayId = getDisplayId(mCameraSurfaceHolder);
            if (mCameraDisplayId == Cyclops.DISPLAY_TYPE_UNDEFINED) {
                Log.e(TAG, "camera[" + mCameraId + "] display is undefined");
            }
        }
        return (mCameraDisplayId == Cyclops.DISPLAY_TYPE_HDMI_TV);
    }

    public void setSyncCallback(SyncCallback cb) {
        mSyncCallback = cb;
    }

    public int setCameraDisplayId(int displayId) {
        if (mCameraSurfaceHolder == null)
            return -1;
        if (mOmapMMHandle == null)
            return -1;
        mCamera.stopPreview();
        setDisplayId(mCameraSurfaceHolder, displayId);
        mCamera.startPreview();
        mCameraDisplayId = displayId;
        return 0;
    }

    public int getCameraDisplayId() {
        if (mCameraSurfaceHolder == null)
            return -1;
        if (mOmapMMHandle == null)
            return -1;
        if (mCameraDisplayId == Cyclops.DISPLAY_TYPE_UNDEFINED) {
            mCameraDisplayId = getDisplayId(mCameraSurfaceHolder);
        }
        return mCameraDisplayId;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(TAG, "onPreviewFrame cam[" + mCameraId + "] len=" + data.length);
        mCamera.addCallbackBuffer(data);
    }

    // TODO Must be inherited from Camera.SyncCallback
    //@Override
    public void onSyncLost(Camera camera) {
        Log.d(TAG, "onSyncLost cam[" + mCameraId + "]");
        if (mSyncCallback != null) {
            mSyncCallback.onSyncLost();
        }
    }

    // TODO Must be inherited from Camera.SyncCallback
    //@Override
    public void onSyncFound(Camera camera) {
        Log.d(TAG, "onSyncFound cam[" + mCameraId + "]");
        if (mSyncCallback != null) {
            mSyncCallback.onSyncFound();
        }
    }

    private static void setDisplayId(SurfaceHolder holder, int displayId) {
        if(DEBUG)Log.d(TAG, "setDisplayId() displayId=" + displayId);
        mOmapMMHandle.setVideoSurface(holder);
        mOmapMMHandle.setDisplayId(displayId);
    }

    private static int getDisplayId(SurfaceHolder holder) {
        mOmapMMHandle.setVideoSurface(holder);
        int displayId = mOmapMMHandle.getDisplayId();
        if(DEBUG)Log.d(TAG, "getDisplayId() displayId=" + displayId);
        return displayId;
    }
}
