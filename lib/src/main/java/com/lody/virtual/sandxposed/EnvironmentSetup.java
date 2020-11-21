package com.lody.virtual.sandxposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import com.lody.virtual.BuildConfig;
import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.helper.utils.VLog;
import com.swift.sandhook.xposedcompat.utils.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import sk.vpkg.provider.BanNotificationProvider;
import sk.vpkg.sign.SKPackageGuard;

public class EnvironmentSetup {
    public static volatile boolean enableEnv = false;

    public static void init(Context context, String packageName, String processName) {
        VLog.d("SandHook","Process attach: "+processName+" in package "+packageName);
        if(enableEnv) initSystemProp(context);
        initForSpecialApps(context, packageName);
        if(BuildConfig.DEBUG)
        {
            initForDebug(context);
        }
    }

    @SuppressLint("SdCardPath")
    private static void initSystemProp(Context context) {
        //inject vxp name
        System.setProperty("vxp", "1");
        String xPath = new File(context.getApplicationInfo().dataDir).getParent();
        System.setProperty("vxp_user_dir", xPath!=null?xPath:"/data/data/io.virtualapp.sandvxposed/cache/");
        //sandvxp
        System.setProperty("sandvxp", "1");
    }

    private final static LinkedList<String> pListCrashBlocker = new LinkedList<>(
            Arrays.asList("com.tencent",
                    "com.netease",
                    "com.eg.android.AlipayGphone",
                    "com.taobao",
                    "com.tmall")
    );
    private static boolean is_HookCrash(String packageName)
    {
        for(String szPkgName : pListCrashBlocker)
        {
            if(packageName.startsWith(szPkgName))
                return true;
        }
        return false;
    }

    private static void initForDebug(final Context context)
    {
        Log.d("SKAppCompat","Debug check.");
        // Forbid Gameguardian or other game hack tool
        boolean ggclassExist = false;
        try{
            context.getClassLoader().loadClass("org.luaj.vm2.Lua");
            ggclassExist = true;
        }catch (Throwable ignored)
        {
        }
        try{
            context.getClassLoader().loadClass("luaj.Lua");
            ggclassExist = true;
        }catch (Throwable ignored)
        {
        }

        if(ggclassExist)
        {
            new Thread()
            {
                @Override
                public void run()
                {
                    Looper.prepare();
                    Toast.makeText(context,"This software may not support.",Toast.LENGTH_LONG)
                            .show();
                    Looper.loop();
                }
            }.start();
            new Thread()
            {
                @Override
                public void run()
                {
                    try{
                        Thread.sleep(1800000L);
                    }catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        return;
                    }
                    System.exit(308);
                }
            }.start();
        }
    }

    private static void initForSpecialApps(final Context context, final String packageName) {
        if (!is_HookCrash(packageName))
            return;
        if(packageName.startsWith("com.tencent.mm"))
        {
            //delete tinker patches
            File dataDir = new File(context.getApplicationInfo().dataDir);
            File tinker = new File(dataDir, "tinker");
            File tinker_temp = new File(dataDir, "tinker_temp");
            File tinker_server = new File(dataDir, "tinker_server");

        try {
            FileUtils.delete(tinker);
            FileUtils.delete(tinker_temp);
            FileUtils.delete(tinker_server);
        } catch (Exception ignored) {}
        }

        // avoid mm kill self
        // final int mainProcessId = Process.myPid();
        XC_MethodHook g_Hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                /*
                int pid = (int) param.args[0];
                if (pid != mainProcessId) {
                    return;
                }
                */
                // try kill main process, find stack
                param.setResult(null);
                /*
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                if (stackTrace == null) {
                    return;
                }
                for (StackTraceElement stackTraceElement : stackTrace) {
                    if (stackTraceElement.getClassName().contains("com.tencent.mm.app")) {
                        XposedBridge.log("do not suicide..." + Arrays.toString(stackTrace));
                        break;
                    }
                }
                */
            }
        };

        try
        {
            // 可以不再钩结束函数。
            //XposedHelpers.findAndHookMethod(Process.class, "killProcess", int.class, g_Hook);
            //XposedHelpers.findAndHookMethod(System.class, "exit", int.class, g_Hook);
            String szEnableRedirectStorage = BanNotificationProvider.getString(context,"disableAdaptApp");
            if(szEnableRedirectStorage!=null)
                SKPackageGuard.antiXposedCheck(packageName);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}
