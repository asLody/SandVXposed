package io.virtualapp.home;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.GmsSupport;
import com.lody.virtual.client.core.RomChecker;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.lody.virtual.client.stub.ChooseTypeAndAccountActivity;
import com.lody.virtual.os.VUserInfo;
import com.lody.virtual.os.VUserManager;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.VCommends;
import io.virtualapp.abs.nestedadapter.SmartRecyclerAdapter;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.LaunchpadAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.location.VirtualLocationSettings;
import io.virtualapp.home.models.AddAppButton;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.AppInfoLite;
import io.virtualapp.home.models.EmptyAppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.models.safePackage;
import io.virtualapp.home.repo.XAppDataInstalled;
import io.virtualapp.widgets.TwoGearsView;
import jonathanfinerty.once.Once;

import static android.support.v7.widget.helper.ItemTouchHelper.ACTION_STATE_DRAG;
import static android.support.v7.widget.helper.ItemTouchHelper.DOWN;
import static android.support.v7.widget.helper.ItemTouchHelper.END;
import static android.support.v7.widget.helper.ItemTouchHelper.LEFT;
import static android.support.v7.widget.helper.ItemTouchHelper.RIGHT;
import static android.support.v7.widget.helper.ItemTouchHelper.START;
import static android.support.v7.widget.helper.ItemTouchHelper.UP;

/**
 * @author Lody
 */
public class HomeActivity extends VActivity implements HomeContract.HomeView {

    private static final String TAG = HomeActivity.class.getSimpleName();

    private HomeContract.HomePresenter mPresenter;
    private TwoGearsView mLoadingView;
    private RecyclerView mLauncherView;
    private View mMenuView;
    private PopupMenu mPopupMenu;
    private View mBottomArea;
    private View mCreateShortcutBox;
    private TextView mCreateShortcutTextView;
    private View mDeleteAppBox;
    private TextView mDeleteAppTextView;
    private LaunchpadAdapter mLaunchpadAdapter;
    private Handler mUiHandler;
    private SearchView mSearchView;
    private List<AppData> listAppData;


