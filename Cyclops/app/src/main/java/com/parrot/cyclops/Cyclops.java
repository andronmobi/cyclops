package com.parrot.cyclops;

import android.os.Environment;

public interface Cyclops {

    public interface PreviewOrientation {
        int LEFT    = 0; // not used
        int TOP     = 1;
        int RIGHT   = 2; // not used
        int BOTTOM  = 3;
    }

    public interface CameraTransformation {
        int CAMERA_TRANSFORM_FLIP_H     = 0x01;
        int CAMERA_TRANSFORM_FLIP_V     = 0x02; /* flip source image vertically (around the horizontal axis)*/
        int CAMERA_TRANSFORM_ROT_90     = 0x04; /* rotate source image 90 degrees clockwise */
        int CAMERA_TRANSFORM_ROT_180    = 0x03; /* rotate source image 180 degrees */
        int CAMERA_TRANSFORM_ROT_270    = 0x07; /* rotate source image 270 degrees clockwise */
    }

    public static final int CYCLOPS_FOREGROUND_NOTIFICATION = 1;

    public static final int DISPLAY_TYPE_UNDEFINED     = -1;
    public static final int DISPLAY_TYPE_LCD_PRIMARY   = 0x0000;
    //public static final int DISPLAY_TYPE_LCD_SECONDARY = 0x0001;
    public static final int DISPLAY_TYPE_HDMI_TV       = 0x0002;

    public static final int MAX_CAMERAS = 1;

    public static final String CAMERA_ID = "camera_id";
    public static final int REAR_CAMERA_ID = 0;
    public static final int USB_CAMERA_ID = 2;

    public static final String CYCLOPS_DIR = Environment.getExternalStorageDirectory().toString() + "/DCIM/Cyclops";
}
