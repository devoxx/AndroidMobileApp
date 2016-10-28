package com.devoxx.android.adapter.schedule;

import com.annimon.stream.Optional;
import com.devoxx.android.adapter.ListAdapterClickListener;
import com.devoxx.android.adapter.schedule.model.BreakScheduleItem;
import com.devoxx.android.adapter.schedule.model.ScheduleItem;
import com.devoxx.android.adapter.schedule.model.TalksScheduleItem;
import com.devoxx.android.adapter.schedule.model.creator.ScheduleLineupDataCreator;
import com.devoxx.android.view.list.schedule.BreakItemView_;
import com.devoxx.android.view.list.schedule.TalkItemView_;
import com.devoxx.android.view.list.schedule.TalksMoreItemView_;
import com.devoxx.android.view.list.schedule.TimespanItemView_;
import com.devoxx.android.view.listholder.schedule.BadItemHolder;
import com.devoxx.android.view.listholder.schedule.BaseItemHolder;
import com.devoxx.android.view.listholder.schedule.BreakItemHolder;
import com.devoxx.android.view.listholder.schedule.TalkItemHolder;
import com.devoxx.android.view.listholder.schedule.TalksMoreItemHolder;
import com.devoxx.android.view.listholder.schedule.TimespanItemHolder;
import com.devoxx.connection.model.SlotApiModel;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

@EBean
public class ScheduleDayLineupAdapter extends RecyclerView.Adapter<BaseItemHolder> {

	public static final int TIMESPAN_VIEW = 1;
	public static final int BREAK_VIEW = 2;
	public static final int TALK_VIEW = 3;
	public static final int TALK_MORE_VIEW = 4;
	static final int BAD_VIEW_TYPE = 5;
	public static final int INVALID_RUNNING_SLOT_INDEX = -1;

	@IntDef({
			TIMESPAN_VIEW,
			BREAK_VIEW,
			TALK_VIEW,
			TALK_MORE_VIEW,
			BAD_VIEW_TYPE
	})
	@Retention(RetentionPolicy.SOURCE)
	public @interface ViewType {
	}

	@Bean
	ScheduleLineupDataCreator scheduleLineupDataCreator;

	private final List<ScheduleItem> data = new ArrayList<>();
	private ListAdapterClickListener clickListener;

	public void setData(List<ScheduleItem> aData) {
		data.clear();
		data.addAll(aData);
	}

	public void setListener(ListAdapterClickListener listener) {
		clickListener = listener;
	}

	public Optional<SlotApiModel> getClickedSlot(int position) {
		final ScheduleItem scheduleItem = getItem(position);
		return (scheduleItem != null) ? scheduleItem.getItem(position) : Optional.empty();
	}

	@Override
	public BaseItemHolder onCreateViewHolder(
			ViewGroup parent, @ScheduleDayLineupAdapter.ViewType int viewType) {
		final Context context = parent.getContext();
		final BaseItemHolder result;

		switch (viewType) {
			case TIMESPAN_VIEW:
				result = new TimespanItemHolder(TimespanItemView_.build(context));
				break;
			case BREAK_VIEW:
				result = new BreakItemHolder(BreakItemView_.build(context));
				break;
			case TALK_VIEW:
				result = new TalkItemHolder(TalkItemView_.build(context));
				break;
			case TALK_MORE_VIEW:
				result = new TalksMoreItemHolder(TalksMoreItemView_.build(context));
				break;
			case BAD_VIEW_TYPE:
				result = new BadItemHolder(new View(context));
				break;
			default:
				throw new IllegalStateException("No holder for view type: " + viewType);
		}

		return result;
	}

	@Override
	public void onBindViewHolder(BaseItemHolder holder, int position) {
		if (holder instanceof BreakItemHolder) {
			setupBreakItemHolder((BreakItemHolder) holder, getItem(position));
		} else if (holder instanceof TalkItemHolder) {
			setupTalkItemHolder(holder, getItem(position), position);
		} else if (holder instanceof TimespanItemHolder) {
			setupTimespanItemHolder((TimespanItemHolder) holder, getItem(position));
		} else if (holder instanceof TalksMoreItemHolder) {
			setupMoreItemHolder((TalksMoreItemHolder) holder, position);
		}
	}

	public int getRunningFirstPosition() {
		for (ScheduleItem item : data) {
			if (item instanceof TalksScheduleItem && ((TalksScheduleItem) item).isRunning()) {
				return item.getStartIndex();
			}
		}
		return INVALID_RUNNING_SLOT_INDEX;
	}

	private void setupMoreItemHolder(TalksMoreItemHolder holder, int position) {
		final TalksScheduleItem item = (TalksScheduleItem) getItem(position);
		holder.setRunIndicatorVisibility(item);

		holder.setupMore(item, () -> {
			item.switchTalksVisibility();
			holder.toggleIndicator();
			scheduleLineupDataCreator.refreshIndexes(data);

			final int start = item.getStartIndexForHide();
			final int count = item.getItemCountForHide();
			if (item.isOthersVisible()) {
				notifyItemRangeInserted(start, count);
			} else {
				notifyItemRangeRemoved(start, count);
			}

			holder.setRunIndicatorVisibility(item);
		});
	}

	private void setupTimespanItemHolder(TimespanItemHolder holder, ScheduleItem scheduleItem) {
		final TalksScheduleItem item = (TalksScheduleItem) scheduleItem;
		holder.setupTimespan(item.getStartTime(), item.getEndTime(), item.isRunning());
	}

	private void setupBreakItemHolder(BreakItemHolder holder, ScheduleItem scheduleItem) {
		final BreakScheduleItem breakScheduleItem = (BreakScheduleItem) scheduleItem;
		final SlotApiModel breakModel = breakScheduleItem.getBreakModel();
		holder.setupBreak(breakModel);
	}

	private void setupTalkItemHolder(BaseItemHolder holder, ScheduleItem scheduleItem, int position) {
		final TalksScheduleItem item = (TalksScheduleItem) scheduleItem;
		final Optional<SlotApiModel> slotModel = item.getItem(position);
		if (slotModel.isPresent()) {
			// Show running indicator for last talk in the item.
			final boolean withRunningIndicator = item.isRunning() && position == item.getStopIndex();
			((TalkItemHolder) holder).setupTalk(slotModel.get(), withRunningIndicator);
			setupOnItemClickListener(holder, position);
		}
	}

	private void setupOnItemClickListener(BaseItemHolder holder, int position) {
		holder.itemView.setOnClickListener(v ->
				clickListener.onListAdapterItemClick(v, position, getItemId(position)));
	}

	@Override
	@ScheduleDayLineupAdapter.ViewType
	public int getItemViewType(int position) {
		final ScheduleItem scheduleItem = getItem(position);
		return (scheduleItem != null) ? scheduleItem.getItemType(position) : BAD_VIEW_TYPE;
	}

	@Override
	public int getItemCount() {
		int result = 0;
		for (ScheduleItem scheduleItem : data) {
			result += scheduleItem.getSize();
		}
		return result;
	}

	@Nullable
	private ScheduleItem getItem(int position) {
		for (ScheduleItem scheduleItem : data) {
			final int startIndex = scheduleItem.getStartIndex();
			final int stopIndex = scheduleItem.getStopIndex();
			if (position >= startIndex && position <= stopIndex) {
				return scheduleItem;
			}
		}

		return null;
	}
}
