# 员工端小程序详细说明

员工端小程序面向物业员工与门卫角色，提供工单处理、公告发布、论坛互动与出行码核验等能力。

## 目录结构

```
xiaochenxu_yuangong/
├─ pages/
│  ├─ login/                # 登录
│  ├─ index/                # 首页
│  ├─ repair/               # 工单列表与详情
│  ├─ notice/               # 公告列表与发布
│  ├─ scan/                 # 出行码扫码核验
│  ├─ forum/                # 论坛
│  └─ profile/              # 个人中心
├─ app.js
├─ app.json
└─ app.wxss
```

## 页面清单

页面配置位于 [app.json](file:///e:/grxm/Property%20management%20system/xiaochenxu_yuangong/app.json)：

- 登录：员工登录
- 首页：员工功能入口
- 报修：工单列表与详情
- 公告：公告列表与新增
- 扫码：出行码核验
- 论坛：帖子列表、详情与发布
- 个人中心：资料与密码修改

## 请求封装与认证

请求封装位于 [app.js](file:///e:/grxm/Property%20management%20system/xiaochenxu_yuangong/app.js)：

- baseUrl 默认 http://localhost:8080
- 登录成功后保存 token
- 未登录访问会自动跳转到登录页
- 文件下载链接自动拼接 token

## 功能说明与业务流程

- 工单处理：查看工单列表与详情，跟进处理结果
- 公告发布：新增公告并对外展示
- 论坛互动：帖子浏览、详情查看与发布
- 扫码核验：核销出行码，更新通行状态

## 页面结构细分

- 报修：列表与详情联动
- 公告：列表与新增发布
- 扫码：扫码核验入口
- 个人中心：资料与密码维护

## 开发与运行

使用微信开发者工具打开 xiaochenxu_yuangong 目录即可运行与调试。

## 关键配置

- 基础接口地址：`xiaochenxu_yuangong/app.js`
- 小程序 AppId：`xiaochenxu_yuangong/project.config.json`
