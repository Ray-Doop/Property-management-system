<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">服务评价</h2>
          <p class="section-subtitle">查看和回复用户对报修服务的评价</p>
        </div>
        <div class="right">
          <div class="search-bar">
            <el-input
              v-model="searchId"
              placeholder="搜索工单ID"
              clearable
              prefix-icon="Search"
              class="search-input"
              @clear="loadData"
              @keyup.enter="loadData"
            />
            <el-select v-model="filterScore" placeholder="评分筛选" clearable class="score-select" @change="loadData">
              <el-option label="5分 (非常满意)" :value="5" />
              <el-option label="4分 (满意)" :value="4" />
              <el-option label="3分 (一般)" :value="3" />
              <el-option label="2分 (不满意)" :value="2" />
              <el-option label="1分 (非常不满意)" :value="1" />
            </el-select>
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe class="modern-table">
        <el-table-column prop="evalId" label="ID" width="80" align="center" />
        
        <el-table-column prop="assignmentId" label="工单ID" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain" round>{{ row.assignmentId }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="180">
          <template #default="{ row }">
            <el-rate
              v-model="row.score"
              disabled
              show-score
              text-color="#f59e0b"
              score-template="{value}分"
            />
          </template>
        </el-table-column>

        <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-text">{{ row.content || '无文字评价' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评价用户" min-width="180">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar 
                :size="36" 
                :src="row.avatarUrl || defaultAvatar" 
                class="user-avatar"
              >
                {{ (row.nickname || row.userId || '?').toString().charAt(0) }}
              </el-avatar>
              <div class="user-text">
                <span class="user-name">
                  {{ row.isAnonymous === '是' ? '匿名用户' : (row.nickname || '用户 ' + row.userId) }}
                </span>
                <span class="user-id" v-if="row.isAnonymous !== '是'">ID: {{ row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="evalTime" label="评价时间" width="170" sortable>
          <template #default="{ row }">
            <span class="time-text">{{ row.evalTime }}</span>
          </template>
        </el-table-column>

        <el-table-column label="管理员回复" min-width="200">
          <template #default="{ row }">
            <div v-if="row.replyContent" class="reply-content">
              <div class="reply-text">{{ row.replyContent }}</div>
              <div class="reply-time">{{ row.replyTime }}</div>
            </div>
            <el-tag v-else type="warning" size="small" effect="light">待回复</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              v-if="!row.replyContent" 
              type="primary" 
              link 
              @click="handleReply(row)"
            >
              回复
            </el-button>
            <el-button 
              v-else 
              type="info" 
              link 
              disabled
            >
              已回复
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
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <!-- 回复弹窗 -->
    <el-dialog 
      v-model="replyVisible" 
      title="回复评价" 
      width="500px" 
      class="modern-dialog"
      destroy-on-close
    >
      <el-form :model="replyForm">
        <el-form-item>
          <el-input
            v-model="replyForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入您的回复内容，友好的回复能提升用户满意度..."
            resize="none"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="replyVisible = false">取消</el-button>
          <el-button type="primary" @click="submitReply" :loading="submitting">
            提交回复
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const defaultAvatar = "http://localhost:8080/files/download/img.jpg"

const loading = ref(false)
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchId = ref('')
const filterScore = ref('')

const replyVisible = ref(false)
const submitting = ref(false)
const replyForm = reactive({
  evalId: null,
  content: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      score: filterScore.value || undefined,
      assignmentId: searchId.value || undefined
    }
    const res = await request.get('/repair/evaluation/list', { params })
    if (res.code === '200') {
      tableData.value = res.data.list
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

const handleReply = (row) => {
  replyForm.evalId = row.evalId
  replyForm.content = ''
  replyVisible.value = true
}

const submitReply = async () => {
  if (!replyForm.content.trim()) {
    return ElMessage.warning('请输入回复内容')
  }
  
  submitting.value = true
  try {
    const res = await request.post('/repair/evaluation/reply', null, {
      params: {
        evalId: replyForm.evalId,
        replyContent: replyForm.content
      }
    })
    
    if (res.code === '200') {
      ElMessage.success('回复成功')
      replyVisible.value = false
      loadData()
    } else {
      ElMessage.error(res.msg || '回复失败')
    }
  } catch (error) {
    ElMessage.error('系统错误')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
})
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

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 200px;
}

.score-select {
  width: 160px;
}

/* User Info Styling */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  border: 1px solid var(--border-light);
  flex-shrink: 0;
}

.user-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  line-height: 1.3;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-main);
}

.user-id {
  font-size: 12px;
  color: var(--text-secondary);
}

.content-text {
  color: var(--text-regular);
  font-size: 14px;
}

.time-text {
  color: var(--text-secondary);
  font-size: 13px;
}

/* Reply Styling */
.reply-content {
  background: var(--bg-color);
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--primary-color);
}

.reply-text {
  font-size: 13px;
  color: var(--text-regular);
  margin-bottom: 4px;
  word-break: break-all;
}

.reply-time {
  font-size: 12px;
  color: var(--text-placeholder);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>
