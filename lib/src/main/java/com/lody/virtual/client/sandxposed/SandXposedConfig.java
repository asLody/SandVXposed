package com.lody.virtual.client.sandxposed;

import com.trend.lazyinject.annotation.Component;
import com.trend.lazyinject.annotation.Provide;

import java.util.List;

@Component
public interface SandXposedConfig {

    //providers
    @Provide
    boolean xposedEnabled();
    @Provide
    List<XposedModule> modules();

    //ipc calls

}
