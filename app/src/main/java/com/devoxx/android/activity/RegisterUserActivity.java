package com.devoxx.android.activity;

import com.devoxx.R;
import com.devoxx.data.conference.ConferenceConstants;
import com.devoxx.data.conference.ConferenceManager;
import com.devoxx.data.register.BaseExtractor;
import com.devoxx.data.register.DefaultExtractor;
import com.devoxx.data.user.UserManager;
import com.devoxx.devoxx_pl.data.DevoxxPlExtractor;
import com.devoxx.devoxx_pl.connection.model.DevoxxPlUserModel;
import com.devoxx.devoxx_pl.nfc.NfcConnectionActivity;
import com.devoxx.devoxx_pl.nfc.NfcConnectionActivity_;
import com.devoxx.integrations.IntegrationProvider;
import com.devoxx.utils.InfoUtil;
import com.google.android.gms.vision.barcode.Barcode;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import android.app.Activity;
import android.content.Intent;
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

	@ViewById(R.id.registerUserViaNfc)
	View nfcScanning;

	private BaseExtractor infoExtractor = new BaseExtractor();

	private static final int SCAN_NFC_RC = 39;

	@AfterViews void afterViews() {
		nfcScanning.setVisibility(ConferenceConstants.
				isPoland(conferenceManager) ? View.VISIBLE : View.GONE);
	}

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

}
