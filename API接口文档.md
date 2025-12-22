# PDA 接口说明书 v2.0（面向 WMS & AGV 调度系统）

> **说明：**
>
> * 本文档只描述 **PDA 需要发送的 HTTP API**。
> * PDA 一部分请求发送给 **WMS**，一部分请求发送给 **AGV 调度系统**。
> * PC 端 WMS 预登记等操作不在本说明范围内。

---

## 0. 系统与交互关系概述

* **WMS 系统**：管理库位、托盘、阀门基础数据，提供托盘扫码、阀门绑定、阀门查询、任务统计等接口。

* **AGV 调度系统**：负责接收 PDA 呼叫指令，生成并下发 AGV 搬运任务，任务编号以 R/S/H/C + 时间组成。

* **PDA**：
  * 入库：托盘扫码 → 阀门绑定（WMS）→ 呼叫入库（AGV）
  * 送检：选阀门（WMS）→ 呼叫送检（AGV）→ 空托回库（AGV）
  * 回库：选阀门（WMS）→ 呼叫托盘（AGV）→ 阀门回库（AGV）
  * 出库：选阀门（WMS）→ 呼叫出库（AGV）→ 空托回库（AGV）
  * 任务管理：查询任务记录（WMS），并能对**待执行**任务进行取消（AGV）。

---

## 1. 基础约定

### 1.1 基础 URL

* **WMS 接口基础地址**

```text
http://localhost:8080/api
```

* **AGV 调度系统接口基础地址**

```text
http://192.168.1.20:81/pt
```

（具体域名/IP 以现场部署为准，当前配置为：192.168.1.20:81）

### 1.2 通用请求头

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}   // 登录后获取
```

### 1.3 统一响应格式（推荐）

PDA 期望 WMS / AGV 返回统一格式：

**成功：**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { }
}
```

**失败：**

```json
{
  "code": 400,
  "message": "错误信息描述",
  "data": null
}
```

> AGV 调度系统如果有自己的 code 体系（如 20000、90000），建议由中间层适配成以上格式。

---

## 2. 公共数据模型 & 枚举

### 2.1 数据模型

#### 2.1.1 托盘（Pallet）

```json
{
  "palletNo": "11-01",             // 托盘编号
  "palletType": "SMALL",          // SMALL / LARGE
  "swapStation": "WAREHOUSE_SWAP_1", // 库前置换区站点编码（与 AGV pointCode 对应）
  "binCode": "2-01"               // 库位号，与调度系统 binCode 一致
}
```

#### 2.1.2 阀门（Valve / 物料）

```json
{
  "valveNo": "V20250101-001",     // 阀门唯一编号（WMS 内部主键）
  "matCode": "MAT-DN50-001",      // 物料编码，对接 AGV 使用
  "valveModel": "DN50",           // 型号
  "vendorName": "XX阀门厂",       // 厂家
  "inboundDate": "2025-01-15",    // 入库日期 yyyy-MM-dd
  "palletNo": "11-01",
  "binCode": "2-01",
  "valveStatus": "IN_STOCK"       // 在库/送检中/已检测/已出库
}
```

#### 2.1.3 任务（Task）

```json
{
  "outID": "R20250715145830999",  // 任务编号，R/S/H/C + 时间，AGV 生成
  "taskType": "INBOUND",          // 业务任务类型
  "status": "PENDING",            // PENDING/EXECUTING/COMPLETED/CANCELLED/FAILED
  "createTime": "2025-01-15 14:58:30",
  "palletNo": "11-01",
  "valveNo": "V20250101-001",
  "matCode": "MAT-DN50-001",
  "binCode": "2-01"
}
```

### 2.2 枚举

* **托盘型号 `palletType`**
  * `SMALL`：小托盘（1 类托盘，132 个）
  * `LARGE`：大托盘（2 类托盘，33 个）

* **阀门状态 `valveStatus`**
  * `IN_STOCK`：在库
  * `IN_INSPECTION`：检测中
  * `INSPECTED`：已检测
  * `OUTBOUND`：已出库

* **业务任务类型 `taskType`**
  * `INBOUND`：入库（R 开头）
  * `SEND_INSPECTION`：送检（S 开头）
  * `RETURN`：回库（H 开头）
  * `OUTBOUND`：出库（C 开头）

