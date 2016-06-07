package com.devoxx.android.fragment.track;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.devoxx.R;
import com.devoxx.android.adapter.track.TracksAdapter;
import com.devoxx.android.fragment.common.BaseListFragment;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.manager.SlotsDataManager;
import com.devoxx.data.schedule.filter.model.RealmScheduleDayItemFilter;
import com.devoxx.data.schedule.search.SearchManager;
import com.devoxx.navigation.Navigator;
import com.devoxx.navigation.NeededUpdateListener;
import com.devoxx.utils.DateUtils;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.Receiver;
import org.joda.time.DateTime;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;

@EFragment(R.layout.fragment_list)
public class TracksListFragment extends BaseListFragment implements NeededUpdateListener {

	@Bean
	SlotsDataManager slotsDataManager;

	@Bean
	Navigator navigator;

	@Bean
	TracksAdapter tracksAdapter;

	@FragmentArg
	String trackId;

	@AfterInject void afterInject() {
		final String lastQuery = searchManager.getLastQuery();
		final List<SlotApiModel> tracks = filterSlotsByDayWithLastQuery(lastQuery);
		tracksAdapter.setData(tracks);
		tracksAdapter.setItemClickListener(this);
	}

	@AfterViews void afterViewsInternal() {
		super.afterViews();
		scrollToFirstActiveItem();
	}

	@Override public void onListAdapterItemClick(View clickedView, int position, long id) {
		final SlotApiModel slotApiModel = tracksAdapter.getClickedItem(position);
		navigator.openTalkDetails(getMainActivity(), slotApiModel, getParentFragment(), false);
	}

	@Override protected RecyclerView.Adapter getAdapter() {
		return tracksAdapter;
	}

	@Receiver(actions = {SearchManager.SEARCH_INTENT_ACTION}) void onRefreshData() {
		final String lastQuery = searchManager.getLastQuery();
		final List<SlotApiModel> tracks = filterSlotsByDayWithLastQuery(lastQuery);
		tracksAdapter.setData(tracks);
		tracksAdapter.notifyDataSetChanged();
		scrollToFirstActiveItem();
	}

	private void scrollToFirstActiveItem() {
		final int index = tracksAdapter.getRunningFirstIndex();
		if (index != TracksAdapter.INVALID_RUNNING_FIRST_INDEX) {
			recyclerView.scrollToPosition(index);
		}
	}

	private SlotApiModel.FilterPredicate filterPredicate = new SlotApiModel.FilterPredicate();

	private List<SlotApiModel> filterSlotsByDayWithLastQuery(String lastQuery) {
		filterPredicate.setQuery(lastQuery);

		final List<SlotApiModel> slots =
				Stream.of(slotsDataManager.getLastTalks())
						.filter(slot -> {
							final boolean properTrack = slot.talk != null
									&& slot.talk.trackId.equalsIgnoreCase(trackId);
							final boolean properTalk = filterPredicate.test(slot);

							return properTrack && properTalk;
						})
						.collect(Collectors.<SlotApiModel>toList());

		final List<RealmScheduleDayItemFilter> dayFilters
				= filterManager.getActiveDayFilters();
		final DateTime filterTime = new DateTime();
		final DateTime slotDate = new DateTime();

		return Stream.of(slots)
				.filter(value -> {
					final DateTime rhs = slotDate.withMillis(value.fromTimeMs());
					for (RealmScheduleDayItemFilter dayFilter : dayFilters) {
						final DateTime lhs = filterTime.withMillis(dayFilter.getDayMs());
						if (DateUtils.isSameDay(lhs, rhs)) {
							return true;
						}
					}
					return false;
				})
				.collect(Collectors.toList());
	}

	@Override
	public void refreshData() {
		onRefreshData();
	}
}
