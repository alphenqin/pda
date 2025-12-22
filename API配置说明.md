# API配置说明

## 1. 配置服务器地址

在 `app/src/main/java/com/qs/qs5502demo/api/ApiConfig.java` 文件中配置服务器地址：

```java
// WMS接口基础地址
public static final String WMS_BASE_URL = "http://localhost:8080/api";

// AGV调度系统接口基础地址
public static final String AGV_BASE_URL = "http://192.168.1.20:81/pt";
```

**当前配置**：
- WMS地址：已配置为 `http://localhost:8080/api`
- AGV地址：已配置为 `http://192.168.1.20:81/pt`

### ⚠️ 重要提示：localhost 连接问题

**问题说明：**
- 如果PDA应用运行在Android设备或模拟器上，`localhost` 或 `127.0.0.1` 指向的是设备本身，而不是开发电脑
- 因此会出现 "Failed to connect to localhost/127.0.0.1:8080" 错误

**解决方案：**

#### 方案1：Android模拟器
如果使用Android模拟器，将地址改为：
```java
public static final String WMS_BASE_URL = "http://10.0.2.2:8080/api";
```
> `10.0.2.2` 是Android模拟器访问开发电脑localhost的特殊地址

#### 方案2：真机调试
如果使用真机，需要：
1. 查看开发电脑的IP地址（Windows: `ipconfig`，Mac/Linux: `ifconfig`）
2. 确保PDA和开发电脑在同一局域网
3. 将地址改为开发电脑的实际IP，例如：
```java
public static final String WMS_BASE_URL = "http://192.168.1.100:8080/api";
```

#### 方案3：WMS服务器已部署
如果WMS服务器已部署在其他服务器上，直接使用服务器地址：
```java
public static final String WMS_BASE_URL = "http://wms-server-ip:8080/api";
```

## 2. 已实现的接口

### WMS接口（WmsApiService）
- ✅ 登录接口：`POST /api/auth/login`
- ✅ 托盘扫码接口：`POST /api/pallet/scan`
- ✅ 阀门绑定接口：`POST /api/valve/bind`
- ✅ 阀门查询接口：`POST /api/valve/query`
- ✅ 任务记录查询接口：`POST /api/task/query`

### AGV调度系统接口（AgvApiService）
- ✅ 统一任务发送接口 `POST /taskSent`（所有任务都通过此接口）
  - 入库：呼叫入库（type=01，从置换区取货→库位放货）
  - 送检：呼叫送检（type=01，从库位取货→检测区放货）
  - 送检：空托回库（type=01，从检测区取空托→库位放空托）
  - 回库：呼叫托盘（type=01，从库位取空托→检测区放空托）
  - 回库：阀门回库（type=01，从检测区取货→库位放货）
  - 出库：呼叫出库（type=01，从库位取货→置换区放货）
  - 出库：空托回库（type=01，从置换区取空托→库位放空托）
  - 取消任务（type=13，清空指定outID任务）
- ✅ 查询任务结果 `POST /taskResult`（可选，用于查询任务状态）

## 3. 已更新的功能模块

### 登录模块
- ✅ 登录页面（LoginActivity）
- ✅ Token保存和管理（PreferenceUtil）
- ✅ 主界面登录检查

### 入库模块
- ✅ InboundActivity - 已更新使用新API
- ✅ BindValveActivity - 已更新使用新API

### 其他模块
- ✅ SendInspectionActivity（送检）- 已更新使用新API
- ✅ ReturnWarehouseActivity（回库）- 已更新使用新API
- ✅ OutboundActivity（出库）- 已更新使用新API
- ✅ TaskManageActivity（任务管理）- 已更新使用新API
- ✅ SelectValveActivity（选阀门）- 已更新使用新API

## 4. 数据模型更新

已更新的数据模型：
- ✅ Pallet - 添加了 `binCode` 字段
- ✅ Valve - 添加了 `matCode`、`binCode`、`valveStatus` 字段
- ✅ Task - 添加了 `outID`、`matCode`、`binCode` 字段

## 5. 使用示例

### 登录
```java
WmsApiService wmsApiService = new WmsApiService(context);
LoginRequest request = new LoginRequest();
request.setUsername("pda001");
request.setPassword("123456");
request.setDeviceCode("PDA-01");
LoginResponse response = wmsApiService.login(request);
```

### 托盘扫码
```java
Pallet pallet = wmsApiService.scanPallet(barcode, context);
```

### 呼叫入库
```java
AgvApiService agvApiService = new AgvApiService();
String outID = DateUtil.generateTaskNo("R");  // 生成任务编号
AgvResponse response = agvApiService.callInbound(
    "WAREHOUSE_SWAP_1",  // 置换区站点
    "2-01",              // 库位号
    "MAT-DN50-001",      // 物料编码
    outID,               // 任务编号
    context
);
if (response.isSuccess()) {
    // 成功，outID即为任务编号
}
```

## 6. 注意事项

1. **网络权限**：确保 AndroidManifest.xml 中已添加网络权限
2. **Token管理**：Token会自动添加到请求头中，无需手动处理
3. **错误处理**：所有API调用都在后台线程执行，需要在主线程更新UI
4. **服务器地址**：部署前务必修改 ApiConfig.java 中的服务器地址

## 7. 已完成的工作

✅ 所有Activity已更新完成：
- ✅ InboundActivity（入库）
- ✅ BindValveActivity（阀门绑定）
- ✅ SendInspectionActivity（送检）
- ✅ ReturnWarehouseActivity（回库）
- ✅ OutboundActivity（出库）
- ✅ TaskManageActivity（任务管理）
- ✅ SelectValveActivity（选阀门）

所有Activity都已：
1. ✅ 将 `ApiClient` 替换为 `WmsApiService` 和 `AgvApiService`
2. ✅ 使用 `binCode` 替代 `locationCode`
3. ✅ 使用 `outID` 替代 `taskNo`
4. ✅ 在后台线程执行网络请求，主线程更新UI
5. ✅ 添加了分页参数支持
6. ✅ 根据任务类型自动筛选阀门状态