* **任务状态 `status`（业务侧）**
  * `PENDING`：待执行（AGV 尚未执行）
  * `EXECUTING`：执行中
  * `COMPLETED`：已完成
  * `CANCELLED`：已取消
  * `FAILED`：失败

---

## 3. PDA → WMS 接口（托盘/阀门/任务统计）

这些接口用于**读写 WMS 的业务数据**，对应 PPT 中：托盘扫码、阀门绑定、选阀门、任务查询。

### 3.1 登录接口（主界面前置）

> PPT 虽未画出登录页，但 PDA 实际部署需有登录。

* **URL**：`POST /api/auth/login`
* **说明**：PDA 用户登录 WMS，获取 Token。

**请求**

```json
{
  "username": "pda001",
  "password": "123456",
  "deviceCode": "PDA-01"
}
```

**响应**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "xxx",
    "expireAt": "2025-01-15 23:59:59",
    "userName": "张三",
    "roles": ["PDA_USER"]
  }
}
```

---

### 3.2 托盘扫码接口（入库二级页面 第 1 步）

> PPT：入库二级页面步骤 1「托盘扫码，识别出托盘编号、托盘型号，获得置换区站点号和该托盘对应的库位号」。

* **URL**：`POST /api/pallet/scan`
* **说明**：根据托盘二维码/条码，从 WMS 获取托盘信息。

**请求**

```json
{
  "barcode": "PALLET-QR-CODE-11-01"
}
```

**响应（成功）**

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "palletNo": "11-01",
    "palletType": "SMALL",
    "swapStation": "WAREHOUSE_SWAP_1",
    "binCode": "2-01"
  }
}
```

---

### 3.3 阀门绑定接口（入库三级页面）

> PPT：「阀门绑定界面」，输入阀门编号、型号、厂家、入库日期，绑定托盘号 & 库位号后返回上层页面。

* **URL**：`POST /api/valve/bind`
* **说明**：在 WMS 中绑定阀门与托盘、库位的关系。

**请求**

```json
{
  "valveNo": "V20250101-001",
  "matCode": "MAT-DN50-001",
  "valveModel": "DN50",
  "vendorName": "XX阀门厂",
  "inboundDate": "2025-01-15",
  "palletNo": "11-01",
  "binCode": "2-01"
}
```

**响应**

```json
{
  "code": 200,
  "message": "绑定成功",
  "data": {
    "success": true
  }
}
```

---

### 3.4 阀门查询接口（送检/回库/出库 三级"选阀门界面"）

> PPT：送检/回库/出库均有「选阀门」三级页面，输入厂家名称、阀门编号/型号，查询并返回托盘号和库位号。

* **URL**：`POST /api/valve/query`
* **说明**：WMS 中根据条件查询阀门列表。

**请求**

```json
{
  "vendorName": "XX阀门厂",
  "valveNo": "V20250101-001",
  "valveModel": "DN50",
  "inboundDate": "2025-01-15",
  "valveStatus": "IN_STOCK",
  "pageNum": 1,
  "pageSize": 20
}
```

**响应**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "valveNo": "V20250101-001",
        "matCode": "MAT-DN50-001",
        "valveModel": "DN50",
        "vendorName": "XX阀门厂",
        "inboundDate": "2025-01-15",
        "palletNo": "11-01",
        "binCode": "2-01",
        "valveStatus": "IN_STOCK"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

### 3.5 任务记录查询接口（任务管理 → 任务查询界面）

> PPT：任务管理二级页面 → 任务查询界面，按起始日期/结束日期查询任务，默认当天，列表显示结果。

* **URL**：`POST /api/task/query`
* **说明**：从 WMS 查询历史任务记录列表，由 WMS 统计（包括从 AGV 获取到的 outID）。

**请求**

```json
{
  "startDate": "2025-01-15",
  "endDate": "2025-01-15",
  "taskType": "INBOUND",
  "status": "PENDING",
  "pageNum": 1,
  "pageSize": 20
}
```

**响应**

