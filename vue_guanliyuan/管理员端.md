# 管理员端（Vue 3）详细说明

管理员端基于 Vue 3 与 Element Plus，实现管理员登录、数据统计、公告、论坛、报修、缴费、出行码与用户管理等核心能力。

## 目录结构

```
vue_guanliyuan/
├─ src/
│  ├─ api/                 # 接口封装
│  ├─ layouts/             # 布局
│  ├─ pages/               # 业务页面
│  ├─ router/              # 路由与权限
│  └─ components/          # 通用组件
├─ package.json
└─ vite.config.js
```

## 路由与页面

路由集中在 [index.js](/vue_guanliyuan/src/router/index.js)：

- 登录：/login
- 数据统计：/dashboard
- 个人信息：/profile
- 出行码：/travel/pass-list
- 公告：/system/notice/create、/system/notice/list、/system/notice/audit
- 用户与员工：/system/user、/system/employee、/system/approval
- 角色管理：/system/role
- 论坛管理：/forum/posts、/forum/posts/:id、/forum/comments、/forum/mute
- 报修管理：/repair/list、/repair/feedback
- 缴费管理：/pay/fee-list、/pay/result
- 登录日志：/monitor/logininfor

## 权限与登录流程

- 登录入口：/LoginRegister/adminlogin
- 登录成功后保存 token 到 localStorage
- 路由守卫在访问后台页面时校验 role
- 允许角色：ADMIN、SUPER_ADMIN、OPERATOR

实现位置：

- 路由守卫逻辑：[index.js](file:///e:/grxm/Property%20management%20system/vue_guanliyuan/src/router/index.js)

## API 访问与 Token 注入

请求封装位于 [request.js](file:///e:/grxm/Property%20management%20system/vue_guanliyuan/src/api/request.js)：

- baseURL 默认 http://localhost:8080
- 自动携带 Authorization: Bearer token
- 统一处理 401/403/404/500

## 功能模块详解

### 数据统计与概览

- 接口：/admin/stats/overview
- 内容：用户数、管理员数、帖子数等汇总数据
- 展示：折线图与趋势统计

### 公告管理

- 列表与搜索：/notice/List
- 发布与草稿：/notice/publish、/notice/saveDraft
- 详情与删除：/notice/{noticeId}、DELETE /notice/{id}
- 编辑器：WangEditor 富文本，附件上传至 /files/upload

### 报修工单

- 列表筛选：按状态、优先级查询
- 工单分配：调用分配接口，更新为已指派
- 取消工单：/repair/cancel/{orderId}
- 评价反馈：维修完成后评价与反馈

### 论坛管理

- 列表分页：/Forum/SelectPage
- 置顶与加精：更新 status 为 3/2
- 删除帖子：DELETE /Forum/delMyPost
- 评论管理：删除评论、禁言用户

### 缴费管理

- 账单查询：/api/fee/allBills
- 月度账单：/api/fee/publishMonthly
- 账单状态：已取消、待支付、已支付

### 出行码管理

- 列表筛选：/travel-pass/selectPage
- 扫码核验：/travel-pass/verify
- 状态流转：ISSUED → ENTERED → EXITED

### 用户与员工

- 用户审核：/LoginRegister/approval、/LoginRegister/pass、/LoginRegister/refuse
- 用户管理：/LoginRegister/selectAllUser、/LoginRegister/mute、/LoginRegister/ban
- 员工管理：/employee/selectPage、/employee/add、/employee/del

### 个人信息

- 基本信息：/LoginRegister/GetAdminData（Redis 缓存 20 分钟）
- 退出登录：/LoginRegister/logout

## 本地运行

```
cd vue_guanliyuan
npm install
npm run dev
```

## 配置项

- 接口地址：`vue_guanliyuan/src/api/request.js`
- 登录状态：localStorage 的 code_user 或 userInfo
