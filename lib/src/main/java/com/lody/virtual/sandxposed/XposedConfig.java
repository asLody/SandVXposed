package com.lody.virtual.sandxposed;

import com.trend.lazyinject.annotation.Component;
import com.trend.lazyinject.annotation.Provide;

@Component
public interface XposedConfig {

    //providers
    @Provide
    boolean xposedEnabled();

    //ipc calls
    void enableXposed(boolean enable);
    boolean moduleEnable(String pkg);
    void enableModule(String pkg, boolean enable);

}
