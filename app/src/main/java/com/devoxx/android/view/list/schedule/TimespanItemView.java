package com.devoxx.android.view.list.schedule;

import com.devoxx.R;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.list_item_timespan)
public class TimespanItemView extends LinearLayout {

	private static final String TIME_FORMAT_RAW = "HH:mm";
	private static final DateTimeFormatter dtf = DateTimeFormat.forPattern(TIME_FORMAT_RAW);

	public static final String TIMESPAN_PLACEHOLDER = "%s-%s";
	public static final String RUNNING_TIMESPAN_PLACEHOLDER = "NOW: %s to %s";

	@ViewById(R.id.list_item_timespan)
	TextView label;

	@ViewById(R.id.list_item_timespan_running_indicator)
	View runningIndicator;

	@ColorRes(R.color.primary)
	int notRunningTimespanColor;

	@ColorRes(R.color.running_timespan)
	int runningTimespanColor;

	public TimespanItemView(Context context) {
		super(context);
	}

	public TimespanItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TimespanItemView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public TimespanItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public void setupTimespan(long timeStart, long timeEnd, boolean running) {
		final String startString = formatTime(timeStart);
		final String endString = formatTime(timeEnd);
		if (running) {
			label.setText(String.format(RUNNING_TIMESPAN_PLACEHOLDER, startString, endString));
			label.setTextColor(runningTimespanColor);
		} else {
			label.setText(String.format(TIMESPAN_PLACEHOLDER, startString, endString));
			label.setTextColor(notRunningTimespanColor);
		}

		runningIndicator.setVisibility(running ? View.VISIBLE : GONE);
	}

	private static final DateTime DATE_TIME = new DateTime();

	public static String formatTime(long time) {
		return DATE_TIME.withMillis(time).toString(dtf);
	}
}
