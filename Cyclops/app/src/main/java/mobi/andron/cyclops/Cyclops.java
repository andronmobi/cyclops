package mobi.andron.cyclops;

public interface Cyclops {

    public interface PreviewOrientation {
        int LEFT    = 0; // not used
        int TOP     = 1;
        int RIGHT   = 2; // not used
        int BOTTOM  = 3;
    }

    public static final int CYCLOPS_FOREGROUND_NOTIFICATION = 1;

    public static final int DISPLAY_TYPE_UNDEFINED     = -1;
    public static final int DISPLAY_TYPE_LCD_PRIMARY   = 0x0000;
    //public static final int DISPLAY_TYPE_LCD_SECONDARY = 0x0001;
    public static final int DISPLAY_TYPE_HDMI_TV       = 0x0002;

    public static final int MAX_CAMERAS = 2;

    public static final String CAMERA_ID = "camera_id";
    public static final int REAR_CAMERA_ID = 0;
    public static final int USB_CAMERA_ID = 2;

}
