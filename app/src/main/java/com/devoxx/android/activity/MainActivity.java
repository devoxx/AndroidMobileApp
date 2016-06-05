package com.devoxx.android.activity;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.devoxx.R;
import com.devoxx.android.fragment.common.EmptyFragment_;
import com.devoxx.android.fragment.schedule.ScheduleMainFragment_;
import com.devoxx.android.fragment.speaker.SpeakersFragment_;
import com.devoxx.android.fragment.track.TracksMainFragment_;
import com.devoxx.connection.model.SlotApiModel;
import com.devoxx.data.Settings_;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.manager.NotificationsManager;
import com.devoxx.data.manager.SlotsDataManager;
import com.devoxx.data.manager.SpeakersDataManager;
import com.devoxx.data.model.RealmConference;
import com.devoxx.data.schedule.search.SearchManager;
import com.devoxx.integrations.IntegrationProvider;
import com.devoxx.integrations.huntly.HuntlyController;
import com.devoxx.integrations.huntly.HuntlyPresenter;
import com.devoxx.navigation.Navigator;
import com.devoxx.utils.FontUtils;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.ColorRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity {

	private static final String TAG_CONTENT_FRAGMENT = "content_fragment";
	private static final String TAG_CONTENT_FRAGMENT_SECOND = "content_fragment_second";
	private static final java.lang.String LAST_CLICKED_ITEM_ID = "last_clicked_item";

	@Bean
	Navigator navigator;

	@Bean
	SlotsDataManager slotsDataManager;

	@Bean
	SpeakersDataManager speakersDataManager;

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	FontUtils fontUtils;

	@Bean
	InfoUtil infoUtil;

	@Bean
	SearchManager searchManager;

	@Bean
	IntegrationProvider integrationProvider;

	@Pref
	Settings_ settings;

	@ViewById(R.id.toolbar)
	Toolbar toolbar;

	@ViewById(R.id.menuContainer)
	ViewGroup menuContainer;

	@ViewById(R.id.menu_schedule)
	View menuScheduleView;

	@ViewById(R.id.content_frame_second)
	View secondFragmentContainer;

	@ColorRes(R.color.primary_text)
	int selectedTabColor;

	@ColorRes(R.color.tab_text_unselected)
	int unselectedTabColor;


	private String fromNotificationSlotId;
	private boolean isSavedInstanceState;
	private int lastClickedMenuId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isSavedInstanceState = savedInstanceState != null;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.hasExtra(NotificationsManager.EXTRA_TALK_ID)) {
			fromNotificationSlotId = intent.getStringExtra(
					NotificationsManager.EXTRA_TALK_ID);
			loadTalkFromNotification();
		}
	}

	@AfterViews void afterViews() {
		setupToolbar();

		if (!isSavedInstanceState) {
			loadCoreData();
		}
	}

	@Click({R.id.menu_schedule, R.id.menu_tracks, R.id.menu_speakers, R.id.menu_map})
	void onMainMenuClick(View view) {
		handleMenuClick(view.getId());
	}

	private void handleMenuClick(final int id) {
		lastClickedMenuId = id;
		setupMenuAppearance(lastClickedMenuId);
		switch (lastClickedMenuId) {
			case R.id.menu_schedule:
				openSchedule();
				break;
			case R.id.menu_tracks:
				openTracks();
				break;
			case R.id.menu_speakers:
				openSpeakers();
				break;
			case R.id.menu_map:
				openMaps();
				break;
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		lastClickedMenuId = savedInstanceState.getInt(LAST_CLICKED_ITEM_ID);
		handleMenuClick(lastClickedMenuId);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(LAST_CLICKED_ITEM_ID, lastClickedMenuId);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		supportInvalidateOptionsMenu();
		integrationProvider.provideIntegrationController()
				.handleAppResume(conferenceManager.getActiveConference()
						.get().getIntegrationId(), this);

		conferenceManager.updateSlotsIfNeededAsync();
	}

	@Receiver(actions = {HuntlyPresenter.INTEGRATION_DIALOG_DISMISSED, HuntlyController.USER_DATA_UPDATED},
			registerAt = Receiver.RegisterAt.OnCreateOnDestroy) void onIntegrationDialogDismissed() {
		supportInvalidateOptionsMenu();
	}

	@Override
	protected void onDestroy() {
		searchManager.clearLastQuery();
		super.onDestroy();
	}

	private void openMaps() {
		navigator.openMaps(this);
	}

	private void openSpeakers() {
		replaceFragmentInGivenContainer(SpeakersFragment_.builder().build(), false, R.id.content_frame);
	}

	private void openTracks() {
		replaceFragmentInGivenContainer(TracksMainFragment_.builder().build(), false, R.id.content_frame);

		if (deviceUtil.isLandscapeTablet()) {
			replaceFragmentInGivenContainer(EmptyFragment_.builder().build(), false, R.id.content_frame_second);
		}
	}

	private void openSchedule() {
		replaceFragmentInGivenContainer(ScheduleMainFragment_.builder().build(), false, R.id.content_frame);

		if (deviceUtil.isLandscapeTablet()) {
			replaceFragmentInGivenContainer(EmptyFragment_.builder().build(), false, R.id.content_frame_second);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/** We need to call super to get onActivityResult on calling fragment. */
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void setupMenuAppearance(int id) {
		final int size = menuContainer.getChildCount();
		for (int i = 0; i < size; i++) {
			final ViewGroup child = (ViewGroup) menuContainer.getChildAt(i);
			final boolean shouldBeSelected = id == child.getId();
			child.setSelected(shouldBeSelected);

			final ImageView icon = (ImageView) child.getChildAt(0);
			if (shouldBeSelected) {
				icon.setColorFilter(selectedTabColor);
			} else {
				icon.clearColorFilter();
			}
		}
	}

	private void loadCoreData() {
		conferenceManager.createSpeakersRepository();

		onMainMenuClick(menuScheduleView);

		if (TextUtils.isEmpty(fromNotificationSlotId)) {
			initIncomingSlotId();
		}

		final boolean fromNotification = !TextUtils.isEmpty(fromNotificationSlotId);
		if (fromNotification) {
			loadTalkFromNotification();
		}
	}

	private void initIncomingSlotId() {
		final Intent intent = getIntent();
		if (intent != null && intent.hasExtra(NotificationsManager.EXTRA_TALK_ID)) {
			fromNotificationSlotId = intent.getStringExtra(
					NotificationsManager.EXTRA_TALK_ID);
		}
	}

	private void loadTalkFromNotification() {
		final List<SlotApiModel> items = slotsDataManager.getLastTalks();
		final Optional<SlotApiModel> optModel = Stream.of(items)
				.filter(new SlotApiModel.SameModelPredicate(fromNotificationSlotId))
				.findFirst();

		if (optModel.isPresent()) {
			final Fragment fr = getSupportFragmentManager()
					.findFragmentByTag(TAG_CONTENT_FRAGMENT);
			if (fr == null) {
				navigator.openTalkDetails(this, optModel.get(), true);
			} else {
				handleMenuClick(R.id.menu_schedule);
				navigator.openTalkDetails(this, optModel.get(), fr, true);
			}
		} else {
			infoUtil.showToast(R.string.no_talk_foud);
		}
	}

	public void replaceFragmentInGivenContainer(Fragment fragment, boolean addToBackStack, int container) {
		replaceFragmentInGivenContainer(fragment, addToBackStack, FragmentTransaction.TRANSIT_NONE, container);
	}

	public void replaceFragmentInGivenContainer(
			Fragment fragment, boolean addToBackStack, int fragmentTransition, int container) {
		final FragmentManager fm = getSupportFragmentManager();
		final FragmentTransaction ft = fm.beginTransaction();
		ft.setTransition(fragmentTransition);
		ft.replace(container, fragment, container == R.id.content_frame
				? TAG_CONTENT_FRAGMENT : TAG_CONTENT_FRAGMENT_SECOND);
		if (addToBackStack) {
			ft.addToBackStack(null);
		}
		ft.commitAllowingStateLoss();
	}

	private void setupToolbar() {
		setSupportActionBar(toolbar);
		final TextView title = (TextView) toolbar.findViewById(R.id.toolbarTitle);
		fontUtils.applyTypeface(title, FontUtils.Font.REGULAR);
		final Optional<RealmConference> conference = conferenceManager.getActiveConference();
		if (conference.isPresent()) {
			title.setText(conference.get().getCountry());
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		getSupportActionBar().setTitle("");
	}
}
