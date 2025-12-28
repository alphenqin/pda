package com.qs.qs5502demo.send;

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
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.util.HashMap;
import java.util.Map;

public class SendInspectionActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallSend;
    private Button btnEmptyPalletReturn1;
    private Button btnEmptyPalletReturn2;
    private Button btnBack;
    
    private WmsApiService wmsApiService;
    
    private String palletNo;
    private String binCode;
    private String matCode;
    private String inspectionStation = "INSPECTION_STATION_1"; // 检测区站点，可根据实际情况配置
    private Valve selectedValve;

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
        btnEmptyPalletReturn1 = (Button) findViewById(R.id.btnEmptyPalletReturn1);
        btnEmptyPalletReturn2 = (Button) findViewById(R.id.btnEmptyPalletReturn2);
        btnBack = (Button) findViewById(R.id.btnBack);
        
        updateStatus(false);
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
        
        btnEmptyPalletReturn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callEmptyPalletReturn("1");
            }
        });
        
        btnEmptyPalletReturn2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callEmptyPalletReturn("2");
            }
        });
    }
    
    /**
     * 呼叫送检
     */
    private void callSendInspection() {
        if (selectedValve == null || palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫送检")
            .setMessage("阀门编号：" + selectedValve.getValveNo() + 
                       "\n托盘号：" + palletNo + 
                       "\n库位号：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCallSendInspection();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行呼叫送检
     */
    private void performCallSendInspection() {
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
                    params.put("toBinCode", inspectionStation);
                    if (matCode != null) {
                        params.put("matCode", matCode);
                    }
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, SendInspectionActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outID;
                                updateStatus(true);
                                Toast.makeText(SendInspectionActivity.this, 
                                    "呼叫送检成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SendInspectionActivity.this, "呼叫送检失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SendInspectionActivity.this, "呼叫送检失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 空托回库
     */
    private void callEmptyPalletReturn(String palletType) {
        if (binCode == null || binCode.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认空托回库")
            .setMessage("将" + palletType + "#空托盘送回库位：" + binCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performEmptyPalletReturn(palletType);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行空托回库
     */
    private void performEmptyPalletReturn(String palletType) {
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
                    params.put("fromBinCode", inspectionStation);
                    params.put("toBinCode", binCode);
                    params.put("remark", "EMPTY_RETURN_FROM_INSPECTION");
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, SendInspectionActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outID;
                                Toast.makeText(SendInspectionActivity.this, 
                                    "空托回库成功，任务号：" + taskNo, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(SendInspectionActivity.this, "空托回库失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SendInspectionActivity.this, "空托回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            selectedValve = (Valve) data.getSerializableExtra("valve");
            if (selectedValve != null) {
                palletNo = selectedValve.getPalletNo();
                binCode = selectedValve.getBinCode();
                matCode = selectedValve.getMatCode();
                
                tvPalletNo.setText(palletNo);
                tvLocationCode.setText(binCode);
                updateStatus(true);
            }
        }
    }
}

