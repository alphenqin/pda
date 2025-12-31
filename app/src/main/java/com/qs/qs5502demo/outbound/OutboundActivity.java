package com.qs.qs5502demo.outbound;

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
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.send.SelectValveActivity;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OutboundActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallOutbound;
    private Button btnEmptyPalletReturn;
    private Button btnBack;
    private CharSequence callOutboundLabel;
    private CharSequence emptyPalletReturnLabel;
    
    private WmsApiService wmsApiService;
    
    private String palletNo;
    private String binCode;
    private String matCode;
    private String swapStation = "WAREHOUSE_SWAP_1"; // 置换区站点
    private Valve selectedValve;
    private String lastOutboundToBinCode;
    private static final long OUTBOUND_POLL_INTERVAL_MS = 5000L;
    private static final long OUTBOUND_TIMEOUT_MS = 40L * 60L * 1000L;
    private static final String PALLET_TYPE_SMALL = "t1";
    private static final String PALLET_TYPE_LARGE = "t2";
    private static final String SMALL_BUFFER_BIN = "B3-15-01";
    private static final String LARGE_BUFFER_BIN = "B3-14-01";
    private static final String SMALL_DOCK_BIN = "D2-小托盘接驳点";
    private static final String LARGE_DOCK_BIN = "D2-大托盘接驳点";
    private static final String SMALL_OUTBOUND_TARGET = "Z3-检测点";
    private static final String LARGE_OUTBOUND_TARGET = "Z4-检测点";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_outbound);
        
        wmsApiService = new WmsApiService(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnCallOutbound = (Button) findViewById(R.id.btnCallOutbound);
        btnEmptyPalletReturn = (Button) findViewById(R.id.btnEmptyPalletReturn);
        btnBack = (Button) findViewById(R.id.btnBack);
        callOutboundLabel = btnCallOutbound.getText();
        emptyPalletReturnLabel = btnEmptyPalletReturn.getText();
        
        updateStatus(false);
        updateOutboundLockUi();
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSelectValve.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 跳转到选阀门页面
                Intent intent = new Intent(OutboundActivity.this, SelectValveActivity.class);
                intent.putExtra("taskType", "OUTBOUND");
                startActivityForResult(intent, 400);
            }
        });
        
        btnCallOutbound.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callOutbound();
            }
        });
        
        btnEmptyPalletReturn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callEmptyPalletReturn();
            }
        });
    }
    
    /**
     * 呼叫出库
     */
    private void callOutbound() {
        if (PreferenceUtil.getOutboundLock(this)) {
            Toast.makeText(this, "出库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (PreferenceUtil.getOutboundEmptyReturnLock(this)) {
            Toast.makeText(this, "空托回库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedValve == null || palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫出库")
            .setMessage("阀门编号：" + selectedValve.getValveNo() + 
                       "\n托盘号：" + palletNo + 
                       "\n库位号：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallOutbound();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行呼叫出库
     */
    private void performCallOutbound() {
        Toast.makeText(this, "正在创建出库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String palletType = resolvePalletTypeCode(palletNo);
                    if (palletType == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(OutboundActivity.this, "无法识别托盘类型", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    setOutboundLock(true);
                    String bufferBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_BUFFER_BIN : SMALL_BUFFER_BIN;
                    String dockBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_DOCK_BIN : SMALL_DOCK_BIN;
                    String targetBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_OUTBOUND_TARGET : SMALL_OUTBOUND_TARGET;

                    // 阶段1：库位 -> 中转位
                    String baseOutId = DateUtil.generateTaskNo("C");
                    String outIdStage1 = baseOutId + "-1";
                    Map<String, String> stage1 = new HashMap<>();
                    stage1.put("taskType", Task.TYPE_OUTBOUND);
                    stage1.put("outID", outIdStage1);
                    stage1.put("deviceCode", PreferenceUtil.getDeviceCode(OutboundActivity.this));
                    stage1.put("palletNo", palletNo);
                    stage1.put("fromBinCode", binCode);
                    stage1.put("toBinCode", bufferBin);
                    stage1.put("remark", "OUTBOUND_STAGE1");
                    stage1.put("agvRange", "1");
                    TaskDispatchResult stage1Result = wmsApiService.dispatchTask(stage1, OutboundActivity.this);
                    if (stage1Result == null) {
                        throw new Exception("阶段1下发失败");
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "阶段1已下发，等待完成...", Toast.LENGTH_SHORT).show();
                        }
                    });

                    boolean stage1Completed = waitForTaskCompleted(outIdStage1);
                    if (!stage1Completed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(OutboundActivity.this, "阶段1未完成，停止后续下发", Toast.LENGTH_SHORT).show();
                            }
                        });
                        setOutboundLock(false);
                        return;
                    }

                    // 阶段2：接驳点 -> 检测点
                    String outIdStage2 = baseOutId + "-2";
                    Map<String, String> stage2 = new HashMap<>();
                    stage2.put("taskType", Task.TYPE_OUTBOUND);
                    stage2.put("outID", outIdStage2);
                    stage2.put("deviceCode", PreferenceUtil.getDeviceCode(OutboundActivity.this));
                    stage2.put("palletNo", palletNo);
                    stage2.put("fromBinCode", dockBin);
                    stage2.put("toBinCode", targetBin);
                    if (matCode != null) {
                        stage2.put("matCode", matCode);
                    }
                    stage2.put("remark", "OUTBOUND_STAGE2");
                    stage2.put("agvRange", "2");
                    TaskDispatchResult result = wmsApiService.dispatchTask(stage2, OutboundActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outIdStage2;
                                lastOutboundToBinCode = targetBin;
                                Toast.makeText(OutboundActivity.this, 
                                    "呼叫出库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(OutboundActivity.this, "呼叫出库失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    if (result == null) {
                        setOutboundLock(false);
                        return;
                    }

                    boolean stage2Completed = waitForTaskCompleted(outIdStage2);
                    if (stage2Completed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus(true);
                            }
                        });
                    }
                    setOutboundLock(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "呼叫出库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    setOutboundLock(false);
                }
            }
        }).start();
    }
    
    /**
     * 空托回库
     */
    private void callEmptyPalletReturn() {
        if (binCode == null || binCode.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        if (PreferenceUtil.getOutboundEmptyReturnLock(this)) {
            Toast.makeText(this, "空托回库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (PreferenceUtil.getOutboundLock(this)) {
            Toast.makeText(this, "出库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lastOutboundToBinCode == null || lastOutboundToBinCode.isEmpty()) {
            Toast.makeText(this, "请先呼叫出库", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认空托回库")
            .setMessage("将空托盘送回库位：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performEmptyPalletReturn();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行空托回库
     */
    private void performEmptyPalletReturn() {
        Toast.makeText(this, "正在创建空托回库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String palletType = resolvePalletTypeCode(palletNo);
                    if (palletType == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(OutboundActivity.this, "无法识别托盘类型", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    setOutboundEmptyReturnLock(true);
                    String bufferBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_BUFFER_BIN : SMALL_BUFFER_BIN;
                    String dockBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_DOCK_BIN : SMALL_DOCK_BIN;

                    // 阶段1：检测点 -> 接驳点
                    String baseOutId = DateUtil.generateTaskNo("H");
                    String outIdStage1 = baseOutId + "-1";
                    Map<String, String> stage1 = new HashMap<>();
                    stage1.put("taskType", Task.TYPE_RETURN);
                    stage1.put("outID", outIdStage1);
                    stage1.put("deviceCode", PreferenceUtil.getDeviceCode(OutboundActivity.this));
                    if (palletNo != null) {
                        stage1.put("palletNo", palletNo);
                    }
                    stage1.put("fromBinCode", lastOutboundToBinCode);
                    stage1.put("toBinCode", dockBin);
                    stage1.put("remark", "OUTBOUND_EMPTY_RETURN_STAGE1");
                    stage1.put("agvRange", "2");
                    TaskDispatchResult stage1Result = wmsApiService.dispatchTask(stage1, OutboundActivity.this);
                    if (stage1Result == null) {
                        throw new Exception("阶段1下发失败");
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "阶段1已下发，等待完成...", Toast.LENGTH_SHORT).show();
                        }
                    });

                    boolean stage1Completed = waitForTaskCompleted(outIdStage1);
                    if (!stage1Completed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(OutboundActivity.this, "阶段1未完成，停止后续下发", Toast.LENGTH_SHORT).show();
                            }
                        });
                        setOutboundEmptyReturnLock(false);
                        return;
                    }

                    // 阶段2：中转位 -> 库位
                    String outIdStage2 = baseOutId + "-2";
                    Map<String, String> stage2 = new HashMap<>();
                    stage2.put("taskType", Task.TYPE_RETURN);
                    stage2.put("outID", outIdStage2);
                    stage2.put("deviceCode", PreferenceUtil.getDeviceCode(OutboundActivity.this));
                    if (palletNo != null) {
                        stage2.put("palletNo", palletNo);
                    }
                    stage2.put("fromBinCode", bufferBin);
                    stage2.put("toBinCode", binCode);
                    stage2.put("remark", "OUTBOUND_EMPTY_RETURN_STAGE2");
                    stage2.put("agvRange", "1");
                    TaskDispatchResult result = wmsApiService.dispatchTask(stage2, OutboundActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outIdStage2;
                                Toast.makeText(OutboundActivity.this, 
                                    "空托回库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(OutboundActivity.this, "空托回库失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    if (result == null) {
                        setOutboundEmptyReturnLock(false);
                        return;
                    }

                    boolean stage2Completed = waitForTaskCompleted(outIdStage2);
                    if (stage2Completed) {
                        // No UI state to update besides lock.
                    }
                    setOutboundEmptyReturnLock(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "空托回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    setOutboundEmptyReturnLock(false);
                }
            }
        }).start();
    }
    
    /**
     * 更新状态标识
     */
    private void updateStatus(boolean success) {
        if (success) {
            viewStatus.setBackgroundColor(0xFF4CAF50);
        } else {
            viewStatus.setBackgroundColor(0xFFCCCCCC);
        }
    }

    private void applyAgvRange(Map<String, String> params) {
        String agvRange = PreferenceUtil.getAgvRange(this);
        if (agvRange != null && !agvRange.isEmpty()) {
            params.put("agvRange", agvRange);
        }
    }

    private void setOutboundLock(boolean locked) {
        PreferenceUtil.saveOutboundLock(this, locked);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateOutboundLockUi();
            }
        });
    }

    private void updateOutboundLockUi() {
        updateOutboundEmptyReturnLockUi();
    }

    private void setOutboundEmptyReturnLock(boolean locked) {
        PreferenceUtil.saveOutboundEmptyReturnLock(this, locked);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateOutboundEmptyReturnLockUi();
            }
        });
    }

    private void updateOutboundEmptyReturnLockUi() {
        boolean locked = PreferenceUtil.getOutboundEmptyReturnLock(this);
        boolean outboundLocked = PreferenceUtil.getOutboundLock(this);
        boolean callOutboundEnabled = !locked && !outboundLocked;
        btnCallOutbound.setEnabled(callOutboundEnabled);
        if (callOutboundEnabled) {
            btnCallOutbound.setAlpha(1.0f);
            btnCallOutbound.setText(callOutboundLabel);
        } else {
            btnCallOutbound.setAlpha(0.4f);
            btnCallOutbound.setText(callOutboundLabel + (outboundLocked ? "（出库中）" : "（空托回库中）"));
        }

        boolean emptyReturnEnabled = !locked && !outboundLocked;
        btnEmptyPalletReturn.setEnabled(emptyReturnEnabled);
        if (emptyReturnEnabled) {
            btnEmptyPalletReturn.setAlpha(1.0f);
            btnEmptyPalletReturn.setText(emptyPalletReturnLabel);
        } else {
            btnEmptyPalletReturn.setAlpha(0.4f);
            if (locked) {
                btnEmptyPalletReturn.setText(emptyPalletReturnLabel + "（回库中）");
            } else {
                btnEmptyPalletReturn.setText(emptyPalletReturnLabel + "（出库中）");
            }
        }
    }

    private String resolvePalletTypeCode(String palletNo) {
        if (palletNo == null) {
            return null;
        }
        String normalized = palletNo.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String lower = normalized.toLowerCase();
        if (lower.contains("t1")) {
            return PALLET_TYPE_SMALL;
        }
        if (lower.contains("t2")) {
            return PALLET_TYPE_LARGE;
        }
        char first = lower.charAt(0);
        if (first == 'x') {
            return PALLET_TYPE_SMALL;
        }
        if (first == 'd') {
            return PALLET_TYPE_LARGE;
        }
        return null;
    }

    private boolean waitForTaskCompleted(String outId) {
        long deadline = System.currentTimeMillis() + OUTBOUND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Map<String, String> params = new HashMap<>();
                long now = System.currentTimeMillis();
                String startDate = DateUtil.formatDate(new Date(now - 24L * 60L * 60L * 1000L));
                String endDate = DateUtil.formatDate(new Date(now + 24L * 60L * 60L * 1000L));
                params.put("startDate", startDate);
                params.put("endDate", endDate);
                params.put("pageNum", "1");
                params.put("pageSize", "50");
                params.put("deviceCode", PreferenceUtil.getDeviceCode(OutboundActivity.this));
                PageResponse<Task> pageResponse = wmsApiService.queryTasks(params, OutboundActivity.this);
                if (pageResponse != null && pageResponse.getList() != null) {
                    for (Task task : pageResponse.getList()) {
                        if (outId.equals(task.getTaskId())) {
                            String status = task.getStatus();
                            if (Task.STATUS_COMPLETED.equals(status)) {
                                return true;
                            }
                            if (Task.STATUS_FAILED.equals(status) || Task.STATUS_CANCELLED.equals(status)) {
                                return false;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Continue polling until timeout.
            }
            try {
                Thread.sleep(OUTBOUND_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 400 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            selectedValve = (Valve) data.getSerializableExtra("valve");
            if (selectedValve != null) {
                palletNo = selectedValve.getPalletNo();
                binCode = selectedValve.getBinCode();
                matCode = selectedValve.getMatCode();
                lastOutboundToBinCode = null;
                
                tvPalletNo.setText(palletNo);
                tvLocationCode.setText(binCode);
                updateStatus(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOutboundLockUi();
        updateOutboundEmptyReturnLockUi();
    }
}

