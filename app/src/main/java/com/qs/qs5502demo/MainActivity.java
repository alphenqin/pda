package com.qs.qs5502demo;

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
import com.qs.qs5502demo.inbound.InboundActivity;
import com.qs.qs5502demo.outbound.OutboundActivity;
import com.qs.qs5502demo.returnwarehouse.ReturnWarehouseActivity;
import com.qs.qs5502demo.send.SendInspectionActivity;
import com.qs.qs5502demo.task.TaskManageActivity;
import com.qs.qs5502demo.model.InboundLockStatus;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
	
	private TextView tvDateTime;
	private TextView tvUserInfo;
	private Button btnLogout;
	private Button btnInbound;
	private Button btnSendInspection;
	private Button btnReturn;
	private Button btnOutbound;
	private CharSequence inboundLabel;
	private CharSequence sendInspectionLabel;
	private CharSequence returnLabel;
	private CharSequence outboundLabel;
	private Handler handler;
	private Runnable updateTimeRunnable;
	private Runnable inboundLockRunnable;
	private WmsApiService wmsApiService;
	private boolean inboundLocked = false;
	private boolean inspectionLocked = false;
	private static final long INBOUND_LOCK_POLL_MS = 5000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 检查是否已登录
		String token = PreferenceUtil.getToken(this);
		if (token == null || token.isEmpty()) {
			// 未登录，跳转到登录页面
			startActivity(new Intent(this, com.qs.qs5502demo.LoginActivity.class));
			finish();
			return;
		}
		
		setContentView(R.layout.activity_main);
		
		tvDateTime = (TextView) findViewById(R.id.tvDateTime);
		tvUserInfo = (TextView) findViewById(R.id.tvUserInfo);
		btnLogout = (Button) findViewById(R.id.btnLogout);
		btnInbound = (Button) findViewById(R.id.btnInbound);
		btnSendInspection = (Button) findViewById(R.id.btnSendInspection);
		btnReturn = (Button) findViewById(R.id.btnReturn);
		btnOutbound = (Button) findViewById(R.id.btnOutbound);
		inboundLabel = btnInbound.getText();
		sendInspectionLabel = btnSendInspection.getText();
		returnLabel = btnReturn.getText();
		outboundLabel = btnOutbound.getText();
		wmsApiService = new WmsApiService(this);
		
		// 显示用户名
		String userName = PreferenceUtil.getUserName(this);
		if (userName != null && !userName.isEmpty()) {
			tvUserInfo.setText(userName);
		}
		
		// 退出按钮点击事件
		btnLogout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showLogoutDialog();
			}
		});
		
		// 初始化按钮点击事件
		btnInbound.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				navigateIfUnlocked(InboundActivity.class);
			}
		});
		
		btnSendInspection.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				navigateIfUnlocked(SendInspectionActivity.class);
			}
		});
		
		btnReturn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				navigateIfUnlocked(ReturnWarehouseActivity.class);
			}
		});
		
		btnOutbound.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				navigateIfUnlocked(OutboundActivity.class);
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

		startInboundLockPolling();
	}
	
	private void updateDateTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		// 设置为东八区（Asia/Shanghai）
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
		tvDateTime.setText(sdf.format(new Date()));
	}

	private void navigateIfUnlocked(Class<?> targetActivity) {
		if (targetActivity == InboundActivity.class && inspectionLocked) {
			Toast.makeText(MainActivity.this, "送检任务执行中，请稍后再试", Toast.LENGTH_SHORT).show();
			return;
		}
		if (targetActivity != InboundActivity.class && inboundLocked) {
			Toast.makeText(MainActivity.this, "入库任务执行中，请稍后再试", Toast.LENGTH_SHORT).show();
			return;
		}
		if ((targetActivity == ReturnWarehouseActivity.class || targetActivity == OutboundActivity.class)
			&& inspectionLocked) {
			Toast.makeText(MainActivity.this, "送检任务执行中，请稍后再试", Toast.LENGTH_SHORT).show();
			return;
		}
		startActivity(new Intent(MainActivity.this, targetActivity));
	}

	private void startInboundLockPolling() {
		inboundLockRunnable = new Runnable() {
			@Override
			public void run() {
				refreshInboundLockStatus();
				handler.postDelayed(this, INBOUND_LOCK_POLL_MS);
			}
		};
		handler.post(inboundLockRunnable);
	}

	private void refreshInboundLockStatus() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					InboundLockStatus inboundStatus = wmsApiService.getInboundLockStatus(MainActivity.this);
					InboundLockStatus inspectionStatus = wmsApiService.getInspectionLockStatus(MainActivity.this);
					boolean inbound = inboundStatus != null && inboundStatus.isLocked();
					boolean inspection = inspectionStatus != null && inspectionStatus.isLocked();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							applyLocks(inbound, inspection);
						}
					});
				} catch (Exception e) {
					// Keep current state on error.
				}
			}
		}).start();
	}

	private void applyLocks(boolean inbound, boolean inspection) {
		boolean stateChanged = inboundLocked != inbound || inspectionLocked != inspection;
		inboundLocked = inbound;
		inspectionLocked = inspection;
		if (stateChanged) {
			String message;
			if (inboundLocked && inspectionLocked) {
				message = "入库/送检任务执行中，入库/回库/出库已锁定";
			} else if (inboundLocked) {
				message = "入库任务执行中，送检/回库/出库已锁定";
			} else if (inspectionLocked) {
				message = "送检任务执行中，入库/回库/出库已锁定";
			} else {
				message = "任务已完成，按钮已解锁";
			}
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
		}

		updateLockedButton(btnInbound, inboundLabel, !inspectionLocked, inspectionLocked ? "送检锁定" : null);
		updateLockedButton(btnSendInspection, sendInspectionLabel, !inboundLocked, inboundLocked ? "入库锁定" : null);
		boolean returnEnabled = !inboundLocked && !inspectionLocked;
		String returnReason = inboundLocked ? "入库锁定" : (inspectionLocked ? "送检锁定" : null);
		updateLockedButton(btnReturn, returnLabel, returnEnabled, returnReason);
		boolean outboundEnabled = !inboundLocked && !inspectionLocked;
		String outboundReason = inboundLocked ? "入库锁定" : (inspectionLocked ? "送检锁定" : null);
		updateLockedButton(btnOutbound, outboundLabel, outboundEnabled, outboundReason);
	}

	private void updateLockedButton(Button button, CharSequence label, boolean enabled, String reason) {
		button.setEnabled(enabled);
		if (enabled) {
			button.setAlpha(1.0f);
			button.setText(label);
		} else {
			button.setAlpha(0.4f);
			if (reason != null) {
				button.setText(label + "（" + reason + "）");
			} else {
				button.setText(label + "（锁定）");
			}
		}
	}
	
	/**
	 * 显示退出登录确认对话框
	 */
	private void showLogoutDialog() {
		new AlertDialog.Builder(this)
			.setTitle("退出登录")
			.setMessage("确定要退出登录吗？")
			.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(android.content.DialogInterface dialog, int which) {
					logout();
				}
			})
			.setNegativeButton("取消", null)
			.show();
	}
	
	/**
	 * 执行退出登录
	 */
	private void logout() {
		// 清除保存的用户信息
		PreferenceUtil.clearAll(this);
		
		Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
		
		// 跳转到登录界面
		Intent intent = new Intent(this, LoginActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		finish();
	}
	
	@Override
	protected void onDestroy() {
		if (handler != null && updateTimeRunnable != null) {
			handler.removeCallbacks(updateTimeRunnable);
		}
		if (handler != null && inboundLockRunnable != null) {
			handler.removeCallbacks(inboundLockRunnable);
		}
		super.onDestroy();
	}
}
