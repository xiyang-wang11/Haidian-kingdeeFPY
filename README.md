# 海典-金蝶开票对接程序 部署说明

## 一、环境要求

- JDK 11+
- Maven 3.6+
- MySQL 5.7+ / 8.0+

## 二、数据库初始化

```sql
-- 1. 创建数据库
CREATE DATABASE invoice_db DEFAULT CHARACTER SET utf8mb4;

-- 2. 执行建表脚本
source src/main/resources/db/init.sql
```

## 三、修改配置

编辑 `src/main/resources/application.yml`，修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://你的数据库地址:3306/invoice_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: 数据库用户名
    password: 数据库密码

kingdee:
  api:
    app-id: CQJC
    app-secret: "Z!GEm)[O&_%{H8"
    account-id: "1640533801123708928"
    user: "13714962604"
```

## 四、编译打包

```bash
mvn clean package -DskipTests
```

## 五、启动运行

```bash
java -jar target/invoice-assistant-1.0.0.jar
```

或指定外部配置文件：

```bash
java -jar target/invoice-assistant-1.0.0.jar --spring.config.location=./application.yml
```

## 六、接口说明

### 1. 金蝶开票结果回调接口

**POST** `/api/invoice/callback`

金蝶发票云开票完成后，调用此接口将结果写入中间表。

请求体示例（成功）：
```json
{
  "billNo": "HD2025001",
  "success": true,
  "invoiceCode": "044001900111",
  "invoiceNo": "12345678",
  "invoiceDate": "20250715",
  "qrCode": "...",
  "kingdeeResponse": "{...}"
}
```

请求体示例（失败）：
```json
{
  "billNo": "HD2025001",
  "success": false,
  "failReason": "购方税号不存在"
}
```

### 2. 查询开票状态接口

**GET** `/api/invoice/status/{billNo}`

供海典系统查询某单据的开票状态。

响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "billNo": "HD2025001",
    "isInvoicing": "已开票",
    "invoiceCode": "044001900111",
    "invoiceNo": "12345678",
    "invoiceDate": "20250715"
  }
}
```

## 七、业务流程说明

```
海典系统
  │
  │  写入中间表（is_invoicing='未开票'）
  ▼
invoice_middle 表
  │
  │  本程序每10秒轮询一次
  ▼
InvoiceMonitorService（定时任务）
  │
  │  发现未开票记录，状态改为'开票中'（乐观锁防并发）
  ▼
KingdeeAuthService（获取/缓存 appToken + accessToken）
  │
  ▼
KingdeeInvoiceService（调用金蝶开票接口）
  │
  ├─ 成功 → 状态改为'已开票'，写入发票号码等信息
  └─ 失败 → 状态改为'开票失败'，记录失败原因，retry_count+1
              （retry_count <= 3 时下次轮询会重试）

金蝶发票云（异步回调）
  │
  │  POST /api/invoice/callback
  ▼
InvoiceCallbackController（写入开票结果到中间表）
```

## 八、中间表状态说明

| is_invoicing | 含义 |
|---|---|
| 未开票 | 海典写入，等待处理 |
| 开票中 | 本程序已抢占，正在调用金蝶接口 |
| 已开票 | 金蝶开票成功 |
| 开票失败 | 调用失败，retry_count<=3时会自动重试 |

## 九、注意事项

1. **中间表字段对应**：收到Excel表结构后，需核对 `InvoiceMiddle.java` 和 `InvoiceMiddleDetail.java` 中的字段与实际表结构是否一致，并同步修改 `init.sql`。
2. **金蝶接口参数**：`OpenInvoiceRequest` 中的字段名（如 `fpqqlsh`、`gfmc` 等）需根据金蝶实际接口文档确认。
3. **重试机制**：`retry_count > 3` 的记录不再自动重试，需人工介入处理。
4. **Token缓存**：appToken 和 accessToken 默认缓存55分钟，如金蝶实际过期时间不同，调整 `token-expire-minutes` 配置。
