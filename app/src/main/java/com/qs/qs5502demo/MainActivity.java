package com.qs.qs5502demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.inbound.InboundActivity;
import com.qs.qs5502demo.outbound.OutboundActivity;
import com.qs.qs5502demo.returnwarehouse.ReturnWarehouseActivity;
import com.qs.qs5502demo.send.SendInspectionActivity;
import com.qs.qs5502demo.task.TaskManageActivity;
import com.qs.qs5502demo.util.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
	
	private TextView tvDateTime;
	private TextView tvUserInfo;
	private Handler handler;
	private Runnable updateTimeRunnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		tvDateTime = (TextView) findViewById(R.id.tvDateTime);
		tvUserInfo = (TextView) findViewById(R.id.tvUserInfo);
		
		// 初始化按钮点击事件
		findViewById(R.id.btnInbound).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, InboundActivity.class));
			}
		});
		
		findViewById(R.id.btnSendInspection).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, SendInspectionActivity.class));
			}
		});
		
		findViewById(R.id.btnReturn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, ReturnWarehouseActivity.class));
			}
		});
		
		findViewById(R.id.btnOutbound).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, OutboundActivity.class));
			}
		});
		
		findViewById(R.id.btnTaskManage).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, TaskManageActivity.class));
			}
		});
		
		// 启动时间更新
		handler = new Handler();
		updateTimeRunnable = new Runnable() {
			@Override
			public void run() {
				updateDateTime();
				handler.postDelayed(this, 1000);
			}
		};
		handler.post(updateTimeRunnable);
	}
	
	private void updateDateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		tvDateTime.setText(sdf.format(new Date()));
	}
	
	@Override
	protected void onDestroy() {
		if (handler != null && updateTimeRunnable != null) {
			handler.removeCallbacks(updateTimeRunnable);
		}
		super.onDestroy();
	}
}
