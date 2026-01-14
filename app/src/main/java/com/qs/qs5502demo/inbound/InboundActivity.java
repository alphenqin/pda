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
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.AvailablePallet;
import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.model.PalletTypeOption;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.TaskLockStatus;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;
import com.qs.qs5502demo.util.ScanHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboundActivity extends Activity {

    private static final String DEFAULT_SWAP_STATION = "WAREHOUSE_SWAP_1";
    private static final long INBOUND_LOCK_POLL_MS = 5000L;

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
    private TextView tvPalletType;
    
    private WmsApiService wmsApiService;
    private ScanHelper scanHelper;
    
    private String palletNo;
    private String binCode;
    private String swapStation;
    private String palletType;
    private String matCode;  // 阀门物料编码
    private boolean isValveBound = false;  // 阀门是否已绑定
    private boolean isPalletScanEnabled = true;
    private final List<PalletTypeOption> palletTypeList = new ArrayList<>();
    private PalletTypeOption selectedPalletType;
    private Handler handler = new Handler();
    private Runnable inboundLockRunnable;
    private long lastLockStatusErrorAt = 0L;
    private boolean inboundLocked = false;
    private boolean callInboundInProgress = false;
    private CharSequence callInboundLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_inbound);
        
        wmsApiService = new WmsApiService(this);
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
        tvPalletType = (TextView) findViewById(R.id.tvPalletType);
        callInboundLabel = btnCallInbound.getText();
        
        isPalletScanEnabled = PreferenceUtil.getWmsPalletScanEnabled(this);
        togglePalletEntryMode(isPalletScanEnabled);

        // 初始状态
        updateStatus(false);
        refreshInboundLockStatus();
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
                handlePickPallet();
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
            Toast.makeText(this, "请先完成样品绑定", Toast.LENGTH_SHORT).show();
            return;
        }

        checkInboundLockAndConfirm();
    }

    private void checkInboundLockAndConfirm() {
        Toast.makeText(this, "正在检查入库状态...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskLockStatus status = wmsApiService.getTaskLockStatus(InboundActivity.this);
                    boolean locked = status != null && status.isInboundLocked();
                    boolean inspectionLocked = status != null && status.isInspectionLocked();
                    boolean inspectionEmptyReturnLocked = status != null && status.isInspectionEmptyReturnLocked();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            applyInboundLock(locked);
                            if (locked) {
                                Toast.makeText(InboundActivity.this, "入库任务执行中，请稍后再试", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (inspectionLocked || inspectionEmptyReturnLocked) {
                                Toast.makeText(InboundActivity.this, "送检任务执行中，请稍后再试", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            showInboundConfirmDialog();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "入库状态检查失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showInboundConfirmDialog() {
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
            if (palletTypeList.isEmpty()) {
                loadPalletTypes(false);
            }
        }
    }

    private void handlePickPallet() {
        if (palletTypeList.isEmpty()) {
            loadPalletTypes(true);
            return;
        }
        showPalletTypeDialog();
    }

    private void fetchAvailablePallet(PalletTypeOption palletTypeOption) {
        if (palletTypeOption == null || palletTypeOption.getId() == null) {
            Toast.makeText(this, "托盘类型无效", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在获取可用托盘...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AvailablePallet availablePallet = wmsApiService.getAvailablePallet(
                        palletTypeOption.getId(), InboundActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (availablePallet == null || availablePallet.getPalletNo() == null
                                || availablePallet.getPalletNo().isEmpty()) {
                                Toast.makeText(InboundActivity.this, "未获取到可用托盘", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            palletNo = availablePallet.getPalletNo();
                            binCode = availablePallet.getBinCode();
                            swapStation = DEFAULT_SWAP_STATION;
                            palletType = palletTypeOption.getTypeCode();
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
                            Toast.makeText(InboundActivity.this, "选取托盘失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void loadPalletTypes(boolean showDialog) {
        Toast.makeText(this, "正在获取托盘类型...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<PalletTypeOption> types = wmsApiService.listPalletTypes(InboundActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            palletTypeList.clear();
                            if (types != null) {
                                palletTypeList.addAll(types);
                            }
                            if (palletTypeList.isEmpty()) {
                                Toast.makeText(InboundActivity.this, "未获取到托盘类型", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (selectedPalletType != null) {
                                boolean stillExists = false;
                                for (PalletTypeOption option : palletTypeList) {
                                    if (option.getId() != null && option.getId().equals(selectedPalletType.getId())) {
                                        stillExists = true;
                                        break;
                                    }
                                }
                                if (!stillExists) {
                                    updateSelectedPalletType(null);
                                }
                            }
                            if (showDialog) {
                                showPalletTypeDialog();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "获取托盘类型失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void showPalletTypeDialog() {
        if (palletTypeList.isEmpty()) {
            Toast.makeText(this, "暂无托盘类型可选", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[palletTypeList.size()];
        int selectedIndex = -1;
        for (int i = 0; i < palletTypeList.size(); i++) {
            PalletTypeOption option = palletTypeList.get(i);
            String name = option.getTypeName() != null ? option.getTypeName() : "";
            String code = option.getTypeCode() != null ? option.getTypeCode() : "";
            String label = name.isEmpty() ? code : name + (code.isEmpty() ? "" : " (" + code + ")");
            items[i] = label.isEmpty() ? "未命名" : label;
            if (selectedPalletType != null && option.getId() != null && option.getId().equals(selectedPalletType.getId())) {
                selectedIndex = i;
            }
        }
        new AlertDialog.Builder(this)
            .setTitle("选择托盘类型")
            .setSingleChoiceItems(items, selectedIndex, null)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    AlertDialog alert = (AlertDialog) dialog;
                    int index = alert.getListView().getCheckedItemPosition();
                    if (index >= 0 && index < palletTypeList.size()) {
                        PalletTypeOption option = palletTypeList.get(index);
                        updateSelectedPalletType(option);
                        fetchAvailablePallet(option);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void updateSelectedPalletType(PalletTypeOption option) {
        selectedPalletType = option;
        if (option == null) {
            tvPalletType.setText("未选择");
            return;
        }
        String label = option.getTypeName() != null ? option.getTypeName() : "";
        if (label.isEmpty()) {
            label = option.getTypeCode() != null ? option.getTypeCode() : "";
        }
        tvPalletType.setText(label.isEmpty() ? "未命名" : label);
        palletNo = null;
        binCode = null;
        tvPalletNo.setText("--");
        tvLocationCode.setText("--");
        updateStatus(false);
    }
    
    /**
     * 执行呼叫入库
     */
    private void performCallInbound() {
        if (callInboundInProgress) {
            Toast.makeText(this, "入库任务下发中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        callInboundInProgress = true;
        btnCallInbound.setEnabled(false);
        btnCallInbound.setAlpha(0.4f);
        btnCallInbound.setText(callInboundLabel + "（处理中）");
        // 显示加载提示
        Toast.makeText(this, "正在创建入库任务...", Toast.LENGTH_SHORT).show();
        
        // 在后台线程执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 生成任务编号
                    String outID = DateUtil.generateTaskNo("R");
                    
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_INBOUND);
                    params.put("outID", outID);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(InboundActivity.this));
                    params.put("palletNo", palletNo);
                    params.put("fromBinCode", swapStation);
                    params.put("toBinCode", binCode);
                    if (matCode != null) {
                        params.put("matCode", matCode);
                    }
                    applyAgvRange(params);
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, InboundActivity.this);
                    
                    // 更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callInboundInProgress = false;
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outID;
                                updateStatus(true);
                                Toast.makeText(InboundActivity.this, 
                                    "呼叫入库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(InboundActivity.this, "呼叫入库失败", Toast.LENGTH_SHORT).show();
                            }
                            applyInboundLock(inboundLocked);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callInboundInProgress = false;
                            Toast.makeText(InboundActivity.this, "呼叫入库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            applyInboundLock(inboundLocked);
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

    private void applyAgvRange(Map<String, String> params) {
        String agvRange = PreferenceUtil.getAgvRange(this);
        if (agvRange != null && !agvRange.isEmpty()) {
            params.put("agvRange", agvRange);
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
            Toast.makeText(this, "样品绑定成功", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startInboundLockPolling();
        boolean enabled = PreferenceUtil.getWmsPalletScanEnabled(this);
        if (enabled != isPalletScanEnabled) {
            isPalletScanEnabled = enabled;
            togglePalletEntryMode(isPalletScanEnabled);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopInboundLockPolling();
    }

    private void startInboundLockPolling() {
        if (inboundLockRunnable != null) {
            return;
        }
        inboundLockRunnable = new Runnable() {
            @Override
            public void run() {
                refreshInboundLockStatus();
                handler.postDelayed(this, INBOUND_LOCK_POLL_MS);
            }
        };
        handler.post(inboundLockRunnable);
    }

    private void stopInboundLockPolling() {
        if (inboundLockRunnable != null) {
            handler.removeCallbacks(inboundLockRunnable);
            inboundLockRunnable = null;
        }
    }

    private void refreshInboundLockStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskLockStatus status = wmsApiService.getTaskLockStatus(InboundActivity.this);
                    boolean locked = status != null && status.isInboundLocked();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            applyInboundLock(locked);
                        }
                    });
                } catch (Exception e) {
                    long now = System.currentTimeMillis();
                    if (now - lastLockStatusErrorAt > 30000L) {
                        lastLockStatusErrorAt = now;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(InboundActivity.this, "锁状态刷新失败，请检查网络/服务", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void applyInboundLock(boolean locked) {
        inboundLocked = locked;
        btnCallInbound.setEnabled(!inboundLocked);
        if (inboundLocked) {
            btnCallInbound.setAlpha(0.4f);
            btnCallInbound.setText(callInboundLabel + "（入库锁定）");
        } else {
            btnCallInbound.setAlpha(1.0f);
            btnCallInbound.setText(callInboundLabel);
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

