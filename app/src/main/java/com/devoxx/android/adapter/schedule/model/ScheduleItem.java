package com.devoxx.android.adapter.schedule.model;

import com.annimon.stream.Optional;
import com.devoxx.android.adapter.schedule.ScheduleDayLineupAdapter;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.utils.tuple.Tuple;

import java.util.List;

public abstract class ScheduleItem {

	private final Tuple<Long, Long> slotTimespan;
	private int startIndex, stopIndex;

	ScheduleItem(long startTime, long endTime, int aStartIndex, int aStopIndex) {
		startIndex = aStartIndex;
		this.stopIndex = aStopIndex;
		slotTimespan = new Tuple<>(startTime, endTime);
	}

	public long getStartTime() {
		return slotTimespan.first;
	}

	public long getEndTime() {
		return slotTimespan.second;
	}

	public int getStopIndex() {
		return stopIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public void setStopIndex(int stopIndex) {
		this.stopIndex = stopIndex;
	}

	public abstract int getSize();

	@ScheduleDayLineupAdapter.ViewType
	public abstract int getItemType(int position);

	public abstract Optional<SlotApiModel> getItem(int position);

	public abstract List<SlotApiModel> getAllItems();
}
