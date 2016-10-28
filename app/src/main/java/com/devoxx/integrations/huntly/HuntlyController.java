package com.devoxx.integrations.huntly;

import com.annimon.stream.Optional;
import com.devoxx.BuildConfig;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.register.BaseExtractor;
import com.devoxx.integrations.huntly.connection.HuntlyConnection;
import com.devoxx.integrations.huntly.connection.model.HuntlyActivityCompleteResponse;
import com.devoxx.integrations.huntly.connection.model.HuntlyDeepLinkConf;
import com.devoxx.integrations.huntly.connection.model.HuntlyEvent;
import com.devoxx.integrations.huntly.connection.model.HuntlyProfileProperty;
import com.devoxx.integrations.huntly.connection.model.HuntlyPromo;
import com.devoxx.integrations.huntly.connection.model.HuntlyQuestActivity;
import com.devoxx.integrations.huntly.connection.model.HuntlyRegisterResponse;
import com.devoxx.integrations.huntly.connection.model.HuntlyUserStats;
import com.devoxx.integrations.huntly.storage.RealmHuntlyDeepLinks;
import com.devoxx.integrations.huntly.storage.RealmHuntlyEvent;
import com.devoxx.integrations.huntly.storage.RealmHuntlyQuestActivity;
import com.devoxx.integrations.huntly.storage.RealmHuntlyUserStats;
import com.devoxx.utils.Logger;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.realm.Case;
import io.realm.Realm;
import retrofit2.Response;

@EBean
public class HuntlyController {

	public static final String USER_DATA_UPDATED = "HuntlyController.USER_DATA_UPDATED";

	static final long UNKNOWN_EVENT_ID = -1;

	@RootContext Context context;
	@Bean HuntlyConnection huntlyConnection;
	@Bean RealmProvider realmProvider;
	@Pref HuntlySettings_ huntlySettings;

	void register() {
		try {
			final String id = obtainId();
			final Response<HuntlyRegisterResponse> response = huntlyConnection.login(id);
			if (response.isSuccessful()) {
				final HuntlyRegisterResponse registerResponse = response.body();
				saveToken(registerResponse.getUser().getToken());
			}
		} catch (Exception exc) {
			Logger.exc(exc);
		}
	}

	@Background void updateUserProfileAsync(String confId, String finalCode,
	                                        BaseExtractor infoExtractor) {
		if (!isEventIdAvailable()) {
			return;
		}

		final Realm realm = realmProvider.getRealm();
		try {
			final long innerId = findRealmHuntlyEventId(confId);
			final List<HuntlyProfileProperty> properties = createProperties(finalCode, infoExtractor);
			huntlyConnection.updateUserProfile(innerId, properties);
		} catch (IOException e) {
			Logger.exc(e);
		} finally {
			realm.close();
		}
	}

	void fetchEvents(String confId) {
		try {
			final Response<List<HuntlyEvent>> response = huntlyConnection.events();
			if (response.isSuccessful()) {
				final List<HuntlyEvent> events = response.body();
				final Realm realm = realmProvider.getRealm();
				realm.beginTransaction();
				for (HuntlyEvent event : events) {
					realm.copyToRealmOrUpdate(RealmHuntlyEvent.fromApi(event));

					if (event.getExternalId().equalsIgnoreCase(confId)) {
						huntlySettings.edit().eventId().put(event.getEventId()).apply();
					}
				}
				realm.commitTransaction();
				realm.close();
			}
		} catch (IOException e) {
			Logger.exc(e);
		}
	}

	private void fetchPromoText(String confId) {
		final long innerId = findRealmHuntlyEventId(confId);
		try {
			final Response<HuntlyPromo> r = huntlyConnection.promo(innerId);
			if (r.isSuccessful()) {
				huntlySettings.edit().promo().put(r.body().getPromo()).apply();
			}
		} catch (IOException e) {
			Logger.exc(e);
		}
	}

