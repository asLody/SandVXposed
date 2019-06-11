package com.lody.virtual.client.env;

import android.content.Intent;

import com.lody.virtual.client.stub.ShortcutHandleActivity;

/**
 * @author Lody
 *
 */
public class Constants {

	public static final int OUTSIDE_APP_UID = 9999;

	public static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
	public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.package_name";

	/**
	 * If an apk declared the "fake-signature" attribute on its Application TAG,
	 * we will use its signature instead of the real signature.
	 *
	 * For more detail, please see :
	 * https://github.com/microg/android_packages_apps_GmsCore/blob/master/
	 * patches/android_frameworks_base-M.patch.
	 */
	public static final String FEATURE_FAKE_SIGNATURE = "fake-signature";


	public static final String ACTION_NEW_TASK_CREATED = "virtual.intent.action.APP_LAUNCHED";
	public static final String ACTION_PACKAGE_WILL_ADDED = "virtual.intent.action.PACKAGE_WILL_ADDED";
	public static final String ACTION_PACKAGE_ADDED = "virtual." + Intent.ACTION_PACKAGE_ADDED;
	public static final String ACTION_PACKAGE_REMOVED = "virtual." + Intent.ACTION_PACKAGE_REMOVED;
	public static final String ACTION_PACKAGE_CHANGED = "virtual." + Intent.ACTION_PACKAGE_CHANGED;
	public static final String ACTION_USER_ADDED = "virtual." + "android.intent.action.USER_ADDED";
	public static final String ACTION_USER_REMOVED = "virtual." + "android.intent.action.USER_REMOVED";
	public static final String ACTION_USER_INFO_CHANGED = "virtual." + "android.intent.action.USER_CHANGED";
	public static final String ACTION_USER_STARTED = "Virtual." + "android.intent.action.USER_STARTED";


	/**
	 * Server process name of VA
	 */
	public static String SERVER_PROCESS_NAME = ":x";

	public static String HELPER_PROCESS_NAME = ":helper";
	/**
	 * The activity who handle the shortcut.
	 */
	public static String SHORTCUT_PROXY_ACTIVITY_NAME = ShortcutHandleActivity.class.getName();

	public static String ACTION_SHORTCUT = ".virtual.action.shortcut";

	public static String ACTION_BADGER_CHANGE = ".virtual.action.BADGER_CHANGE";

	public static String NOTIFICATION_CHANNEL = "virtual_default";

	public static String NOTIFICATION_DAEMON_CHANNEL = "virtual_daemon";
}
