import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";


// 创建 Axios 实例
const request = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 30000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const user = JSON.parse(localStorage.getItem("code_user") || "{}");
    const token = user?.token || "";

    // 携带 JWT
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }

    // ⚠️ 如果不是上传文件，且非 GET 请求，设置默认 Content-Type
    if (
      config.method !== "get" &&
      config.headers["Content-Type"] !== "multipart/form-data"
    ) {
      config.headers["Content-Type"] = "application/json;charset=utf-8";
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    let res = response.data;
    if (typeof res === "string") {
      try {
        res = JSON.parse(res);
      } catch (e) {}
    }

    // 统一处理权限或登录过期（不再强制登出/跳转，避免404等场景误触发）
    if (res.code === "401") {
      ElMessage.error(res.msg || "无效或过期的请求，请检查权限或重新尝试");
      return Promise.reject(res);
    }

    if (res.code === "403") {
      ElMessage.error(res.msg || "无权限访问该接口");
      return Promise.reject(res);
    }

    return res;
  },
  (error) => {
    const status = error.response?.status;
    if (status === 404) ElMessage.error("请求接口不存在");
    else if (status === 500) ElMessage.error("服务器内部错误");
    else if (status === 403) ElMessage.error("无权限访问该接口");
    else ElMessage.error("网络请求失败，请稍后重试");

    return Promise.reject(error);
  }
);

export default request;
