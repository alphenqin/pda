package com.qs.qs5502demo.emptypallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.OutsideEmptyPallet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmptyPalletReturnActivity extends Activity {

    private Button btnBack;
    private Button btnRefresh;
    private Button btnSubmit;
    private CheckBox cbSelectAll;
    private LinearLayout listContainer;
    private WmsApiService wmsApiService;

    private final Map<Long, CheckBox> checkBoxMap = new LinkedHashMap<>();
    private final Map<Long, OutsideEmptyPallet> recordMap = new LinkedHashMap<>();
    private boolean loading = false;
    private boolean submitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_empty_pallet_return);

        wmsApiService = new WmsApiService(this);
        btnBack = (Button) findViewById(R.id.btnBack);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        cbSelectAll = (CheckBox) findViewById(R.id.cbSelectAll);
        listContainer = (LinearLayout) findViewById(R.id.listContainer);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadOutsideEmptyPallets();
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSubmit();
            }
        });
        cbSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (CheckBox checkBox : checkBoxMap.values()) {
                    checkBox.setChecked(isChecked);
                }
            }
        });

        loadOutsideEmptyPallets();
    }

    private void loadOutsideEmptyPallets() {
        if (loading) {
            return;
        }
        loading = true;
        setButtonsEnabled(false);
        listContainer.removeAllViews();
        TextView loadingView = buildText("正在加载库外空托盘...");
        listContainer.addView(loadingView);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<OutsideEmptyPallet> records = wmsApiService.listOutsideEmptyPallets(EmptyPalletReturnActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading = false;
                            setButtonsEnabled(true);
                            renderList(records);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading = false;
                            setButtonsEnabled(true);
                            listContainer.removeAllViews();
                            listContainer.addView(buildText("加载失败：" + e.getMessage()));
                            Toast.makeText(EmptyPalletReturnActivity.this, "加载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void renderList(List<OutsideEmptyPallet> records) {
        checkBoxMap.clear();
        recordMap.clear();
        cbSelectAll.setChecked(false);
        listContainer.removeAllViews();
        if (records == null || records.isEmpty()) {
            listContainer.addView(buildText("暂无待回库空托盘"));
            return;
        }
        for (OutsideEmptyPallet record : records) {
            if (record == null || record.getId() == null) {
                continue;
            }
            recordMap.put(record.getId(), record);
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(buildRecordText(record));
            checkBox.setTextSize(17);
            checkBox.setPadding(12, 14, 12, 14);
            listContainer.addView(checkBox);
            checkBoxMap.put(record.getId(), checkBox);
        }
    }

    private String buildRecordText(OutsideEmptyPallet record) {
        String source = "OUTBOUND".equalsIgnoreCase(record.getSourceType()) ? "出库" : "送检";
        String status = "FAILED".equalsIgnoreCase(record.getStatus()) ? "（失败可重试）" : "";
        return "库外站点：" + record.getStationCode()
            + "\n原库位：" + displayText(record.getTargetBinCode())
            + "\n回库目标：提交时自动分配"
            + "\n来源：" + source + status;
    }

    private String displayText(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private TextView buildText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18);
        textView.setPadding(16, 20, 16, 20);
        return textView;
    }

    private void confirmSubmit() {
        List<Long> ids = collectSelectedIds();
        if (ids.isEmpty()) {
            Toast.makeText(this, "请选择库外站点", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("确认空托回库")
            .setMessage("已选择 " + ids.size() + " 个库外空托盘，确认下发回库任务？")
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    submitReturn(ids);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private List<Long> collectSelectedIds() {
        List<Long> ids = new ArrayList<>();
        for (Map.Entry<Long, CheckBox> entry : checkBoxMap.entrySet()) {
            if (entry.getValue().isChecked()) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    private void submitReturn(List<Long> ids) {
        if (submitting) {
            Toast.makeText(this, "空托回库任务提交中，请稍后", Toast.LENGTH_SHORT).show();
            return;
        }
        submitting = true;
        setButtonsEnabled(false);
        Toast.makeText(this, "正在提交空托回库任务...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int accepted = wmsApiService.returnOutsideEmptyPallets(ids, EmptyPalletReturnActivity.this);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            submitting = false;
                            setButtonsEnabled(true);
                            Toast.makeText(EmptyPalletReturnActivity.this,
                                "已提交 " + accepted + " 个空托回库任务", Toast.LENGTH_LONG).show();
                            loadOutsideEmptyPallets();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            submitting = false;
                            setButtonsEnabled(true);
                            Toast.makeText(EmptyPalletReturnActivity.this,
                                "提交失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnRefresh.setEnabled(enabled);
        btnSubmit.setEnabled(enabled);
        cbSelectAll.setEnabled(enabled);
        btnRefresh.setAlpha(enabled ? 1.0f : 0.4f);
        btnSubmit.setAlpha(enabled ? 1.0f : 0.4f);
        cbSelectAll.setAlpha(enabled ? 1.0f : 0.4f);
    }
}
