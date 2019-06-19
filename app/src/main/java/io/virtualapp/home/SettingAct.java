package io.virtualapp.home;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import androidx.appcompat.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.ipc.VActivityManager;
import com.sk.app.SettingUtils;
import com.sk.app.UpdateExistApp;
import com.sk.fwindow.skFloattingWin;
import com.sk.listapp.AppDataManager;
import com.sk.listapp.XAppManager;

import java.util.List;

import io.virtualapp.R;
import jonathanfinerty.once.Once;
import sk.vpkg.live.AutoRunUtils;
import sk.vpkg.provider.BanNotificationProvider;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingAct extends AppCompatPreferenceActivity
{

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            String stringValue = value.toString();

            if (preference instanceof ListPreference)
            {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference)
            {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue))
                {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else
                {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null)
                    {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else
                    {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else
            {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName)
                || SKResstart.class.getName().equals(fragmentName)
                || SKSettings.class.getName().equals(fragmentName)
                || SKsetAppLiving.class.getName().equals(fragmentName)
                || SKAppFloatingWindowSetting.class.getName().equals(fragmentName)
                || SKAppStorageRedirect.class.getName().equals(fragmentName)
                || SKUAppDataSetting.class.getName().equals(fragmentName)
                || SKAppWakeUp.class.getName().equals(fragmentName)
                || SKAppFullScreen.class.getName().equals(fragmentName)
                || SKUpdateAppApps.class.getName().equals(fragmentName)
                || SKDisableAppAdapt.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setTitle(R.string.about);
            hDialog.setMessage(getResources().getString(R.string.about_info).
                    replaceAll("##","\n"));
            hDialog.setPositiveButton(R.string.desktop, (dialog, which) ->
                    getActivity().finish())
                    .setCancelable(false).create().show();
            hDialog.setNegativeButton(R.string.back, (dialog, which) ->
                    dialog.dismiss())
                    .setCancelable(false).create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.enable_search_app);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        if (Once.beenDone("enable_search_app"))
                        {
                            Once.clearDone("enable_search_app");
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                if (!Once.beenDone("enable_search_app"))
                {
                    Once.markDone("enable_search_app");
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.safe_mode_tips);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.still_safe,
                    (dialog, which) ->
                    {
                        if (Once.beenDone("disable_safe_mode"))
                        {
                            Once.clearDone("disable_safe_mode");
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.disable, (dialog, which) ->
            {
                if (!Once.beenDone("disable_safe_mode"))
                {
                    Once.markDone("disable_safe_mode");
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKSettings extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            Intent hAppInfoSettings=new Intent(getActivity(), XAppManager.class);
            getActivity().startActivity(hAppInfoSettings);
            getActivity().finish();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                // getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKResstart extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            VActivityManager.get().killAllApps();
            Toast.makeText(getActivity(),R.string.restartfinish,Toast.LENGTH_LONG).show();
            getActivity().finish();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKsetAppLiving extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.non_stop);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        if (Once.beenDone("app_force_live"))
                        {
                            Once.clearDone("app_force_live");
                        }
                        try
                        {
                            String ismakeMeLiveEnable = BanNotificationProvider.getString(
                                    VirtualCore.get().getContext(),
                                    "makeMeLive"
                            );
                            if(ismakeMeLiveEnable!=null)
                            {
                                BanNotificationProvider.remove(VirtualCore.get().getContext(),
                                        "makeMeLive"
                                        );
                            }
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                if (!Once.beenDone("app_force_live"))
                {
                    Once.markDone("app_force_live");
                }
                try
                {
                    String ismakeMeLiveEnable = BanNotificationProvider.getString(
                            VirtualCore.get().getContext(),
                            "makeMeLive"
                    );
                    if(ismakeMeLiveEnable==null)
                    {
                        BanNotificationProvider.save(VirtualCore.get().getContext(),
                                "makeMeLive",
                                "Enabled"
                        );
                    }
                    SettingUtils.enterWhiteListSetting(VirtualCore.get().getContext());
                }catch (Throwable e)
                {
                    e.printStackTrace();
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKAppFloatingWindowSetting extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.ensure_enable_floating_win);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        if (Once.beenDone("enable_floating_win"))
                        {
                            Once.clearDone("enable_floating_win");
                        }
                        getActivity().stopService(new Intent
                                (getActivity(), skFloattingWin.class));
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                if (!Once.beenDone("enable_floating_win"))
                {
                    Once.markDone("enable_floating_win");
                }
                getActivity().startService(new Intent
                        (getActivity(), skFloattingWin.class));
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKAppStorageRedirect extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.ensure_enable_storage_redirect);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"StorageRedirect");
                        if(szEnableRedirectStorage!=null)
                        {
                            BanNotificationProvider.remove(getActivity(),"StorageRedirect");
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"StorageRedirect");
                if(szEnableRedirectStorage==null)
                {
                    BanNotificationProvider.save(getActivity(),"StorageRedirect","Enabled");
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKUpdateAppApps extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            if(getActivity()==null)return;
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.sk_upgrade_localapp);
            hDialog.setPositiveButton(R.string.accept, (dialog, which) ->
            {
                getActivity().startActivity(new Intent(getActivity(), UpdateExistApp.class));
                getActivity().finish();
            })
                    .setNegativeButton(R.string.back, (dialog, which) -> getActivity().finish())
                    .setTitle(R.string.title_activity_update_exist_app)
                    .setCancelable(false)
                    .create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKUAppDataSetting extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            if(getActivity()==null)return;
            getActivity().startActivity(new Intent(getActivity(), AppDataManager.class));
            getActivity().finish();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKAppWakeUp extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.wake_up_setting);
            hDialog.setTitle(R.string.enable_wakeup).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        AutoRunUtils.disableWakeUp();
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                AutoRunUtils.enableWakeUp();
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKAppFullScreen extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.enable_full_screen);
            hDialog.setTitle(R.string.SK_Settings).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"enableFullScreen");
                        if(szEnableRedirectStorage!=null)
                        {
                            BanNotificationProvider.remove(getActivity(),"enableFullScreen");
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"enableFullScreen");
                if(szEnableRedirectStorage==null)
                {
                    BanNotificationProvider.save(getActivity(),"enableFullScreen","Enabled");
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SKDisableAppAdapt extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_appset);
            setHasOptionsMenu(true);
            AlertDialog.Builder hDialog = new AlertDialog.Builder(getActivity());
            hDialog.setMessage(R.string.disabel_app_adapt_tip);
            hDialog.setTitle(R.string.disable_app_adapt).setNegativeButton(R.string.disable,
                    (dialog, which) ->
                    {
                        String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"disableAdaptApp");
                        if(szEnableRedirectStorage==null)
                        {
                            BanNotificationProvider.save(getActivity(),"disableAdaptApp","disabled");
                        }
                        getActivity().finish();
                    });
            hDialog.setPositiveButton(R.string.enable, (dialog, which) ->
            {
                String szEnableRedirectStorage = BanNotificationProvider.getString(getActivity(),"disableAdaptApp");
                if(szEnableRedirectStorage!=null)
                {
                    BanNotificationProvider.remove(getActivity(),"disableAdaptApp");
                }
                getActivity().finish();
            })
                    .setCancelable(false);
            hDialog.create().show();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item)
        {
            int id = item.getItemId();
            if (id == android.R.id.home)
            {
                startActivity(new Intent(getActivity(), SettingAct.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
