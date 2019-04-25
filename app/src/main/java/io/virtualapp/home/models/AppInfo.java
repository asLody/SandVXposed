package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

import com.lody.virtual.server.pm.parser.VPackage;

/**
 * @author Lody
 */

public class AppInfo {
    public String packageName;
    public String path;
    public boolean fastOpen;
    public Drawable icon;
    public CharSequence name;
    public int cloneCount;
    public VPackage.XposedModule xposedModule;
}
