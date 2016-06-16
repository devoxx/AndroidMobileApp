package com.devoxx.data.vote.interfaces;

public interface IOnVoteForTalkListener {
	void onVoteForTalkSucceed();

	void onVoteForTalkFailed();

	void onCantVoteMoreThanOnce();
}
