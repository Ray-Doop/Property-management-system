<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">评论管理</h2>
          <p class="section-subtitle">查看用户评论、快速定位与处置</p>
        </div>
        <div class="right">
          <el-button type="primary" icon="Refresh" @click="loadComments">刷新列表</el-button>
        </div>
      </div>

      <el-table :data="data.tableData" v-loading="loading" stripe class="modern-table">
        <el-table-column prop="commentId" label="ID" width="80" align="center" />
        
        <el-table-column label="所属文章" min-width="200">
          <template #default="{ row }">
            <span 
              class="post-link" 
              @click="goToDetail(row.postId)"
              title="点击查看文章详情"
            >
              {{ row.postTitle || '无标题文章' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="评论内容" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="comment-content">{{ formatContent(row.content) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评论用户" width="180">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.avatarUrl || defaultAvatar" class="user-avatar" />
              <div class="user-text">
                <span class="user-id">{{ row.userId || '匿名' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdTime" label="评论时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatTime(row.createdTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="right">
          <template #default="{ row }">
            <el-button 
              type="warning" 
              link 
              size="small" 
              @click="confirmMute(row.userId)"
            >
              禁言用户
            </el-button>
            <el-button 
              type="danger" 
              link 
              size="small" 
              @click="confirmDelete(row.commentId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="data.pageNum"
          v-model:page-size="data.pageSize"
          :total="data.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'

const defaultAvatar = "http://localhost:8080/files/download/img.jpg";

const router = useRouter()
const loading = ref(false)

const data = reactive({
  tableData: [],
  pageNum: 1,
  pageSize: 10,
  total: 0
})


onMounted(() => {
  loadComments()
})

// 获取评论列表（所有评论）
const loadComments = async () => {
  loading.value = true
  try {
    const response = await request.get('/Forum/SelectAllComment', {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize
      }
    })
    if (response.code === "200") {
      data.tableData = response.data.list || []
      data.total = response.data.total
    } else {
      ElMessage.error(response.msg || '数据加载失败')
    }
  } catch (error) {
    ElMessage.error('请求失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 跳转文章详情
const goToDetail = (postId) =>
  router.push(`/forum/posts/${postId}`)

// 删除评论
const deleteComment = async (commentId) => {
  try {
    await request.delete(`/Forum/deleteComment/${commentId}`)
    ElMessage.success("删除成功")
    loadComments()
  } catch (error) {
    ElMessage.error("删除评论失败")
  }
}

// 删除确认弹窗
const confirmDelete = (commentId) => {
  ElMessageBox.confirm(
    '此操作将永久删除该评论，是否继续？',
    '删除确认',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  ).then(() => deleteComment(commentId))
   .catch(() => {})
}

// 禁言用户
const muteUser = async (userId, remark = '违规评论') => {
  try {
    await request.post('/LoginRegister/mute', null, { params: { userId, remark } })
    ElMessage.success('禁言成功')
  } catch (error) {
    ElMessage.error('禁言失败')
  }
}

// 禁言确认弹窗
const confirmMute = (userId) => {
  ElMessageBox.prompt('请输入禁言原因', '禁言确认', {
    confirmButtonText: '确定禁言',
    cancelButtonText: '取消',
    inputValue: '违规评论'
  }).then(({ value }) => {
    muteUser(userId, value)
  }).catch(() => {})
}


const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', { year:'numeric', month:'2-digit', day:'2-digit', hour:'2-digit', minute:'2-digit' }).replace(/\//g,'-')
}


const formatContent = (content) => {
  if (!content) return '内容已被删除'
  return content.replace(/<[^>]+>/g,'')
}

// 分页
const handleSizeChange = (newSize) => { data.pageSize=newSize; data.pageNum=1; loadComments() }
const handlePageChange = (newPage) => { data.pageNum=newPage; loadComments() }

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

.post-link {
  color: var(--el-color-primary);
  cursor: pointer;
  font-weight: 500;
  transition: opacity 0.2s;
}

.post-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.comment-content {
  color: var(--text-main);
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  border: 1px solid var(--border-light);
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-id {
  font-size: 14px;
  color: var(--text-main);
  font-weight: 500;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>