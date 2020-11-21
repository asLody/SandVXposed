package io.virtualapp.splash;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.mobstat.StatService;
import com.lody.virtual.client.core.VirtualCore;
import com.sk.desktop.SKDesktop;
import com.sk.verify.msVerify;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.FlurryROMCollector;
import io.virtualapp.home.HomeActivity;
import jonathanfinerty.once.Once;

import static com.sk.verify.msVerify.chkIsCotainsMyQQ;

public class SplashActivity extends VActivity {

    static private boolean is_initialized = false;

    private void toDesktop()
    {
        if(Once.beenDone("useNewDesktop"))
        {
            HomeActivity.goHome(this);
        }
        else
        {
            SKDesktop.initDesktop(this);
        }
    }

    private void bindAndInit()
    {
        toDesktop();
        bindMTA();
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!chkIsCotainsMyQQ(getResources().getString(R.string.about_info))
        || !(new msVerify().chkSign(getResources().getString(R.string.about_info))))
        {
            finish();
            return;
        }

        if(is_initialized)
        {
            try
            {
                initMTA();
            }catch (Throwable e)
            {
                e.printStackTrace();
            }
            return;
        }
        @SuppressWarnings("unused")
        boolean enterGuide = !Once.beenDone(Once.THIS_APP_INSTALL, VCommends.TAG_NEW_VERSION);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        VUiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            if (!Once.beenDone("collect_flurry")) {
                FlurryROMCollector.startCollect();
                Once.markDone("collect_flurry");
            }
            doActionInThread();
            com.sk.SKAppLoad.InitApp.InitVApp();
            time = System.currentTimeMillis() - time;
            long delta = 5000L - time;
            if (delta > 0) {
                VUiKit.sleep(delta);
            }
        }).done((res) -> {
            try{
                is_initialized = true;
                initMTA();
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        });
    }

    private void appLikeOnCreate()
    {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.KILL_BACKGROUND_PROCESSES)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.KILL_BACKGROUND_PROCESSES)||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.KILL_BACKGROUND_PROCESSES,
                                Manifest.permission.READ_PHONE_STATE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        ResultGen);
            }
        }
        else
        {
            bindAndInit();
        }
    }

    private void initMTA()
    {
        if(!Once.beenDone("user_privacy_1"))
        {
            AlertDialog.Builder hBuilder = new AlertDialog.Builder(this);
            hBuilder.setTitle(R.string.user_privacy_policy);
            hBuilder.setMessage(R.string.user_privacy_policy_detail);
            hBuilder.setCancelable(false);
            hBuilder.setNegativeButton(R.string.back, (dialogInterface, i) -> finish());
            hBuilder.setPositiveButton(R.string.accept, (dialogInterface, i) ->
            {
                Once.markDone("user_privacy_1");
                appLikeOnCreate();
            });
            hBuilder.create().show();
        }
        else
        {
            appLikeOnCreate();
        }
    }

    private void bindMTA()
    {
        try{
            StatService.setAuthorizedState(this, true);
            StatService.start(this);
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static final public int ResultGen = 0x80;

    @Override
    public void onRequestPermissionsResult(int ret,
                                           String permissions[], int[] grantResults)
    {
        if (ret == ResultGen)
        {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                bindAndInit();
            }
            else
            {
                finish();
            }
        }
    }


    private void doActionInThread() {
        if (!VirtualCore.get().isEngineLaunched()) {
            VirtualCore.get().waitForEngine();
        }
    }
}
