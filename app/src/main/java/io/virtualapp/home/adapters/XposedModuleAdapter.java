package io.virtualapp.home.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.lody.virtual.sandxposed.XposedConfig;

import java.util.List;

import io.virtualapp.R;
import io.virtualapp.home.LoadingActivity;
import io.virtualapp.home.models.AppData;
import io.virtualapp.home.models.MultiplePackageAppData;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.AppRepository;

public class XposedModuleAdapter extends RecyclerView.Adapter<XposedModuleAdapter.ViewHolder> {


    public XposedConfig config;
    private Context context;
    private LayoutInflater mInflater;
    private List<AppData> modules;
    private AppRepository repository;

    /*
    @InjectComponent
    XposedConfig config;
    */

    public XposedModuleAdapter(Context context, AppRepository repository, List<AppData> modules) {
        this.context = context;
        mInflater = LayoutInflater.from(context);
        this.modules = modules;
        this.repository = repository;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(mInflater.inflate(R.layout.list_item_module, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        viewHolder.bind(modules.get(i));
    }

    @Override
    public int getItemCount() {
        return modules.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon;
        TextView title;
        TextView desc;
        TextView version;
        CheckBox enable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.description);
            version = itemView.findViewById(R.id.version_name);
            enable = itemView.findViewById(R.id.checkbox);
        }

        public void bind(AppData data) {
            icon.setImageDrawable(data.getIcon());
            title.setText(data.getName());
            version.setText(data.versionName());
            try
            {
                enable.setChecked(config.moduleEnable(data.getPackageName()));
            }
            catch(Throwable e)
            {
                e.printStackTrace();
            }
            enable.setOnCheckedChangeListener((compoundButton, b) -> config.enableModule(data.getPackageName(), b));
            if (data.getXposedModule() != null) {
                desc.setText(data.getXposedModule().desc);
            }
            if (data.canLaunch()) {
                itemView.setOnClickListener(view -> launchModule(data));
            } else {
                itemView.setOnClickListener(null);
            }
            if (data.canDelete()) {
                itemView.setOnLongClickListener(view -> deleteModule(data));
            } else {
                itemView.setOnLongClickListener(null);
            }
        }

        void launchModule(AppData data) {
            try {
                if (data instanceof PackageAppData) {
                    PackageAppData appData = (PackageAppData) data;
                    appData.isFirstOpen = false;
                    LoadingActivity.launch(context, appData.packageName, 0);
                } else if (data instanceof MultiplePackageAppData) {
                    MultiplePackageAppData multipleData = (MultiplePackageAppData) data;
                    multipleData.isFirstOpen = false;
                    LoadingActivity.launch(context, multipleData.appInfo.packageName, ((MultiplePackageAppData) data).userId);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        boolean deleteModule(AppData data) {
            new AlertDialog.Builder(context)
                    .setTitle("删除模块")
                    .setMessage("您真的要删除 " + data.getName() + "?")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        try {
                            if (data instanceof PackageAppData) {
                                repository.removeVirtualApp(((PackageAppData) data).packageName, 0);
                            } else {
                                MultiplePackageAppData appData = (MultiplePackageAppData) data;
                                repository.removeVirtualApp(appData.appInfo.packageName, appData.userId);
                            }
                            modules.remove(data);
                            notifyDataSetChanged();
                        } catch (Throwable throwable) {

                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return false;
        }

    }

}
