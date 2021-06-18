package com.lody.virtual.sandxposed;

import android.content.Context;
import android.text.TextUtils;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.remote.InstalledAppInfo;
import com.swift.sandhook.xposedcompat.XposedCompat;

import java.io.File;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import me.weishu.reflection.Reflection;
import sk.vpkg.fasthook.HookMode;
import sk.vpkg.fasthook.SKFastHook;
import sk.vpkg.fasthook.SKFastHookManager;

import static com.swift.sandhook.xposedcompat.utils.DexMakerUtils.MD5;

public class SandXposed {
    public static void freeReflection(Context base)
    {
        Reflection.unseal(base);
    }

    public static void init(Boolean debugEnabled) {
    }

    public static void injectXposedModule(Context context, String packageName, String processName) {

        if (BlackList.canNotInject(packageName, processName))
            return;

        List<InstalledAppInfo> appInfos = VirtualCore.get().getInstalledApps(InstalledAppInfo.FLAG_XPOSED_MODULE | InstalledAppInfo.FLAG_ENABLED_XPOSED_MODULE);
        ClassLoader classLoader = context.getClassLoader();

        XposedCompat.context = context;
        XposedCompat.packageName = packageName;
        XposedCompat.processName = processName;
        XposedCompat.cacheDir = new File(context.getCacheDir(), MD5(processName));
        XposedCompat.classLoader = XposedCompat.getSandHookXposedClassLoader(classLoader, XposedBridge.class.getClassLoader());
        XposedCompat.isFirstApplication = true;

        SandHookHelper.initHookPolicy();

        try{
            SKFastHookManager.setHookMode(HookMode.getHookMode(context));
        }catch (Throwable e)
        {
            e.printStackTrace();
        }

        for (InstalledAppInfo module:appInfos) {
            if (TextUtils.equals(packageName, module.packageName)) {
                VLog.d("injectXposedModule", "injectSelf : " + processName);
            }
            try
            {
                if(SKFastHookManager.isSandHook())
                    XposedCompat.loadModule(module.apkPath, module.getOdexFile().getParent(), module.libPath, XposedBridge.class.getClassLoader());
                else if(SKFastHookManager.isEpicBusiness())
                {
                    XposedCompat.loadModule(module.apkPath, module.getOdexFile().getParent(), module.libPath, XposedBridge.class.getClassLoader());
                    // Setup HookMode
                }
                else if(SKFastHookManager.isSHook())
                {
                    SKFastHook.loadModule(module.apkPath,module.getOdexFile().getParent(),
                            module.libPath,module.getApplicationInfo(0),SKFastHook.class.getClassLoader());
                }
                else if(SKFastHookManager.isWhale())
                {
                    // Bugs: May crash on Android Q devices.
                    // So this method may not be published.
                    continue;
                }
                else if(SKFastHookManager.isYahfa())
                {
                    // TODO: fix yahfa crash on Android Q+
                    // SK团队 专业 - 专精
                }
                else
                {
                    continue;
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
                VLog.e("injectXposedModule","Inject failed...");
                break;
            }
        }
        EnvironmentSetup.init(context, packageName, processName);

        try {
            XposedCompat.callXposedModuleInit();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }



}
