<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">员工列表</h2>
          <p class="section-subtitle">管理物业员工信息与组织分工</p>
        </div>
        <div class="right">
          <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 120px; margin-right: 10px" @change="load">
            <el-option label="正常" :value="1" />
            <el-option label="封禁" :value="0" />
            <el-option label="锁定" :value="2" />
          </el-select>
          <el-input
            v-model="query.nickname"
            placeholder="搜索员工姓名"
            clearable
            prefix-icon="Search"
            class="search-input"
            @clear="load"
            @keyup.enter="load"
          />
          <el-button type="primary" @click="load">
            <el-icon class="el-icon--left"><Search /></el-icon>搜索
          </el-button>
          <el-button type="primary" @click="openAddDialog">
            <el-icon class="el-icon--left"><Plus /></el-icon>新增员工
          </el-button>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" style="width: 100%" class="modern-table">
        <el-table-column label="员工" min-width="220">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar :size="40" :src="getAvatarUrl(row)" class="user-avatar" />
              <div class="user-text">
                <div class="nickname">{{ row.nickname || row.username || "未设置姓名" }}</div>
                <div class="username">ID: {{ row.employeeId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="手机号" width="160">
          <template #default="{ row }">
            <span>{{ row.phone || "-" }}</span>
          </template>
        </el-table-column>

        <el-table-column label="工作内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.specialty || "-" }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" effect="light" round>
              {{ formatStatus(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="最后登录时间" min-width="180">
          <template #default="{ row }">
            <span>{{ row.lastLoginTime || row.last_login_time || "-" }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" align="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button type="warning" link @click="confirmStatus(row, 0, '封禁')">封禁</el-button>
              <el-button type="danger" link @click="confirmStatus(row, 2, '锁定')">锁定</el-button>
            </template>
            <template v-else-if="row.status === 0">
              <el-button type="success" link @click="confirmStatus(row, 1, '解封')">解封</el-button>
            </template>
            <template v-else-if="row.status === 2">
              <el-button type="success" link @click="confirmStatus(row, 1, '解锁')">解锁</el-button>
            </template>
            <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-sizes="[10, 20, 50]"
          v-model:page-size="pageSize"
          v-model:current-page="pageNum"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="addDialogVisible" title="新增员工" width="520px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="addForm.nickname" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="职称" prop="permission">
          <el-select v-model="addForm.permission" placeholder="请选择职称" style="width: 100%" @change="handlePermissionChange">
            <el-option label="维修人员" value="维修人员" />
            <el-option label="保洁人员" value="保洁人员" />
            <el-option label="系统管理员" value="系统管理员" />
            <el-option label="门卫人员" value="门卫人员" />
            <el-option label="保安" value="保安" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作内容" prop="specialty">
          <el-select v-model="addForm.specialty" placeholder="请选择工作内容" style="width: 100%">
            <el-option
              v-for="item in specialtyOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="addForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="0" :value="0" />
            <el-option label="1" :value="1" />
            <el-option label="2" :value="2" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="addLoading" @click="submitAdd">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from "vue";
import request from "@/api/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Plus } from "@element-plus/icons-vue";

const query = ref({ nickname: "", status: null });
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);
const defaultAvatar = "http://localhost:8080/files/download/img.jpg";
const addDialogVisible = ref(false);
const addLoading = ref(false);
const addFormRef = ref();
const repairCategories = ref([]);
const specialtyOptions = ref([]);
const defaultSpecialtyOptions = {
  保洁人员: ["公共区域清洁", "楼道清洁", "垃圾清运", "绿化保洁"],
  系统管理员: ["系统维护", "权限管理", "数据备份"],
  门卫人员: ["门禁管理", "访客登记", "巡逻值守"],
  保安: ["秩序维护", "巡逻执勤", "突发事件处理"]
};
const addForm = ref({
  username: "",
  password: "",
  nickname: "",
  phone: "",
  permission: "",
  specialty: "",
  status: 1
});
const addRules = ref({
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
  nickname: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  permission: [{ required: true, message: "请选择职称", trigger: "change" }],
  specialty: [{ required: true, message: "请选择工作内容", trigger: "change" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }]
});

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/employee/selectPage", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value, 
        nickname: query.value.nickname,
        status: query.value.status 
      } 
    });
    if (res.code === "200") {
      list.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      ElMessage.error(res.msg || "加载失败");
    }
  } catch (e) {
    ElMessage.error("请求失败，请检查网络连接");
  } finally {
    loading.value = false;
  }
};

