package com.devoxx.android.adapter.track;

import com.devoxx.android.adapter.ListAdapterClickListener;
import com.devoxx.android.view.list.schedule.TalkItemView_;
import com.devoxx.android.view.listholder.track.BaseTrackHolder;
import com.devoxx.android.view.listholder.track.TalkTrackHolder;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.downloader.TracksDownloader;
import com.devoxx.data.manager.NotificationsManager;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

@EBean
public class TracksAdapter extends RecyclerView.Adapter<BaseTrackHolder> {

	public static final int INVALID_RUNNING_FIRST_INDEX = -1;

	@Bean
	TracksDownloader tracksDownloader;

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	NotificationsManager notificationsManager;

	private final List<SlotApiModel> data = new ArrayList<>();
	private ListAdapterClickListener clickListener;

	public void setData(List<SlotApiModel> aData) {
		data.clear();
		data.addAll(aData);
	}

	@Override
	public BaseTrackHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new TalkTrackHolder(TalkItemView_.build(parent.getContext()));
	}

	@Override
	public void onBindViewHolder(BaseTrackHolder holder, int position) {
		final SlotApiModel slot = data.get(position);
		boolean isPreviousAlsoRunning = false;
		if (position > 0) {
			isPreviousAlsoRunning = isRunningItem(data.get(position - 1));
		}

		holder.setupView(slot, isRunningItem(slot), isPreviousAlsoRunning);

		holder.itemView.setOnClickListener(v ->
				clickListener.onListAdapterItemClick(v, position, getItemId(position)));
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	public SlotApiModel getClickedItem(int position) {
		return data.get(position);
	}

	public int getRunningFirstIndex() {
		int index = INVALID_RUNNING_FIRST_INDEX;
		final int size = data.size();
		for (int i = 0; i < size; i++) {
			if (isRunningItem(data.get(i))) {
				index = i;
				break;
			}
		}
		return index;
	}

	private boolean isRunningItem(SlotApiModel slot) {
		final long currentTime = ConferenceManager.getNow();
		return slot.fromTimeMs() <= currentTime
				&& slot.toTimeMs() >= currentTime;
	}

	public void setItemClickListener(ListAdapterClickListener listener) {
		clickListener = listener;
	}
}
