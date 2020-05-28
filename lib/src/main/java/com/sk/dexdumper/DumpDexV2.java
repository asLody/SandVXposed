package com.sk.dexdumper;

import android.content.Context;

import com.lody.virtual.helper.utils.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import sk.vpkg.provider.BanNotificationProvider;

public class DumpDexV2
{
    private static final String TAG = DumpDexV2.class.getSimpleName();
    private static boolean JNI_Loaded = false;
    public static void dumpDexDirectly(String sPackage)
    {
        try{
            System.loadLibrary("sdump");
            JNI_Loaded = true;
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
        if(JNI_Loaded)
        {
            try{
                dump(sPackage);
            }catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }
    public static void setDumpEnabled(Context context, Boolean isEnable)
    {
        String strEnabled = BanNotificationProvider.getString(context,TAG);
        if(strEnabled!=null&&(!isEnable))
        {
            BanNotificationProvider.remove(context,TAG);
        }
        else if(isEnable&&(strEnabled==null))
        {
            BanNotificationProvider.save(context,TAG,TAG);
        }
    }
    public static Boolean isDumpEnabled(Context context)
    {
        String strEnabled = BanNotificationProvider.getString(context,TAG);
        if(strEnabled!=null)
        {
            return strEnabled.equals(TAG);
        }
        return Boolean.FALSE;
    }
    public static java.io.File buildAndCleanUpDir(Context baseCtx)
    {
        java.io.File f0 = getStoreDir(baseCtx);
        if(!f0.exists())
        {
            if(!f0.mkdirs())return null;
        }
        java.io.File[] files = f0.listFiles();
        for(java.io.File f1 : files)
        {
            if(!f1.delete())
            {
                if(f1.isDirectory())
                    FileUtils.deleteDir(f1);
            }
        }
        return f0;
    }
    public static java.io.File[] allDumpedFiles(Context baseCtx)
    {
        ArrayList<File> fList = new ArrayList<>();
        try{
            for(File f0 : getStoreDir(baseCtx).listFiles())
            {
                if(!f0.isDirectory())
                {
                    fList.add(f0);
                }
            }
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
        return fList.toArray(new File[0]);
    }
    public static java.io.File getStoreDir(Context baseCtx)
    {
        try{
            File oneFile = baseCtx.getExternalFilesDir("skdexdump");
            if(oneFile!=null)
            {
                if(!oneFile.exists())
                {
                    if(!oneFile.mkdirs())throw new FileNotFoundException("not found external");
                }
                return oneFile;
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        // Not found
        return baseCtx.getDir("skdexdump",Context.MODE_PRIVATE);
    }
    private static native void dump(String szPath);
}
