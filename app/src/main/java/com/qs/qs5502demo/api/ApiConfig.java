package com.qs.qs5502demo.api;

import android.content.Context;
import com.qs.qs5502demo.util.PreferenceUtil;

/**
 * API配置类
 * PDA 只连接 WMS，AGV 调度系统由 WMS 后端转发。
 */
public class ApiConfig {
    
    // 默认服务器地址
    // WMS接口基础地址（10.0.2.2是Android模拟器访问开发电脑localhost的特殊地址）
    // 本地开发环境
//    private static final String DEFAULT_WMS_BASE_URL = "http://10.0.2.2:8080/api";
    // 部署环境
    private static final String DEFAULT_WMS_BASE_URL = "http://192.168.2.4:8080/api";
    
    private static Context appContext;
    
    /**
     * 初始化配置（在Application或主Activity中调用）
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }
    
    /**
     * 获取WMS服务器地址
     * 优先从SharedPreferences读取，如果没有则返回默认值
     */
    public static String getWmsBaseUrl() {
        if (appContext != null) {
            String url = PreferenceUtil.getWmsBaseUrl(appContext);
            if (url != null && !url.isEmpty()) {
                return url;
            }
        }
        return DEFAULT_WMS_BASE_URL;
    }
    
    /**
     * 设置WMS服务器地址
     */
    public static void setWmsBaseUrl(Context context, String url) {
        PreferenceUtil.saveWmsBaseUrl(context, url);
        if (appContext == null) {
            appContext = context.getApplicationContext();
        }
    }
    
    /**
     * 获取默认WMS地址（用于配置界面显示）
     */
    public static String getDefaultWmsBaseUrl() {
        return DEFAULT_WMS_BASE_URL;
    }
    
}

