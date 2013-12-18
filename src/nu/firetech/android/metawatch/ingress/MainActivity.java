package nu.firetech.android.metawatch.ingress;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.activity_main);
		findPreference("status").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
				startActivityForResult(intent, 0);
				return false;
			}
		});
		findPreference("test").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				NotificationService.sendNotification(MainActivity.this, "Test message");
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		CheckBoxPreference statusPref = (CheckBoxPreference)findPreference("status");
		boolean serviceEnabled = NotificationService.isEnabled(this);
		statusPref.setChecked(serviceEnabled);
		int statusColor = (serviceEnabled ? Color.GREEN : Color.RED);
    	Spannable spannableText = (Spannable) new SpannableString(
    			getString(serviceEnabled ? R.string.status_label_on : R.string.status_label_off));
    	spannableText.setSpan(new ForegroundColorSpan(statusColor), 0, spannableText.length(), 0);
    	statusPref.setTitle(spannableText);
		
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		String[] keys = new String[] {"vibrate_on", "vibrate_off", "vibrate_cycles"};
		String[] def = new String[] {"500", "500", "2"};
		for (int i = 0; i < keys.length; i++) {
			Preference pref = findPreference(keys[i]);
			pref.setSummary(sharedPreferences.getString(keys[i], def[i]));
		}
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    Preference pref = findPreference(key);

	    if (pref instanceof EditTextPreference) {
	    	pref.setSummary(((EditTextPreference) pref).getText());
	    }
	}
}
