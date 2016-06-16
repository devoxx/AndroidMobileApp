package com.devoxx.devoxx_pl.connection;

import com.devoxx.BuildConfig;

import org.androidannotations.annotations.EBean;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@EBean
public class DevoxxPlConnection {

	private DevoxxPlApi api;

	public void setup() {
		final OkHttpClient.Builder builder = new OkHttpClient.Builder();

		if (BuildConfig.DEBUG) {
			final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
			httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
			builder.addInterceptor(httpLoggingInterceptor);
		}

		final Retrofit retrofit = new Retrofit.Builder()
				.baseUrl("http://reg.devoxx.pl/")
				.client(builder.build())
				.addConverterFactory(GsonConverterFactory.create())
				.build();
		api = retrofit.create(DevoxxPlApi.class);
	}

	public DevoxxPlApi getApi() {
		return api;
	}
}
