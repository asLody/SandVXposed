package io.virtualapp.home.models;

import android.graphics.drawable.Drawable;

import com.lody.virtual.server.pm.parser.VPackage;

/**
 * @author Lody
 */

public interface AppData {

    boolean isLoading();

    boolean isFirstOpen();

    Drawable getIcon();

    String getName();

    String getPackageName();

    String versionName();

    boolean canReorder();

    boolean canLaunch();

    boolean canDelete();

    boolean canCreateShortcut();

    VPackage.XposedModule getXposedModule();
}
