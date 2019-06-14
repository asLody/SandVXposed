package io.virtualapp.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.sk.verify.msVerify;

import java.lang.reflect.Field;
import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private static final String PKG_NAME_ARGUMENT = "MODEL_ARGUMENT";
    private static final String KEY_INTENT = "KEY_INTENT";
    private static final String KEY_USER = "KEY_USER";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            boolean result = fixOrientation();
            Log.i("LoadingActivity","Fixed Orientation"+result);
        }
        super.onCreate(savedInstanceState);
        try {
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
            if (intent == null) {
                Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            VirtualCore.get().setUiCallback(intent, mUiCallback);
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
        }catch (Throwable e)
        {
            e.printStackTrace();
            Toast.makeText(this, R.string.launch_failed, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) throws RemoteException {
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
