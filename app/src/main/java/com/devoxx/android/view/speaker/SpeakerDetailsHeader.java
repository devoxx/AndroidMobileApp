package com.devoxx.android.view.speaker;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.devoxx.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@EViewGroup(R.layout.fragment_speaker_details_toolbar_include)
public class SpeakerDetailsHeader extends LinearLayout {

	@ViewById(R.id.header_view_title)
	TextView title;

	@ViewById(R.id.header_view_sub_title)
	TextView subtitle;

	@ViewById(R.id.header_view_image)
	ImageView image;

	@AfterViews void afterViews() {
		setOrientation(VERTICAL);
	}

	public void setupHeader(String imageUrl, String titleVal, String subtitleVal) {
		title.setText(titleVal);
		subtitle.setText(subtitleVal);
		image.setVisibility(View.VISIBLE);

		Glide.with(getContext())
				.load(imageUrl)
				.asBitmap()
				.placeholder(R.drawable.ic_speaker_placeholder)
				.error(R.drawable.ic_speaker_placeholder)
				.fallback(R.drawable.ic_speaker_placeholder)
				.centerCrop()
				.into(new BitmapImageViewTarget(image) {
					@Override
					public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
						final RoundedBitmapDrawable circularBitmapDrawable =
								RoundedBitmapDrawableFactory.create(
										image.getResources(), resource);
						circularBitmapDrawable.setCircular(true);
						image.setImageDrawable(circularBitmapDrawable);
					}
				});
	}

	public void setupHeader(String titleVal, String subtitleVal) {
		title.setText(titleVal);
		subtitle.setText(subtitleVal);
		image.setVisibility(View.GONE);
	}

	public void setImageAlpha(int alpha) {
		image.setImageAlpha(alpha);
	}

	public int getImageHeight() {
		return image.getMeasuredHeight();
	}

	public void applyImageBottomMargin(int bottomMargin) {
		final LinearLayout.LayoutParams lp = (LayoutParams) image.getLayoutParams();
		lp.bottomMargin = bottomMargin;
		image.setLayoutParams(lp);
	}

	public TextView getTitle() {
		return title;
	}

	public SpeakerDetailsHeader(Context context) {
		super(context);
	}

	public SpeakerDetailsHeader(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SpeakerDetailsHeader(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SpeakerDetailsHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
}
