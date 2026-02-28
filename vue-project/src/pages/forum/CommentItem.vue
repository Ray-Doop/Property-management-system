<template>
  <div class="comment-item">
    <el-avatar
      :size="40"
      :src="comment.avatarUrl || defaultAvatar"
      class="comment-avatar"
      @error="handleImageError"
    />
    <div class="comment-body">
      <div class="comment-header">
        <div class="user-info">
          <span class="nickname">{{ comment.nickname || "用户 " + comment.userId }}</span>
          <span class="time">{{ formatDate(comment.createdTime || comment.createTime) }}</span>
        </div>
        <div class="actions">
          <el-button
            type="danger"
            link
            size="small"
            @click="confirmDelete"
          >
            删除
          </el-button>
          <el-button
            type="warning"
            link
            size="small"
            @click="confirmMute"
          >
            禁言
          </el-button>
        </div>
      </div>
      
      <div class="comment-content">
        {{ comment.status === 0 ? "该评论已删除" : comment.content }}
      </div>

      <div v-if="comment.replies?.length" class="replies-container">
        <div
          v-for="reply in comment.replies"
          :key="reply.commentId"
          class="reply-item"
        >
          <el-avatar
            :size="24"
            :src="reply.avatarUrl || defaultAvatar"
            class="reply-avatar"
            @error="handleImageError"
          />
          <div class="reply-body">
            <div class="reply-header">
              <div class="reply-user-info">
                <el-tag size="small" type="info" effect="plain" class="reply-badge">追评</el-tag>
                <span class="nickname">{{ reply.nickname || "用户 " + reply.userId }}</span>
                <span class="time">{{ formatDate(reply.createdTime || reply.createTime) }}</span>
              </div>
              <div class="reply-actions">
                <el-button
                  type="danger"
                  link
                  size="small"
                  @click="confirmDeleteReply(reply.commentId)"
                >
                  删除
                </el-button>
                <el-button
                  type="warning"
                  link
                  size="small"
                  @click="confirmMuteReply(reply.userId)"
                >
                  禁言
                </el-button>
              </div>
            </div>
            <div class="reply-content">
              {{ reply.status === 0 ? "该评论已删除" : reply.content }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus";

const props = defineProps({
  comment: { type: Object, required: true },
});
const emit = defineEmits(["delete-comment", "mute-comment"]);

const defaultAvatar = "http://localhost:8080/files/download/img.jpg";
const formatDate = (date) =>
  date ? new Date(date).toLocaleString("zh-CN", { hour12: false }) : "";
const handleImageError = (e) => { e.target.src = defaultAvatar; };

// 删除确认
const confirmDelete = () => {
  ElMessageBox.confirm("确定要删除该评论吗？", "删除评论", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => emit("delete-comment", props.comment.commentId))
    .catch(() => {});
};

// 禁言确认
const confirmMute = () => {
  ElMessageBox.prompt("请输入禁言原因", "禁言用户", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
  }).then(({ value }) => emit("mute-comment", { userId: props.comment.userId, remark: value }))
    .catch(() => {});
};

const confirmDeleteReply = (commentId) => {
  ElMessageBox.confirm("确定要删除该追评吗？", "删除追评", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => emit("delete-comment", commentId))
    .catch(() => {});
};

const confirmMuteReply = (userId) => {
  ElMessageBox.prompt("请输入禁言原因", "禁言用户", {
    confirmButtonText: "确认",
    cancelButtonText: "取消",
  }).then(({ value }) => emit("mute-comment", { userId, remark: value }))
    .catch(() => {});
};
</script>

<style scoped>
.comment-item {
  display: flex;
  gap: 16px;
  padding: 20px 0;
  border-bottom: 1px solid var(--border-light);
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nickname {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}

.time {
  font-size: 12px;
  color: var(--text-secondary);
}

.actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.comment-item:hover .actions {
  opacity: 1;
}

.comment-content {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.6;
  margin-bottom: 12px;
  word-wrap: break-word;
}

.replies-container {
  margin-top: 12px;
  padding: 16px;
  background-color: var(--bg-color);
  border-radius: var(--radius-md);
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border-light);
}

.reply-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.reply-item:first-child {
  padding-top: 0;
}

.reply-avatar {
  flex-shrink: 0;
}

.reply-body {
  flex: 1;
  min-width: 0;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 6px;
}

.reply-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.reply-badge {
  margin-right: 4px;
}

.reply-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.reply-item:hover .reply-actions {
  opacity: 1;
}

.reply-content {
  font-size: 14px;
  color: var(--text-regular);
  line-height: 1.5;
}
</style>
