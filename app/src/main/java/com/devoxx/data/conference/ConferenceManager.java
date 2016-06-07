package com.devoxx.data.conference;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.crashlytics.android.Crashlytics;
import com.devoxx.connection.cfp.model.ConferenceApiModel;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.Settings_;
import com.devoxx.data.cache.BaseCache;
import com.devoxx.data.conference.model.ConferenceDay;
import com.devoxx.data.downloader.ConferenceDownloader;
import com.devoxx.data.downloader.TracksDownloader;
import com.devoxx.data.manager.SlotsDataManager;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.model.RealmConference;
import com.devoxx.data.schedule.filter.ScheduleFilterManager;
import com.devoxx.data.user.UserManager;
import com.devoxx.integrations.IntegrationController;
import com.devoxx.integrations.IntegrationProvider;
import com.devoxx.integrations.huntly.HuntlyController;
import com.google.gson.Gson;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.realm.Realm;

@EBean(scope = EBean.Scope.Singleton)
public class ConferenceManager {

	private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	public interface IConferencesListener {

		void onConferencesDataStart();

		void onConferencesAvailable(List<ConferenceApiModel> conferenceS);

		void onConferencesError();

	}

	public interface IConferenceDataListener {

		void onConferenceDataStart();

		void onConferenceDataAvailable(boolean isAnyTalks);

		void onConferenceDataError();

	}

	@RootContext
	Context context;

	@Bean
	IntegrationProvider integrationProvider;

	@Bean
	ConferenceDownloader conferenceDownloader;

	@Bean
	ScheduleFilterManager scheduleFilterManager;

	@Bean
	SlotsDataManager slotsDataManager;

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	TracksDownloader tracksDownloader;

	@Bean
	RealmProvider realmProvider;

	@Bean
	BaseCache baseCache;

	@Bean
	UserManager userManager;

	@Bean
	HuntlyController huntlyController;

	@Pref
	Settings_ settings;

	private List<ConferenceDay> conferenceDays;

	public void createSpeakersRepository() {
		speakersDataManager.createSpeakersRepository();
	}

	@Background
	public void fetchAvailableConferences() {
		try {
			notifyConferencesListenerAboutStart(allConferencesDataListener);
			final List<ConferenceApiModel> conferences = conferenceDownloader.fetchAllConferences();
			notifyConferencesListenerSuccess(allConferencesDataListener, conferences);
		} catch (IOException e) {
			notifyConferencesListenerError(allConferencesDataListener);
		}
	}

