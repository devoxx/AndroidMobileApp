package com.devoxx.android.fragment.map;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.devoxx.R;
import com.devoxx.android.fragment.common.BaseFragment;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;

import android.widget.ImageView;

import uk.co.senab.photoview.PhotoViewAttacher;

@EFragment(R.layout.fragment_map_floor)
public class MapFloorFragment extends BaseFragment {

	@FragmentArg
	String imageUrl;

	@ViewById(R.id.mapFloorImage)
	ImageView imageView;

	@AfterViews void afterViews() {
		Glide.with(getMainActivity())
				.load(imageUrl)
				.dontTransform()
				.listener(new RequestListener<String, GlideDrawable>() {
					@Override
					public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
						return false;
					}

					@Override
					public boolean onResourceReady(
							GlideDrawable resource, String model,
							Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
						if (getActivity() != null && imageView != null) {
							final PhotoViewAttacher pa = new PhotoViewAttacher(imageView);
							pa.setScaleType(ImageView.ScaleType.CENTER_CROP);
							pa.setScaleLevels(1f, 2f, 4f);
						}
						return false;
					}
				})
				.into(imageView);
	}
}
