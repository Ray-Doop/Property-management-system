# 物业管理系统总览

本项目由后端服务、管理员端 Web、用户端小程序、员工端小程序四部分组成，围绕物业管理的登录认证、公告、报修、论坛、出行码、缴费与审核等业务实现端到端闭环。



## 项目结构

```
Property management system/
├─ springboot/                  # 后端（Spring Boot 多模块）
│  ├─ core-api/                  # 应用入口与资源配置
│  ├─ core-auth/                 # 登录/认证/鉴权
│  ├─ core-business/             # 业务模块（论坛/报修/缴费/出行码等）
│  ├─ core-common/               # 通用配置与工具
│  ├─ core-domain/               # 实体与 DTO
│  ├─ core-system/               # 系统管理模块
│  └─ pom.xml                    # 父工程
├─ vue_guanliyuan/               # 管理员端（Vue 3 + Vite）
│  ├─ src/                       # 前端源码
│  ├─ dist/                      # 构建产物
│  └─ package.json
├─ xiaochenxu_yonghu/            # 用户端小程序
├─ xiaochenxu_yuangong/          # 员工端小程序
├─ 数据库/
│  └─ hr888.sql                  # 初始化脚本
└─ README.md
```

## 技术栈

- 后端：Spring Boot 3.4.7、MyBatis、Redis、JWT、MySQL、Alipay SDK
- 管理员端：Vue 3、Vite、Element Plus、Axios、WangEditor
- 小程序端：微信小程序原生框架
- 构建：Maven、Node.js

## 组件概览

- 后端服务：多模块 Spring Boot 服务，对外提供统一 REST API
- 管理员端：面向物业管理角色的管理后台
- 用户端小程序：住户侧业务入口，提供报修、缴费、出行码等功能
- 员工端小程序：员工侧业务入口，提供工单、公告、扫码核验等能力

## 总体功能

- 统一登录认证与权限控制
- 公告发布与阅读闭环
- 报修工单提交、派单、处理与评价
- 社区论坛发帖、评论与管理
- 出行码申请、核验与记录
- 物业缴费与账单管理

## 关键配置

### 后端配置

配置文件位置：

- `springboot/core-api/src/main/resources/resources/application.yml`

需要补充的环境变量（示例）：

```
ALIPAY_APP_ID=你的AppId
ALIPAY_PRIVATE_KEY=你的支付宝私钥
ALIPAY_PUBLIC_KEY=你的支付宝公钥
ALIPAY_NOTIFY_URL=你的回调地址
ALIPAY_RETURN_URL=你的同步返回地址
WECHAT_APPID=你的小程序AppId
WECHAT_SECRET=你的小程序AppSecret
JWT_SECRET=你的JWT签名密钥
```

文件上传位置：

- `file.upload-path` 默认指向 `C:\uploads\avatar`

### 管理员端配置

接口地址在 `vue_guanliyuan/src/api/request.js` 中配置：

```
baseURL: "http://localhost:8080"
```

如果后端端口或域名变化，请同步修改该配置。

## 运行方式

### 后端

进入后端目录并启动核心模块：

```
cd springboot
.\mvnw.cmd -pl core-api -am spring-boot:run
```

说明：

- 后端应用入口位于 [DemoApplication](file:///e:/grxm/Property%20management%20system/springboot/core-api/src/main/java/com/example/DemoApplication.java)
- 配置文件位于 [application.yml](file:///e:/grxm/Property%20management%20system/springboot/core-api/src/main/resources/resources/application.yml)

### 前端（管理员端）

```
cd vue_guanliyuan
npm install
npm run dev
```

### 数据库

将 `数据库/hr888.sql` 导入 MySQL：

```
mysql -u root -p < 数据库/hr888.sql
```

### 小程序（用户端/员工端）

使用微信开发者工具分别打开：

- [xiaochenxu_yonghu](file:///e:/grxm/Property%20management%20system/xiaochenxu_yonghu)
- [xiaochenxu_yuangong](file:///e:/grxm/Property%20management%20system/xiaochenxu_yuangong)

接口地址配置位于：

- [xiaochenxu_yonghu/app.js](file:///e:/grxm/Property%20management%20system/xiaochenxu_yonghu/app.js)
- [xiaochenxu_yuangong/app.js](file:///e:/grxm/Property%20management%20system/xiaochenxu_yuangong/app.js)

## 其他文档

- [多线程使用指南](/springboot/MULTITHREADING_GUIDE.md)
- [线程模型详解](/springboot/THREAD_MODEL_EXPLANATION.md)
>>>>>>> 010bde43 (补充md)
