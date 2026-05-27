# 海典-金蝶开票对接中间程序

海典 WMS 系统与金蝶发票云之间的开票对接服务。海典通过 HTTP 接口提交单据，本程序写入中间表后立即触发金蝶开票，金蝶异步回调将结果写回中间表，海典再轮询查询结果。

## 技术栈

- Java 11 + Spring Boot 2.7.18
- MyBatis-Plus 3.5.5
- MySQL 5.7+ / 8.0+
- FastJSON2 2.0.43

## 业务流程

```
海典系统
  │  POST /api/invoice/submit（提交单据）
  ▼
InvoiceSubmitController → 写入中间表（is_invoicing=0）→ 立即触发开票
  │
  ▼
KingdeeInvoiceService → 调用金蝶开票 API
  │  1. getAppToken → 2. login（获取 accessToken）→ 3. openInvoice
  │
  ├─ 开票中（异步）→ 金蝶回调 POST /api/invoice/callback → 写入发票信息（is_invoicing=1）
  └─ 开票失败 → is_invoicing=2，记录失败原因

InvoiceMonitorService（定时任务，每10秒）
  └─ 轮询 is_invoicing=0 的记录，补偿触发开票（防止首次触发失败漏单）

海典系统
  └─ GET /api/invoice/status/{billNo}（轮询查询开票结果）
```

## 接口说明

### 1. 提交单据开票

**POST** `/api/invoice/submit`

请求体为 JSON 数组，每个元素对应一张单据：

```json
[
  {
    "billNo": "20260524000013",
    "billDate": "2026-01-28",
    "invoiceType": "10xdp",
    "buyerName": "购买方名称",
    "buyerTaxpayerId": "91440300XXXXXXXX",
    "buyerProperty": 0,
    "buyerRecipientMail": "xxx@example.com",
    "sellerName": "销售方名称",
    "sellerBankAndAccount": "银行 账号",
    "sellerAddressAndTel": "地址 电话",
    "totalAmount": 100.00,
    "includeTaxFlag": 0,
    "autoInvoice": 1,
    "drawer": "开票人",
    "billDetail": [
      {
        "goodsCode": "商品编码",
        "revenueCode": "税收分类编码",
        "amount": 100.00,
        "taxRate": 0.13,
        "quantity": 1,
        "units": "个",
        "lineProperty": "2"
      }
    ]
  }
]
```

响应：
```json
{ "code": 200, "message": "处理完成，成功 1 条", "data": null }
```

### 2. 金蝶开票结果回调

**POST** `/api/invoice/callback`

由金蝶发票云主动调用，无需海典触发。金蝶回调地址需在金蝶后台配置为：
`http://124.221.93.209:8080/api/invoice/callback`

判断开票成功的依据：`invoiceCode` 或 `invoiceNum` 任一不为空（全电发票无 invoiceCode，仅有 invoiceNum）。

必须返回：
```json
{ "success": true, "code": "0", "message": "success" }
```

> **注意**：金蝶回调的 `data` 字段可能是 JSON 数组，也可能是 base64 编码的字符串，程序已做兼容处理（`KingdeeCallbackRequest.decodeData()`）。

### 3. 查询开票状态

**GET** `/api/invoice/status/{billNo}`

响应示例（成功）：
```json
{
  "code": 200,
  "data": {
    "billNo": "20260524000013",
    "isInvoicing": 1,
    "invoiceCode": "",
    "invoiceNum": "24865868013476259840",
    "invoiceDate": "2026-05-26",
    "invoicePdfFileUrl": "https://..."
  }
}
```

`isInvoicing` 值：`0`-未开票/处理中，`1`-已开票，`2`-开票失败

## 数据库表结构

数据库名：`invoice_system`

| 表名 | 说明 |
|------|------|
| `t_sim_original_bill` | 开票中间表主表，主键 `id` 为 VARCHAR(50) |
| `t_sim_original_bill_item` | 开票中间表明细，通过 `billid` 关联主表 `id` |

初始化脚本：`src/main/resources/db/init.sql`

关键字段说明：

