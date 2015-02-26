package mobi.andron.cyclops;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.ti.omap.omap_mm_library.OmapMMLibrary;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class CameraCtrl implements Camera.PreviewCallback, Camera.PictureCallback {

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

    private static final Uri STORAGE_URI = Images.Media.EXTERNAL_CONTENT_URI;
    public static final String CYCLOPS_DIR = "/DCIM/Cyclops";
    private static OmapMMLibrary mOmapMMHandle;

    private Context mContext;
    private Camera mCamera;
    private int mCameraId;
    private Camera.Size mCameraSize;
    private int mCameraDisplayId;
    private int mCameraState;
    private boolean mIsSnapshotInProg;
    private SurfaceHolder mCameraSurfaceHolder;
    private boolean mCameraMirroring;
    private boolean mCameraRotation180;
    private SyncCallback mSyncCallback;

    static {
        mOmapMMHandle = new OmapMMLibrary();
        mOmapMMHandle.native_init();
    }

    public CameraCtrl(Context context, int cameraId) {
        mContext = context;
        mCameraId = cameraId;
        mCameraDisplayId = -1;
        mCameraState = CameraState.IDLE;
        mCameraMirroring = false;
        mCameraRotation180 = false;
        mIsSnapshotInProg = false;
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

    public void takePicture() {
        if (mCamera != null && mCameraState == CameraState.OPENED) {
            // we are in the progress of taking a snapshot
            if (mIsSnapshotInProg) {
                return;
            }
            mCamera.takePicture(null, null, null, this);
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        long dateTaken = System.currentTimeMillis();
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat = new SimpleDateFormat(mContext.getString(R.string.image_file_name_format));
        String title = dateFormat.format(date);
        String filename = title + ".jpg";
        String dirName = Environment.getExternalStorageDirectory().toString() + CYCLOPS_DIR;
        Uri uri = addImage(mContext.getContentResolver(), title, dateTaken, null, dirName, filename, null, data);
        String msg;
        if (uri != null) {
            msg = mContext.getString(R.string.image_taken);
        } else {
            msg = mContext.getString(R.string.image_not_taken);
        }
        Toast toast = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    //
    // Stores a bitmap or a jpeg byte array to a file (using the specified
    // directory and filename). Also add an entry to the media store for
    // this picture. The title, dateTaken, location are attributes for the
    // picture. The degree is a one element array which returns the orientation
    // of the picture.
    //
    public static Uri addImage(ContentResolver cr, String title, long dateTaken,
                               Location location, String directory, String filename,
                               Bitmap source, byte[] jpegData) {
        // We should store image data earlier than insert it to ContentProvider,
        // otherwise we may not be able to generate thumbnail in time.
        OutputStream outputStream = null;
        String filePath = directory + "/" + filename;
        try {
            File dir = new File(directory);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(directory, filename);
            outputStream = new FileOutputStream(file);
            if (source != null) {
                source.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            } else {
                outputStream.write(jpegData);
            }
        } catch (FileNotFoundException ex) {
            Log.w(TAG, ex);
            return null;
        } catch (IOException ex) {
            Log.w(TAG, ex);
            return null;
        } finally {
            closeSilently(outputStream);
        }

        // Read back the compressed file size.
        long size = new File(directory, filename).length();

        ContentValues values = new ContentValues(9);
        values.put(MediaStore.Images.Media.TITLE, title);

        // That filename is what will be handed to Gmail when a user shares a
        // photo. Gmail gets the name of the picture attachment from the
        // "DISPLAY_NAME" field.
        values.put(Images.Media.DISPLAY_NAME, filename);
        values.put(Images.Media.DATE_TAKEN, dateTaken);
        values.put(Images.Media.MIME_TYPE, "image/jpeg");
        //values.put(Images.Media.ORIENTATION, ?);
        values.put(Images.Media.DATA, filePath);
        values.put(Images.Media.SIZE, size);

        if (location != null) {
            values.put(Images.Media.LATITUDE, location.getLatitude());
            values.put(Images.Media.LONGITUDE, location.getLongitude());
        }

        return cr.insert(STORAGE_URI, values);
    }

    private static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // do nothing
        }
    }
}
