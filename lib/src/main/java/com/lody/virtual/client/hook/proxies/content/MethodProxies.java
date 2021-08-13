package com.lody.virtual.client.hook.proxies.content;

import android.content.pm.ApplicationInfo;
import android.database.IContentObserver;
import android.net.Uri;
import android.os.Build;

import com.lody.virtual.client.VClientImpl;
import com.lody.virtual.client.hook.base.MethodProxy;
import com.lody.virtual.client.hook.base.ReplaceLastUidMethodProxy;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.helper.utils.OSUtils;
import com.lody.virtual.os.VUserHandle;
import com.lody.virtual.server.content.VContentService;

import java.lang.reflect.Method;

/*
 * @author: 0xF
 * www.die.lu
 */
class MethodProxies {

    static class NotifyChange extends MethodProxy {

        @Override
        public String getMethodName() {
            return "notifyChange";
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return super.beforeCall(who, method, args);
            }
            ApplicationInfo currentApplicationInfo = VClientImpl.get().getCurrentApplicationInfo();
            if (currentApplicationInfo == null) {
                return super.beforeCall(who, method, args);
            }
            int targetSdkVersion = currentApplicationInfo.targetSdkVersion;

            int length = args.length;
            int index = -1;
            for (int i = 0; i < length; i++) {
                Object obj = args[length - 1];
                if (obj != null && obj.getClass() == Integer.class) {
                    if ((int) obj == targetSdkVersion) {
                        index = i;
                    }
                }
            }
            /*
            In ContentService, it contains this code:

            if (targetSdkVersion >= Build.VERSION_CODES.O) {
                throw new SecurityException(msg);
            } else {
                if (msg.startsWith("Failed to find provider")) {
                    // Sigh, we need to quietly let apps targeting older API
                    // levels notify on non-existent providers.
                } else {
                    Log.w(TAG, "Ignoring notify for " + uri + " from " + uid + ": " + msg);
                    return;
                }
            }
            we just modify the targetSdkVersion dynamic to fake it.
            */
            if (index != -1) {
                args[index] = Build.VERSION_CODES.N_MR1;
            }

            return super.beforeCall(who, method, args);
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            try {
                return super.call(who, method, args);
            } catch (Throwable se) {
                if (se.getCause() instanceof SecurityException && OSUtils.getInstance().isAndroidQ()) {
                    // ignored
                } else {
                    se.printStackTrace();
                }
            }
            return null;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    public static class UnregisterContentObserver extends MethodProxy
    {
        @Override
        public String getMethodName()
        {
            return "unregisterContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable
        {
            if(args==null||args.length==0)return null;
            IContentObserver observer = (IContentObserver) args[0];
            if(observer==null)return null;
            VContentService.get().unregisterContentObserver(observer);
            try{
                return super.call(who, method, args);
            }catch (Exception e)
            {
                e.printStackTrace();
                if(!(e.getCause() instanceof SecurityException))throw e;
            }
            return null;
        }
    }

    static class RegisterContentObserver extends ReplaceLastUidMethodProxy {
        private static final String TAG = RegisterContentObserver.class.getSimpleName();

        public RegisterContentObserver() {
            super("registerContentObserver");
        }

        @Override
        public String getMethodName() {
            return "registerContentObserver";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (args.length >= 5) {
                    args[4] = Build.VERSION_CODES.LOLLIPOP_MR1;
                }
            }
            Uri uri = (Uri) args[0];
            boolean notifyForDescendents = (boolean) args[1];
            IContentObserver observer = (IContentObserver) args[2];
            if (VPackageManager.get().resolveContentProvider(uri.getAuthority(),
                    0, VUserHandle.myUserId())!=null) {
                VContentService.get().registerContentObserver(uri, notifyForDescendents, observer);
                return 0;
            } else {
                try{
                    return super.call(who, method, args);
                }catch (Exception e)
                {
                    e.printStackTrace();
                    if(!(e.getCause() instanceof SecurityException))throw e;
                }
                return null;
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess() && OSUtils.getInstance().isAndroidQ();
        }
    }

}