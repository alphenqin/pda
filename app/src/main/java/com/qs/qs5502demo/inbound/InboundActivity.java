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
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.TaskLockStatus;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;
import com.qs.qs5502demo.util.ScanHelper;

import java.util.HashMap;
import java.util.Map;

public class InboundActivity extends Activity {

    private static final long INBOUND_LOCK_POLL_MS = 5000L;
    private static final String SMALL_LOAD_BIN_1 = "Z1-装卸点";
    private static final String SMALL_LOAD_BIN_2 = "Z2-装卸点";
    private static final String SMALL_LOAD_BIN_3 = "Z3-装卸点";
    private static final String SMALL_LOAD_BIN_4 = "Z4-装卸点";
    private static final String LARGE_LOAD_BIN = "Z5-装卸点";

    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private TextView tvInboundStation;
    private View viewStatus;
    private View layoutScanPallet;
    private Button btnScanPallet;
    private Button btnBindValve;
    private Button btnSelectInboundStation;
    private Button btnCallInbound;
    private Button btnBack;
    
    private WmsApiService wmsApiService;
    private ScanHelper scanHelper;
    
    private String palletNo;
    private String binCode;
    private String swapStation;
    private String palletType;
    private String matCode;  // 阀门物料编码
    private boolean isValveBound = false;  // 阀门是否已绑定
    private boolean isPalletScanEnabled = true;
    private Handler handler = new Handler();
    private Runnable inboundLockRunnable;
    private long lastLockStatusErrorAt = 0L;
    private boolean inboundLocked = false;
    private boolean callInboundInProgress = false;
    private boolean inboundSubmitted = false;
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
        tvInboundStation = (TextView) findViewById(R.id.tvInboundStation);
        viewStatus = findViewById(R.id.viewStatus);
        layoutScanPallet = findViewById(R.id.layoutScanPallet);
        btnScanPallet = (Button) findViewById(R.id.btnScanPallet);
        btnBindValve = (Button) findViewById(R.id.btnBindValve);
        btnSelectInboundStation = (Button) findViewById(R.id.btnSelectInboundStation);
        btnCallInbound = (Button) findViewById(R.id.btnCallInbound);
        btnBack = (Button) findViewById(R.id.btnBack);
        callInboundLabel = btnCallInbound.getText();
        
        isPalletScanEnabled = PreferenceUtil.getWmsPalletScanEnabled(this);
        togglePalletEntryMode(isPalletScanEnabled);
        updateStepLabels();