| 字段 | 说明 |
|------|------|
| `is_invoicing` | 开票状态：`0`-未开票，`1`-已开票，`2`-开票失败 |
| `invoice_num` | 发票号码（全电发票以此判断成功） |
| `invoice_code` | 发票代码（全电发票为空） |
| `invoice_pdf_file_url` | 发票 PDF 下载地址（回调写入） |
| `return_msg` | 失败原因 |

### 手动写入测试数据示例

**注意：必须先插入主表，再插入明细表**（明细表 `billid` 外键依赖主表 `id`）。

```sql
-- 第一步：插入主表
INSERT INTO t_sim_original_bill (
  id, bill_no, bill_date, invoice_property, invoice_type,
  buyer_name, buyer_taxpayer_id, buyer_address_and_tel, buyer_bank_and_account,
  buyer_recipient_mail, buyer_property, org_code,
  seller_name, seller_taxpayer_id, seller_address_and_tel, seller_bank_and_account,
  total_amount, include_tax_flag, auto_invoice, drawer, remark,
  is_invoicing, create_time, update_time
) VALUES (
  'TEST-HD-20260526-0001', 'HD-BILL-20260526-0001', '2026-05-26', 0, '10xdp',
  '购买方名称', '91440300XXXXXXXX', '地址 电话', '银行 账号',
  'xxx@example.com', 0, 'Org-00001',
  '金蝶软件（中国）有限公司', '915003006188392540', '广东省深圳市南山区 0755-12345678', '招商银行深圳分行 9876543210',
  100.00, 0, 1, '张三', '测试开票',
  0, NOW(), NOW()
);

-- 第二步：插入明细表
INSERT INTO t_sim_original_bill_item (
  id, billid, goods_name, goods_code, revenue_code,
  quantity, units, price, amount, tax_rate, tax_amount,
  line_property, privilege_flag, create_time, update_time
) VALUES (
  'TEST-HD-20260526-0001-D01', 'TEST-HD-20260526-0001',
  '旅游服务费', 'ATS318964000896', '3070301000000000000',
  1, '次', 100.00, 100.00, 0.06, 6.00,
  '2', '0', NOW(), NOW()
);
```

## 配置说明

主配置文件：`src/main/resources/application.yml`

### 金蝶发票云接口参数（演示环境）

| 配置项 | 值 |
|--------|-----|
| getAppToken 地址 | `https://cosmic-demo.piaozone.com/demo/api/getAppToken.do` |
| login 地址 | `https://cosmic-demo.piaozone.com/demo/api/login.do` |
| 开票接口地址 | `https://cosmic-demo.piaozone.com/demo/kapi/app/sim/openApi` |
| appId | `CQJC` |
| appSecret | `Z!GEm)[O&_%{H8` |
| accountId | `1640533801123708928` |
| 登录用户（手机号） | `13714962604` |
| businessSystemCode | `CQJC` |
| Token 缓存时间 | `55` 分钟 |

### 数据库参数

| 环境 | 地址 | 库名 | 用户名 | 密码 |
|------|------|------|--------|------|
| 本地开发 | `localhost:3306` | `invoice_system` | `root` | `123456` |
| 生产服务器 | `124.221.93.209:3306` | `invoice_system` | `invoice` | `Inv@2026#Prod` |

### 其他参数

| 配置项 | 说明 | 值 |
|--------|------|----|
| `server.port` | 服务端口 | `8080` |
| `monitor.poll-interval-seconds` | 定时轮询间隔（秒） | `10` |
| `monitor.batch-size` | 每次轮询最大处理条数 | `50` |

### 生产环境外部配置

生产环境使用 `/opt/invoice-assistant/application.yml` 覆盖 jar 内配置，不修改打包内的 yml。关键覆盖项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/invoice_system?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
    username: invoice
    password: Inv@2026#Prod
```

## 本地开发

```bash
# 编译打包
mvn clean package -DskipTests

# 启动（使用内置配置）
java -jar target/invoice-assistant-1.0.0.jar

