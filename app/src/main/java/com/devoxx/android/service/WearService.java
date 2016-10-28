package com.devoxx.android.service;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.annimon.stream.Optional;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.devoxx.android.adapter.schedule.model.creator.ScheduleLineupDataCreator;
import com.devoxx.common.utils.Constants;
import com.devoxx.common.wear.GoogleApiConnector;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.connection.model.TalkSpeakerApiModel;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.conference.model.ConferenceDay;
import com.devoxx.data.manager.AbstractDataManager;
import com.devoxx.data.manager.NotificationsManager;
import com.devoxx.data.manager.SlotsDataManager;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.model.RealmSpeaker;
import com.devoxx.data.user.UserFavouritedTalksManager;
import com.devoxx.event.ScheduleEvent;
import com.devoxx.utils.Logger;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.WearableListenerService;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import pl.tajchert.buswear.EventBus;

@EService
public class WearService extends WearableListenerService {

	private final static String TAG = WearService.class.getCanonicalName();

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	SlotsDataManager slotsDataManager;

	@Bean
	ScheduleLineupDataCreator scheduleLineupDataCreator;

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	UserFavouritedTalksManager userFavouritedTalksManager;

	@Bean
	NotificationsManager notificationsManager;

	private GoogleApiConnector mGoogleApiConnector;

	@Override
	public void onCreate() {
		super.onCreate();
		mGoogleApiConnector = new GoogleApiConnector(this);
	}

	@Override
	public void onDestroy() {
		mGoogleApiConnector.disconnect();
		super.onDestroy();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		if (!conferenceManager.isConferenceChoosen()) {
			return;
		}

		final String path = messageEvent.getPath();
		final String data = new String(messageEvent.getData());

		if (path.startsWith(Constants.CHANNEL_ID + Constants.SCHEDULES_PATH)) {

			// send schedules to the Wearable
			sendSchedules();
		} else if (path.startsWith(Constants.CHANNEL_ID + Constants.SLOTS_PATH)) {

			// send slots to the Wearable
			try {
				sendSlots(Long.parseLong(data));

			} catch (Exception ex) {
				Log.e(TAG, ex.getLocalizedMessage());
			}
		} else if (path.startsWith(Constants.CHANNEL_ID + Constants.TALK_PATH)) {

			// send the talk to the Wearable
			sendTalk(data);
		} else if (path.startsWith(Constants.CHANNEL_ID + Constants.SPEAKER_PATH)) {

			// send the speaker to the Wearable
			sendSpeaker(data);
		} else if (path.startsWith(Constants.CHANNEL_ID + Constants.FAVORITE_PATH)) {

			// send the favorite's status of a talk
			sendFavorite(data);
		} else if (path.equalsIgnoreCase(Constants.CHANNEL_ID + Constants.ADD_FAVORITE_PATH)) {

			// Add the favorite's status to a talk
			addFavorite(data);
		} else if (path.equalsIgnoreCase(Constants.CHANNEL_ID + Constants.REMOVE_FAVORITE_PATH)) {

			// Remove the favorite's status of a talk
			removeFavorite(data);
		} else if (path.equalsIgnoreCase(Constants.CHANNEL_ID + Constants.TWITTER_PATH)) {

			// Twitter
			followOnTwitter(data);
		}

	}

	//
	// Twitter
	//

