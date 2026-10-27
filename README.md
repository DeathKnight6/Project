# 模拟股票交易系统

前后端分离的模拟股票交易系统：注册登录、行情浏览、买卖下单、撮合成交、持仓与订单管理。

- **后端**：Java 25 + Spring Boot 3.5 + Spring Data JPA + Spring Security + JWT，开发库用 H2 内存库
- **前端**：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router
- **端口**：后端 8080，前端 5173

---

## 一、快速开始

### 1. 前置依赖

| 依赖 | 版本要求 | 检测命令 |
|------|---------|---------|
| JDK | 25+ | `java -version` |
| Maven | 3.8+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |

> 本机已装 JDK 25（`D:\jdk-25\jdk-25.0.2`）与 Maven 3.9.9（`D:\apache-maven-3.9.9`）。
> 若这两个目录不在系统 `PATH` 中，`start.ps1`、`start.sh` 和 `.vscode/tasks.json` 已内置回退路径，无需额外配置。

### 2. 一键启动

**Windows 双击启动（最简单）：**

在资源管理器中双击项目根目录的 **`启动系统.bat`**，前后端就绪后会自动打开浏览器。启动失败时窗口会保留错误信息。使用过程中保留启动窗口，按 `Ctrl+C` 停止本次启动的服务。

