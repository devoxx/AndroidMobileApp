package com.devoxx.android.fragment.settings;

import com.devoxx.R;
import com.devoxx.android.activity.SelectorActivity_;
import com.devoxx.data.Settings_;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.user.UserManager;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.PreferenceClick;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;

@EFragment
public class SettingsFragment extends PreferenceFragment {

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	UserManager userManager;

	@Bean
	InfoUtil infoUtil;

	@Pref
	Settings_ settings;

	@AfterViews void afterViews() {
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();
		setupUserCodePreference();
	}

	@PreferenceClick(R.string.settings_user_code) void onClearUserCodeClick() {
		if (userManager.isFirstTimeUser()) {
			userManager.openUserScanBadge();
		} else {
			userManager.clearCode();
			infoUtil.showToast(R.string.user_code_cleared);
		}
		setupUserCodePreference();
	}

	@PreferenceClick(R.string.settings_change_conf_key) void onChangeConferenceClick() {
		ActivityCompat.finishAffinity(getActivity());
		conferenceManager.requestConferenceChange();
		SelectorActivity_.intent(this).flags(Intent.FLAG_ACTIVITY_CLEAR_TOP).start();
	}

	@PreferenceClick(R.string.settings_refresh_schedule_data_key) void onRefreshScheduleDataClick() {
		conferenceManager.forceUpdateFromSettings(getActivity().getApplicationContext());
	}

	private void setupUserCodePreference() {
		final Preference preference = getPreferenceManager().findPreference(
				getString(R.string.settings_user_code));
		final String title;
		final String summary;
		if (userManager.isFirstTimeUser()) {
			title = getString(R.string.user_scan_qr);
			summary = getString(R.string.user_code_scan_desc);
		} else {
			title = getString(R.string.user_code_clear);
			summary = getString(R.string.user_code_clear_desc);
		}
		preference.setTitle(title);
		preference.setSummary(summary);
	}
}
