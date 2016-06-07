package com.devoxx.android.activity;

import com.devoxx.R;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.user.UserManager;
import com.devoxx.devoxx_pl.connection.model.DevoxxPlUserModel;
import com.devoxx.devoxx_pl.nfc.NfcConnectionActivity;
import com.devoxx.devoxx_pl.nfc.NfcConnectionActivity_;
import com.devoxx.integrations.IntegrationProvider;
import com.devoxx.utils.InfoUtil;
import com.google.android.gms.vision.barcode.Barcode;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import io.scalac.scanner.BarcodeCaptureActivity;

@EActivity(R.layout.activity_register_user)
public class RegisterUserActivity extends BaseActivity {

	private static final int RC_BARCODE_CAPTURE = 1578;

	@Bean
	InfoUtil infoUtil;

	@Bean
	IntegrationProvider integrationProvider;

	@Bean
	ConferenceManager conferenceManager;

	@Bean
	UserManager userManager;

	@ViewById(R.id.registerUserInfo)
	TextView userInfo;

	@ViewById(R.id.registerUserinput)
	EditText codeInput;

	private BaseExtractor infoExtractor = new BaseExtractor();

	private static final int SCAN_NFC_RC = 39;

	@Click(R.id.registerUserViaNfc) void onNfcClick() {
		NfcConnectionActivity_.intent(this).startForResult(SCAN_NFC_RC);
	}

	@Click(R.id.registerUserViaQr) void onScannerClick() {
		final Intent intent = new Intent(this, BarcodeCaptureActivity.class);
		intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
		startActivityForResult(intent, RC_BARCODE_CAPTURE);
	}

	@Click(R.id.registerUserSaveCode) void onSaveClick() {
		final String message;
		final boolean finishScreen;

		final String userId = infoExtractor.getUserId().second;
		final String input = codeInput.getText().toString();
		final String finalCode = validateInput(userId) ?
				userId : validateInput(input) ? input : null;

		if (validateInput(finalCode)) {
			userManager.saveUserCode(finalCode);
			message = getString(R.string.register_success_message);
			finishScreen = true;
			integrationProvider.provideIntegrationController()
					.userRegistered(conferenceManager.getActiveConference()
							.get().getIntegrationId(), finalCode, infoExtractor);
		} else {
			message = getString(R.string.register_failed_message);
			finishScreen = false;
		}

		infoUtil.showToast(message);

		if (finishScreen) {
			finish();
		}
	}

	private boolean validateInput(String input) {
		return !TextUtils.isEmpty(input);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SCAN_NFC_RC) {
			handleNfcScanningResult(resultCode, data);
		} else if (data != null && data.hasExtra(BarcodeCaptureActivity.BarcodeObject)) {
			final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
			fillUserInfo(new DefaultExtractor(barcode.displayValue));
		}
	}

	private void handleNfcScanningResult(int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			final DevoxxPlUserModel userModel = (DevoxxPlUserModel) data.
					getSerializableExtra(NfcConnectionActivity.KEY_RESULT_MODEL);
			fillUserInfo(new DevoxxPlExtractor(userModel));
		}
	}

	private void fillUserInfo(BaseExtractor extractor) {
		infoExtractor = extractor;

		final String userId = infoExtractor.getUserId().second;
		final String userCompany = infoExtractor.getUserCompany().second;
		final String userName = infoExtractor.getUserName().second;
		final String userSurname = infoExtractor.getUserSurname().second;

		userInfo.setVisibility(View.VISIBLE);
		userInfo.setText(String.format("%s\n%s\n%s\n%s", userName, userSurname, userCompany, userId));

		codeInput.setVisibility(View.GONE);
	}

	public static class BaseExtractor {
		private static final String EMPTY = "";

		protected String userName() {
			return EMPTY;
		}

		protected String userSurname() {
			return EMPTY;
		}

		protected String userCompany() {
			return EMPTY;
		}

		protected String userJob() {
			return EMPTY;
		}

		protected String userId() {
			return EMPTY;
		}

		public final Pair<String, String> getUserName() {
			return new Pair<>("userName", userName());
		}

		public final Pair<String, String> getUserSurname() {
			return new Pair<>("userSurname", userSurname());
		}

		public final Pair<String, String> getUserCompany() {
			return new Pair<>("userCompany", userCompany());
		}

		public final Pair<String, String> getUserJob() {
			return new Pair<>("userJob", userJob());
		}

		public final Pair<String, String> getUserId() {
			return new Pair<>("userId", userId());
		}
	}

	public static class DefaultExtractor extends BaseExtractor {
		private String[] dataParts;

		public DefaultExtractor(String data) {
			dataParts = data.split(",");
		}

		@Override protected String userName() {
			return extractIfExists(dataParts, 1);
		}

		@Override protected String userSurname() {
			return extractIfExists(dataParts, 2);
		}

		@Override protected String userCompany() {
			return extractIfExists(dataParts, 3);
		}

		@Override protected String userJob() {
			return extractIfExists(dataParts, 5);
		}

		@Override protected String userId() {
			return extractIfExists(dataParts, 0);
		}

		private String extractIfExists(String[] array, int index) {
			return index >= array.length ? "" : array[index];
		}
	}

	private static class DevoxxPlExtractor extends BaseExtractor {

		private final DevoxxPlUserModel model;

		private DevoxxPlExtractor(DevoxxPlUserModel model) {
			this.model = model;
		}

		@Override protected String userName() {
			return model.firstName;
		}

		@Override protected String userSurname() {
			return model.lastName;
		}

		@Override protected String userId() {
			return model.userId;
		}
	}
}
