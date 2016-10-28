package com.devoxx.data.schedule.filter.model;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RealmScheduleCustomFilter extends RealmObject {
	@PrimaryKey
	private String key;
	private String label;
	private boolean isActive;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean active) {
		isActive = active;
	}
}
