package com.devoxx.devoxx_pl.connection.model;

import java.io.Serializable;

public class DevoxxPlUserModel implements Serializable {
	public String userId;
	public String email;
	public String firstName;
	public String lastName;
	public String registrationStatus;
	public String appearances;

	@Override public String toString() {
		return "DevoxxPlUserModel{" +
				"userId='" + userId + '\'' +
				", email='" + email + '\'' +
				", firstName='" + firstName + '\'' +
				", lastName='" + lastName + '\'' +
				", registrationStatus='" + registrationStatus + '\'' +
				", appearances='" + appearances + '\'' +
				'}';
	}
}
