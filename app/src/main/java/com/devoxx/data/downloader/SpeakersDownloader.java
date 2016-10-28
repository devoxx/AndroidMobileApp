package com.devoxx.data.downloader;

import com.devoxx.connection.Connection;
import com.devoxx.connection.DevoxxApi;
import com.devoxx.connection.model.SpeakerShortApiModel;
import com.devoxx.data.DataInformation_;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.cache.SpeakerCache;
import com.devoxx.data.cache.SpeakersCache;
import com.devoxx.data.model.RealmSpeaker;
import com.devoxx.data.model.RealmSpeakerShort;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.IOException;
import java.util.List;

import io.realm.Realm;
import okhttp3.ResponseBody;
import retrofit2.Call;

@EBean
public class SpeakersDownloader {

	@Bean
	Connection connection;

	@Bean
	RealmProvider realmProvider;

	@Bean
	SpeakerCache speakerCache;

	@Bean
	SpeakersCache speakersCache;

	@Pref
	DataInformation_ dataInformation;

	public RealmSpeaker downloadSpeakerSync(final String confCode, final String uuid) throws IOException {
		final RealmSpeaker result;
		if (!speakerCache.isValid(uuid)) {
			final DevoxxApi devoxxApi = connection.getDevoxxApi();

			final Call<ResponseBody> callSpeaker = devoxxApi.speaker(confCode, uuid);
			final ResponseBody responseBody = callSpeaker.execute().body();

			if (responseBody == null) {
				throw new IOException("No response body!");
			}

			final String rawModel = responseBody.string();
			speakerCache.upsert(rawModel, uuid);

			final Realm realm = realmProvider.getRealm();
			realm.beginTransaction();
			result = realm.createOrUpdateObjectFromJson(RealmSpeaker.class, rawModel);
			realm.commitTransaction();
			realm.close();
		} else {
			final Realm realm = realmProvider.getRealm();
			result = realm.where(RealmSpeaker.class).equalTo("uuid", uuid).findFirst();
			realm.close();
		}
		return result;
	}

	public List<SpeakerShortApiModel> downloadSpeakersShortInfoList(final String confCode) throws IOException {
		final List<SpeakerShortApiModel> speakers;
		if (speakersCache.isValid()) {
			speakers = speakersCache.getData();
		} else {
			final DevoxxApi devoxxApi = connection.getDevoxxApi();
			final Call<List<SpeakerShortApiModel>> call = devoxxApi.speakers(confCode);
			speakers = call.execute().body();

			if (speakers == null) {
				throw new IOException("No response body!");
			}

			speakersCache.upsert(speakers);
		}

		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		for (SpeakerShortApiModel apiModel : speakers) {
			final RealmSpeakerShort speakerShort = RealmSpeakerShort.fromApi(apiModel);
			realm.copyToRealmOrUpdate(speakerShort);
		}
		realm.commitTransaction();
		realm.close();

		return speakers;
	}
}
