package io.virtualapp.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.Toast;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;
import com.lody.virtual.remote.InstallResult;
import com.lody.virtual.remote.InstalledAppInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.models.safePackage;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.home.repo.PackageAppDataStorage;
import io.virtualapp.home.repo.XAppDataInstalled;
import jonathanfinerty.once.Once;
import sk.vpkg.manager.RenameAppUtils;

/**
 * @author Lody
 */
class HomePresenterImpl implements HomeContract.HomePresenter {

    private HomeContract.HomeView mView;
    private Activity mActivity;
    private AppRepository mRepo;
    private AppData mTempAppData;


    HomePresenterImpl(HomeContract.HomeView view) {
        mView = view;
        mActivity = view.getActivity();
        mRepo = new AppRepository(mActivity);
        mView.setPresenter(this);
    }

    @Override
    public void start() {
        dataChanged();
        if (!Once.beenDone(VCommends.TAG_SHOW_ADD_APP_GUIDE)) {
            mView.showGuide();
            Once.markDone(VCommends.TAG_SHOW_ADD_APP_GUIDE);
        }
        if (!Once.beenDone(VCommends.TAG_ASK_INSTALL_GMS) && GmsSupport.isOutsideGoogleFrameworkExist()) {
            mView.askInstallGms();
            Once.markDone(VCommends.TAG_ASK_INSTALL_GMS);
        }
        mView.showUpdateTips();
    }

