package com.devoxx.integrations.huntly;

import com.annimon.stream.Optional;
import com.devoxx.R;
import com.devoxx.android.fragment.common.BaseMenuFragment;
import com.devoxx.data.register.BaseExtractor;
import com.devoxx.integrations.IntegrationController;
import com.devoxx.integrations.huntly.connection.HuntlyConnection;
import com.devoxx.integrations.huntly.connection.model.HuntlyUserStats;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Menu;
import android.view.MenuItem;

@EBean
public class HuntlyIntegrationController implements IntegrationController {

	@Bean HuntlyConnection huntlyConnection;
	@Bean HuntlyPresenter huntlyPresenter;
	@Bean HuntlyController huntlyController;
	@RootContext Context context;

	@Override public void register() {
		huntlyController.register();
	}

	@Override public void downloadNeededData(String confCode) {
		huntlyController.fetchEvents(confCode);
		huntlyController.fetchOtherData(confCode);
	}

	@Override public void updateNeededData(String confCode) {
		final HandlerThread handlerThread = new HandlerThread("updateNeededData");
		handlerThread.start();
		final Handler handler = new Handler(handlerThread.getLooper());
		handler.post(() -> huntlyController.fetchOtherData(confCode));
	}

	@Override public void handleAppResume(String confId, Activity activity) {
		huntlyPresenter.showFirstRunDialogIfNeeded(confId, activity);
		huntlyPresenter.updateUserStatsAsync(null);
	}

	@Override public void talkVoted(String confId, Activity activity) {
		huntlyPresenter.voteSucceed(confId, activity);
	}

	@Override
	public void userRegistered(String confId, String finalCode,
														 BaseExtractor infoExtractor) {
		huntlyController.updateUserProfileAsync(confId, finalCode, infoExtractor);
	}

	@Override public void setupIntegrationToolbarMenuItem(Menu menu) {
		final MenuItem menuItem = menu.findItem(R.id.action_integration);
		if (menuItem != null) {
			final Optional<HuntlyUserStats> statsOpt = huntlyController.currentUserStats();
			if (statsOpt.isPresent()) {
				final HuntlyUserStats stats = statsOpt.get();
				menuItem.setVisible(true);
				menuItem.setIcon(BaseMenuFragment.buildCounterDrawable(context, stats.getPoints(),
						R.drawable.ic_menu_devoxx_hunty_integration_icon,
						R.layout.toolbar_menu_item_with_badge_centered_view));
			} else {
				menuItem.setVisible(false);
			}
		}
	}

	@Override public void handleToolbarIconClick(String confId, Activity activity) {
		huntlyPresenter.decideToOpenAppOrPlayStore(confId, activity);
	}

	@Override public void init() {
		huntlyConnection.init();
	}

	@Override public void clear() {
		huntlyController.clear();
	}
}