```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "outID": "R20250715145830999",
        "taskType": "INBOUND",
        "status": "PENDING",
        "createTime": "2025-01-15 14:58:30",
        "palletNo": "11-01",
        "valveNo": "V20250101-001",
        "matCode": "MAT-DN50-001",
        "binCode": "2-01"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

## 4. PDA → AGV 调度系统接口（呼叫入库 / 送检 / 回库 / 出库 / 取消任务）

这一部分接口是 **PDA 直接调用 AGV 调度系统**，对应 PPT 中所有"呼叫 XXX""空托回库""阀门回库""取消任务"按钮。

> **注意**：所有 AGV 接口统一调用 `/taskSent` 接口，通过不同的 `type` 和 `points` 参数实现不同的业务功能。

### 4.1 发送任务接口（统一接口）

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：AGV 调度系统统一的任务发送接口，所有任务（取放货、充电、急停、取消等）都通过此接口发送。

**请求参数**

| 参数名称      | 数据类型 | 必填 | 说明                                              |
| ------- | ------- | ---- | ----------------------------------------------- |
| taskCode | string | 否 | 调度系统任务编号，不传则系统自动生成 |
| type    | string | 是 | 任务类型：01取放货任务、05充电、10急停、12解除急停、13清空指定outID任务、18解除待命 |
| IsOrder | string | 否 | 是否有序，默认否 |
| agvRange | string | 否 | AGV范围，多个用逗号隔开 |
| points  | array | 是（取放货任务） | 作业点对象列表 |
| level   | string | 否 | 任务级别：1优先任务，2普通任务(默认) |
| clearOutID | string | 否 | 被清空的外部业务id（清空指定任务时为必填） |
| outID   | string | 否 | 外部业务id（PDA生成，格式：R/S/H/C + 时间戳） |

**point对象参数**

| 参数名称 | 数据类型 | 必填 | 说明 |
| ------- | ------- | ---- | ---- |
| sn | string | 是 | 作业序号 |
| pointCode | string | 是 | 作业编号 |
| pointType | string | 是 | 作业类型：02通用(包括取货)、04放货 |
| matCode | string | 否 | 取放货物料信息 |

**响应格式**

```json
{
  "code": "20000",
  "message": ""
}
```

> **说明**：
> * `code` 为字符串类型：`"20000"` 表示成功，`"90000"` 表示失败
> * PDA 需要根据业务场景构建不同的 `points` 数组来实现不同的任务

---

### 4.2 入库：呼叫入库（入库二级页面 第 3 步）

> PPT：入库二级页面第 3 步「呼叫入库」，调用 AGV，形成 R 开头的任务编号。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从置换区取货，放到仓库库位。

**请求示例**

```json
{
  "type": "01",
  "outID": "R20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "WAREHOUSE_SWAP_1",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "2-01",
      "pointType": "04",
      "matCode": "MAT-DN50-001"
    }
  ]
}
```

> 说明：
>
> * `outID` 由 PDA 生成，格式：R + 时间戳（如 R20250115145830123）
> * `points[0]` 为取货点（置换区站点）
> * `points[1]` 为放货点（仓库库位），需传入 `matCode`

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.3 送检：呼叫送检（送检二级页面 第 2 步）

> PPT：送检二级页面：1 选阀门（WMS）→ 2 呼叫送检（AGV）。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从样品库库位取货，送到检测区站点。

**请求示例**

```json
{
  "type": "01",
  "outID": "S20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "2-01",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "INSPECTION_STATION_1",
      "pointType": "04",
      "matCode": "MAT-DN50-001"
    }
  ]
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.4 送检：空托回库（送检二级页面 第 3 步）

> PPT：送检二级页面第 3 步「空托回库」，检测区工作人员在阀门取下后，从 PDA 点空托回库。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从检测区站点取空托盘，送回样品库库位。

**请求示例**

```json
{
  "type": "01",
  "outID": "H20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "INSPECTION_STATION_1",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "2-01",
      "pointType": "04"
    }
  ]
}
```

> 说明：空托回库时，放货点不传 `matCode`（或传空字符串）

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.5 回库：呼叫托盘（回库二级页面 第 2 步）

