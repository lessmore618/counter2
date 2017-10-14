package me.tsukanov.counter.ui;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;


public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String KEY_REMOVE_COUNTERS = "removeCounters";
    private static final String KEY_VERSION = "version";
    private static final String KEY_THEME = "theme";
    // ADDING CODE
    private static final String KEY_COUNT_AMOUNT = "countAmount";

    private static final String THEME_DARK = "dark";
    private static final String THEME_LIGHT = "light";

    private static final int MIN_COUNT_TICKS = 1;
    private static final int MAX_COUNT_TICKS = 100;


    private SharedPreferences mSharedPref;
    private AppCompatDelegate mDelegate;
    private SettingsFragment fragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (mSharedPref.getString(KEY_THEME, THEME_LIGHT).equals(THEME_DARK)) {
            setTheme(R.style.AppTheme_Dark);
        }

        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            onCreatePreferenceActivity();
        } else {
            onCreatePreferenceFragment();
        }
    }

    @SuppressWarnings("deprecation")
    private void onCreatePreferenceActivity() {
        addPreferencesFromResource(R.xml.settings);
        findPreference(KEY_VERSION).setSummary(getAppVersion());
        findPreference(KEY_THEME).setSummary(getCurrentThemeName());
        findPreference(KEY_REMOVE_COUNTERS)
                .setOnPreferenceClickListener(getOnRemoveCountersClickListener());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void onCreatePreferenceFragment() {
        fragment = new SettingsFragment();
        fragment.setOnRemoveCountersClickListener(getOnRemoveCountersClickListener());
        fragment.setAppVersion(getAppVersion());
        fragment.setTheme(getCurrentThemeName());
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit();
    }

    private String getCurrentThemeName() {
        switch (mSharedPref.getString(KEY_THEME, THEME_LIGHT)) {
            case THEME_LIGHT:
                return getResources().getString(R.string.settings_theme_light);
            case THEME_DARK:
                return getResources().getString(R.string.settings_theme_dark);
            default:
                return "Unknown";
        }
    }

    private OnPreferenceClickListener getOnRemoveCountersClickListener() {
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showWipeDialog();
                return true;
            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getAppVersion() {
        try {
            return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            return getResources().getString(R.string.unknown);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }


//// ADDING CODE
//editTextPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object newValue) {
//            int val = Integer.parseInt(newValue.toString());
//            if ((val > minPort) && (val < maxPort)) {
//
//                Log.d(LOGTAG, "Value saved: " + val);
//                return true;
//            }
//            else {
//                // invalid you can show invalid message
//                Toast.makeText(getApplicationContext(), "error text", Toast.LENGTH_LONG).show();
//                return false;
//            }
//        }
//    });

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_THEME)) {
            Preference pref;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                pref = findPreference(KEY_THEME);
            } else {
                pref = fragment.findPreference(KEY_THEME);
            }
            if (pref != null) {
                pref.setSummary(getCurrentThemeName());
            }
            // Restart to apply new theme, go back to this settings screen
            TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, MainActivity.class))
                    .addNextIntent(this.getIntent())
                    .startActivities();
        }
    }

    private void showWipeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.settings_wipe_confirmation);
        builder.setPositiveButton(R.string.settings_wipe_confirmation_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                CounterApplication app = (CounterApplication) getApplication();
                app.removeCounters();

                SharedPreferences.Editor settingsEditor = mSharedPref.edit();
                if (app.counters.isEmpty()) {
                    settingsEditor.putString(MainActivity.STATE_ACTIVE_COUNTER, getString(R.string.default_counter_name));
                } else {
                    settingsEditor.putString(MainActivity.STATE_ACTIVE_COUNTER, app.counters.keySet().iterator().next());
                }
                settingsEditor.commit();

                Toast.makeText(
                        getBaseContext(),
                        getResources().getText(R.string.toast_wipe_success),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        builder.setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        private OnPreferenceClickListener mOnRemoveCountersClickListener;
        // ADDING CODE
        private OnPreferenceClickListener mEditTextNumberChangedListener;
        private String mAppVersion;
        private String mTheme;

        public void setOnRemoveCountersClickListener(OnPreferenceClickListener onRemoveCountersClickListener) {
            mOnRemoveCountersClickListener = onRemoveCountersClickListener;
        }

        public void setAppVersion(String appVersion) {
            mAppVersion = appVersion;
        }

        public void setTheme(String theme) {
            mTheme = theme;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            findPreference(KEY_VERSION).setSummary(mAppVersion);
            findPreference(KEY_THEME).setSummary(mTheme);
            findPreference(KEY_REMOVE_COUNTERS).setOnPreferenceClickListener(mOnRemoveCountersClickListener);
            // ADDING CODE
            findPreference(KEY_COUNT_AMOUNT).setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {

                    int val = Integer.parseInt((String) value);
                    if (val < MIN_COUNT_TICKS) {
                        val = MIN_COUNT_TICKS;
                    } else if (val > MAX_COUNT_TICKS) {
                        val = MAX_COUNT_TICKS;
                    }

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    settings.edit().putString(KEY_COUNT_AMOUNT, "" + val).apply();
                    return false;


//                    if(newValue!=oldValue){
//
//                        settings.edit().putString(KEY_COUNT_AMOUNT,""+newValue).apply();
//                        return false;
//                    } else {
//                        return true;
//
//                    }

//                    int val2 = Integer.parseInt(settings.getString(KEY_COUNT_AMOUNT,"1"));

//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
//                    SharedPreferences.Editor prefEditor = sharedPref.edit(); // Get preference in editor mode
//                    prefEditor.putString("your_edit_text_pref_key", "DEFAULT-VALUE"); // set your default value here (could be empty as well)
//                    prefEditor.commit(); // finally save changes


//                    try {
//                        Toast.makeText(getContext(), "Value saved: " + newValue, Toast.LENGTH_LONG).show();
//                    } catch (Exception e) {
//
//                    }
//                    return false;

                }
            });
        }
    }
}