const getAvatarUrl = (row) => {
  if (!row) return defaultAvatar;
  const url = row.avatarUrl || row.avatar;
  if (!url) return defaultAvatar;
  return url.startsWith("http") ? url : `http://localhost:8080/files/${url}`;
};

const confirmDelete = (row) => {
  if (!row || !row.employeeId) {
    ElMessage.error("员工ID不存在，无法删除");
    return;
  }
  ElMessageBox.confirm(`确定删除员工 "${row.nickname || row.username}" 吗？`, "删除员工", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(async () => {
    try {
      const res = await request.delete("/employee/del/" + row.employeeId);
      if (res.code === "200") {
        ElMessage.success("删除成功");
        if (list.value.length === 1 && pageNum.value > 1) {
          pageNum.value -= 1;
        }
        load();
      } else {
        ElMessage.error(res.msg || "删除失败");
      }
    } catch (e) {
      ElMessage.error("删除失败");
    }
  }).catch(() => {});
};

const formatStatus = (status) => {
  if (status === 0 || status === 1 || status === 2) return String(status);
  return "-";
};

const getStatusTagType = (status) => {
  if (status === 1) return "success";
  if (status === 0) return "info";
  if (status === 2) return "danger";
  return "info";
};

const confirmStatus = (row, status, label) => {
  if (!row || !row.employeeId) {
    ElMessage.error("员工ID不存在，无法操作");
    return;
  }
  ElMessageBox.confirm(`确定对员工 "${row.nickname || row.username}" 执行${label}操作吗？`, "状态变更", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(async () => {
    try {
      const res = await request.put("/employee/updata", { employeeId: row.employeeId, status });
      if (res.code === "200") {
        ElMessage.success(`${label}成功`);
        load();
      } else {
        ElMessage.error(res.msg || `${label}失败`);
      }
    } catch (e) {
      ElMessage.error(`${label}失败`);
    }
  }).catch(() => {});
};

const openAddDialog = () => {
  addDialogVisible.value = true;
  addForm.value = {
    username: "",
    password: "",
    nickname: "",
    phone: "",
    permission: "",
    specialty: "",
    status: 1
  };
  specialtyOptions.value = [];
  if (addFormRef.value) {
    addFormRef.value.clearValidate();
  }
};

const handlePermissionChange = async (value) => {
  if (value === "维修人员") {
    if (repairCategories.value.length === 0) {
      await loadRepairCategories();
    }
    specialtyOptions.value = repairCategories.value;
  } else {
    specialtyOptions.value = defaultSpecialtyOptions[value] || [];
  }
  addForm.value.specialty = "";
};

const loadRepairCategories = async () => {
  try {
    const res = await request.get("/repair/categories");
    if (res.code === "200") {
      repairCategories.value = (res.data || []).map((item) => item.categoryName).filter(Boolean);
    } else {
      ElMessage.error(res.msg || "获取维修类别失败");
    }
  } catch (e) {
    ElMessage.error("获取维修类别失败");
  }
};

const submitAdd = async () => {
  if (!addFormRef.value) return;
  const valid = await addFormRef.value.validate();
  if (!valid) return;
  addLoading.value = true;
  try {
    const res = await request.post("/employee/add", addForm.value);
    if (res.code === "200") {
      ElMessage.success("新增成功");
      addDialogVisible.value = false;
      load();
    } else {
      ElMessage.error(res.msg || "新增失败");
    }
  } catch (e) {
    ElMessage.error("请求失败，请检查网络连接");
  } finally {
    addLoading.value = false;
  }
};

load();
</script>
<style scoped>
.page-container {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.modern-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 22px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  gap: 16px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1f2f3d;
  margin: 0 0 6px 0;
}

.section-subtitle {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 240px;
}

.modern-table :deep(.el-table__header th) {
  background: #f7f8fb;
  color: #606266;
  font-weight: 600;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.user-text .nickname {
  font-weight: 600;
  color: #303133;
}

.user-text .username {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.text-placeholder {
  color: #c0c4cc;
  font-size: 12px;
}

.pagination-container {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
