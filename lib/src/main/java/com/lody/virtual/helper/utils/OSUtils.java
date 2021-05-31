package com.lody.virtual.helper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import com.lody.virtual.helper.compat.SystemPropertiesCompat;

/**
 * Created by 247321453 on 2016/7/17.
 * @author ganyao
 * @author QQ 647564826
 */
public class OSUtils {
	private static final String KEY_EMUI_VERSION_CODE = "ro.build.version.emui";
	private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
	private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
	private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";

	private static final OSUtils sOSUtils = new OSUtils();
	private boolean emui;
	private boolean miui;
	private boolean flyme;
	private String miuiVersion;
	private OSUtils() {
		Properties properties;
		try {
			properties = new Properties();
			properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
		} catch (IOException e) {
			properties = null;
		}
		if (properties != null) {
			emui = !TextUtils.isEmpty(properties.getProperty(KEY_EMUI_VERSION_CODE));
			miuiVersion = properties.getProperty(KEY_MIUI_VERSION_CODE);
			miui = !TextUtils.isEmpty(miuiVersion) || !TextUtils.isEmpty(properties.getProperty(KEY_MIUI_VERSION_NAME))
					|| !TextUtils.isEmpty(properties.getProperty(KEY_MIUI_INTERNAL_STORAGE));
		}
		flyme = hasFlyme();
	}

	public static OSUtils getInstance() {
		return sOSUtils;
	}

	public String getMiuiVersion() {
		return miuiVersion;
	}

	public boolean isEmui() {
		if (Build.DISPLAY.toUpperCase().startsWith("EMUI")) {
			return true;
		}
		String property = SystemPropertiesCompat.get("ro.build.version.emui");
		return property != null;
	}

	public boolean isMiui() {
		return SystemPropertiesCompat.getInt("ro.miui.ui.version.code", 0) > 0;
	}

	public boolean isFlyme() {
		return Build.DISPLAY.toLowerCase().contains("flyme");
	}

	public static boolean isColorOS() {
		return SystemPropertiesCompat.isExist("ro.build.version.opporom")
				|| SystemPropertiesCompat.isExist("ro.rom.different.version");
	}

	public static boolean is360UI() {
		String property = SystemPropertiesCompat.get("ro.build.uiversion");
		return property != null && property.toUpperCase().contains("360UI");
	}

	public static boolean isLetv() {
		return Build.MANUFACTURER.equalsIgnoreCase("Letv");
	}

	public static boolean isVivo() {
		return SystemPropertiesCompat.isExist("ro.vivo.os.build.display.id");
	}

	public boolean isAndroidQ() {
		return Build.VERSION.SDK_INT>=29;
	}

	public boolean isAndroidS() {
		try {
			return Build.VERSION.SDK_INT >= 31 || (Build.VERSION.SDK_INT >= 30 && Build.VERSION.PREVIEW_SDK_INT > 0);
		}catch (Throwable t)
		{
			return false;
		}
	}

	private boolean hasFlyme() {
		try {
			final Method method = Build.class.getMethod("hasSmartBar");
			return method != null;
		} catch (final Exception e) {
			return false;
		}
	}
}
