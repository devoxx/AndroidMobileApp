package com.devoxx.android.fragment.talk;

import com.annimon.stream.Optional;
import com.bumptech.glide.Glide;
import com.devoxx.BuildConfig;
import com.devoxx.R;
import com.devoxx.android.fragment.common.BaseFragment;
import com.devoxx.android.fragment.schedule.ScheduleLineupFragment;
import com.devoxx.android.view.talk.TalkDetailsHeader;
import com.devoxx.android.view.talk.TalkDetailsSectionClickableItem;
import com.devoxx.android.view.talk.TalkDetailsSectionClickableItem_;
import com.devoxx.android.view.talk.TalkDetailsSectionItem;
import com.devoxx.android.view.talk.TalkDetailsSectionItem_;
import com.devoxx.common.utils.Constants;
import com.devoxx.connection.Connection;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.connection.model.TalkFullApiModel;
import com.devoxx.connection.model.TalkSpeakerApiModel;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.manager.NotificationsManager;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.model.RealmConference;
import com.devoxx.data.user.UserFavouritedTalksManager;
import com.devoxx.data.user.UserManager;
import com.devoxx.data.vote.interfaces.IOnVoteForTalkListener;
import com.devoxx.data.vote.interfaces.ITalkVoter;
import com.devoxx.data.vote.voters.TalkVoter;
import com.devoxx.event.ScheduleEvent;
import com.devoxx.navigation.Navigator;
import com.devoxx.utils.DeviceUtil;
import com.devoxx.utils.InfoUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import pl.tajchert.buswear.EventBus;

@EFragment(R.layout.fragment_talk)
public class TalkFragment extends BaseFragment implements AppBarLayout.OnOffsetChangedListener {

	public static final String DATE_TEXT_FORMAT = "MMMM dd, yyyy"; // April 20, 2014
	public static final String TIME_TEXT_FORMAT = "HH:mm"; // 9:30
	private static final float FULL_FACTOR = 1f;

	@Bean
	DeviceUtil deviceUtil;

	@Bean
	NotificationsManager notificationsManager;

	@Bean
	UserFavouritedTalksManager userFavouritedTalksManager;

	@Bean
	Navigator navigator;

	@Bean
	Connection connection;

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	InfoUtil infoUtil;

	@Bean
	UserManager userManager;

	@Bean(TalkVoter.class)
	ITalkVoter talkVoter;

	@SystemService
	LayoutInflater li;

	@FragmentArg
	SlotApiModel slotApiModel;

	@FragmentArg
	boolean notifyAboutChange;

	@ViewById(R.id.talkDetailsScheduleBtn)
	FloatingActionButton scheduleButton;

	@ViewById(R.id.talkDetailsLikeBtn)
	FloatingActionButton voteButton;

	@ViewById(R.id.fragment_talk_toolbar)
	Toolbar toolbar;

	@ViewById(R.id.main_appbar)
	AppBarLayout appBarLayout;

	@ViewById(R.id.main_collapsing)
	CollapsingToolbarLayout collapsingToolbarLayout;

	@ViewById(R.id.toolbar_header_view)
	TalkDetailsHeader toolbarHeaderView;

	@ViewById(R.id.float_header_view)
	TalkDetailsHeader floatHeaderView;

	@ViewById(R.id.talkDetailsContainer)
	LinearLayout sectionContainer;

	@ViewById(R.id.talkDetailsDescription)
	TextView description;

	@ViewById(R.id.main_backdrop)
	ImageView backgroundImage;

	private boolean shouldHideToolbarHeader = false;
	private SlotApiModel slotModel;

	private GoogleApiClient mApiClient;

	@AfterViews void afterViews() {
		setHasOptionsMenu(!deviceUtil.isTablet());
		setupMainLayout();

		if (deviceUtil.isLandscapeTablet() && slotApiModel != null) {
			setupFragment(slotApiModel, notifyAboutChange);
		}

		EventBus.getDefault().register(this);
	}

	@Click(R.id.talkDetailsScheduleBtn) void onScheduleButtonClick() {
		if (userFavouritedTalksManager.isFavouriteTalk(slotModel.slotId)) {
			userFavouritedTalksManager.unFavouriteTalk(slotModel.slotId);
			notificationsManager.removeNotification(slotModel.slotId);
		} else {
			userFavouritedTalksManager.favouriteTalk(slotModel.slotId);
			notificationsManager.scheduleNotification(slotModel, true);
			infoUtil.showToast("Talk marked as favorited.");
		}

		showScheduleChange();
		sendToWearable();
	}

	private void showScheduleChange() {
		if (deviceUtil.isLandscapeTablet()) {
			// Notify ScheduleLineupFragment about change.
			getActivity().sendBroadcast(ScheduleLineupFragment.getRefreshIntent());
		} else {
			notifyHostActivityAboutChangeOccured();
		}

		setupScheduleButton();
	}