    @Override
    public void launchApp(AppData data) {
        try {
            if (data instanceof PackageAppData) {
                PackageAppData appData = (PackageAppData) data;
                appData.isFirstOpen = false;
                LoadingActivity.launch(mActivity, appData.packageName, 0);
            } else if (data instanceof MultiplePackageAppData) {
                MultiplePackageAppData multipleData = (MultiplePackageAppData) data;
                multipleData.isFirstOpen = false;
                LoadingActivity.launch(mActivity, multipleData.appInfo.packageName, ((MultiplePackageAppData) data).userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dataChanged() {
        mView.showLoading();
        mRepo.getVirtualApps().done(mView::loadFinish).fail(mView::loadError);
    }

    @Override
    public void addApp(AppInfoLite info) {
        class AddResult {
            private PackageAppData appData;
            private int userId;
            private boolean justEnableHidden;
        }
        XAppDataInstalled lpXappData = new XAppDataInstalled();
        try
        {
            lpXappData.pkgName = info.packageName;
            mView.addAppToLauncher(lpXappData);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            return;
        }
        AddResult addResult = new AddResult();
        InstalledAppInfo lpInstallInfo = null;
        try {
            lpInstallInfo= VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
        } catch (Throwable e) {
            // ignored
        }
        if(lpInstallInfo!=null)
        {
            if(HomeActivity.hHomeAct!=null) {
                AlertDialog.Builder hBuilder = new AlertDialog.Builder(HomeActivity.hHomeAct);
                hBuilder.setMessage(R.string.ensure_override_app)
                        .setTitle(R.string.virtual_installer)
                        .setPositiveButton(R.string.override_app, (dialog, which) -> VUiKit.defer().when(()-> {
                            // Empty for reserve.
                        }).done((rest)-> VUiKit.defer().when(() -> {
                            addResult.justEnableHidden = false;
                            List<String> pkg_Det = Arrays.asList(safePackage.safe_Package);
                            if (Once.beenDone("disable_safe_mode") || pkg_Det.contains(info.packageName)) {
                                InstallResult res = mRepo.addVirtualApp(info);
                            }
                        }).then((res) -> {
                            try {
                                addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                mView.removeAppToLauncher(lpXappData);
                            }
                        }).done(res -> {
                            boolean multipleVersion = addResult.justEnableHidden && addResult.userId != 0;
                            try {
                                if (addResult.appData.getXposedModule() != null) {
                                    Toast.makeText(mActivity, String.format(mActivity.getString(R.string.module_install_success), addResult.appData.name), Toast.LENGTH_SHORT).show();
                                    mView.removeAppToLauncher(lpXappData);
                                    return;
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                                mView.removeAppToLauncher(lpXappData);
                                return;
                            }
                            mView.removeAppToLauncher(lpXappData);
                            if (!multipleVersion) {
                                PackageAppData data = addResult.appData;
                                data.isLoading = true;
                                mView.addAppToLauncher(data);
                                handleOptApp(data, info.packageName, true);
                            } else {
                                MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                                data.isLoading = true;
                                mView.addAppToLauncher(data);
                                handleOptApp(data, info.packageName, false);
                            }
                        })))
                        .setNegativeButton(R.string.clone_apps, (dialog, which) -> VUiKit.defer().when(()-> {
                            // Empty for reserve.
                        }).done((rest)-> VUiKit.defer().when(() -> {
                            InstalledAppInfo installedAppInfo = null;
                            try {
                                installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                try {
                                    if (HomeActivity.hHomeAct != null)
                                        Toast.makeText(HomeActivity.hHomeAct, R.string.launch_failed, Toast.LENGTH_SHORT)
                                                .show();
                                } catch (Throwable hE) {
                                    // ignored
                                }
                                return;
                            }
                            addResult.justEnableHidden = true;
                            try{
                                int[] userIds = installedAppInfo != null ?
                                        installedAppInfo.getInstalledUsers() : new int[0];
                                int nextUserId = userIds.length;
                                for (int i = 0; i < userIds.length; i++) {
                                    if (userIds[i] != i) {
                                        nextUserId = i;
                                        break;
                                    }
                                }
                                addResult.userId = nextUserId;
                                if (VUserManager.get().getUserInfo(nextUserId) == null) {
                                    String nextUserName = "Space " + (nextUserId + 1);
                                    VUserInfo newUserInfo = VUserManager.get().createUser(nextUserName, VUserInfo.FLAG_ADMIN);
                                    if (newUserInfo == null) {
                                        return;
                                    }
                                }
                                VirtualCore.get().installPackageAsUser(nextUserId, info.packageName);
                            }
                            catch (Throwable installExp)
                            {
                                installExp.printStackTrace();
                            }
                        }).then((res) -> {
                            try {
                                addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
                            } catch (Throwable e) {
                                e.printStackTrace();
                                mView.removeAppToLauncher(lpXappData);
                                return;
                            }
                        }).done(res -> {
                            boolean multipleVersion = addResult.justEnableHidden && addResult.userId != 0;
                            try {
                                if (addResult.appData.getXposedModule() != null) {
                                    Toast.makeText(mActivity, String.format(mActivity.getString(R.string.module_install_success), addResult.appData.name), Toast.LENGTH_SHORT).show();
                                    mView.removeAppToLauncher(lpXappData);
                                    return;
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                                mView.removeAppToLauncher(lpXappData);
                                return;
                            }
                            mView.removeAppToLauncher(lpXappData);
                            if (!multipleVersion) {
                                PackageAppData data = addResult.appData;
                                data.isLoading = true;
                                mView.addAppToLauncher(data);
                                handleOptApp(data, info.packageName, true);
                            } else {
                                MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                                data.isLoading = true;
                                mView.addAppToLauncher(data);
                                handleOptApp(data, info.packageName, false);
                            }
                        })))
                        .setNeutralButton(R.string.back, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    mView.removeAppToLauncher(lpXappData);
                                }
                                catch (Throwable t)
                                {
                                    t.printStackTrace();
                                }
                            }
                        })
                        .create().show();
            }
            else
            {
                VUiKit.defer().when(()-> {
                    // Empty for reserve.
                }).done((rest)-> VUiKit.defer().when(() -> {
                    InstalledAppInfo installedAppInfo = null;
                    try {
                        installedAppInfo = VirtualCore.get().getInstalledAppInfo(info.packageName, 0);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return;
                    }
                    addResult.justEnableHidden = installedAppInfo != null;
                    if (addResult.justEnableHidden) {
                        int[] userIds = installedAppInfo.getInstalledUsers();
                        int nextUserId = userIds.length;
                        for (int i = 0; i < userIds.length; i++) {
                            if (userIds[i] != i) {
                                nextUserId = i;
                                break;
                            }
                        }
                        addResult.userId = nextUserId;
                        if (VUserManager.get().getUserInfo(nextUserId) == null) {
                            String nextUserName = "Space " + (nextUserId + 1);
                            VUserInfo newUserInfo = VUserManager.get().createUser(nextUserName, VUserInfo.FLAG_ADMIN);
                            if (newUserInfo == null) {
                                throw new IllegalStateException();
                            }
                        }
                        List<String> pkg_Det = Arrays.asList(safePackage.safe_Package);
                        if (!Once.beenDone("disable_safe_mode") && !pkg_Det.contains(info.packageName)) {
                            return;
                        } else {
                            boolean success = VirtualCore.get().installPackageAsUser(nextUserId, info.packageName);
                            if (!success) {
                                throw new IllegalStateException();
                            }
                        }
                    } else {
                        List<String> pkg_Det = Arrays.asList(safePackage.safe_Package);
                        if (Once.beenDone("disable_safe_mode") || pkg_Det.contains(info.packageName)) {
                            InstallResult res = mRepo.addVirtualApp(info);
                        }
                    }
                }).then((res) -> {
                    try {
                        addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        mView.removeAppToLauncher(lpXappData);
                        return;
                    }
                }).done(res -> {
                    boolean multipleVersion = addResult.justEnableHidden && addResult.userId != 0;
                    try {
                        if (addResult.appData.getXposedModule() != null) {
                            Toast.makeText(mActivity, String.format(mActivity.getString(R.string.module_install_success), addResult.appData.name), Toast.LENGTH_SHORT).show();
                            mView.removeAppToLauncher(lpXappData);
                            return;
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        mView.removeAppToLauncher(lpXappData);
                        return;
                    }
                    mView.removeAppToLauncher(lpXappData);
                    if (!multipleVersion) {
                        PackageAppData data = addResult.appData;
                        data.isLoading = true;
                        mView.addAppToLauncher(data);
                        handleOptApp(data, info.packageName, true);
                    } else {
                        MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                        data.isLoading = true;
                        mView.addAppToLauncher(data);
                        handleOptApp(data, info.packageName, false);
                    }
                }));
            }
        }
        else
        {
            VUiKit.defer().when(()-> {
                // Empty for reserve.
            }).done((rest)-> VUiKit.defer().when(() -> {
                addResult.justEnableHidden = false;
                List<String> pkg_Det = Arrays.asList(safePackage.safe_Package);
                if (Once.beenDone("disable_safe_mode") || pkg_Det.contains(info.packageName)) {
                    mRepo.addVirtualApp(info);
                }
            }).then((res) -> {
                try {
                    addResult.appData = PackageAppDataStorage.get().acquire(info.packageName);
                } catch (Throwable e) {
                    e.printStackTrace();
                    mView.removeAppToLauncher(lpXappData);
                    try
                    {
                        if(HomeActivity.hHomeAct!=null)
                        {
                            HomeActivity.hHomeAct.RefreshDesktop();
                        }
                    }
                    catch (Throwable px)
                    {
                        px.printStackTrace();
                    }
                }
            }).done(res -> {
                boolean multipleVersion = addResult.justEnableHidden && addResult.userId != 0;
                try {
                    if (addResult.appData.getXposedModule() != null) {
                        Toast.makeText(mActivity, String.format(mActivity.getString(R.string.module_install_success), addResult.appData.name), Toast.LENGTH_SHORT).show();
                        mView.removeAppToLauncher(lpXappData);
                        return;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    mView.removeAppToLauncher(lpXappData);
                    return;
                }
                mView.removeAppToLauncher(lpXappData);
                if (!multipleVersion) {
                    PackageAppData data = addResult.appData;
                    data.isLoading = true;
                    mView.addAppToLauncher(data);
                    handleOptApp(data, info.packageName, true);
                } else {
                    MultiplePackageAppData data = new MultiplePackageAppData(addResult.appData, addResult.userId);
                    data.isLoading = true;
                    mView.addAppToLauncher(data);
                    handleOptApp(data, info.packageName, false);
                }
            }));
        }
    }


    private void handleOptApp(AppData data, String packageName, boolean needOpt) {
        VUiKit.defer().when(() -> {
            long time = System.currentTimeMillis();
            if (needOpt) {
                try {
                    VirtualCore.get().preOpt(packageName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            time = System.currentTimeMillis() - time;
            if (time < 1500L) {
                try {
                    Thread.sleep(1500L - time);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).done((res) -> {
            if (data instanceof PackageAppData) {
                ((PackageAppData) data).isLoading = false;
                ((PackageAppData) data).isFirstOpen = true;
            } else if (data instanceof MultiplePackageAppData) {
                ((MultiplePackageAppData) data).isLoading = false;
                ((MultiplePackageAppData) data).isFirstOpen = true;
            }
            mView.refreshLauncherItem(data);
        });
    }

    @Override
    public void deleteApp(AppData data) {
        try {
            mView.removeAppToLauncher(data);
            if (data instanceof PackageAppData) {
                mRepo.removeVirtualApp(((PackageAppData) data).packageName, 0);
            } else {
                MultiplePackageAppData appData = (MultiplePackageAppData) data;
                mRepo.removeVirtualApp(appData.appInfo.packageName, appData.userId);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createShortcut(AppData data) {
        VirtualCore.OnEmitShortcutListener listener = new VirtualCore.OnEmitShortcutListener() {
            @Override
            public Bitmap getIcon(Bitmap originIcon) {
                return originIcon;
            }

            @Override
            public String getName(String originName) {
                return originName + "(SVX)";
            }

            @Override
            public String getNameEx(String packageName, String origName, int uid)
            {
                if(origName==null||packageName==null)return null;
                String customSuffix = "";
                String customName = RenameAppUtils.getRenamedApp(packageName,uid,origName);
                if(customName.equals(origName))
                {
                    customSuffix += "("+ uid +")";
                }
                return (customName + customSuffix);
            }
        };
        if (data instanceof PackageAppData) {
            VirtualCore.get().createShortcut(0, ((PackageAppData) data).packageName, listener);
        } else if (data instanceof MultiplePackageAppData) {
            MultiplePackageAppData appData = (MultiplePackageAppData) data;
            VirtualCore.get().createShortcut(appData.userId, appData.appInfo.packageName, listener);
        }
    }

}
