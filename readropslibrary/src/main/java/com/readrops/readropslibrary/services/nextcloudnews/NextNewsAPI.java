package com.readrops.readropslibrary.services.nextcloudnews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.readrops.readropslibrary.services.NextNewsService;
import com.readrops.readropslibrary.services.nextcloudnews.json.NextNewsFeeds;
import com.readrops.readropslibrary.services.nextcloudnews.json.NextNewsFolders;
import com.readrops.readropslibrary.services.nextcloudnews.json.NextNewsItems;
import com.readrops.readropslibrary.utils.HttpManager;

import java.io.IOException;

import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NextNewsAPI {

    private static final String TAG = NextNewsAPI.class.getSimpleName();

    private NextNewsService api;

    public NextNewsAPI() {

    }

    private Retrofit getConfiguredRetrofitInstance(@NonNull HttpManager httpManager) {
        return new Retrofit.Builder()
                .baseUrl(httpManager.getCredentials().getUrl() + NextNewsService.ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpManager.getOkHttpClient())
                .build();
    }

    public SyncResult sync(@NonNull Credentials credentials, @NonNull SyncType syncType, @Nullable SyncData data) throws IOException {
        HttpManager httpManager = new HttpManager(credentials);
        Retrofit retrofit = getConfiguredRetrofitInstance(httpManager);

        api = retrofit.create(NextNewsService.class);

        SyncResult syncResult = new SyncResult();
        switch (syncType) {
            case INITIAL_SYNC:
                initialSync(syncResult);
                break;
            case CLASSIC_SYNC:
                if (data == null)
                    throw new NullPointerException("SyncData can't be null");

                classicSync(syncResult, data);
                break;
        }

        return syncResult;
    }

    private void initialSync(SyncResult syncResult) throws IOException {
        getFeedsAndFolders(syncResult);

        Response<NextNewsItems> itemsResponse = api.getItems(3, false, -1).execute();
        NextNewsItems itemList = itemsResponse.body();

        if (!itemsResponse.isSuccessful())
            syncResult.setError(true);

        if (itemList != null)
            syncResult.setItems(itemList.getItems());
    }

    private void classicSync(SyncResult syncResult, SyncData data) throws IOException {
        putModifiedItems(data, syncResult);
        getFeedsAndFolders(syncResult);

        Response<NextNewsItems> itemsResponse = api.getNewItems(data.getLastModified(), 3).execute();
        NextNewsItems itemList = itemsResponse.body();

        if (!itemsResponse.isSuccessful())
            syncResult.setError(true);

        if (itemList != null)
            syncResult.setItems(itemList.getItems());
    }

    private void getFeedsAndFolders(SyncResult syncResult) throws IOException {
        Response<NextNewsFeeds> feedResponse = api.getFeeds().execute();
        NextNewsFeeds feedList = feedResponse.body();

        if (!feedResponse.isSuccessful())
            syncResult.setError(true);

        Response<NextNewsFolders> folderResponse = api.getFolders().execute();
        NextNewsFolders folderList = folderResponse.body();

        if (!folderResponse.isSuccessful())
            syncResult.setError(true);

        if (folderList != null)
            syncResult.setFolders(folderList.getFolders());

        if (feedList != null)
            syncResult.setFeeds(feedList.getFeeds());

    }

    private void putModifiedItems(SyncData data, SyncResult syncResult) throws IOException {
        Response readItemsResponse = api.setArticlesState(StateType.READ.name(),
                data.getReadItems()).execute();

        Response unreadItemsResponse = api.setArticlesState(StateType.UNREAD.toString(),
                data.getUnreadItems()).execute();

        if (!readItemsResponse.isSuccessful())
            syncResult.setError(true);

        if (!unreadItemsResponse.isSuccessful())
            syncResult.setError(true);
    }

    public enum SyncType {
        INITIAL_SYNC,
        CLASSIC_SYNC
    }

    public enum StateType {
        READ,
        UNREAD,
        STARRED,
        UNSTARRED
    }
}