package io.virtualapp.splash;

import android.os.Bundle;
import android.view.WindowManager;

import com.lody.virtual.client.core.VirtualCore;
import com.sk.verify.msVerify;
import com.tencent.stat.StatConfig;
import com.tencent.stat.StatService;

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
            HomeActivity.goHome(this);
            finish();
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
            HomeActivity.goHome(this);
            // 腾讯用户统计
            // [可选]设置是否打开debug输出，上线时请关闭，Logcat标签为"MtaSDK"
            StatConfig.setDebugEnable(false);
            // 基础统计API
            StatService.registerActivityLifecycleCallbacks(this.getApplication());
            is_initialized = true;
            finish();
        });
    }


    private void doActionInThread() {
        if (!VirtualCore.get().isEngineLaunched()) {
            VirtualCore.get().waitForEngine();
        }
    }
}
