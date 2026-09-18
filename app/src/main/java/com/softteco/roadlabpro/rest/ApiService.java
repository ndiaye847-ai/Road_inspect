package com.softteco.roadlabpro.rest;

import com.softteco.roadlabpro.rest.dto.DriveFile;
import com.softteco.roadlabpro.rest.dto.DriveSearchResult;
import com.softteco.roadlabpro.rest.dto.FileSearchItem;
import com.softteco.roadlabpro.rest.dto.GoogleToken;
import com.softteco.roadlabpro.rest.dto.AccountData;
import com.softteco.roadlabpro.util.Constants;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @Headers({
        Constants.USER_AGENT_HEADER + ": " + Constants.USER_AGENT_HEADER_VALUE
    })
    @FormUrlEncoded
    @POST(Constants.GOOGLE_TOKEN_URL)
    Call<GoogleToken> getGoogleApiToken(@Field("code") String code, @Field("client_id") String clientId,
                                        @Field("client_secret") String clientSecret, @Field("redirect_uri") String redirectUri,
                                        @Field("refresh_token") String refreshToken, @Field("grant_type") String grantType);

    @GET(Constants.GOOGLE_API_BASE_URL + "/drive/v2/about")
    Call<AccountData> getUserInfo(@Query("fields") String fields, @Header("Authorization") String authorization);

    @POST(Constants.GOOGLE_API_BASE_URL + "/upload/drive/v2/files")
    Call<DriveFile> uploadGooogleFile(@Query("uploadType") String uploadType, @Query("fields") String fields,
                                      @Header("Content-Type") String contentType,
                                      @Header("Content-Length") long length,
                                      @Header("Authorization") String authorization,
                                      @Body RequestBody data);

    @PUT(Constants.GOOGLE_API_BASE_URL + "/drive/v2/files/{fileId}")
    Call<DriveFile> setFileFolder(@Path("fileId") String fileId, @Query("fields") String fields,
                                  @Header("Authorization") String authorization, @Body RequestBody data);

    @POST(Constants.GOOGLE_API_BASE_URL + "/drive/v2/files")
    Call<DriveFile> createFolder(@Header("Content-Type") String contentType, @Query("fields") String fields,
                                 @Header("Authorization") String authorization, @Body RequestBody data);

    @GET(Constants.GOOGLE_API_BASE_URL + "/drive/v3/files")
    Call<DriveSearchResult> searchFiles(@Header("Authorization") String authorization, @Query("q") String searchQuery);

}
