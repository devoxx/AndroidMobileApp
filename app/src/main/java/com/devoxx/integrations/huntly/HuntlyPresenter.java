package com.devoxx.integrations.huntly;

import com.afollestad.materialdialogs.MaterialDialog;
import com.devoxx.R;
import com.devoxx.integrations.huntly.connection.model.HuntlyDeepLinkConf;
import com.devoxx.integrations.huntly.connection.model.HuntlyQuestActivity;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

@EBean
public class HuntlyPresenter {

	public static final String INTEGRATION_DIALOG_DISMISSED = "HuntlyPresenter.INTEGRATION_DIALOG_DISMISSED";
	private static final String BROWSER_FALLBACK_URI_PREFIX = "https://play.google.com/store/apps/details?id=";

	@RootContext Context context;
	@Pref HuntlySettings_ huntlySettings;
	@Bean HuntlyController huntlyController;
	@Bean InfoUtil infoUtil;

	void showFirstRunDialogIfNeeded(String confId, Activity activity) {
		if (huntlySettings.isFirstRun().getOr(true) && huntlyController.isAnyFirstRunQuest()) {
			huntlySettings.edit().isFirstRun().put(false).apply();
			final HuntlyQuestActivity quest = huntlyController.getFirstRunQuest();
			completeQuest(quest, confId, activity, R.string.huntly_welcome_bonus);
		}
	}

	void voteSucceed(String confId, Activity activity) {
		if (huntlyController.isAnyVoteQuests()) {
			final HuntlyQuestActivity quest = huntlyController.getVoteQuest();
			completeQuest(quest, confId, activity, R.string.huntly_points_won);
		}
	}

	void decideToOpenAppOrPlayStore(String confId, Activity mainActivity) {
		final HuntlyDeepLinkConf deepLinks = huntlyController.currentDeepLinks(confId);
		final Intent startIntent = new Intent(Intent.ACTION_VIEW);
		startIntent.setData(Uri.parse(deepLinks.getDeepLink()));
		final PackageManager packageManager = mainActivity.getPackageManager();
		final List<ResolveInfo> list = packageManager.queryIntentActivities(startIntent, 0);

		if (list.isEmpty()) {
			openPlayStore(deepLinks, mainActivity);
		} else {
			startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mainActivity.startActivity(startIntent);
		}
	}

	private void openPlayStore(HuntlyDeepLinkConf deepLinks, Activity mainActivity) {
		final Uri marketUri = Uri.parse(deepLinks.getPlayStoreUri());

		try {
			mainActivity.startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
		} catch (Exception e) {

			final Uri uri = Uri.parse(deepLinks.getPlayStoreUri());
			final List<String> parts = uri.getQueryParameters("id");

			if (parts != null && !parts.isEmpty()) {
				mainActivity.startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse(BROWSER_FALLBACK_URI_PREFIX + parts.get(0))));
			}
		}
	}

	private void completeQuest(HuntlyQuestActivity quest, String confId, Activity activity, int messageResId) {
		huntlyController.completeQuestAsync(quest, confId);

		if (activity != null && !activity.isFinishing()) {
			final MaterialDialog md = new MaterialDialog.Builder(activity)
					.customView(R.layout.huntly_first_run_layout, true)
					.negativeText(android.R.string.ok)
					.dismissListener(dialog -> notifyListeners())
					.positiveText(R.string.play_more)
					.onPositive((dialog, which) ->
							decideToOpenAppOrPlayStore(confId, activity)).build();
			TextView view = (TextView) md.getCustomView().findViewById(R.id.huntlyDialogMessage);
			view.setText(messageResId);

			view = (TextView) md.getCustomView().findViewById(R.id.huntlyDialogPointsCount);
			view.setText(String.format(Locale.getDefault(), "+%d", quest.getSingleReward()));

			view = (TextView) md.getCustomView().findViewById(R.id.huntlyDialogPromo);
			view.setText(huntlySettings.promo().get());

			md.show();
		}

		updateUserStatsAsync(null);
	}

	private void notifyListeners() {
		context.sendBroadcast(new Intent(INTEGRATION_DIALOG_DISMISSED));
	}

	@Background void updateUserStatsAsync(HuntlyController.UserStatsListener listener) {
		huntlyController.updateUserStats(listener);
	}
}
