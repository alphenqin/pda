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

    private static final int VALVE_QUERY_PAGE_SIZE = 100;
    
    private EditText etValveNo;
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
        etValveNo = (EditText) findViewById(R.id.etValveNo);
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
        String valveNo = etValveNo.getText().toString().trim();
        String inboundDate = etInboundDate.getText().toString().trim();

        if (!valveNo.isEmpty()) {
            params.put("valveNo", valveNo);
        }
        if (!inboundDate.isEmpty()) {
            params.put("inboundDate", inboundDate);
        }
        
        // PDA 列表没有分页控件，查询时一次拉取全部分页结果。
        params.put("pageSize", String.valueOf(VALVE_QUERY_PAGE_SIZE));
        
        // 根据任务类型设置阀门状态筛选
        String taskType = getIntent().getStringExtra("taskType");
        if (taskType != null) {
            if ("SEND_INSPECTION".equals(taskType)) {
                params.put("valveStatus", "IN_STOCK"); // 送检只查询待检测的
            } else if ("OUTBOUND".equals(taskType)) {
                params.put("valveStatus", "IN_STOCK,INSPECTED"); // 出库查询待检测和已检测的
            } else if ("RETURN".equals(taskType)) {
                params.put("valveStatus", "IN_INSPECTION"); // 回库只查询检测中的
            }
        }
        
        Toast.makeText(this, "正在查询...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Valve> queryResult = queryAllValves(params);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            valveList.clear();
                            selectedValve = null;
                            valveList.addAll(queryResult);
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

    private List<Valve> queryAllValves(Map<String, String> baseParams) throws java.io.IOException {
        List<Valve> result = new ArrayList<Valve>();
        int pageNum = 1;
        int total = -1;

        while (true) {
            Map<String, String> pageParams = new HashMap<String, String>(baseParams);
            pageParams.put("pageNum", String.valueOf(pageNum));
            pageParams.put("pageSize", String.valueOf(VALVE_QUERY_PAGE_SIZE));

            PageResponse<Valve> pageResponse = wmsApiService.queryValves(pageParams, SelectValveActivity.this);
            List<Valve> pageList = pageResponse != null ? pageResponse.getList() : null;
            if (pageResponse != null) {
                total = pageResponse.getTotal();
            }
            if (pageList == null || pageList.isEmpty()) {
                break;
            }

            result.addAll(pageList);
            if (total >= 0 && result.size() >= total) {
                break;
            }
            if (pageList.size() < VALVE_QUERY_PAGE_SIZE) {
                break;
            }
            pageNum++;
        }

        return result;
    }

    /**
     * 确认选择
     */
    private void confirmSelection() {
        if (selectedValve == null) {
            Toast.makeText(this, "请选择一条样品记录", Toast.LENGTH_SHORT).show();
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
            TextView tvPalletInfo;
            
            ViewHolder(View itemView) {
                super(itemView);
                rbSelected = (RadioButton) itemView.findViewById(R.id.rbSelected);
                tvValveNo = (TextView) itemView.findViewById(R.id.tvValveNo);
                tvPalletInfo = (TextView) itemView.findViewById(R.id.tvPalletInfo);
                
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
                tvValveNo.setText("出厂编号：" + valve.getValveNo());
                tvPalletInfo.setText("库位：" + displayText(valve.getBinCode()));
            }
        }
    }

    private String displayText(String value) {
        return value == null || value.trim().isEmpty() ? "--" : value;
    }
}
