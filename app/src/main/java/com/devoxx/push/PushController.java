package com.devoxx.push;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.crashlytics.android.Crashlytics;
import com.devoxx.BuildConfig;
import com.devoxx.R;
import com.devoxx.utils.Logger;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.androidannotations.annotations.res.StringRes;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EBean
public class PushController {

	private static final String PUSH_SETTINGS = "PushController.PUSH_SETTINGS";
	private static final String PUSH_SETTINGS_KEY = "PushController.PUSH_SETTINGS_KEY";

	@RootContext protected Context context;
	@StringRes(R.string.gcm_defaultSenderId) protected String senderId;

	private AmazonSNSClient client = new AmazonSNSClient(new AWSCredentials() {
		@Override public String getAWSAccessKeyId() {
			return BuildConfig.AWS_KEY;
		}

		@Override public String getAWSSecretKey() {
			return BuildConfig.AWS_SECRET;
		}
	});

	@Background
	public void uploadToken() {
		final String token = getToken();
		if (token != null) {
			try {
				registerWithSNS(token);
			} catch (AmazonClientException exc) {
				Crashlytics.logException(exc);
			}
		}
	}

	@Nullable
	private String getToken() {
		try {
			final InstanceID instanceID = InstanceID.getInstance(context);
			return instanceID.getToken(senderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
		} catch (IOException e) {
			Crashlytics.logException(e);
			return null;
		}
	}

	private void registerWithSNS(String token) {
		client.setEndpoint(BuildConfig.AWS_ENDPOINT);

		String endpointArn = retrieveEndpointArn();

		boolean updateNeeded = false;
		boolean createNeeded = (null == endpointArn);

		if (createNeeded) {
			// No platform endpoint ARN is stored; need to call createEndpoint.
			endpointArn = createEndpoint(BuildConfig.AWS_APP_ARN, token);
			createNeeded = false;
		}

		// Look up the platform endpoint and make sure the data in it is current, even if
		// it was just created.
		try {
			final GetEndpointAttributesRequest geaReq = new GetEndpointAttributesRequest().withEndpointArn(endpointArn);
			final GetEndpointAttributesResult geaRes = client.getEndpointAttributes(geaReq);

			updateNeeded = !geaRes.getAttributes().get("Token").equals(token)
					|| !geaRes.getAttributes().get("Enabled").equalsIgnoreCase("true");

		} catch (Resources.NotFoundException nfe) {
			// We had a stored ARN, but the platform endpoint associated with it
			// disappeared. Recreate it.
			createNeeded = true;
		}

		if (createNeeded) {
			createEndpoint(BuildConfig.AWS_APP_ARN, token);
		}

		if (updateNeeded) {
			// The platform endpoint is out of sync with the current data;
			// update the token and enable it.
			Logger.l("Updating platform endpoint " + endpointArn);

			final Map<String, String> attribs = new HashMap<>();
			attribs.put("Token", token);
			attribs.put("Enabled", "true");

			final SetEndpointAttributesRequest saeReq =
					new SetEndpointAttributesRequest()
							.withEndpointArn(endpointArn)
							.withAttributes(attribs);

			client.setEndpointAttributes(saeReq);
		}

		final ListTopicsResult listTopicsResult = client.listTopics();
		Logger.l("ListTopicsResult: " + listTopicsResult.toString());

		final SubscribeResult subscribeResult = client.subscribe(
				BuildConfig.AWS_TOPIC_ARN, "application", endpointArn);

		Logger.l(subscribeResult.toString());
	}

	@NonNull
	private String createEndpoint(String applicationArn, String token) throws InvalidParameterException {
		String endpointArn;
		try {
			Logger.l("Creating platform endpoint with token " + token);

			final CreatePlatformEndpointRequest cpeReq = new CreatePlatformEndpointRequest()
					.withPlatformApplicationArn(applicationArn).withToken(token);

			final CreatePlatformEndpointResult cpeRes = client.createPlatformEndpoint(cpeReq);

			endpointArn = cpeRes.getEndpointArn();
		} catch (InvalidParameterException ipe) {
			final String message = ipe.getMessage();
			Logger.l("Exception message: " + message);

			Pattern p = Pattern
					.compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " +
							"with the same token.*");
			final Matcher m = p.matcher(message);
			if (m.matches()) {
				// The platform endpoint already exists for this token, but with
				// additional custom data that
				// createEndpoint doesn't want to overwrite. Just use the
				// existing platform endpoint.
				endpointArn = m.group(1);
			} else {
				// Rethrow the exception, the input is actually bad.
				throw ipe;
			}
		}
		storeEndpointArn(endpointArn);
		return endpointArn;
	}

	private String retrieveEndpointArn() {
		final SharedPreferences sharedPreferences =
				context.getSharedPreferences(PUSH_SETTINGS, Context.MODE_PRIVATE);
		return sharedPreferences.getString(PUSH_SETTINGS_KEY, null);
	}

	private void storeEndpointArn(String endpointArn) {
		final SharedPreferences.Editor sharedPreferencesEditor =
				context.getSharedPreferences(PUSH_SETTINGS, Context.MODE_PRIVATE).edit();
		sharedPreferencesEditor.putString(PUSH_SETTINGS_KEY, endpointArn).apply();
	}
}