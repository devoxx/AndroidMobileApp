package com.devoxx.data.manager;

import com.devoxx.R;
import com.devoxx.android.activity.MainActivity_;
import com.devoxx.android.fragment.schedule.ScheduleLineupFragment;
import com.devoxx.android.receiver.AlarmReceiver_;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.model.RealmNotification;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.SystemService;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmResults;

@EBean(scope = EBean.Scope.Singleton)
public class NotificationsManager {

	public static final String EXTRA_TALK_ID = "com.devoxx.android.intent.extra.TALK_ID";
	public static final String NOTIFICATION_TALK_TYPE = "com.devoxx.android.intent.NOTIFICATION_TALK_TYPE";
	public static final String NOTIFICATION_POST_TYPE = "com.devoxx.android.intent.NOTIFICATION_POST_TYPE";

	private static final boolean FAKE_TIME = false;

	private static final long DEBUG_BEFORE_TALK_NOTIFICATION_SPAN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long DEBUG_POST_TALK_NOTIFICATION_DELAY_MS = TimeUnit.SECONDS.toMillis(10);

	private static final long PROD_BEFORE_TALK_NOTIFICATION_SPAN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long PROD_POST_TALK_NOTIFICATION_DELAY_MS = TimeUnit.MINUTES.toMillis(15);

	@RootContext
	Context context;

	@Bean
	RealmProvider realmProvider;

	@SystemService
	AlarmManager alarmManager;

	@SystemService
	NotificationManager notificationManager;

	@SystemService
	PowerManager powerManager;

	@AfterInject void afterInject() {
		warmUpScheduledNotificationsCache();
	}

	public void scheduleNotification(SlotApiModel slotApiModel, boolean withToast) {
		final NotificationConfiguration cfg = NotificationConfiguration.create(slotApiModel, withToast);
		scheduleNotificationFromConfiguration(cfg);
	}

	private void scheduleNotificationFromConfiguration(NotificationConfiguration cfg) {
		if (cfg.isInvalid()) {
			return;
		}

		// We can't do notification for past events...
		if (!cfg.canScheduleNotification()) {
			Toast.makeText(context,
					context.getString(R.string.toast_notification_not_set),
					Toast.LENGTH_SHORT).show();
			return;
		}


		storeConfiguration(cfg);

		scheduleTalkNotificationAlarm(cfg);
		schedulePostNotificationAlarm(cfg);

		addInfoToCache(cfg);

		if (cfg.isWithToast()) {

			final DateTime talkNotificationTime = new DateTime(cfg.getTalkNotificationTime());

			String pattern;

			final boolean isFuture = talkNotificationTime.isAfter(DateTime.now());
			if (isFuture) {
				pattern = "dd/MM/YY HH:mm";
			} else {
				pattern = "HH:mm";
			}

			final DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
			final String time = talkNotificationTime.toString(dtf);

			Toast.makeText(context, context.getString(R.string.toast_notification_set_at) +
					" " + time, Toast.LENGTH_SHORT).show();
		}
	}

