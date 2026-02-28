<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">用户管理</h2>
          <p class="section-subtitle">管理系统注册用户及账号状态</p>
        </div>
        <div class="right">
          <el-select
            v-model="statusFilter"
            placeholder="状态筛选"
            clearable
            style="width: 120px; margin-right: 10px"
            @change="fetchUsers"
          >
            <el-option label="正常" :value="1" />
            <el-option label="未激活" :value="0" />
            <el-option label="禁言" :value="2" />
            <el-option label="封禁" :value="3" />
          </el-select>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索用户昵称/ID"
            clearable
            prefix-icon="Search"
            class="search-input"
            @clear="fetchUsers"
            @keyup.enter="fetchUsers"
          />
          <el-button type="primary" @click="fetchUsers">
            <el-icon class="el-icon--left"><Search /></el-icon>搜索
          </el-button>
        </div>
      </div>

      <!-- 👥 用户表格 -->
      <el-table :data="users" v-loading="loading" style="width: 100%" class="modern-table">
        <!-- 头像 -->
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar :size="40" :src="row.avatarUrl || defaultAvatar" class="user-avatar" />
              <div class="user-text">
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
                <div class="username">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="phone" label="联系方式" width="140" />
        
        <el-table-column label="房产信息" min-width="180">
          <template #default="{ row }">
            <div v-if="row.buildingNo" class="house-info">
              <el-tag size="small" type="info" effect="plain">{{ row.buildingNo }}栋</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ row.unitNo }}单元</el-tag>
              <el-tag size="small" type="info" effect="plain">{{ row.roomNo }}室</el-tag>
            </div>
            <span v-else class="text-placeholder">未绑定房产</span>
          </template>
        </el-table-column>

        <el-table-column prop="area" label="面积" width="100">
          <template #default="{ row }">
            <span v-if="row.area">{{ row.area }}㎡</span>
            <span v-else class="text-placeholder">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="info" effect="light" round>未激活</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" effect="light" round>正常</el-tag>
            <el-tag v-else-if="row.status === 2" type="warning" effect="light" round>禁言</el-tag>
            <el-tag v-else-if="row.status === 3" type="danger" effect="light" round>封禁</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="200" fixed="right" align="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            
            <template v-if="row.status === 1">
               <el-button type="warning" link @click="muteUser(row)">禁言</el-button>
               <el-button type="danger" link @click="banUser(row)">封禁</el-button>
            </template>
            
            <template v-else-if="row.status === 0">
               <el-button type="success" link @click="activateUser(row)">激活</el-button>
            </template>

            <template v-else>
               <el-button type="success" link @click="passUser(row)">恢复</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 📄 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          v-model:page-size="pageSize"
          v-model:current-page="pageNum"
          @current-change="fetchUsers"
          @size-change="fetchUsers"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户档案" width="650px" destroy-on-close class="user-profile-dialog">
      <div class="profile-header">
        <div class="avatar-wrapper">
          <el-avatar :size="80" :src="detailForm.avatarUrl || defaultAvatar" class="profile-avatar" />
          <el-upload
            class="avatar-edit-trigger"
            action="http://localhost:8080/files/upload"
            :data="{ folder: 'avatar' }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
          >
            <div class="edit-mask"><el-icon><Camera /></el-icon></div>
          </el-upload>
        </div>
        <div class="profile-meta">
          <div class="main-name">{{ detailForm.nickname || '未设置昵称' }}</div>
          <div class="sub-meta">
            <el-tag size="small" effect="plain" type="info">ID: {{ detailForm.userId }}</el-tag>
            <el-tag size="small" effect="plain" type="info">@{{ detailForm.username }}</el-tag>
            <el-tag 
              size="small" 
              :type="detailForm.status === 1 ? 'success' : 'danger'"
              effect="light"
            >
              {{ detailForm.status === 1 ? '状态正常' : '异常状态' }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 信息修改 -->
        <el-tab-pane label="基本资料" name="info">
          <el-form 
            :model="detailForm" 
            :rules="detailRules"
            ref="detailFormRef"
            label-position="top" 
            class="compact-form"
          >
            <el-divider content-position="left">联系信息</el-divider>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="用户昵称" prop="nickname">
                  <el-input v-model="detailForm.nickname" prefix-icon="User" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="detailForm.phone" prefix-icon="Iphone" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-divider content-position="left">房产信息</el-divider>
            <div class="house-grid">
              <el-form-item label="楼栋" prop="buildingNo" class="grid-item">
                <el-input v-model="detailForm.buildingNo"><template #append>栋</template></el-input>
              </el-form-item>
              <el-form-item label="单元" prop="unitNo" class="grid-item">
                <el-input v-model="detailForm.unitNo"><template #append>单元</template></el-input>
              </el-form-item>
              <el-form-item label="房间" prop="roomNo" class="grid-item">
                <el-input v-model="detailForm.roomNo"><template #append>室</template></el-input>
              </el-form-item>
              <el-form-item label="面积" prop="area" class="grid-item">
                <el-input v-model="detailForm.area"><template #append>㎡</template></el-input>
              </el-form-item>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 密码修改 -->
        <el-tab-pane label="安全设置" name="password">
          <div class="security-panel">
            <div class="security-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <h3>重置登录密码</h3>
            <p class="security-tip">设置新密码后，用户下一次登录需要使用新密码。请确保操作经过授权。</p>
            
            <el-form 
              :model="passwordForm" 
              :rules="passwordRules"
              ref="passwordFormRef"
              label-width="0" 
              class="security-form"
            >
              <el-form-item prop="newPassword">
                <el-input 
                  type="password" 
                  v-model="passwordForm.newPassword" 
                  placeholder="请输入新密码（至少6位）" 
                  show-password
                  prefix-icon="Key"
                  size="large"
                />
              </el-form-item>
              <el-button type="primary" size="large" style="width: 100%" @click="saveUser">
                确认重置密码
              </el-button>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer" v-if="activeTab === 'info'">
          <el-button @click="detailVisible = false">取消</el-button>
          <el-button type="primary" @click="saveUser">保存修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessageBox, ElMessage } from "element-plus";
