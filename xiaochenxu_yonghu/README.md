# 用户端小程序详细说明

用户端小程序面向住户，提供注册登录、公告阅读、报修、论坛、缴费与出行码等功能。

## 目录结构

```
xiaochenxu_yonghu/
├─ pages/
│  ├─ index/                 # 首页
│  ├─ login/                 # 登录
│  ├─ register/              # 注册
│  ├─ forum/                 # 论坛
│  ├─ notice/                # 公告
│  ├─ repair/                # 报修
│  ├─ pay/                   # 缴费
│  ├─ pass/                  # 出行码
│  └─ profile/               # 我的
├─ components/
├─ utils/
├─ app.js
├─ app.json
└─ app.wxss
```

## 页面清单

页面配置位于 [app.json](file:///e:/grxm/Property%20management%20system/xiaochenxu_yonghu/app.json)：

- 首页：首页入口
- 登录：账号登录
- 注册：住户注册
- 论坛：帖子列表、详情、发布
- 公告：列表与详情
- 报修：列表、提交、详情
- 缴费：账单列表、支付沙箱
- 出行码：出行码列表、记录详情
- 我的：资料编辑、密码修改

## 导航与入口

- 底部 Tab：首页、社区圈、我的
- 首页聚合公告、常用功能入口与状态提示

## 请求封装与认证

请求封装位于 [app.js](file:///e:/grxm/Property%20management%20system/xiaochenxu_yonghu/app.js)：

- baseUrl 默认 http://localhost:8080
- token 保存到本地缓存并在请求头自动带上 Bearer
- 支持文件下载链接自动拼接 token

## 功能说明与业务流程

- 登录注册：登录成功后保存 token，后续接口自动带上 Bearer
- 公告：公告列表与详情阅读
- 报修：提交工单、查看工单进度与详情
- 论坛：帖子浏览、发布与详情互动
- 缴费：账单列表与支付结果查看
- 出行码：出行码列表、记录详情
- 个人中心：资料维护与密码修改

## 页面结构细分

- 首页：公告/功能入口聚合展示
- 社区圈：列表、详情、发布
- 报修：列表、提交、详情三段式流程
- 缴费：账单列表与支付沙箱
- 出行码：出行码与记录分离展示

## 开发与运行

使用微信开发者工具打开 xiaochenxu_yonghu 目录即可运行与调试。

## 关键配置

- 基础接口地址：`xiaochenxu_yonghu/app.js`
- 小程序 AppId：`xiaochenxu_yonghu/project.config.json`