	private void schedulePostNotificationAlarm(NotificationConfiguration cfg) {
		final PendingIntent pendingIntent = createPostNotificationPendingIntent(cfg);
		final long notificationTime = cfg.getPostTalkNotificationTime();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
		} else {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
		}
	}

	private void scheduleTalkNotificationAlarm(NotificationConfiguration cfg) {
		final PendingIntent pendingIntent = createTalkNotificationPendingIntent(cfg);
		final long notificationTime = cfg.getTalkNotificationTime();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
		} else {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
		}
	}

	private PendingIntent createPostNotificationPendingIntent(NotificationConfiguration cfg) {
		final Intent intent = new Intent(context, AlarmReceiver_.class);
		final String slotID = cfg.getSlotId();
		intent.setAction(NOTIFICATION_POST_TYPE);
		intent.putExtra(EXTRA_TALK_ID, slotID);
		return PendingIntent.getBroadcast(context, slotID.hashCode(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private PendingIntent createTalkNotificationPendingIntent(NotificationConfiguration cfg) {
		final Intent intent = new Intent(context, AlarmReceiver_.class);
		intent.setAction(NOTIFICATION_TALK_TYPE);
		final String slotID = cfg.getSlotId();
		intent.putExtra(EXTRA_TALK_ID, slotID);
		return PendingIntent.getBroadcast(context, slotID.hashCode(), intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	private PendingIntent createTalkPendingIntentToOpenMainActivity(String slotID) {
		final Intent intent = new Intent(context, MainActivity_.class);
		intent.putExtra(EXTRA_TALK_ID, slotID);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		return PendingIntent.getActivity(context, slotID.hashCode(),
				intent, PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public void removeNotification(String slotid) {
		unscheduleNotification(slotid, true);
	}

	private void unscheduleNotification(String slotId, boolean finishNotification) {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		if (finishNotification) {
			cancelPostTalkNotificationOnAlarmManager(slotId);
			cancelTalkNotificationOnAlarmManager(slotId);
			flagNotificationAsComplete(realm, slotId);
		} else {
			cancelTalkNotificationOnAlarmManager(slotId);
			flagNotificationAsFiredForTalk(realm, slotId);
		}
		realm.commitTransaction();
	}

	private void cancelPostTalkNotificationOnAlarmManager(String slotId) {
		final NotificationConfiguration cfg = getConfiguration(slotId);
		if (cfg == null) {
			// talk schedule is already cancelled
			return;
		}
		final PendingIntent toBeCancelled = createPostNotificationPendingIntent(cfg);
		toBeCancelled.cancel();
		alarmManager.cancel(toBeCancelled);
	}

	private void cancelTalkNotificationOnAlarmManager(String slotId) {
		final NotificationConfiguration cfg = getConfiguration(slotId);
		if (cfg == null) {
			// talk schedule already cancelled
			return;
		}
		final PendingIntent toBeCancelled = createTalkNotificationPendingIntent(cfg);
		toBeCancelled.cancel();
		alarmManager.cancel(toBeCancelled);
	}

	private void flagNotificationAsFiredForTalk(Realm realm, String slotId) {
		final RealmNotification notification = realm.where(RealmNotification.class)
				.equalTo(RealmNotification.Contract.SLOT_ID, slotId).findFirst();
		notification.setFiredForTalk(true);
		markAsFiredForTalkInCache(slotId);
	}

	private void flagNotificationAsComplete(Realm realm, String slotId) {
		realm.where(RealmNotification.class).equalTo(RealmNotification.Contract.SLOT_ID,
				slotId).findAll().clear();
		removeInfoFromCache(slotId);
	}

	public void showNotificationForVote(String slotId, String title, String desc) {
		final Realm realm = realmProvider.getRealm();
		final RealmNotification realmNotification = realm
				.where(RealmNotification.class)
				.equalTo(RealmNotification.Contract.SLOT_ID, slotId).findFirst();

		final Notification notification = createPostNotification(
				title, desc, realmNotification, createTalkPendingIntentToOpenMainActivity(slotId));
		notificationManager.notify(slotId.hashCode(), notification);

		unscheduleNotification(slotId, true);
	}

	public void showNotificationForTalk(String slotId) {
		final Realm realm = realmProvider.getRealm();
		final RealmNotification realmNotification = realm.where(RealmNotification.class)
				.equalTo(RealmNotification.Contract.SLOT_ID, slotId).findFirst();

		if (isNotificationBeforeEvent(realmNotification)) {
			final Notification notification = createTalkNotification(
					realmNotification, createTalkPendingIntentToOpenMainActivity(slotId));
			notificationManager.notify(slotId.hashCode(), notification);

			notifyListenerAboutTalkNotification();
		}

		unscheduleNotification(slotId, false);
	}

	private void notifyListenerAboutTalkNotification() {
		context.sendBroadcast(ScheduleLineupFragment.getRefreshIntent());
	}

	public boolean isNotificationAvailable(String slotId) {
		return scheduledNotifications.containsKey(slotId);
	}

	/* key: SLOT_ID, value: IS_FIRED_FOR_TALK */
	private HashMap<String, Boolean> scheduledNotifications = new HashMap<>();

	private void warmUpScheduledNotificationsCache() {
		final Realm realm = realmProvider.getRealm();
		final RealmResults<RealmNotification> rr = realm.where(RealmNotification.class).findAll();
		for (RealmNotification rn : rr) {
			scheduledNotifications.put(rn.getSlotId(), rn.isFiredForTalk());
		}
	}

	public boolean isNotificationScheduled(String slotId) {
		return scheduledNotifications.containsKey(slotId) && !scheduledNotifications.get(slotId);
	}

	private void addInfoToCache(NotificationConfiguration cfg) {
		scheduledNotifications.put(cfg.getSlotId(), false);
	}

	private void removeInfoFromCache(String slotId) {
		scheduledNotifications.remove(slotId);
	}

	private void markAsFiredForTalkInCache(String slotId) {
		scheduledNotifications.put(slotId, true);
	}

	private Notification createPostNotification(
			String title, String desc,
			RealmNotification realmNotification, PendingIntent contentIntent) {
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
		notificationBuilder.setContentTitle(title);
		notificationBuilder.setContentText(desc);
		notificationBuilder.setContentIntent(contentIntent);
		notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
		notificationBuilder.setWhen(realmNotification.getTalkTime());
		notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
		notificationBuilder.setTicker(desc);

		Notification notification = notificationBuilder.build();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		return notification;
	}

	@NonNull
	private Notification createTalkNotification(RealmNotification realmNotification, PendingIntent contentIntent) {
		final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
		notificationBuilder.setContentTitle(realmNotification.getRoomName());
		notificationBuilder.setContentText(realmNotification.getTalkTitle());
		notificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
				.bigText(realmNotification.getTalkTitle()));
		notificationBuilder.setContentIntent(contentIntent);
		notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
		notificationBuilder.setWhen(realmNotification.getTalkTime());
		notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
		notificationBuilder.setTicker(realmNotification.getTalkTitle());

		final Notification notification = notificationBuilder.build();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.defaults |= Notification.DEFAULT_LIGHTS;
		return notification;
	}

	private boolean isNotificationBeforeEvent(RealmNotification realmNotification) {
		return FAKE_TIME || realmNotification.getTalkTime()
				> getNowMillis() - 600000;
	}

	public List<RealmNotification> getAlarms() {
		final Realm realm = realmProvider.getRealm();
		return realm.where(RealmNotification.class).findAll();
	}

	@SuppressLint("Wakelock")
	public void resetAlarms() {
		Log.i(NotificationsManager.class.getSimpleName(), "resetAlarms");

		final PowerManager.WakeLock wakeLock = powerManager.
				newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmService");
		wakeLock.acquire();

		final List<RealmNotification> notificationsList = getAlarms();
		final int size = notificationsList.size();
		for (int i = 0; i < size; i++) {
			final RealmNotification model = notificationsList.get(i);
			final NotificationConfiguration cfg = NotificationConfiguration.create(model);
			cancelPostTalkNotificationOnAlarmManager(cfg.getSlotId());
			cancelTalkNotificationOnAlarmManager(cfg.getSlotId());
			scheduleNotificationFromConfiguration(cfg);
		}

		wakeLock.release();
	}

	private NotificationConfiguration getConfiguration(String slotID) {
		final Realm realm = realmProvider.getRealm();
		final RealmNotification rn = realm.where(RealmNotification.class).equalTo("slotId", slotID).findFirst();
		realm.close();
		if (rn == null) {
			// talk schedule is already cancelled
			return null;
		}
		return NotificationConfiguration.create(rn);
	}

	private void storeConfiguration(NotificationConfiguration notifyModel) {
		final Realm realm = realmProvider.getRealm();
		realm.beginTransaction();
		final RealmNotification model = new RealmNotification();
		model.setTalkNotificationTime(notifyModel.getTalkNotificationTime());
		model.setTalkTime(notifyModel.getTalkStartTime());
		model.setPostNotificationTime(notifyModel.getPostTalkNotificationTime());
		model.setSlotId(notifyModel.getSlotId());
		model.setRoomName(notifyModel.getRoomName());
		model.setTalkTitle(notifyModel.getTalkTitle());
		model.setTalkEndTime(notifyModel.getEndTime());
		realm.copyToRealmOrUpdate(model);
		realm.commitTransaction();
		realm.close();
	}

	private static long getNowMillis() {
		return DateTime.now().getMillis();
	}

	static class NotificationConfiguration {
		private final String talkSlotId;
		private final String talkTitle;
		private final String talkRoom;

		private final long talkStartTime;
		private final long talkEndTime;
		private final boolean withToast;

		// Notification for talk.
		private final long talkNotificationTime;

		// Notification for post-talk (eg. voting).
		private final long postTalkNotificationTime;

		public static NotificationConfiguration create(SlotApiModel slotApiModel, boolean withToast) {
			return new NotificationConfiguration(slotApiModel, withToast);
		}

		NotificationConfiguration(
				String talkSlotId, String talkTitle, String talkRoom,
				long talkStartTime, long talkEndTime, boolean withToast, long talkNotificationTime,
				long postTalkNotificationTime) {
			this.talkSlotId = talkSlotId;
			this.talkTitle = talkTitle;
			this.talkRoom = talkRoom;
			this.talkStartTime = talkStartTime;
			this.talkEndTime = talkEndTime;
			this.withToast = withToast;
			this.talkNotificationTime = talkNotificationTime;
			this.postTalkNotificationTime = postTalkNotificationTime;
		}

		public static NotificationConfiguration create(RealmNotification rn) {
			return new NotificationConfiguration(
					rn.getSlotId(),
					rn.getTalkTitle(),
					rn.getRoomName(),
					rn.getTalkTime(),
					rn.getTalkEndTime(),
					rn.isWithToast(),
					rn.getTalkNotificationTime(),
					rn.getPostNotificationTime()
			);
		}

		NotificationConfiguration(SlotApiModel slotApiModel, boolean toastInfo) {
			talkSlotId = slotApiModel.slotId;
			talkTitle = slotApiModel.talk.title;
			talkRoom = slotApiModel.roomName;
			talkStartTime = slotApiModel.fromTimeMs();
			talkEndTime = slotApiModel.toTimeMs();
			withToast = toastInfo;

			final long beforeTalkNotificationTime = FAKE_TIME
					? DEBUG_BEFORE_TALK_NOTIFICATION_SPAN_MS
					: PROD_BEFORE_TALK_NOTIFICATION_SPAN_MS;

			// In debug we set talk notification in future for tests...
			talkNotificationTime = FAKE_TIME
					? getNowMillis() + beforeTalkNotificationTime
					: talkStartTime - beforeTalkNotificationTime;

			// In debug we set post-talk notification in small amout on time for tests...
			postTalkNotificationTime = talkEndTime +
					(FAKE_TIME
							? DEBUG_POST_TALK_NOTIFICATION_DELAY_MS
							: PROD_POST_TALK_NOTIFICATION_DELAY_MS);
		}

		public long getTalkNotificationTime() {
			return talkNotificationTime < getNowMillis() ? talkStartTime : talkNotificationTime;
		}

		public long getPostTalkNotificationTime() {
			return postTalkNotificationTime;
		}

		public long getTalkStartTime() {
			return talkStartTime;
		}

		public boolean canScheduleNotification() {
			return FAKE_TIME || /*Debug allow all*/
					talkNotificationTime > getNowMillis() || /*Set normal reminder, we have a time to set reminder ex 1h before talk start.*/
					talkStartTime > getNowMillis() /*Set notification for talk start time, because we are less then ex 1h before talk start.*/
					;
		}

		public boolean isWithToast() {
			return withToast;
		}

		public String getSlotId() {
			return talkSlotId;
		}

		public String getTalkTitle() {
			return talkTitle;
		}

		public String getRoomName() {
			return talkRoom;
		}

		public long getEndTime() {
			return talkEndTime;
		}

		public boolean isInvalid() {
			return talkNotificationTime == 0 || postTalkNotificationTime == 0 ||
					talkEndTime == 0 || talkStartTime == 0;
		}
	}
}