import { Search, Camera, Lock, Key, User, Iphone } from "@element-plus/icons-vue";
import request from "@/api/request";
import defaultAvatar from "@/assets/default-avatar.png";

const users = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const searchKeyword = ref("");
const statusFilter = ref(null);
const loading = ref(false);

const detailVisible = ref(false);
const activeTab = ref("info");
const detailForm = ref({});
const passwordForm = ref({ newPassword: "" });

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

const fetchUsers = async () => {
  loading.value = true;
  try {
    const res = await request.get("/LoginRegister/selectAllUser", {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        nickname: searchKeyword.value || "",
        status: statusFilter.value,
      },
    });
    if (res.code === "200") {
      users.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } finally {
    loading.value = false;
  }
};

const detailFormRef = ref();
const passwordFormRef = ref();

const detailRules = {
  nickname: [{ required: true, message: "请输入用户昵称", trigger: "blur" }],
  phone: [
    { required: true, message: "请输入手机号码", trigger: "blur" },
    { pattern: /^1[3-9]\d{9}$/, message: "手机号格式不正确", trigger: "blur" }
  ],
  buildingNo: [{ required: true, message: "请输入楼栋号", trigger: "blur" }],
  unitNo: [{ required: true, message: "请输入单元号", trigger: "blur" }],
  roomNo: [{ required: true, message: "请输入房间号", trigger: "blur" }],
  area: [{ required: true, message: "请输入面积", trigger: "blur" }]
};

const passwordRules = {
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" }
  ]
};

const openDetail = async (row) => {
  try {
    const res = await request.get("/LoginRegister/selectUserByUsername", {
      params: {
        username: row.username
      }
    });
    if (res.code === "200") {
      detailForm.value = { ...res.data };
      detailVisible.value = true;
      activeTab.value = "info";
      passwordForm.value.newPassword = "";
      if (detailFormRef.value) detailFormRef.value.clearValidate();
      if (passwordFormRef.value) passwordFormRef.value.clearValidate();
    }
  } catch (e) {
    ElMessage.error("获取详情失败");
  }
};

const handleAvatarSuccess = (res) => {
  if (res.code === "200") {
    detailForm.value.avatarUrl = res.data;
    ElMessage.success("头像上传成功");
  } else {
    ElMessage.error("头像上传失败");
  }
};

const saveUser = async () => {
  try {
    // 保存基本信息
    if (activeTab.value === "info") {
       if (!detailFormRef.value) return;
       await detailFormRef.value.validate(async (valid) => {
         if (valid) {
           const res = await request.post('/LoginRegister/UpdateUserData', detailForm.value);
           if (res.code === "200") {
             ElMessage.success("用户信息已更新");
             detailVisible.value = false;
             fetchUsers();
           } else {
             ElMessage.error(res.msg || "更新失败");
           }
         }
       });
    }
    
    // 保存密码（仅在密码 tab 且输入了新密码时）
    if (activeTab.value === "password" && passwordForm.value.newPassword) {
       if (!passwordFormRef.value) return;
       await passwordFormRef.value.validate(async (valid) => {
         if (valid) {
           await request.post("/LoginRegister/UpdatePassword", {
            userId: detailForm.value.userId,
            password: passwordForm.value.newPassword
          });
          ElMessage.success("密码重置成功");
          detailVisible.value = false;
          fetchUsers();
         }
       });
    }
  } catch (e) {
     console.error(e);
  }
};

