package com.adobe.sign.adobelibrary.utils;

import okhttp3.*;
import okio.Buffer;

import java.io.IOException;

/**
 * Created by Calvin on 4/18/2017
 * for the AdobeLibrary project.
 */
public class RESTfulUtils {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType xForm = MediaType.parse("application/x-www-form-urlencoded");

    private static OkHttpClient client = new OkHttpClient();

    public static String post(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String getAgreementsResponse(String apiAddress) throws IOException {
        Request request = new Request.Builder()
                .addHeader("Access-Token", AuthorizationUtils.getToken().getAccess_token())
                .url(apiAddress)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String getAuthResponse(String authURL, String json) throws IOException {
        RequestBody body = RequestBody.create(xForm, json);
        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .url(authURL)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    public static String requestBodyToString(Request request) {
        try {
            Request copy = request.newBuilder().build();
            Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "Error converting RequestBody to String";
        }
    }
}