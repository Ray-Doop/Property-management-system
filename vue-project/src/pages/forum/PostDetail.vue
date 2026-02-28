<template>
  <div class="page-container" v-loading="loading">
    <div class="header-section">
      <div class="left">
        <el-button @click="goBack" icon="ArrowLeft" circle plain style="margin-right: 12px"></el-button>
        <div>
          <h2 class="section-title">帖子详情</h2>
          <p class="section-subtitle">查看帖子内容、评论及相关操作</p>
        </div>
      </div>
      <div class="right">
        <el-button 
          :type="post.isTop ? 'warning' : 'primary'" 
          plain 
          @click="toggleTop"
        >
          {{ post.isTop ? "取消置顶" : "置顶" }}
        </el-button>
        <el-button 
          :type="post.isEssence ? 'warning' : 'success'" 
          plain 
          @click="toggleEssence"
        >
          {{ post.isEssence ? "取消加精" : "加精" }}
        </el-button>
        <el-button type="warning" plain @click="muteAuthor">禁言作者</el-button>
        <el-button type="danger" plain @click="confirmDelete">删除帖子</el-button>
      </div>
    </div>

    <div class="content-wrapper">
      <!-- 帖子主体 -->
      <div class="modern-card post-card">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta-row">
          <div class="author-cell">
            <el-avatar :size="40" :src="author.avatarUrl || defaultAvatar" />
            <div class="author-details">
              <span class="author-name">{{ author.nickname || "用户 " + post.userId }}</span>
              <span class="publish-time">{{ formatDate(post.createdTime) }}</span>
            </div>
          </div>
          <div class="tags-cell">
            <el-tag v-if="post.isTop" type="danger" effect="dark">置顶</el-tag>
            <el-tag v-if="post.isEssence" type="warning" effect="dark">加精</el-tag>
            <el-tag type="info" effect="plain">{{ post.sectionName || '未知板块' }}</el-tag>
          </div>
        </div>

        <div class="post-body">
          <div class="post-text">{{ post.content }}</div>
          
          <div v-if="post.attachments?.length" class="attachments-grid">
            <div
              v-for="att in post.attachments"
              :key="att.attachmentId"
              class="attachment-item"
            >
              <el-image 
                v-if="!isVideo(att.filePath)" 
                :src="att.filePath" 
                class="attachment-image"
                fit="cover"
                :preview-src-list="[att.filePath]"
                preview-teleported
              />
              <video v-else controls class="attachment-video">
                <source :src="att.filePath" type="video/mp4" />
              </video>
            </div>
          </div>
        </div>
      </div>

      <!-- 评论区 -->
      <div class="modern-card comments-card">
        <div class="card-header">
          <h3>全部评论 ({{ post.comments?.length || 0 }})</h3>
        </div>
        <div v-if="post.comments?.length" class="comments-list">
          <CommentItem
            v-for="c in post.comments"
            :key="c.commentId"
            :comment="c"
            @delete-comment="handleDeleteComment"
            @mute-comment="handleMuteComment"
          />
        </div>
        <el-empty v-else description="暂无评论" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { ArrowLeft } from "@element-plus/icons-vue";
import CommentItem from "./CommentItem.vue";
import request from "@/api/request.js";

const router = useRouter();
const route = useRoute();
const postId = route.params.id;

const post = ref({});
const author = ref({});
const loading = ref(false);
const defaultAvatar = "http://localhost:8080/files/download/img.jpg";

// ------------------- 工具函数 -------------------
const goBack = () => router.back();
const formatDate = (date) =>
  date ? new Date(date).toLocaleString("zh-CN", { hour12: false }) : "";
const isVideo = (url) => url?.toLowerCase().endsWith(".mp4");

// ------------------- 页面数据 -------------------
const fetchPostDetail = async () => {
  try {
    loading.value = true;
    const res = await request.get(`/Forum/PostDetail/${postId}`);
    if (res.code !== "200" || !res.data) throw new Error(res.msg || "获取帖子失败");
    post.value = res.data;

    // 获取作者信息
    const userRes = await request.get("/Forum/selectUserData", {
      params: { userId: post.value.userId }
    });
    if (userRes.code === "200") {
      author.value = userRes.data;
    }
  } catch (err) {
    ElMessage.error(err.message);
  } finally {
    loading.value = false;
  }
};

// ------------------- 操作逻辑 -------------------
const toggleTop = async () => {
  try {
    const api = post.value.isTop ? "/Forum/cancelTop" : "/Forum/top";
    await request.post(api, null, { params: { postId } });
    post.value.isTop = !post.value.isTop;
    ElMessage.success("操作成功");
  } catch (e) {
    ElMessage.error("操作失败");
  }
};

const toggleEssence = async () => {
  try {
    const api = post.value.isEssence ? "/Forum/cancelExquisite" : "/Forum/exquisite";
    await request.post(api, null, { params: { postId } });
    post.value.isEssence = !post.value.isEssence;
    ElMessage.success("操作成功");
  } catch (e) {
    ElMessage.error("操作失败");
  }
};

const muteAuthor = () => {
  ElMessageBox.prompt("请输入禁言原因", "禁言作者", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
  }).then(async ({ value }) => {
    try {
      await request.post("/LoginRegister/mute", null, {
        params: { userId: post.value.userId, remark: value || "违规发帖" }
      });
      ElMessage.success("已禁言作者");
    } catch (e) {
      ElMessage.error("禁言失败");
    }
  }).catch(() => {});
};

const confirmDelete = () => {
  ElMessageBox.confirm("确定删除该帖子吗？此操作不可恢复", "警告", {
    type: "warning",
    confirmButtonText: "确定删除",
    cancelButtonText: "取消"
  }).then(async () => {
    try {
      await request.delete("/Forum/delMyPost", { params: { postId } });
      ElMessage.success("删除成功");
      goBack();
    } catch (e) {
      ElMessage.error("删除失败");
    }
  }).catch(() => {});
};

// ------------------- 评论操作 -------------------
const handleDeleteComment = async (commentId) => {
  try {
    await request.delete(`/Forum/deleteComment/${commentId}`);
    ElMessage.success("评论已删除");
    fetchPostDetail(); // 刷新
  } catch (e) {
    ElMessage.error("删除失败");
  }
};

const handleMuteComment = async ({ userId, remark }) => {
  try {
    await request.post("/LoginRegister/mute", null, { params: { userId, remark } });
    ElMessage.success("用户已禁言");
  } catch (e) {
    ElMessage.error("禁言失败");
  }
};

onMounted(() => {
  fetchPostDetail();
});
</script>

<style scoped>
.page-container {
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-section .left {
  display: flex;
  align-items: center;
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

.modern-card {
  background: var(--surface-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-light);
  padding: 32px;
  margin-bottom: 24px;
}

.post-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0 0 20px 0;
}

.post-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 24px;
}

.author-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-details {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-main);
}

.publish-time {
  font-size: 13px;
  color: var(--text-secondary);
}

.tags-cell {
  display: flex;
  gap: 8px;
}

.post-text {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-main);
  margin-bottom: 24px;
  white-space: pre-wrap;
}

.attachments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.attachment-item {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-light);
  aspect-ratio: 16/9;
}

.attachment-image, .attachment-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.comments-card {
  padding: 24px;
}

.card-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-main);
}
</style>