	@UiThread void notifyConferencesListenerAboutStart(WeakReference<IConferencesListener> listener) {
		isDownloadingAllConferencesData = true;
		final IConferencesListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferencesDataStart();
		}
	}

	@UiThread void notifyConferencesListenerSuccess(
			WeakReference<IConferencesListener> listener, List<ConferenceApiModel> list) {
		isDownloadingAllConferencesData = false;
		final IConferencesListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferencesAvailable(list);
		}
	}

	@UiThread void notifyConferencesListenerError(
			WeakReference<IConferencesListener> listener) {
		isDownloadingAllConferencesData = false;
		final IConferencesListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferencesError();
		}
	}

	public void openLastConference() {
		notifyConferenceListenerStart(confDataListener);
		notifyConferenceListenerSuccess(confDataListener, true);
	}

	public boolean isLastSelectedConference(ConferenceApiModel selectedConference) {
		final Optional<String> id = getActiveConferenceId();
		return id.isPresent() && id.get().equalsIgnoreCase(selectedConference.id);
	}

	public void requestConferenceChange() {
		settings.edit().requestedConferenceChange().put(true).apply();
	}

	public boolean requestedChangeConference() {
		final boolean result = settings.requestedConferenceChange().get();
		settings.edit().requestedConferenceChange().put(false).apply();
		return result;
	}

	public boolean isConferenceChoosen() {
		return getActiveConference().isPresent();
	}

	public void setupDefaultTimeZone() {
		final Optional<RealmConference> optional = getActiveConference();
		if (optional.isPresent()) {
			final RealmConference conference = optional.get();
			final String timeZoneId;
			switch (conference.getId()) {
				default:
				case "DevoxxFR2016":
					timeZoneId = "Europe/Paris";
					break;
				case "DevoxxUK2016":
					timeZoneId = "Europe/London";
					break;
				case "DevoxxPL2015":
				case "DevoxxPL2016":
					timeZoneId = "Europe/Warsaw";
					break;
				case "DV15":
					timeZoneId = "Europe/Brussels";
					break;
				case "DevoxxMA2015":
				case "DevoxxMA2016":
					timeZoneId = "Europe/Morocco";
					break;
			}

			DateTimeZone.setDefault(DateTimeZone.forID(timeZoneId));
		}
	}

	private boolean isDownloadingAllConferencesData = false;

	private WeakReference<IConferencesListener> allConferencesDataListener;

	public boolean registerAllConferencesDataListener(IConferencesListener listener) {
		allConferencesDataListener = new WeakReference<>(listener);
		return isDownloadingAllConferencesData;
	}

	public void unregisterAllConferencesDataListener() {
		allConferencesDataListener.clear();
	}

	private boolean isDownloadingConferenceData = false;

	private WeakReference<IConferenceDataListener> confDataListener;

	public boolean registerConferenceDataListener(IConferenceDataListener listener) {
		confDataListener = new WeakReference<>(listener);
		return isDownloadingConferenceData;
	}

	public void unregisterConferenceDataListener() {
		confDataListener.clear();
	}

	public ConferenceApiModel lastSelectedConference() {
		final String rawData = settings.lastSelectedConference().getOr("");
		return !TextUtils.isEmpty(rawData) ? new Gson().fromJson(rawData, ConferenceApiModel.class) : null;
	}

	public void initWitStaticData() {
		conferenceDownloader.initWitStaticData();
	}

	public void updateSlotsIfNeededAsync() {
		final Optional<RealmConference> conference = getActiveConference();
		if (conference.isPresent()) {
			final String confCode = conference.get().getId();
			slotsDataManager.updateSlotsAsync(confCode);
		}
	}

	public void forceUpdateScheduleData() {
		final Optional<RealmConference> conference = getActiveConference();
		if (conference.isPresent()) {
			final String confCode = conference.get().getId();
			slotsDataManager.forceUpdateSlotsAsync(confCode);
		}
	}

	@Background
	public void fetchConferenceData(
			ConferenceApiModel conferenceApiModel) {
		saveLastSelectedConference(conferenceApiModel);

		final String confCode = conferenceApiModel.id;
		try {
			notifyConferenceListenerStart(confDataListener);

			tracksDownloader.downloadTracksDescriptions(confCode);
			final boolean isAnyTalks = slotsDataManager.fetchTalksSync(confCode);
			speakersDataManager.fetchSpeakersSync(confCode);
			createSpeakersRepository();

			saveActiveConference(conferenceApiModel);
			setupDefaultTimeZone();

			final List<ConferenceDay> conferenceDays = getConferenceDays();
			scheduleFilterManager.createDayFiltersDefinition(conferenceDays);

			final IntegrationController integrationController = integrationProvider.provideIntegrationController();
			integrationController.register();
			integrationController.downloadNeededData(conferenceApiModel.integration_id);

			notifyConferenceListenerSuccess(confDataListener, isAnyTalks);
		} catch (IOException e) {
			clearCurrentConferenceData();
			notifyConferenceListenerError(confDataListener);
		}
	}

	private void saveLastSelectedConference(ConferenceApiModel conferenceApiModel) {
		settings.edit().lastSelectedConference().put(new Gson().toJson(conferenceApiModel)).apply();
	}

	@UiThread void notifyConferenceListenerStart(WeakReference<IConferenceDataListener> listener) {
		isDownloadingConferenceData = true;
		final IConferenceDataListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferenceDataStart();
		}
	}

	@UiThread
	void notifyConferenceListenerSuccess(WeakReference<IConferenceDataListener> listener, boolean isAnyTalks) {
		isDownloadingConferenceData = false;
		final IConferenceDataListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferenceDataAvailable(isAnyTalks);
		}
	}

	@UiThread void notifyConferenceListenerError(WeakReference<IConferenceDataListener> listener) {
		isDownloadingConferenceData = false;
		final IConferenceDataListener internalListener = listener.get();
		if (internalListener != null) {
			internalListener.onConferenceDataError();
		}
	}

	public List<ConferenceDay> getConferenceDays() {
		final RealmConference realmConference = getActiveConference().get();
		final String fromDate = realmConference.getFromDate();
		final String toDate = realmConference.getToDate();
		final DateTime fromConfDate = parseConfDate(fromDate);
		final DateTime toConfDate = parseConfDate(toDate);
		final DateTime now = new DateTime(getNow());

		final int daysSpan = Days.daysBetween(fromConfDate, toConfDate).getDays();

		final List<ConferenceDay> result = new ArrayList<>(daysSpan + 1 /* include days */);
		for (int i = 0; i <= daysSpan; i++) {
			final DateTime tmpDate = fromConfDate.plusDays(i);
			final boolean isToday = tmpDate.getDayOfYear() == now.getDayOfYear();
			result.add(new ConferenceDay(
					tmpDate.getMillis(),
					tmpDate.dayOfWeek().getAsText(Locale.getDefault()),
					isToday));
		}

		conferenceDays = new ArrayList<>(result);

		return result;
	}

	@Background
	public void updateActiveConferenceFromCfp() {
		try {
			final List<ConferenceApiModel> confs = conferenceDownloader.fetchAllConferences();
			final Optional<RealmConference> optional = getActiveConference();
			if (confs != null && optional.isPresent()) {
				final RealmConference activeConf = optional.get();

				for (ConferenceApiModel conf : confs) {
					if (conf.id.equals(activeConf.getId())) {

						final Realm realm = realmProvider.getRealm();
						realm.beginTransaction();
						realm.copyToRealmOrUpdate(new RealmConference(conf));
						realm.commitTransaction();
						realm.close();
					}
				}
			}
		} catch (IOException e) {
			Crashlytics.logException(e);
		}
	}

	public Optional<RealmConference> getActiveConference() {
		final Realm realm = realmProvider.getRealm();
		return Optional.ofNullable(realm.where(RealmConference.class).findFirst());
	}

	public Optional<String> getActiveConferenceId() {
		final RealmConference conference = getActiveConference().orElse(null);
		return Optional.ofNullable(conference != null ? conference.getId() : null);
	}

	public void clearCurrentConferenceData() {
		clearCurrentConference();
		clearSlotsData();
		clearTracksData();
		clearSpeakersData();
		clearFiltersDefinitions();
		clearCache();

		integrationProvider.provideIntegrationController().clear();
		userManager.clearCode();
	}

	public Optional<ConferenceDay> getCurrentConfDay() {
		return Stream.of(conferenceDays).filter(ConferenceDay::isRunning).findFirst();
	}

	public static long getNow() {
		return DateTime.now().getMillis();
	}

	private void clearFiltersDefinitions() {
		scheduleFilterManager.removeAllFilters();
	}

	private void clearSpeakersData() {
		speakersDataManager.clearData();
	}

	private void clearTracksData() {
		tracksDownloader.clearTracksData();
	}

	private void clearCache() {
		baseCache.clearAllCache();
	}

	private void clearSlotsData() {
		slotsDataManager.clearData();
	}

	private void clearCurrentConference() {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		realm.where(RealmConference.class).findAll().clear();
		realm.commitTransaction();
		realm.close();
	}

	private void saveActiveConference(ConferenceApiModel conferenceApiModel) {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		realm.allObjects(RealmConference.class).clear();
		realm.copyToRealmOrUpdate(new RealmConference(conferenceApiModel));
		realm.commitTransaction();
		realm.close();
	}

	public static DateTime parseConfDate(String stringDate) {
		DateTimeFormatter formatter = DateTimeFormat.forPattern(DATE_FORMAT);
		return formatter.parseDateTime(stringDate);
	}
}