**Windows PowerShell（推荐，无需安装 Bash）：**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\start.ps1
```

该命令仅为这一次脚本执行设置执行策略，不修改系统的永久设置。脚本会自动查找 JDK 25+、Maven 和 Node.js；使用 `npm.cmd` 避免 `npm.ps1` 的执行策略问题。日志写入 `.run/`。

只检查环境：`powershell -NoProfile -ExecutionPolicy Bypass -File .\start.ps1 -Check`。
首次下载依赖较慢时，可在启动命令末尾添加 `-StartupTimeoutSeconds 600`。

**Git Bash / Linux / macOS：**

```bash
bash start.sh
```

脚本会依次启动后端与前端，并在终端打印访问地址；`Ctrl+C` 会停止本次启动的服务。Windows 脚本会复用已就绪的现有服务。如果两个服务已经运行，直接显示访问地址后退出。

### 3. 或在 VS Code 中启动

- 运行任务 `start-all`（`Ctrl+Shift+B`）——并行启动前后端
- 或按 `F5` 选择 **Full Stack Debug** 复合调试——同时调试 Java 后端与 Chrome 中的前端

### 4. 访问地址

| 入口 | 地址 |
|------|------|
| 前端页面 | http://localhost:5173 |
| 登录页 | http://localhost:5173/login |
| 后端 API | http://localhost:8080/api |
| H2 控制台（需手动启用） | http://localhost:8080/h2-console |

H2 控制台登录信息：JDBC URL `jdbc:h2:mem:stockdb`，用户名 `sa`，密码留空。

控制台默认关闭。仅在本地调试需要时，启动前设置环境变量 `H2_CONSOLE_ENABLED=true`；远程访问始终关闭。
PowerShell 示例：`$env:H2_CONSOLE_ENABLED='true'`，然后在同一终端启动后端。

---

## 二、默认账号

| 角色 | 登录名 | 密码 | 初始资金 | 预置底仓 |
|------|--------|------|---------|---------|
| 普通用户 | `user001` | `123456` | 100,000 元 | 600519 × 1000 股、000001 × 2000 股 |
| 管理员 | `admin` | `admin123` | 1,000,000 元 | 5 只股票各 1000 股 |

管理员 `type=1`，普通用户 `type=0`。新注册用户自动获赠 100,000 元模拟资金，但**不含底仓**。

预置 5 只股票：贵州茅台(600519)、平安银行(000001)、招商银行(600036)、中国平安(601318)、宁德时代(300750)。

> **关于底仓**：卖出必须先持有股票。若所有账号都从零持仓起步，通过界面永远无法产生一笔成交，
> 撮合功能也就无从演示——因此 `DataInitializer` 为两个演示账号预置了底仓。
> 若不需要（例如作为正式作业提交，要求严格贴合「只有资金没有持仓」的初始状态），
> 删掉 `init/DataInitializer.java` 中的 `initPositions()` 调用即可，其余逻辑不受影响。

---

## 三、项目结构

```
stock-trading-system/
├── .vscode/                  # VS Code 任务与调试配置
├── backend/                  # Spring Boot 后端
│   └── src/main/java/com/stocktrading/
│       ├── config/           # SecurityConfig（含 CORS）
│       ├── controller/       # REST 接口
│       ├── service/          # 业务逻辑（含撮合引擎）
│       ├── repository/       # 数据访问
│       ├── entity/           # JPA 实体
│       ├── dto/              # 请求/响应对象
│       ├── security/         # JWT 工具与过滤器
│       ├── exception/        # 全局异常与错误码
│       └── init/             # 启动数据初始化
├── frontend/                 # Vue 3 前端
│   └── src/
│       ├── api/              # Axios 封装与接口定义
│       ├── router/           # 路由与登录守卫
│       ├── stores/           # Pinia 状态
│       ├── views/            # 页面
│       ├── components/       # 公共组件
│       └── utils/            # 工具函数
├── README.md
├── 启动系统.bat              # 双击启动并打开浏览器
├── start.ps1                 # Windows PowerShell 启动入口
└── start.sh
```

---

## 四、数据库设计

H2 内存库，JPA 自动建表（`ddl-auto: create-drop`，重启即重置）。

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `users` | 用户 | id, name, login_name, passwd, type, cny_free, cny_frozen, created_at |
| `stock` | 股票 | id, code, name, current_price |
| `orders` | 订单 | id, user_id, stock_id, type, price, undealed, dealed, canceled, created_at |
| `user_position` | 持仓 | user_id, stock_id, num_free, num_frozen（联合主键） |
| `transactions` | 成交 | id, buy_order_id, sell_order_id, price, amount, created_at |
| `trading_mutex` | 交易入口互斥锁 | 固定 id=1，下单和撤单共用 |

关系：`users` 1–N `orders` / `user_position`，`stock` 1–N `orders` / `user_position`，
`orders` 通过 `buy_order_id`、`sell_order_id` 关联到 `transactions`。

### 切换到 MySQL

编辑 `backend/src/main/resources/application.yml`，用文件末尾注释里的 MySQL 配置替换 `datasource` 与 `jpa` 段，
并把 `ddl-auto` 改为 `update` 即可。

---

## 五、核心业务规则

### 撮合规则

1. 买入价 ≥ 卖出价才成交。
2. **价格优先**：买单价高者先成交，卖单价低者先成交。
3. **时间优先**：同价时先挂单者先成交。
4. **成交价取先到一方（挂单方）的价格**：新订单总是去吃委托簿里已挂的单，因此成交价就是对手挂单价。
5. 支持**部分成交**：成交量 = min(买单未成交, 卖单未成交)，剩余部分继续挂单。

### 资金与持仓冻结

| 动作 | 资金 | 股票 |
|------|------|------|
| 买入下单 | `cny_free` 划出 `price × amount` 到 `cny_frozen` | — |
| 卖出下单 | — | `num_free` 划出 `amount` 到 `num_frozen` |
| 成交（买方） | 按委托价释放冻结，按成交价实付，**价差退回可用** | `num_free` 增加 |
| 成交（卖方） | `cny_free` 增加成交款 | `num_frozen` 扣减 |
| 撤单 | 未成交部分的冻结资金退回 `cny_free` | 未成交部分的冻结股票退回 `num_free` |

已成交部分不可撤销。买单出价高于成交价时，多冻结的差额会自动退回可用资金。

### 并发控制与输入限制

- 下单和撤单在读取业务数据前，先锁定 `trading_mutex` 的唯一一行；锁随整个事务提交或回滚释放。
- 交易事务串行执行，避免撤单与撮合互相等待，也避免同时到达的买卖订单互相不可见而遗留可成交挂单。
- 用户、持仓和对手订单仍保留行锁。此方案适合课程演示和小规模模拟交易，所有股票共享交易入口，吞吐量受限；高并发场景需要重新设计订单队列和账户结算流程。
- 单笔数量为 1～1,000,000 股的整数，价格最多保留两位小数，单笔委托金额不超过 100,000,000 元。前后端均检查，服务层再次校验。
- 小数数量、未知买卖方向或错误 JSON 返回 HTTP 400 与业务码 `1001`。

### 认证

登录成功返回 JWT，前端存入 `localStorage`，后续请求携带 `Authorization: Bearer <token>`。
密码使用 BCrypt 哈希存储，不落明文。

---

## 六、API 规范

统一响应格式：

```json
{ "code": 0, "message": "success", "data": {} }
```

`code = 0` 表示成功，非 0 为业务错误码（见 `exception/ErrorCode.java`）。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/auth/login` | 登录 | 否 |
| POST | `/api/auth/register` | 注册 | 否 |
| GET | `/api/auth/me` | 当前用户信息 | 是 |
| GET | `/api/stock/list` | 股票列表 | 否 |
| POST | `/api/trade/place` | 下单 | 是 |
| POST | `/api/trade/cancel/{orderId}` | 撤单 | 是 |
| GET | `/api/portfolio/summary` | 资产汇总 | 是 |
| GET | `/api/portfolio/positions` | 持仓列表 | 是 |
| GET | `/api/portfolio/orders?page=0&size=50` | 订单列表（分页） | 是 |

