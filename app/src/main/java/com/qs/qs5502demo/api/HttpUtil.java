package com.qs.qs5502demo.api;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.qs.qs5502demo.LoginActivity;
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
        
        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response.body() != null) {
                responseBody = response.body().string();
            }

            Log.d(TAG, "Response Code: " + response.code());
            Log.d(TAG, "Response: " + responseBody);

            // 检查401状态码（未授权），自动退出登录
            if (response.code() == 401) {
                handleUnauthorized(context);
                throw new IOException("登录已过期，请重新登录");
            }
            Integer bodyCode = extractCode(responseBody);
            if (bodyCode != null && bodyCode == 401) {
                handleUnauthorized(context);
                throw new IOException("登录已过期，请重新登录");
            }

            if (!response.isSuccessful()) {
                String errorMessage = extractErrorMessage(responseBody);
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    throw new IOException(errorMessage);
                }
                throw new IOException("Unexpected code " + response);
            }

            return responseBody;
        }
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

    /**
     * 处理未授权情况，自动退出登录
     */
    private static void handleUnauthorized(Context context) {
        if (context != null) {
            // 清除保存的用户信息
            PreferenceUtil.clearAll(context);

            // 发送广播通知其他组件登录状态已改变
            Intent intent = new Intent("com.qs.qs5502demo.ACTION_LOGOUT");
            context.sendBroadcast(intent);

            // 启动登录Activity
            Intent loginIntent = new Intent(context, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(loginIntent);
        }
    }

    private static String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            JsonElement element = new JsonParser().parse(responseBody);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement msg = obj.get("msg");
                if (msg != null && !msg.isJsonNull()) {
                    return msg.getAsString();
                }
                JsonElement message = obj.get("message");
                if (message != null && !message.isJsonNull()) {
                    return message.getAsString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse error response", e);
        }
        return null;
    }

    private static Integer extractCode(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            JsonElement element = new JsonParser().parse(responseBody);
            if (element != null && element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                JsonElement code = obj.get("code");
                if (code != null && !code.isJsonNull()) {
                    if (code.isJsonPrimitive()) {
                        try {
                            return code.getAsInt();
                        } catch (Exception ignored) {
                            String raw = code.getAsString();
                            if (raw != null && !raw.isEmpty()) {
                                return Integer.parseInt(raw);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse code response", e);
        }
        return null;
    }
}
