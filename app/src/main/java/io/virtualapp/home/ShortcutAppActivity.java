package io.virtualapp.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;

public class ShortcutAppActivity extends AppCompatActivity
{
    private static void launchActivity(String szPackageName, Integer nUserId)
    {
        try {
            Intent intent = VirtualCore.get().getLaunchIntent(szPackageName, nUserId);
            intent.setSelector(null);
            VActivityManager.get().startActivity(intent, nUserId);
        } catch (Throwable ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        finish();
        Bundle lpBundle = getIntent().getExtras();
        if(lpBundle!=null)
        {
            try
            {
                // Fix android O shortcut by Saurik QQ 384550791
                String szPackage = lpBundle.getString("pArgsToLaunch");
                if (szPackage == null)
                {
                    throw new ClassNotFoundException("Can not found args");
                }
                int dwUserID = lpBundle.getInt("dwUserID");
                //XAppDataInstalled lpAppInfo = new XAppDataInstalled();
                //lpAppInfo.pkgName = pIntentInvoke;
                // LoadingActivity.launch(ShortcutAppActivity.this,pIntentInvoke,dwUserID);
                launchActivity(szPackage,dwUserID);
                return;
            }
            catch(Throwable e)
            {
                e.printStackTrace();
            }
        }
        launchActivity("com.tencent.mm",0);
    }
}
