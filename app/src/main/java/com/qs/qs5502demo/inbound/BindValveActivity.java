package com.qs.qs5502demo.inbound;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.ApiClient;
import com.qs.qs5502demo.api.ApiService;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.util.DateUtil;

public class BindValveActivity extends Activity {
    
    private TextView tvPalletNoReadonly;
    private TextView tvLocationCodeReadonly;
    private EditText etInboundDate;
    private EditText etVendorName;
    private EditText etValveNo;
    private EditText etValveModel;
    private Button btnSubmit;
    private Button btnCancel;
    
    private ApiService apiService;
    
    private String palletNo;
    private String locationCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind_valve);
        
        apiService = new ApiClient();
        
        // 获取传入的参数
        palletNo = getIntent().getStringExtra("palletNo");
        locationCode = getIntent().getStringExtra("locationCode");
        
        if (palletNo == null || palletNo.isEmpty()) {
            Toast.makeText(this, "托盘号无效，请返回重新扫码", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupListeners();
        loadData();
    }
    
    private void initViews() {
        tvPalletNoReadonly = (TextView) findViewById(R.id.tvPalletNoReadonly);
        tvLocationCodeReadonly = (TextView) findViewById(R.id.tvLocationCodeReadonly);
        etInboundDate = (EditText) findViewById(R.id.etInboundDate);
        etVendorName = (EditText) findViewById(R.id.etVendorName);
        etValveNo = (EditText) findViewById(R.id.etValveNo);
        etValveModel = (EditText) findViewById(R.id.etValveModel);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnCancel = (Button) findViewById(R.id.btnCancel);
    }
    
    private void setupListeners() {
        btnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                submitBind();
            }
        });
    }
    
    private void loadData() {
        tvPalletNoReadonly.setText(palletNo);
        tvLocationCodeReadonly.setText(locationCode);
        
        // 默认入库日期为今天
        etInboundDate.setText(DateUtil.getCurrentDate());
    }
    
    /**
     * 提交绑定
     */
    private void submitBind() {
        // 表单校验
        String vendorName = etVendorName.getText().toString().trim();
        String valveNo = etValveNo.getText().toString().trim();
        String valveModel = etValveModel.getText().toString().trim();
        String inboundDate = etInboundDate.getText().toString().trim();
        
        if (vendorName.isEmpty()) {
            Toast.makeText(this, "请输入厂家名称", Toast.LENGTH_SHORT).show();
            etVendorName.requestFocus();
            return;
        }
        
        if (valveNo.isEmpty()) {
            Toast.makeText(this, "请输入阀门编号", Toast.LENGTH_SHORT).show();
            etValveNo.requestFocus();
            return;
        }
        
        if (valveModel.isEmpty()) {
            Toast.makeText(this, "请输入阀门型号", Toast.LENGTH_SHORT).show();
            etValveModel.requestFocus();
            return;
        }
        
        if (inboundDate.isEmpty()) {
            Toast.makeText(this, "请输入入库日期", Toast.LENGTH_SHORT).show();
            etInboundDate.requestFocus();
            return;
        }
        
        // 创建阀门对象
        Valve valve = new Valve();
        valve.setValveNo(valveNo);
        valve.setValveModel(valveModel);
        valve.setVendorName(vendorName);
        valve.setInboundDate(inboundDate);
        valve.setPalletNo(palletNo);
        valve.setLocationCode(locationCode);
        
        // 显示加载提示
        Toast.makeText(this, "正在绑定...", Toast.LENGTH_SHORT).show();
        
        // 在后台线程执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean success = apiService.bindValve(valve);
                    
                    // 更新UI
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(BindValveActivity.this, "绑定成功", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(BindValveActivity.this, "绑定失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(BindValveActivity.this, "绑定失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}

