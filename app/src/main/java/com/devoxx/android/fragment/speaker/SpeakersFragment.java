package com.devoxx.android.fragment.speaker;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.devoxx.R;
import com.devoxx.android.fragment.common.BaseMenuFragment;
import com.devoxx.connection.Connection;
import com.devoxx.data.RealmProvider;
import com.devoxx.data.Settings_;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.model.RealmSpeakerShort;
import com.devoxx.utils.DeviceUtil;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EFragment(R.layout.fragment_speakers)
public class SpeakersFragment extends BaseMenuFragment {

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	RealmProvider realmProvider;

	@Bean
	Connection connection;

	@Bean
	DeviceUtil deviceUtil;

	@Pref
	Settings_ settings;

	@SystemService
	InputMethodManager inputMethodManager;

	@ViewById(R.id.listView)
	ListView listView;

	@ViewById(R.id.listViewContainer)
	View container;

	private ItemAdapter itemAdapter;
	private Collator collator;

	@AfterInject void afterInject() {
		itemAdapter = new ItemAdapter();
	}

	@AfterViews void afterViewsInternal() {
		super.afterViews();

		collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);

		listView.setAdapter(itemAdapter);

		final List<RealmSpeakerShort> speakers =
				speakersDataManager.getAllShortSpeakers();
		populateList(speakers);

		// By Default open first speaker in landscape mode.
		if (deviceUtil.isLandscapeTablet()) {
			navigator.openSpeakerDetails(getActivity(),
					itemAdapter.getClickedItem(0).getUuid());
		}

		listView.setOnItemClickListener((parent, view, position, id) ->
				handleSpeakerClick(itemAdapter.getClickedItem(position).getUuid()));

