package com.devoxx.connection;

import com.devoxx.connection.model.ConferenceSingleApiModel;
import com.devoxx.connection.model.SpeakerFullApiModel;
import com.devoxx.connection.model.SpeakerShortApiModel;
import com.devoxx.connection.model.SpecificScheduleApiModel;
import com.devoxx.connection.model.TalkFullApiModel;
import com.devoxx.connection.model.TracksApiModel;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface DevoxxApi {

	@GET("/api/conferences/{confCode}/speakers") Call<List<SpeakerShortApiModel>> speakers(
			@Path("confCode") String confCode
	);

	@GET("/api/conferences/{confCode}/schedules/{dayOfWeek}")
	Call<SpecificScheduleApiModel> specificSchedule(
			@Path("confCode") String confCode,
			@Path("dayOfWeek") String dayOfWeek
	);

	@GET("/api/conferences/{confCode}/speakers/{uuid}") Call<ResponseBody> speaker(
			@Path("confCode") String confCode,
			@Path("uuid") String uuid
	);

	@GET Call<SpeakerFullApiModel> speaker(
			@Url String url
	);

	@GET("/api/conferences/{confCode}/tracks") Call<TracksApiModel> tracks(
			@Path("confCode") String confCode
	);

	@GET("/api/conferences/{confCode}") Call<ConferenceSingleApiModel> conference(
			@Path("confCode") String confCode
	);

	@GET("/api/conferences/{confCode}/talks/{talkId}") Call<TalkFullApiModel> talk(
			@Path("confCode") String confCode,
			@Path("talkId") String talkId
	);
}
