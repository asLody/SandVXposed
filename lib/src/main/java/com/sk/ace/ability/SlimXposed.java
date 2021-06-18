package com.sk.ace.ability;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import lu.die.shook.SlimHook;

public class SlimXposed {
    private static boolean isDebugApp(Context context)
    {
        ApplicationInfo info = context.getApplicationInfo();
        return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    public static void init(Context context)
    {
        try{
            SlimHook.setDebug(isDebugApp(context));
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
