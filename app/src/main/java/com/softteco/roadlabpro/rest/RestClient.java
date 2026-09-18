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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
                    .client(getUnsafeOkHttpClient())
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

    public OkHttpClient getUnsafeOkHttpClient() {
        try {
            X509TrustManager trustManager = new MyTrustManager();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(getSslSocketFactory(trustManager), trustManager);
            builder.hostnameVerifier(new NullHostNameVerifier());
            builder.addInterceptor(new RequestInterceptor());
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SSLSocketFactory getSslSocketFactory(X509TrustManager trustManager) {
        SSLContext sslContext = null;
        try {
            // Create an SSLContext that uses our TrustManager
            sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = {trustManager};
            sslContext.init(null, trustManagers, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        if (sslContext != null) {
            return sslContext.getSocketFactory();
        }
        return null;
    }

    private static class MyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    public static class NullHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }
}
