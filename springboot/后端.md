# 后端服务详细说明

后端基于 Spring Boot 多模块架构，对管理员端与两类小程序提供统一 REST API，覆盖登录认证、公告、论坛、报修、出行码与缴费等业务。

## 模块结构

模块依赖定义在 [core-api/pom.xml](/springboot/core-api/pom.xml)：

- core-api：应用入口与资源配置
- core-auth：认证授权、JWT 校验、权限控制
- core-business：论坛、报修、出行码、缴费、公告等业务模块
- core-system：管理员、员工等系统能力
- core-domain：实体与 DTO
- core-common：通用工具、线程池、异常处理

## 启动入口

- 启动类：[DemoApplication](/springboot/core-api/src/main/java/com/example/DemoApplication.java)
- 资源配置目录：`core-api/src/main/resources/resources`

## 安全与权限

权限控制位于 [SecurityConfig](/springboot/core-auth/src/main/java/com/example/modules/auth/security/SecurityConfig.java)：

- 放行接口：登录、注册、验证码、微信登录、支付宝回调、文件下载
- 管理员接口：员工管理、后台统计、论坛管理、缴费账单等
- 普通用户接口：报修、出行码、论坛等
- 门卫/员工接口：出行码核验、工单处理等

## 认证流程

- 登录接口返回 JWT
- 前端统一通过 Authorization: Bearer token 访问受保护接口
- 认证逻辑在 JWT 过滤器与安全配置中完成

## 角色与权限

- 管理员：后台管理、审核、统计、论坛管理、缴费账单
- 员工：工单处理、公告发布、出行码核验
- 用户：报修、论坛、出行码、缴费等
- 门卫：出行码核验

## 业务接口概览

- 登录注册：/LoginRegister/userLogin、/LoginRegister/adminlogin、/LoginRegister/employeeLogin
- 公告：/notice/List、/notice/publish、/notice/saveDraft、/notice/{id}
- 报修：/repair/myRepair、/repair/submit、/repair/dispatchOrder、/repair/complete
- 论坛：/Forum/SelectPage、/Forum/delMyPost、/Forum/SelectAllComment
- 出行码：/travel-pass/selectPage、/travel-pass/verify
- 缴费：/api/fee/allBills、/api/fee/publishMonthly

## 配置与环境变量

配置文件：

- [application.yml](/springboot/core-api/src/main/resources/resources/application.yml)

关键环境变量：

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

## 数据库

初始化脚本：

- [hr888.sql](/数据库/hr888.sql)

示例表：

- admin：管理员账号与角色
- employee：员工账号与工种
- fee_bill：物业费账单
- access_log：访问日志

## 文件上传

文件上传路径由 `file.upload-path` 配置，默认指向 `C:\uploads\avatar`。

## 多线程与性能

线程池与并发模型说明：

- [多线程使用指南](/springboot/MULTITHREADING_GUIDE.md)
- [线程模型详解](/springboot/THREAD_MODEL_EXPLANATION.md)

## 本地运行

```
cd springboot
.\mvnw.cmd -pl core-api -am spring-boot:run
```
