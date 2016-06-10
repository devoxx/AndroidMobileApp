package com.devoxx.android.activity;

import com.annimon.stream.Optional;
import com.bumptech.glide.Glide;
import com.devoxx.R;
import com.devoxx.android.view.selector.SelectorValues;
import com.devoxx.android.view.selector.SelectorView;
import com.devoxx.common.utils.Constants;
import com.devoxx.common.wear.GoogleApiConnector;
import com.devoxx.connection.Connection;
import com.devoxx.connection.cfp.model.ConferenceApiModel;
import com.devoxx.connection.vote.VoteConnection;
import com.devoxx.data.Settings_;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.model.RealmConference;
import com.devoxx.navigation.Navigator;
import com.devoxx.utils.BlurTransformation;
import com.devoxx.utils.FontUtils;
import com.devoxx.utils.GooglePlayServicesUtils;
import com.devoxx.utils.InfoUtil;
import com.devoxx.utils.ViewUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_selector)
public class SelectorActivity extends BaseActivity implements ConferenceManager.IConferencesListener,
		ConferenceManager.IConferenceDataListener, SelectorView.IWheelItemActionListener {

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	Connection connection;

	@Bean
	VoteConnection voteConnection;

	@Bean
	FontUtils fontUtils;

	@Bean
	ViewUtils viewUtils;

	@Bean Navigator navigator;

	@Pref
	Settings_ settings;

	@ViewById(R.id.selectorMainContainerImage)
	ImageView mainImage;

	@ViewById(R.id.selectorWheel)
	SelectorView selectorView;

	@ViewById(R.id.selectorGo)
	TextView goButton;

	@ViewById(R.id.selectorCurrentConference)
	TextView currentConferenceLabel;

	@ViewById(R.id.selectorCurrentConferenceInfo)
	TextView confInfo;

	@ViewById(R.id.selectorMainContainer)
	View mainContainer;

	@ViewById(R.id.selectorDaysLeft)
	SelectorValues daysLeft;

	@ViewById(R.id.selectorProposals)
	SelectorValues talks;

	@ViewById(R.id.selectorRegistrations)
	SelectorValues capacity;

	private ConferenceApiModel lastSelectedConference;

	private GoogleApiConnector mGoogleApiConnector;

	@AfterViews void afterViews() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			final int statusBarHeight = viewUtils.getStatusBarHeight();
			mainContainer.setPadding(mainContainer.getPaddingLeft(), statusBarHeight,
					mainContainer.getPaddingRight(), mainContainer.getPaddingBottom());
		}

		fontUtils.applyTypeface(currentConferenceLabel, FontUtils.Font.REGULAR);
		fontUtils.applyTypeface(goButton, FontUtils.Font.REGULAR);
		fontUtils.applyTypeface(confInfo, FontUtils.Font.REGULAR);

		setupImageColorFilter();

		mGoogleApiConnector = new GoogleApiConnector(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final boolean isLoadingData = conferenceManager.registerConferenceDataListener(this);
		conferenceManager.registerAllConferencesDataListener(this);

		if (conferenceManager.isConferenceChoosen()
				&& !conferenceManager.requestedChangeConference()) {
			final Optional<RealmConference> conference = conferenceManager.getActiveConference();
			if (conference.isPresent()) {
				setupRequiredApis(conference.get().getCfpURL(),
						conference.get().getVotingURL());
			}

			conferenceManager.updateActiveConferenceFromCfp();

			navigateToHome();
		} else if (isLoadingData) {
			selectorView.hideIcons();
			selectorView.showProgress();
			hideGoButtonForce();
			lastSelectedConference = conferenceManager.lastSelectedConference();
			loadBackgroundImage(lastSelectedConference.splashImgURL);
			setupConfInfo(lastSelectedConference);
		} else {
			conferenceManager.fetchAvailableConferences();
			selectorView.setListener(this);

			if (!GooglePlayServicesUtils.requestGooglePlayServices(this)) {
				infoUtil.showToast("Google Play Services needs to be updated.");
			}
		}
	}

	@Override
	protected void onStop() {
		conferenceManager.unregisterConferenceDataListener();
		conferenceManager.unregisterAllConferencesDataListener();

		mGoogleApiConnector.disconnect();

		super.onStop();
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		lastSelectedConference = conferenceManager.lastSelectedConference();
		if (lastSelectedConference != null) {
			setupConfInfo(lastSelectedConference);
		}
	}

	@Click(R.id.selectorGo) void onGoClick(View view) {
		if (isGoButtonHidden() || lastSelectedConference == null) {
			return;
		}

		if (connection.isOnline()) {
			if (!conferenceManager.isLastSelectedConference(lastSelectedConference)) {
				conferenceManager.clearCurrentConferenceData();

				// clear the cache used by the wearable device to display the
				mGoogleApiConnector.deleteItems(Constants.CHANNEL_ID + Constants.SCHEDULES_PATH);
			}

			setupRequiredApis(lastSelectedConference.cfpURL, lastSelectedConference.votingURL);
			conferenceManager.fetchConferenceData(lastSelectedConference);
		} else if (conferenceManager.isLastSelectedConference(lastSelectedConference)) {
			conferenceManager.openLastConference();
		} else {
			infoUtil.showToast(R.string.no_internet_connection);
		}
	}

	private boolean isGoButtonHidden() {
		return goButton.getScaleX() == 0f;
	}

	private void hideGoButtonForce() {
		goButton.clearAnimation();
		goButton.setScaleY(0f);
		goButton.setScaleX(0f);
	}

	private void hideGoButton() {
		goButton.clearAnimation();
		goButton.animate().scaleY(0f).scaleX(0f)
				.setInterpolator(new AnticipateInterpolator(1.5f))
				.setDuration(150).start();
	}

	private void showGoButton() {
		goButton.clearAnimation();
		goButton.animate().scaleY(1f).scaleX(1f)
				.setInterpolator(new OvershootInterpolator(1.5f))
				.setDuration(150).start();
	}

	@Bean
	InfoUtil infoUtil;

	@Click(R.id.selectorRegistrations) void onCapacityClick() {
		final Optional<RealmConference> conference = conferenceManager.getActiveConference();
		if (conference.isPresent()) {
			navigator.openRegister(this, conference.get().getRegURL());
		}

	}

	private void navigateToHome() {
		MainActivity_.intent(this).start();
		finish();
	}

	@Override
	public void onConferencesDataStart() {
		// TODO
	}

	@Override
	public void onConferencesAvailable(List<ConferenceApiModel> conferences) {
		for (ConferenceApiModel conference : conferences) {
			Glide.with(this).load(conference.splashImgURL).preload();
		}

		selectorView.prepareForConferences(conferences);

		if (lastSelectedConference != null) {
			selectorView.restorePreviousStateIfAny(lastSelectedConference);
		} else {
			selectNearConference(conferences);
		}

		showGoButton();
	}

	private void selectNearConference(List<ConferenceApiModel> conferences) {
		final int size = conferences.size();
		int index = -1;

		for (int i = 0; i < size; i++) {
			final ConferenceApiModel conference = conferences.get(i);
			final DateTime startConf = ConferenceManager.parseConfDate(conference.fromDate);
			final DateTime endConf = ConferenceManager.parseConfDate(conference.toDate);

			final int startConfDayOfYear = startConf.getDayOfYear();
			final int nowDayOfYear = new DateTime().getDayOfYear();

			// active conference
			if (nowDayOfYear >= startConfDayOfYear && nowDayOfYear <= endConf.getDayOfYear()) {
				index = i;
				break;
			}
		}

		// find fallback conference
		if (index == -1) {
			for (int i = 0; i < size; i++) {
				final ConferenceApiModel conference = conferences.get(i);
				final DateTime startConf = ConferenceManager.parseConfDate(conference.fromDate);

				final int startConfDayOfYear = startConf.getDayOfYear();
				final int nowDayOfYear = new DateTime().getDayOfYear();

				if (nowDayOfYear <= startConfDayOfYear) {
					index = i;
					break;
				}
			}
		}

		selectorView.selectConference(index);
	}

	@Override
	public void onConferencesError() {
		if (lastSelectedConference != null) {
			selectorView.restorePreviousStateIfAny(lastSelectedConference);
		} else {
			selectorView.defaultConference();
		}

		showGoButton();
	}

	@Override
	public void onConferenceDataStart() {
		selectorView.hideIcons();
		selectorView.showProgress();
		hideGoButton();
	}

	@Override
	public void onConferenceDataAvailable(boolean isAnyTalks) {
		if (isAnyTalks) {
			navigateToHome();
		} else {
			conferenceManager.clearCurrentConferenceData();

			showGoButton();
			selectorView.hideProgress();
			selectorView.showIcons();
			infoUtil.showToast(R.string.no_data_available);
		}
	}

	@Override
	public void onConferenceDataError() {
		showGoButton();
		selectorView.hideProgress();
		selectorView.showIcons();
		infoUtil.showToast(R.string.something_went_wrong);
	}

	@Override
	public void onWheelItemSelected(ConferenceApiModel data) {
		loadBackgroundImage(data.splashImgURL);
		setupConfInfo(data);
		lastSelectedConference = data;
	}

	private void loadBackgroundImage(String url) {
		Glide.with(this)
				.load(url)
				.bitmapTransform(new BlurTransformation(this, 5))
				.crossFade()
				.into(mainImage);
	}

	@Override
	public void onWheelItemClicked(ConferenceApiModel data) {
		setupConfInfo(data);
	}

	private void setupRequiredApis(String cfpUrl, String votingUrl) {
		connection.setupConferenceApi(cfpUrl);
		voteConnection.setupApi(votingUrl);
	}

	private void setupConfInfo(ConferenceApiModel data) {
		currentConferenceLabel.setText(data.country);
		final DateTime startDate = ConferenceManager.parseConfDate(data.fromDate);
		final DateTime endDate = ConferenceManager.parseConfDate(data.toDate);

		final DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/YYYY");
		final String endDateRaw = fmt.print(endDate);

		confInfo.setText(String.format(Locale.getDefault(),
				getString(R.string.selector_conf_info_format),
				startDate.getDayOfMonth(), endDateRaw, data.venue));

		final DateTime now = new DateTime();
		final int days = Math.max(Days.daysBetween(now, startDate).getDays(), 0);
		if (days == 0) {
			daysLeft.setVisibility(View.GONE);
		} else {
			daysLeft.setupView(getString(R.string.selector_days), days);
			daysLeft.setVisibility(View.VISIBLE);
		}

		talks.setupView(getString(R.string.selector_talks), Integer.decode(data.sessions));
		capacity.setupView(getString(R.string.selector_capacity), Integer.decode(data.capacity));
	}

	private void setupImageColorFilter() {
		final ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		final ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
		mainImage.setColorFilter(filter);
	}
}
