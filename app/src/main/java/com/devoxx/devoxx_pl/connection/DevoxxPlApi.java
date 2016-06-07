package com.devoxx.devoxx_pl.connection;

import com.devoxx.devoxx_pl.connection.model.DevoxxPlUserModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface DevoxxPlApi {
	@GET("api/1.0/person/nfc/{user}") Call<DevoxxPlUserModel> user(@Path("user") String user);
}
