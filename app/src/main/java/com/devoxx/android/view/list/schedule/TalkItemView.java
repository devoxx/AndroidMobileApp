package com.devoxx.android.view.list.schedule;

import com.bumptech.glide.Glide;
import com.devoxx.R;
import com.devoxx.android.view.talk.ScheduleSpeakerView;
import com.devoxx.android.view.talk.ScheduleSpeakerView_;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.connection.model.TalkBaseApiModel;
import com.devoxx.connection.model.TalkFullApiModel;
import com.devoxx.connection.model.TalkSpeakerApiModel;
import com.devoxx.data.downloader.TracksDownloader;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.user.UserFavouritedTalksManager;
import com.google.android.flexbox.FlexboxLayout;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.res.DimensionPixelOffsetRes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.list_item_talk)
public class TalkItemView extends LinearLayout {

	private static final String DAY_TEXT_FORMAT = "EEE dd"; // WED 11

	@ViewById(R.id.list_item_talk_title)
	TextView title;

	@ViewById(R.id.list_item_talk_track)
	TextView track;

	@ViewById(R.id.list_item_talk_place)
	TextView place;

	@ViewById(R.id.list_item_talk_track_icon)
	ImageView trackIcon;

	@ViewById(R.id.list_item_talk_track_schedule)
	ImageView scheduleIcon;

	@ViewById(R.id.list_item_timespan_running_first_indicator)
	View runningFirstIndicator;

	@ViewById(R.id.list_item_timespan_running_second_indicator)
	View runningSecondIndicator;

	@ViewById(R.id.list_item_talk_time_container)
	View timeContainer;

	@ViewById(R.id.list_item_talk_time)
	TextView time;

	@ViewById(R.id.list_item_talk_speakers_container) FlexboxLayout speakersContainer;

	@ColorRes(R.color.primary)
	int scheduledIndicatorColor;

	@DimensionPixelOffsetRes(R.dimen.value_4dp) int margin;

	@Bean
	TracksDownloader tracksDownloader;

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	UserFavouritedTalksManager userFavouritedTalksManager;

	@AfterViews void afterViews() {
		setOrientation(VERTICAL);
		setGravity(Gravity.CENTER_VERTICAL);
		setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	public TalkItemView setupTalk(SlotApiModel slotModel) {
		if (slotModel.notAllocated) {
			// TODO Handle somehow free slots.
			title.setText("Free slot...");
			place.setText(slotModel.roomName);
			track.setText("");
			trackIcon.setImageDrawable(null);
		} else {
			final TalkFullApiModel talkModel = slotModel.talk;
			title.setText(talkModel.title);
			track.setText(talkModel.track);
			place.setText(slotModel.roomName);

			final DateTime date = slotModel.fromTime();
			final String dateRaw = date.toString(DateTimeFormat.forPattern(DAY_TEXT_FORMAT));
			time.setText(String.format("%s, %s-%s", dateRaw, slotModel.fromTime, slotModel.toTime));

			Glide.with(getContext())
					.load(obtainTrackIconUrl(talkModel))
					.placeholder(R.drawable.th_background)
					.into(trackIcon);

			speakersContainer.removeAllViews();

			for (TalkSpeakerApiModel speaker : slotModel.talk.speakers) {
				final ScheduleSpeakerView speakerView = ScheduleSpeakerView_.build(getContext());
				final String uuid = TalkSpeakerApiModel.getUuidFromLink(speaker.link);
				final String url = speakersDataManager.imageUrlByUuid(uuid);
				speakerView.setupView(speaker.name, url);

				final FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				speakersContainer.addView(speakerView, lp);
			}
		}

		if (userFavouritedTalksManager.isFavouriteTalk(slotModel.slotId)) {
			scheduleIcon.setVisibility(View.VISIBLE);
			scheduleIcon.setColorFilter(scheduledIndicatorColor, PorterDuff.Mode.MULTIPLY);
		} else {
			scheduleIcon.setVisibility(View.GONE);
		}

		setupNormalBackground();

		return this;
	}

	public void showRunningIndicator(boolean show) {
		runningSecondIndicator.setVisibility(show ? VISIBLE : INVISIBLE);
	}

	public TalkItemView showRunningDoubleIndicator(boolean runningItem, boolean isPreviousRunning) {
		runningFirstIndicator.setVisibility(runningItem ?
				(isPreviousRunning ? INVISIBLE : VISIBLE) : INVISIBLE);
		runningSecondIndicator.setVisibility(runningItem ? VISIBLE : INVISIBLE);
		return this;
	}

	private String obtainTrackIconUrl(TalkBaseApiModel slotModel) {
		return tracksDownloader.getTrackIconUrl(slotModel.trackId);
	}

	public TalkItemView(Context context) {
		super(context);
	}

	public TalkItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TalkItemView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public TalkItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public TalkItemView withoutTrackName() {
		track.setVisibility(View.GONE);
		return this;
	}

	public void withTime() {
		timeContainer.setVisibility(View.VISIBLE);
	}

	private void setupNormalBackground() {
		int[] attrs = new int[]{android.R.attr.selectableItemBackground};
		TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
		Drawable drawableFromTheme = ta.getDrawable(0);
		ta.recycle();
		setBackground(drawableFromTheme);
	}
}
