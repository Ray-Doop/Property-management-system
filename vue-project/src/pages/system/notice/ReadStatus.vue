<template>
  <div class="notice-page">
    <div class="page-header">
      <div class="title-area">
        <h2>阅读情况</h2>
        <p class="subtitle">查看公告阅读人员与未阅读人员情况</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model="searchTitle"
          placeholder="搜索公告标题"
          class="search-input"
          clearable
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">搜索</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">公告数量</div>
        <div class="stat-value">{{ total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">阅读人员数量</div>
        <div class="stat-value">{{ totalRead }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">未阅读人员数量</div>
        <div class="stat-value">{{ totalUnread }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">阅读人员占比</div>
        <div class="stat-value">{{ overallRate }}%</div>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">阅读概览</div>
        <div class="table-subtitle">共 {{ total }} 条公告</div>
      </div>

      <el-table :data="tableData" stripe class="notice-table">
        <el-table-column prop="noticeId" label="公告ID" width="100" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip min-width="200" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="阅读人员/未阅读人员" width="180">
          <template #default="{ row }">
            <div class="count-pair">
              <span class="count read">{{ row.readCount }}</span>
              <span class="divider">/</span>
              <span class="count unread">{{ row.unreadCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="阅读率" width="200">
          <template #default="{ row }">
            <el-progress
              :percentage="calculateRate(row)"
              :status="calculateRate(row) === 100 ? 'success' : ''"
              :format="formatProgress"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openDetails(row, 1)">
              阅读人员详情
            </el-button>
            <el-button size="small" type="warning" plain @click="openDetails(row, 0)">
              未阅读人员详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailsVisible"
      :title="detailsTitle"
      width="80%"
      destroy-on-close
    >
      <ReadTable
        v-if="detailsVisible"
        :embedded="true"
        :initial-notice-id="currentNoticeId"
        :initial-read-status="currentReadStatus"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '@/api/request'
import ReadTable from './ReadTable.vue'

const searchTitle = ref('')
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailsVisible = ref(false)
const detailsTitle = ref('')
const currentNoticeId = ref('')
const currentReadStatus = ref(null)

const loadData = async () => {
  const res = await request.get('/notice/summaryPage', {
    params: {
      title: searchTitle.value,
      page: currentPage.value,
      size: pageSize.value
    }
  })
  if (res.code === '200') {
    tableData.value = res.data.list
    total.value = res.data.total
  }
}

const calculateRate = (row) => {
  if (!row.total || row.total === 0) return 0
  const rate = (row.readCount / row.total) * 100
  return Number(rate.toFixed(1))
}

const formatProgress = (percentage) => {
  return `${percentage}%`
}

const totalRead = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (Number(item.readCount) || 0), 0)
})

const totalUnread = computed(() => {
  return tableData.value.reduce((sum, item) => sum + (Number(item.unreadCount) || 0), 0)
})

const overallRate = computed(() => {
  const totalCount = totalRead.value + totalUnread.value
  if (!totalCount) return 0
  return Number(((totalRead.value / totalCount) * 100).toFixed(1))
})

const openDetails = (row, status) => {
  currentNoticeId.value = row.noticeId
  currentReadStatus.value = status
  detailsTitle.value = `[${row.title}] - ${status === 1 ? '阅读人员' : '未阅读人员'}名单`
  detailsVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.notice-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-area h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
}

.subtitle {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-input {
  width: 260px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  background: var(--surface-color);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
}

.stat-label {
  color: var(--text-secondary);
  font-size: 12px;
}

.stat-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-main);
}

.table-card {
  border-radius: 16px;
  border: 1px solid var(--border-light);
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.table-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.count-pair {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.count {
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 12px;
}

.count.read {
  color: #16a34a;
  background: rgba(22, 163, 74, 0.12);
}

.count.unread {
  color: #f97316;
  background: rgba(249, 115, 22, 0.12);
}

.divider {
  color: var(--text-placeholder);
  font-size: 12px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }

  .search-input {
    flex: 1;
  }

  .stat-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
