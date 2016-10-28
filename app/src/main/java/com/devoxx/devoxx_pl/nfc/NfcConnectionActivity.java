package com.devoxx.devoxx_pl.nfc;

import com.crashlytics.android.Crashlytics;
import com.devoxx.R;
import com.devoxx.devoxx_pl.connection.DevoxxPlApi;
import com.devoxx.devoxx_pl.connection.DevoxxPlConnection;
import com.devoxx.devoxx_pl.connection.model.DevoxxPlUserModel;
import com.devoxx.utils.InfoUtil;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.UnknownHostException;

import retrofit2.Response;

@EActivity(R.layout.activity_nfc_connection)
public class NfcConnectionActivity extends AppCompatActivity {

	private static final long RECHECK_NFC_SETTINGS_INTERVAL_MS = 500;
	public static final String KEY_RESULT_MODEL = "key_result_model";

	@ViewById protected ProgressBar loadingBar;
	@ViewById protected TextView loadingLabel;
	@ViewById protected Button enableNfcBtn;

	@Bean DevoxxPlConnection devoxxPlConnection;
	@Bean InfoUtil infoUtil;

	private NfcTagAdapter adapter;
	private Handler handler;
	private Runnable checkNfcSettings = new Runnable() {
		@Override public void run() {
			if (isNfcEnabled()) {
				if (handler != null) {
					handler.removeCallbacks(this);
				}

				setupViewWithNfc();
			} else {
				if (handler != null) {
					handler.postDelayed(this, RECHECK_NFC_SETTINGS_INTERVAL_MS);
				}

				setupViewWithNoNfc();
			}
		}
	};

	@AfterViews protected void init() {
		devoxxPlConnection.setup();
		adapter = new NfcTagAdapter(this);
		handler = new Handler(Looper.getMainLooper());
	}

	private void setupViewWithNoNfc() {
		loadingLabel.setText(R.string.disabled_nfc);
		enableNfcBtn.setVisibility(View.VISIBLE);
	}

	private void setupViewWithNfc() {
		loadingLabel.setText(R.string.nfc_connection_waiting);
		enableNfcBtn.setVisibility(View.GONE);
	}

	@Click(R.id.enableNfcBtn) void onEnableNfcClick() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
			startActivity(intent);
		} else {
			Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
			startActivity(intent);
		}
	}

	@Click(R.id.backBtn) public void goBackToHomeActivity() {
		finish();
	}

	@Override protected void onNewIntent(Intent intent) {
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			showLoading();

			final byte[] rawId = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
			final String nfcTagId = adapter.byteArrayToHexString(rawId);
			handleScannedNfcId(nfcTagId);
		}
	}

	@Background void handleScannedNfcId(String userId) {
		final DevoxxPlApi api = devoxxPlConnection.getApi();

		try {
			final Response<DevoxxPlUserModel> response = api.user(userId).execute();
			if (response.isSuccessful()) {
				goBackToRegisterActivity(response.body());
			} else {
				handleError(getString(R.string.connection_error));
			}
		} catch (IOException e) {
			if (e instanceof UnknownHostException) {
				handleError(getString(R.string.connection_error));
			} else {
				handleError(getString(R.string.nfc_scanning_error));
			}
			Crashlytics.logException(e);
		}

		hideLoading();
	}

	@UiThread void showLoading() {
		loadingBar.setVisibility(View.VISIBLE);
	}

	@UiThread void hideLoading() {
		loadingBar.setVisibility(View.INVISIBLE);
	}

	@UiThread void goBackToRegisterActivity(DevoxxPlUserModel body) {
		adapter.cancelPendingIntent();

		setResult(Activity.RESULT_OK, packModelToIntent(body));
		finish();
	}

	@UiThread void handleError(String msg) {
		infoUtil.showToast(msg);
	}

	@Override public void onPause() {
		super.onPause();
		adapter.disableForegroundDispatch();

		if (handler != null) {
			handler.removeCallbacks(checkNfcSettings);
		}
	}

	@Override public void onResume() {
		super.onResume();
		adapter.enableForegroundDispatch();

		if (handler != null) {
			handler.post(checkNfcSettings);
		}
	}

	private Intent packModelToIntent(DevoxxPlUserModel body) {
		return new Intent().putExtra(KEY_RESULT_MODEL, body);
	}

	private boolean isNfcEnabled() {
		final NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
		final NfcAdapter adapter = manager.getDefaultAdapter();
		return adapter != null && adapter.isEnabled();
	}
}