> PPT：回库二级页面：1 选阀门（WMS）→ 2 呼叫托盘（AGV：送空托到检测区）。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从样品库库位取空托盘，送到检测区站点。

**请求示例**

```json
{
  "type": "01",
  "outID": "H20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "2-01",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "INSPECTION_STATION_1",
      "pointType": "04"
    }
  ]
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.6 回库：阀门回库（回库二级页面 第 3 步）

> PPT：回库第 3 步「阀门回库」，检测区工人将阀门放到托盘上后，从 PDA 点"回库"。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从检测区站点取载有阀门的托盘，送回样品库库位。

**请求示例**

```json
{
  "type": "01",
  "outID": "H20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "INSPECTION_STATION_1",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "2-01",
      "pointType": "04",
      "matCode": "MAT-DN50-001"
    }
  ]
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.7 出库：呼叫出库（出库二级页面 第 2 步）

> PPT：出库二级页面：1 选阀门（WMS）→ 2 呼叫出库（AGV）。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从样品库库位取货，送到库前置换区。

**请求示例**

```json
{
  "type": "01",
  "outID": "C20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "2-01",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "WAREHOUSE_SWAP_1",
      "pointType": "04",
      "matCode": "MAT-DN50-001"
    }
  ]
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.8 出库：空托回库（出库二级页面 第 3 步）

> PPT：出库第 3 步「空托回库」，工人取下阀门装车后，从 PDA 点空托回库。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：从库前置换区取空托盘，送回样品库库位。

**请求示例**

```json
{
  "type": "01",
  "outID": "H20250115145830123",
  "level": "2",
  "points": [
    {
      "sn": "01",
      "pointCode": "WAREHOUSE_SWAP_1",
      "pointType": "02"
    },
    {
      "sn": "02",
      "pointCode": "2-01",
      "pointType": "04"
    }
  ]
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

---

### 4.9 取消任务接口（任务管理二级页面 "取消任务"）

> PPT：在任务查询结果中，工人勾选"待执行"任务后点击"取消任务"，执行中的任务不能取消。

* **URL**：`POST http://192.168.1.20:81/pt/taskSent`
* **说明**：清空指定 `outID` 的任务。

**请求示例**

```json
{
  "type": "13",
  "clearOutID": "R20250115145830123",
  "outID": "R20250115145830123"
}
```

**响应**

```json
{
  "code": "20000",
  "message": ""
}
```

> 说明：
> * `type` 为 `"13"` 表示清空指定outID任务
> * `clearOutID` 和 `outID` 都传入要取消的任务编号
> * 若任务已开始执行，AGV 返回失败；PDA 前端据此提示"任务状态不允许取消"

### 4.10 查询任务结果接口（可选）

* **URL**：`POST http://192.168.1.20:81/pt/taskResult`
* **说明**：查询指定任务的状态和执行情况。

**请求**

```json
{
  "outID": "R20250115145830123"
}
```

**响应**

```json
{
  "code": "20000",
  "status": "02",
  "points": [
    {
      "sn": "01",
      "pointCode": "WAREHOUSE_SWAP_1",
      "pointType": "02",
      "pointAction": "01-04-02-06-03",
      "pointStep": "01-04-02"
    },
    {
      "sn": "02",
      "pointCode": "2-01",
      "pointType": "04",
      "pointAction": "01-06-02-05-03",
      "pointStep": ""
    }
  ],
  "agvCode": "1",
  "message": ""
}
```

> 说明：
> * `status`：01等待执行、02执行中、08执行完成、09强制清空
> * `pointStep` 与 `pointAction` 一致时，表示该作业点执行完成

---

## 5. 按页面梳理：页面 → 接口映射

最后给你一个「**页面到接口**」的对照表，方便你丢给前端/安卓/Cursor 直接按页面开发。

### 5.1 主界面

> PPT：主界面按钮：入库、送检、回库、出库、任务管理。

* 不直接调用接口（仅导航）。
* 可选：进入主界面前调用 `POST /api/auth/login`。

---

### 5.2 入库流程

> PPT：入库二级页面：1 托盘扫码 → 2 阀门绑定 → 3 呼叫入库。

**入库二级页面**

1. 按钮【托盘扫码】
   * 调用：`POST /api/pallet/scan`（WMS）

2. 按钮【阀门绑定】（跳入"阀门绑定界面"）

3. 按钮【呼叫入库】
   * 调用：`POST /api/agv/inbound/call`（AGV）

**阀门绑定界面（入库三级页面）**

* 按钮【绑定】
  * 调用：`POST /api/valve/bind`（WMS）

---

### 5.3 送检流程

> PPT：送检二级页面：1 选阀门 → 2 呼叫送检 → 3 #空托回库。

**送检二级页面**

1. 按钮【选阀门】（跳入"选阀门界面"）

2. 按钮【呼叫送检】
   * 调用：`POST /api/agv/inspection/send`（AGV）

3. 按钮【1# 空托回库】、【2# 空托回库】
   * 调用：`POST /api/agv/pallet/returnFromInspection`（AGV）

**选阀门界面（送检三级页面）**

* 按钮【查询】
  * 调用：`POST /api/valve/query`（WMS）

* 按钮【确认】
  * 不调接口，只是把选中的阀门信息带回上一层页面。

---

### 5.4 回库流程

> PPT：回库二级页面：1 选阀门 → 2 呼叫托盘 → 3 #阀门回库。

**回库二级页面**

1. 按钮【选阀门】（跳入"选阀门界面"）

2. 按钮【呼叫托盘】
   * 调用：`POST /api/agv/pallet/callToInspection`（AGV）

3. 按钮【1# 阀门回库】、【2# 阀门回库】
   * 调用：`POST /api/agv/valve/returnToWarehouse`（AGV）

**选阀门界面（回库三级页面）**

* **与送检选阀门完全共用同一接口**：
  * 【查询】：`POST /api/valve/query`（WMS）
  * 【确认】：回填选中记录。

---

### 5.5 出库流程

> PPT：出库二级页面：1 选阀门 → 2 呼叫出库 → 3 #空托回库。

**出库二级页面**

1. 按钮【选阀门】（跳入"选阀门界面"）

2. 按钮【呼叫出库】
   * 调用：`POST /api/agv/outbound/call`（AGV）

3. 按钮【1# 空托回库】、【2# 空托回库】
   * 调用：`POST /api/agv/pallet/returnFromSwap`（AGV）

**选阀门界面（出库三级页面）**

* 同上：`POST /api/valve/query`（WMS）+【确认】回填。

---

### 5.6 任务管理 → 任务查询与取消

> PPT：任务管理二级页面 → 任务查询界面，按日期查询任务，勾选待执行任务，点击"取消任务"。

**任务管理二级页面**

* 按钮【任务查询】 → 进入"任务查询界面"。

**任务查询界面**

1. 按钮【查询】
   * 调用：`POST /api/task/query`（WMS）
   * 起始/结束日期为空时默认当天。

2. 按钮【取消任务】（对选中的"待执行"任务）
   * 调用：`POST /api/agv/task/cancel`（AGV）

---

## 6. 注意事项

1. **时间格式**：所有日期时间字段统一使用格式：
   - 日期：`yyyy-MM-dd`，例如：`2025-01-15`
   - 日期时间：`yyyy-MM-dd HH:mm:ss`，例如：`2025-01-15 14:58:30`

2. **字符编码**：所有请求和响应使用UTF-8编码

3. **网络超时**：建议设置请求超时时间为10秒

4. **重试机制**：网络请求失败时，建议实现重试机制（最多重试3次）

5. **错误处理**：所有接口调用都需要进行错误处理，向用户显示友好的错误提示

6. **数据校验**：PDA端需要对输入数据进行校验，减少无效请求

7. **日志记录**：建议记录所有API调用日志，便于问题排查

8. **接口版本**：如果后续需要升级接口，建议在URL中添加版本号，如：`/api/v1/pallet/scan`

9. **字段命名**：字段命名以《AGV 调度系统接口 V7.0》为基准（如 `outID`、`binCode`、`matCode` 等）

---

## 7. 版本历史

- **v2.0** (2025-01-15)：重构版本，明确区分 WMS 和 AGV 调度系统接口，按页面流程组织，增加页面到接口映射表
- **v1.0.0** (2025-01-15)：初始版本，包含6个核心API接口
