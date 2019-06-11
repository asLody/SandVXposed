package io.virtualapp.home;

import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

import com.lody.virtual.client.natives.NativeMethods;
import com.lody.virtual.helper.utils.Reflect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lody
 */
public class FlurryROMCollector {

    private static final String TAG = FlurryROMCollector.class.getSimpleName();

    public static void startCollect() {
        Log.d(TAG, "start collect...");
        NativeMethods.init();
        if (NativeMethods.gCameraNativeSetup == null) {
            reportCameraNativeSetup();
        }
        Log.d(TAG, "end collect...");
    }


    private static void reportCameraNativeSetup() {
        for (Method method : Camera.class.getDeclaredMethods()) {
            if ("native_setup".equals(method.getName())) {
                break;
            }
        }
    }
}
