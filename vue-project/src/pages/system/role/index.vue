<template>
  <div class="page-container">
    <div class="header-section">
      <div class="title-area">
        <h2>管理员与角色管理</h2>
        <p class="subtitle">配置系统管理员账号、分配角色权限及账号状态控制</p>
      </div>
      <div class="filter-area">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px; margin-right: 10px" @change="load">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" icon="Plus" @click="openAddDialog">新增管理员</el-button>
        <el-button type="primary" icon="Refresh" @click="load">刷新列表</el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
        <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="管理员信息" min-width="200">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatar || defaultAvatar" class="avatar">
                {{ row.username?.charAt(0) }}
              </el-avatar>
              <div class="info-text">
                <div class="name">{{ row.nickname || row.username }}</div>
                <div class="sub-info">@{{ row.username }} (ID: {{ row.adminId }})</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 当前角色 -->
        <el-table-column label="当前角色" width="180">
          <template #default="{ row }">
            <el-select 
              v-model="row.role" 
              size="small" 
              class="role-select"
              @change="updateRole(row)"
            >
              <template #prefix>
                <el-icon :class="getRoleIconClass(row.role)"><UserFilled /></el-icon>
              </template>
              <el-option label="超级管理员" value="SUPER_ADMIN" />
              <el-option label="普通管理员" value="ADMIN" />
              <el-option label="操作员" value="OPERATOR" />
            </el-select>
          </template>
        </el-table-column>

        
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
              @change="updateStatus(row)"
            />
          </template>
        </el-table-column>

        <!-- 联系方式 -->
        <el-table-column label="联系方式" min-width="180">
          <template #default="{ row }">
            <div class="contact-info">
              <div v-if="row.phone"><el-icon><Iphone /></el-icon> {{ row.phone }}</div>
              <div v-if="row.email"><el-icon><Message /></el-icon> {{ row.email }}</div>
              <div v-if="!row.phone && !row.email" class="empty-text">未绑定</div>
            </div>
          </template>
        </el-table-column>

        <!-- 创建时间 -->
        <el-table-column prop="createTime" label="创建时间" width="180" sortable />

        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    
    <el-dialog v-model="addDialogVisible" title="新增管理员" width="500px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-form-item label="头像" prop="avatarUrl">
          <el-upload
            class="avatar-uploader"
            action="http://localhost:8080/files/upload"
            :data="{ folder: 'avatar' }"
            :show-file-list="false"
            :on-success="handleAvatarSuccess"
            :before-upload="beforeAvatarUpload"
          >
            <el-avatar v-if="addForm.avatarUrl" :size="80" :src="getImageUrl(addForm.avatarUrl)" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            <div class="upload-text">点击上传头像</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="addForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="普通管理员" value="ADMIN" />
            <el-option label="操作员" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAddAdmin" :loading="addLoading">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from "vue";
import request from "@/api/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh, UserFilled, Iphone, Message, Plus } from "@element-plus/icons-vue";

const defaultAvatar = "http://localhost:8080/files/download/img.jpg";

// 头像上传相关函数
const getImageUrl = (url) => {
  if (url && url.startsWith('http')) {
    return url;
  }
  return url ? `http://localhost:8080/files/${url}` : defaultAvatar;
};

const handleAvatarSuccess = (response) => {
  if (response.code === "200") {
    addForm.avatarUrl = response.data;
  } else {
    ElMessage.error("头像上传失败");
  }
};

const beforeAvatarUpload = (file) => {
  const isJPGOrPNG = file.type === 'image/jpeg' || file.type === 'image/png';
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPGOrPNG) {
    ElMessage.error('头像只能是 JPG/PNG 格式!');
  }
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB!');
  }
  return isJPGOrPNG && isLt2M;
};

const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const statusFilter = ref(null);
const addDialogVisible = ref(false);
const addLoading = ref(false);
const addFormRef = ref();

const addForm = reactive({
  username: '',
  password: '',
  nickname: '',
  role: 'OPERATOR',
  phone: '',
  email: '',
  avatarUrl: ''
});

const addRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
});

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/admin/list", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value,
        status: statusFilter.value 
      } 
    });
    if (res.code === "200") {
      list.value = res.data.list || [];
      total.value = res.data.total || 0;
    }
  } finally {
    loading.value = false;
  }
};

const updateRole = async (row) => {
  try {
    await request.post("/admin/updateRole", null, { params: { adminId: row.adminId, role: row.role } });
    ElMessage.success("角色权限已更新");
  } catch (e) {
    ElMessage.error("更新失败");
    load(); // 回滚
  }
};

const updateStatus = async (row) => {
  try {
    await request.post("/admin/updateStatus", null, { params: { adminId: row.adminId, status: row.status } });
    ElMessage.success(row.status === 1 ? "账号已启用" : "账号已禁用");
  } catch (e) {
    ElMessage.error("状态更新失败");
    row.status = row.status === 1 ? 0 : 1; // 回滚
  }
};

const confirmDelete = (row) => {
  ElMessageBox.confirm(`确定删除管理员 "${row.nickname || row.username}" 吗？`, "删除管理员", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(async () => {
    await request.delete("/admin/delete", { params: { adminId: row.adminId } });
    ElMessage.success("管理员已删除");
    load();
  }).catch(() => {});
};

const getRoleIconClass = (role) => {
  return role === 'SUPER_ADMIN' ? 'text-danger' : (role === 'ADMIN' ? 'text-primary' : 'text-info');
};

const openAddDialog = () => {
  addDialogVisible.value = true;
  // 重置表单
  Object.assign(addForm, {
    username: '',
    password: '',
    nickname: '',
    role: 'OPERATOR',
    phone: '',
    email: '',
    avatarUrl: ''
  });
  if (addFormRef.value) {
    addFormRef.value.clearValidate();
  }
};

const submitAddAdmin = async () => {
  if (!addFormRef.value) return;

  const valid = await addFormRef.value.validate();
  if (!valid) return;

  addLoading.value = true;
  try {
    const submitData = {
      ...addForm,
      avatar: addForm.avatarUrl
    };

    const res = await request.post("/admin/add", submitData);
    if (res.code === "200") {
      ElMessage.success("管理员添加成功");
      addDialogVisible.value = false;
      load();
    } else {
      ElMessage.error(res.msg || "添加失败");
    }
  } catch (e) {
    ElMessage.error("网络错误，请重试");
  } finally {
    addLoading.value = false;
  }
};

onMounted(() => {
  load();
});
</script>

<style scoped>
.page-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.title-area h2 {
  font-size: 24px;
  color: #1f2f3d;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.table-card {
  border-radius: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-text {
  display: flex;
  flex-direction: column;
}

.info-text .name {
  font-weight: 500;
  color: #303133;
}

.info-text .sub-info {
  font-size: 12px;
  color: #909399;
}

.role-select {
  width: 140px;
}

.contact-info {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.contact-info .el-icon {
  margin-right: 4px;
  vertical-align: middle;
}

.empty-text {
  color: #c0c4cc;
  font-style: italic;
}

.text-danger { color: #f56c6c; }
.text-primary { color: #409eff; }
.text-info { color: #909399; }

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
