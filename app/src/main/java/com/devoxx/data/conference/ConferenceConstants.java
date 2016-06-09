package com.devoxx.data.conference;

import com.annimon.stream.Optional;

public final class ConferenceConstants {
	private static final String DEVOXX_PL = "devoxxpl";
	private static final String DEVOXX_UK = "devoxxuk";
	private static final String DEVOXX_US = "devoxxus";
	private static final String DEVOXX_MA = "devoxxma";
	private static final String DEVOXX_FR = "devoxxfr";
	private static final String DEVOXX_BE_1 = "dv15";
	private static final String DEVOXX_BE_2 = "dv16";
	private static final String DEVOXX_BE_3 = "devoxxbe";

	public static boolean isPoland(ConferenceManager conferenceManager) {
		final Optional<String> opt = conferenceManager.getActiveConferenceId();
		return opt.isPresent() && opt.get().toLowerCase().contains(DEVOXX_PL);
	}
}
