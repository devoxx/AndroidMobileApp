package com.devoxx.devoxx_pl.data;

import com.devoxx.data.register.BaseExtractor;
import com.devoxx.devoxx_pl.connection.model.DevoxxPlUserModel;

public class DevoxxPlExtractor extends BaseExtractor {

	private final DevoxxPlUserModel model;

	public DevoxxPlExtractor(DevoxxPlUserModel model) {
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
