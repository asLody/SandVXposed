package com.lody.virtual.sandxposed;

import android.content.Context;
import android.os.Process;

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


    public static void init(Context context, String packageName, String processName) {
        VLog.d("SandHook","Process attach: "+processName+" in package "+packageName);
        initSystemProp(context);
        initForSpecialApps(context, packageName);
    }

    private static void initSystemProp(Context context) {
        //inject vxp name
        System.setProperty("vxp", "1");
        System.setProperty("vxp_user_dir", new File(context.getApplicationInfo().dataDir).getParent());
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

    private static void initForSpecialApps(Context context, String packageName) {
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
            XposedHelpers.findAndHookMethod(Process.class, "killProcess", int.class, g_Hook);
            XposedHelpers.findAndHookMethod(System.class, "exit", int.class, g_Hook);
            String szEnableRedirectStorage = BanNotificationProvider.getString(context,"disableAdaptApp");
            if(szEnableRedirectStorage==null)
                SKPackageGuard.antiXposedCheck(packageName);
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}
