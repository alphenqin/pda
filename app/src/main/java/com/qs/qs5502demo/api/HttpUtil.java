package com.qs.qs5502demo.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * HTTP请求工具类
 */
public class HttpUtil {
    
    private static final String TAG = "HttpUtil";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int TIMEOUT = 10; // 超时时间（秒）
    
    private static OkHttpClient client;
    private static Gson gson;
    
    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
        
        gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }
    
    /**
     * POST请求
     */
    public static String post(String url, String json, Context context) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json");
        
        // 添加Token
        String token = PreferenceUtil.getToken(context);
        if (token != null && !token.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        
        Request request = builder.build();
        
        Log.d(TAG, "POST " + url);
        Log.d(TAG, "Request: " + json);
        
        Response response = client.newCall(request).execute();
        String responseBody = response.body().string();
        
        Log.d(TAG, "Response Code: " + response.code());
        Log.d(TAG, "Response: " + responseBody);
        
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        
        return responseBody;
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * 将JSON字符串转换为对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
    
    /**
     * 将JSON字符串转换为对象（支持泛型）
     */
    public static <T> T fromJson(String json, Type type) {
        return gson.fromJson(json, type);
    }
}