		listView.setOnTouchListener((v, event) -> {
			if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
				inputMethodManager.hideSoftInputFromWindow(listView.getWindowToken(), 0);
			}
			return false;
		});
	}

	@Override
	public int getMenuRes() {
		return R.menu.speakers_menu;
	}

	@Override
	protected void onSearchQuery(String query) {
		final List<RealmSpeakerShort> speakers = speakersDataManager.
				getAllShortSpeakersWithFilter(query);
		populateList(speakers);
	}

	private void handleSpeakerClick(String speakeruuid) {
		if (speakersDataManager.isExists(speakeruuid) || connection.isOnline()) {
			navigator.openSpeakerDetails(getActivity(), speakeruuid);
		} else {
			infoUtil.showToast(R.string.internet_connection_is_needed);
		}
	}

	private void populateList(List<RealmSpeakerShort> speakers) {
		final List<SpeakersGroup> list = Stream.of(speakers)
				.groupBy(speakerFirstLetterGroupping())
				.map(speakersGroupMapper())
				.sorted((lhs, rhs) -> collator.compare(lhs.getGroupLetter(), rhs.getGroupLetter()))
				.collect(Collectors.<SpeakersGroup>toList());

		itemAdapter.setSpeakers(list);
		itemAdapter.notifyDataSetChanged();
	}

	private static Function<Map.Entry<String,
			List<RealmSpeakerShort>>, SpeakersGroup> speakersGroupMapper() {
		return value -> {
			final List<RealmSpeakerShort> list =
					Stream.of(value.getValue())
							.sortBy(value1 -> value1.getFirstName().toLowerCase())
							.collect(Collectors.<RealmSpeakerShort>toList());
			return new SpeakersGroup(value.getKey(), list);
		};
	}

	private static Function<RealmSpeakerShort, String> speakerFirstLetterGroupping() {
		return value -> value.getFirstName().substring(0, 1).toLowerCase();
	}

	class ItemAdapter extends BaseAdapter {

		private List<SpeakersGroup> speakers;
		private int size;

		public void setSpeakers(List<SpeakersGroup> speakers) {
			this.speakers = new ArrayList<>(speakers.size());
			this.speakers.addAll(speakers);
			rebuildIndexes();
		}

		private void rebuildIndexes() {
			size = 0;
			for (SpeakersGroup speaker : speakers) {
				final int groupSize = speaker.speakersSize();
				speaker.setStartIndex(size);
				size += groupSize;
				speaker.setStopIndex(size - 1);
			}
		}

		@Override
		public int getCount() {
			return size;
		}

		@Override
		public Object getItem(int position) {
			// Not used at all. See getItemGroup();
			return null;
		}

		@NonNull
		private SpeakersGroup getItemGroup(int position) {
			for (SpeakersGroup speaker : speakers) {
				final int startIndex = speaker.getStartIndex();
				final int stopIndex = speaker.getStopIndex();
				if (position >= startIndex && position <= stopIndex) {
					return speaker;
				}
			}

			throw new IllegalStateException("Should not be here!");
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final View viewItem;
			final ViewHolder holder;

			if (convertView == null) {
				viewItem = getActivity().getLayoutInflater()
						.inflate(R.layout.speakers_all_list_item, parent, false);
				holder = new ViewHolder();
				holder.textSpeaker = (TextView) viewItem.findViewById(R.id.textSpeaker);
				holder.textLetter = (TextView) viewItem.findViewById(R.id.textSpeakersGroupFirstLetter);
				holder.imageSpeaker = (ImageView) viewItem.findViewById(R.id.imageSpeaker);
				holder.divider = viewItem.findViewById(R.id.speakerItemDivider);
				viewItem.setTag(holder);
			} else {
				viewItem = convertView;
				holder = (ViewHolder) viewItem.getTag();
			}

			setupView(position, holder);

			return viewItem;
		}

		private void setupView(int position, final ViewHolder holder) {
			final SpeakersGroup group = getItemGroup(position);
			final RealmSpeakerShort speakerItem = group.getSpeakerByGlobalPosition(position);
			holder.textSpeaker.setText(String.format("%s %s",
					speakerItem.getFirstName(), speakerItem.getLastName()));

			final boolean shouldLetterBeVisible = position == group.getStartIndex();
			holder.textLetter.setVisibility(shouldLetterBeVisible ? View.VISIBLE : View.INVISIBLE);
			holder.textLetter.setText(group.getGroupLetter());

			final boolean shouldDividerBeVisible = group.getStopIndex() == position;
			holder.divider.setVisibility(shouldDividerBeVisible ? View.VISIBLE : View.GONE);

			setupImage(holder, speakerItem);
		}

		private void setupImage(final ViewHolder holder, RealmSpeakerShort speakerItem) {
			Glide.with(getMainActivity())
					.load(speakerItem.getAvatarURL())
					.asBitmap()
					.centerCrop()
					.placeholder(R.drawable.ic_speaker_placeholder)
					.error(R.drawable.ic_speaker_placeholder)
					.fallback(R.drawable.ic_speaker_placeholder)
					.into(new BitmapImageViewTarget(holder.imageSpeaker) {
						@Override
						public void onResourceReady(
								Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
							final RoundedBitmapDrawable circularBitmapDrawable =
									RoundedBitmapDrawableFactory.create(
											holder.imageSpeaker.getResources(), resource);
							circularBitmapDrawable.setCircular(true);
							holder.imageSpeaker.setImageDrawable(circularBitmapDrawable);
						}
					});
		}

		public RealmSpeakerShort getClickedItem(int position) {
			return getItemGroup(position).getSpeakerByGlobalPosition(position);
		}

		private class ViewHolder {
			public TextView textSpeaker;
			public TextView textLetter;
			public ImageView imageSpeaker;
			public View divider;
		}
	}

	private static class SpeakersGroup {
		private String groupLetter;
		private List<RealmSpeakerShort> speakers;

		private int startIndex, stopIndex;

		public SpeakersGroup(String groupLetter, List<RealmSpeakerShort> speakers) {
			this.groupLetter = groupLetter;
			this.speakers = speakers;
		}

		public String getGroupLetter() {
			return groupLetter;
		}

		public int speakersSize() {
			return speakers != null ? speakers.size() : 0;
		}

		public int getStartIndex() {
			return startIndex;
		}

		public void setStartIndex(int startIndex) {
			this.startIndex = startIndex;
		}

		public int getStopIndex() {
			return stopIndex;
		}

		public void setStopIndex(int stopIndex) {
			this.stopIndex = stopIndex;
		}

		@NonNull
		public RealmSpeakerShort getSpeakerByGlobalPosition(int globalPosition) {
			return speakers.get(globalPosition - startIndex);
		}
	}
}
