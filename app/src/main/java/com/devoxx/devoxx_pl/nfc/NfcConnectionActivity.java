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
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;

import java.io.IOException;
import java.net.UnknownHostException;

import retrofit2.Response;

@EActivity(R.layout.activity_nfc_connection)
public class NfcConnectionActivity extends AppCompatActivity {

	public static final String KEY_RESULT_MODEL = "key_result_model";

	@ViewById protected ProgressBar loadingBar;

	@Bean DevoxxPlConnection devoxxPlConnection;
	@Bean InfoUtil infoUtil;

	private NfcTagAdapter adapter;

	@AfterViews protected void init() {
		devoxxPlConnection.setup();

		loadingBar.setVisibility(View.VISIBLE);

		adapter = new NfcTagAdapter(this);
	}

	@Click(R.id.backBtn) public void goBackToHomeActivity() {
		finish();
	}

	@Override protected void onNewIntent(Intent intent) {
		loadingBar.setVisibility(View.INVISIBLE);

		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
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
	}

	@UiThread void goBackToRegisterActivity(DevoxxPlUserModel body) {
		adapter.cancelPendingIntent();

		setResult(Activity.RESULT_OK, packModelToIntent(body));
		finish();
	}

	@UiThread void handleError(String msg) {
		infoUtil.showToast(msg);
		loadingBar.setVisibility(View.VISIBLE);
	}

	private Intent packModelToIntent(DevoxxPlUserModel body) {
		final Intent intent = new Intent();
		intent.putExtra(KEY_RESULT_MODEL, body);
		return intent;
	}

	@Override public void onPause() {
		super.onPause();
		adapter.disableForegroundDispatch();
	}

	@Override public void onResume() {
		super.onResume();
		adapter.enableForegroundDispatch();
	}
}
