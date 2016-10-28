package com.devoxx.data.downloader;

import com.devoxx.common.utils.Constants;
import com.devoxx.common.wear.GoogleApiConnector;
import com.devoxx.connection.Connection;
import com.devoxx.connection.DevoxxApi;
import com.devoxx.connection.cfp.model.ConferenceApiModel;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.connection.model.SpecificScheduleApiModel;
import com.devoxx.data.cache.SlotsCache;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.model.RealmConference;
import com.google.gson.Gson;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.joda.time.DateTime;

import android.content.Context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;

@EBean
public class SlotsDownloader {

	@Bean Connection connection;
	@Bean SlotsCache slotsCache;
	@RootContext Context context;

	private List<SlotApiModel> downloadTalksHelper(DownloadRequest downloadRequest, boolean force) throws IOException {
		final List<SlotApiModel> result;

		if (slotsCache.isValid(downloadRequest.confCode) && !force) {
			result = slotsCache.getData(downloadRequest.confCode);
		} else {
			result = downloadAllData(downloadRequest);
			slotsCache.upsert(deserializeData(result), downloadRequest.confCode);
		}

		return result;
	}

	public List<SlotApiModel> downloadTalks(DownloadRequest downloadRequest) throws IOException {
		return downloadTalksHelper(downloadRequest, false);
	}

	public List<SlotApiModel> downloadTalksForPush(DownloadRequest downloadRequest) throws IOException {
		return downloadTalksHelper(downloadRequest, true);
	}

	public boolean isDownloadNeeded(String confCode) {
		return !slotsCache.isValid(confCode);
	}

	private String deserializeData(List<SlotApiModel> result) {
		return new Gson().toJson(result);
	}

	private List<SlotApiModel> downloadAllData(DownloadRequest downloadRequest) throws IOException {

		final GoogleApiConnector googleApiConnector = new GoogleApiConnector(context);

		// clear the cache used by the wearable device
		googleApiConnector.deleteAllItems(Constants.CHANNEL_ID);

		final Set<SlotApiModel> result = new HashSet<>();
		for (String day : downloadRequest.days) {
			downloadTalkSlotsForDay(downloadRequest.confCode, result, day);
		}

		googleApiConnector.disconnect();

		return new ArrayList<>(result);
	}

	private void downloadTalkSlotsForDay(
			String confCode, Set<SlotApiModel> result, String day) throws IOException {

		final DevoxxApi devoxxApi = connection.getDevoxxApi();

		if (devoxxApi != null) {
			final Call<SpecificScheduleApiModel> call = devoxxApi.specificSchedule(confCode, day);
			final SpecificScheduleApiModel body = call.execute().body();

			if (body != null && body.slots != null) {
				result.addAll(body.slots);
			}
		}
	}

	public static class DownloadRequest {
		private final List<String> days;
		private final String confCode;

		public DownloadRequest(ConferenceApiModel conference) {
			days = initDays(ConferenceManager.parseConfDate(conference.fromDate),
					ConferenceManager.parseConfDate(conference.toDate));
			this.confCode = conference.id;
		}

		public DownloadRequest(RealmConference conference) {
			days = initDays(ConferenceManager.parseConfDate(conference.getFromDate()),
					ConferenceManager.parseConfDate(conference.getToDate()));
			this.confCode = conference.getId();
		}

		private List<String> initDays(DateTime start, DateTime end) {
			final int daysBetween = end.getDayOfYear() - start.getDayOfYear();
			final List<String> result = new ArrayList<>(daysBetween);

			for (int i = 0; i <= daysBetween; i++) {
				final String dayName = start.dayOfWeek().getAsText(Locale.US).toLowerCase();
				result.add(dayName);
				start = start.plusDays(1);
			}

			return result;
		}

		public String getConfCode() {
			return confCode;
		}
	}
}