// 状态操作
const activateUser = (row) => {
  const name = row.nickname || row.username || "";
  ElMessageBox.confirm(`确定要激活用户 "${name}" 吗？`, "提示", {
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消"
  }).then(async () => {
    try {
      const res = await request.post("/LoginRegister/pass", null, { params: { username: row.username } });
      if (res.code === "200") {
        ElMessage.success("激活成功");
        fetchUsers();
      } else {
        ElMessage.error(res.msg || "激活失败");
      }
    } catch (e) {
      ElMessage.error("激活失败");
    }
  });
};

const passUser = (row) => {
  const name = row.nickname || row.username || "";
  ElMessageBox.confirm(`确定要恢复用户 "${name}" 吗？`, "提示", {
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消"
  }).then(async () => {
    try {
      const res = await request.post("/LoginRegister/unmute", null, { params: { userId: row.userId } });
      if (res.code === "200") {
        ElMessage.success("恢复成功");
        fetchUsers();
      } else {
        ElMessage.error(res.msg || "恢复失败");
      }
    } catch (e) {
      ElMessage.error("恢复失败");
    }
  });
};

const muteUser = (row) => {
  const name = row.nickname || row.username || "";
  ElMessageBox.confirm(`确定要禁言用户 "${name}" 吗？`, "提示", {
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消"
  }).then(async () => {
    try {
      const res = await request.post("/LoginRegister/mute", null, { params: { userId: row.userId, remark: "管理员操作" } });
      if (res.code === "200") {
        ElMessage.success("禁言成功");
        fetchUsers();
      } else {
        ElMessage.error(res.msg || "禁言失败");
      }
    } catch (e) {
      ElMessage.error("禁言失败");
    }
  });
};

const banUser = (row) => {
  const name = row.nickname || row.username || "";
  ElMessageBox.confirm(`确定要封禁用户 "${name}" 吗？`, "提示", {
    type: "warning",
    confirmButtonText: "确定",
    cancelButtonText: "取消"
  }).then(async () => {
    try {
      const res = await request.post("/LoginRegister/ban", null, { params: { userId: row.userId, remark: "管理员操作" } });
      if (res.code === "200") {
        ElMessage.success("封禁成功");
        fetchUsers();
      } else {
        ElMessage.error(res.msg || "封禁失败");
      }
    } catch (e) {
      ElMessage.error("封禁失败");
    }
  });
};

onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.page-container {
  padding: 0;
}

.modern-card {
  background: var(--surface-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-light);
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
  color: var(--text-main);
  line-height: 1.2;
}

.section-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 4px 0 0;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 240px;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  border: 1px solid var(--border-light);
}

.user-text {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: 500;
  color: var(--text-main);
  font-size: 14px;
}

.username {
  font-size: 12px;
  color: var(--text-secondary);
}

.house-info {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.text-placeholder {
  color: var(--text-placeholder);
  font-size: 13px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.user-profile-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.profile-header {
  background: linear-gradient(135deg, var(--el-color-primary-light-9) 0%, #ffffff 100%);
  padding: 30px;
  display: flex;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid var(--border-light);
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
}

.profile-avatar {
  border: 4px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-edit-trigger {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 28px;
  height: 28px;
  background: var(--el-color-primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  border: 2px solid #fff;
  transition: all 0.2s;
}

.avatar-edit-trigger:hover {
  transform: scale(1.1);
  background: var(--el-color-primary-dark-2);
}

.profile-meta {
  flex: 1;
}

.main-name {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-main);
  margin-bottom: 8px;
}

.sub-meta {
  display: flex;
  gap: 8px;
}

.profile-tabs {
  padding: 0 30px 20px;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-light);
}

.compact-form {
  padding-top: 20px;
}

.house-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  background: var(--el-fill-color-light);
  padding: 20px;
  border-radius: 8px;
}

.grid-item {
  margin-bottom: 0 !important;
}

.security-panel {
  text-align: center;
  padding: 40px 20px;
}

.security-icon {
  font-size: 48px;
  color: var(--el-color-warning);
  margin-bottom: 16px;
}

.security-tip {
  color: var(--text-secondary);
  margin-bottom: 30px;
}

.security-form {
  max-width: 360px;
  margin: 0 auto;
}
</style>
