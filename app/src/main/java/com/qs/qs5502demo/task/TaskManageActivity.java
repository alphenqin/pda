package com.qs.qs5502demo.task;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.qs.pda5502demo.R;
import com.qs.qs5502demo.api.AgvApiService;
import com.qs.qs5502demo.api.WmsApiService;
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.util.DateUtil;
import com.qs.qs5502demo.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

public class TaskManageActivity extends Activity {
    
    private EditText etStartDate;
    private EditText etEndDate;
    private Button btnSearch;
    private Button btnCancelTask;
    private Button btnRefresh;
    private Button btnBack;
    private RecyclerView rvTaskList;
    
    private WmsApiService wmsApiService;
    private AgvApiService agvApiService;
    private TaskAdapter adapter;
    private List<Task> taskList;
    private Task selectedTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_task_manage);
        
        wmsApiService = new WmsApiService(this);
        agvApiService = new AgvApiService();
        taskList = new ArrayList<>();
        
        initViews();
        setupListeners();
        
        // 默认查询当天任务
        searchTasks();
    }
    
    private void initViews() {
        etStartDate = (EditText) findViewById(R.id.etStartDate);
        etEndDate = (EditText) findViewById(R.id.etEndDate);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnCancelTask = (Button) findViewById(R.id.btnCancelTask);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnBack = (Button) findViewById(R.id.btnBack);
        rvTaskList = (RecyclerView) findViewById(R.id.rvTaskList);
        
        // 设置RecyclerView
        rvTaskList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter();
        rvTaskList.setAdapter(adapter);
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
                searchTasks();
            }
        });
        
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchTasks();
            }
        });
        
        btnCancelTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelTask();
            }
        });
    }
    
    /**
     * 查询任务
     */
    private void searchTasks() {
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();
        
        // 如果都为空，默认查询当天
        if (startDate.isEmpty() && endDate.isEmpty()) {
            startDate = DateUtil.getCurrentDate();
            endDate = DateUtil.getCurrentDate();
        }
        
        // 构建查询参数
        final Map<String, String> params = new HashMap<>();
        if (!startDate.isEmpty()) {
            params.put("startDate", startDate);
        }
        if (!endDate.isEmpty()) {
            params.put("endDate", endDate);
        }
        params.put("pageNum", "1");
        params.put("pageSize", "20");
        
        Toast.makeText(this, "正在查询...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PageResponse<Task> pageResponse = wmsApiService.queryTasks(params, TaskManageActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            taskList.clear();
                            if (pageResponse != null && pageResponse.getList() != null) {
                                taskList.addAll(pageResponse.getList());
                            }
                            adapter.notifyDataSetChanged();
                            
                            if (taskList.isEmpty()) {
                                Toast.makeText(TaskManageActivity.this, "未查询到数据", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(TaskManageActivity.this, "查询到 " + taskList.size() + " 条记录", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TaskManageActivity.this, "查询失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 取消任务
     */
    private void cancelTask() {
        if (selectedTask == null) {
            Toast.makeText(this, "请选择一条任务", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!selectedTask.canCancel()) {
            Toast.makeText(this, "只能取消状态为\"待执行\"的任务", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("确认取消任务")
            .setMessage("任务编号：" + selectedTask.getOutID() + "\n任务类型：" + selectedTask.getTaskTypeDisplay())
            .setPositiveButton("确认", new android.content.DialogInterface.OnClickListener() {
                @Override
                public void onClick(android.content.DialogInterface dialog, int which) {
                    performCancelTask();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行取消任务
     */
    private void performCancelTask() {
        Toast.makeText(this, "正在取消任务...", Toast.LENGTH_SHORT).show();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String outID = selectedTask.getOutID();
                    String operator = PreferenceUtil.getUserName(TaskManageActivity.this);
                    boolean success = agvApiService.cancelTask(outID, operator, TaskManageActivity.this);
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                Toast.makeText(TaskManageActivity.this, "取消任务成功", Toast.LENGTH_SHORT).show();
                                selectedTask.setStatus(Task.STATUS_CANCELLED);
                                adapter.notifyDataSetChanged();
                                selectedTask = null;
                            } else {
                                Toast.makeText(TaskManageActivity.this, "取消任务失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(TaskManageActivity.this, "取消任务失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
    
    /**
     * 任务列表适配器
     */
    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Task task = taskList.get(position);
            holder.bind(task, position);
        }
        
        @Override
        public int getItemCount() {
            return taskList.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSelected;
            TextView tvTaskNo;
            TextView tvTaskType;
            TextView tvTaskStatus;
            TextView tvTaskTime;
            TextView tvTaskInfo;
            
            ViewHolder(View itemView) {
                super(itemView);
                cbSelected = (CheckBox) itemView.findViewById(R.id.cbSelected);
                tvTaskNo = (TextView) itemView.findViewById(R.id.tvTaskNo);
                tvTaskType = (TextView) itemView.findViewById(R.id.tvTaskType);
                tvTaskStatus = (TextView) itemView.findViewById(R.id.tvTaskStatus);
                tvTaskTime = (TextView) itemView.findViewById(R.id.tvTaskTime);
                tvTaskInfo = (TextView) itemView.findViewById(R.id.tvTaskInfo);
                
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            selectedTask = taskList.get(pos);
                            notifyDataSetChanged();
                        }
                    }
                });
            }
            
            void bind(Task task, int position) {
                cbSelected.setChecked(selectedTask == task);
                tvTaskNo.setText("任务编号：" + task.getOutID());
                tvTaskType.setText("任务类型：" + task.getTaskTypeDisplay());
                tvTaskStatus.setText("状态：" + task.getStatusDisplay());
                tvTaskTime.setText("创建时间：" + task.getCreateTime());
                
                String info = "";
                if (task.getPalletNo() != null && !task.getPalletNo().isEmpty()) {
                    info += "托盘：" + task.getPalletNo();
                }
                if (task.getBinCode() != null && !task.getBinCode().isEmpty()) {
                    if (!info.isEmpty()) info += "  ";
                    info += "库位：" + task.getBinCode();
                }
                tvTaskInfo.setText(info);
            }
        }
    }
}

