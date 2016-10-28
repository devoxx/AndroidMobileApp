package com.devoxx.push;

import com.devoxx.BuildConfig;
import com.devoxx.connection.cfp.model.ConferenceApiModel;
import com.devoxx.data.conference.ConferenceManager_;
import com.devoxx.data.downloader.SlotsDownloader;
import com.devoxx.data.manager.SlotsDataManager;
import com.devoxx.data.manager.SlotsDataManager_;
import com.google.android.gms.gcm.GcmListenerService;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class PushGcmListenerService extends GcmListenerService {

	@Override public void onMessageReceived(String from, Bundle data) {
		super.onMessageReceived(from, data);

		final Context context = getApplicationContext();
		final ConferenceManager_ conferenceManager = ConferenceManager_.getInstance_(context);
		final ConferenceApiModel conferenceApiModel = conferenceManager.lastSelectedConference();

		if (conferenceApiModel != null) {
			final SlotsDataManager slotsDataManager = SlotsDataManager_.getInstance_(context);
			slotsDataManager.forceUpdateSlotsAsync(context,
					new SlotsDownloader.DownloadRequest(conferenceApiModel));
		}

		if (BuildConfig.DEBUG) {
			new Handler(Looper.getMainLooper()).post
					(() -> Toast.makeText(getApplicationContext(),
							"Push received: " + data.toString(), Toast.LENGTH_LONG).show());
		}
	}
}
