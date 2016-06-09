package com.devoxx.data.register;

import android.support.v4.util.Pair;

public class BaseExtractor {

	private static final String KEY_USER_ID = "userId";
	private static final String KEY_USER_NAME = "userName";
	private static final String KEY_USER_SURNAME = "userSurname";
	private static final String KEY_USER_JOB = "userJob";
	private static final String KEY_USER_COMPANY = "userCompany";

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
		return new Pair<>(KEY_USER_NAME, userName());
	}

	public final Pair<String, String> getUserSurname() {
		return new Pair<>(KEY_USER_SURNAME, userSurname());
	}

	public final Pair<String, String> getUserCompany() {
		return new Pair<>(KEY_USER_COMPANY, userCompany());
	}

	public final Pair<String, String> getUserJob() {
		return new Pair<>(KEY_USER_JOB, userJob());
	}

	public final Pair<String, String> getUserId() {
		return new Pair<>(KEY_USER_ID, userId());
	}
}
