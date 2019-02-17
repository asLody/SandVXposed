package io.virtualapp.sandxposed;

import com.lody.virtual.client.sandxposed.SandXposedConfig;
import com.lody.virtual.client.sandxposed.XposedModule;
import com.trend.lazyinject.annotation.ComponentImpl;

import java.util.List;

@ComponentImpl(process = "io.virtualapp.sandvxposed")
public class SandXposedConfigComponent implements SandXposedConfig {
    @Override
    public boolean xposedEnabled() {
        return false;
    }

    @Override
    public List<XposedModule> modules() {
        return null;
    }
}
