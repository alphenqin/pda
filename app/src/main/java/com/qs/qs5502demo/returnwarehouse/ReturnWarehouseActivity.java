package com.qs.qs5502demo.returnwarehouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.qs.qs5502demo.model.TaskLockStatus;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.send.SelectValveActivity;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.util.HashMap;
import java.util.Map;

public class ReturnWarehouseActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallPallet;
    private Button btnValveReturn;
    private Button btnBack;
    private CharSequence callPalletLabel;
    private CharSequence valveReturnLabel;
    
    private WmsApiService wmsApiService;
    
    private String palletNo;
    private String binCode;
    private String matCode;
    private String inspectionTargetBin;
    private Valve selectedValve;
    private static final long CALL_PALLET_POLL_INTERVAL_MS = 5000L;
    private static final long CALL_PALLET_TIMEOUT_MS = 40L * 60L * 1000L;
    private static final String PALLET_TYPE_SMALL = "t1";
    private static final String PALLET_TYPE_LARGE = "t2";
    private static final String SMALL_BUFFER_BIN = "B3-15-01";
    private static final String LARGE_BUFFER_BIN = "B3-14-01";
    private static final String SMALL_DOCK_BIN = "D2-小托盘接驳点";
    private static final String LARGE_DOCK_BIN = "D2-大托盘接驳点";
    private static final long LOCK_POLL_INTERVAL_MS = 5000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockStatusRunnable;
    private boolean returnCallLocked = false;
    private boolean returnValveLocked = false;
    private boolean callPalletInProgress = false;
    private boolean valveReturnInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_return);
        
        wmsApiService = new WmsApiService(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnCallPallet = (Button) findViewById(R.id.btnCallPallet);
        btnValveReturn = (Button) findViewById(R.id.btnValveReturn);
        btnBack = (Button) findViewById(R.id.btnBack);
        callPalletLabel = btnCallPallet.getText();
        valveReturnLabel = btnValveReturn.getText();
        
        updateStatus(false);
        updateButtonLocks();
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
                Intent intent = new Intent(ReturnWarehouseActivity.this, SelectValveActivity.class);
                intent.putExtra("taskType", "RETURN");
                startActivityForResult(intent, 300);
            }
        });
        
        btnCallPallet.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callPallet();
            }
        });
        
        btnValveReturn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callValveReturn();
            }
        });
    }
    
    /**
     * 呼叫托盘
     */
    private void callPallet() {
        if (returnCallLocked || callPalletInProgress) {
            Toast.makeText(this, "呼叫托盘进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (returnValveLocked || valveReturnInProgress) {
            Toast.makeText(this, "样品回库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedValve == null || palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先选择样品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (inspectionTargetBin == null || inspectionTargetBin.isEmpty()) {
            Toast.makeText(this, "送检目标站点未设置，请先完成送检", Toast.LENGTH_SHORT).show();
            return;
        }

        String palletType = resolvePalletTypeCode(palletNo);
        if (palletType == null) {
            Toast.makeText(this, "无法识别托盘类型", Toast.LENGTH_SHORT).show();
            return;
        }
        String bufferBin = PALLET_TYPE_LARGE.equalsIgnoreCase(palletType) ? LARGE_BUFFER_BIN : SMALL_BUFFER_BIN;
        
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫托盘")
            .setMessage("将空托盘从库位 " + binCode + " 运送到中转位 " + bufferBin +
                "\n完成后送往目标站点：" + inspectionTargetBin)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallPallet();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行呼叫托盘
     */
    private void performCallPallet() {
        Toast.makeText(this, "正在创建呼叫托盘任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String palletType = resolvePalletTypeCode(palletNo);
                    if (palletType == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "无法识别托盘类型", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    callPalletInProgress = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });

                    String outId = DateUtil.generateTaskNo("H");
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_RETURN);
                    params.put("outID", outId);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(ReturnWarehouseActivity.this));
                    params.put("palletNo", palletNo);
                    params.put("fromBinCode", binCode);
                    params.put("toBinCode", inspectionTargetBin);
                    params.put("remark", "RETURN_CALL_PALLET");
                    if (selectedValve != null && selectedValve.getValveNo() != null) {
                        params.put("valveNo", selectedValve.getValveNo());
                    }
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, ReturnWarehouseActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outId;
                                Toast.makeText(ReturnWarehouseActivity.this,
                                    "呼叫托盘已下发，任务号：" + taskNo,
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ReturnWarehouseActivity.this, "呼叫托盘下发失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    if (result == null) {
                        callPalletInProgress = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateButtonLocks();
                            }
                        });
                        return;
                    }

                    String taskNo = result.getOutID() != null ? result.getOutID() : outId;
                    boolean completed = waitForTaskCompleted(taskNo);
                    if (!completed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "呼叫托盘未完成", Toast.LENGTH_SHORT).show();
                            }
                        });
                        callPalletInProgress = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateButtonLocks();
                            }
                        });
                        return;
                    }

                    callPalletInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReturnWarehouseActivity.this, "呼叫托盘失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    callPalletInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 阀门回库
     */
    private void callValveReturn() {
        if (binCode == null || binCode.isEmpty()) {
            Toast.makeText(this, "请先选择样品", Toast.LENGTH_SHORT).show();
            return;
        }
        if (inspectionTargetBin == null || inspectionTargetBin.isEmpty()) {
            Toast.makeText(this, "送检目标站点未设置，请先完成送检", Toast.LENGTH_SHORT).show();
            return;
        }
        if (returnValveLocked || valveReturnInProgress) {
            Toast.makeText(this, "样品回库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认样品回库")
            .setMessage("将样品送回库位：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performValveReturn();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行阀门回库
     */
    private void performValveReturn() {
        Toast.makeText(this, "正在创建回库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String palletType = resolvePalletTypeCode(palletNo);
                    if (palletType == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "无法识别托盘类型", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    valveReturnInProgress = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });

                    String outId = DateUtil.generateTaskNo("H");
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_RETURN);
                    params.put("outID", outId);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(ReturnWarehouseActivity.this));
                    params.put("palletNo", palletNo);
                    params.put("fromBinCode", inspectionTargetBin);
                    params.put("toBinCode", binCode);
                    if (matCode != null) {
                        params.put("matCode", matCode);
                    }
                    params.put("remark", "VALVE_RETURN");
                    if (selectedValve != null && selectedValve.getValveNo() != null) {
                        params.put("valveNo", selectedValve.getValveNo());
                    }
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, ReturnWarehouseActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outId;
                                Toast.makeText(ReturnWarehouseActivity.this, 
                                    "样品回库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ReturnWarehouseActivity.this, "样品回库下发失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    if (result == null) {
                        valveReturnInProgress = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateButtonLocks();
                            }
                        });
                        return;
                    }

                    String taskNo = result.getOutID() != null ? result.getOutID() : outId;
                    boolean completed = waitForTaskCompleted(taskNo);
                    if (completed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateStatus(true);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "样品回库未完成", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    valveReturnInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReturnWarehouseActivity.this, "样品回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    valveReturnInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
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

    private void updateButtonLocks() {
        boolean callPalletLocked = returnCallLocked;
        boolean valveReturnLocked = returnValveLocked;
        boolean callPalletEnabled = !callPalletLocked && !valveReturnLocked && !callPalletInProgress;
        btnCallPallet.setEnabled(callPalletEnabled);
        if (callPalletEnabled) {
            btnCallPallet.setAlpha(1.0f);
            btnCallPallet.setText(callPalletLabel);
        } else {
            btnCallPallet.setAlpha(0.4f);
            if (valveReturnLocked) {
                btnCallPallet.setText(callPalletLabel + "（样品回库中）");
            } else if (callPalletInProgress) {
                btnCallPallet.setText(callPalletLabel + "（处理中）");
            } else {
                btnCallPallet.setText(callPalletLabel + "（呼叫托盘中）");
            }
        }

        boolean valveReturnEnabled = !callPalletLocked && !valveReturnLocked && !valveReturnInProgress;
        btnValveReturn.setEnabled(valveReturnEnabled);
        if (valveReturnEnabled) {
            btnValveReturn.setAlpha(1.0f);
            btnValveReturn.setText(valveReturnLabel);
        } else {
            btnValveReturn.setAlpha(0.4f);
            if (valveReturnLocked) {
                btnValveReturn.setText(valveReturnLabel + "（回库中）");
            } else if (valveReturnInProgress) {
                btnValveReturn.setText(valveReturnLabel + "（处理中）");
            } else {
                btnValveReturn.setText(valveReturnLabel + "（呼叫托盘中）");
            }
        }
    }

    private void startLockStatusPolling() {
        if (lockStatusRunnable != null) {
            return;
        }
        lockStatusRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLockStatus();
                handler.postDelayed(this, LOCK_POLL_INTERVAL_MS);
            }
        };
        handler.post(lockStatusRunnable);
    }

    private void stopLockStatusPolling() {
        if (lockStatusRunnable != null) {
            handler.removeCallbacks(lockStatusRunnable);
            lockStatusRunnable = null;
        }
    }

    private void refreshLockStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskLockStatus status = wmsApiService.getTaskLockStatus(ReturnWarehouseActivity.this);
                    boolean callLocked = status != null && status.isReturnCallLocked();
                    boolean valveLocked = status != null && status.isReturnValveLocked();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnCallLocked = callLocked;
                            returnValveLocked = valveLocked;
                            updateButtonLocks();
                        }
                    });
                } catch (Exception e) {
                    // Keep current state on error.
                }
            }
        }).start();
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
        long deadline = System.currentTimeMillis() + CALL_PALLET_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Map<String, String> params = new HashMap<>();
                String today = DateUtil.getCurrentDate();
                params.put("startDate", today);
                params.put("endDate", today);
                params.put("pageNum", "1");
                params.put("pageSize", "50");
                params.put("deviceCode", PreferenceUtil.getDeviceCode(ReturnWarehouseActivity.this));
                PageResponse<Task> pageResponse = wmsApiService.queryTasks(params, ReturnWarehouseActivity.this);
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
                Thread.sleep(CALL_PALLET_POLL_INTERVAL_MS);
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
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            selectedValve = (Valve) data.getSerializableExtra("valve");
            if (selectedValve != null) {
                palletNo = selectedValve.getPalletNo();
                binCode = selectedValve.getBinCode();
                matCode = selectedValve.getMatCode();
                inspectionTargetBin = selectedValve.getInspectionTargetBin();
                
                tvPalletNo.setText(palletNo);
                tvLocationCode.setText(binCode);
                updateStatus(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLockStatusPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLockStatusPolling();
    }
}

