package io.virtualapp.home;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;

import io.virtualapp.R;
import io.virtualapp.VApp;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.home.adapters.AppChooseAct;
import io.virtualapp.home.adapters.AppPagerAdapter;
import io.virtualapp.home.models.AppInfoLite;
import sk.vpkg.provider.BanNotificationProvider;

/**
 * @author Lody
 */
public class ListAppActivity extends VActivity {

    private Toolbar mToolBar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;

    public final static int resultCore = 0x2e;

    public static void gotoListApp(Activity activity) {
        Intent intent = new Intent(activity, ListAppActivity.class);
        activity.startActivityForResult(intent, VCommends.REQUEST_SELECT_APP);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode!= AppChooseAct.resultCore)return;
        if(getActivity()==null)return;
        if(data==null)return;
        try
        {
            String sPath = data.getStringExtra("path");
            if(sPath==null)return;
            Intent d = new Intent();
            ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>();
            PackageInfo pkgInfo = null;
            try {
                pkgInfo = getActivity().getPackageManager().getPackageArchiveInfo(
                        sPath, 0);
            } catch (Exception e) {
                // Ignore
            }
            String szPkg = "com.sk.sp";
            if(pkgInfo!=null)
            {
                if(pkgInfo.packageName!=null)
                    szPkg = pkgInfo.packageName;
            }
            dataList.add(new AppInfoLite(szPkg, sPath, false));
            d.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
            getActivity().setResult(Activity.RESULT_OK, d);
            getActivity().finish();
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorPrimaryDark)));
        setContentView(R.layout.activity_clone_app);
        mToolBar = (Toolbar) findViewById(R.id.clone_app_tool_bar);
        mTabLayout = (TabLayout) mToolBar.findViewById(R.id.clone_app_tab_layout);
        mViewPager = (ViewPager) findViewById(R.id.clone_app_view_pager);
        setupToolBar();
        mViewPager.setAdapter(new AppPagerAdapter(getSupportFragmentManager()));
        mTabLayout.setupWithViewPager(mViewPager);
        // Request permission to access external storage
        String szEnableRedirectStorage = BanNotificationProvider.getString(VApp.getApp(),"enablePackageScan");
        if(szEnableRedirectStorage != null)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, 0);
                }
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                }
            }
        }
    }

    private void setupToolBar() {
        setSupportActionBar(mToolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                mViewPager.setAdapter(new AppPagerAdapter(getSupportFragmentManager()));
                break;
            }
        }
    }
}
