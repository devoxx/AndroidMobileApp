package com.devoxx.android.fragment.schedule;

import com.annimon.stream.Optional;
import com.devoxx.R;
import com.devoxx.android.adapter.schedule.ScheduleDayLineupAdapter;
import com.devoxx.android.adapter.schedule.model.ScheduleItem;
import com.devoxx.android.adapter.schedule.model.creator.ScheduleLineupDataCreator;
import com.devoxx.android.fragment.common.BaseListFragment;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.DataInformation_;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.schedule.filter.ScheduleFilterManager;
import com.devoxx.data.schedule.search.SearchManager;
import com.devoxx.navigation.Navigator;
import com.devoxx.navigation.NeededUpdateListener;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EFragment(R.layout.fragment_list)
public class ScheduleLineupFragment extends BaseListFragment implements NeededUpdateListener {

	public static final String REFRESH_ACTION = "com.devoxx.android.intent.REFRESH_ACTION";

	private static final long UNKNOWN_LINEUP_TIME = -1;
	private static final long CHECK_RUNNING_SESSIONS_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);

	@FragmentArg
	long lineupDayMs = UNKNOWN_LINEUP_TIME;

	@Bean
	Navigator navigator;

	@Bean
	ScheduleDayLineupAdapter scheduleDayLineupAdapter;

	@Bean
	InfoUtil infoUtil;

	@Bean
	ScheduleLineupDataCreator scheduleLineupDataCreator;

	@Pref
	DataInformation_ dataInformation;

	@AfterInject void afterInject() {
		if (lineupDayMs == UNKNOWN_LINEUP_TIME) {
			throw new RuntimeException("Lineup day time must be provided!");
		}
	}

	@Override
	protected void afterViews() {
		super.afterViews();
		scheduleDayLineupAdapter.setListener(this);
		initAdapterWithLastQuery();
	}

	@Override
	public void onResume() {
		super.onResume();
		triggerRunningSessionCheck();
	}

	public void triggerRunningSessionCheck() {
		checkNowRunningSessions();
	}

	private void checkNowRunningSessions() {
		final int runningIndex = scheduleDayLineupAdapter.getRunningFirstPosition();
		final boolean hasRunningSession = runningIndex != ScheduleDayLineupAdapter.INVALID_RUNNING_SLOT_INDEX;
		if (!hasRunningSession) {
			return;
		}

		final long lastCheck = dataInformation.lastRunningSessionCheckTime().get();
		final long now = ConferenceManager.getNow();
		final long diffMs = now - lastCheck;
		final boolean shouldForceCheck = Math.signum(diffMs) == -1; // User changed time in system.

		if (shouldForceCheck || diffMs > CHECK_RUNNING_SESSIONS_INTERVAL_MS) {
			dataInformation.edit().lastRunningSessionCheckTime().put(now).apply();
			initAdapterWithLastQuery();
		}
	}

	@Receiver(actions = {SearchManager.SEARCH_INTENT_ACTION,
			ScheduleFilterManager.FILTERS_CHANGED_ACTION}) void onRefreshData() {
		final String lastQuery = searchManager.getLastQuery();
		List<ScheduleItem> items = searchManager.handleSearchQuery(lineupDayMs, lastQuery);
		items = filterManager.applyTracksFilter(items);
		scheduleDayLineupAdapter.setData(items);
		scheduleDayLineupAdapter.notifyDataSetChanged();
	}

	@Override
	protected RecyclerView.Adapter getAdapter() {
		return scheduleDayLineupAdapter;
	}

	@Override
	public void onListAdapterItemClick(View clickedView, int position, long id) {
		final Optional<SlotApiModel> slotApiModel = scheduleDayLineupAdapter.getClickedSlot(position);
		if (slotApiModel.isPresent() && slotApiModel.get().isTalk()) {
			navigator.openTalkDetails(getMainActivity(), slotApiModel.get(), getParentFragment(), false);
		}
	}

	@Receiver(actions = {REFRESH_ACTION}) void onTalkNotification() {
		onRefreshData();
	}

	public static Intent getRefreshIntent() {
		return new Intent(REFRESH_ACTION);
	}

	private void initAdapterWithLastQuery() {
		final String lastQuery = searchManager.getLastQuery();
		List<ScheduleItem> items;
		if (!TextUtils.isEmpty(lastQuery)) {
			items = searchManager.handleSearchQuery(lineupDayMs, lastQuery);
		} else {
			items = scheduleLineupDataCreator.prepareInitialData(lineupDayMs);
		}
		items = filterManager.applyTracksFilter(items);
		scheduleDayLineupAdapter.setData(items);
		scheduleDayLineupAdapter.notifyDataSetChanged();
	}

	@Override
	public void refreshData() {
		onRefreshData();
	}
}
