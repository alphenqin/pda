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
import com.qs.qs5502demo.api.AgvApiService;
import com.qs.qs5502demo.model.AgvResponse;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.send.SelectValveActivity;
import com.qs.qs5502demo.util.DateUtil;

public class OutboundActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private View viewStatus;
    private Button btnSelectValve;
    private Button btnCallOutbound;
    private Button btnEmptyPalletReturn1;
    private Button btnEmptyPalletReturn2;
    private Button btnBack;
    
    private AgvApiService agvApiService;
    
    private String palletNo;
    private String binCode;
    private String matCode;
    private String swapStation = "WAREHOUSE_SWAP_1"; // 置换区站点
    private Valve selectedValve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_outbound);
        
        agvApiService = new AgvApiService();
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnCallOutbound = (Button) findViewById(R.id.btnCallOutbound);
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
     * 呼叫出库
     */
    private void callOutbound() {
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
                    // 生成任务编号
                    String outID = DateUtil.generateTaskNo("C");
                    
                    // 调用AGV接口创建出库任务
                    AgvResponse response = agvApiService.callOutbound(
                        binCode, swapStation, matCode, outID, OutboundActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null && response.isSuccess()) {
                                updateStatus(true);
                                Toast.makeText(OutboundActivity.this, 
                                    "呼叫出库成功，任务号：" + outID, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                String msg = response != null ? response.getMessage() : "呼叫出库失败";
                                Toast.makeText(OutboundActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "呼叫出库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    
                    // 调用AGV接口创建空托回库任务
                    AgvResponse response = agvApiService.returnPalletFromSwap(
                        swapStation, binCode, outID, OutboundActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null && response.isSuccess()) {
                                Toast.makeText(OutboundActivity.this, 
                                    "空托回库成功，任务号：" + outID, 
                                    Toast.LENGTH_LONG).show();
                            } else {
                                String msg = response != null ? response.getMessage() : "空托回库失败";
                                Toast.makeText(OutboundActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(OutboundActivity.this, "空托回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        if (requestCode == 400 && resultCode == RESULT_OK && data != null) {
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

