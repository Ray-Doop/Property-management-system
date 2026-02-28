<template>
  <div class="bg">
    <div
      style="
        width: 350px;
        background-color: #fff;
        border-radius: 5px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
        padding: 40px 20px;
      "
    >
      <el-form ref="formRef" :model="data.form" :rules="data.rules">
        <div
          style="
            margin-bottom: 40px;
            text-align: center;
            font-weight: bold;
            font-size: 24px;
          "
        >
          智慧社区登录
        </div>
        <el-form-item prop="username">
          <el-input
            size="large"
            v-model="data.form.username"
            autocomplete="off"
            prefix-icon="User"
            placeholder="请输入账号"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            size="large"
            show-password
            v-model="data.form.password"
            autocomplete="off"
            prefix-icon="Lock"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div style="display: flex; gap: 10px; align-items: flex-start">
            <el-input
              size="large"
              v-model="data.form.captchaCode"
              autocomplete="off"
              placeholder="请输入验证码"
              style="flex: 1"
            />
            <div 
              style="
                width: 120px; 
                height: 40px; 
                cursor: pointer; 
                border: 1px solid #dcdfe6; 
                border-radius: 4px;
                display: flex;
                align-items: center;
                justify-content: center;
                background: #f5f7fa;
              "
              @click="refreshCaptcha"
              v-loading="captchaLoading"
            >
              <img 
                v-if="captchaImage" 
                :src="captchaImage" 
                alt="验证码"
                style="width: 100%; height: 100%; object-fit: contain;"
              />
              <span v-else style="color: #909399; font-size: 12px;">点击获取</span>
            </div>
          </div>
        </el-form-item>
        <div style="margin-bottom: 20px">
          <el-button
            style="width: 100%"
            size="large"
            type="primary"
            @click="login"
            :loading="loginLoading"
            >登录</el-button
          >
        </div>
        <div style="text-align: right; color: #6b7280; font-size: 12px">
          仅限管理员账号登录
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import request from "@/api/request.js";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

// 页面加载时自动获取验证码
onMounted(() => {
  refreshCaptcha();
});

const formRef = ref();
const captchaImage = ref("");
const captchaId = ref("");
const captchaLoading = ref(false);
const loginLoading = ref(false);

const data = reactive({
  form: {
    captchaId: "",
    captchaCode: ""
  },
  rules: {
    username: [
      { required: true, message: "请输入账号", trigger: "blur" },
      { min: 3, message: "账号最少 3 位", trigger: "blur" },
    ],
    password: [{ required: true, message: "请输入密码", trigger: "blur" }],
    captchaCode: [{ required: true, message: "请输入验证码", trigger: "blur" }],
  },
});

// 获取验证码
const refreshCaptcha = async () => {
  captchaLoading.value = true;
  try {
    const res = await request.get("/LoginRegister/captcha");
    if (res.code === "200") {
      captchaImage.value = res.data.imageBase64;
      captchaId.value = res.data.captchaId;
      data.form.captchaId = res.data.captchaId;
      data.form.captchaCode = ""; // 清空输入
    } else {
      ElMessage.error(res.msg || "获取验证码失败");
    }
  } catch (error) {
    console.error("获取验证码失败", error);
    ElMessage.error("获取验证码失败，请稍后重试");
  } finally {
    captchaLoading.value = false;
  }
};

const login = () => {
  localStorage.removeItem("code_user");
  formRef.value.validate((valid) => {
    if (valid) {
      // 确保验证码ID已设置
      if (!data.form.captchaId) {
        ElMessage.warning("请先获取验证码");
        refreshCaptcha();
        return;
      }
      
      loginLoading.value = true;
      
      // 先取出表单里的角色，拼接接口
      request.post("/LoginRegister/adminlogin", data.form).then((res) => {
        if (res.code == "200") {
          localStorage.setItem("code_user", JSON.stringify(res.data || {}));
          ElMessage.success("登录成功");

          const userRole = res.data.role; // 例如 ADMIN / USER / OPERATOR
          if (userRole === "ADMIN" || userRole === "SUPER_ADMIN" || userRole === "OPERATOR") {
            router.push("/dashboard");
          } else {
            ElMessage.error("无权限访问管理员后台");
            localStorage.removeItem("code_user");
          }
        } else {
          ElMessage.error(res.msg || "登录失败，请检查账号密码或验证码");
          // 登录失败后刷新验证码
          refreshCaptcha();
        }
      }).catch((err) => {
        console.error("登录请求失败:", err);
        ElMessage.error(err.response?.data?.msg || "登录失败，请稍后重试");
        // 登录失败后刷新验证码
        refreshCaptcha();
      }).finally(() => {
        loginLoading.value = false;
      });
    }
  });
};

</script>

<style scoped>
.bg {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background-image: url("@/assets/imgs/bg.jpg");
  background-size: cover;
}
</style>

