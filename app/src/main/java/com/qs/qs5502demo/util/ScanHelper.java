package com.qs.qs5502demo.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 扫码工具类，封装QS5502的扫码功能
 */
public class ScanHelper {
    
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
        
        // 注册广播接收器
        receiver = new ScanBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.qs.scancode");
        context.registerReceiver(receiver, intentFilter);
        
        // 发送扫描触发广播
        Intent intentBroadcast = new Intent();
        intentBroadcast.setAction("hbyapi.intent.key_scan_down");
        context.sendBroadcast(intentBroadcast);
    }

    /**
     * 停止扫码（注销广播接收器）
     */
    public void stopScan() {
        if (receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
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
            if ("com.qs.scancode".equals(intent.getAction())) {
                String barcode = intent.getExtras().getString("data");
                if (callback != null && barcode != null) {
                    callback.onScanResult(barcode);
                }
                // 扫码完成后注销接收器
                stopScan();
            }
        }
    }
}

