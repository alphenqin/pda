package com.qs.qs5502demo.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * 扫码工具类，封装QS5502的扫码功能
 */
public class ScanHelper {
    
    private static final String TAG = "ScanHelper";
    
    public interface ScanCallback {
        void onScanResult(String barcode);
    }

    private Context context;
    private ScanBroadcastReceiver receiver;
    private ScanCallback callback;

    public ScanHelper(Context context) {
        this.context = context;
    }

    /**
     * 开始扫码
     */
    public void startScan(ScanCallback callback) {
        this.callback = callback;
        
        // 先停止之前的扫描
        stopScan();
        
        try {
            // 注册广播接收器
            receiver = new ScanBroadcastReceiver();
            IntentFilter intentFilter = new IntentFilter();
            // 添加多种可能的扫描结果广播动作（QS5502常用）
            intentFilter.addAction("com.qs.scancode");
            intentFilter.addAction("com.honeywell.decode.intent.action.ACTION_DECODE");
            intentFilter.addAction("com.honeywell.intent.action.SCAN");
            intentFilter.addAction("com.symbol.datawedge.api.RESULT_ACTION");
            intentFilter.addAction("com.honeywell.scanner.ACTION");
            context.registerReceiver(receiver, intentFilter);
            
            Log.d(TAG, "扫描广播接收器已注册");
            
            // 尝试多种方式触发扫描
            // 方式1: QS5502 专用触发广播
            try {
                Intent intent1 = new Intent();
                intent1.setAction("hbyapi.intent.key_scan_down");
                context.sendBroadcast(intent1);
                Log.d(TAG, "已发送扫描触发广播: hbyapi.intent.key_scan_down");
            } catch (Exception e) {
                Log.e(TAG, "发送扫描触发广播失败", e);
            }
            
            // 方式2: Honeywell 通用触发
            try {
                Intent intent2 = new Intent();
                intent2.setAction("com.honeywell.decode.intent.action.START_SCAN");
                context.sendBroadcast(intent2);
                Log.d(TAG, "已发送扫描触发广播: com.honeywell.decode.intent.action.START_SCAN");
            } catch (Exception e) {
                Log.e(TAG, "发送Honeywell扫描触发广播失败", e);
            }
            
            // 方式3: 通用扫描触发
            try {
                Intent intent3 = new Intent();
                intent3.setAction("com.honeywell.intent.action.START_SCAN");
                context.sendBroadcast(intent3);
                Log.d(TAG, "已发送扫描触发广播: com.honeywell.intent.action.START_SCAN");
            } catch (Exception e) {
                Log.e(TAG, "发送通用扫描触发广播失败", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "启动扫描失败", e);
        }
    }

    /**
     * 停止扫码（注销广播接收器）
     */
    public void stopScan() {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
                Log.d(TAG, "扫描广播接收器已注销");
            } catch (Exception e) {
                Log.e(TAG, "注销扫描广播接收器失败", e);
            }
            receiver = null;
        }
        callback = null;
    }

    /**
     * 扫码广播接收器
     */
    private class ScanBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "收到扫描广播: " + action);
            
            String barcode = null;
            
            // 尝试多种方式获取扫描数据
            if ("com.qs.scancode".equals(action)) {
                // QS5502 专用格式
                if (intent.getExtras() != null) {
                    barcode = intent.getExtras().getString("data");
                }
            } else if ("com.honeywell.decode.intent.action.ACTION_DECODE".equals(action)) {
                // Honeywell 格式
                if (intent.getExtras() != null) {
                    barcode = intent.getExtras().getString("data");
                    if (barcode == null) {
                        barcode = intent.getExtras().getString("decode_data");
                    }
                }
            } else if ("com.honeywell.intent.action.SCAN".equals(action)) {
                // Honeywell 扫描格式
                if (intent.getExtras() != null) {
                    barcode = intent.getExtras().getString("data");
                    if (barcode == null) {
                        barcode = intent.getExtras().getString("decode_data");
                    }
                    if (barcode == null) {
                        barcode = intent.getExtras().getString("barcode_string");
                    }
                }
            } else if ("com.symbol.datawedge.api.RESULT_ACTION".equals(action)) {
                // Symbol/DataWedge 格式
                if (intent.getExtras() != null) {
                    barcode = intent.getExtras().getString("com.symbol.datawedge.data_string");
                }
            } else if ("com.honeywell.scanner.ACTION".equals(action)) {
                // Honeywell 扫描器格式
                if (intent.getExtras() != null) {
                    barcode = intent.getExtras().getString("data");
                }
            }
            
            if (barcode != null && !barcode.isEmpty()) {
                Log.d(TAG, "扫描成功，条码: " + barcode);
                if (callback != null) {
                    callback.onScanResult(barcode);
                }
                // 扫码完成后注销接收器
                stopScan();
            } else {
                Log.w(TAG, "扫描数据为空");
            }
        }
    }
}
