package com.qs.qs5502demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.LoginRequest;
import com.qs.qs5502demo.model.LoginResponse;
import com.qs.qs5502demo.util.PreferenceUtil;

public class LoginActivity extends Activity {
    
    private EditText etUsername;
    private EditText etPassword;
    private EditText etDeviceCode;
    private Button btnLogin;
    private TextView tvError;
    private TextView tvConfig;
    
    private WmsApiService wmsApiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_login);
        
        // 检查是否已登录
        String token = PreferenceUtil.getToken(this);
        if (token != null && !token.isEmpty()) {
            // 已登录，直接跳转到主界面
            startMainActivity();
            return;
        }
        
        wmsApiService = new WmsApiService(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etDeviceCode = (EditText) findViewById(R.id.etDeviceCode);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        tvError = (TextView) findViewById(R.id.tvError);
        tvConfig = (TextView) findViewById(R.id.tvConfig);
        
        // 设置默认值
        etUsername.setText("admin");
        etPassword.setText("admin123");
        etDeviceCode.setText("PDA-01");
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });
        
        tvConfig.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开服务器配置界面
                Intent intent = new Intent(LoginActivity.this, ServerConfigActivity.class);
                startActivity(intent);
            }
        });
    }
    
    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String deviceCode = etDeviceCode.getText().toString().trim();
        
        if (username.isEmpty()) {
            showError("请输入用户名");
            return;
        }
        
        if (password.isEmpty()) {
            showError("请输入密码");
            return;
        }
        
        if (deviceCode.isEmpty()) {
            showError("请输入设备编码");
            return;
        }
        
        // 隐藏错误信息
        hideError();
        
        // 禁用登录按钮
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");
        
        // 创建登录请求
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setDeviceCode(deviceCode);
        
        // 在后台线程执行登录
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LoginResponse response = wmsApiService.login(request);
                    
                    // 切换到主线程更新UI
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            handleLoginResponse(response);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // 切换到主线程显示错误
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            handleLoginError(e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    
    private void handleLoginResponse(LoginResponse response) {
        btnLogin.setEnabled(true);
        btnLogin.setText("登录");
        
        if (response != null && response.getCode() == 200 && response.getData() != null) {
            // 登录成功，保存token和用户信息
            String token = response.getData().getToken();
            String userName = response.getData().getUserName();
            
            PreferenceUtil.saveToken(this, token);
            PreferenceUtil.saveUserName(this, userName);
            
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            
            // 跳转到主界面
            startMainActivity();
        } else {
            // 登录失败
            String message = response != null ? response.getMessage() : "登录失败";
            showError(message);
        }
    }
    
    private void handleLoginError(String errorMessage) {
        btnLogin.setEnabled(true);
        btnLogin.setText("登录");
        showError(errorMessage != null ? errorMessage : "网络请求失败，请检查网络连接");
    }
    
    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
    
    private void hideError() {
        tvError.setVisibility(View.GONE);
    }
    
    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

