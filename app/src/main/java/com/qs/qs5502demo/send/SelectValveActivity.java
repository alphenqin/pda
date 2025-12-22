package com.qs.qs5502demo.send;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Valve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

public class SelectValveActivity extends Activity {
    
    private EditText etVendorName;
    private EditText etValveNo;
    private EditText etValveModel;
    private EditText etInboundDate;
    private Button btnSearch;
    private Button btnConfirm;
    private Button btnBack;
    private RecyclerView rvValveList;
    
    private WmsApiService wmsApiService;
    private ValveAdapter adapter;
    private List<Valve> valveList;
    private Valve selectedValve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_select_valve);
        
        wmsApiService = new WmsApiService(this);
        valveList = new ArrayList<>();
        
        initViews();
        setupListeners();
    }
    
    private void initViews() {
        etVendorName = (EditText) findViewById(R.id.etVendorName);
        etValveNo = (EditText) findViewById(R.id.etValveNo);
        etValveModel = (EditText) findViewById(R.id.etValveModel);
        etInboundDate = (EditText) findViewById(R.id.etInboundDate);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnBack = (Button) findViewById(R.id.btnBack);
        rvValveList = (RecyclerView) findViewById(R.id.rvValveList);
        
        // 设置RecyclerView
        rvValveList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ValveAdapter();
        rvValveList.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchValves();
            }
        });
        
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmSelection();
            }
        });
    }
    
    /**
     * 查询阀门
     */
    private void searchValves() {
        // 构建查询参数
        final Map<String, String> params = new HashMap<String, String>();
        String vendorName = etVendorName.getText().toString().trim();
        String valveNo = etValveNo.getText().toString().trim();
        String valveModel = etValveModel.getText().toString().trim();
        String inboundDate = etInboundDate.getText().toString().trim();
        
        if (!vendorName.isEmpty()) {
            params.put("vendorName", vendorName);
        }
        if (!valveNo.isEmpty()) {
            params.put("valveNo", valveNo);
        }
        if (!valveModel.isEmpty()) {
            params.put("valveModel", valveModel);
        }
        if (!inboundDate.isEmpty()) {
            params.put("inboundDate", inboundDate);
        }
        
        // 添加分页参数
        params.put("pageNum", "1");
        params.put("pageSize", "20");
        
        // 根据任务类型设置阀门状态筛选
        String taskType = getIntent().getStringExtra("taskType");
        if (taskType != null) {
            if ("SEND_INSPECTION".equals(taskType) || "OUTBOUND".equals(taskType)) {
                params.put("valveStatus", "IN_STOCK"); // 送检和出库只查询在库的
            } else if ("RETURN".equals(taskType)) {
                params.put("valveStatus", "INSPECTED"); // 回库只查询已检测的
            }
        }
        
        Toast.makeText(this, "正在查询...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PageResponse<Valve> pageResponse = wmsApiService.queryValves(params, SelectValveActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            valveList.clear();
                            if (pageResponse != null && pageResponse.getList() != null) {
                                valveList.addAll(pageResponse.getList());
                            }
                            adapter.notifyDataSetChanged();
                            
                            if (valveList.isEmpty()) {
                                Toast.makeText(SelectValveActivity.this, "未查询到数据", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(SelectValveActivity.this, "查询到 " + valveList.size() + " 条记录", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SelectValveActivity.this, "查询失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 确认选择
     */
    private void confirmSelection() {
        if (selectedValve == null) {
            Toast.makeText(this, "请选择一条阀门记录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 返回选中的阀门
        android.content.Intent intent = new android.content.Intent();
        intent.putExtra("valve", selectedValve);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    /**
     * 阀门列表适配器
     */
    private class ValveAdapter extends RecyclerView.Adapter<ValveAdapter.ViewHolder> {
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_valve, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Valve valve = valveList.get(position);
            holder.bind(valve, position);
        }
        
        @Override
        public int getItemCount() {
            return valveList.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            RadioButton rbSelected;
            TextView tvValveNo;
            TextView tvValveModel;
            TextView tvVendorName;
            TextView tvPalletInfo;
            TextView tvInboundDate;
            
            ViewHolder(View itemView) {
                super(itemView);
                rbSelected = (RadioButton) itemView.findViewById(R.id.rbSelected);
                tvValveNo = (TextView) itemView.findViewById(R.id.tvValveNo);
                tvValveModel = (TextView) itemView.findViewById(R.id.tvValveModel);
                tvVendorName = (TextView) itemView.findViewById(R.id.tvVendorName);
                tvPalletInfo = (TextView) itemView.findViewById(R.id.tvPalletInfo);
                tvInboundDate = (TextView) itemView.findViewById(R.id.tvInboundDate);
                
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            selectedValve = valveList.get(pos);
                            notifyDataSetChanged();
                        }
                    }
                });
            }
            
            void bind(Valve valve, int position) {
                rbSelected.setChecked(selectedValve == valve);
                tvValveNo.setText("阀门编号：" + valve.getValveNo());
                tvValveModel.setText("阀门型号：" + valve.getValveModel());
                tvVendorName.setText("厂家：" + valve.getVendorName());
                tvPalletInfo.setText("托盘：" + valve.getPalletNo() + "  库位：" + valve.getBinCode());
                tvInboundDate.setText("入库日期：" + valve.getInboundDate());
            }
        }
    }
}

