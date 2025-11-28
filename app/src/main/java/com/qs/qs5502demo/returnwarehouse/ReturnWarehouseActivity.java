package com.qs.qs5502demo.returnwarehouse;

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
import com.qs.qs5502demo.api.ApiClient;
import com.qs.qs5502demo.api.ApiService;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.send.SelectValveActivity;

public class ReturnWarehouseActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallPallet;
    private Button btnValveReturn1;
    private Button btnValveReturn2;
    private Button btnBack;
    
    private ApiService apiService;
    
    private String palletNo;
    private String locationCode;
    private Valve selectedValve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);
        
        apiService = new ApiClient();
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnCallPallet = (Button) findViewById(R.id.btnCallPallet);
        btnValveReturn1 = (Button) findViewById(R.id.btnValveReturn1);
        btnValveReturn2 = (Button) findViewById(R.id.btnValveReturn2);
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
        
        btnValveReturn1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callValveReturn("1");
            }
        });
        
        btnValveReturn2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callValveReturn("2");
            }
        });
    }
    
    /**
     * 呼叫托盘
     */
    private void callPallet() {
        if (selectedValve == null || palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认呼叫托盘")
            .setMessage("将空托盘从库位 " + locationCode + " 运送到检测区站点")
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
                    Task task = apiService.createTask(
                        Task.TYPE_RETURN,
                        palletNo,
                        locationCode,
                        "WAREHOUSE_LOCATION_" + locationCode.replace("-", "_"),
                        "INSPECTION_STATION",
                        selectedValve.getValveNo()
                    );
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (task != null && task.getTaskNo() != null) {
                                Toast.makeText(ReturnWarehouseActivity.this, 
                                    "呼叫托盘成功，任务号：" + task.getTaskNo(), 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ReturnWarehouseActivity.this, "呼叫托盘失败", Toast.LENGTH_SHORT).show();
                            }
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
                }
            }
        }).start();
    }
    
    /**
     * 阀门回库
     */
    private void callValveReturn(String palletType) {
        if (locationCode == null || locationCode.isEmpty()) {
            Toast.makeText(this, "请先选择阀门", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认阀门回库")
            .setMessage("将" + palletType + "#阀门送回库位：" + locationCode)
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performValveReturn(palletType);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行阀门回库
     */
    private void performValveReturn(String palletType) {
        Toast.makeText(this, "正在创建回库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Task task = apiService.createTask(
                        Task.TYPE_RETURN,
                        palletNo,
                        locationCode,
                        "INSPECTION_STATION",
                        "WAREHOUSE_LOCATION_" + locationCode.replace("-", "_"),
                        selectedValve != null ? selectedValve.getValveNo() : null
                    );
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (task != null && task.getTaskNo() != null) {
                                updateStatus(true);
                                Toast.makeText(ReturnWarehouseActivity.this, 
                                    "阀门回库成功，任务号：" + task.getTaskNo(), 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ReturnWarehouseActivity.this, "阀门回库失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReturnWarehouseActivity.this, "阀门回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            selectedValve = (Valve) data.getSerializableExtra("valve");
            if (selectedValve != null) {
                palletNo = selectedValve.getPalletNo();
                locationCode = selectedValve.getLocationCode();
                
                tvPalletNo.setText(palletNo);
                tvLocationCode.setText(locationCode);
                updateStatus(true);
            }
        }
    }
}

