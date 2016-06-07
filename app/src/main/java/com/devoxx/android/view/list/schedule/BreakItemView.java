package com.devoxx.android.view.list.schedule;

import com.devoxx.R;
import com.devoxx.connection.model.BreakApiModel;
import com.devoxx.connection.model.SlotApiModel;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.list_item_break)
public class BreakItemView extends LinearLayout {

	@ViewById(R.id.list_item_break_label)
	TextView label;

	@ViewById(R.id.list_item_timespan)
	TextView timeLabel;

	@StringRes(R.string.default_break_label)
	String DEFAULT_BREAK_LABEL;

	@AfterViews void afterViews() {
		setOrientation(VERTICAL);
		setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	public void setupBreak(SlotApiModel slotModel) {
		final BreakApiModel breakModel = slotModel.slotBreak;
		String finalBreakLabel = DEFAULT_BREAK_LABEL;
		if (breakModel != null) {
			finalBreakLabel = slotModel.slotBreak.nameEN;
		}
		label.setText(finalBreakLabel);

		final String startString = TimespanItemView.formatTime(slotModel.fromTimeMs());
		final String endString = TimespanItemView.formatTime(slotModel.toTimeMs());
		timeLabel.setText(String.format(TimespanItemView.TIMESPAN_PLACEHOLDER, startString, endString));
	}

	public BreakItemView(Context context) {
		super(context);
	}

	public BreakItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BreakItemView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public BreakItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

}
