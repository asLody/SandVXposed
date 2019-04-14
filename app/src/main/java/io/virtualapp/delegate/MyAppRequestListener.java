package io.virtualapp.delegate;

import android.content.Context;
import android.widget.Toast;

import com.lody.virtual.client.core.InstallStrategy;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.remote.InstallResult;

import java.io.IOException;

import io.virtualapp.R;
import io.virtualapp.VApp;

/**
 * @author Lody
 */

public class MyAppRequestListener implements VirtualCore.AppRequestListener {

    private final Context context;

    public MyAppRequestListener(Context context) {
        this.context = context;
    }

    @Override
    public void onRequestInstall(String path) {
        android.widget.Toast.makeText(VApp.getApp(), "正在安装: " + path+"，"+R.string.SKRestartTips, Toast.LENGTH_LONG).show();
        InstallResult res = VirtualCore.get().installPackage(path, InstallStrategy.UPDATE_IF_EXIST);
        if (res.isSuccess) {
            try {
                VirtualCore.get().preOpt(res.packageName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (res.isUpdate) {
                Toast.makeText(context, "更新: " + res.packageName + " 成功!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "安装: " + res.packageName + " 成功!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "安装失败: " + res.error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestUninstall(String pkg) {
        Toast.makeText(context, "卸载: " + pkg, Toast.LENGTH_SHORT).show();

    }
}
