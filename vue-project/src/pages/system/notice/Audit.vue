<template>
  <div class="notice-page">
    <div class="page-header">
      <div class="title-area">
        <h2>公告审核</h2>
        <p class="subtitle">审核员工提交的公告发布申请</p>
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
        <el-button @click="resetFilter">重置</el-button>
      </div>
    </div>

    <el-card class="table-card" shadow="never">
      <div class="table-header">
        <div class="table-title">待审核公告</div>
        <div class="table-subtitle">共 {{ total }} 条待审核公告</div>
      </div>
      <el-table :data="list" stripe class="notice-table">
        <el-table-column prop="noticeId" label="公告ID" width="100" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip min-width="180" />
        <el-table-column prop="publisherName" label="发布人" width="140" />
        <el-table-column prop="publishTime" label="发布时间" width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row)" size="small">{{ getStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="action-row">
              <el-button size="small" type="primary" plain @click="openContent(row)">详情</el-button>
              <el-button size="small" type="success" plain @click="handleAudit(row.noticeId, 1)">通过</el-button>
              <el-button size="small" type="danger" plain @click="handleAudit(row.noticeId, 0)">驳回</el-button>
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
import { ref } from "vue";
import request from "@/api/request.js";
import { ElMessage, ElMessageBox } from "element-plus";

const title = ref("");
const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const contentVisible = ref(false);
const contentTitle = ref("公告详情");
const contentHtml = ref("");
const contentLoading = ref(false);
const contentAttachments = ref([]);

const load = async () => {
  const res = await request.get("/notice/List", {
    params: {
      page: pageNum.value,
      size: pageSize.value,
      title: title.value,
      status: 2,
    },
  });
  if (res.code === "200") {
    list.value = res.data.list || [];
    total.value = res.data.total || 0;
  }
};

const resetFilter = () => {
  title.value = "";
  load();
};

const getStatusLabel = (row) => {
  const map = {
    0: "草稿",
    1: "已发布",
    2: "待审核",
    3: "已删除",
  };
  return map[row.status] || "未知";
};

const getStatusType = (row) => {
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

const handleAudit = async (noticeId, status) => {
  const actionText = status === 1 ? "通过" : "驳回";
  try {
    await ElMessageBox.confirm(`确认${actionText}该公告吗？`, "提示", {
      confirmButtonText: "确认",
      cancelButtonText: "取消",
      type: status === 1 ? "success" : "warning",
    });
    const res = await request.post("/notice/audit", { noticeId, status });
    if (res.code === "200") {
      ElMessage.success(`已${actionText}`);
      load();
    } else {
      ElMessage.error(res.msg || `${actionText}失败`);
    }
  } catch (e) {}
};

load();
</script>

<style scoped>
.notice-page {
  padding: 20px 24px 32px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 18px;
  flex-wrap: wrap;
  gap: 12px;
}

.title-area h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-main);
}

.subtitle {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 260px;
}

.table-card {
  border-radius: 12px;
  border: 1px solid var(--border-light);
  background: var(--surface-color);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
}

.table-title {
  font-size: 16px;
  font-weight: 600;
}

.table-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
}

.notice-table :deep(.el-table__row) {
  height: 56px;
}

.action-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.content-loading,
.content-empty {
  padding: 20px 0;
  color: var(--text-secondary);
  text-align: center;
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

@media (max-width: 960px) {
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  .search-input {
    width: 200px;
  }
}
</style>
