package com.qs.qs5502demo.send;

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
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.TaskLockStatus;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendInspectionActivity extends Activity {

    private static final long INSPECTION_LOCK_POLL_MS = 5000L;
    private static final String INSPECTION_AREA_WAITING = "WAITING";
    private static final String INSPECTION_AREA_FLOW = "FLOW_DEVICE";
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallSend;
    private Button btnEmptyPalletReturn;
    private Button btnBack;
    
    private WmsApiService wmsApiService;
    
    private String palletNo;
    private String binCode;
    private String matCode;
    private String inspectionStation = "INSPECTION_STATION_1"; // 检测区站点，可根据实际情况配置
    private Valve selectedValve;
    private Handler handler = new Handler();
    private Runnable inspectionLockRunnable;
    private long lastLockStatusErrorAt = 0L;
    private boolean inspectionLocked = false;
    private boolean inspectionEmptyReturnLocked = false;
    private CharSequence callSendLabel;
    private CharSequence emptyReturnLabel;
    private boolean callSendInProgress = false;
    private boolean emptyReturnInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_send_inspection);
        
        wmsApiService = new WmsApiService(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnCallSend = (Button) findViewById(R.id.btnCallSend);
        btnEmptyPalletReturn = (Button) findViewById(R.id.btnEmptyPalletReturn);
        btnBack = (Button) findViewById(R.id.btnBack);
        callSendLabel = btnCallSend.getText();
        emptyReturnLabel = btnEmptyPalletReturn.getText();
        
        updateStatus(false);
        restoreSelectedValve();
        refreshInspectionLockStatus();
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
                Intent intent = new Intent(SendInspectionActivity.this, SelectValveActivity.class);
                intent.putExtra("taskType", "SEND_INSPECTION");
                startActivityForResult(intent, 200);
            }
        });
        
        btnCallSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callSendInspection();
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
     * 呼叫送检
     */
    private void callSendInspection() {
        if (selectedValve == null || palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先选择样品", Toast.LENGTH_SHORT).show();
            return;
        }

        showInspectionAreaDialog();
    }

    private void showInspectionAreaDialog() {
        final String[] items = new String[] { "待检区", "直排流量装置区" };
        new AlertDialog.Builder(this)
            .setTitle("选择目标站点")
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    String area = which == 0 ? INSPECTION_AREA_WAITING : INSPECTION_AREA_FLOW;
                    String label = items[which];
                    showSendInspectionConfirm(area, label);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showSendInspectionConfirm(String area, String areaLabel) {
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫送检")
            .setMessage("样品编号：" + selectedValve.getValveNo() +
                       "\n托盘号：" + palletNo +
                       "\n库位号：" + binCode +
                       "\n目标区域：" + areaLabel)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallSendInspection(area);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行呼叫送检
     */
    private void performCallSendInspection(String area) {
        if (callSendInProgress) {
            Toast.makeText(this, "送检任务下发中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        callSendInProgress = true;
        btnCallSend.setEnabled(false);
        btnCallSend.setAlpha(0.4f);
        btnCallSend.setText(callSendLabel + "（处理中）");
        Toast.makeText(this, "正在创建送检任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 生成任务编号
                    String outID = DateUtil.generateTaskNo("S");
                    
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_SEND_INSPECTION);
                    params.put("outID", outID);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(SendInspectionActivity.this));
                    params.put("palletNo", palletNo);
                    params.put("fromBinCode", binCode);
                    params.put("toBinCode", area);
                    params.put("inspectionArea", area);
                    if (selectedValve != null && selectedValve.getValveNo() != null) {
                        params.put("valveNo", selectedValve.getValveNo());
                    }
                    if (matCode != null) {
                        params.put("matCode", matCode);
                    }
                    applyAgvRange(params);
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, SendInspectionActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callSendInProgress = false;
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outID;
                                updateStatus(true);
                                persistSelectedValve();
                                Toast.makeText(SendInspectionActivity.this, 
                                    "呼叫送检成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SendInspectionActivity.this, "呼叫送检失败", Toast.LENGTH_SHORT).show();
                            }
                            applyInspectionLock(inspectionLocked, inspectionEmptyReturnLocked);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            callSendInProgress = false;
                            Toast.makeText(SendInspectionActivity.this, "呼叫送检失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            applyInspectionLock(inspectionLocked, inspectionEmptyReturnLocked);
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 空托回库
     */
    private void callEmptyPalletReturn() {
        if (binCode == null || binCode.isEmpty()) {
            Toast.makeText(this, "请先选择样品", Toast.LENGTH_SHORT).show();
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
        if (emptyReturnInProgress) {
            Toast.makeText(this, "空托回库下发中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }
        emptyReturnInProgress = true;
        btnEmptyPalletReturn.setEnabled(false);
        btnEmptyPalletReturn.setAlpha(0.4f);
        btnEmptyPalletReturn.setText(emptyReturnLabel + "（处理中）");
        Toast.makeText(this, "正在创建空托回库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 生成任务编号
                    String outID = DateUtil.generateTaskNo("H");
                    
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_RETURN);
                    params.put("outID", outID);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(SendInspectionActivity.this));
                    if (palletNo != null) {
                        params.put("palletNo", palletNo);
                    }
                    params.put("fromBinCode", binCode);
                    params.put("toBinCode", binCode);
                    params.put("remark", "INSPECTION_EMPTY_RETURN");
                    if (selectedValve != null && selectedValve.getValveNo() != null) {
                        params.put("valveNo", selectedValve.getValveNo());
                    }
                    applyAgvRange(params);
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, SendInspectionActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            emptyReturnInProgress = false;
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outID;
                                PreferenceUtil.clearLastSendInspectionValve(SendInspectionActivity.this);
                                Toast.makeText(SendInspectionActivity.this, 
                                    "空托回库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SendInspectionActivity.this, "空托回库失败", Toast.LENGTH_SHORT).show();
                            }
                            applyInspectionLock(inspectionLocked, inspectionEmptyReturnLocked);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            emptyReturnInProgress = false;
                            Toast.makeText(SendInspectionActivity.this, "空托回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            applyInspectionLock(inspectionLocked, inspectionEmptyReturnLocked);
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

    @Override
    protected void onResume() {
        super.onResume();
        startInspectionLockPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopInspectionLockPolling();
    }

    private void startInspectionLockPolling() {
        if (inspectionLockRunnable != null) {
            return;
        }
        inspectionLockRunnable = new Runnable() {
            @Override
            public void run() {
                refreshInspectionLockStatus();
                handler.postDelayed(this, INSPECTION_LOCK_POLL_MS);
            }
        };
        handler.post(inspectionLockRunnable);
    }

    private void stopInspectionLockPolling() {
        if (inspectionLockRunnable != null) {
            handler.removeCallbacks(inspectionLockRunnable);
            inspectionLockRunnable = null;
        }
    }

    private void refreshInspectionLockStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskLockStatus status = wmsApiService.getTaskLockStatus(SendInspectionActivity.this);
                    boolean locked = status != null && status.isInspectionLocked();
                    boolean emptyReturnLocked = status != null && status.isInspectionEmptyReturnLocked();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            applyInspectionLock(locked, emptyReturnLocked);
                        }
                    });
                } catch (Exception e) {
                    long now = System.currentTimeMillis();
                    if (now - lastLockStatusErrorAt > 30000L) {
                        lastLockStatusErrorAt = now;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SendInspectionActivity.this, "锁状态刷新失败，请检查网络/服务", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void applyInspectionLock(boolean locked, boolean emptyReturnLocked) {
        inspectionLocked = locked;
        inspectionEmptyReturnLocked = emptyReturnLocked;
        boolean callSendEnabled = !inspectionLocked && !callSendInProgress;
        btnCallSend.setEnabled(callSendEnabled);
        if (callSendEnabled) {
            btnCallSend.setAlpha(1.0f);
            btnCallSend.setText(callSendLabel);
        } else {
            btnCallSend.setAlpha(0.4f);
            btnCallSend.setText(callSendLabel + (inspectionLocked ? "（送检锁定）" : "（处理中）"));
        }

        boolean emptyReturnEnabled = !inspectionLocked && !inspectionEmptyReturnLocked && !emptyReturnInProgress;
        btnEmptyPalletReturn.setEnabled(emptyReturnEnabled);
        if (emptyReturnEnabled) {
            btnEmptyPalletReturn.setAlpha(1.0f);
            btnEmptyPalletReturn.setText(emptyReturnLabel);
        } else {
            btnEmptyPalletReturn.setAlpha(0.4f);
            if (inspectionLocked) {
                btnEmptyPalletReturn.setText(emptyReturnLabel + "（送检锁定）");
            } else if (inspectionEmptyReturnLocked) {
                btnEmptyPalletReturn.setText(emptyReturnLabel + "（空托回库中）");
            } else {
                btnEmptyPalletReturn.setText(emptyReturnLabel + "（处理中）");
            }
        }
    }

    private void restoreSelectedValve() {
        Valve cachedValve = PreferenceUtil.getLastSendInspectionValve(this);
        if (cachedValve != null) {
            applySelectedValve(cachedValve);
            Toast.makeText(this, "已恢复上次送检样品", Toast.LENGTH_SHORT).show();
            return;
        }
        recoverFromRecentSendInspectionTask();
    }

    private void recoverFromRecentSendInspectionTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_SEND_INSPECTION);
                    params.put("pageNum", "1");
                    params.put("pageSize", "50");
                    String deviceCode = PreferenceUtil.getDeviceCode(SendInspectionActivity.this);
                    if (deviceCode != null && !deviceCode.isEmpty()) {
                        params.put("deviceCode", deviceCode);
                    }
                    PageResponse<Task> pageResponse = wmsApiService.queryTasks(params, SendInspectionActivity.this);
                    Task latestTask = selectLatestTask(pageResponse != null ? pageResponse.getList() : null);
                    if (latestTask == null) {
                        return;
                    }
                    String latestPalletNo = latestTask.getPalletNo();
                    String latestBinCode = latestTask.getBinCode();
                    if ((latestPalletNo == null || latestPalletNo.isEmpty())
                        && (latestBinCode == null || latestBinCode.isEmpty())) {
                        return;
                    }
                    Valve valve = new Valve();
                    valve.setValveNo(latestTask.getValveNo());
                    valve.setPalletNo(latestPalletNo);
                    valve.setBinCode(latestBinCode);
                    valve.setMatCode(latestTask.getMatCode());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            applySelectedValve(valve);
                            persistSelectedValve();
                            Toast.makeText(SendInspectionActivity.this, "已从最近送检任务恢复样品", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception ignored) {
                    // 恢复失败时静默处理，不阻断主流程
                }
            }
        }).start();
    }

    private Task selectLatestTask(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        Task latest = null;
        long latestTime = Long.MIN_VALUE;
        for (Task task : tasks) {
            if (task == null || !Task.TYPE_SEND_INSPECTION.equals(task.getTaskType())) {
                continue;
            }
            long taskTime = parseTaskCreateTime(task.getCreateTime());
            if (latest == null || taskTime > latestTime) {
                latest = task;
                latestTime = taskTime;
            }
        }
        return latest;
    }

    private long parseTaskCreateTime(String time) {
        if (time == null || time.trim().isEmpty()) {
            return Long.MIN_VALUE;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(time);
            return date != null ? date.getTime() : Long.MIN_VALUE;
        } catch (ParseException e) {
            return Long.MIN_VALUE;
        }
    }

    private void applySelectedValve(Valve valve) {
        selectedValve = valve;
        if (selectedValve == null) {
            palletNo = null;
            binCode = null;
            matCode = null;
            tvPalletNo.setText("--");
            tvLocationCode.setText("--");
            updateStatus(false);
            return;
        }
        palletNo = selectedValve.getPalletNo();
        binCode = selectedValve.getBinCode();
        matCode = selectedValve.getMatCode();
        tvPalletNo.setText(palletNo != null && !palletNo.isEmpty() ? palletNo : "--");
        tvLocationCode.setText(binCode != null && !binCode.isEmpty() ? binCode : "--");
        updateStatus((palletNo != null && !palletNo.isEmpty()) || (binCode != null && !binCode.isEmpty()));
    }

    private void persistSelectedValve() {
        if (selectedValve == null) {
            return;
        }
        PreferenceUtil.saveLastSendInspectionValve(this, selectedValve);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            Valve valve = (Valve) data.getSerializableExtra("valve");
            applySelectedValve(valve);
            persistSelectedValve();
        }
    }
}

