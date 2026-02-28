<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">禁言管理</h2>
          <p class="section-subtitle">集中处理禁言与封禁用户，支持快速解封</p>
        </div>
        <div class="right">
          <el-button type="primary" icon="Refresh" @click="load">刷新列表</el-button>
        </div>
      </div>

      <div class="filter-area">
        <el-select v-model="query.status" placeholder="状态筛选" class="status-select" @change="load" style="width: 160px; margin-right: 12px;">
          <el-option label="已禁言" :value="2" />
          <el-option label="已封禁" :value="3" />
        </el-select>
        <el-input
          v-model="query.username"
          placeholder="搜索用户"
          prefix-icon="Search"
          class="search-input"
          clearable
          style="width: 240px;"
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load" style="margin-left: 12px;">筛选</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="modern-table">
        <!-- 用户信息 -->
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="40" :src="row.avatarUrl || row.avatar || defaultAvatar" class="user-avatar">
                {{ row.username?.charAt(0) }}
              </el-avatar>
              <div class="info-text">
                <div class="name">{{ row.nickname || row.username }}</div>
                <div class="sub-info">@{{ row.username }} (ID: {{ row.userId }})</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="当前状态" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.status === 2" type="warning" effect="light" round>
              <el-icon><Mute /></el-icon> 禁言中
            </el-tag>
            <el-tag v-else-if="row.status === 3" type="danger" effect="light" round>
              <el-icon><CircleClose /></el-icon> 已封禁
            </el-tag>
          </template>
        </el-table-column>

        <!-- 违规原因 -->
        <el-table-column prop="remark" label="备注/原因" show-overflow-tooltip min-width="200">
          <template #default="{ row }">
             <span v-if="row.remark" class="remark-text">{{ row.remark }}</span>
             <span v-else class="empty-text">无备注</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="180" align="right">
          <template #default="scope">
            <el-button 
              type="primary" 
              link 
              icon="Unlock"
              @click="handleUnmute(scope.row)"
            >
              解除限制
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/api/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh, Mute, CircleClose, Unlock } from "@element-plus/icons-vue";

const defaultAvatar = "http://localhost:8080/files/download/img.jpg";

const query = ref({ username: "", status: 2 });
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/LoginRegister/selectAllUser", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value, 
        username: query.value.username, 
        status: query.value.status 
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

const handleUnmute = (row) => {
  const actionText = row.status === 3 ? "解封" : "解除禁言";
  ElMessageBox.confirm(
    `确定要对用户 "${row.username}" 进行${actionText}操作吗？`,
    "操作确认",
    {
      confirmButtonText: "确定解除",
      cancelButtonText: "取消",
      type: "warning",
    }
  ).then(async () => {
    try {
      await request.post("/LoginRegister/unmute", null, { params: { userId: row.userId } });
      ElMessage.success(`${actionText}成功`);
      load();
    } catch (error) {
      ElMessage.error("操作失败");
    }
  });
};

onMounted(() => {
  load();
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
  align-items: center;
  justify-content: space-between;
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

.filter-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  border: 1px solid var(--border-light);
}

.info-text {
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 500;
  color: var(--text-main);
  font-size: 14px;
}

.sub-info {
  font-size: 12px;
  color: var(--text-secondary);
}

.remark-text {
  color: var(--text-regular);
  font-size: 13px;
}

.empty-text {
  color: var(--text-placeholder);
  font-size: 13px;
  font-style: italic;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>