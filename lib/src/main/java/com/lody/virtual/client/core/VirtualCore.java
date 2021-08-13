package com.lody.virtual.client.core;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;

import com.lody.virtual.R;
import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.env.VirtualRuntime;
import com.lody.virtual.client.fixer.ContextFixer;
import com.lody.virtual.client.hook.delegate.ComponentDelegate;
import com.lody.virtual.client.hook.delegate.PhoneInfoDelegate;
import com.lody.virtual.client.hook.delegate.TaskDescriptionDelegate;
import com.lody.virtual.client.ipc.ServiceManagerNative;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.client.stub.VASettings;
import com.lody.virtual.helper.compat.BundleCompat;
import com.lody.virtual.helper.ipcbus.IPCBus;
import com.lody.virtual.helper.ipcbus.IPCSingleton;
import com.lody.virtual.helper.ipcbus.IServerCache;
import com.lody.virtual.helper.utils.BitmapUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;
import com.lody.virtual.server.ServiceCache;
import com.lody.virtual.server.interfaces.IAppManager;
import com.lody.virtual.server.interfaces.IAppRequestListener;
import com.lody.virtual.server.interfaces.IPackageObserver;
import com.lody.virtual.server.interfaces.IUiCallback;

import java.io.File;
import java.io.IOException;
import java.util.List;

import dalvik.system.DexFile;
import mirror.android.app.ActivityThread;
import sk.vpkg.location.getPkgLocation;
import sk.vpkg.manager.RenameAppUtils;
import sk.vpkg.notification.SKVPackageNotificationHook;
import sk.vpkg.xposed.XposedUtils;

/**
 * @author Lody
 * @version 3.5
 */
public final class VirtualCore {

    public static final int GET_HIDDEN_APP = 0x00000001;

    @SuppressLint("StaticFieldLeak")
    private static VirtualCore gCore = new VirtualCore();
    private final int myUid = Process.myUid();
    /**
     * Client Package Manager
     */
    private PackageManager unHookPackageManager;
    /**
     * Host package name
     */
    private String hostPkgName;
    /**
     * ActivityThread instance
     */
    private Object mainThread;
    private Context context;
    /**
     * Main ProcessName
     */
    private String mainProcessName;
    /**
     * Real Process Name
     */
    private String processName;
    private ProcessType processType;
    private IPCSingleton<IAppManager> singleton = new IPCSingleton<>(IAppManager.class);
    private boolean isStartUp;
    private PackageInfo hostPkgInfo;
    private int systemPid;
    private ConditionVariable initLock = new ConditionVariable();
    private PhoneInfoDelegate phoneInfoDelegate;
    private ComponentDelegate componentDelegate;
    private TaskDescriptionDelegate taskDescriptionDelegate;

    private VirtualCore() {
    }

    public static VirtualCore get() {
        return gCore;
    }

    public static PackageManager getPM() {
        return get().getPackageManager();
    }

    public static Object mainThread() {
        return get().mainThread;
    }

    public ConditionVariable getInitLock() {
        return initLock;
    }

    public int myUid() {
        return myUid;
    }

    public int myUserId() {
        return VUserHandle.getUserId(myUid);
    }

    public ComponentDelegate getComponentDelegate() {
        return componentDelegate == null ? ComponentDelegate.EMPTY : componentDelegate;
    }

    public void setComponentDelegate(ComponentDelegate delegate) {
        this.componentDelegate = delegate;
    }

    public PhoneInfoDelegate getPhoneInfoDelegate() {
        return phoneInfoDelegate;
    }

    public void setPhoneInfoDelegate(PhoneInfoDelegate phoneInfoDelegate) {
        this.phoneInfoDelegate = phoneInfoDelegate;
    }

    public void setCrashHandler(CrashHandler handler) {
        VClientImpl.get().setCrashHandler(handler);
    }

    public TaskDescriptionDelegate getTaskDescriptionDelegate() {
        return taskDescriptionDelegate;
    }

    public void setTaskDescriptionDelegate(TaskDescriptionDelegate taskDescriptionDelegate) {
        this.taskDescriptionDelegate = taskDescriptionDelegate;
    }

