# 智慧物业系统项目说明

## 1. 项目概述
本项目为“智慧物业”一体化平台，覆盖住户端、员工端与管理员端三类使用场景，提供公告、报修、通行码、论坛、缴费支付等核心物业业务能力。系统采用前后端分离架构：后端基于 Spring Boot 多模块服务，管理员端为 Vue 3 管理后台，住户端与员工端为微信小程序。

## 2. 目录结构
- springboot/：后端多模块服务
  - core-api：接口层与 MyBatis Mapper 配置
  - core-auth：认证授权、登录与安全配置
  - core-business：业务模块（公告、报修、通行码等）
  - core-common：通用配置与工具（Redis、支付、异常、工具类等）
  - core-domain：领域实体与 DTO 定义
  - core-system：系统管理与基础能力
- vue-project/：管理员 Web 管理端（Vue 3 + Element Plus）
- xiaochenxu/：住户端微信小程序
- yuangong_xiaochenxu/：员工端微信小程序

## 3. 技术栈
### 3.1 后端
- Spring Boot 3.x、Spring Security + JWT
- MyBatis + MySQL
- Redis 缓存
- 支付宝 SDK
- Maven 构建，Java 17

### 3.2 管理员端 Web
- Vue 3 + Vite
- Element Plus、ECharts、wangeditor
- Axios、Vue Router

### 3.3 小程序端
- 微信小程序原生框架
- 组件化页面与自定义组件

## 4. 角色与端侧定位
- 住户端：面向业主/住户，处理日常物业服务与缴费
- 员工端：面向维修与核销岗位，处理工单与通行码核验
- 管理员端：面向运营与管理人员，进行全局业务管理与数据统计

## 5. 住户端功能详述
住户端功能详见 [住户端功能详细说明.md](file:///C:/Users/30274/Desktop/xm/xiaochenxu/住户端功能详细说明.md)，核心模块如下：
- 登录与注册：验证码登录、微信登录、用户注册、JWT Token 管理
- 个人信息：资料修改、头像更新、密码修改、文件上传
- 公告：公告列表、详情、已读标记、阅读统计
- 出行码：生成/刷新、验证、出行记录与统计
- 报修：提交报修、查询进度、取消、评价
- 论坛：发帖、查看详情、评论、收藏、审核状态
- 物业缴费：账单查询、支付订单创建、支付回调与结果查询
- 运营与监控：日志、性能监控、缓存与安全策略、工程化规范

## 6. 员工端功能详述
员工端功能详见 [员工端功能技术实现详细说明.md](file:///C:/Users/30274/Desktop/xm/yuangong_xiaochenxu/员工端功能技术实现详细说明.md)，核心模块如下：
- 登录与请求拦截：员工登录、Token 存储、权限拦截
- 个人信息：基础资料展示、退出登录
- 公告管理：公告列表与发布
- 出行码核销：扫码核验、状态更新
- 维修工单：接单、处理、完成、评价回复
- 论坛互动：帖子列表、详情、发布、评论、收藏与点赞

## 7. 管理员端功能详述
管理员端功能详见 [管理员端功能技术实现详细说明.md](file:///C:/Users/30274/Desktop/xm/vue-project/管理员端功能技术实现详细说明.md)，核心模块如下：
- 登录与权限：验证码登录、路由守卫、角色控制
- 个人信息：管理员资料与头像管理
- 数据概览：统计概览与可视化图表
- 公告管理：发布、草稿、详情、删除、阅读情况
- 出行码管理：通行码列表与状态筛选
- 报修管理：工单分派、状态管理与取消
- 论坛管理：帖子置顶/加精/删除、评论管理、禁言
- 缴费管理：账单查询、月度账单发布、状态管理
- 扫码核验：上传二维码图片验证并更新通行状态

## 8. 后端模块说明
后端为 Maven 多模块工程，核心模块职责如下：
- core-auth：登录、验证码验证、JWT 生成、权限校验
- core-api：接口与 Mapper 配置，统一 API 入口
- core-business：公告、报修、通行码、论坛、缴费等业务能力
- core-common：Redis、异常处理、线程池、支付配置与工具类
- core-domain：业务实体、DTO、枚举与消息模型
- core-system：系统管理与基础能力扩展

## 9. 核心数据模型（核心实体）
实体定义集中在 [core-domain](file:///C:/Users/30274/Desktop/xm/springboot/core-domain)：
- 用户与组织：User、Admin、Employee
- 公告：Notice、NoticeAttachment、NoticeRead
- 报修：RepairOrder、RepairCategory、RepairEvaluation、RepairAssignment
- 通行码：TravelPassRecord
- 论坛：ForumPost、ForumComment、ForumAttachment、ForumSection
- 缴费与支付：FeeBill、PayOrder
- 消息与日志：Message、MessageRecipient、AccessLog

## 10. 关键业务流程
### 10.1 登录与鉴权
- 账号登录/微信登录 → JWT Token 生成 → Redis 存储 → 前端持久化 → 请求拦截与鉴权

### 10.2 报修流程
- 住户提交报修 → 工单分派/接单 → 处理中 → 完成 → 住户评价 → 评价回复

### 10.3 通行码流程
- 住户生成通行码 → 员工/门卫扫码验证 → 状态流转（ISSUED→ENTERED→EXITED） → 记录与统计

### 10.4 缴费支付流程
- 账单生成 → 用户查询与选择 → 创建支付订单 → 支付宝回调 → 状态同步与通知

## 11. 前端运行说明
### 11.1 管理员端
- 运行方式：执行 Vite dev/bulid/preview 脚本
- 依赖说明：详见 [package.json](file:///C:/Users/30274/Desktop/xm/vue-project/package.json)

### 11.2 小程序端
- 住户端：使用微信开发者工具导入 xiaochenxu/
- 员工端：使用微信开发者工具导入 yuangong_xiaochenxu/

## 12. 相关文档索引
- 住户端功能详细说明：[住户端功能详细说明.md](file:///C:/Users/30274/Desktop/xm/xiaochenxu/住户端功能详细说明.md)
- 员工端功能技术实现：[员工端功能技术实现详细说明.md](file:///C:/Users/30274/Desktop/xm/yuangong_xiaochenxu/员工端功能技术实现详细说明.md)
- 管理员端功能技术实现：[管理员端功能技术实现详细说明.md](file:///C:/Users/30274/Desktop/xm/vue-project/管理员端功能技术实现详细说明.md)
