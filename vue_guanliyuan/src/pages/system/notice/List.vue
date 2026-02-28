<template>
  <div class="notice-page">
    <h2 class="title">公告管理</h2>

    <!-- 搜索与操作栏 -->
    <div class="toolbar">
      <div class="left">
        <el-input
          v-model="searchTitle"
          placeholder="请输入公告标�?
          clearable
          style="width: 220px; margin-right: 10px"
        />
        <el-button type="primary" @click="fetchNoticeList">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <div class="right">
        <el-button type="success" @click="openPublishDialog">发布公告</el-button>
        <el-button type="info" @click="toggleDraftBox">
          {{ showingDrafts ? "返回全部" : "草稿�? }}
        </el-button>
      </div>
    </div>

    <!-- 公告表格 -->
    <el-table
      :data="notices"
      border
      stripe
      v-loading="loading"
      style="width: 100%"
    >
      <el-table-column prop="noticeId" label="公告ID" width="80" align="center" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="publisherName" label="发布�? width="150" align="center" />
      <el-table-column prop="publishTime" label="发布时间" width="180" align="center" />
      <el-table-column prop="remark" label="备注" width="160" align="center" />

      <el-table-column label="状�? width="100" align="center">
        <template #default="{ row }">
          <el-tag 
            :type="row.status === 1 ? 'success' : row.status === 0 ? 'info' : row.status === 2 ? 'warning' : row.status === 4 ? 'warning' : 'danger'">
            {{ row.status === 0 ? "草稿" : row.status === 1 ? "已发�? : row.status === 2 ? "已撤�? : row.status === 3 ? "已删�? : row.status === 4 ? "待发�? : "未知" }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="showDetail(row)" style="margin-right: 5px">详情</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.noticeId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="detailVisible" title="公告详情" width="650px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="标题">{{ currentNotice.title }}</el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentNotice.content }}</el-descriptions-item>
        <el-descriptions-item label="发布�?>{{ currentNotice.publisherName }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ currentNotice.publishTime }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentNotice.remark }}</el-descriptions-item>
      </el-descriptions>

      <!-- 附件预览 -->
      <div v-if="currentNotice.attachments && currentNotice.attachments.length > 0" class="image-preview">
        <h4 style="margin-top: 10px; margin-bottom: 10px">附件�?/h4>
        <div class="image-list">
          <div v-for="(att, i) in currentNotice.attachments" :key="i" class="attachment-item">
            <img
              v-if="isImage(att.fileUrl)"
              :src="att.fileUrl"
              alt="公告附件"
              class="preview-img"
              @click="previewImage(att.fileUrl)"
            />
            <div v-else class="file-item">
              <el-button 
                type="primary" 
                @click="downloadFile(att.fileUrl, att)"
                style="width: 100%"
              >
                <i class="el-icon-download"></i> {{ getFileName(att.fileUrl, att) }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 发布公告弹窗 -->
    <el-dialog v-model="publishVisible" title="发布公告" width="650px" destroy-on-close>
      <el-form :model="publishForm" label-width="90px">
        <el-form-item label="公告标题">
          <el-input v-model="publishForm.title" placeholder="请输入公告标�? />
        </el-form-item>

        <el-form-item label="公告内容">
          <el-input
            v-model="publishForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内�?
          />
        </el-form-item>

        <el-form-item label="定时发布">
          <el-date-picker
            v-model="publishForm.publishTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="不选则立即发布"
            clearable
            style="width: 100%"
          />
        </el-form-item>

        <!-- 上传图片 -->
        <el-form-item label="上传图片">
          <el-upload
            class="upload-demo"
            action="http://localhost:8080/files/upload"
            :data="{ folder: 'notice' }"
            name="file"
            list-type="picture-card"
            :file-list="fileList"
            multiple
            :show-file-list="true"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
          >
            <template #default>
              <i class="el-icon-plus"></i>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="publishForm.remark" placeholder="请输入备�? />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="info" @click="saveAsDraft">存为草稿</el-button>
        <el-button type="success" @click="submitPublish">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/api/request";

const notices = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const searchTitle = ref("");
const showingDrafts = ref(false);

const detailVisible = ref(false);
const publishVisible = ref(false);

const currentNotice = ref({});
const fileList = ref([]);
const publishForm = ref({
  title: "",
  content: "",
  remark: "",
  publishTime: "",
  attachments: [],
  publisherName: "",
  adminId:""
});

// 获取公告列表
const fetchNoticeList = async () => {
  loading.value = true;
  try {
    const res = await request.get("/notice/List", {
      params: {
        page: pageNum.value,
        size: pageSize.value,
        title: searchTitle.value || undefined,
      },
    });

    if (res.code === "200") {
      // 如果显示草稿，需要过�?
      if (showingDrafts.value) {
        notices.value = (res.data.list || []).filter((n) => n.status === 0);
        // 草稿模式下，total需要重新计�?
        total.value = notices.value.length;
      } else {
        notices.value = res.data.list || [];
        total.value = res.data.total || 0;
      }
    } else {
      ElMessage.error(res.msg || "获取公告列表失败");
    }
  } catch (error) {
    console.error("获取公告列表失败:", error);
    ElMessage.error("获取公告列表失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};

const resetSearch = () => {
  searchTitle.value = "";
  pageNum.value = 1; // 重置到第一�?
  fetchNoticeList();
};

const handlePageChange = (page) => {
  pageNum.value = page;
  fetchNoticeList();
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  pageNum.value = 1; // 改变每页条数时重置到第一�?
  fetchNoticeList();
};

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm("确认删除该公告？", "提示", { type: "warning" });
    const res = await request.delete(`/notice/${id}`);
    if (res.code === "200") {
      ElMessage.success("删除成功");
      fetchNoticeList();
    }
  } catch {}
};

const openPublishDialog = () => {
  publishVisible.value = true;
  publishForm.value = {
    title: "",
    content: "",
    remark: "",
    publishTime: "",
    attachments: [],
    publisherName: "",
  };
  fileList.value = [];
};

// 上传成功
const handleUploadSuccess = (response, file, fileList_) => {
  if (response?.data) {
    publishForm.value.attachments.push({
      fileName: file.name,
      fileUrl: response.data,
    });
  }
  fileList.value = fileList_;
};

// 删除附件
const handleRemove = (file, fileList_) => {
  publishForm.value.attachments = publishForm.value.attachments.filter(
    (att) => att.fileUrl !== file.url
  );
  fileList.value = fileList_;
};

const showDetail = async (row) => {
  try {
    // 从后端获取完整的公告详情（包含附件）
    const res = await request.get(`/notice/${row.noticeId}`);
    if (res.code === "200") {
      currentNotice.value = res.data;
      detailVisible.value = true;
    } else {
      ElMessage.error("获取详情失败");
    }
  } catch (err) {
    console.error("获取详情出错:", err);
    // 如果接口失败，使用列表中的数�?
    currentNotice.value = row;
    detailVisible.value = true;
  }
};

// 发布公告
const submitPublish = async () => {
  const user = JSON.parse(localStorage.getItem("code_user"));
  if (!user?.username) {
    ElMessage.error("未登录或未获取到用户�?);
    return;
  }

  publishForm.value.publisherName = user.username;
  publishForm.value.adminId
 = user.adminId
;

  const res = await request.post("/notice/publish", publishForm.value);
  if (res.code === "200") {
    ElMessage.success("发布成功");
    publishVisible.value = false;
    fetchNoticeList();
  } else {
    ElMessage.error(res.msg || "发布失败");
  }
};

// 保存草稿
const saveAsDraft = async () => {
  const user = JSON.parse(localStorage.getItem("code_user"));
  if (!user?.username) {
    ElMessage.error("未登录或未获取到用户�?);
    return;
  }

  publishForm.value.publisherName = user.username;
  publishForm.value.adminId = user.adminId;

  const res = await request.post("/notice/saveDraft", publishForm.value);
  if (res.code === "200") {
    ElMessage.success("草稿保存成功");
    publishVisible.value = false;
    fetchNoticeList();
  } else {
    ElMessage.error(res.msg || "保存草稿失败");
  }
};

// 切换草稿�?
const toggleDraftBox = () => {
  showingDrafts.value = !showingDrafts.value;
  pageNum.value = 1; // 切换时重置到第一�?
  fetchNoticeList();
};

// 判断是否为图�?
const isImage = (url) => {
  if (!url) return false;
  const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp'];
  const lowerUrl = url.toLowerCase();
  return imageExtensions.some(ext => lowerUrl.includes(ext));
};

// 获取文件�?
const getFileName = (url, attachment) => {
  // 优先使用附件对象中的fileName
  if (attachment && attachment.fileName) {
    return attachment.fileName;
  }
  
  if (!url) return '未知文件';
  
  // URL格式: http://localhost:8080/files/download/时间戳_文件�?
  // 提取最后一部分
  const parts = url.split('/');
  const lastPart = parts[parts.length - 1];
  
  if (!lastPart) return '未知文件';
  
  // 如果包含下划线，去掉时间戳前缀（格式：时间戳_文件名）
  const underscoreIndex = lastPart.indexOf('_');
  if (underscoreIndex > 0 && underscoreIndex < lastPart.length - 1) {
    // 检查下划线前是否是纯数字（时间戳）
    const prefix = lastPart.substring(0, underscoreIndex);
    if (/^\d+$/.test(prefix)) {
      // 是时间戳格式，返回下划线后的部分
      return lastPart.substring(underscoreIndex + 1);
    }
  }
  
  // 否则直接返回文件�?
  return lastPart;
};

// 预览图片
const previewImage = (url) => {
  window.open(url, '_blank');
};

// 下载文件
const downloadFile = async (url, attachment) => {
  if (!url) {
    ElMessage.error('文件URL不存�?);
    return;
  }
  
  try {
    // 获取文件�?
    const fileName = getFileName(url, attachment);
    
    // 方法1: 直接打开链接（适用于GET请求的下载接口）
    const link = document.createElement('a');
    link.href = url;
    link.target = '_blank';
    link.download = fileName;
    
    // 添加到DOM并触发点�?
    document.body.appendChild(link);
    link.click();
    
    // 延迟移除，确保下载开�?
    setTimeout(() => {
      document.body.removeChild(link);
    }, 100);
    
    ElMessage.success('开始下�? ' + fileName);
  } catch (error) {
    console.error('下载失败:', error);
    ElMessage.error('下载失败，请检查文件是否存�?);
  }
};

onMounted(fetchNoticeList);
</script>

<style scoped>
.notice-page {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 15px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.preview-img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.preview-img:hover {
  transform: scale(1.05);
}

.attachment-item {
  margin-bottom: 10px;
}

.file-item {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #f5f5f5;
}
.pagination-container {
  text-align: right;
  margin-top: 15px;
}
.upload-demo .el-icon-plus {
  font-size: 28px;
  color: #409eff;
}
</style>