    public int[] getGids() {
        return hostPkgInfo.gids;
    }

    public Context getContext() {
        return context;
    }

    public PackageManager getPackageManager() {
        return context.getPackageManager();
    }

    public String getHostPkg() {
        return hostPkgName;
    }

    public PackageManager getUnHookPackageManager() {
        return unHookPackageManager;
    }


    public void startup(Context context) throws Throwable {
        if (!isStartUp) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // throw new IllegalStateException("VirtualCore.startup() must called in main thread.");
                VLog.e("VirtualCore","VirtualCore.startup() must called in main thread.");
            }
            VASettings.STUB_CP_AUTHORITY = context.getPackageName() + "." + VASettings.STUB_DEF_AUTHORITY;
            ServiceManagerNative.SERVICE_CP_AUTH = context.getPackageName() + "." + ServiceManagerNative.SERVICE_DEF_AUTH;
            this.context = context;
            mainThread = ActivityThread.currentActivityThread.call();
            unHookPackageManager = context.getPackageManager();
            hostPkgInfo = unHookPackageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
            IPCBus.initialize(new IServerCache() {
                @Override
                public void join(String serverName, IBinder binder) {
                    ServiceCache.addService(serverName, binder);
                }

                @Override
                public IBinder query(String serverName) {
                    return ServiceManagerNative.getService(serverName);
                }
            });
            detectProcessType();
            InvocationStubManager invocationStubManager = InvocationStubManager.getInstance();
            invocationStubManager.init();
            invocationStubManager.injectAll();
            ContextFixer.fixContext(context);
            isStartUp = true;
            if (initLock != null) {
                initLock.open();
                initLock = null;
            }
        }
    }

    public void waitForEngine() {
        ServiceManagerNative.ensureServerStarted();
    }

    public boolean isEngineLaunched() {
        String engineProcessName = getEngineProcessName();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.processName.endsWith(engineProcessName)) {
                return true;
            }
        }
        return false;
    }

    public String getEngineProcessName() {
        return context.getString(R.string.engine_process_name);
    }

    public void initialize(VirtualInitializer initializer) {
        if (initializer == null) {
            throw new IllegalStateException("Initializer = NULL");
        }
        switch (processType) {
            case Main:
                initializer.onMainProcess();
                break;
            case VAppClient:
                initializer.onVirtualProcess();
                break;
            case Server:
                initializer.onServerProcess();
                break;
            case CHILD:
                initializer.onChildProcess();
                break;
        }
    }

    private void detectProcessType() {
        // Host package name
        hostPkgName = context.getApplicationInfo().packageName;
        // Main process name
        mainProcessName = context.getApplicationInfo().processName;
        // Current process name
        processName = ActivityThread.getProcessName.call(mainThread);
        if (processName.equals(mainProcessName)) {
            processType = ProcessType.Main;
        } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
            processType = ProcessType.Server;
        } else if (VActivityManager.get().isAppProcess(processName)) {
            processType = ProcessType.VAppClient;
        } else {
            processType = ProcessType.CHILD;
        }
        if (isVAppProcess()) {
            systemPid = VActivityManager.get().getSystemPid();
        }
    }

    private IAppManager getService() {
        return singleton.get();
    }

    /**
     * @return If the current process is used to VA.
     */
    public boolean isVAppProcess() {
        return ProcessType.VAppClient == processType;
    }

    /**
     * @return If the current process is the main.
     */
    public boolean isMainProcess() {
        return ProcessType.Main == processType;
    }

    /**
     * @return If the current process is the child.
     */
    public boolean isChildProcess() {
        return ProcessType.CHILD == processType;
    }

    /**
     * @return If the current process is the server.
     */
    public boolean isServerProcess() {
        return ProcessType.Server == processType;
    }

    /**
     * @return the <em>actual</em> process name
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * @return the <em>Main</em> process name
     */
    public String getMainProcessName() {
        return mainProcessName;
    }

    /**
     * Optimize the Dalvik-Cache for the specified package.
     *
     * @param pkg package name
     * @throws IOException
     */
    @Deprecated
    public void preOpt(String pkg) throws IOException {
        InstalledAppInfo info = getInstalledAppInfo(pkg, 0);
        if (info != null && !info.dependSystem) {
            DexFile.loadDex(info.apkPath, info.getOdexFile().getPath(), 0).close();
        }
    }

    /**
     * Is the specified app running in foreground / background?
     *
     * @param packageName package name
     * @param userId      user id
     * @return if the specified app running in foreground / background.
     */
    public boolean isAppRunning(String packageName, int userId) {
        return VActivityManager.get().isAppRunning(packageName, userId);
    }

    public InstallResult installPackage(String apkPath, int flags) {
        try {
            return getService().installPackage(apkPath, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void addVisibleOutsidePackage(String pkg) {
        try {
            getService().addVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void removeVisibleOutsidePackage(String pkg) {
        try {
            getService().removeVisibleOutsidePackage(pkg);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsidePackageVisible(String pkg) {
        try {
            return getService().isOutsidePackageVisible(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isAppInstalled(String pkg) {
        try {
            return getService().isAppInstalled(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isPackageLaunchable(String packageName) {
        InstalledAppInfo info = getInstalledAppInfo(packageName, 0);
        return info != null
                && getLaunchIntent(packageName, info.getInstalledUsers()[0]) != null;
    }

    public Intent getLaunchIntent(String packageName, int userId) {
        VPackageManager pm = VPackageManager.get();
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // reuse the intent instance
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = pm.queryIntentActivities(intentToResolve, intentToResolve.resolveType(context), 0, userId);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        ActivityInfo activityInfo = null;
        for (ResolveInfo resolveInfo : ris) {
            if (resolveInfo.activityInfo.enabled) {
                // select the first enabled component
                activityInfo = resolveInfo.activityInfo;
                break;
            }
        }

        if (activityInfo == null) {
            activityInfo = ris.get(0).activityInfo;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(activityInfo.packageName,
                activityInfo.name);
        return intent;
    }

    public boolean createShortcut(int userId, String packageName, OnEmitShortcutListener listener) {
        return createShortcut(userId, packageName, null, listener);
    }

    public class VReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Do nothing
        }
    }

    public boolean createShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = null;
        try {
            setting = getInstalledAppInfo(packageName, 0);
        }catch (Throwable e)
        {
            e.printStackTrace();
            return false;
        }
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name = "";
        Bitmap icon;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            if(sequence != null)
                name = sequence.toString();
            icon = BitmapUtils.drawableToBitmap(appInfo.loadIcon(pm));
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getNameEx(packageName,name,userId);
            if (newName != null) {
                name = newName;
            }
            Bitmap newIcon = listener.getIcon(icon);
            if (newIcon != null) {
                icon = newIcon;
            }
        }
        try{
            if(icon.getByteCount()>1000000)
            {
                // 图标过大
                return false;
            }
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = new Intent();
        shortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
        shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if (splash != null) {
            shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
        }
        shortcutIntent.putExtra("_VA_|_intent_", targetIntent);
        shortcutIntent.putExtra("_VA_|_uri_", targetIntent.toUri(0));
        shortcutIntent.putExtra("_VA_|_user_id_", userId);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(Context.SHORTCUT_SERVICE);
            if(shortcutManager==null)return false;
            if(shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo info = null;
                try
                {
                    Intent shortcutInfoIntent = new Intent();
                    shortcutInfoIntent.setAction("com.sk.vxpmain");
                    shortcutInfoIntent.putExtra("pArgsToLaunch", packageName);
                    shortcutInfoIntent.putExtra("dwUserID", userId);

                    info = new ShortcutInfo.Builder(context, targetIntent.toUri(0) + userId)
                            .setIcon(Icon.createWithBitmap(icon))
                            .setShortLabel(name)
                            .setIntent(shortcutInfoIntent)
                            .build();
                    //当添加快捷方式的确认弹框弹出来时，将被回调
                    PendingIntent shortcutCallbackIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, VReceiver.class),
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    shortcutManager.requestPinShortcut(info, shortcutCallbackIntent.getIntentSender());
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
            }
        }
        else
        {
            try{
                Intent addIntent = new Intent();
                // 内置内容
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                addIntent.putExtra("duplicate", true);
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
                addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                context.sendBroadcast(addIntent);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
        return true;
    }

    public boolean removeShortcut(int userId, String packageName, Intent splash, OnEmitShortcutListener listener) {
        InstalledAppInfo setting = getInstalledAppInfo(packageName, 0);
        if (setting == null) {
            return false;
        }
        ApplicationInfo appInfo = setting.getApplicationInfo(userId);
        PackageManager pm = context.getPackageManager();
        String name;
        try {
            CharSequence sequence = appInfo.loadLabel(pm);
            name = sequence.toString();
        } catch (Throwable e) {
            return false;
        }
        if (listener != null) {
            String newName = listener.getNameEx(packageName,name,userId);
            if (newName != null) {
                name = newName;
            }
        }
        Intent targetIntent = getLaunchIntent(packageName, userId);
        if (targetIntent == null) {
            return false;
        }
        Intent shortcutIntent = new Intent();
        shortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
        shortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
        if (splash != null) {
            shortcutIntent.putExtra("_VA_|_splash_", splash.toUri(0));
        }
        shortcutIntent.putExtra("_VA_|_intent_", targetIntent);
        shortcutIntent.putExtra("_VA_|_uri_", targetIntent.toUri(0));
        shortcutIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
        context.sendBroadcast(addIntent);
        return true;
    }

    public abstract static class UiCallback extends IUiCallback.Stub {
    }

    public void setUiCallback(Intent intent, IUiCallback callback) {
        if (callback != null) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_ui_callback_", callback.asBinder());
            intent.putExtra("_VA_|_sender_", bundle);
        }
    }

    public InstalledAppInfo getInstalledAppInfo(String pkg, int flags) {
        try {
            return getService().getInstalledAppInfo(pkg, flags);
        } catch (RemoteException e) {
            // return VirtualRuntime.crash(e);
            return null;
        }
    }

    public int getInstalledAppCount() {
        try {
            return getService().getInstalledAppCount();
        } catch (RemoteException e) {
            e.printStackTrace();
            return 0;
            // return VirtualRuntime.crash(e);
        }
    }

    public boolean isStartup() {
        return isStartUp;
    }

    public boolean uninstallPackageAsUser(String pkgName, int userId) {
        try {
            try{
                RenameAppUtils.undoRenameByUid(pkgName, userId);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
            return getService().uninstallPackageAsUser(pkgName, userId);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public boolean uninstallPackage(String pkgName) {
        try{
            RenameAppUtils.undoRenameByUid(pkgName, 0);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        try
        {
            SKVPackageNotificationHook hHook = new SKVPackageNotificationHook();
            hHook.CleanPackage(pkgName);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
        try{
            File ext = new File(context.getCacheDir(),"v_user");
            ext = new File(ext, pkgName);
            if(ext.exists())
            {
                if(!ext.delete())ext.deleteOnExit();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        getPkgLocation.removeLocFromPkg(pkgName);
        XposedUtils.clearXposed(pkgName);
        try {
            return getService().uninstallPackage(pkgName);
        } catch (RemoteException e) {
            // Ignore
        }
        return false;
    }

    public Resources getResources(String pkg) throws Resources.NotFoundException {
        InstalledAppInfo installedAppInfo = getInstalledAppInfo(pkg, 0);
        if (installedAppInfo != null) {
            AssetManager assets = mirror.android.content.res.AssetManager.ctor.newInstance();
            mirror.android.content.res.AssetManager.addAssetPath.call(assets, installedAppInfo.apkPath);
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        throw new Resources.NotFoundException(pkg);
    }

    public synchronized ActivityInfo resolveActivityInfo(Intent intent, int userId) {
        ActivityInfo activityInfo = null;
        if (intent.getComponent() == null) {
            ResolveInfo resolveInfo = VPackageManager.get().resolveIntent(intent, intent.getType(), 0, userId);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                activityInfo = resolveInfo.activityInfo;
                intent.setClassName(activityInfo.packageName, activityInfo.name);
            }
        } else {
            activityInfo = resolveActivityInfo(intent.getComponent(), userId);
        }
        if (activityInfo != null) {
            if (activityInfo.targetActivity != null) {
                ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
                activityInfo = VPackageManager.get().getActivityInfo(componentName, 0, userId);
                intent.setComponent(componentName);
            }
        }
        return activityInfo;
    }

    public ActivityInfo resolveActivityInfo(ComponentName componentName, int userId) {
        return VPackageManager.get().getActivityInfo(componentName, 0, userId);
    }

    public ServiceInfo resolveServiceInfo(Intent intent, int userId) {
        ServiceInfo serviceInfo = null;
        ResolveInfo resolveInfo = VPackageManager.get().resolveService(intent, intent.getType(), 0, userId);
        if (resolveInfo != null) {
            serviceInfo = resolveInfo.serviceInfo;
        }
        return serviceInfo;
    }

    public void killApp(String pkg, int userId) {
        VActivityManager.get().killAppByPkg(pkg, userId);
    }

    public void killAllApps() {
        VActivityManager.get().killAllApps();
    }

    public List<InstalledAppInfo> getInstalledApps(int flags) {
        try {
            return getService().getInstalledApps(flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public List<InstalledAppInfo> getInstalledAppsAsUser(int userId, int flags) {
        try {
            return getService().getInstalledAppsAsUser(userId, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void clearAppRequestListener() {
        try {
            getService().clearAppRequestListener();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void scanApps() {
        try {
            getService().scanApps();
        } catch (RemoteException e) {
            // Ignore
        }
    }

    public IAppRequestListener getAppRequestListener() {
        try {
            return getService().getAppRequestListener();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void setAppRequestListener(final AppRequestListener listener) {
        IAppRequestListener inner = new IAppRequestListener.Stub() {
            @Override
            public void onRequestInstall(final String path) {
                VirtualRuntime.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestInstall(path);
                    }
                });
            }

            @Override
            public void onRequestUninstall(final String pkg) {
                VirtualRuntime.getUIHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onRequestUninstall(pkg);
                    }
                });
            }
        };
        try {
            getService().setAppRequestListener(inner);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean isPackageLaunched(int userId, String packageName) {
        try {
            return getService().isPackageLaunched(userId, packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
            // return VirtualRuntime.crash(e);
        }
    }

    public void setPackageHidden(int userId, String packageName, boolean hidden) {
        try {
            getService().setPackageHidden(userId, packageName, hidden);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public boolean installPackageAsUser(int userId, String packageName) {
        try {
            return getService().installPackageAsUser(userId, packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
            // return VirtualRuntime.crash(e);
        }
    }

    public boolean isAppInstalledAsUser(int userId, String packageName) {
        try {
            return getService().isAppInstalledAsUser(userId, packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
            // return VirtualRuntime.crash(e);
        }
    }

    public int[] getPackageInstalledUsers(String packageName) {
        try {
            return getService().getPackageInstalledUsers(packageName);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public abstract static class PackageObserver extends IPackageObserver.Stub {
    }

    public void registerObserver(IPackageObserver observer) {
        try {
            getService().registerObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public void unregisterObserver(IPackageObserver observer) {
        try {
            getService().unregisterObserver(observer);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public boolean isOutsideInstalled(String packageName) {
        try {
            return unHookPackageManager.getApplicationInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            // Ignore
        }
        return false;
    }


    public int getSystemPid() {
        return systemPid;
    }

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Virtual app process
         */
        VAppClient,
        /**
         * Main process
         */
        Main,
        /**
         * Child process
         */
        CHILD
    }

    public interface AppRequestListener {
        void onRequestInstall(String path);

        void onRequestUninstall(String pkg);
    }

    public interface OnEmitShortcutListener {
        Bitmap getIcon(Bitmap originIcon);

        String getName(String originName);

        String getNameEx(String packageName,String pkgName,int uid);
    }

    public static abstract class VirtualInitializer {
        public void onMainProcess() {
        }

        public void onVirtualProcess() {
        }

        public void onServerProcess() {
        }

        public void onChildProcess() {
        }
    }
}