	// Open the Twitter application or the browser if the app is not installed
	private void followOnTwitter(String inputData) {

		String twitterName = inputData;

		if (TextUtils.isEmpty(twitterName) == false) {
			twitterName = twitterName.toLowerCase().replaceFirst("@", "");

			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + twitterName));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);
		}

	}


	private void sendSchedules() {

		// TODO: ensure that we have data

		final List<ConferenceDay> days = conferenceManager.getConferenceDays();

		final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.CHANNEL_ID + Constants.SCHEDULES_PATH);


		// set the header (timestamp is used to force a onDataChanged event on the wearable)
		final DataMap headerMap = new DataMap();
		headerMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
		putDataMapRequest.getDataMap().putDataMap(Constants.HEADER_PATH, headerMap);

		// Prepare and save the country code
		final DataMap countryMap = new DataMap();
		countryMap.putString(Constants.DATAMAP_COUNTRY, conferenceManager.getActiveConference().get().getCountry());
		putDataMapRequest.getDataMap().putDataMap(Constants.COUNTRY_PATH, countryMap);

		// Prepare and save the schedule
		ArrayList<DataMap> schedulesDataMap = new ArrayList<>();
		for (ConferenceDay day : days) {

			final DataMap scheduleDataMap = new DataMap();

			// process and push schedule's data
			String dayName = Uri.parse(day.getName()).getLastPathSegment();
			scheduleDataMap.putString(Constants.DATAMAP_DAY_NAME, dayName);
			scheduleDataMap.putLong(Constants.DATAMAP_DAY_MILLIS, day.getDayMs());

			schedulesDataMap.add(scheduleDataMap);
		}

		// store the list schedules
		putDataMapRequest.getDataMap().putDataMapArrayList(Constants.LIST_PATH, schedulesDataMap);

		// send the schedules
		mGoogleApiConnector.sendMessage(putDataMapRequest);
	}


	// Send the schedule's slots to the watch.
	private void sendSlots(Long dayMs) {

		// TODO: ensure that we have data

		List<SlotApiModel> slotApiModelList = null;

		// fetch for the slot
		final List<ConferenceDay> days = conferenceManager.getConferenceDays();
		for (ConferenceDay day : days) {
			if (day.getDayMs() == dayMs) {
				slotApiModelList = slotsDataManager.getSlotsForDay(day.getDayMs());
				break;
			}
		}

		if (slotApiModelList == null) {
			// not found
			return;
		}


		Collections.sort(slotApiModelList);


		final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.CHANNEL_ID + Constants.SLOTS_PATH + "/" + dayMs);

		// set the header (timestamp is used to force a onDataChanged event on the wearable)
		final DataMap headerMap = new DataMap();
		headerMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
		putDataMapRequest.getDataMap().putDataMap(Constants.HEADER_PATH, headerMap);

		ArrayList<DataMap> slotsDataMap = new ArrayList<>();

		for (int index = 0; index < slotApiModelList.size(); index++) {

			final DataMap scheduleDataMap = new DataMap();

			final SlotApiModel slot = slotApiModelList.get(index);

			// process the data
			scheduleDataMap.putString(Constants.DATAMAP_ROOM_NAME, slot.roomName);
			scheduleDataMap.putLong(Constants.DATAMAP_FROM_TIME_MILLIS, slot.fromTimeMs());
			scheduleDataMap.putLong(Constants.DATAMAP_TO_TIME_MILLIS, slot.toTimeMs());

			if (slot.isBreak()) {
				DataMap breakDataMap = new DataMap();

				//breakDataMap.putString("id", slot.getBreak().getId());
				breakDataMap.putString(Constants.DATAMAP_NAME_EN, slot.slotBreak.nameEN);
				breakDataMap.putString(Constants.DATAMAP_NAME_FR, slot.slotBreak.nameFR);

				scheduleDataMap.putDataMap(Constants.DATAMAP_BREAK, breakDataMap);
			}


			if (slot.isTalk()) {
				DataMap talkDataMap = new DataMap();

				talkDataMap.putString(Constants.DATAMAP_ID, slot.talk.id);
				talkDataMap.putBoolean(Constants.DATAMAP_FAVORITE, userFavouritedTalksManager.isFavouriteTalk(slot.slotId));
				talkDataMap.putString(Constants.DATAMAP_TRACK_ID, slot.talk.trackId);
				talkDataMap.putString(Constants.DATAMAP_TITLE, slot.talk.title);
				talkDataMap.putString(Constants.DATAMAP_LANG, slot.talk.lang);

				scheduleDataMap.putDataMap(Constants.DATAMAP_TALK, talkDataMap);
			}

			slotsDataMap.add(scheduleDataMap);
		}

		// store the list in the datamap to send it to the wear
		putDataMapRequest.getDataMap().putDataMapArrayList(Constants.LIST_PATH, slotsDataMap);

		// send the slots
		mGoogleApiConnector.sendMessage(putDataMapRequest);
	}


	private void sendTalk(String talkId) {


		SlotApiModel slotApiModel = getSlotByTalkId(talkId);
		if (slotApiModel == null) {
			return;
		}

		final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.CHANNEL_ID + Constants.TALK_PATH + "/" + talkId);

		// set the header (timestamp is used to force a onDataChanged event on the wearable)
		final DataMap headerMap = new DataMap();
		headerMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
		putDataMapRequest.getDataMap().putDataMap(Constants.HEADER_PATH, headerMap);

		final DataMap talkDataMap = new DataMap();

		// process the data
		talkDataMap.putString(Constants.DATAMAP_ID, talkId);
		talkDataMap.putBoolean(Constants.DATAMAP_FAVORITE, userFavouritedTalksManager.isFavouriteTalk(slotApiModel.slotId));
		talkDataMap.putString(Constants.DATAMAP_TALK_TYPE, slotApiModel.talk.talkType);
		talkDataMap.putString(Constants.DATAMAP_TRACK, slotApiModel.talk.track);
		talkDataMap.putString(Constants.DATAMAP_TRACK_ID, slotApiModel.talk.track);
		talkDataMap.putString(Constants.DATAMAP_TITLE, slotApiModel.talk.title);
		talkDataMap.putString(Constants.DATAMAP_LANG, slotApiModel.talk.lang);
		talkDataMap.putString(Constants.DATAMAP_SUMMARY, slotApiModel.talk.summary);

		ArrayList<DataMap> speakersDataMap = new ArrayList<>();

		// process each speaker's data
		if (slotApiModel.talk.speakers != null) {


			for (int index = 0; index < slotApiModel.talk.speakers.size(); index++) {

				final TalkSpeakerApiModel speaker = slotApiModel.talk.speakers.get(index);

				final DataMap speakerDataMap = new DataMap();

				String uuid = Uri.parse(speaker.link.href).getLastPathSegment();
				speakerDataMap.putString(Constants.DATAMAP_UUID, uuid);
				speakerDataMap.putString(Constants.DATAMAP_NAME, speaker.getName());

				speakersDataMap.add(speakerDataMap);
			}
		}

		if (speakersDataMap.size() > 0) {
			talkDataMap.putDataMapArrayList(Constants.SPEAKERS_PATH, speakersDataMap);
		}

		// store the list in the datamap to send it to the wear
		putDataMapRequest.getDataMap().putDataMap(Constants.DETAIL_PATH, talkDataMap);

		// send the talk
		//sendToWearable(putDataMapRequest);

		mGoogleApiConnector.sendMessage(putDataMapRequest);
	}

	// Send the speaker's detail to the watch.
	private void sendSpeaker(final String uuid) {


		final String id = conferenceManager.getActiveConferenceId().get();
		speakersDataManager.fetchSpeakerAsync(id, uuid,
				new AbstractDataManager.IDataManagerListener<RealmSpeaker>() {
					@Override
					public void onDataStartFetching() {

					}

					@Override
					public void onDataAvailable(List<RealmSpeaker> items) {
						Logger.l("Should not be there");
					}

					@Override
					public void onDataAvailable(RealmSpeaker item) {
						final RealmSpeaker speaker = speakersDataManager.getByUuid(uuid);

						final String dataPath = Constants.CHANNEL_ID + Constants.SPEAKER_PATH + "/" + uuid;

						PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(dataPath);

						// set the header (timestamp is used to force a onDataChanged event on the wearable)
						final DataMap headerMap = new DataMap();
						headerMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
						putDataMapRequest.getDataMap().putDataMap(Constants.HEADER_PATH, headerMap);

						final DataMap speakerDataMap = new DataMap();

						// process the data
						speakerDataMap.putString(Constants.DATAMAP_UUID, uuid);
						speakerDataMap.putString(Constants.DATAMAP_FIRST_NAME, speaker.getFirstName());
						speakerDataMap.putString(Constants.DATAMAP_LAST_NAME, speaker.getLastName());
						speakerDataMap.putString(Constants.DATAMAP_COMPANY, speaker.getCompany());
						speakerDataMap.putString(Constants.DATAMAP_BIO, speaker.getBio());
						speakerDataMap.putString(Constants.DATAMAP_BLOG, speaker.getBlog());
						speakerDataMap.putString(Constants.DATAMAP_TWITTER, speaker.getTwitter());
						speakerDataMap.putString(Constants.DATAMAP_AVATAR_URL, speaker.getAvatarURL());

						// store the list in the datamap to send it to the wear
						putDataMapRequest.getDataMap().putDataMap(Constants.DETAIL_PATH, speakerDataMap);

						// send the speaker
						mGoogleApiConnector.sendMessage(putDataMapRequest);

						if ((speaker.getAvatarURL() == null) || (speaker.getAvatarURL().isEmpty())) {
							return;
						}

						final ImageTarget imageTarget = new ImageTarget(dataPath, speakerDataMap);

						new Handler(Looper.getMainLooper()).post(new Runnable() {
							@Override
							public void run() {
								Glide.with(WearService.this)
										.load(speaker.getAvatarURL())
										.asBitmap()
										.centerCrop()
										.override(100, 100)
										.into(imageTarget);
							}
						});

					}

					@Override
					public void onDataError(IOException e) {
						if (e instanceof UnknownHostException) {
							Logger.l("Connection error");
						} else {
							Logger.l("Something went wrong");
						}
					}
				});
	}


	public class ImageTarget implements Target {

		private String mDataPath;
		private DataMap mSpeakerDataMap;


		public ImageTarget(String dataPath, DataMap speakerDataMap) {
			mDataPath = dataPath;
			mSpeakerDataMap = speakerDataMap;
		}

		@Override
		public void onLoadStarted(Drawable placeholder) {

		}

		@Override
		public void onLoadFailed(Exception e, Drawable errorDrawable) {

		}

		@Override
		public void onResourceReady(Object resource, GlideAnimation glideAnimation) {

			// reset the existing speaker's data item
			mGoogleApiConnector.deleteItems(mDataPath);

			Bitmap bitmap = (Bitmap) resource;

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
			byte[] byteArray = byteArrayOutputStream.toByteArray();

			String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

			// update the data map with the avatar
			mSpeakerDataMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
			mSpeakerDataMap.putString(Constants.DATAMAP_AVATAR_IMAGE, encoded);

			// as the datamap has changed, a onDataChanged event will be fired on the remote node

			// store the list in the datamap to send it to the wear
			final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(mDataPath);
			putDataMapRequest.getDataMap().putDataMap(Constants.DETAIL_PATH, mSpeakerDataMap);

			// send the speaker with its profile image
			mGoogleApiConnector.sendMessage(putDataMapRequest);

		}

		@Override
		public void onLoadCleared(Drawable placeholder) {

		}

		@Override
		public void getSize(SizeReadyCallback cb) {

		}

		@Override
		public void setRequest(Request request) {

		}

		@Override
		public Request getRequest() {
			return null;
		}

		@Override
		public void onStart() {

		}

		@Override
		public void onStop() {

		}

		@Override
		public void onDestroy() {

		}
	}


	// send Favorite to the watch
	private void sendFavorite(String talkId) {

		SlotApiModel slotApiModel = getSlotByTalkId(talkId);
		if (slotApiModel == null) {
			return;
		}

		// send the event
		PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.CHANNEL_ID + Constants.FAVORITE_PATH + "/" + talkId);

		// set the header (timestamp is used to force a onDataChanged event on the wearable)
		final DataMap headerMap = new DataMap();
		headerMap.putString(Constants.DATAMAP_TIMESTAMP, new Date().toString());
		putDataMapRequest.getDataMap().putDataMap(Constants.HEADER_PATH, headerMap);

		// store the data
		DataMap dataMap = new DataMap();
		dataMap.putBoolean(Constants.DATAMAP_FAVORITE, userFavouritedTalksManager.isFavouriteTalk(slotApiModel.slotId));

		// store the event in the datamap to send it to the wear
		putDataMapRequest.getDataMap().putDataMap(Constants.DETAIL_PATH, dataMap);

		// send the favorite's status to the watch
		mGoogleApiConnector.sendMessage(putDataMapRequest);
	}

	// remove the favorite from the talk
	private void removeFavorite(String talkId) {

		SlotApiModel slotApiModel = getSlotByTalkId(talkId);
		if (slotApiModel == null) {
			return;
		}

		notificationsManager.removeNotification(slotApiModel.slotId);
		userFavouritedTalksManager.unFavouriteTalk(slotApiModel.slotId);

		EventBus.getDefault().postLocal(new ScheduleEvent());

		sendFavorite(talkId);
	}

	// add the favorite to the talk
	private void addFavorite(String talkId) {

		SlotApiModel slotApiModel = getSlotByTalkId(talkId);
		if (slotApiModel == null) {
			return;
		}

		notificationsManager.scheduleNotification(slotApiModel, false);
		userFavouritedTalksManager.favouriteTalk(slotApiModel.slotId);

		EventBus.getDefault().postLocal(new ScheduleEvent());

		sendFavorite(talkId);
	}


	private SlotApiModel getSlotByTalkId(String talkId) {

		final String confId = conferenceManager.getActiveConferenceId().get();
		try {
			speakersDataManager.fetchSpeakersSync(confId);
		} catch (IOException e) {
			return null;
		}

		if (slotsDataManager == null) {
			return null;
		}

		final Optional<SlotApiModel> opt = slotsDataManager.getSlotByTalkId(talkId);
		if (!opt.isPresent()) {
			return null;
		}
		SlotApiModel slotApiModel = opt.get();

		if (!slotApiModel.isTalk()) {
			// not a talk
			return null;
		}

		return slotApiModel;
	}

}