订单接口支持 `openOnly=true`：由后端先筛选未撤销且有未成交数量的订单，再分页。交易页使用该参数，较早的挂单不会被最近的历史订单遮蔽；同一时间的订单按 ID 倒序排列。

### 接口调用示例

```bash
# 1. 登录拿 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginName":"user001","passwd":"123456"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. 查看股票列表
curl -s http://localhost:8080/api/stock/list

# 3. 买入 600519 10 股，出价 1680.00（初始可用资金为 100000 元）
curl -s -X POST http://localhost:8080/api/trade/place \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"stockId":1,"type":"BUY","price":1680.00,"amount":10}'

# 4. 查看持仓
curl -s http://localhost:8080/api/portfolio/positions -H "Authorization: Bearer $TOKEN"
```

> 注：`type` 字段取值为 `BUY` / `SELL`（枚举名），而不是 0/1。

### 撮合演示

没有对手盘时订单会挂在委托簿上。用两个预置账号对敲即可完整看到「挂单 → 撮合 → 交割」：

```bash
# 1) user001 挂卖单 100 股 @ 1650.00 —— 无对手方，进入委托簿
# 2) admin   下买单 100 股 @ 1700.00 —— 与上面挂单撮合
#    成交价为 1650.00（挂单方价格，不是买方出价 1700）
#    admin 多冻结的 (1700-1650)×100 = 5000 元自动退回可用资金
# 3) user001 持仓 -100 股、可用资金 +165000 元
#    admin   持仓 +100 股、可用资金 -165000 元
```

---

## 七、测试

```bash
cd backend && mvn test
```

`MatchingEngineTest` 覆盖 10 个用例：完全成交、部分成交、价格优先、时间优先、成交价取挂单方并退还差价、
价格不交叉不成交、撤单解冻、重复撤单被拒、资金不足、可用股票不足。

`TradingIntegrationTest` 另有 7 个用例：20 笔并发买卖订单、撤单与成交竞争时的资金及股票守恒、历史订单超过 50 条时仍能查询早期挂单、未登录访问 `/api/auth/me`、超量/超额委托、非法买卖方向及小数数量。并发测试使用独立事务与独立测试数据库。

前端检查与构建：`cd frontend` 后执行 `npm run build`。

---

## 八、常见问题

| 问题 | 排查方向 |
|------|---------|
| `mvn` 找不到 | 安装 Maven，或确认 `D:\apache-maven-3.9.9\bin` 已加入 `PATH` |
| Maven 用了 JDK 8 编译报错 | 设置 `JAVA_HOME` 指向 JDK 25+，或在 `tasks.json` 的 `env` 中改路径 |
| 前端跨域 | 检查 `vite.config.ts` 的 `server.proxy` 是否把 `/api` 转发到 8080 |
| 接口返回 401 | token 缺失或已过期，重新登录；清理 `localStorage` 后重试 |
| H2 表不存在 | 确认 `ddl-auto: create-drop`；内存库重启即重置 |
| 端口占用 | 改 `application.yml` 的 `server.port` 与 `vite.config.ts` 的 `server.port` |
| 前端 esbuild 未执行 postinstall | `npm approve-scripts --allow-scripts-pending` |

---

## 九、可选扩展

以下功能本文档未实现，可作为后续迭代方向：

- 接入真实行情 API
- WebSocket 实时推送价格与成交回报
- K 线图与交易深度图（`components/` 下预留位置）
- Redis 缓存行情
- Docker Compose 一键部署
- 管理员后台（当前 `admin` 账号仅有 `type=1` 标识，未开放管理接口）
- 量化策略回测框架