	private void fetchDeepLinks(String confId) {
		final Realm realm = realmProvider.getRealm();
		final long innerId = findRealmHuntlyEventId(confId);
		try {
			final Response<HuntlyDeepLinkConf> r = huntlyConnection.deepLinks(innerId);
			if (r.isSuccessful()) {
				realm.beginTransaction();
				realm.copyToRealmOrUpdate(RealmHuntlyDeepLinks.fromApi(r.body(), confId));
				realm.commitTransaction();
			}
		} catch (IOException e) {
			Logger.exc(e);
		} finally {
			realm.close();
		}
	}

	private void fetchActivities(String confCode) {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		realm.clear(RealmHuntlyQuestActivity.class);
		realm.commitTransaction();

		final long innerId = findRealmHuntlyEventId(confCode);
		try {
			final Response<List<HuntlyQuestActivity>> r = huntlyConnection.activities(innerId);
			if (r.isSuccessful()) {
				final List<HuntlyQuestActivity> list = r.body();
				realm.beginTransaction();
				for (HuntlyQuestActivity ac : list) {
					realm.copyToRealmOrUpdate(RealmHuntlyQuestActivity.fromApi(ac));
				}
				realm.commitTransaction();
			}
		} catch (IOException e) {
			Logger.exc(e);
		} finally {
			realm.close();
		}
	}

	@Nullable HuntlyQuestActivity getVoteQuest() {
		return getQuest(HuntlyQuestActivity.QUEST_ACTIVITY_VOTE);
	}

	boolean isAnyVoteQuests() {
		return isQuestAvailable(HuntlyQuestActivity.QUEST_ACTIVITY_VOTE);
	}

	@Background void completeQuestAsync(HuntlyQuestActivity quest, String confCode) {
		try {
			final Response<HuntlyActivityCompleteResponse> r =
					huntlyConnection.completeQuest(quest.getQuestId());
			fetchActivities(confCode);
			if (r.isSuccessful()) {
				Logger.l(r.body().getStatus());
			}
		} catch (IOException e) {
			Logger.exc(e);
		}
	}

	boolean isAnyFirstRunQuest() {
		return isQuestAvailable(HuntlyQuestActivity.QUEST_ACTIVITY_FIRST_RUN);
	}

	@Nullable HuntlyQuestActivity getFirstRunQuest() {
		return getQuest(HuntlyQuestActivity.QUEST_ACTIVITY_FIRST_RUN);
	}

	public void fetchOtherData(String confCode) {
		if (isEventIdAvailable(confCode)) {
			fetchDeepLinks(confCode);
			fetchActivities(confCode);
			fetchPromoText(confCode);
			updateUserStats(null);

			logDataInfo();
		}
	}

	private void logDataInfo() {
		if (BuildConfig.DEBUG) {
			Logger.l("Huntly integration info, firstRunQuest: "
					+ getQuest(HuntlyQuestActivity.QUEST_ACTIVITY_FIRST_RUN)
					+ "\n" + "voteQuest: " + getQuest(HuntlyQuestActivity.QUEST_ACTIVITY_VOTE));
		}
	}

	boolean isEventIdAvailable() {
		return huntlySettings.eventId().get() != UNKNOWN_EVENT_ID;
	}

	boolean isEventIdAvailable(String confCode) {
		return findRealmHuntlyEventId(confCode) != UNKNOWN_EVENT_ID;
	}

	public void clear() {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		realm.clear(RealmHuntlyEvent.class);
		realm.clear(RealmHuntlyQuestActivity.class);
		realm.clear(RealmHuntlyUserStats.class);
		realm.clear(RealmHuntlyDeepLinks.class);
		realm.commitTransaction();

		huntlySettings.clear();
	}

	interface UserStatsListener {
		void onUserStatsAvailable(HuntlyUserStats stats);

		void onUserStatsFailure();
	}

