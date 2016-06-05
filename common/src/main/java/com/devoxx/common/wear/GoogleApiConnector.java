package com.devoxx.common.wear;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by eloudsa on 31/03/16.
 */
public class GoogleApiConnector  {

    private GoogleApiClient mApiClient;
    private Context mContext;


    public GoogleApiConnector(Context context) {
        mContext = context;

        // Connect to Play Services
        mApiClient = new GoogleApiClient.Builder(mContext)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();
    }

    public void disconnect() {
        if ((mApiClient != null) && (mApiClient.isConnected())) {
            mApiClient.disconnect();
        }
    }


    private void connectWearApi(final Runnable onConnectedAction) {

        if (mApiClient.isConnected()) {
            onConnectedAction.run();
        } else {
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            onConnectedAction.run();
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {

                        }
                    }).build();
            mApiClient.connect();
        }
    }


    public void sendMessage(final PutDataMapRequest putDataMapRequest) {

        connectWearApi(new Runnable() {
            @Override
            public void run() {
                Wearable.DataApi.putDataItem(mApiClient, putDataMapRequest.asPutDataRequest());
            }
        });
    }


    public void deleteItems(final String dataPath) {

        connectWearApi(new Runnable() {
            @Override
            public void run() {
                Uri uri = new Uri.Builder()
                        .scheme(PutDataRequest.WEAR_URI_SCHEME)
                        .path(dataPath)
                        .build();

                Wearable.DataApi.deleteDataItems(mApiClient, uri, DataApi.FILTER_LITERAL);
            }
        });
    }

    public void deleteAllItems(final String dataPath) {

        connectWearApi(new Runnable() {
            @Override
            public void run() {
                Uri uri = new Uri.Builder()
                        .scheme(PutDataRequest.WEAR_URI_SCHEME)
                        .path(dataPath)
                        .build();

                Wearable.DataApi.deleteDataItems(mApiClient, uri, DataApi.FILTER_PREFIX);
            }
        });
    }





}
