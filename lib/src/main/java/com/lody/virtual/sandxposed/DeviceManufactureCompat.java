package com.lody.virtual.sandxposed;

import android.os.Build;

public class DeviceManufactureCompat
{
    public static boolean isMIUI() {
        String manufacturer = Build.MANUFACTURER;
        if ("xiaomi".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    public static boolean isEMUI() {
        String manufacturer = Build.MANUFACTURER;
        if ("HUAWEI".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    public static boolean isOPPO() {
        String manufacturer = Build.MANUFACTURER;
        if ("OPPO".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    public static boolean isVIVO() {
        String manufacturer = Build.MANUFACTURER;
        if ("vivo".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }
}
