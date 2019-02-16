package com.lody.virtual.client.sandhook;

import android.content.Context;
import android.text.TextUtils;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstalledAppInfo;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

import static com.lody.virtual.client.sandhook.SandHookHelper.MD5;
import static com.lody.virtual.client.sandhook.SandHookHelper.initHookPolicy;

public class SandXposed {

    public static void injectXposedModule(Context context, String packageName, String processName) {

        List<InstalledAppInfo> appInfos = VirtualCore.get().getInstalledApps(0);
        ClassLoader classLoader = context.getClassLoader();

        for (InstalledAppInfo module:appInfos) {
            if (TextUtils.equals(packageName, module.packageName))
                continue;
            XposedCompat.loadModule(module.apkPath, module.getOdexFile().getParent(), module.libPath, XposedBridge.class.getClassLoader());
        }

        XposedCompat.context = context;
        XposedCompat.packageName = packageName;
        XposedCompat.processName = processName;
        XposedCompat.cacheDir = new File(context.getCacheDir(), MD5(processName));
        XposedCompat.classLoader = XposedCompat.getSandHookXposedClassLoader(classLoader, XposedBridge.class.getClassLoader());
        XposedCompat.isFirstApplication = true;

        initHookPolicy();

        try {
            XposedCompat.callXposedModuleInit();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }



}
