package com.qs.qs5502demo.returnwarehouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.AvailableBin;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.TaskLockStatus;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.send.SelectValveActivity;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.util.HashMap;
import java.util.Map;

public class ReturnWarehouseActivity extends Activity {
    
    private TextView tvPalletNo;
    private TextView tvLocationCode;
    private TextView tvReturnStation;
    private TextView tvStorageFloor;
    private View viewStatus;
    private Button btnSelectReturnStation;
    private Button btnSelectValve;
    private Button btnValveReturn;
    private Button btnBack;
    private CharSequence valveReturnLabel;
    
    private WmsApiService wmsApiService;
    
    private String palletNo;
    private String binCode;
    private String oldBinCode;
    private String returnOutsideSite;
    private String matCode;
    private String inspectionTargetBin;
    private Integer storageLevel;
    private Valve selectedValve;
    private static final String PALLET_TYPE_SMALL = "t1";
    private static final String PALLET_TYPE_LARGE = "t2";
    private static final int BIN_TYPE_SMALL_PALLET = 1;
    private static final int BIN_TYPE_LARGE_PALLET = 2;
    private static final String SMALL_BUFFER_BIN = "B3-15-01";
    private static final String LARGE_BUFFER_BIN = "B3-14-01";
    private static final String SMALL_DOCK_BIN = "D2-小托盘接驳点";
    private static final String LARGE_DOCK_BIN = "D2-大托盘接驳点";
    private static final String SMALL_RETURN_OUTSIDE_SITE_1 = "Z1-装卸点";
    private static final String SMALL_RETURN_OUTSIDE_SITE_2 = "Z2-装卸点";
    private static final String SMALL_RETURN_OUTSIDE_SITE_3 = "Z3-装卸点";
    private static final String SMALL_RETURN_OUTSIDE_SITE_4 = "Z4-装卸点";
    private static final String LARGE_RETURN_OUTSIDE_SITE = "Z5-装卸点";
    private static final long LOCK_POLL_INTERVAL_MS = 5000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable lockStatusRunnable;
    private long lastLockStatusErrorAt = 0L;
    private boolean returnValveLocked = false;
    private boolean valveReturnInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_return);
        
        wmsApiService = new WmsApiService(this);
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        tvPalletNo = (TextView) findViewById(R.id.tvPalletNo);
        tvLocationCode = (TextView) findViewById(R.id.tvLocationCode);
        tvReturnStation = (TextView) findViewById(R.id.tvReturnStation);
        tvStorageFloor = (TextView) findViewById(R.id.tvStorageFloor);
        viewStatus = findViewById(R.id.viewStatus);
        btnSelectReturnStation = (Button) findViewById(R.id.btnSelectReturnStation);
        btnSelectValve = (Button) findViewById(R.id.btnSelectValve);
        btnValveReturn = (Button) findViewById(R.id.btnValveReturn);
        btnBack = (Button) findViewById(R.id.btnBack);
        valveReturnLabel = btnValveReturn.getText();
        
        updateStatus(false);
        updateButtonLocks();
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
                if (returnValveLocked || valveReturnInProgress) {
                    Toast.makeText(ReturnWarehouseActivity.this, "样品回库进行中，请稍后再操作", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 跳转到选阀门页面
                Intent intent = new Intent(ReturnWarehouseActivity.this, SelectValveActivity.class);
                intent.putExtra("taskType", "RETURN");
                startActivityForResult(intent, 300);
            }
        });

        btnSelectReturnStation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (returnValveLocked || valveReturnInProgress) {
                    Toast.makeText(ReturnWarehouseActivity.this, "样品回库进行中，请稍后再操作", Toast.LENGTH_SHORT).show();
                    return;
                }
                showReturnStationDialog();
            }
        });
        
        btnValveReturn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                callValveReturn();
            }
        });
    }
    
    /**
     * 阀门回库
     */
    private void callValveReturn() {
        if (selectedValve == null) {
            Toast.makeText(this, "请先选择出厂编号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isBlank(returnOutsideSite) || storageLevel == null) {
            Toast.makeText(this, "请先选择库外站点/存放库位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isBlank(binCode)) {
            Toast.makeText(this, "未获取到可用库位", Toast.LENGTH_SHORT).show();
            return;
        }
        if (returnValveLocked || valveReturnInProgress) {
            Toast.makeText(this, "样品回库进行中，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        showValveReturnConfirm();
    }

    private void showValveReturnConfirm() {
        new AlertDialog.Builder(this)
            .setTitle("确认样品回库")
            .setMessage("出厂编号：" + selectedValve.getValveNo()
                + "\n库外站点：" + returnOutsideSite
                + "\n原库位：" + displayText(oldBinCode)
                + "\n新库位：" + binCode
                + "\n存放库位：" + getStorageFloorText())
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performValveReturn();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行阀门回库
     */
    private void performValveReturn() {
        Toast.makeText(this, "正在创建回库任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String effectivePalletNo = getEffectivePalletNo();
                    String palletType = resolvePalletTypeCodeByBinType(selectedValve == null ? null : selectedValve.getBinType());
                    if (palletType == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "无法识别目标库位类型", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    if (isBlank(returnOutsideSite)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "请先选择库外站点", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    inspectionTargetBin = returnOutsideSite;
                    valveReturnInProgress = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });

                    String outId = DateUtil.generateTaskNo("H");
                    Map<String, String> params = new HashMap<>();
                    params.put("taskType", Task.TYPE_RETURN);
                    params.put("outID", outId);
                    params.put("deviceCode", PreferenceUtil.getDeviceCode(ReturnWarehouseActivity.this));
                    params.put("palletNo", effectivePalletNo);
                    params.put("fromBinCode", returnOutsideSite);
                    params.put("toBinCode", binCode);
                    params.put("storageLevel", String.valueOf(storageLevel));
                    if (matCode != null) {
                        params.put("matCode", matCode);
                    }
                    params.put("remark", "VALVE_RETURN");
                    if (selectedValve != null && selectedValve.getValveNo() != null) {
                        params.put("valveNo", selectedValve.getValveNo());
                    }
                    TaskDispatchResult result = wmsApiService.dispatchTask(params, ReturnWarehouseActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                String taskNo = result.getOutID() != null ? result.getOutID() : outId;
                                resetReturnFormAfterQueue();
                                Toast.makeText(ReturnWarehouseActivity.this, 
                                    "样品回库任务已下发，任务号：" + taskNo,
                                    Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(ReturnWarehouseActivity.this, "样品回库下发失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    if (result == null) {
                        valveReturnInProgress = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateButtonLocks();
                            }
                        });
                        return;
                    }

                    valveReturnInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ReturnWarehouseActivity.this, "样品回库失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    valveReturnInProgress = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateButtonLocks();
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

    private void updateButtonLocks() {
        boolean valveReturnLocked = returnValveLocked;
        boolean valveReturnEnabled = !valveReturnLocked && !valveReturnInProgress;
        boolean selectionEnabled = valveReturnEnabled;
        btnSelectReturnStation.setEnabled(selectionEnabled);
        btnSelectValve.setEnabled(selectionEnabled);
        btnSelectReturnStation.setAlpha(selectionEnabled ? 1.0f : 0.4f);
        btnSelectValve.setAlpha(selectionEnabled ? 1.0f : 0.4f);
        btnValveReturn.setEnabled(valveReturnEnabled);
        if (valveReturnEnabled) {
            btnValveReturn.setAlpha(1.0f);
            btnValveReturn.setText(valveReturnLabel);
        } else {
            btnValveReturn.setAlpha(0.4f);
            if (valveReturnLocked) {
                btnValveReturn.setText(valveReturnLabel + "（回库中）");
            } else if (valveReturnInProgress) {
                btnValveReturn.setText(valveReturnLabel + "（处理中）");
            } else {
                btnValveReturn.setText(valveReturnLabel + "（锁定中）");
            }
        }
    }

    private void showReturnStationDialog() {
        String[] stations = getReturnStationOptions();
        int selectedIndex = -1;
        for (int i = 0; i < stations.length; i++) {
            if (stations[i].equals(returnOutsideSite)) {
                selectedIndex = i;
                break;
            }
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 8, 24, 0);

        TextView stationTitle = new TextView(this);
        stationTitle.setText("库外站点");
        stationTitle.setTextSize(16);
        content.addView(stationTitle);

        RadioGroup stationGroup = new RadioGroup(this);
        stationGroup.setOrientation(RadioGroup.VERTICAL);
        final int stationIdBase = 1000;
        for (int i = 0; i < stations.length; i++) {
            RadioButton item = new RadioButton(this);
            item.setId(stationIdBase + i);
            item.setText(formatReturnStationLabel(stations[i]));
            item.setTextSize(16);
            stationGroup.addView(item);
            if (i == selectedIndex) {
                stationGroup.check(item.getId());
            }
        }
        content.addView(stationGroup);

        TextView floorTitle = new TextView(this);
        floorTitle.setText("存放库位");
        floorTitle.setTextSize(16);
        floorTitle.setPadding(0, 16, 0, 0);
        content.addView(floorTitle);

        RadioGroup floorGroup = new RadioGroup(this);
        floorGroup.setOrientation(RadioGroup.VERTICAL);
        final int firstFloorId = 2001;
        final int secondFloorId = 2002;
        final int thirdFloorId = 2003;
        RadioButton firstFloor = new RadioButton(this);
        firstFloor.setId(firstFloorId);
        firstFloor.setText("存放在一层");
        firstFloor.setTextSize(16);
        floorGroup.addView(firstFloor);
        RadioButton secondFloor = new RadioButton(this);
        secondFloor.setId(secondFloorId);
        secondFloor.setText("存放在二层");
        secondFloor.setTextSize(16);
        floorGroup.addView(secondFloor);
        RadioButton thirdFloor = new RadioButton(this);
        thirdFloor.setId(thirdFloorId);
        thirdFloor.setText("存放在三层");
        thirdFloor.setTextSize(16);
        floorGroup.addView(thirdFloor);
        if (storageLevel != null) {
            if (storageLevel == 1) {
                floorGroup.check(firstFloorId);
            } else if (storageLevel == 2) {
                floorGroup.check(secondFloorId);
            } else if (storageLevel == 3) {
                floorGroup.check(thirdFloorId);
            }
        }
        content.addView(floorGroup);

        new AlertDialog.Builder(this)
            .setTitle("选择库外站点")
            .setView(content)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    int stationCheckedId = stationGroup.getCheckedRadioButtonId();
                    int stationIndex = stationCheckedId - stationIdBase;
                    int floorCheckedId = floorGroup.getCheckedRadioButtonId();
                    if (stationIndex < 0 || stationIndex >= stations.length) {
                        Toast.makeText(ReturnWarehouseActivity.this, "请选择库外站点", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (floorCheckedId != firstFloorId && floorCheckedId != secondFloorId && floorCheckedId != thirdFloorId) {
                        Toast.makeText(ReturnWarehouseActivity.this, "请选择存放库位", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    returnOutsideSite = stations[stationIndex];
                    storageLevel = floorCheckedId == firstFloorId ? 1 : (floorCheckedId == secondFloorId ? 2 : 3);
                    tvReturnStation.setText(formatReturnStationLabel(returnOutsideSite));
                    tvStorageFloor.setText(getStorageFloorText());
                    if (selectedValve == null) {
                        binCode = null;
                        tvLocationCode.setText("--");
                        updateStatus(false);
                    } else {
                        fetchAvailableBinForReturnSelection();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void fetchAvailableBinForReturnSelection() {
        String palletType = resolvePalletTypeCodeByBinType(selectedValve == null ? null : selectedValve.getBinType());
        if (palletType == null) {
            Toast.makeText(this, "无法识别原库位类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isReturnStationAllowed(returnOutsideSite, palletType)) {
            Toast.makeText(this, "当前样品托盘类型不支持该库外站点", Toast.LENGTH_SHORT).show();
            returnOutsideSite = null;
            storageLevel = null;
            binCode = null;
            tvReturnStation.setText("未选择");
            tvStorageFloor.setText("未选择");
            tvLocationCode.setText("--");
            updateStatus(false);
            return;
        }

        Toast.makeText(this, "正在获取可用库位...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AvailableBin availableBin = wmsApiService.getAvailableBin(palletType, storageLevel,
                        returnOutsideSite, ReturnWarehouseActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (availableBin == null || isBlank(availableBin.getBinCode())) {
                                binCode = null;
                                tvLocationCode.setText("--");
                                updateStatus(false);
                                Toast.makeText(ReturnWarehouseActivity.this, "未获取到可用库位", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            binCode = availableBin.getBinCode();
                            palletNo = binCode;
                            tvPalletNo.setText(displayText(selectedValve != null ? selectedValve.getValveNo() : null));
                            tvLocationCode.setText(binCode);
                            updateStatus(true);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binCode = null;
                            tvLocationCode.setText("--");
                            updateStatus(false);
                            Toast.makeText(ReturnWarehouseActivity.this, "查询库位失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String[] getReturnStationOptions() {
        String palletType = resolvePalletTypeCodeByBinType(selectedValve == null ? null : selectedValve.getBinType());
        if (PALLET_TYPE_LARGE.equalsIgnoreCase(palletType)) {
            return new String[]{LARGE_RETURN_OUTSIDE_SITE};
        }
        if (PALLET_TYPE_SMALL.equalsIgnoreCase(palletType)) {
            return new String[]{
                SMALL_RETURN_OUTSIDE_SITE_1,
                SMALL_RETURN_OUTSIDE_SITE_2,
                SMALL_RETURN_OUTSIDE_SITE_3,
                SMALL_RETURN_OUTSIDE_SITE_4
            };
        }
        return new String[]{
            SMALL_RETURN_OUTSIDE_SITE_1,
            SMALL_RETURN_OUTSIDE_SITE_2,
            SMALL_RETURN_OUTSIDE_SITE_3,
            SMALL_RETURN_OUTSIDE_SITE_4,
            LARGE_RETURN_OUTSIDE_SITE
        };
    }

    private String formatReturnStationLabel(String station) {
        if (isBlank(station)) {
            return "未选择";
        }
        if (SMALL_RETURN_OUTSIDE_SITE_1.equals(station)
            || SMALL_RETURN_OUTSIDE_SITE_2.equals(station)
            || SMALL_RETURN_OUTSIDE_SITE_3.equals(station)
            || SMALL_RETURN_OUTSIDE_SITE_4.equals(station)) {
            return station + "(小托盘)";
        }
        if (LARGE_RETURN_OUTSIDE_SITE.equals(station)) {
            return station + "(大托盘)";
        }
        return station;
    }

    private boolean isReturnStationAllowed(String station, String palletType) {
        if (PALLET_TYPE_LARGE.equalsIgnoreCase(palletType)) {
            return LARGE_RETURN_OUTSIDE_SITE.equals(station);
        }
        if (PALLET_TYPE_SMALL.equalsIgnoreCase(palletType)) {
            return SMALL_RETURN_OUTSIDE_SITE_1.equals(station)
                || SMALL_RETURN_OUTSIDE_SITE_2.equals(station)
                || SMALL_RETURN_OUTSIDE_SITE_3.equals(station)
                || SMALL_RETURN_OUTSIDE_SITE_4.equals(station);
        }
        return false;
    }

    private String getStorageFloorText() {
        if (storageLevel == null) {
            return "未选择";
        }
        if (storageLevel == 1) {
            return "一层";
        }
        if (storageLevel == 2) {
            return "二层";
        }
        if (storageLevel == 3) {
            return "三层";
        }
        return "未选择";
    }

    private void resetReturnFormAfterQueue() {
        selectedValve = null;
        palletNo = null;
        binCode = null;
        oldBinCode = null;
        returnOutsideSite = null;
        storageLevel = null;
        matCode = null;
        inspectionTargetBin = null;
        tvPalletNo.setText("--");
        tvLocationCode.setText("--");
        tvReturnStation.setText("未选择");
        tvStorageFloor.setText("未选择");
        updateStatus(false);
    }

    private void startLockStatusPolling() {
        if (lockStatusRunnable != null) {
            return;
        }
        lockStatusRunnable = new Runnable() {
            @Override
            public void run() {
                refreshLockStatus();
                handler.postDelayed(this, LOCK_POLL_INTERVAL_MS);
            }
        };
        handler.post(lockStatusRunnable);
    }

    private void stopLockStatusPolling() {
        if (lockStatusRunnable != null) {
            handler.removeCallbacks(lockStatusRunnable);
            lockStatusRunnable = null;
        }
    }

    private void refreshLockStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TaskLockStatus status = wmsApiService.getTaskLockStatus(ReturnWarehouseActivity.this);
                    boolean valveLocked = status != null && status.isReturnValveLocked();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            returnValveLocked = valveLocked;
                            updateButtonLocks();
                        }
                    });
                } catch (Exception e) {
                    long now = System.currentTimeMillis();
                    if (now - lastLockStatusErrorAt > 30000L) {
                        lastLockStatusErrorAt = now;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReturnWarehouseActivity.this, "锁状态刷新失败，请检查网络/服务", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private String resolvePalletTypeCodeByBinType(Integer binType) {
        if (binType == null) {
            return null;
        }
        if (binType == BIN_TYPE_LARGE_PALLET) {
            return PALLET_TYPE_LARGE;
        }
        if (binType == BIN_TYPE_SMALL_PALLET) {
            return PALLET_TYPE_SMALL;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            // 获取选中的阀门信息
            selectedValve = (Valve) data.getSerializableExtra("valve");
            if (selectedValve != null) {
                palletNo = trimToNull(selectedValve.getPalletNo());
                oldBinCode = trimToNull(selectedValve.getBinCode());
                if (oldBinCode == null) {
                    oldBinCode = trimToNull(selectedValve.getRemark());
                }
                binCode = null;
                matCode = trimToNull(selectedValve.getMatCode());
                inspectionTargetBin = selectedValve.getInspectionTargetBin();

                tvPalletNo.setText(displayText(selectedValve.getValveNo()));
                tvLocationCode.setText("--");
                updateStatus(false);
                if (isBlank(returnOutsideSite) || storageLevel == null) {
                    tvReturnStation.setText("未选择");
                    tvStorageFloor.setText("未选择");
                } else {
                    tvReturnStation.setText(formatReturnStationLabel(returnOutsideSite));
                    tvStorageFloor.setText(getStorageFloorText());
                    fetchAvailableBinForReturnSelection();
                }
            }
        }
    }

    private String getEffectivePalletNo() {
        if (!isBlank(palletNo)) {
            return palletNo;
        }
        if (!isBlank(oldBinCode)) {
            return oldBinCode;
        }
        return binCode;
    }

    private String displayText(String value) {
        return isBlank(value) ? "--" : value;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLockStatusPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLockStatusPolling();
    }
}

