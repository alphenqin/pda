package com.qs.qs5502demo.inbound;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.AgvApiService;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.AgvResponse;
import com.qs.qs5502demo.model.AvailableBin;
import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;
import com.qs.qs5502demo.util.ScanHelper;

public class InboundActivity extends Activity {

    private static final String DEFAULT_SWAP_STATION = "WAREHOUSE_SWAP_1";

    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private View layoutScanPallet;
    private View layoutManualBin;
    private Button btnScanPallet;
    private Button btnPickBin;
    private Button btnBindValve;
    private Button btnCallInbound;
    private Button btnBack;
    
    private WmsApiService wmsApiService;
    private AgvApiService agvApiService;
    private ScanHelper scanHelper;
    
    private String palletNo;
    private String binCode;
    private String swapStation;
    private String palletType;
    private String matCode;  // 阀门物料编码
    private boolean isValveBound = false;  // 阀门是否已绑定
    private boolean isPalletScanEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_inbound);
        
        wmsApiService = new WmsApiService(this);
        agvApiService = new AgvApiService();
        scanHelper = new ScanHelper(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        layoutScanPallet = findViewById(R.id.layoutScanPallet);
        layoutManualBin = findViewById(R.id.layoutManualBin);
        btnScanPallet = (Button) findViewById(R.id.btnScanPallet);
        btnPickBin = (Button) findViewById(R.id.btnPickBin);
        btnBindValve = (Button) findViewById(R.id.btnBindValve);
        btnCallInbound = (Button) findViewById(R.id.btnCallInbound);
        btnBack = (Button) findViewById(R.id.btnBack);
        
        isPalletScanEnabled = PreferenceUtil.getWmsPalletScanEnabled(this);
        togglePalletEntryMode(isPalletScanEnabled);

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

        btnPickBin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchAvailableBin();
            }
        });
        
        btnBindValve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (palletNo == null || palletNo.isEmpty()) {
                    String msg = isPalletScanEnabled ? "请先完成托盘扫码" : "请先录入库位号";
                    Toast.makeText(InboundActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转到阀门绑定页面
                Intent intent = new Intent(InboundActivity.this, BindValveActivity.class);
                intent.putExtra("palletNo", palletNo);
                intent.putExtra("binCode", binCode);
                intent.putExtra("swapStation", swapStation);
                intent.putExtra("palletType", palletType);
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
        // 提示信息：说明扫描键位置
        Toast.makeText(this, "请按设备侧面的扫描键（或直接扫描条码）", Toast.LENGTH_LONG).show();
        
        // 启动扫描监听
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
        // 在后台线程执行网络请求
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 调用API获取托盘信息
                    Pallet pallet = wmsApiService.scanPallet(barcode, InboundActivity.this);
                    
                    // 切换到主线程更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (pallet != null) {
                                palletNo = pallet.getPalletNo();
                                binCode = pallet.getBinCode();
                                swapStation = pallet.getSwapStation();
                                palletType = pallet.getPalletType();
                                
                                tvPalletNo.setText(palletNo);
                                tvLocationCode.setText(binCode);
                                
                                updateStatus(true);
                                Toast.makeText(InboundActivity.this, "扫码成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(InboundActivity.this, "扫码失败，未找到托盘信息", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "扫码失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 呼叫入库
     */
    private void callInbound() {
        if (palletNo == null || palletNo.isEmpty()) {
            String msg = isPalletScanEnabled ? "请先完成托盘扫码" : "请先录入库位号";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValveBound) {
            Toast.makeText(this, "请先完成阀门绑定", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫入库")
            .setMessage("托盘号：" + palletNo + "\n库位号：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallInbound();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void togglePalletEntryMode(boolean enableScan) {
        if (enableScan) {
            layoutScanPallet.setVisibility(View.VISIBLE);
            layoutManualBin.setVisibility(View.GONE);
        } else {
            layoutScanPallet.setVisibility(View.GONE);
            layoutManualBin.setVisibility(View.VISIBLE);
        }
    }

    private void fetchAvailableBin() {
        Toast.makeText(this, "正在获取可用库位...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AvailableBin availableBin = wmsApiService.getAvailableBin(InboundActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (availableBin == null || availableBin.getBinCode() == null
                                || availableBin.getBinCode().isEmpty()) {
                                Toast.makeText(InboundActivity.this, "未获取到可用库位", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            binCode = availableBin.getBinCode();
                            palletNo = binCode;
                            swapStation = DEFAULT_SWAP_STATION;
                            tvPalletNo.setText(palletNo);
                            tvLocationCode.setText(binCode);
                            updateStatus(true);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "获取库位失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
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
                    // 生成任务编号
                    String outID = DateUtil.generateTaskNo("R");
                    
                    // 调用AGV接口创建入库任务
                    AgvResponse response = agvApiService.callInbound(
                        swapStation, binCode, matCode, outID, InboundActivity.this);
                    
                    // 更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null && response.isSuccess()) {
                                updateStatus(true);
                                Toast.makeText(InboundActivity.this, 
                                    "呼叫入库成功，任务号：" + outID, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                String msg = response != null ? response.getMessage() : "呼叫入库失败";
                                Toast.makeText(InboundActivity.this, msg, Toast.LENGTH_SHORT).show();
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
            // 阀门绑定成功，获取物料编码
            if (data != null) {
                matCode = data.getStringExtra("matCode");
            }
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

    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = PreferenceUtil.getWmsPalletScanEnabled(this);
        if (enabled != isPalletScanEnabled) {
            isPalletScanEnabled = enabled;
            togglePalletEntryMode(isPalletScanEnabled);
        }
    }
}

