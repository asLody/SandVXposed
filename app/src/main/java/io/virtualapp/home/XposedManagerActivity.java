package io.virtualapp.home;

import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import android.widget.Toast;

import com.lody.virtual.sandxposed.XposedConfig;
import com.trend.lazyinject.annotation.InjectComponent;

import java.util.ArrayList;
import java.util.List;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.adapters.XposedModuleAdapter;
import io.virtualapp.home.adapters.decorations.ItemOffsetDecoration;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.repo.AppRepository;
import io.virtualapp.sandxposed.XposedConfigComponent;

public class XposedManagerActivity extends VActivity {

    SwitchCompat xposedSwitch;
    RecyclerView recyclerView;

    AppRepository appRepository;
    List<AppData> modules = new ArrayList<>();

    XposedModuleAdapter adapter;

    // @InjectComponent
    XposedConfig config = new XposedConfigComponent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try
        {
            setContentView(R.layout.activity_xposed_manager);
            setSupportActionBar(findViewById(R.id.toolbar));
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null)
            {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
            initXposedGlobalSettings();
            initModuleList();
        }
        catch (Throwable e)
        {
            if(HomeActivity.hHomeAct!=null)
            {
                Snackbar.make(HomeActivity.hHomeAct.getWindow().getDecorView(),R.string.launch_failed
                ,Snackbar.LENGTH_LONG)
                        .setAction(R.string.refresh, v ->
                                HomeActivity.hHomeAct.RefreshDesktop())
                        .show();
            }
            e.printStackTrace();
            finish();
        }
        android.widget.Toast.makeText(this,R.string.SKRestartTips, Toast.LENGTH_LONG).show();
    }

    private void initXposedGlobalSettings() {
        xposedSwitch = findViewById(R.id.xposed_enable_switch);
        xposedSwitch.setChecked(config.xposedEnabled());
        xposedSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            config.enableXposed(b);
            recyclerView.setEnabled(b);
            recyclerView.setAlpha(b ? 1 : 0.5f);
        });
    }

    private void initModuleList() {

        boolean xposedEnabled = config.xposedEnabled();

        recyclerView = findViewById(R.id.module_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ItemOffsetDecoration(VUiKit.dpToPx(getContext(), 4)));
        recyclerView.setEnabled(xposedEnabled);
        recyclerView.setAlpha(xposedEnabled ? 1 : 0.5f);

        appRepository = new AppRepository(this);
        adapter = new XposedModuleAdapter(this, appRepository, modules);
        adapter.config = config;
        appRepository.getVirtualXposedModules()
                .done(result -> {
                    modules.clear();
                    modules.addAll(result);
                    adapter.notifyDataSetChanged();
                });

        recyclerView.setAdapter(adapter);

    }

}
