package com.lody.virtual.client.core;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RomChecker
{
    private static final String KEY_VERSION_EMUI = "ro.build.version.emui";
    private static final String KEY_VERSION_MIUI = "ro.miui.ui.version.name";
    private static final String KEY_VERSION_OPPO = "ro.build.version.opporom";
    private static final String KEY_VERSION_SMARTISAN = "ro.smartisan.version";
    private static final String KEY_VERSION_VIVO = "ro.vivo.os.version";
    public static final String ROM_EMUI = "EMUI";
    public static final String ROM_FLYME = "FLYME";
    public static final String ROM_MIUI = "MIUI";
    public static final String ROM_OPPO = "OPPO";
    public static final String ROM_QIKU = "QIKU";
    public static final String ROM_SMARTISAN = "SMARTISAN";
    public static final String ROM_VIVO = "VIVO";
    private static final String TAG = "Rom";
    private static String sName;
    private static String sVersion;

    public static boolean check(String str) {
        if (sName != null) {
            return sName.equals(str);
        }
        CharSequence prop = getProp(KEY_VERSION_MIUI);
        sVersion = (String) prop;
        if (TextUtils.isEmpty(prop)) {
            prop = getProp(KEY_VERSION_EMUI);
            sVersion = (String) prop;
            if (TextUtils.isEmpty(prop)) {
                prop = getProp(KEY_VERSION_OPPO);
                sVersion = (String) prop;
                if (TextUtils.isEmpty(prop)) {
                    prop = getProp(KEY_VERSION_VIVO);
                    sVersion = (String) prop;
                    if (TextUtils.isEmpty(prop)) {
                        prop = getProp(KEY_VERSION_SMARTISAN);
                        sVersion = (String) prop;
                        if (TextUtils.isEmpty(prop)) {
                            sVersion = Build.DISPLAY;
                            if (sVersion.toUpperCase().contains(ROM_FLYME)) {
                                sName = ROM_FLYME;
                            } else {
                                sName = Build.MANUFACTURER.toUpperCase();
                            }
                        } else {
                            sName = ROM_SMARTISAN;
                        }
                    } else {
                        sName = ROM_VIVO;
                    }
                } else {
                    sName = ROM_OPPO;
                }
            } else {
                sName = ROM_EMUI;
            }
        } else {
            sName = ROM_MIUI;
        }
        return sName.equals(str);
    }

    public static String getName() {
        if (sName == null) {
            check("");
        }
        return sName;
    }

    public static String getProp(String str) {
        BufferedReader bufferedReader;
        Throwable e;
        String str2;
        StringBuilder stringBuilder;
        Throwable th;
        BufferedReader bufferedReader2 = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getprop ");
            stringBuilder2.append(str);
            bufferedReader = new BufferedReader(new InputStreamReader(runtime.exec(stringBuilder2.toString()).getInputStream()), 1024);
            try {
                String readLine = bufferedReader.readLine();
                bufferedReader.close();
                try {
                    bufferedReader.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return readLine;
            } catch (IOException e3) {
                e = e3;
                try {
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to read prop ");
                    stringBuilder.append(str);
                    Log.e(str2, stringBuilder.toString(), e);
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    bufferedReader2 = bufferedReader;
                    if (bufferedReader2 != null) {
                        try {
                            bufferedReader2.close();
                        } catch (IOException e4) {
                            e4.printStackTrace();
                        }
                    }
                    throw th;
                }
            }
        } catch (IOException e5) {
            e = e5;
            bufferedReader = null;
            str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to read prop ");
            stringBuilder.append(str);
            Log.e(str2, stringBuilder.toString(), e);
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        } catch (Throwable th3) {
            th = th3;
            if (bufferedReader2 != null) {
                try {
                    bufferedReader2.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return null;
        }
    }

    public static String getVersion() {
        if (sVersion == null) {
            check("");
        }
        return sVersion;
    }

    public static boolean is360() {
        if (!check(ROM_QIKU)) {
            if (!check("360")) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmui() {
        return check(ROM_EMUI);
    }

    public static boolean isFlyme() {
        return check(ROM_FLYME);
    }

    public static boolean isMiui() {
        return check(ROM_MIUI);
    }

    public static boolean isOppo() {
        return check(ROM_OPPO);
    }

    public static boolean isSmartisan() {
        return check(ROM_SMARTISAN);
    }

    public static boolean isVivo() {
        return check(ROM_VIVO);
    }
}
