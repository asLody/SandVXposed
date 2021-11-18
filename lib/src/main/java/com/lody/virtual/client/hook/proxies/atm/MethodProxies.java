package com.lody.virtual.client.hook.proxies.atm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.utils.MethodParameterUtils;
import com.lody.virtual.client.ipc.ActivityClientRecord;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.ChooserActivity;
import com.lody.virtual.helper.compat.ActivityManagerCompat;
import com.lody.virtual.helper.compat.BuildCompat;
import com.lody.virtual.helper.utils.ArrayUtils;
import com.lody.virtual.helper.utils.ComponentUtils;
import com.lody.virtual.helper.utils.FileUtils;
import com.lody.virtual.helper.utils.VLog;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.interfaces.IAppRequestListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import static com.lody.virtual.client.stub.VASettings.INTERCEPT_BACK_HOME;

//
// Created by Swift Gan on 2019/3/18.
//

// Add Q Support by Saurik on 2019/11/27
public class MethodProxies {
    static class AvoidOverridePendingTransition extends MethodProxy
    {
        @Override
        public String getMethodName()
        {
            return "overridePendingTransition";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable
        {
            if("com.tencent.mm".equals(args[1]))
            {
                try
                {
                    args[2] = android.R.anim.slide_in_left;
                    args[3] = android.R.anim.slide_out_right;
                    return method.invoke(who, args);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    return null;
                }
            }
            args[2] = android.R.anim.fade_in;
            args[3] = android.R.anim.fade_out;
            return method.invoke(who,args);
        }
    }

    static class GetCallingPackage extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getCallingPackage";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            IBinder token = (IBinder) args[0];
            return VActivityManager.get().getCallingPackage(token);
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class activityDestroyed extends MethodProxy
    {
        @Override
        public String getMethodName()
        {
            return "activityDestroyed";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable
        {
            IBinder token = (IBinder) args[0];
            VActivityManager.get().onActivityDestroy(token);
            return super.call(who, method, args);
        }
    }

    static class activityResumed extends MethodProxy
    {

        @Override
        public String getMethodName()
        {
            return "activityResumed";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable
        {
            IBinder token = (IBinder) args[0];
            VActivityManager.get().onActivityResumed(token);
            return super.call(who, method, args);
        }
    }

    static class finishActivity extends MethodProxy
    {

        @Override
        public String getMethodName()
        {
            return "finishActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable
        {
            IBinder token = (IBinder) args[0];
            VActivityManager.get().finishActivity(token);
            return super.call(who, method, args);
        }
    }

    static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";
        private static final String SCHEME_CONTENT = "content";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            VLog.d("Q_M", "---->StartActivity 类");

            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                VLog.d("Q_M", "---->intentIndex < 0");
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            Intent intent = (Intent) args[intentIndex];
            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;
            int userId = VUserHandle.myUserId();

            if (ComponentUtils.isStubComponent(intent)) {
                VLog.d("Q_M", "---->ComponentUtils.isStubComponent");
                return method.invoke(who, args);
            }

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                VLog.d("Q_M", "---->application/vnd.android.package-archive");
                if (handleInstallRequest(intent)) {
                    return 0;
                }
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {
                VLog.d("Q_M", "---->package");
                if (handleUninstallRequest(intent)) {
                    return 0;
                }
            }

            String resultWho = null;
            int requestCode = 0;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent.setComponent(new ComponentName(getHostContext(), ChooserActivity.class));
                intent.putExtra(Constants.EXTRA_USER_HANDLE, userId);
                intent.putExtra(ChooserActivity.EXTRA_DATA, options);
                intent.putExtra(ChooserActivity.EXTRA_WHO, resultWho);
                intent.putExtra(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }
            if (intent.getScheme() != null && intent.getScheme().equals(SCHEME_PACKAGE) && intent.getData() != null) {
                if (intent.getAction() != null && intent.getAction().startsWith("android.settings.")) {
                    intent.setData(Uri.parse("package:" + getHostPkg()));
                }
            }

            ActivityInfo activityInfo = VirtualCore.get().resolveActivityInfo(intent, userId);
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : " + intent);

                VLog.d("Q_M", "---->StartActivity who=" + who);
                VLog.d("Q_M", "---->StartActivity intent=" + intent);
                VLog.d("Q_M", "---->StartActivity resultTo=" + resultTo);

                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }

                if (INTERCEPT_BACK_HOME && Intent.ACTION_MAIN.equals(intent.getAction())
                        && intent.getCategories().contains("android.intent.category.HOME")
                        && resultTo != null) {
                    VActivityManager.get().finishActivity(resultTo);
                    return 0;
                }

                if(BuildCompat.isR())
                {
                    MethodParameterUtils.replaceFirstAppPkg(args);
                }

                return method.invoke(who, args);
            }
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, VUserHandle.myUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VLog.d("Q_M", "---->sendActivityResult to=" + resultTo+ " who "+resultWho
                        +" code "+requestCode);
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
            if (resultTo != null) {
                ActivityClientRecord r = VActivityManager.get().getActivityRecord(resultTo);
                if (r != null && r.activity != null) {
                    try {
                        TypedValue out = new TypedValue();
                        Resources.Theme theme = r.activity.getResources().newTheme();
                        theme.applyStyle(activityInfo.getThemeResource(), true);

                        VLog.d("Q_M", "---->StartActivity Anim=" + activityInfo.getThemeResource());
                        if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                            TypedArray array = theme.obtainStyledAttributes(out.data,
                                    new int[]{
                                            android.R.attr.activityOpenEnterAnimation,
                                            android.R.attr.activityOpenExitAnimation
                                    });

                            r.activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                            array.recycle();
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
            return res;
        }


        private boolean handleInstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    try {
                        listener.onRequestInstall(sourceFile.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                    try {
                        inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                        outputStream = new FileOutputStream(sharedFileCopy);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.closeQuietly(inputStream);
                        FileUtils.closeQuietly(outputStream);
                    }
                    try {
                        listener.onRequestInstall(sharedFileCopy.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    try {
                        listener.onRequestUninstall(pkg);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

    }

}
