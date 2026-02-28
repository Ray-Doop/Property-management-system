<template>
  <div class="page-container">
    <div class="profile-layout">
      <!-- Left: User Card -->
      <div class="left-column">
        <div class="modern-card user-card">
          <div class="avatar-wrapper">
            <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/files/upload"
              :data="{ folder: 'avatar' }"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
            >
              <el-avatar :size="120" :src="getImageUrl(user.avatarUrl)" class="avatar" />
              <div class="upload-overlay">
                <el-icon><Camera /></el-icon>
              </div>
            </el-upload>
          </div>
          <h2 class="nickname">{{ user.nickname || user.username }}</h2>
          <p class="username">@{{ user.username }}</p>
          <div class="role-badge">
            <el-tag v-if="isAdmin" type="primary" effect="dark" round>管理员</el-tag>
            <el-tag v-else type="success" effect="dark" round>住户</el-tag>
          </div>
          
          <div class="action-btn">
            <el-button type="primary" @click="editDialog = true" round style="width: 100%">
              编辑资料
            </el-button>
          </div>
        </div>
      </div>

      <!-- Right: Info & Security -->
      <div class="right-column">
        <!-- Basic Info -->
        <div class="modern-card info-card">
          <div class="card-header">
            <h3 class="card-title">基本信息</h3>
            <span v-if="!isAdmin" class="header-hint">
              如需修改房产信息，请联系管理员
            </span>
          </div>
          
          <div class="info-grid">
            <template v-if="isAdmin">
              <div class="info-item">
                <span class="label">管理员ID</span>
                <span class="value">{{ user.adminId }}</span>
              </div>
              <div class="info-item">
                <span class="label">角色权限</span>
                <span class="value">{{ user.role }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机号码</span>
                <span class="value">{{ user.phone || '未绑定' }}</span>
              </div>
              <div class="info-item">
                <span class="label">电子邮箱</span>
                <span class="value">{{ user.email || '未绑定' }}</span>
              </div>
              <div class="info-item full-width">
                <span class="label">备注信息</span>
                <span class="value">{{ user.remark || '无' }}</span>
              </div>
            </template>

            <template v-else>
              <div class="info-item">
                <span class="label">手机号码</span>
                <span class="value">{{ user.phone }}</span>
              </div>
              <div class="info-item">
                <span class="label">车辆信息</span>
                <span class="value">{{ user.vehicleInfo || "暂无" }}</span>
              </div>
              <div class="info-item">
                <span class="label">楼栋号</span>
                <span class="value">{{ user.buildingNo }}栋</span>
              </div>
              <div class="info-item">
                <span class="label">单元号</span>
                <span class="value">{{ user.unitNo }}单元</span>
              </div>
              <div class="info-item">
                <span class="label">房间号</span>
                <span class="value">{{ user.roomNo }}室</span>
              </div>
              <div class="info-item">
                <span class="label">房产面积</span>
                <span class="value">{{ user.area }}㎡</span>
              </div>
            </template>
            
            <div class="info-item">
              <span class="label">注册时间</span>
              <span class="value">{{ user.createTime || user.registerTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">最后登录</span>
              <span class="value">{{ user.lastLoginTime }}</span>
            </div>
            <div class="info-item">
              <span class="label">账号状态</span>
              <span class="value">
                <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small" round>
                  {{ user.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </span>
            </div>
          </div>
        </div>

        <!-- Security -->
        <div class="modern-card info-card" style="margin-top: 24px;">
          <div class="card-header">
            <h3 class="card-title">安全设置</h3>
          </div>
          <el-form :model="pwdForm" label-position="top" class="pwd-form">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="旧密码">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="新密码">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="确认新密码">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
                </el-form-item>
              </el-col>
            </el-row>
            <div class="form-footer">
              <el-button type="primary" @click="updatePassword">更新密码</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- Edit Dialog -->
    <el-dialog v-model="editDialog" title="修改资料" width="500px" class="modern-dialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        
        <template v-if="isAdmin">
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" />
          </el-form-item>
        </template>

        <template v-else>
          <el-form-item label="车辆信息">
            <el-input v-model="form.vehicleInfo" />
          </el-form-item>
        </template>

      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="updateUserInfo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from "vue";
import request from "@/api/request";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Camera } from "@element-plus/icons-vue";
import defaultAvatar from "@/assets/default-avatar.png";
const router = useRouter();

const user = reactive({
  username: "",
  nickname: "",
  avatarUrl: "",
  phone: "",
  lastLoginTime: "",
  userId: null,
  vehicleInfo: "",
  registerTime: "",
  buildingNo: "",
  unitNo: "",
  roomNo: "",
  area: "",
  adminId: null,
  role: "",
  email: "",
  remark: "",
  status: 1,
  createTime: ""
});

const form = reactive({
  nickname: "",
  phone: "",
  vehicleInfo: "",
  email: "",
  remark: ""
});

const pwdForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const editDialog = ref(false);

const isAdmin = computed(() => {
  return user.role && (user.role.includes('ADMIN') || user.role === 'OPERATOR');
});

const getImageUrl = (url) => {
  if (!url) return defaultAvatar;
  try {
    const user = JSON.parse(localStorage.getItem("code_user") || "{}");
    const token = user.token || "";
    if (url.startsWith('http')) {
       return `${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
    }
    return `http://localhost:8080${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
  } catch {
    return defaultAvatar;
  }
};

onMounted(() => {
  let storedUser = localStorage.getItem("code_user");
  if (!storedUser) return;
  try {
    storedUser = JSON.parse(storedUser);
  } catch (e) {}
  
  const id = storedUser.adminId || storedUser.userId;
  const role = storedUser.role || "USER";

  let url = "/LoginRegister/GetUserData";
  let params = {};

  if (role.includes('ADMIN') || role === 'OPERATOR') {
    url = "/LoginRegister/GetAdminData";
    params = { adminId: id };
  } else {
    params = { userId: id };
  }

  request.get(url, { params: params }).then((res) => {
    if (res.code === "200") {
      Object.assign(user, res.data);
      if (res.data.avatar && !user.avatarUrl) {
         user.avatarUrl = res.data.avatar;
      }
      form.nickname = res.data.nickname;
      form.phone = res.data.phone;
      if (isAdmin.value) {
         form.email = res.data.email;
         form.remark = res.data.remark;
      } else {
         form.vehicleInfo = res.data.vehicleInfo;
      }
    }
  });
});

const handleAvatarSuccess = (res) => {
  if (res.code === "200") {
    user.avatarUrl = res.data;
    // Update backend
    updateUserInfo();
  }
};

const updateUserInfo = () => {
  let url = "/LoginRegister/UpdateUserData";
  let payload = { ...form, userId: user.userId, avatar: user.avatarUrl };
  
  if (isAdmin.value) {
    url = "/LoginRegister/UpdateAdminData";
    payload = { ...form, adminId: user.adminId, avatar: user.avatarUrl };
  }

  request.post(url, payload).then((res) => {
    if (res.code === "200") {
      Object.assign(user, form);
      const storedUser = JSON.parse(localStorage.getItem("code_user") || "{}");
      const newUser = { ...storedUser, ...form, avatarUrl: user.avatarUrl };
      localStorage.setItem("code_user", JSON.stringify(newUser));
      editDialog.value = false;
      ElMessage.success("资料修改成功");
    } else {
      ElMessage.error(res.msg || "修改失败");
    }
  });
};

const updatePassword = () => {
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    return ElMessage.error("两次输入的新密码不一致");
  }

  let checkUrl = "/LoginRegister/CheckOldPassword";
  let updateUrl = "/LoginRegister/UpdatePassword";
  let checkPayload = { username: user.username, password: pwdForm.oldPassword };
  let updatePayload = { username: user.username, password: pwdForm.newPassword };

  if (isAdmin.value) {
    checkUrl = "/LoginRegister/CheckOldAdminPassword";
    updateUrl = "/LoginRegister/UpdateAdminPassword";
  }

  request.post(checkUrl, checkPayload).then((res) => {
    if (res.code === "200") {
      request.post(updateUrl, updatePayload).then((r2) => {
        if (r2.code === "200") {
          ElMessage.success("密码修改成功，请重新登录");
          localStorage.removeItem("code_user");
          localStorage.removeItem("token");
          router.push("/login");
        } else {
          ElMessage.error(r2.msg || "修改失败");
        }
      });
    } else {
      ElMessage.error(res.msg || "旧密码错误");
    }
  });
};
</script>

<style scoped>
.page-container {
  padding: 0;
}

.profile-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.left-column {
  width: 320px;
  flex-shrink: 0;
}

.right-column {
  flex: 1;
  min-width: 0;
}

.modern-card {
  background: var(--surface-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-light);
  padding: 32px;
}

.user-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: 16px;
  cursor: pointer;
}

.avatar {
  border: 4px solid var(--surface-color);
  box-shadow: var(--shadow-md);
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 24px;
}

.avatar-wrapper:hover .upload-overlay {
  opacity: 1;
}

.nickname {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 4px 0;
}

.username {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 16px 0;
}

.role-badge {
  margin-bottom: 24px;
}

.action-btn {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}

.header-hint {
  font-size: 12px;
  color: var(--text-secondary);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item.full-width {
  grid-column: span 2;
}

.info-item .label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item .value {
  font-size: 15px;
  color: var(--text-main);
  font-weight: 500;
}

.pwd-form {
  max-width: 100%;
}

.form-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .profile-layout {
    flex-direction: column;
  }
  .left-column {
    width: 100%;
  }
}
</style>
