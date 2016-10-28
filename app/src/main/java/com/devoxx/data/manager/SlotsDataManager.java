package com.devoxx.data.manager;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.crashlytics.android.Crashlytics;
import com.devoxx.R;
import com.devoxx.android.fragment.schedule.ScheduleLineupFragment;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.dao.SlotDao;
import com.devoxx.data.downloader.SlotsDownloader;
import com.devoxx.utils.InfoUtil;
import com.devoxx.utils.Logger;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EBean(scope = EBean.Scope.Singleton)
public class SlotsDataManager extends AbstractDataManager<SlotApiModel> {

	@Bean
	SlotsDownloader slotsDownloader;

	@Bean
	SlotDao slotDao;

	@Bean
	InfoUtil infoUtil;

	private List<SlotApiModel> allSlots = new ArrayList<>();
	private List<SlotApiModel> talks = new ArrayList<>();

	@AfterInject void afterInject() {
		allSlots.clear();
		allSlots.addAll(slotDao.getAllSlots());

		talks.clear();
		talks.addAll(Stream.of(allSlots)
				.filter(value -> value.isTalk() && !value.isBreak())
				.filter(value1 -> !value1.notAllocated)
				.collect(Collectors.<SlotApiModel>toList()));
	}

	public Optional<SlotApiModel> getSlotByTalkId(final String talkId) {
		return Stream.of(allSlots).filter(value -> value.isTalk() && !value.isBreak()
				&& value.talk.id.equals(talkId)).findFirst();
	}

	public List<SlotApiModel> getLastTalks() {
		return Stream.of(talks).distinct().collect(Collectors.toList());
	}

	public List<SlotApiModel> getSlotsForDay(final long timeMs) {
		final DateTime requestedDate = new DateTime(timeMs);
		final DateTime tmpDate = new DateTime();
		final DateTimeComparator dateComparator = DateTimeComparator.getDateOnlyInstance();

		return Stream.of(allSlots)
				.filter(value -> dateComparator.compare(requestedDate, tmpDate.withMillis(
						value.fromTimeMs())) == 0)
				.filter(value1 -> !value1.notAllocated)
				.distinct()
				.collect(Collectors.toList());
	}

	public boolean fetchTalksSync(final SlotsDownloader.DownloadRequest downloadRequest) throws IOException {
		updateTalksSync(downloadRequest, true, false);

		return !Stream.of(allSlots).filter(SlotApiModel::isTalk)
				.collect(Collectors.toList()).isEmpty();
	}

	private void updateTalksSync(SlotsDownloader.DownloadRequest downloadRequest, boolean withClear, boolean forPush) throws IOException {
		if (withClear) {
			allSlots.clear();
		}

		if (forPush) {
			allSlots = slotsDownloader.downloadTalksForPush(downloadRequest);
		} else {
			allSlots = slotsDownloader.downloadTalks(downloadRequest);
		}

		slotDao.saveSlots(allSlots);

		final List<SlotApiModel> talks = Stream.of(allSlots)
				.filter(value -> value.talk != null && !value.isBreak())
				.collect(Collectors.<SlotApiModel>toList());
		this.talks.clear();
		this.talks.addAll(talks);
	}

	@Override
	public void clearData() {
		allSlots.clear();
		talks.clear();
		slotDao.clearData();
	}

	public void updateSlotsAsync(Context context, SlotsDownloader.DownloadRequest downloadRequest) {
		final HandlerThread handlerThread = new HandlerThread("updateSlotsAsync");
		handlerThread.start();
		final Looper looper = handlerThread.getLooper();
		new Handler(looper).post(() -> {
			try {
				if (slotsDownloader.isDownloadNeeded(downloadRequest.getConfCode())) {
					updateTalksSync(downloadRequest, false, false);
					context.sendBroadcast(ScheduleLineupFragment.getRefreshIntent());
				}
			} catch (IOException e) {
				Crashlytics.logException(e);
				Logger.exc(e);
			}
		});
	}

	public void forceUpdateSlotsAsync(Context context, SlotsDownloader.DownloadRequest downloadRequest) {
		final Intent notifyIntent = ScheduleLineupFragment.getReFetchIntent();
		forceUpdateSlotsAsyncHelper(context, downloadRequest, notifyIntent);
	}

	private void forceUpdateSlotsAsyncHelper(Context context, SlotsDownloader.DownloadRequest downloadRequest, Intent notifyIntent) {
		final HandlerThread handlerThread = new HandlerThread("forceUpdateSlotsAsync");
		handlerThread.start();
		final Looper looper = handlerThread.getLooper();
		new Handler(looper).post(() -> {
			try {
				infoUtil.showToast(R.string.updating_schedule_data);
				updateTalksSync(downloadRequest, true, true);
				context.sendBroadcast(notifyIntent);
				infoUtil.showToast(R.string.updated_schedule_data);
			} catch (IOException e) {
				Crashlytics.logException(e);
				infoUtil.showToast(R.string.connection_error);
			}
		});
	}
}
