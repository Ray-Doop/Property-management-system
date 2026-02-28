<template>
  <div class="page-container">
    <!-- Header -->
    <div class="header-section">
      <div class="title-area">
        <h2>通行日志</h2>
        <p class="subtitle">实时监控社区出入记录与停车费缴纳情况</p>
      </div>
      <div class="filter-area">
        <el-input 
          v-model="query.username" 
          placeholder="搜索用户" 
          prefix-icon="Search"
          style="width: 200px" 
          clearable 
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" class="log-card">
      <el-table :data="list" v-loading="loading" :row-class-name="tableRowClassName">
        
        <!-- 时间线列 -->
        <el-table-column label="时间" width="200">
          <template #default="{ row }">
            <div class="timeline-cell">
              <div v-if="row.entryTime" class="time-point entry">
                <el-tag size="small" type="success" effect="dark">入场</el-tag>
                <span class="time">{{ formatTime(row.entryTime) }}</span>
              </div>
              <div v-if="row.exitTime" class="time-point exit">
                <el-tag size="small" type="warning" effect="dark">离场</el-tag>
                <span class="time">{{ formatTime(row.exitTime) }}</span>
              </div>
              <div v-if="!row.entryTime && !row.exitTime" class="time-point">
                <el-tag size="small" type="info">发放</el-tag>
                <span class="time">{{ formatTime(row.issueTime) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 停留时长 -->
        <el-table-column label="停留时长" width="150">
          <template #default="{ row }">
            <span v-if="row.entryTime && row.exitTime" class="duration">
              {{ calculateDuration(row.entryTime, row.exitTime) }}
            </span>
            <span v-else class="duration-placeholder">--</span>
          </template>
        </el-table-column>

        <!-- 用户/车辆 -->
        <el-table-column label="通行主体" min-width="200">
          <template #default="{ row }">
            <div class="subject-info">
              <el-icon class="icon"><User /></el-icon>
              <span class="name">{{ row.username }}</span>
              <el-tag v-if="row.plateNumber" size="small" type="warning" class="plate-tag">
                {{ row.plateNumber }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- 费用 -->
        <el-table-column label="停车费" width="150" align="right">
          <template #default="{ row }">
            <div v-if="row.fee > 0" class="fee-info">
              <span class="amount">¥ {{ row.fee }}</span>
              <el-tag v-if="row.paid" type="success" size="small" class="paid-tag">已支付</el-tag>
              <el-tag v-else type="danger" size="small" class="paid-tag">待支付</el-tag>
            </div>
            <span v-else class="free">免费 / 无费用</span>
          </template>
        </el-table-column>

        
        <el-table-column label="当前状态" width="120" align="center">
          <template #default="{ row }">
             <span :class="['status-dot', row.status.toLowerCase()]"></span>
             {{ getStatusLabel(row.status) }}
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
import { Search, Refresh, User } from "@element-plus/icons-vue";
import dayjs from "dayjs";

const query = ref({ username: "", status: "" });
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const loading = ref(false);

const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/travel-pass/selectPage", { 
      params: { 
        pageNum: pageNum.value, 
        pageSize: pageSize.value, 
        status: query.value.status, 
        username: query.value.username 
      } 
    });
    if (res && res.list) {
      list.value = res.list;
      total.value = res.total;
    }
  } finally {
    loading.value = false;
  }
};

const formatTime = (time) => {
  if (!time) return '';
  return dayjs(time).format('MM-DD HH:mm:ss');
};

const calculateDuration = (start, end) => {
  const s = dayjs(start);
  const e = dayjs(end);
  const diffMinutes = e.diff(s, 'minute');
  const hours = Math.floor(diffMinutes / 60);
  const mins = diffMinutes % 60;
  if (hours > 0) return `${hours}小时 ${mins}分`;
  return `${mins}分钟`;
};

const getStatusLabel = (status) => {
  const map = { 'ISSUED': '生效中', 'ENTERED': '场内', 'EXITED': '已完成' };
  return map[status] || status;
};

const tableRowClassName = ({ row }) => {
  if (row.status === 'ENTERED') return 'active-row';
  return '';
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

.log-card {
  border-radius: 8px;
}


.timeline-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.time-point {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.time-point.entry .time { font-weight: 500; color: #67c23a; }
.time-point.exit .time { font-weight: 500; color: #e6a23c; }

/* 主体信息样式 */
.subject-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.plate-tag {
  font-family: monospace;
}

/* 费用样式 */
.fee-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.amount {
  font-weight: bold;
  color: #f56c6c;
}
.free {
  color: #909399;
  font-size: 12px;
}

/* 状态点 */
.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
}
.status-dot.issued { background-color: #409eff; }
.status-dot.entered { background-color: #67c23a; }
.status-dot.exited { background-color: #909399; }

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
