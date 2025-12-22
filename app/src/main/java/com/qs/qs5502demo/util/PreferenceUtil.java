package com.qs.qs5502demo.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences工具类，用于保存和读取用户信息、token等
 */
public class PreferenceUtil {
    
    private static final String PREF_NAME = "pda_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_EXPIRE_AT = "expire_at";
    private static final String KEY_WMS_BASE_URL = "wms_base_url";
    private static final String KEY_AGV_BASE_URL = "agv_base_url";
    
    /**
     * 保存Token
     */
    public static void saveToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }
    
    /**
     * 获取Token
     */
    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TOKEN, null);
    }
    
    /**
     * 保存用户名
     */
    public static void saveUserName(Context context, String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USER_NAME, userName).apply();
    }
    
    /**
     * 获取用户名
     */
    public static String getUserName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_NAME, "");
    }
    
    /**
     * 保存过期时间
     */
    public static void saveExpireAt(Context context, String expireAt) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_EXPIRE_AT, expireAt).apply();
    }
    
    /**
     * 保存WMS服务器地址
     */
    public static void saveWmsBaseUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WMS_BASE_URL, url).apply();
    }
    
    /**
     * 获取WMS服务器地址
     */
    public static String getWmsBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WMS_BASE_URL, null);
    }
    
    /**
     * 保存AGV服务器地址
     */
    public static void saveAgvBaseUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AGV_BASE_URL, url).apply();
    }
    
    /**
     * 获取AGV服务器地址
     */
    public static String getAgvBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AGV_BASE_URL, null);
    }
    
    /**
     * 清除所有保存的数据（退出登录时调用）
     */
    public static void clearAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}