    public static void goHome(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        overridePendingTransition(0, 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mUiHandler = new Handler(Looper.getMainLooper());
        bindViews();
        initLaunchpad();
        initMenu();
        hHomeAct=this;
        new HomePresenterImpl(this).start();

        if (!Once.beenDone("user_license")) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about)
                    .setMessage(R.string.software_license)
                    .setCancelable(false)
                    .setNegativeButton(R.string.back, (dialog, which) -> finish())
                    .setPositiveButton(R.string.accept, (dialog, which) ->
                            Once.markDone("user_license"))
                    .create().show();
        }
    }

    private void initMenu() {
        mPopupMenu = new PopupMenu(new ContextThemeWrapper(this, R.style.Theme_AppCompat_Light), mMenuView);
        Menu menu = mPopupMenu.getMenu();
        setIconEnable(menu, true);
        menu.add("Xposed管理器").setIcon(R.drawable.ic_xposed).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, XposedManagerActivity.class));
            return false;
        });
        menu.add("账户").setIcon(R.drawable.ic_account).setOnMenuItemClickListener(item -> {
            List<String> names;
            List<VUserInfo> users;
            try
            {
                users = VUserManager.get().getUsers();
                names = new ArrayList<>(users.size());
                for (VUserInfo info : users)
                {
                    names.add(info.name);
                }
            }
            catch(Throwable e)
            {
                e.printStackTrace();
                return false;
            }
            CharSequence[] items = new CharSequence[names.size()];
            for (int i = 0; i < names.size(); i++) {
                items[i] = names.get(i);
            }
            new AlertDialog.Builder(this)
                    .setTitle("请选择一个用户")
                    .setItems(items, (dialog, which) -> {
                        VUserInfo info = users.get(which);
                        Intent intent = new Intent(this, ChooseTypeAndAccountActivity.class);
                        intent.putExtra(ChooseTypeAndAccountActivity.KEY_USER_ID, info.id);
                        startActivity(intent);
                    }).show();
            return false;
        });
        /*
        // 去掉没用的项目防止卡顿
        menu.add("虚拟存储").setIcon(R.drawable.ic_vs).setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "The coming", Toast.LENGTH_SHORT).show();
            return false;
        });
        menu.add("通知管理").setIcon(R.drawable.ic_notification).setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "The coming", Toast.LENGTH_SHORT).show();
            return false;
        });
        */
        menu.add("虚拟位置").setIcon(R.drawable.ic_notification).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, VirtualLocationSettings.class));
            return true;
        });
        menu.add("设置").setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingAct.class));
            return false;
        });
        menu.add(R.string.restartapp).setIcon(R.drawable.ic_settings).setOnMenuItemClickListener(item -> {
            AlertDialog.Builder hBuilder = new AlertDialog.Builder(HomeActivity.this);
            hBuilder.setTitle(R.string.restartapp).setMessage(R.string.ensurerestart);
            hBuilder.
                    setNegativeButton("×", (dialog, which) ->
                    {
                        // 不做任何事情
                        return;
                    }) .
                    setPositiveButton("√", (dialog, which) ->
                    {
                        VActivityManager.get().killAllApps();
                        Toast.makeText(this,R.string.restartfinish,Toast.LENGTH_LONG).show();
                    });
            hBuilder.setCancelable(false).create().show();
            return false;
        });
        mMenuView.setOnClickListener(v -> mPopupMenu.show());
    }

    private static void setIconEnable(Menu menu, boolean enable) {
        try {
            @SuppressLint("PrivateApi")
            Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);
            m.invoke(menu, enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindViews() {
        mLoadingView = (TwoGearsView) findViewById(R.id.pb_loading_app);
        mLauncherView = (RecyclerView) findViewById(R.id.home_launcher);
        mMenuView = findViewById(R.id.home_menu);
        mBottomArea = findViewById(R.id.bottom_area);
        mCreateShortcutBox = findViewById(R.id.create_shortcut_area);
        mCreateShortcutTextView = (TextView) findViewById(R.id.create_shortcut_text);
        mDeleteAppBox = findViewById(R.id.delete_app_area);
        mDeleteAppTextView = (TextView) findViewById(R.id.delete_app_text);
        // 搜索
        mSearchView = (SearchView) findViewById(R.id.homeSearchApp);
    }

    private void initLaunchpad() {
        mLauncherView.setHasFixedSize(true);
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL);
        mLauncherView.setLayoutManager(layoutManager);
        mLaunchpadAdapter = new LaunchpadAdapter(this);
        SmartRecyclerAdapter wrap = new SmartRecyclerAdapter(mLaunchpadAdapter);
        View footer = new View(this);
        footer.setLayoutParams(new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(this, 60)));
        wrap.setFooterView(footer);
        mLauncherView.setAdapter(wrap);
        mLauncherView.addItemDecoration(new ItemOffsetDecoration(this, R.dimen.desktop_divider));
        ItemTouchHelper touchHelper = new ItemTouchHelper(new LauncherTouchCallback());
        touchHelper.attachToRecyclerView(mLauncherView);
        mLaunchpadAdapter.setAppClickListener((pos, data) -> {
            if (!data.isLoading()) {
                if (data instanceof AddAppButton) {
                    onAddAppButtonClick();
                }
                mLaunchpadAdapter.notifyItemChanged(pos);
                mPresenter.launchApp(data);
            }
        });
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                try
                {
                    if (listAppData == null) return false;
                    if (listAppData.isEmpty()) return false;
                    Iterator<AppData> lpAppData = listAppData.iterator();
                    while (lpAppData.hasNext())
                    {
                        AppData lpData = lpAppData.next();
                        if (!lpData.getName().contains(query))
                            lpAppData.remove();
                    }
                    mLaunchpadAdapter.setList(listAppData);
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                try
                {
                    if (listAppData == null)
                    {
                        listAppData = mLaunchpadAdapter.getList();
                    }
                    if (newText.isEmpty())
                    {
                        if (listAppData != null)
                        {
                            new HomePresenterImpl(HomeActivity.this).start();
                            listAppData = null;
                        }
                    }
                }
                catch(Throwable e)
                {
                    e.printStackTrace();
                }
                return false;
            }
        });
        if (Once.beenDone("app_force_live"))
        {
            Intent intent = new Intent(HomeActivity.this, MakeMeLive.class);
            startService(intent);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        HomeActivity.hHomeAct=null;
    }

    private void onAddAppButtonClick() {
        ListAppActivity.gotoListApp(this);
    }

    private void deleteApp(int position) {
        AppData data = mLaunchpadAdapter.getList().get(position);
        new AlertDialog.Builder(this)
                .setTitle("删除应用")
                .setMessage("您真的要删除 " + data.getName() + "?")
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    mPresenter.deleteApp(data);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void createShortcut(int position) {
        try
        {
            boolean bIsEMUI = RomChecker.isEmui();
            if (bIsEMUI)
            {
                if (!Once.beenDone("emui_shortcut_perm"))
                {
                    AlertDialog.Builder hDialog = new AlertDialog.Builder(HomeActivity.hHomeAct);
                    // 消极
                    hDialog.setNegativeButton("知道了", (dialog, which) ->
                            {
                                Once.markDone("emui_shortcut_perm");
                                Toast.makeText(HomeActivity.hHomeAct, "记得允许权限哦~", Toast.LENGTH_SHORT).show();
                            });
                    hDialog.setMessage(R.string.rom_shortcut_tips);
                    hDialog.setTitle(R.string.create_shortcut).setCancelable(false);
                    hDialog.create().show();
                }
            }
        }catch (Throwable e)
        {
            e.printStackTrace();
        }
        AppData model = mLaunchpadAdapter.getList().get(position);
        if (model instanceof PackageAppData || model instanceof MultiplePackageAppData) {
            mPresenter.createShortcut(model);
        }
    }

    @Override
    public void setPresenter(HomeContract.HomePresenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showBottomAction() {
        mBottomArea.setTranslationY(mBottomArea.getHeight());
        mBottomArea.setVisibility(View.VISIBLE);
        mBottomArea.animate().translationY(0).setDuration(500L).start();
    }

    @Override
    public void hideBottomAction() {
        mBottomArea.setTranslationY(0);
        ObjectAnimator transAnim = ObjectAnimator.ofFloat(mBottomArea, "translationY", 0, mBottomArea.getHeight());
        transAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                mBottomArea.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        transAnim.setDuration(500L);
        transAnim.start();
    }

    @Override
    public void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
        mLoadingView.startAnim();
    }

    @Override
    public void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
        mLoadingView.stopAnim();
    }

    @Override
    public void loadFinish(List<AppData> list) {
        list.add(new AddAppButton(this));
        mLaunchpadAdapter.setList(list);
        hideLoading();
    }

    @Override
    public void loadError(Throwable err) {
        err.printStackTrace();
        hideLoading();
    }

    @Override
    public void showGuide() {

    }

    @Override
    public void addAppToLauncher(AppData model) {
        List<AppData> dataList = mLaunchpadAdapter.getList();
        boolean replaced = false;
        for (int i = 0; i < dataList.size(); i++) {
            AppData data = dataList.get(i);
            if (data instanceof EmptyAppData) {
                mLaunchpadAdapter.replace(i, model);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            mLaunchpadAdapter.add(model);
            mLauncherView.smoothScrollToPosition(mLaunchpadAdapter.getItemCount() - 1);
        }
    }


    @Override
    public void removeAppToLauncher(AppData model) {
        mLaunchpadAdapter.remove(model);
    }

    @Override
    public void refreshLauncherItem(AppData model) {
        mLaunchpadAdapter.refresh(model);
    }

    @Override
    public void askInstallGms() {
        new AlertDialog.Builder(this)
                .setTitle("欢迎使用")
                .setMessage("你好，我们在您的手机上查找到了谷歌服务，需要将谷歌服务添加到本应用当中吗？")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    defer().when(() -> {
                        GmsSupport.installGApps(0);
                    }).done((res) -> {
                        mPresenter.dataChanged();
                    });
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        Toast.makeText(HomeActivity.this,
                                "以后您也可以在设置里面添加谷歌服务。", Toast.LENGTH_LONG).show())
                .setCancelable(false)
                .show();
    }

    static public HomeActivity hHomeAct = null;
    static public String strPkgName = "";
    public void InstallAppByPath(String szAppPath)
    {
        Intent xdata;
        File tempFile;
        try
        {
            ArrayList<AppInfoLite> dataList = new ArrayList<AppInfoLite>(1);
            tempFile = new File(szAppPath.trim());
            dataList.add(new AppInfoLite(tempFile.getName(), szAppPath.trim(), true));
            xdata = new Intent();
            xdata.putParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST, dataList);
            strPkgName=tempFile.getName();
        }
        catch(Throwable e)
        {
            e.printStackTrace();
            return;
        }
        try
        {
            onActivityResult(1,RESULT_OK,xdata);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
        XAppDataInstalled hInstalled = new XAppDataInstalled();
        hInstalled.pkgName=tempFile.getName();
        addAppToLauncher(hInstalled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            List<String> pkg_Det = Arrays.asList(safePackage.safe_Package);
            Toast.makeText(this,R.string.wait,Toast.LENGTH_LONG)
                    .show();
            List<AppInfoLite> appList = data.getParcelableArrayListExtra(VCommends.EXTRA_APP_INFO_LIST);
            if (appList != null) {
                for (AppInfoLite info : appList) {
                    if (!Once.beenDone("disable_safe_mode")&&!pkg_Det.contains(info.packageName))
                    {
                        android.app.AlertDialog.Builder hBuilder
                                = new android.app.AlertDialog.Builder(HomeActivity.this);
                        hBuilder.setTitle(R.string.sk_failed)
                                .setMessage(R.string.unknown_package)
                                .setCancelable(false);
                        hBuilder.setPositiveButton(R.string.back, (dialog, which) ->
                                Toast.makeText(HomeActivity.this,R.string.launch_failed,Toast.LENGTH_SHORT)
                                        .show())
                                .create().show();
                        break;
                    }
                    else
                        mPresenter.addApp(info);
                }
            }
        }
    }

    private class LauncherTouchCallback extends ItemTouchHelper.SimpleCallback {

        int[] location = new int[2];
        boolean upAtDeleteAppArea;
        boolean upAtCreateShortcutArea;
        RecyclerView.ViewHolder dragHolder;

        LauncherTouchCallback() {
            super(UP | DOWN | LEFT | RIGHT | START | END, 0);
        }

        @Override
        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            return 0;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            try {
                AppData data = mLaunchpadAdapter.getList().get(viewHolder.getAdapterPosition());
                if (!data.canReorder()) {
                    return makeMovementFlags(0, 0);
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return super.getMovementFlags(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            int pos = viewHolder.getAdapterPosition();
            int targetPos = target.getAdapterPosition();
            mLaunchpadAdapter.moveItem(pos, targetPos);
            return true;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                if (actionState == ACTION_STATE_DRAG) {
                    if (dragHolder != viewHolder) {
                        dragHolder = viewHolder;
                        viewHolder.itemView.setScaleX(1.2f);
                        viewHolder.itemView.setScaleY(1.2f);
                        if (mBottomArea.getVisibility() == View.GONE) {
                            showBottomAction();
                        }
                    }
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current, RecyclerView.ViewHolder target) {
            if (upAtCreateShortcutArea || upAtDeleteAppArea) {
                return false;
            }
            try {
                AppData data = mLaunchpadAdapter.getList().get(target.getAdapterPosition());
                return data.canReorder();
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder instanceof LaunchpadAdapter.ViewHolder) {
                LaunchpadAdapter.ViewHolder holder = (LaunchpadAdapter.ViewHolder) viewHolder;
                viewHolder.itemView.setScaleX(1f);
                viewHolder.itemView.setScaleY(1f);
                viewHolder.itemView.setBackgroundColor(holder.color);
            }
            super.clearView(recyclerView, viewHolder);
            if (dragHolder == viewHolder) {
                if (mBottomArea.getVisibility() == View.VISIBLE) {
                    mUiHandler.postDelayed(HomeActivity.this::hideBottomAction, 200L);
                    if (upAtCreateShortcutArea) {
                        createShortcut(viewHolder.getAdapterPosition());
                    } else if (upAtDeleteAppArea) {
                        deleteApp(viewHolder.getAdapterPosition());
                    }
                }
                dragHolder = null;
            }
        }


        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            if (actionState != ACTION_STATE_DRAG || !isCurrentlyActive) {
                return;
            }
            View itemView = viewHolder.itemView;
            itemView.getLocationInWindow(location);
            int x = (int) (location[0] + dX);
            int y = (int) (location[1] + dY);

            mBottomArea.getLocationInWindow(location);
            int baseLine = location[1] - mBottomArea.getHeight();
            if (y >= baseLine) {
                mDeleteAppBox.getLocationInWindow(location);
                int deleteAppAreaStartX = location[0];
                if (x < deleteAppAreaStartX) {
                    upAtCreateShortcutArea = true;
                    upAtDeleteAppArea = false;
                    mCreateShortcutTextView.setTextColor(Color.parseColor("#0099cc"));
                    mDeleteAppTextView.setTextColor(Color.WHITE);
                } else {
                    upAtDeleteAppArea = true;
                    upAtCreateShortcutArea = false;
                    mDeleteAppTextView.setTextColor(Color.parseColor("#0099cc"));
                    mCreateShortcutTextView.setTextColor(Color.WHITE);
                }
            } else {
                upAtCreateShortcutArea = false;
                upAtDeleteAppArea = false;
                mDeleteAppTextView.setTextColor(Color.WHITE);
                mCreateShortcutTextView.setTextColor(Color.WHITE);
            }
        }
    }
}
