package io.virtualapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.sandxposed.SandXposed;
import com.sk.ace.ability.SlimXposed;
import com.sk.dexdumper.DumpDexV2;

import io.virtualapp.delegate.MyAppRequestListener;
import io.virtualapp.delegate.MyComponentDelegate;
import io.virtualapp.delegate.MyPhoneInfoDelegate;
import io.virtualapp.delegate.MyTaskDescriptionDelegate;
import jonathanfinerty.once.Once;

/**
 * @author Lody
 */
public class VApp extends MultiDexApplication {

    private static VApp gApp;
    private SharedPreferences mPreferences;

    public static VApp getApp() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SandXposed.freeReflection(base);
        }
        if(Build.VERSION.SDK_INT >= 30)
        {
            com.sk.SKAppLoad.InitApp.bindApplicationPassCheck();
        }
        try{
            com.sk.SKAppLoad.InitApp.emplaceDeviceCompat();
        }catch (Exception ignored)
        {
        }
        SlimXposed.init(base);
        VLog.OPEN_LOG = BuildConfig.DEBUG;
        mPreferences = base.getSharedPreferences("va", Context.MODE_MULTI_PROCESS);
        VASettings.ENABLE_IO_REDIRECT = true;
        VASettings.ENABLE_INNER_SHORTCUT = false;
        try{
            if(DumpDexV2.isDumpEnabled(base))
            {
                DumpDexV2.dumpDexDirectly(DumpDexV2.buildAndCleanUpDir(base).
                        getAbsolutePath());
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        gApp = this;
        super.onCreate();
        // lazyInjectInit();
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {

            @Override
            public void onMainProcess() {
                Once.initialise(VApp.this);
            }

            @Override
            public void onVirtualProcess() {
                //listener components
                virtualCore.setComponentDelegate(new MyComponentDelegate());
                //fake phone imei,macAddress,BluetoothAddress
                virtualCore.setPhoneInfoDelegate(new MyPhoneInfoDelegate());
                //fake task description's icon and title
                virtualCore.setTaskDescriptionDelegate(new MyTaskDescriptionDelegate());
            }

            @Override
            public void onServerProcess() {
                virtualCore.setAppRequestListener(new MyAppRequestListener(VApp.this));
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqq");
                virtualCore.addVisibleOutsidePackage("com.tencent.mobileqqi");
                virtualCore.addVisibleOutsidePackage("com.tencent.minihd.qq");
                virtualCore.addVisibleOutsidePackage("com.tencent.qqlite");
                virtualCore.addVisibleOutsidePackage("com.facebook.katana");
                virtualCore.addVisibleOutsidePackage("com.whatsapp");
                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
                virtualCore.addVisibleOutsidePackage("com.immomo.momo");
            }
        });
    }

    public static SharedPreferences getPreferences() {
        return getApp().mPreferences;
    }

    /*
    private void lazyInjectInit() {
        LazyInject.init(this);
        LazyInject.addBuildMap(Auto_ComponentBuildMap.class);
    }
    */

}
