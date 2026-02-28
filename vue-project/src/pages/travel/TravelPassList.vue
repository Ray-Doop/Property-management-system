<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">通行码管理</h2>
          <p class="section-subtitle">管理社区住户的电子通行证与出入记录</p>
        </div>
        <div class="right">
          <div class="search-bar">
            <el-input
              v-model="query.username"
              placeholder="搜索用户或车牌"
              clearable
              prefix-icon="Search"
              style="width: 240px"
              @clear="load"
              @keyup.enter="load"
            />
            <el-select v-model="query.status" placeholder="通行状态" clearable style="width: 140px" @change="load">
              <el-option label="全部状态" value="" />
              <el-option label="待使用" value="ISSUED" />
              <el-option label="已入场" value="ENTERED" />
              <el-option label="已出场" value="EXITED" />
              <el-option label="已过期" value="EXPIRED" />
            </el-select>
            <el-button type="primary" @click="load">搜索</el-button>
          </div>
        </div>
      </div>

      <el-table :data="list" v-loading="loading" class="modern-table">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar :size="40" :src="row.avatar || defaultAvatar" class="user-avatar">{{ (row.nickname || row.username)?.charAt(0) }}</el-avatar>
              <div class="user-text">
                <div class="nickname">{{ row.nickname || '未设置昵称' }}</div>
                <div class="username">ID: {{ row.userId || row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="通行类型" width="140">
          <template #default="{ row }">
            <div class="type-tag" :class="row.hasVehicle ? 'vehicle' : 'person'">
              <el-icon><component :is="row.hasVehicle ? 'Van' : 'User'" /></el-icon>
              <span>{{ row.hasVehicle ? '车辆' : '人员' }}</span>
            </div>
            <div v-if="row.hasVehicle" class="plate-number">{{ row.plateNumber }}</div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" round>
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="时间信息" min-width="240">
          <template #default="{ row }">
            <div class="time-info">
              <div class="time-row">
                <span class="label">生成:</span>
                <span class="value">{{ formatTime(row.issueTime) }}</span>
              </div>
              <div class="time-row">
                <span class="label">过期:</span>
                <span class="value" :class="{ 'text-danger': isExpired(row) }">{{ formatTime(row.expireTime) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
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
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="通行码详情"
      width="600px"
      class="modern-dialog"
      destroy-on-close
    >
      <div class="detail-container" v-if="detailData">
        <!-- 状态流程 -->
        <div class="step-container">
          <el-steps :active="getStepActive(detailData.status)" finish-status="success" align-center>
            <el-step title="已生成" :description="formatTimeShort(detailData.issueTime)" />
            <el-step title="已入场" :description="detailData.enterTime ? formatTimeShort(detailData.enterTime) : '-'" />
            <el-step title="已出场" :description="detailData.exitTime ? formatTimeShort(detailData.exitTime) : '-'" />
          </el-steps>
        </div>

        <div class="info-grid">
          <div class="info-card">
            <div class="card-title">基本信息</div>
            <div class="info-row">
              <span class="label">用户姓名</span>
              <span class="value">{{ detailData.nickname || detailData.username }}</span>
            </div>
            <div class="info-row">
              <span class="label">通行类型</span>
              <span class="value">
                <el-tag size="small" :type="detailData.hasVehicle ? 'warning' : 'info'">
                  {{ detailData.hasVehicle ? '车辆通行' : '人员通行' }}
                </el-tag>
              </span>
            </div>
            <div v-if="detailData.hasVehicle" class="info-row">
              <span class="label">车牌号码</span>
              <span class="value font-mono">{{ detailData.plateNumber }}</span>
            </div>
            <div class="info-row">
              <span class="label">当前状态</span>
              <span class="value">
                <el-tag :type="getStatusType(detailData.status)" size="small">{{ getStatusLabel(detailData.status) }}</el-tag>
              </span>
            </div>
          </div>

          <div class="info-card">
            <div class="card-title">时效控制</div>
            <div class="info-row">
              <span class="label">生效时间</span>
              <span class="value">{{ formatTime(detailData.issueTime) }}</span>
            </div>
            <div class="info-row">
              <span class="label">过期时间</span>
              <span class="value" :class="{ 'text-danger': isExpired(detailData) }">{{ formatTime(detailData.expireTime) }}</span>
            </div>
            <div class="info-row" v-if="detailData.enterTime">
              <span class="label">入场记录</span>
              <span class="value">{{ formatTime(detailData.enterTime) }}</span>
            </div>
            <div class="info-row" v-if="detailData.exitTime">
              <span class="label">出场记录</span>
              <span class="value">{{ formatTime(detailData.exitTime) }}</span>
            </div>
          </div>
        </div>
        
        <div class="remark-card" v-if="detailData.remark">
          <div class="card-title">备注说明</div>
          <div class="remark-content">{{ detailData.remark }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/api/request.js";
import { Search, Van, User } from "@element-plus/icons-vue";

const defaultAvatar = "https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png";

const query = ref({ username: "", status: "" });
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

// 详情弹窗相关
const detailVisible = ref(false);
const detailData = ref(null);

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/travel-pass/selectPage", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value, 
        status: query.value.status || undefined, 
        username: query.value.username || undefined
      } 
    });
    if (res && res.list) {
      list.value = res.list;
      total.value = res.total;
    } else if (res.code === "200" && res.data) {
       // 兼容可能的后端结构变化
       list.value = res.data.list || [];
       total.value = res.data.total || 0;
    }
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const getStatusType = (status) => {
  const map = {
    'ISSUED': 'primary',
    'ENTERED': 'success',
    'EXITED': 'info',
    'EXPIRED': 'danger'
  };
  return map[status] || 'info';
};

const getStatusLabel = (status) => {
  const map = {
    'ISSUED': '待使用',
    'ENTERED': '已入场',
    'EXITED': '已出场',
    'EXPIRED': '已过期'
  };
  return map[status] || status;
};

const getStepActive = (status) => {
  if (status === 'ISSUED') return 1;
  if (status === 'ENTERED') return 2;
  if (status === 'EXITED' || status === 'EXPIRED') return 3;
  return 0;
};

const isExpired = (row) => {
  if (!row || row.status === 'EXITED' || row.status === 'EXPIRED') return false;
  if (!row.expireTime) return false;
  return new Date(row.expireTime) < new Date();
};

const formatTimeShort = (time) => {
  if (!time) return '';
  const date = new Date(time);
  return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const formatTime = (time) => {
  if (!time) return '-';
  const date = new Date(time);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

const viewDetail = (row) => {
  detailData.value = row;
  detailVisible.value = true;
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
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  padding: 24px;
  min-height: calc(100vh - 120px);
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
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.section-subtitle {
  color: #8c8c8c;
  font-size: 13px;
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 12px;
}

/* 表格样式 */
.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

.user-info-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.nickname {
  font-weight: 500;
  color: #333;
}

.username {
  font-size: 12px;
  color: #999;
}

.type-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.type-tag.vehicle {
  color: #e6a23c;
}

.type-tag.person {
  color: #409eff;
}

.plate-number {
  font-family: monospace;
  font-size: 12px;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  margin-top: 4px;
  display: inline-block;
  color: #333;
}

.time-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
}

.time-row {
  display: flex;
  gap: 8px;
}

.label {
  color: #999;
}

.value {
  color: #333;
  font-family: monospace;
}

.text-danger {
  color: #f56c6c;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 弹窗样式 */
.step-container {
  padding: 20px 0;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.info-card, .remark-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  color: #333;
  border-left: 3px solid #409eff;
  padding-left: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.remark-card {
  margin-top: 20px;
}

.remark-content {
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}

.font-mono {
  font-family: monospace;
}
</style>

