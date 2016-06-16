package com.devoxx.android.view.talk;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.devoxx.R;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DimensionPixelOffsetRes;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.schedule_item_speaker_container)
public class ScheduleSpeakerView extends LinearLayout {

	@ViewById(R.id.scheduleItemSpeakerContainerImage) ImageView imageView;
	@ViewById(R.id.scheduleItemSpeakerContainerLabel) TextView textView;

	@DimensionPixelOffsetRes(R.dimen.schedule_speaker_image_size) int imageSize;
	@DimensionPixelOffsetRes(R.dimen.value_4dp) int padding;

	private Paint rectPaint;
	private Paint imageBckgPaint;

	@AfterInject void afterInject() {
		setWillNotDraw(false);
		setOrientation(HORIZONTAL);
		setGravity(Gravity.CENTER_VERTICAL);

		rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		rectPaint.setColor(Color.WHITE);
		rectPaint.setStyle(Paint.Style.FILL);

		imageBckgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		imageBckgPaint.setColor(Color.parseColor("#10212121"));
		imageBckgPaint.setStyle(Paint.Style.FILL);
	}

	public void setupView(String name, String url) {
		textView.setText(name);

		Glide.with(getContext())
				.load(url)
				.asBitmap()
				.centerCrop()
				.override(imageSize, imageSize)
				.placeholder(R.drawable.ic_speaker_placeholder)
				.error(R.drawable.ic_speaker_placeholder)
				.fallback(R.drawable.ic_speaker_placeholder)
				.into(new BitmapImageViewTarget(imageView) {
					@Override
					public void onResourceReady(
							Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
						final RoundedBitmapDrawable circularBitmapDrawable =
								RoundedBitmapDrawableFactory.create(
										imageView.getResources(), resource);
						circularBitmapDrawable.setCircular(true);
						imageView.setImageDrawable(circularBitmapDrawable);
					}
				});
	}

	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + 2 * padding);
	}

	@Override protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final int imageHeight = imageView.getMeasuredHeight();
		final int width = getMeasuredWidth();

		final float rectRadius = imageHeight / 2;
		final float rx = width - rectRadius;
		final float ry = imageHeight - rectRadius;

		canvas.drawRect(rectRadius, padding, width - rectRadius, imageHeight + padding, rectPaint);
		canvas.drawCircle(rectRadius, padding + rectRadius, rectRadius, imageBckgPaint);
		canvas.drawCircle(rx, padding + ry, rectRadius, rectPaint);
	}

	public ScheduleSpeakerView(Context context) {
		super(context);
	}

	public ScheduleSpeakerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ScheduleSpeakerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ScheduleSpeakerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
}