        // 初始状态
        updateStatus(false);
        refreshInboundLockStatus();
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptExit();
            }
        });
        
        btnScanPallet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPendingBinding()) {
                    confirmCancelBindingBeforeAction(new Runnable() {
                        @Override
                        public void run() {
                            scanPallet();
                        }
                    }, "重新扫码将取消当前样品绑定，是否继续？");
                    return;
                }
                scanPallet();
            }
        });
        
        btnBindValve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (palletNo == null || palletNo.isEmpty()) {
                    String msg = isPalletScanEnabled ? "请先完成托盘扫码" : "请先选择库外站点";
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

        btnSelectInboundStation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showInboundStationDialog();
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
                                palletType = pallet.getPalletType();
                                if (pallet.getSwapStation() != null && !pallet.getSwapStation().isEmpty()) {
                                    updateInboundStationSelection(pallet.getSwapStation());
                                }
                                inboundSubmitted = false;
                                applyPalletTypeDefaults();
                                
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
            String msg = isPalletScanEnabled ? "请先完成托盘扫码" : "请先选择库外站点";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isValveBound) {
            Toast.makeText(this, "请先完成样品绑定", Toast.LENGTH_SHORT).show();
            return;
        }

        if (swapStation == null || swapStation.isEmpty()) {
            Toast.makeText(this, "请先选择库外站点", Toast.LENGTH_SHORT).show();
            return;
        }

        showInboundConfirmDialog();
    }

    private void showInboundConfirmDialog() {
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫入库")
            .setMessage("托盘号：" + palletNo + "\n库位号：" + binCode + "\n库外站点：" + swapStation)
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
        layoutScanPallet.setVisibility(enableScan ? View.VISIBLE : View.GONE);
    }

    private void fetchAvailablePallet(String outsideSite) {
        if (outsideSite == null || outsideSite.isEmpty()) {
            Toast.makeText(this, "库外站点无效", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在获取可用托盘...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AvailablePallet availablePallet = wmsApiService.getAvailablePallet(outsideSite, InboundActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (availablePallet == null || availablePallet.getPalletNo() == null
                                || availablePallet.getPalletNo().isEmpty()) {
                                palletNo = null;
                                binCode = null;
                                palletType = resolvePalletTypeByOutsideSite(outsideSite);
                                tvPalletNo.setText("--");
                                tvLocationCode.setText("--");
                                updateStatus(false);
                                Toast.makeText(InboundActivity.this, "该库外站点未找到可用托盘", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            palletNo = availablePallet.getPalletNo();
                            binCode = availablePallet.getBinCode();
                            palletType = resolvePalletTypeByOutsideSite(outsideSite);
                            inboundSubmitted = false;
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
                            Toast.makeText(InboundActivity.this, "查询托盘失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            if (!isPalletScanEnabled) {
                                palletNo = null;
                                binCode = null;
                                tvPalletNo.setText("--");
                                tvLocationCode.setText("--");
                                updateStatus(false);
                            }
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
        if (callInboundInProgress) {
            Toast.makeText(this, "入库任务下发中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        callInboundInProgress = true;
        refreshCallInboundButtonState();
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
                                inboundSubmitted = true;
                                resetInboundFormAfterQueue();
                                Toast.makeText(InboundActivity.this, 
                                    "入库任务已加入队列，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(InboundActivity.this, "呼叫入库失败", Toast.LENGTH_SHORT).show();
                            }
                            refreshCallInboundButtonState();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callInboundInProgress = false;
                            Toast.makeText(InboundActivity.this, "呼叫入库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            refreshCallInboundButtonState();
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

    private void resetInboundFormAfterQueue() {
        isValveBound = false;
        matCode = null;
        palletNo = null;
        binCode = null;
        inboundSubmitted = false;
        tvPalletNo.setText("--");
        tvLocationCode.setText("--");
        if (isPalletScanEnabled) {
            palletType = null;
            updateInboundStationSelection(null);
        } else if (swapStation != null && !swapStation.isEmpty()) {
            fetchAvailablePallet(swapStation);
        }
        updateStatus(false);
    }

    private void refreshCallInboundButtonState() {
        btnCallInbound.setEnabled(!callInboundInProgress);
        if (callInboundInProgress) {
            btnCallInbound.setAlpha(0.4f);
            btnCallInbound.setText(callInboundLabel + "（处理中）");
        } else {
            btnCallInbound.setAlpha(1.0f);
            btnCallInbound.setText(callInboundLabel);
        }
    }

    private void showInboundStationDialog() {
        String[] stations = getInboundStationOptions();
        int selectedIndex = -1;
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].equals(swapStation)) {
                selectedIndex = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
            .setTitle("选择库外站点")
            .setSingleChoiceItems(stations, selectedIndex, null)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    AlertDialog alert = (AlertDialog) dialog;
                    int index = alert.getListView().getCheckedItemPosition();
                    if (index >= 0 && index < stations.length) {
                        String station = stations[index];
                        updateInboundStationSelection(station);
                        if (!isPalletScanEnabled) {
                            fetchAvailablePallet(station);
                        } else if (!isInboundStationAllowed(station)) {
                            Toast.makeText(InboundActivity.this, "当前托盘类型不支持该库外站点", Toast.LENGTH_SHORT).show();
                            updateInboundStationSelection(null);
                        }
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void applyPalletTypeDefaults() {
        if (!isPalletScanEnabled) {
            palletType = resolvePalletTypeByOutsideSite(swapStation);
            return;
        }
        String[] stations = getInboundStationOptions();
        if (stations.length == 1) {
            updateInboundStationSelection(stations[0]);
            return;
        }
        if (!isInboundStationAllowed(swapStation)) {
            updateInboundStationSelection(null);
        } else {
            updateInboundStationSelection(swapStation);
        }
    }

    private String[] getInboundStationOptions() {
        if (!isPalletScanEnabled || palletType == null || palletType.isEmpty()) {
            return new String[]{SMALL_LOAD_BIN_1, SMALL_LOAD_BIN_2, SMALL_LOAD_BIN_3, SMALL_LOAD_BIN_4, LARGE_LOAD_BIN};
        }
        if (isSmallPalletType(palletType)) {
            return new String[]{SMALL_LOAD_BIN_1, SMALL_LOAD_BIN_2, SMALL_LOAD_BIN_3, SMALL_LOAD_BIN_4};
        }
        if (isLargePalletType(palletType)) {
            return new String[]{LARGE_LOAD_BIN};
        }
        return new String[0];
    }

    private boolean isInboundStationAllowed(String station) {
        if (station == null || station.isEmpty()) {
            return false;
        }
        String[] stations = getInboundStationOptions();
        for (String candidate : stations) {
            if (station.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void updateInboundStationSelection(String station) {
        swapStation = station;
        tvInboundStation.setText(station == null || station.isEmpty() ? "未选择" : station);
    }

    private String resolvePalletTypeByOutsideSite(String outsideSite) {
        if (outsideSite == null || outsideSite.isEmpty()) {
            return null;
        }
        return LARGE_LOAD_BIN.equals(outsideSite) ? "t2" : "t1";
    }

    private void updateStepLabels() {
        btnSelectInboundStation.setText("1. 选择库外站点");
        btnBindValve.setText(isPalletScanEnabled ? "3. 样品绑定" : "2. 样品绑定");
        btnCallInbound.setText(isPalletScanEnabled ? "4. 呼叫入库" : "3. 呼叫入库");
        callInboundLabel = btnCallInbound.getText();
        if (btnScanPallet != null) {
            btnScanPallet.setText("2. 托盘扫码");
        }
    }

    private boolean isSmallPalletType(String type) {
        return "SMALL".equalsIgnoreCase(type) || "t1".equalsIgnoreCase(type);
    }

    private boolean isLargePalletType(String type) {
        return "LARGE".equalsIgnoreCase(type) || "t2".equalsIgnoreCase(type);
    }

    private void applyAgvRange(Map<String, String> params) {
        String agvRange = PreferenceUtil.getAgvRange(this);
        if (agvRange != null && !agvRange.isEmpty()) {
            params.put("agvRange", agvRange);
        }
    }

    private void attemptExit() {
        if (callInboundInProgress) {
            Toast.makeText(this, "入库任务下发中，请稍后再退出", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hasPendingBinding()) {
            confirmCancelBindingBeforeAction(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, "退出后将取消当前样品绑定，是否继续？");
            return;
        }
        finish();
    }

    private boolean hasPendingBinding() {
        return isValveBound && !inboundSubmitted && palletNo != null && !palletNo.isEmpty();
    }

    private void confirmCancelBindingBeforeAction(final Runnable afterCancelAction, String message) {
        new AlertDialog.Builder(this)
            .setTitle("取消绑定")
            .setMessage(message)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    cancelBinding(afterCancelAction);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void cancelBinding(final Runnable afterCancelAction) {
        Toast.makeText(this, "正在取消绑定...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = wmsApiService.unbindPallet(palletNo, InboundActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                isValveBound = false;
                                matCode = null;
                                inboundSubmitted = false;
                                palletNo = null;
                                binCode = null;
                                if (isPalletScanEnabled) {
                                    palletType = null;
                                    updateInboundStationSelection(null);
                                } else {
                                    palletType = resolvePalletTypeByOutsideSite(swapStation);
                                }
                                tvPalletNo.setText("--");
                                tvLocationCode.setText("--");
                                updateStatus(false);
                                if (!isPalletScanEnabled && swapStation != null && !swapStation.isEmpty()) {
                                    fetchAvailablePallet(swapStation);
                                }
                                Toast.makeText(InboundActivity.this, "已取消绑定", Toast.LENGTH_SHORT).show();
                                if (afterCancelAction != null) {
                                    afterCancelAction.run();
                                }
                            } else {
                                Toast.makeText(InboundActivity.this, "取消绑定失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(InboundActivity.this, "取消绑定失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
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
            updateStepLabels();
            if (!isPalletScanEnabled && swapStation != null && !swapStation.isEmpty()) {
                fetchAvailablePallet(swapStation);
            }
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
    }
    
    @Override
    protected void onDestroy() {
        if (scanHelper != null) {
            scanHelper.stopScan();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        attemptExit();
    }

}

