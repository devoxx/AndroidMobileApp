package com.devoxx.data.manager;

import com.devoxx.connection.model.SpeakerShortApiModel;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.downloader.SpeakersDownloader;
import com.devoxx.data.model.RealmSpeaker;
import com.devoxx.data.model.RealmSpeakerShort;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Case;
import io.realm.Realm;

@EBean(scope = EBean.Scope.Singleton)
public class SpeakersDataManager extends AbstractDataManager<RealmSpeaker> {

	@Bean
	SpeakersDownloader speakersDownloader;

	@Bean
	RealmProvider realmProvider;

	private Map<String, String> uuidToImageUrl;

	public List<SpeakerShortApiModel> fetchSpeakersSync(final String confCode) throws IOException {
		return speakersDownloader.downloadSpeakersShortInfoList(confCode);
	}

	@Background
	public void fetchSpeakerAsync(
			final String confCode, final String uuid,
			final IDataManagerListener<RealmSpeaker> listener) {
		try {
			notifyAboutStart(listener);
			final RealmSpeaker result;
			if (isExists(uuid)) {
				result = getByUuid(uuid);
			} else {
				result = speakersDownloader.downloadSpeakerSync(confCode, uuid);
			}
			notifyAboutSuccess(listener, result);
		} catch (IOException e) {
			notifyAboutFailed(listener, e);
		}
	}

	public boolean isExists(String uuid) {
		return getByUuid(uuid) != null;
	}

	public RealmSpeaker getByUuid(String uuid) {
		final Realm realm = realmProvider.getRealm();
		final RealmSpeaker result = realm.where(RealmSpeaker.class).
				equalTo(RealmSpeaker.Contract.UUID, uuid).findFirst();
		realm.close();

		return result;
	}

	public List<RealmSpeakerShort> getAllShortSpeakers() {
		final Realm realm = realmProvider.getRealm();
		final List<RealmSpeakerShort> result = realm.allObjects(RealmSpeakerShort.class);
		realm.close();

		return result;
	}

	public List<RealmSpeakerShort> getAllShortSpeakersWithFilter(String query) {
		final Realm realm = realmProvider.getRealm();
		final List<RealmSpeakerShort> result = realm
				.where(RealmSpeakerShort.class)
				.contains("firstName", query, Case.INSENSITIVE)
				.or()
				.contains("lastName", query, Case.INSENSITIVE)
				.findAll();
		realm.close();

		return result;
	}

	@Override
	public void clearData() {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		realm.allObjects(RealmSpeaker.class).clear();
		realm.allObjects(RealmSpeakerShort.class).clear();
		realm.commitTransaction();
		realm.close();
	}

	@Nullable
	public String imageUrlByUuid(String uuid) {
		if (uuidToImageUrl == null) {
			createSpeakersRepository();
		}

		final String result = uuidToImageUrl.get(uuid);

		return TextUtils.isEmpty(result) ? "" : result;
	}

	public void createSpeakersRepository() {
		final Realm realm = realmProvider.getRealm();
		final List<RealmSpeakerShort> speakers = realm.allObjects(RealmSpeakerShort.class);
		uuidToImageUrl = new HashMap<>(speakers.size());

		for (RealmSpeakerShort speaker : speakers) {
			uuidToImageUrl.put(speaker.getUuid(), speaker.getAvatarURL());
		}

		realm.close();
	}
}
