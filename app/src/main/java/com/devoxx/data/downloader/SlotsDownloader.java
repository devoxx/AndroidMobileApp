package com.devoxx.data.downloader;

import android.content.Context;

import com.devoxx.common.utils.Constants;
import com.devoxx.common.wear.GoogleApiConnector;
import com.devoxx.connection.Connection;
import com.devoxx.connection.DevoxxApi;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.connection.model.SpecificScheduleApiModel;
import com.devoxx.data.cache.SlotsCache;
import com.google.gson.Gson;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;

@EBean
public class SlotsDownloader {

	private final List<String> AVAILABLE_CONFERENCE_DAYS = Collections.unmodifiableList(
			Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
	);

	@Bean
	Connection connection;

	@Bean
	SlotsCache slotsCache;

	@RootContext
	Context context;

	public List<SlotApiModel> forceDownloadTalks(String confCode) throws IOException {
		return downloadAllData(confCode);
	}

	public List<SlotApiModel> downloadTalks(String confCode) throws IOException {
		final List<SlotApiModel> result;

		if (slotsCache.isValid(confCode)) {
			result = slotsCache.getData(confCode);
		} else {
			result = downloadAllData(confCode);
		}

		return result;
	}

	private String deserializeData(List<SlotApiModel> result) {
		return new Gson().toJson(result);
	}

	private List<SlotApiModel> downloadAllData(String confCode) throws IOException {

		final GoogleApiConnector googleApiConnector = new GoogleApiConnector(context);

		// clear the cache used by the wearable device
		googleApiConnector.deleteAllItems(Constants.CHANNEL_ID);

		final List<SlotApiModel> result = new ArrayList<>();
		for (String day : AVAILABLE_CONFERENCE_DAYS) {
			downloadTalkSlotsForDay(confCode, result, day);
		}
		slotsCache.upsert(deserializeData(result), confCode);

		googleApiConnector.disconnect();

		return result;
	}

	private void downloadTalkSlotsForDay(
			String confCode, List<SlotApiModel> result, String day) throws IOException {
		final DevoxxApi devoxxApi = connection.getDevoxxApi();
		final Call<SpecificScheduleApiModel> call =
				devoxxApi.specificSchedule(confCode, day);

		final SpecificScheduleApiModel body = call.execute().body();
		if (body != null && body.slots != null) {
			result.addAll(body.slots);
		}
	}
}
