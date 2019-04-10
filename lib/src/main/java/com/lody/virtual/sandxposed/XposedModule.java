package com.lody.virtual.sandxposed;

import java.io.Serializable;

public class XposedModule implements Serializable {
    public String packageName;
    public String moduleApkPath;
    public String moduleSoPath;
    public String moduleOatPath;
    public boolean enabled;
}
