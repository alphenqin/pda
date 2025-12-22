# WMS接口开发文档

> **文档说明**：本文档为PDA安卓项目所需WMS接口的完整开发规范，供WMS开发团队参考使用。

---

## 目录

1. [基础约定](#1-基础约定)
2. [数据模型定义](#2-数据模型定义)
3. [接口列表](#3-接口列表)
4. [业务流程说明](#4-业务流程说明)
5. [注意事项](#5-注意事项)

---

## 1. 基础约定

### 1.1 接口基础地址

```
http://localhost:8080/api
```

> **注意**：实际部署时请根据现场环境修改此地址。

### 1.2 通用请求头

所有接口请求需要包含以下请求头：

```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer {token}   // 除登录接口外，其他接口都需要Token
```

### 1.3 统一响应格式

所有接口必须返回统一的响应格式：

**成功响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { }
}
```

**失败响应：**
```json
{
  "code": 400,
  "message": "错误信息描述",
  "data": null
}
```

**响应码说明：**
- `200`：操作成功
- `400`：请求参数错误
- `401`：未授权（Token无效或过期）
- `404`：资源不存在
- `500`：服务器内部错误

### 1.4 时间格式

- **日期格式**：`yyyy-MM-dd`，例如：`2025-01-15`
- **日期时间格式**：`yyyy-MM-dd HH:mm:ss`，例如：`2025-01-15 14:58:30`

### 1.5 字符编码

所有请求和响应使用 **UTF-8** 编码。

---

## 2. 数据模型定义

### 2.1 托盘（Pallet）

**字段说明：**

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| palletNo | String | 是 | 托盘编号 | "11-01" |
| palletType | String | 是 | 托盘型号：SMALL（小托盘，132个）/ LARGE（大托盘，33个） | "SMALL" |
| swapStation | String | 是 | 库前置换区站点编码（与AGV调度系统的pointCode对应） | "WAREHOUSE_SWAP_1" |
| binCode | String | 是 | 库位号，与AGV调度系统的binCode一致 | "2-01" |

**JSON示例：**
```json
{
  "palletNo": "11-01",
  "palletType": "SMALL",
  "swapStation": "WAREHOUSE_SWAP_1",
  "binCode": "2-01"
}
```

### 2.2 阀门（Valve）

**字段说明：**

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| valveNo | String | 是 | 阀门唯一编号（WMS内部主键） | "V20250101-001" |
| matCode | String | 是 | 物料编码，对接AGV调度系统使用 | "MAT-DN50-001" |
| valveModel | String | 是 | 阀门型号 | "DN50" |
| vendorName | String | 是 | 厂家名称 | "XX阀门厂" |
| inboundDate | String | 是 | 入库日期（yyyy-MM-dd格式） | "2025-01-15" |
| palletNo | String | 否 | 托盘号 | "11-01" |
| binCode | String | 否 | 库位号 | "2-01" |
| valveStatus | String | 否 | 阀门状态（见枚举说明） | "IN_STOCK" |

**JSON示例：**
```json
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
```

**阀门状态枚举（valveStatus）：**
- `IN_STOCK`：在库
- `IN_INSPECTION`：检测中
- `INSPECTED`：已检测
- `OUTBOUND`：已出库

### 2.3 任务（Task）

**字段说明：**

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| outID | String | 是 | 任务编号（AGV生成，格式：R/S/H/C + 时间戳） | "R20250115145830123" |
| taskType | String | 是 | 业务任务类型（见枚举说明） | "INBOUND" |
| status | String | 是 | 任务状态（见枚举说明） | "PENDING" |
| createTime | String | 是 | 创建时间（yyyy-MM-dd HH:mm:ss格式） | "2025-01-15 14:58:30" |
| palletNo | String | 否 | 托盘号 | "11-01" |
| valveNo | String | 否 | 阀门编号 | "V20250101-001" |
| matCode | String | 否 | 物料编码 | "MAT-DN50-001" |
| binCode | String | 否 | 库位号 | "2-01" |

**JSON示例：**
```json
{
  "outID": "R20250115145830123",
  "taskType": "INBOUND",
  "status": "PENDING",
  "createTime": "2025-01-15 14:58:30",
  "palletNo": "11-01",
  "valveNo": "V20250101-001",
  "matCode": "MAT-DN50-001",
  "binCode": "2-01"
}
```

**业务任务类型枚举（taskType）：**
- `INBOUND`：入库（任务编号以R开头）
- `SEND_INSPECTION`：送检（任务编号以S开头）
- `RETURN`：回库（任务编号以H开头）
- `OUTBOUND`：出库（任务编号以C开头）

**任务状态枚举（status）：**
- `PENDING`：待执行（AGV尚未执行）
- `EXECUTING`：执行中
- `COMPLETED`：已完成
- `CANCELLED`：已取消
- `FAILED`：失败

### 2.4 分页响应（PageResponse）

**字段说明：**

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| list | Array | 是 | 数据列表 | [] |
| total | Integer | 是 | 总记录数 | 100 |
| pageNum | Integer | 是 | 当前页码 | 1 |
| pageSize | Integer | 是 | 每页大小 | 20 |

**JSON示例：**
```json
{
  "list": [],
  "total": 100,
  "pageNum": 1,
  "pageSize": 20
}
```

### 2.5 登录响应数据（LoginData）

**字段说明：**

| 字段名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| token | String | 是 | 访问令牌 | "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." |
| expireAt | String | 是 | Token过期时间（yyyy-MM-dd HH:mm:ss格式） | "2025-01-15 23:59:59" |
| userName | String | 是 | 用户姓名 | "张三" |
| roles | Array[String] | 是 | 用户角色列表 | ["PDA_USER"] |

**JSON示例：**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expireAt": "2025-01-15 23:59:59",
  "userName": "张三",
  "roles": ["PDA_USER"]
}
```

---

## 3. 接口列表

### 3.1 登录接口

**接口说明：** PDA用户登录WMS系统，获取访问Token。

**接口地址：** `POST /api/auth/login`

**请求头：** 无需Token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| username | String | 是 | 用户名 | "pda001" |
| password | String | 是 | 密码 | "123456" |
| deviceCode | String | 是 | 设备编码 | "PDA-01" |

**请求示例：**
```json
{
  "username": "pda001",
  "password": "123456",
  "deviceCode": "PDA-01"
}
```

**响应示例（成功）：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expireAt": "2025-01-15 23:59:59",
    "userName": "张三",
    "roles": ["PDA_USER"]
  }
}
```

**响应示例（失败）：**
```json
{
  "code": 400,
  "message": "用户名或密码错误",
  "data": null
}
```

**业务说明：**
- Token有效期建议设置为24小时
- Token需要在后续所有接口的请求头中携带：`Authorization: Bearer {token}`
- 如果Token过期，返回401状态码，PDA会重新登录

---

### 3.2 托盘扫码接口

**接口说明：** 根据托盘二维码/条码，从WMS获取托盘信息。用于入库流程的第一步。

**接口地址：** `POST /api/pallet/scan`

**请求头：** 需要Token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| barcode | String | 是 | 托盘条码/二维码 | "PALLET-QR-CODE-11-01" |

**请求示例：**
```json
{
  "barcode": "PALLET-QR-CODE-11-01"
}
```

**响应示例（成功）：**
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

**响应示例（失败）：**
```json
{
  "code": 404,
  "message": "托盘不存在",
  "data": null
}
```

**业务说明：**
- 需要根据条码查询托盘基础信息
- 返回的`swapStation`是库前置换区站点编码，用于后续AGV任务
- 返回的`binCode`是该托盘对应的库位号，用于后续AGV任务和阀门绑定

---

### 3.3 阀门绑定接口

**接口说明：** 在WMS中绑定阀门与托盘、库位的关系。用于入库流程的阀门绑定步骤。

**接口地址：** `POST /api/valve/bind`

**请求头：** 需要Token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| valveNo | String | 是 | 阀门唯一编号 | "V20250101-001" |
| matCode | String | 是 | 物料编码 | "MAT-DN50-001" |
| valveModel | String | 是 | 阀门型号 | "DN50" |
| vendorName | String | 是 | 厂家名称 | "XX阀门厂" |
| inboundDate | String | 是 | 入库日期（yyyy-MM-dd格式） | "2025-01-15" |
| palletNo | String | 是 | 托盘号 | "11-01" |
| binCode | String | 是 | 库位号 | "2-01" |

**请求示例：**
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

**响应示例（成功）：**
```json
{
  "code": 200,
  "message": "绑定成功",
  "data": {
    "success": true
  }
}
```

**响应示例（失败）：**
```json
{
  "code": 400,
  "message": "阀门编号已存在",
  "data": null
}
```

**业务说明：**
- 需要校验阀门编号是否已存在
- 需要校验托盘号和库位号是否匹配
- 绑定成功后，阀门状态应设置为`IN_STOCK`（在库）
- 一个托盘可以绑定多个阀门（根据托盘型号：小托盘132个，大托盘33个）

---

### 3.4 阀门查询接口

**接口说明：** 根据条件查询阀门列表。用于送检、回库、出库流程的"选阀门"步骤。

**接口地址：** `POST /api/valve/query`

**请求头：** 需要Token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| vendorName | String | 否 | 厂家名称（模糊查询） | "XX阀门厂" |
| valveNo | String | 否 | 阀门编号（精确查询） | "V20250101-001" |
| valveModel | String | 否 | 阀门型号（模糊查询） | "DN50" |
| inboundDate | String | 否 | 入库日期（yyyy-MM-dd格式） | "2025-01-15" |
| valveStatus | String | 否 | 阀门状态（见枚举） | "IN_STOCK" |
| pageNum | Integer | 否 | 页码，默认1 | 1 |
| pageSize | Integer | 否 | 每页大小，默认20 | 20 |

**请求示例：**
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

**响应示例（成功）：**
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

**业务说明：**
- 支持多条件组合查询，所有条件都是可选的
- 需要支持分页查询
- 不同业务场景会传入不同的`valveStatus`：
  - 送检：查询`IN_STOCK`（在库）状态的阀门
  - 回库：查询`INSPECTED`（已检测）状态的阀门
  - 出库：查询`INSPECTED`（已检测）状态的阀门
- 返回的阀门信息需要包含`palletNo`和`binCode`，用于后续AGV任务

---

### 3.5 任务记录查询接口

**接口说明：** 从WMS查询历史任务记录列表。用于任务管理模块。

**接口地址：** `POST /api/task/query`

**请求头：** 需要Token

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| startDate | String | 否 | 起始日期（yyyy-MM-dd格式），默认当天 | "2025-01-15" |
| endDate | String | 否 | 结束日期（yyyy-MM-dd格式），默认当天 | "2025-01-15" |
| taskType | String | 否 | 任务类型（见枚举） | "INBOUND" |
| status | String | 否 | 任务状态（见枚举） | "PENDING" |
| pageNum | Integer | 否 | 页码，默认1 | 1 |
| pageSize | Integer | 否 | 每页大小，默认20 | 20 |

**请求示例：**
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

**响应示例（成功）：**
```json
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "list": [
      {
        "outID": "R20250115145830123",
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

**业务说明：**
- 需要支持按日期范围查询
- 需要支持按任务类型和状态筛选
- 如果`startDate`和`endDate`都为空，默认查询当天的任务
- 任务记录需要从AGV调度系统同步获取（`outID`是AGV生成的任务编号）
- 任务状态需要实时同步AGV的任务执行状态
- 只有`PENDING`（待执行）状态的任务可以被取消

---

## 4. 业务流程说明

### 4.1 入库流程

**流程步骤：**

1. **托盘扫码** → 调用`POST /api/pallet/scan`
   - 输入：托盘条码
   - 输出：托盘信息（包括`swapStation`和`binCode`）

2. **阀门绑定** → 调用`POST /api/valve/bind`
   - 输入：阀门信息 + 托盘号 + 库位号
   - 输出：绑定结果

3. **呼叫入库** → 调用AGV调度系统（不在WMS接口范围内）
   - PDA生成任务编号（R开头）
   - 调用AGV接口发送任务

**WMS需要做的事情：**
- 提供托盘扫码接口，返回托盘基础信息
- 提供阀门绑定接口，保存阀门与托盘、库位的关联关系
- 记录任务信息（从AGV同步或PDA上报）

---

### 4.2 送检流程

**流程步骤：**

1. **选阀门** → 调用`POST /api/valve/query`
   - 查询条件：`valveStatus = "IN_STOCK"`（在库状态）
   - 输出：阀门列表（包含`palletNo`和`binCode`）

2. **呼叫送检** → 调用AGV调度系统（不在WMS接口范围内）

3. **空托回库** → 调用AGV调度系统（不在WMS接口范围内）

**WMS需要做的事情：**
- 提供阀门查询接口，支持按状态筛选
- 当阀门送检后，更新阀门状态为`IN_INSPECTION`（检测中）

---

### 4.3 回库流程

**流程步骤：**

1. **选阀门** → 调用`POST /api/valve/query`
   - 查询条件：`valveStatus = "INSPECTED"`（已检测状态）
   - 输出：阀门列表

2. **呼叫托盘** → 调用AGV调度系统（不在WMS接口范围内）

3. **阀门回库** → 调用AGV调度系统（不在WMS接口范围内）

**WMS需要做的事情：**
- 提供阀门查询接口，支持按状态筛选
- 当阀门回库后，更新阀门状态为`IN_STOCK`（在库）

---

### 4.4 出库流程

**流程步骤：**

1. **选阀门** → 调用`POST /api/valve/query`
   - 查询条件：`valveStatus = "INSPECTED"`（已检测状态）
   - 输出：阀门列表

2. **呼叫出库** → 调用AGV调度系统（不在WMS接口范围内）

3. **空托回库** → 调用AGV调度系统（不在WMS接口范围内）

**WMS需要做的事情：**
- 提供阀门查询接口，支持按状态筛选
- 当阀门出库后，更新阀门状态为`OUTBOUND`（已出库）

---

### 4.5 任务管理流程

**流程步骤：**

1. **查询任务** → 调用`POST /api/task/query`
   - 输入：日期范围、任务类型、状态等筛选条件
   - 输出：任务列表

2. **取消任务** → 调用AGV调度系统（不在WMS接口范围内）
   - 只能取消`PENDING`（待执行）状态的任务

**WMS需要做的事情：**
- 提供任务查询接口，支持多条件筛选和分页
- 需要从AGV调度系统同步任务状态
- 当任务被取消后，更新任务状态为`CANCELLED`（已取消）

---

## 5. 注意事项

### 5.1 数据一致性

1. **阀门状态管理**
   - 阀门状态需要在业务流程中及时更新：
     - 入库绑定后：`IN_STOCK`
     - 送检后：`IN_INSPECTION`
     - 检测完成后：`INSPECTED`
     - 回库后：`IN_STOCK`
     - 出库后：`OUTBOUND`

2. **任务状态同步**
   - 需要从AGV调度系统同步任务状态
   - 任务状态包括：`PENDING`、`EXECUTING`、`COMPLETED`、`CANCELLED`、`FAILED`

### 5.2 接口性能

1. **响应时间**
   - 所有接口响应时间建议控制在500ms以内
   - 查询接口支持分页，避免一次性返回大量数据

2. **并发处理**
   - 需要考虑多台PDA同时操作的并发场景
   - 阀门绑定、状态更新等操作需要加锁处理

### 5.3 错误处理

1. **参数校验**
   - 所有必填参数必须校验
   - 日期格式、枚举值等需要严格校验

2. **业务校验**
   - 托盘扫码：校验托盘是否存在
   - 阀门绑定：校验阀门编号是否重复、托盘库位是否匹配
   - 阀门查询：校验查询条件合理性

3. **错误提示**
   - 错误信息要清晰明确，便于PDA端显示给用户

### 5.4 安全性

1. **Token管理**
   - Token需要设置有效期（建议24小时）
   - Token过期后返回401，PDA会重新登录
   - 建议使用JWT或其他安全的Token机制

2. **权限控制**
   - 不同用户可能有不同的操作权限
   - 建议在接口层面做权限校验

### 5.5 日志记录

1. **操作日志**
   - 建议记录所有接口调用日志
   - 记录关键业务操作（如阀门绑定、状态变更等）

2. **异常日志**
   - 记录所有异常情况，便于问题排查

### 5.6 与AGV调度系统的对接

1. **任务编号（outID）**
   - 任务编号由PDA生成，格式：R/S/H/C + 时间戳
   - WMS需要保存任务编号，用于任务查询和状态同步

2. **任务状态同步**
   - WMS需要从AGV调度系统同步任务执行状态
   - 可以通过定时轮询或AGV回调的方式同步

3. **库位编码（binCode）**
   - WMS的库位编码需要与AGV调度系统的binCode保持一致
   - 这是AGV任务执行的关键参数

---

## 6. 接口测试建议

### 6.1 测试用例

建议为每个接口编写以下测试用例：

1. **正常流程测试**
   - 参数完整、格式正确的正常请求

2. **参数校验测试**
   - 必填参数缺失
   - 参数格式错误
   - 参数值超出范围

3. **业务逻辑测试**
   - 数据不存在的情况
   - 数据重复的情况
   - 状态不允许的操作

4. **权限测试**
   - Token缺失
   - Token过期
   - Token无效

### 6.2 测试数据准备

建议准备以下测试数据：

1. **托盘数据**
   - 小托盘（SMALL）：132个
   - 大托盘（LARGE）：33个
   - 不同库位的托盘

2. **阀门数据**
   - 不同状态的阀门（在库、检测中、已检测、已出库）
   - 不同厂家、型号的阀门

3. **任务数据**
   - 不同状态的任务（待执行、执行中、已完成、已取消、失败）
   - 不同任务类型的任务（入库、送检、回库、出库）

---

## 7. 附录

### 7.1 接口清单汇总

| 序号 | 接口名称 | 接口地址 | 请求方法 | 说明 |
|------|----------|----------|----------|------|
| 1 | 登录接口 | `/api/auth/login` | POST | 用户登录，获取Token |
| 2 | 托盘扫码接口 | `/api/pallet/scan` | POST | 根据条码查询托盘信息 |
| 3 | 阀门绑定接口 | `/api/valve/bind` | POST | 绑定阀门与托盘、库位 |
| 4 | 阀门查询接口 | `/api/valve/query` | POST | 多条件查询阀门列表 |
| 5 | 任务记录查询接口 | `/api/task/query` | POST | 查询历史任务记录 |

### 7.2 枚举值汇总

**托盘型号（palletType）：**
- `SMALL`：小托盘（132个）
- `LARGE`：大托盘（33个）

**阀门状态（valveStatus）：**
- `IN_STOCK`：在库
- `IN_INSPECTION`：检测中
- `INSPECTED`：已检测
- `OUTBOUND`：已出库

**业务任务类型（taskType）：**
- `INBOUND`：入库
- `SEND_INSPECTION`：送检
- `RETURN`：回库
- `OUTBOUND`：出库

**任务状态（status）：**
- `PENDING`：待执行
- `EXECUTING`：执行中
- `COMPLETED`：已完成
- `CANCELLED`：已取消
- `FAILED`：失败

---

## 8. 联系方式

如有疑问，请联系PDA开发团队。

---

**文档版本：** v1.0  
**最后更新：** 2025-01-15

