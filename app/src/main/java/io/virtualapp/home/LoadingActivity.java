package io.virtualapp.home;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.ipc.VPackageManager;
import com.lody.virtual.server.pm.parser.VPackage;
import com.sk.verify.SKAppLock;
import com.sk.verify.SKAppLockCallBack;
import com.sk.verify.SKLockUtil;
import com.sk.verify.msVerify;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;
import sk.vpkg.provider.BanNotificationProvider;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private static final String PKG_NAME_ARGUMENT = "MODEL_ARGUMENT";
    private static final String KEY_INTENT = "KEY_INTENT";
    private static final String KEY_USER = "KEY_USER";
    private static final String TAG = "LoadingActivity";
    private PackageAppData appModel;
    // private EatBeansView loadingView;

    public static void launch(Context context, String packageName, int userId) {
        try
        {
            Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
            if (intent != null)
            {
                Intent loadingPageIntent = new Intent(context, LoadingActivity.class);
                loadingPageIntent.putExtra(PKG_NAME_ARGUMENT, packageName);
                loadingPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                loadingPageIntent.putExtra(KEY_INTENT, intent);
                loadingPageIntent.putExtra(KEY_USER, userId);
                context.startActivity(loadingPageIntent);
            }
        }
        catch(Throwable e)
        {
            // ignored.
            e.printStackTrace();
        }
    }

    private boolean fixOrientation(){
        try {
            Field field = Activity.class.getDeclaredField("mActivityInfo");
            field.setAccessible(true);
            ActivityInfo o = (ActivityInfo)field.get(this);
            if (o != null)
            {
                o.screenOrientation = -1;
            }
            field.setAccessible(false);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    Intent intentBuffer = null;
    int uidBuffer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean result = fixOrientation();
            Log.i("LoadingActivity","Fixed Orientation"+result);
        }
        super.onCreate(savedInstanceState);
        try {
            start = SystemClock.elapsedRealtime();
            if(!(new msVerify().chkSign(getResources().getString(R.string.about_info))))
            {finish();return;}
            setContentView(R.layout.activity_loading);
            // loadingView = (EatBeansView) findViewById(R.id.loading_anim);
            int userId = getIntent().getIntExtra(KEY_USER, -1);
            String pkg = getIntent().getStringExtra(PKG_NAME_ARGUMENT);
            appModel = PackageAppDataStorage.get().acquire(pkg);
            ImageView iconView = (ImageView) findViewById(R.id.app_icon);
            iconView.setImageDrawable(appModel.icon);
            TextView nameView = (TextView) findViewById(R.id.app_name);
            nameView.setText(String.format(Locale.CHINESE, "正在打开 %s...", appModel.name));
            Button hButton = findViewById(R.id.cancel_loading_btn);
            hButton.setOnClickListener(view -> LoadingActivity.this.finish());
            Intent intent = getIntent().getParcelableExtra(KEY_INTENT);
            intentBuffer = intent;
            uidBuffer= userId;
            if (intent == null) {
                Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            VirtualCore.get().setUiCallback(intent, mUiCallback);
            /*
            VUiKit.defer().when(() -> {
                if (!appModel.fastOpen) {
                    try {
                        VirtualCore.get().preOpt(appModel.packageName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                VActivityManager.get().startActivity(intent, userId);
            });
            */
            try {
                // 如果已经在运行了，那么直接拉起，不做任何检测。
                boolean uiRunning = false;
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                if (am != null) {
                    List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
                    for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
                        String appProcessName = VActivityManager.get().getAppProcessName(runningAppProcess.pid);
                        if (TextUtils.equals(appProcessName, pkg)) {
                            uiRunning = true;
                            break;
                        }
                    }
                }
                if (uiRunning) {
                    launchActivity(intent, userId);
                    return;
                }
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }

            if(SKLockUtil.isAppNeedPwd(appModel.packageName,0))
            {
                Intent vIntent = new Intent(this, SKAppLock.class);
                vIntent.putExtra(SKAppLock.ExtAppNameTag,appModel.name);
                vIntent.putExtra(SKAppLock.ExtAppPkgTag,appModel.packageName);
                SKAppLock.callbackX = xLockCallback;
                startActivity(vIntent);
            }
            else
            {
                checkAndLaunch(intent, userId);
            }
        }catch (Throwable e)
        {
            e.printStackTrace();
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private Intent intentToLaunch;
    private int userToLaunch;
    private static final int REQUEST_PERMISSION_CODE = 192;

    private void checkAndLaunch(Intent intent, int userId) {
        final int RUNTIME_PERMISSION_API_LEVEL = android.os.Build.VERSION_CODES.M;

        if (android.os.Build.VERSION.SDK_INT < RUNTIME_PERMISSION_API_LEVEL) {
            // the device is below Android M, the permissions are granted when install, start directly
            Log.i(TAG, "device's api level below Android M, do not need runtime permission.");
            launchActivityWithDelay(intent, userId);
            return;
        }

        // The device is above android M, support runtime permission.
        String packageName = appModel.packageName;
        String name = appModel.name;

        // analyze permission
        try {
            ApplicationInfo applicationInfo = VPackageManager.get().getApplicationInfo(packageName, 0, 0);
            int targetSdkVersion = applicationInfo.targetSdkVersion;

            if (targetSdkVersion >= RUNTIME_PERMISSION_API_LEVEL) {
                launchActivityWithDelay(intent, userId);
            } else {

                intentToLaunch = intent;
                userToLaunch = userId;

                PackageInfo packageInfo = VPackageManager.get().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS, 0);
                String[] requestedPermissions = packageInfo.requestedPermissions;

                Set<String> dangerousPermissions = new HashSet<>();
                for (String requestedPermission : requestedPermissions) {
                    if (VPackage.PermissionComponent.DANGEROUS_PERMISSION.contains(requestedPermission)) {
                        // dangerous permission, check it
                        if (ContextCompat.checkSelfPermission(this, requestedPermission) != PackageManager.PERMISSION_GRANTED) {
                            dangerousPermissions.add(requestedPermission);
                        }
                    }
                }

                if (dangerousPermissions.isEmpty()) {
                    launchActivityWithDelay(intent, userId);
                } else {
                    AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                            .setTitle("Permission request")
                            .setMessage("You must allow those perm for launch this app")
                            .setPositiveButton("Confirm", (dialog, which) -> {
                                String[] permissionToRequest = dangerousPermissions.toArray(new String[dangerousPermissions.size()]);
                                try {
                                    ActivityCompat.requestPermissions(this, permissionToRequest, REQUEST_PERMISSION_CODE);
                                } catch (Throwable ignored) {
                                }
                            })
                            .create();
                    try {
                        alertDialog.show();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Throwable e) {
            launchActivityWithDelay(intent, userId);
        }
    }

    private long start;

    private void launchActivity(Intent intent, int userId) {
        try {
            //VLog.d("LoadingActivity","launchActivity uid "+userId);
            VActivityManager.get().startActivity(intent, userId);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void launchActivityWithDelay(Intent intent, int userId) {
        final int MAX_WAIT = 500;
        long delta = SystemClock.elapsedRealtime() - start;
        long waitTime = MAX_WAIT - delta;

        if (waitTime <= 0) {
            launchActivity(intent, userId);
        } else {
            VUiKit.defer().when(()->
            {
                try
                {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }).done((result)
                    ->
                    launchActivity(intent, userId)
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                if (intentToLaunch == null) {
                    Toast.makeText(this, "启动出错", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                } else {
                    launchActivityWithDelay(intentToLaunch, userToLaunch);
                }
            } else {
                // 提示用户，targetSdkVersion < 23 无法使用运行时权限
                final String tag = "permission_tips_" + appModel.packageName.replaceAll("\\.", "_");
                if (BanNotificationProvider.getString(this,tag)==null) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage("You denied permission!"+appModel.name)
                            .setPositiveButton("OK", (dialog, which) -> {
                                BanNotificationProvider.save(this,tag,"checked");
                                launchActivityWithDelay(intentToLaunch, userToLaunch);
                                finish();
                            })
                            .create();
                    try {
                        alertDialog.show();
                    } catch (Throwable ignored) {
                        // BadTokenException.
                        Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    launchActivityWithDelay(intentToLaunch, userToLaunch);
                    finish();
                }
            }
        }
    }


    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
            finish();
        }
    };

    private final SKAppLockCallBack xLockCallback = new SKAppLockCallBack()
    {
        @Override
        public void afterThingDone()
        {
            checkAndLaunch(intentBuffer,uidBuffer);
        }

        @Override
        public void afterFailed()
        {
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // loadingView.startAnim();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // loadingView.stopAnim();
    }
}
