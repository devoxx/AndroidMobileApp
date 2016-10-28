package com.devoxx.android.fragment.common;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Optional;
import com.devoxx.R;
import com.devoxx.android.activity.AboutActivity_;
import com.devoxx.android.activity.SettingsActivity_;
import com.devoxx.android.dialog.FiltersDialog;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.model.RealmConference;
import com.devoxx.data.schedule.filter.ScheduleFilterManager;
import com.devoxx.data.schedule.filter.model.RealmScheduleCustomFilter;
import com.devoxx.data.schedule.filter.model.RealmScheduleDayItemFilter;
import com.devoxx.data.schedule.filter.model.RealmScheduleTrackItemFilter;
import com.devoxx.integrations.IntegrationProvider;
import com.devoxx.navigation.Navigator;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OptionsItem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.annotation.MenuRes;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

@EFragment
public abstract class BaseMenuFragment extends BaseFragment
				implements FiltersDialog.IFiltersChangedListener {

	@Bean
	protected ScheduleFilterManager scheduleFilterManager;

	@Bean
	protected InfoUtil infoUtil;

	@Bean
	protected Navigator navigator;

	@Bean
	protected ConferenceManager conferenceManager;

	@Bean
	protected IntegrationProvider integrationProvider;

	private MaterialDialog filtersDialog;
	private String lastQuery;

	@AfterViews
	protected void afterViews() {
		setHasOptionsMenu(true);
	}

	@MenuRes
	protected abstract int getMenuRes();

	protected abstract void onSearchQuery(String query);

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (isAdded()) {
			inflater.inflate(getMenuRes(), menu);
			setupFilterMenuIfNeeded(getContext(), menu);
			setupSearchViewIfNeeded(menu);
			integrationProvider.provideIntegrationController()
							.setupIntegrationToolbarMenuItem(menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onDayFiltersChanged(RealmScheduleDayItemFilter itemFilter, boolean isActive) {
		scheduleFilterManager.updateFilter(itemFilter, isActive);
	}

	@Override
	public void onTrackFiltersChanged(RealmScheduleTrackItemFilter itemFilter, boolean isActive) {
		scheduleFilterManager.updateFilter(itemFilter, isActive);
	}

	@Override
	public void onCustomFiltersChanged(RealmScheduleCustomFilter itemFilter, boolean isActive) {
		scheduleFilterManager.updateFilter(itemFilter, isActive);
	}

	@Override
	public void onFiltersCleared() {
		scheduleFilterManager.clearFilters();
	}

	@Override
	public void onFiltersDismissed() {
		if (isActivityLive()) {
			getActivity().supportInvalidateOptionsMenu();
			getMainActivity().sendBroadcast(new Intent(
							ScheduleFilterManager.FILTERS_CHANGED_ACTION));
		}
	}

	protected boolean isActivityLive() {
		return getActivity() != null && !getActivity().isFinishing();
	}

	@Override
	public void onFiltersDefault() {
		scheduleFilterManager.defaultFilters();
	}

	public static Drawable buildCounterDrawable(Context context, int count, int backgroundImageId, int textResId) {
		final LayoutInflater inflater = LayoutInflater.from(context);
		final RelativeLayout rl = new RelativeLayout(context);
		rl.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT));
		final RelativeLayout view = (RelativeLayout) inflater.inflate(
						textResId, rl, true);
		view.setBackgroundResource(backgroundImageId);

		final TextView textView = (TextView) view.findViewById(R.id.count);
		if (count == 0) {
			textView.setVisibility(View.GONE);
		} else {
			textView.setText(String.valueOf(count));
		}

		view.measure(
						View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
						View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

		view.setDrawingCacheEnabled(true);
		view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		final Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
		view.setDrawingCacheEnabled(false);

		return new BitmapDrawable(context.getResources(), bitmap);
	}

	private void setupFilterMenuIfNeeded(Context context, Menu menu) {
		MenuItem menuItem = menu.findItem(R.id.action_filter);
		if (menuItem != null) {
			if (scheduleFilterManager.isSomeFiltersActive()) {
				final int activeFiltersCount = scheduleFilterManager.activeFiltersCount();
				menuItem.setIcon(buildCounterDrawable(context, activeFiltersCount, R.drawable.ic_filter_white_24px,
								R.layout.toolbar_menu_item_with_badge_view));
			} else {
				menu.findItem(R.id.action_filter).setIcon(R.drawable.ic_filter_outline_white_24px);
			}
		}
	}

	private void setupSearchViewIfNeeded(Menu menu) {
		final MenuItem searchItem = menu.findItem(R.id.action_search);
		if (searchItem != null) {
			SearchView searchView = null;
			if (searchItem != null) {
				searchView = (SearchView) searchItem.getActionView();
			}

			if (searchView != null) {
				searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
					@Override
					public boolean onQueryTextSubmit(String s) {
						onSearchQueryInternal(validateQuery(s));
						return false;
					}

					@Override
					public boolean onQueryTextChange(String s) {
						onSearchQueryInternal(validateQuery(s));
						return false;
					}
				});

				searchView.setOnCloseListener(() -> {
					onSearchQueryInternal("");
					return false;
				});

				searchView.setQueryHint(getString(R.string.search_hint));
			}
		}
	}

	private CountDownTimer countDownTimer = new CountDownTimer(350, 1) {
		@Override
		public void onTick(long millisUntilFinished) {
			// Nothing.
		}

		@Override
		public void onFinish() {
			if (isActivityLive() && isAdded()) {
				onSearchQuery(lastQuery);
			}
		}
	};

	private void onSearchQueryInternal(String query) {
		lastQuery = query;
		countDownTimer.cancel();
		countDownTimer.start();
	}

	private String validateQuery(String query) {
		return query.trim();
	}

	@Override
	public void onPause() {
		closeFilterDialogIfNeeded();
		super.onPause();
	}

	protected void closeFilterDialogIfNeeded() {
		if (filtersDialog != null) {
			filtersDialog.dismiss();
		}
	}

	@OptionsItem(R.id.action_filter)
	protected void onFilterClicked() {
		final List<RealmScheduleDayItemFilter> dayFilters = scheduleFilterManager.getDayFilters();
		final List<RealmScheduleTrackItemFilter> trackFilters = scheduleFilterManager.getTrackFilters();
		final List<RealmScheduleCustomFilter> customFilters = scheduleFilterManager.getCustomFilters();
		filtersDialog = FiltersDialog.showFiltersDialog(getContext(), dayFilters, trackFilters, customFilters, this);
	}

	@OptionsItem(R.id.action_integration)
	protected void onIntegrationIconClick() {
		integrationProvider.provideIntegrationController()
						.handleToolbarIconClick(conferenceManager.getActiveConference()
										.get().getIntegrationId(), getActivity());
	}

	@OptionsItem(R.id.action_settings)
	protected void onSettingsClick() {
		SettingsActivity_.intent(this).start();
	}

	@OptionsItem(R.id.action_about)
	protected void onAboutClick() {
		AboutActivity_.intent(this).start();
	}

	@OptionsItem(R.id.action_purchase_ticket)
	protected void onRegisterClick() {
		final Optional<RealmConference> conference = conferenceManager.getActiveConference();
		if (conference.isPresent()) {
			navigator.openRegister(getActivity(), conference.get().getRegURL());
		}
	}

	@OptionsItem(R.id.action_report_issue)
	protected void onReportIssueClick() {
		navigator.reportIssue(getActivity());
	}
}
