package com.devoxx.android.view.speaker;

import com.devoxx.R;
import com.devoxx.android.fragment.talk.TalkFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.speaker_talk_section_item)
public class SpeakerDetailsTalkItem extends LinearLayout {

	@ViewById(R.id.speakerDetailsTalkSectionTitle)
	TextView title;

	@ViewById(R.id.speakerDetailsTalkSectionSubtitle)
	TextView subtitle;

	@ViewById(R.id.speakerDetailsTalkSectionPlace)
	TextView place;

	@ViewById(R.id.speakerDetailsTalkSectionTime)
	TextView time;

	@AfterViews void afterViews() {
		setOrientation(VERTICAL);
		setupBackground();
	}

	public void setupView(String titleVal, String subtitleVal, DateTime startDate, DateTime endDate, String placeVal) {
		title.setText(titleVal);
		subtitle.setText(subtitleVal);
		place.setText(placeVal);

		final String startDateString = startDate.toString(DateTimeFormat.forPattern(TalkFragment.DATE_TEXT_FORMAT));
		final String startTimeString = startDate.toString(DateTimeFormat.forPattern(TalkFragment.TIME_TEXT_FORMAT));
		final String endTimeString = endDate.toString(DateTimeFormat.forPattern(TalkFragment.TIME_TEXT_FORMAT));
		time.setText(String.format("%s, %s to %s", startDateString, startTimeString, endTimeString));
	}

	private void setupBackground() {
		int[] attrs = new int[]{android.R.attr.selectableItemBackground};
		TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
		Drawable drawableFromTheme = ta.getDrawable(0);
		ta.recycle();
		setBackground(drawableFromTheme);
	}

	public SpeakerDetailsTalkItem(Context context) {
		super(context);
	}

	public SpeakerDetailsTalkItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SpeakerDetailsTalkItem(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SpeakerDetailsTalkItem(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
}
