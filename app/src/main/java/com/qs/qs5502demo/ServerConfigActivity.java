package com.qs.qs5502demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.ApiConfig;

public class ServerConfigActivity extends Activity {
    
    private EditText etWmsUrl;
    private EditText etAgvUrl;
    private Button btnSave;
    private Button btnReset;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_server_config);
        
        initViews();
        loadConfig();
        setupListeners();
    }
    
    private void initViews() {
        etWmsUrl = (EditText) findViewById(R.id.etWmsUrl);
        etAgvUrl = (EditText) findViewById(R.id.etAgvUrl);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnReset = (Button) findViewById(R.id.btnReset);
    }
    
    /**
     * 加载配置，首次打开显示默认值
     */
    private void loadConfig() {
        // 初始化ApiConfig
        ApiConfig.init(this);
        
        // 从SharedPreferences读取保存的配置
        String savedWmsUrl = com.qs.qs5502demo.util.PreferenceUtil.getWmsBaseUrl(this);
        String savedAgvUrl = com.qs.qs5502demo.util.PreferenceUtil.getAgvBaseUrl(this);
        
        // 如果有保存的配置，显示保存的值；否则显示默认值
        if (savedWmsUrl != null && !savedWmsUrl.isEmpty()) {
            etWmsUrl.setText(savedWmsUrl);
        } else {
            etWmsUrl.setText(ApiConfig.getDefaultWmsBaseUrl());
        }
        
        if (savedAgvUrl != null && !savedAgvUrl.isEmpty()) {
            etAgvUrl.setText(savedAgvUrl);
        } else {
            etAgvUrl.setText(ApiConfig.getDefaultAgvBaseUrl());
        }

    }
    
    private void setupListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConfig();
            }
        });
        
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToDefault();
            }
        });
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        String wmsUrl = etWmsUrl.getText().toString().trim();
        String agvUrl = etAgvUrl.getText().toString().trim();
        
        // 验证URL格式
        if (wmsUrl.isEmpty()) {
            Toast.makeText(this, "WMS服务器地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (agvUrl.isEmpty()) {
            Toast.makeText(this, "AGV服务器地址不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 简单的URL格式验证
        if (!wmsUrl.startsWith("http://") && !wmsUrl.startsWith("https://")) {
            Toast.makeText(this, "WMS服务器地址格式不正确，应以http://或https://开头", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!agvUrl.startsWith("http://") && !agvUrl.startsWith("https://")) {
            Toast.makeText(this, "AGV服务器地址格式不正确，应以http://或https://开头", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 保存配置
        ApiConfig.setWmsBaseUrl(this, wmsUrl);
        ApiConfig.setAgvBaseUrl(this, agvUrl);
        Toast.makeText(this, "配置保存成功", Toast.LENGTH_SHORT).show();
        
        // 延迟关闭，让用户看到成功提示
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1000);
    }
    
    /**
     * 重置为默认值
     */
    private void resetToDefault() {
        etWmsUrl.setText(ApiConfig.getDefaultWmsBaseUrl());
        etAgvUrl.setText(ApiConfig.getDefaultAgvBaseUrl());
        Toast.makeText(this, "已重置为默认值", Toast.LENGTH_SHORT).show();
    }
}

