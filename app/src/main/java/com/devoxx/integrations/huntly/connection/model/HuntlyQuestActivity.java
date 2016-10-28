package com.devoxx.integrations.huntly.connection.model;

import com.devoxx.integrations.huntly.storage.RealmHuntlyQuestActivity;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.io.Serializable;

public class HuntlyQuestActivity implements Serializable {

	public static final String QUEST_ACTIVITY_VOTE = "vote";
	public static final String QUEST_ACTIVITY_FIRST_RUN = "firstAppRun";

	@StringDef({QUEST_ACTIVITY_VOTE, QUEST_ACTIVITY_FIRST_RUN})
	public @interface QuestActivity {
	}

	@QuestActivity
	private String activity;
	private long questId;
	private int singleReward;
	private int performedActivities;
	private int maxActivities;

	@Nullable
	public static HuntlyQuestActivity fromDb(RealmHuntlyQuestActivity quest) {
		if (quest == null) {
			return null;
		}

		final HuntlyQuestActivity result = new HuntlyQuestActivity();
		result.activity = quest.getActivity();
		result.questId = quest.getQuestId();
		result.singleReward = quest.getSingleReward();
		result.performedActivities = quest.getPerformedActions();
		result.maxActivities = quest.getMaxActions();
		return result;
	}

	public String getActivity() {
		return activity;
	}

	public long getQuestId() {
		return questId;
	}

	public int getSingleReward() {
		return singleReward;
	}

	public int getPerformedActivities() {
		return performedActivities;
	}

	public int getMaxActivities() {
		return maxActivities;
	}

	@Override public String toString() {
		return "HuntlyQuestActivity{" +
				"activity='" + activity + '\'' +
				", questId=" + questId +
				", singleReward=" + singleReward +
				", performedActivities=" + performedActivities +
				", maxActivities=" + maxActivities +
				'}';
	}
}