	void updateUserStats(@Nullable UserStatsListener listener) {
		if (!isEventIdAvailable()) {
			return;
		}

		final Realm realm = realmProvider.getRealm();
		try {
			final Response<HuntlyUserStats> r = huntlyConnection
					.userStats(huntlySettings.eventId().get());
			if (r.isSuccessful()) {
				realm.beginTransaction();
				final RealmHuntlyUserStats stats = realm
						.copyToRealmOrUpdate(RealmHuntlyUserStats.fromApi(r.body()));
				realm.commitTransaction();

				context.sendBroadcast(new Intent(USER_DATA_UPDATED));

				if (listener != null) {
					listener.onUserStatsAvailable(HuntlyUserStats.fromDb(stats));
				}
			}
		} catch (IOException e) {
			if (listener != null) {
				listener.onUserStatsFailure();
			}
			Logger.exc(e);
		} finally {
			realm.close();
		}
	}

	Optional<HuntlyUserStats> currentUserStats() {
		final Realm realm = realmProvider.getRealm();
		final RealmHuntlyUserStats stats = realm.where(RealmHuntlyUserStats.class).findFirst();
		final HuntlyUserStats result = HuntlyUserStats.fromDb(stats);
		final Optional<HuntlyUserStats> res = Optional.ofNullable(result);
		realm.close();
		return res;
	}

	HuntlyDeepLinkConf currentDeepLinks(String activeConferenceID) {
		final HuntlyDeepLinkConf result;
		final Realm realm = realmProvider.getRealm();
		final RealmHuntlyDeepLinks links = realm.where(RealmHuntlyDeepLinks.class)
				.equalTo("confId", activeConferenceID).findFirst();
		if (links != null) {
			result = HuntlyDeepLinkConf.fromDb(links);
		} else {
			result = null;
		}
		return result;
	}

	public String token() {
		return huntlySettings.token().getOr("");
	}

	private long findRealmHuntlyEventId(String id) {
		final Realm realm = realmProvider.getRealm();
		final RealmHuntlyEvent event = realm.where(RealmHuntlyEvent.class).equalTo("externalId", id).findFirst();
		final long result = event != null ? event.getId() : UNKNOWN_EVENT_ID;
		realm.close();
		return result;
	}

	private List<HuntlyProfileProperty> createProperties(
			String finalCode,
			BaseExtractor infoExtractor) {
		final List<HuntlyProfileProperty> result = new ArrayList<>();
		appendProperty(infoExtractor.getUserId().first, finalCode, result);
		appendProperty(infoExtractor.getUserName(), result);
		appendProperty(infoExtractor.getUserSurname(), result);
		appendProperty(infoExtractor.getUserCompany(), result);
		appendProperty(infoExtractor.getUserJob(), result);
		return result;
	}

	private void appendProperty(String key, String val, List<HuntlyProfileProperty> list) {
		if (!TextUtils.isEmpty(val)) {
			list.add(HuntlyProfileProperty.create(key, val));
		}
	}

	private void appendProperty(Pair<String, String> pair, List<HuntlyProfileProperty> list) {
		if (!TextUtils.isEmpty(pair.second)) {
			list.add(HuntlyProfileProperty.create(pair.first, pair.second));
		}
	}

	@Nullable
	private HuntlyQuestActivity getQuest(@HuntlyQuestActivity.QuestActivity String activity) {
		final Realm realm = realmProvider.getRealm();
		final RealmHuntlyQuestActivity quest = realm.where(RealmHuntlyQuestActivity.class)
				.equalTo("activity", activity, Case.INSENSITIVE).findFirst();
		final HuntlyQuestActivity result = HuntlyQuestActivity.fromDb(quest);
		realm.close();
		return result;
	}

	private boolean isQuestAvailable(@HuntlyQuestActivity.QuestActivity String activity) {
		final Realm realm = realmProvider.getRealm();
		final long count = realm.where(RealmHuntlyQuestActivity.class).equalTo("activity", activity, Case.INSENSITIVE).count();
		realm.close();

		boolean result = false;
		if (count > 0) {
			final HuntlyQuestActivity quest = getQuest(activity);
			if (quest != null) {
				result = quest.getPerformedActivities() < quest.getMaxActivities();
			}
		}
		return result;
	}

	private void saveToken(String token) {
		huntlySettings.edit().token().put(token).apply();
	}

	private String obtainId() throws Exception {
		final AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
		if (adInfo == null) {
			throw new IllegalStateException("No ad id!");
		} else {
			return adInfo.getId();
		}
	}
}
