<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">用户审批</h2>
          <p class="section-subtitle">审核新用户注册申请与资料变更</p>
        </div>
        <div class="right">
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

      <!-- 👥 审批用户表格 -->
      <el-table :data="users" v-loading="loading" style="width: 100%" class="modern-table">
        <!-- 头像 -->
        <el-table-column label="申请用户" min-width="200">
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

        <el-table-column prop="phone" label="手机号" width="140" />
        
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
            <el-tag v-if="row.status === 0" type="warning" effect="light" round>待审核</el-tag>
            <el-tag v-else-if="row.status === 1" type="success" effect="light" round>通过</el-tag>
            <el-tag v-else-if="row.status === 2" type="danger" effect="light" round>拒绝</el-tag>
            <el-tag v-else type="info">未知</el-tag>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-button type="success" link @click="passUser(row)">通过</el-button>
            <el-button type="danger" link @click="refuseUser(row)">拒绝</el-button>
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
    <el-dialog v-model="detailVisible" title="修改用户资料" width="600px" destroy-on-close class="modern-dialog">
      <el-tabs v-model="activeTab" class="detail-tabs">
        <!-- 信息修改 -->
        <el-tab-pane label="基本信息" name="info">
          <div class="form-container">
             <div class="avatar-section">
                <el-avatar :size="80" :src="getImageUrl(detailForm.avatarUrl)" />
                <el-upload
                  class="avatar-uploader"
                  action="http://localhost:8080/files/upload"
                  :data="{ folder: 'avatar' }"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                >
                  <el-button type="primary" link>更换头像</el-button>
                </el-upload>
             </div>
             
             <el-form :model="detailForm" label-width="80px" style="flex: 1">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="昵称">
                    <el-input v-model="detailForm.nickname" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="手机号">
                    <el-input v-model="detailForm.phone" />
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-divider content-position="left">房产信息</el-divider>
              
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="楼栋号">
                    <el-input v-model="detailForm.buildingNo"><template #append>栋</template></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="单元号">
                    <el-input v-model="detailForm.unitNo"><template #append>单元</template></el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="房间号">
                    <el-input v-model="detailForm.roomNo"><template #append>室</template></el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="面积">
                 <el-input v-model="detailForm.area"><template #append>㎡</template></el-input>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 密码修改 -->
        <el-tab-pane label="安全设置" name="password">
          <el-form :model="passwordForm" label-width="100px" style="max-width: 400px; margin: 20px auto;">
            <el-form-item label="重置密码">
              <el-input 
                type="password" 
                v-model="passwordForm.newPassword" 
                placeholder="请输入新密码" 
                show-password
              />
            </el-form-item>
            <el-alert title="重置密码后，用户需使用新密码登录" type="warning" :closable="false" show-icon />
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
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
import { Search } from "@element-plus/icons-vue";
import request from "@/api/request";
import defaultAvatar from "@/assets/default-avatar.png";

const users = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const searchKeyword = ref("");
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
    const res = await request.get("/LoginRegister/approval", {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        nickname: searchKeyword.value || "",
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

const openDetail = async (row) => {
  try {
    const res = await request.get("/LoginRegister/selectUserByUsername", {
      params: { username: row.username }
    });
    if (res.code === "200") {
      detailForm.value = { ...res.data };
      detailVisible.value = true;
      activeTab.value = "info";
      passwordForm.value.newPassword = "";
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
    if (activeTab.value === "info") {
       ElMessage.info("用户信息保存功能待后端接口确认");
    }
    
    if (passwordForm.value.newPassword) {
       await request.post("/LoginRegister/UpdatePassword", {
        userId: detailForm.value.userId,
        password: passwordForm.value.newPassword
      });
      ElMessage.success("密码重置成功");
    }
    
    detailVisible.value = false;
    fetchUsers();
  } catch (e) {
    ElMessage.error("保存失败");
  }
};

// 审核操作
const passUser = (row) => {
  ElMessageBox.confirm(`确认要通过用户 "${row.nickname}" 的申请吗？`, "提示", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
    type: "success",
  }).then(async () => {
    // 假设有 pass 接口
    // await request.post("/LoginRegister/pass", null, { params: { userId: row.userId } });
    ElMessage.success("操作成功 (演示)");
    fetchUsers();
  });
};

const refuseUser = (row) => {
  ElMessageBox.confirm(`确认要拒绝用户 "${row.nickname}" 的申请吗？`, "提示", {
    confirmButtonText: "确认拒绝",
    cancelButtonText: "取消",
    type: "warning",
  }).then(async () => {
    // 假设有 refuse 接口
    ElMessage.success("操作成功 (演示)");
    fetchUsers();
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

.form-container {
  display: flex;
  gap: 32px;
  padding: 10px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding-top: 10px;
  min-width: 100px;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--border-light);
}

.detail-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  height: 48px;
}
</style>