# 启动（使用外部配置文件）
java -jar target/invoice-assistant-1.0.0.jar --spring.config.location=./application-prod.yml
```

## 部署

### 生产服务器信息

| 项目 | 值 |
|------|-----|
| 服务器公网 IP | `124.221.93.209` |
| SSH 登录 | `root@124.221.93.209`，密码 `WXY520@mm` |
| 应用端口 | `8080` |
| 金蝶回调地址 | `http://124.221.93.209:8080/api/invoice/callback` |
| 数据库地址 | `124.221.93.209:3306`，库名 `invoice_system` |
| 数据库用户 | `invoice` / `Inv@2026#Prod` |

### 服务器目录结构

```
/opt/invoice-assistant/
├── invoice-assistant-1.0.0.jar   # 应用 jar 包
├── application.yml               # 外部配置（优先级高于 jar 内配置）
└── app.log                       # 运行日志
```

### 完整重新部署步骤

**第一步：本地打包**
```bash
mvn clean package -DskipTests
# 产物：target/invoice-assistant-1.0.0.jar
```

**第二步：上传 jar 包（Windows 用 pscp，需安装 PuTTY）**
```bash
pscp -pw "WXY520@mm" -hostkey "ssh-ed25519 255 SHA256:n39j3TV8smoSOX97f/D133FMPUf/pG0z4ZOS8vhqsew" ^
  target/invoice-assistant-1.0.0.jar ^
  root@124.221.93.209:/opt/invoice-assistant/invoice-assistant-1.0.0.jar
```

也可通过腾讯云控制台「文件上传」功能上传到 `/opt/invoice-assistant/` 目录。

**第三步：SSH 登录服务器重启（Windows 用 plink）**
```bash
plink -ssh -pw "WXY520@mm" -batch ^
  -hostkey "ssh-ed25519 255 SHA256:n39j3TV8smoSOX97f/D133FMPUf/pG0z4ZOS8vhqsew" ^
  root@124.221.93.209 ^
  "kill -9 $(ps aux | grep invoice-assistant | grep -v grep | awk '{print $2}') 2>/dev/null; sleep 2; nohup java -jar /opt/invoice-assistant/invoice-assistant-1.0.0.jar --spring.config.location=/opt/invoice-assistant/application.yml > /opt/invoice-assistant/app.log 2>&1 &"
```

**第四步：验证启动**
```bash
plink -ssh -pw "WXY520@mm" -batch ^
  -hostkey "ssh-ed25519 255 SHA256:n39j3TV8smoSOX97f/D133FMPUf/pG0z4ZOS8vhqsew" ^
  root@124.221.93.209 "tail -20 /opt/invoice-assistant/app.log"
```

看到 `Tomcat started on port(s): 8080` 即为启动成功。

### 服务器上直接操作（已 SSH 登录后）

```bash
# 停止旧进程
kill -9 $(ps aux | grep invoice-assistant | grep -v grep | awk '{print $2}') 2>/dev/null

# 启动
nohup java -jar /opt/invoice-assistant/invoice-assistant-1.0.0.jar \
  --spring.config.location=/opt/invoice-assistant/application.yml \
  > /opt/invoice-assistant/app.log 2>&1 &

# 查看日志
tail -f /opt/invoice-assistant/app.log

# 查看进程
ps aux | grep invoice-assistant | grep -v grep
```

## 关键设计说明

1. **全电发票判断**：全电发票（`10xdp`/`08xdp`）无 `invoiceCode`，以 `invoiceNum` 不为空判断开票成功。

2. **幂等性**：`/submit` 接口对同一 `billNo` 重复提交会跳过，不会重复开票。

3. **乐观锁防并发**：`lockForInvoicing` 通过 `is_invoicing 0→1` 的原子 UPDATE 防止定时任务和首次触发并发重复开票。

4. **Token 缓存**：appToken 和 accessToken 缓存 55 分钟，Token 过期时自动刷新并重试一次，无需人工干预。

5. **回调兼容性**：金蝶回调的 `data` 字段存在两种格式——JSON 数组对象或 base64 编码字符串。`KingdeeCallbackRequest.decodeData()` 统一兼容处理，自动识别并解码。

6. **回调必须返回 success**：即使内部处理异常也返回 `{"success":true}`，避免金蝶无限重试。异常已记录日志，可通过 `app.log` 人工介入。

7. **补偿机制**：定时任务每 10 秒轮询 `is_invoicing=0` 的记录，对首次触发失败的单据自动补偿重试，防止漏单。
