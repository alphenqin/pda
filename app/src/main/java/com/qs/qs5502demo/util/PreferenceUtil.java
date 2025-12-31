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
    private static final String KEY_WMS_PALLET_SCAN_ENABLED = "wms_pallet_scan_enabled";
    private static final String KEY_DEVICE_CODE = "device_code";
    private static final String KEY_AGV_RANGE = "agv_range";
    private static final String KEY_RETURN_CALL_PALLET_LOCK = "return_call_pallet_lock";
    private static final String KEY_RETURN_VALVE_LOCK = "return_valve_lock";
    private static final String KEY_OUTBOUND_LOCK = "outbound_lock";
    private static final String KEY_OUTBOUND_EMPTY_RETURN_LOCK = "outbound_empty_return_lock";
    
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
     * 保存设备编码
     */
    public static void saveDeviceCode(Context context, String deviceCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_DEVICE_CODE, deviceCode).apply();
    }

    /**
     * 获取设备编码
     */
    public static String getDeviceCode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_DEVICE_CODE, "");
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
     * 保存AGV范围
     */
    public static void saveAgvRange(Context context, String agvRange) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_AGV_RANGE, agvRange).apply();
    }

    /**
     * 获取AGV范围
     */
    public static String getAgvRange(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_AGV_RANGE, null);
    }

    /**
     * 保存是否启用WMS托盘扫码
     */
    public static void saveWmsPalletScanEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_WMS_PALLET_SCAN_ENABLED, enabled).apply();
    }

    /**
     * 获取是否启用WMS托盘扫码（默认启用）
     */
    public static boolean getWmsPalletScanEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_WMS_PALLET_SCAN_ENABLED, true);
    }

    /**
     * 保存回库呼叫托盘锁定状态
     */
    public static void saveReturnCallPalletLock(Context context, boolean locked) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RETURN_CALL_PALLET_LOCK, locked).apply();
    }

    /**
     * 获取回库呼叫托盘锁定状态
     */
    public static boolean getReturnCallPalletLock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_RETURN_CALL_PALLET_LOCK, false);
    }

    /**
     * 保存回库阀门回库锁定状态
     */
    public static void saveReturnValveLock(Context context, boolean locked) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_RETURN_VALVE_LOCK, locked).apply();
    }

    /**
     * 获取回库阀门回库锁定状态
     */
    public static boolean getReturnValveLock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_RETURN_VALVE_LOCK, false);
    }

    /**
     * 保存出库锁定状态
     */
    public static void saveOutboundLock(Context context, boolean locked) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_OUTBOUND_LOCK, locked).apply();
    }

    /**
     * 获取出库锁定状态
     */
    public static boolean getOutboundLock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_OUTBOUND_LOCK, false);
    }

    /**
     * 保存出库空托回库锁定状态
     */
    public static void saveOutboundEmptyReturnLock(Context context, boolean locked) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_OUTBOUND_EMPTY_RETURN_LOCK, locked).apply();
    }

    /**
     * 获取出库空托回库锁定状态
     */
    public static boolean getOutboundEmptyReturnLock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_OUTBOUND_EMPTY_RETURN_LOCK, false);
    }
    
    /**
     * 清除所有保存的数据（退出登录时调用）
     */
    public static void clearAll(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}

