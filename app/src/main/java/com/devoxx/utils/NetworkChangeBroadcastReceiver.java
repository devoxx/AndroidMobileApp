package com.devoxx.utils;

import com.devoxx.connection.Connection;
import com.devoxx.connection.Connection_;
import com.devoxx.push.PushController;
import com.devoxx.push.PushController_;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NetworkChangeBroadcastReceiver extends BroadcastReceiver {
	@Override public void onReceive(Context context, Intent intent) {
		final Connection connection = Connection_.getInstance_(context);
		if (connection.isOnline()) {
			final PushController pushController = PushController_.getInstance_(context);
			pushController.uploadToken();
		}
	}
}
