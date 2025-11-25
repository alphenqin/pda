package com.qs.qs5502demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Selection;
import android.text.Spannable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.qs.pda5502demo.R;

public class MainActivity extends Activity{
	
	private ScanBroadcastReceiver scanBroadcastReceiver;
	
	private EditText edText;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		
		edText=(EditText) findViewById(R.id.edText);
		
		findViewById(R.id.scanBtn).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				//模拟触发扫描按键，发出广播，开始扫描
				Intent intentBroadcast = new Intent();
				intentBroadcast.setAction("hbyapi.intent.key_scan_down");
				sendBroadcast(intentBroadcast);
				
			}
		});
		
		scanBroadcastReceiver = new ScanBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.qs.scancode");
		this.registerReceiver(scanBroadcastReceiver, intentFilter);
		
	}
	
	class ScanBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
		// 获取扫描数据
		String text1 = intent.getExtras().getString("data");
		// 把扫描信息set到编辑框
		edText.setText(text1);
		}
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		//退出时候注销广播接收器
		unregisterReceiver(scanBroadcastReceiver);
		super.onDestroy();
	}

}
