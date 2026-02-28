<template>
  <div class="page-container">
    <div class="header-section">
      <div class="title-area">
        <h2>登录日志审计</h2>
        <p class="subtitle">追踪系统用户的登录活动与安全事件</p>
      </div>
      <div class="filter-area">
        <el-input
          v-model="query.username"
          placeholder="用户"
          prefix-icon="Search"
          style="width: 200px"
          clearable
          @clear="load"
          @keyup.enter="load"
        />
        <el-select v-model="query.role" placeholder="角色筛选" clearable style="width: 140px" @change="load">
          <el-option label="管理员" value="ADMIN"/>
          <el-option label="普通用户" value="USER"/>
          <el-option label="操作员" value="OPERATOR"/>
        </el-select>
        <el-button type="primary" icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" stripe>
        <!-- 状态列 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" effect="dark" round>
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 用户信息 -->
        <el-table-column label="登录用户" min-width="180">
          <template #default="{ row }">
            <div class="user-info">
              <el-icon class="role-icon" :class="row.role === 'ADMIN' ? 'admin' : 'user'">
                <UserFilled />
              </el-icon>
              <div class="info-text">
                <div class="name">{{ row.username }}</div>
                <div class="role-tag">{{ row.role }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 来源信息 -->
        <el-table-column label="来源 IP" prop="ip" width="160">
          <template #default="{ row }">
            <div class="ip-info">
              <el-icon><Location /></el-icon>
              <span>{{ row.ip }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 时间 -->
        <el-table-column label="登录时间" prop="login_time" width="180" sortable>
          <template #default="{ row }">
            <span class="time-text">{{ row.login_time }}</span>
          </template>
        </el-table-column>

        <!-- 详情消息 -->
        <el-table-column label="系统消息" prop="message" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ 'error-msg': !row.success }">{{ row.message }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/api/request.js";
import { Search, Refresh, UserFilled, Location } from "@element-plus/icons-vue";

const query = ref({ username: "", role: "" });
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/admin/logs/list", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value, 
        username: query.value.username, 
        role: query.value.role 
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

.filter-area {
  display: flex;
  gap: 12px;
}

.table-card {
  border-radius: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-icon {
  font-size: 20px;
  padding: 8px;
  border-radius: 50%;
  background: #f0f2f5;
}

.role-icon.admin { color: #409eff; background: #ecf5ff; }
.role-icon.user { color: #67c23a; background: #f0f9eb; }

.info-text .name {
  font-weight: 500;
  color: #303133;
}

.role-tag {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.ip-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #606266;
}

.time-text {
  font-family: monospace;
  color: #606266;
}

.error-msg {
  color: #f56c6c;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>

