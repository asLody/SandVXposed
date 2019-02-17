package com.lody.virtual.client.sandxposed;

import java.io.Serializable;

public class XposedModule implements Serializable {
    public String packageName;
    public String moduleApkPath;
    public String moduleSoPath;
    public String moduleOatPath;
    public boolean enabled;
}
