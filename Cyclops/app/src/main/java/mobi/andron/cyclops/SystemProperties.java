package mobi.andron.cyclops;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public class SystemProperties {

    private static boolean mIsDeviceDetected = false;
    private static String mDeviceName = null;
    private static Method mGetMethod = null;

    private static boolean mTvBusyByOtherApp = false;
    private static boolean mTvBusyByCyclops = false;

    static {
        @SuppressWarnings("rawtypes")
        Class clazz = null;
        try {
            clazz = Class.forName("android.os.SystemProperties");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            mGetMethod = clazz.getDeclaredMethod("get", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void setTvBusyByOtherApp(boolean busy) { mTvBusyByOtherApp = busy; }
    public static boolean isTvBusyByOtherApp() { return mTvBusyByOtherApp; }

    public static void setTvBusyByCyclops(boolean busy) { mTvBusyByCyclops = busy; }
    public static boolean isTvBusyByCyclops() { return mTvBusyByCyclops; }

    public static String getDeviceName() {
        String prop = null;
        try {
            prop = (String) mGetMethod.invoke(null, "ro.product.name");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return prop;
    }

    public static boolean isOverlay0OnTV() {
        String prop = null;
        try {
            prop = (String) mGetMethod.invoke(null, "sys.overlay0.tv");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return "true".equals(prop);
    }

    public boolean isRnb5() {
        if (!mIsDeviceDetected) {
            mDeviceName = getDeviceName();
            mIsDeviceDetected = true;
        }
        return (mDeviceName != null) ? (mDeviceName.equals("rnb5") ? true : false) : false;
    }

}