	// This event is received when a schedule has been changed from the wearable device
	@UiThread
	public void onEvent(ScheduleEvent scheduleEvent) {
		showScheduleChange();
	}


	private void sendToWearable() {

		if (!slotModel.isTalk()) {
			return;
		}

		// send the event to the wearable
		final PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.CHANNEL_ID + Constants.FAVORITE_PATH + "/" + slotModel.talk.id);

		// store the data
		DataMap dataMap = new DataMap();
		dataMap.putBoolean("favorite", userFavouritedTalksManager.isFavouriteTalk(slotModel.slotId));

		// store the event in the datamap to send it to the wear
		putDataMapRequest.getDataMap().putDataMap(Constants.DETAIL_PATH, dataMap);

		mApiClient = new GoogleApiClient.Builder(getContext())
				.addApi(Wearable.API)
				.addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
					@Override
					public void onConnected(Bundle bundle) {
						Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest());
					}

					@Override
					public void onConnectionSuspended(int cause) {

					}
				}).build();
		mApiClient.connect();

	}


	@Click(R.id.talkDetailsNotesBtn) void onNotesClick() {
		openGoogleKeepIfExists();
	}

	private void openGoogleKeepIfExists() {
		final String keepPackageName = "com.google.android.keep";
		try {
			final String action = "android.intent.action.SEND";
			final String mimeType = "text/plain";
			final Intent intent = new Intent(action);
			intent.setType(mimeType);
			intent.putExtra("android.intent.extra.TEXT", buildNoteText());
			intent.setPackage(keepPackageName);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			infoUtil.showToast("Please install the Google Keep app to store your notes.");

			try {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + keepPackageName)));
			} catch (android.content.ActivityNotFoundException anfe) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + keepPackageName)));
			}
		}
	}

	private String buildNoteText() {
		final TalkFullApiModel talk = slotModel.talk;
		return String.format(Locale.getDefault(), "%s", talk.title);
	}

	@Click(R.id.talkDetailsTweetBtn) void onTweetClick() {
		final Optional<RealmConference> conference = conferenceManager.getActiveConference();
		if (conference.isPresent()) {

			final String twitterMessage = String.format("%s\n%s %s %s",
					slotModel.talk.title,
					slotModel.talk.getReadableSpeakers(),
					createWebLink(conference.get(), slotModel),
					conference.get().getHashtag());

			navigator.tweetMessage(getActivity(), twitterMessage);
		}
	}

	private String createWebLink(RealmConference conference, SlotApiModel slot) {
		return String.format("%s%s", conference.getTalkURL(), slot.talk.id);
	}

	@Click(R.id.talkDetailsLikeBtn) void onVoteClick() {
		if (userManager.isFirstTimeUser()) {
			userManager.openUserScanBadge();
		} else if (!talkVoter.isAlreadyVoted(slotModel.talk.id)) {
			talkVoter.showVoteDialog(getActivity(), slotModel, new IOnVoteForTalkListener() {
				@Override
				public void onVoteForTalkSucceed() {
					if (isLive()) {
						setupVoteIcon();
					}
				}

				@Override
				public void onVoteForTalkFailed() {
					if (isLive()) {
						infoUtil.showToast(R.string.something_went_wrong);
					}
				}

				@Override
				public void onCantVoteOnTalkYet() {
					if (isLive()) {
						infoUtil.showToast("Cannot vote on talk yet");
					}
				}

				@Override
				public void onCantVoteMoreThanOnce() {
					if (isLive()) {
						infoUtil.showToast("Cannot vote more than once");
					}
				}
			});
		} else {
			if (isLive()) {
				infoUtil.showToast("You've already voted on this talk!");
			}
		}
	}

	@Override
	public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
		int maxScroll = appBarLayout.getTotalScrollRange();
		float factor = (float) Math.abs(offset) / (float) maxScroll;

		if (factor == FULL_FACTOR && shouldHideToolbarHeader) {
			toolbarHeaderView.setVisibility(View.VISIBLE);
			shouldHideToolbarHeader = !shouldHideToolbarHeader;
		} else if (factor < FULL_FACTOR && !shouldHideToolbarHeader) {
			toolbarHeaderView.setVisibility(View.GONE);
			shouldHideToolbarHeader = !shouldHideToolbarHeader;
		}
	}

	public void setupFragment(SlotApiModel slot, boolean notifyParentAboutChange) {
		slotModel = slot;
		toolbarHeaderView.setupHeader(slot.talk.title, slot.talk.track);
		floatHeaderView.setupHeader(slot.talk.title, slot.talk.track);
		description.setText(Html.fromHtml(slot.talk.summaryAsHtml));
		setupScheduleButton();

		fillSectionsContainer(slot);

		if (notifyParentAboutChange) {
			notifyHostActivityAboutChangeOccured();
		}

		setupVoteIcon();
	}

	private void setupVoteIcon() {
		if (BuildConfig.TEST_VOTE || (talkVoter.isVotingEnabled() && talkVoter.canVoteForTalk(slotModel))) {
			voteButton.setVisibility(View.VISIBLE);

			if (talkVoter.isAlreadyVoted(slotModel.talk.id)) {
				voteButton.setImageResource(R.drawable.ic_heart);
			} else {
				voteButton.setImageResource(R.drawable.ic_heart_outline);
			}
		} else {
			voteButton.setVisibility(View.GONE);
		}
	}

	private void notifyHostActivityAboutChangeOccured() {
		navigator.setUpdateNeeded();
	}

	private void setupScheduleButton() {
		if (scheduleButton != null) {
			if (userFavouritedTalksManager.isFavouriteTalk(slotModel.slotId)) {
				scheduleButton.setImageResource(R.drawable.ic_star);
			} else {
				scheduleButton.setImageResource(R.drawable.ic_star_border);
			}
		}
	}

	private void fillSectionsContainer(SlotApiModel slotModel) {
		sectionContainer.removeAllViews();
		sectionContainer.addView(createDateTimeSection(slotModel));
		sectionContainer.addView(createPresenterSection(slotModel));
		sectionContainer.addView(createRoomSection(slotModel));
		sectionContainer.addView(createFormatSection(slotModel));
	}

	private View createFormatSection(SlotApiModel slotModel) {
		return createSection(R.drawable.ic_format, R.string.talk_details_section_format, slotModel.talk.talkType);
	}

	private View createRoomSection(SlotApiModel slotModel) {
		return createSection(R.drawable.ic_place_big, R.string.talk_details_section_room, slotModel.roomName);
	}

	private View createPresenterSection(SlotApiModel slotModel) {
		final boolean manyGuys = slotModel.talk.speakers.size() > 1;
		return createClickableSection(R.drawable.ic_microphone_big,
				manyGuys ? R.string.talk_details_section_presentors : R.string.talk_details_section_presentor,
				slotModel.talk.speakers);
	}

	private View createClickableSection(
			@DrawableRes int icon, @StringRes int title, List<TalkSpeakerApiModel> readableSpeakers) {
		final TalkDetailsSectionClickableItem result = TalkDetailsSectionClickableItem_.build(getContext());
		result.setupView(icon, title);
		final ViewGroup container = result.getSpeakersContainer();
		for (TalkSpeakerApiModel speaker : readableSpeakers) {
			final View containerInner = li.inflate(
					R.layout.talk_details_section_speaker_item, container, false);
			final TextView speakerView = (TextView) containerInner
					.findViewById(R.id.talkDetailsSectionSpeaker);
			SpannableString content = new SpannableString(speaker.name);
			content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
			speakerView.setText(content);
			final String speakeruuid = TalkSpeakerApiModel.getUuidFromLink(speaker.link);
			containerInner.setOnClickListener(v -> handleSpeakerClick(speakeruuid));
			result.addSpeakerView(containerInner);
		}
		return result;
	}

	private void handleSpeakerClick(String speakeruuid) {
		if (speakersDataManager.isExists(speakeruuid) || connection.isOnline()) {
			navigator.openSpeakerDetails(getActivity(), speakeruuid);
		} else {
			infoUtil.showToast(R.string.internet_connection_is_needed);
		}
	}

	private View createDateTimeSection(SlotApiModel slotModel) {
		final DateTime startDate = slotModel.fromTime();
		final DateTime endDate = slotModel.toTime();
		final String startDateString = startDate.toString(DateTimeFormat.forPattern(DATE_TEXT_FORMAT));
		final String startTimeString = startDate.toString(DateTimeFormat.forPattern(TIME_TEXT_FORMAT));
		final String endTimeString = endDate.toString(DateTimeFormat.forPattern(TIME_TEXT_FORMAT));
		return createSection(R.drawable.ic_access_time_white_48dp, R.string.talk_details_section_date_time,
				String.format("%s, %s to %s", startDateString, startTimeString, endTimeString));
	}

	private TalkDetailsSectionItem createSection(@DrawableRes int icon, @StringRes int title, String subtitle) {
		final TalkDetailsSectionItem result = TalkDetailsSectionItem_.build(getContext());
		result.setupView(icon, title, subtitle);
		return result;
	}

	private void setupMainLayout() {
		collapsingToolbarLayout.setTitle(" ");
		final AppCompatActivity baseActivity = ((AppCompatActivity) getActivity());
		toolbar.setNavigationOnClickListener(v -> baseActivity.finish());

		if (!deviceUtil.isLandscapeTablet()) {
			baseActivity.setSupportActionBar(toolbar);
			baseActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		appBarLayout.addOnOffsetChangedListener(this);

		Glide.with(getActivity()).load(R.drawable.talk_background_image).into(backgroundImage);
	}
}