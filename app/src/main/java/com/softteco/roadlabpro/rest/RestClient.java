package com.softteco.roadlabpro.rest;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import com.softteco.roadlabpro.util.Constants;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import okio.Buffer;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Retrofit;

public class RestClient {

    private static final String TAG = RestClient.class.getSimpleName();

    private static RestClient restClient;
    private Retrofit retrofit;

    public static RestClient getInstance() {
        if (restClient == null) {
            restClient = new RestClient();
        }
        return restClient;
    }

    public ApiService getApiService() {
        return getRetrofitInstance().create(ApiService.class);
    }

    private Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                        public Date deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
                            return new Date(json.getAsJsonPrimitive().getAsLong());
                        }
                    })
                    .setPrettyPrinting()
                    .create();

            retrofit = new Retrofit.Builder()
                    .client(buildHttpClient())
                    .baseUrl(Constants.GOOGLE_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    private class RequestInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            try {
                Log.i(TAG, "request is: \n" + request.toString());
                Log.i(TAG, "request headers are: \n" + request.headers().toString());
                Buffer buffer = new Buffer();
                if (request.body() != null) {
                    request.body().writeTo(buffer);
                }
                String bodyStr = buffer.readUtf8();
                Log.i(TAG, "REQUEST body is: \n" + bodyStr);
                response = chain.proceed(request);
                String responseBodyString = "";
                MediaType type = null;
                if (response.body() != null) {
                    type = response.body().contentType();
                    responseBodyString = response.body().string();
                }
                response = response.newBuilder().body(ResponseBody.create(responseBodyString, type)).build();
                Log.i(TAG, "RESPONSE body is \n" + responseBodyString);
                return response;
            } catch (Exception e) {
                Log.e(TAG, "RequestInterceptor: intercept", e);
            }
            return response;
        }
    }

    // Previously this built an OkHttpClient with a trust-all X509TrustManager and a
    // hostname verifier that accepted every host - i.e. no TLS validation at all,
    // a man-in-the-middle exposure on every request this app makes. It now relies
    // on the platform's default trust store and hostname verification instead.
    private OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new RequestInterceptor())
                .build();
    }
}
