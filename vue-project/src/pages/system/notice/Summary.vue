<template>
  <div class="notice-page">
    <div class="page-header">
      <div class="title-area">
        <h2>公告列表</h2>
        <p class="subtitle">集中管理公告阅读情况与发布记录</p>
      </div>
      <div class="header-actions">
        <el-input
          v-model="title"
          placeholder="搜索公告标题"
          class="search-input"
          clearable
          @clear="load"
          @keyup.enter="load"
        />
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button type="success" @click="toCreate">发布公告</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">总公告数量</div>
        <div class="stat-value">{{ stats.total || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日发布公告数量</div>
        <div class="stat-value">{{ stats.todayPublished || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待审核公告数量</div>
        <div class="stat-value">{{ stats.pendingAudit || 0 }}</div>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">公告概览</div>
        <div class="table-subtitle">共 {{ total }} 条公告</div>
      </div>
      <el-table :data="list" stripe class="notice-table">
        <el-table-column prop="noticeId" label="公告ID" width="100" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip min-width="160" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="发布状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">{{ getStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" plain @click="openContent(row)">公告详情</el-button>
              <el-button size="small" type="success" plain @click="openDetails(row, 1)">已读详情</el-button>
              <el-button size="small" type="warning" plain @click="openDetails(row, 0)">未读详情</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row.noticeId)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
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

    <el-dialog
      v-model="contentVisible"
      :title="contentTitle"
      width="720px"
      destroy-on-close
    >
      <div v-if="contentLoading" class="content-loading">加载中...</div>
      <div v-else-if="!contentHtml" class="content-empty">暂无内容</div>
      <div v-else class="notice-content" v-html="contentHtml"></div>
      <div v-if="contentAttachments.length" class="attachment-section">
        <div class="attachment-title">附件</div>
        <div class="attachment-list">
          <div v-for="item in contentAttachments" :key="item.fileUrl" class="attachment-item">
            <el-image
              v-if="isImageType(item.fileType)"
              :src="item.fileUrl"
              :preview-src-list="[item.fileUrl]"
              fit="cover"
              class="attachment-thumb"
            />
            <div class="attachment-info">
              <div class="attachment-name">{{ item.name }}</div>
              <el-link :href="item.fileUrl" target="_blank" type="primary">打开</el-link>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
  
<script setup>
import { ref, computed } from "vue";
import request from "@/api/request.js";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import ReadTable from "./ReadTable.vue";

const router = useRouter();

const title = ref("");
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const stats = ref({
  total: 0,
  todayPublished: 0,
  pendingAudit: 0
});

const detailsVisible = ref(false);
const detailsTitle = ref("");
const currentNoticeId = ref("");
const currentReadStatus = ref(null);

const contentVisible = ref(false);
const contentTitle = ref("公告详情");
const contentHtml = ref("");
const contentLoading = ref(false);
const contentAttachments = ref([]);

const fetchStats = async () => {
  const res = await request.get("/notice/stats");
  if (res.code === "200") {
    stats.value = res.data;
  }
};

const load = async () => {
  fetchStats();
  const res = await request.get("/notice/summaryPage", { params: { page: pageNum.value, size: pageSize.value, title: title.value } });
  if (res.code === "200") {
    list.value = res.data.list || [];
    total.value = res.data.total || 0;
  }
};

const calculateRate = (row) => {
  if (!row.total || row.total === 0) return 0;
  const rate = (row.readCount / row.total) * 100;
  return Number(rate.toFixed(1));
};

const formatProgress = (percentage) => {
  return `${percentage}%`;
};

const isScheduled = (row) => {
  if (row.status !== 0 || !row.publishTime) return false;
  const time = new Date(String(row.publishTime).replace(/-/g, "/")).getTime();
  return Number.isFinite(time) && time > Date.now();
};

const getStatusLabel = (row) => {
  if (isScheduled(row)) return "定时发布";
  const map = {
    0: "草稿",
    1: "已发布",
    2: "待审核",
    3: "已删除",
  };
  return map[row.status] || "未知";
};

const getStatusType = (row) => {
  if (isScheduled(row)) return "warning";
  const map = {
    0: "info",
    1: "success",
    2: "warning",
    3: "danger",
  };
  return map[row.status] || "info";
};

const resolveContent = (data) => {
  if (!data) return "";
  if (typeof data === "string") return data;
  const candidates = [
    data.content,
    data.notice?.content,
    data.noticeContent,
    data.detail?.content,
    data.html,
    data.remark,
  ];
  return candidates.find((item) => typeof item === "string" && item.trim());
};

const resolveAttachments = (data) => {
  const attachments = data?.attachments || data?.notice?.attachments || [];
  if (!Array.isArray(attachments)) return [];
  return attachments
    .filter((item) => item?.fileUrl)
    .map((item) => ({
      fileUrl: item.fileUrl,
      fileType: item.fileType || "file",
      name: item.fileUrl.split("/").pop() || "附件",
    }));
};

const isImageType = (type) => {
  if (!type) return false;
  const normalized = String(type).toLowerCase();
  return ["image", "png", "jpg", "jpeg", "gif", "webp", "bmp"].includes(normalized);
};

const openDetails = (row, status) => {
  currentNoticeId.value = row.noticeId;
  currentReadStatus.value = status;
  detailsTitle.value = `[${row.title}] - ${status === 1 ? "已读" : "未读"}详情`;
  detailsVisible.value = true;
};

const openContent = async (row) => {
  contentTitle.value = `[${row.title}] - 公告详情`;
  contentHtml.value = "";
  contentAttachments.value = [];
  contentLoading.value = true;
  contentVisible.value = true;
  try {
    const res = await request.get(`/notice/${row.noticeId}`);
    if (res.code === "200") {
      contentHtml.value = resolveContent(res.data) || "";
      contentAttachments.value = resolveAttachments(res.data);
    } else {
      ElMessage.error(res.msg || "获取公告详情失败");
    }
  } catch (e) {
    ElMessage.error("获取公告详情失败");
  } finally {
    contentLoading.value = false;
  }
};

const toCreate = () => {
  router.push({ path: "/system/notice/create" });
};

const handleDelete = async (noticeId) => {
  try {
    await ElMessageBox.confirm("确认删除该公告？", "提示", { type: "warning" });
    const res = await request.delete(`/notice/${noticeId}`);
    if (res.code === "200") {
      ElMessage.success("删除成功");
      load();
    }
  } catch {}
};

load();
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
  
  .notice-table :deep(.el-button) {
    margin-right: 0;
  }

  .notice-table :deep(.el-table__cell) {
    padding: 10px 8px;
  }

  .action-row {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: nowrap;
    white-space: nowrap;
  }

  .table-card :deep(.el-card__body) {
    padding: 16px;
  }

  .content-loading,
  .content-empty {
    color: var(--text-secondary);
    font-size: 13px;
    padding: 16px 4px;
  }

  .notice-content {
    color: var(--text-main);
    line-height: 1.7;
    font-size: 14px;
  }

  .attachment-section {
    margin-top: 16px;
    border-top: 1px solid var(--border-light);
    padding-top: 16px;
  }

  .attachment-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-main);
    margin-bottom: 10px;
  }

  .attachment-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .attachment-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border: 1px solid var(--border-light);
    border-radius: 10px;
    background: var(--bg-color);
  }

  .attachment-thumb {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    flex-shrink: 0;
  }

  .attachment-info {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex: 1;
    gap: 12px;
  }

  .attachment-name {
    font-size: 13px;
    color: var(--text-regular);
    word-break: break-all;
  }

  .notice-table :deep(th.el-table__cell) {
    background: var(--bg-color);
    color: var(--text-secondary);
    font-weight: 600;
  }
  
  .notice-table :deep(.el-table__row) {
    transition: background 0.2s ease;
  }
  
  .notice-table :deep(.el-table__row:hover) {
    background: rgba(59, 130, 246, 0.05);
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
