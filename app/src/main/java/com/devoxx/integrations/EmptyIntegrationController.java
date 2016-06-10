package com.devoxx.integrations;

import com.devoxx.data.register.BaseExtractor;

import org.androidannotations.annotations.EBean;

import android.app.Activity;
import android.view.Menu;

@EBean
public class EmptyIntegrationController implements IntegrationController {
	@Override public void register() {
		// Nothing.
	}

	@Override public void downloadNeededData(String confCode) {
		// Nothing.
	}

	@Override public void handleAppResume(String confCode, Activity activity) {
		// Nothing.
	}

	@Override public void talkVoted(String confCode, Activity activity) {
		// Nothing.
	}

	@Override
	public void userRegistered(String confId, String finalCode,
														 BaseExtractor infoExtractor) {
		// Nothing.
	}

	@Override public void setupIntegrationToolbarMenuItem(Menu menu) {
		// Nothing.
	}

	@Override public void handleToolbarIconClick(String confId, Activity activity) {
		// Nothing.
	}

	@Override public void init() {
		// Nothing.
	}

	@Override public void clear() {
		// Nothing.
	}
}
