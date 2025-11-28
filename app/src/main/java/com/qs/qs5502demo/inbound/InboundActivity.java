package com.qs.qs5502demo.inbound;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.ApiClient;
import com.qs.qs5502demo.api.ApiService;
import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.util.ScanHelper;

public class InboundActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnScanPallet;
    private Button btnBindValve;
    private Button btnCallInbound;
    private Button btnBack;
    
    private ApiService apiService;
    private ScanHelper scanHelper;
    
    private String palletNo;
    private String locationCode;
    private boolean isValveBound = false;  // 阀门是否已绑定

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbound);
        
        apiService = new ApiClient();
        scanHelper = new ScanHelper(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnScanPallet = (Button) findViewById(R.id.btnScanPallet);
        btnBindValve = (Button) findViewById(R.id.btnBindValve);
        btnCallInbound = (Button) findViewById(R.id.btnCallInbound);
        btnBack = (Button) findViewById(R.id.btnBack);
        
        // 初始状态
        updateStatus(false);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnScanPallet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scanPallet();
            }
        });
        
        btnBindValve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (palletNo == null || palletNo.isEmpty()) {
                    Toast.makeText(InboundActivity.this, "请先完成托盘扫码", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转到阀门绑定页面
                Intent intent = new Intent(InboundActivity.this, BindValveActivity.class);
                intent.putExtra("palletNo", palletNo);
                intent.putExtra("locationCode", locationCode);
                startActivityForResult(intent, 100);
            }
        });
        
        btnCallInbound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callInbound();
            }
        });
    }
    
    /**
     * 托盘扫码
     */
    private void scanPallet() {
        Toast.makeText(this, "请按扫描键扫描托盘条码", Toast.LENGTH_SHORT).show();
        
        scanHelper.startScan(new ScanHelper.ScanCallback() {
            @Override
            public void onScanResult(String barcode) {
                // 在UI线程更新界面
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleScanResult(barcode);
                    }
                });
            }
        });
    }
    
    /**
     * 处理扫码结果
     */
    private void handleScanResult(String barcode) {
        try {
            // 调用API获取托盘信息
            Pallet pallet = apiService.scanPallet(barcode);
            
            if (pallet != null) {
                palletNo = pallet.getPalletNo();
                locationCode = pallet.getLocationCode();
                
                tvPalletNo.setText(palletNo);
                tvLocationCode.setText(locationCode);
                
                updateStatus(true);
                Toast.makeText(this, "扫码成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "扫码失败，未找到托盘信息", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "扫码失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 呼叫入库
     */
    private void callInbound() {
        if (palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先完成托盘扫码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValveBound) {
            Toast.makeText(this, "请先完成阀门绑定", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫入库")
            .setMessage("托盘号：" + palletNo + "\n库位号：" + locationCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallInbound();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行呼叫入库
     */
    private void performCallInbound() {
        // 显示加载提示
        Toast.makeText(this, "正在创建入库任务...", Toast.LENGTH_SHORT).show();
        
        // 在后台线程执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建入库任务
                    Task task = apiService.createTask(
                        Task.TYPE_INBOUND,
                        palletNo,
                        locationCode,
                        "WAREHOUSE_SWAP_1",
                        "WAREHOUSE_LOCATION_" + locationCode.replace("-", "_"),
                        null
                    );
                    
                    // 更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (task != null && task.getTaskNo() != null) {
                                updateStatus(true);
                                Toast.makeText(InboundActivity.this, 
                                    "呼叫入库成功，任务号：" + task.getTaskNo(), 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(InboundActivity.this, "呼叫入库失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "呼叫入库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 更新状态标识
     */
    private void updateStatus(boolean success) {
        if (success) {
            viewStatus.setBackgroundColor(0xFF4CAF50);  // 绿色
        } else {
            viewStatus.setBackgroundColor(0xFFCCCCCC);  // 灰色
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // 阀门绑定成功
            isValveBound = true;
            updateStatus(true);
            Toast.makeText(this, "阀门绑定成功", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        if (scanHelper != null) {
            scanHelper.stopScan();
        }
        super.onDestroy();
    }
}

