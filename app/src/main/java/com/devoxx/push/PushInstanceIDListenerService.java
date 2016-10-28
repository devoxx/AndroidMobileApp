package com.devoxx.push;

import com.google.android.gms.iid.InstanceIDListenerService;


public class PushInstanceIDListenerService extends InstanceIDListenerService {
	@Override public void onTokenRefresh() {
		super.onTokenRefresh();
		final PushController pushController = PushController_.getInstance_(getApplicationContext());
		pushController.uploadToken();
	}
}
