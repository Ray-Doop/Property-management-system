<template>
  <div :class="embedded ? 'embedded-container' : 'page'">
    <el-card class="content-card" :shadow="embedded ? 'never' : 'always'" :style="embedded ? 'border:none' : ''">
      <div class="page-toolbar">
        <el-input v-if="!embedded" v-model="noticeId" placeholder="公告ID" style="width:160px" />
        <el-input v-model="keyword" placeholder="用户昵称/手机/住户ID" style="width:260px" />
        <el-select v-model="readStatus" placeholder="阅读状态" clearable style="width:160px">
          <el-option label="未读" :value="0"/>
          <el-option label="已读" :value="1"/>
        </el-select>
        <el-button type="primary" @click="load">搜索</el-button>
      </div>
      <el-table :data="list" stripe>
        <el-table-column label="头像" width="70">
          <template #default="scope">
            <el-avatar :size="36" :src="getImageUrl(scope.row.avatarUrl || scope.row.avatar)" @error="handleAvatarError">
              <img :src="defaultAvatar" />
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="buildingNo" label="楼栋号" width="100" />
        <el-table-column prop="unitNo" label="单元号" width="100" />
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.readStatus === 1 ? 'success' : 'warning'">{{ scope.row.readStatus === 1 ? '已读' : '未读' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="readTime" label="阅读时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="scope">
            <el-button size="small" type="primary" v-if="scope.row.readStatus === 0" @click="mark(scope.row, 1)">标记已读</el-button>
            <el-button size="small" type="warning" v-else @click="mark(scope.row, 0)">标记未读</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:12px; text-align:right">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import request from "@/api/request.js";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import defaultAvatar from "@/assets/default-avatar.png";

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  },
  initialNoticeId: {
    type: [String, Number],
    default: ""
  },
  initialReadStatus: {
    type: Number,
    default: undefined
  }
});

const route = useRoute();
const router = useRouter();
const noticeId = ref(route.query.noticeId || "");
const keyword = ref("");
const readStatus = ref();

const list = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const getImageUrl = (url) => {
  if (!url) return defaultAvatar;
  try {
    const user = JSON.parse(localStorage.getItem("code_user") || "{}");
    const token = user.token || "";
    if (url.startsWith('http')) {
      return `${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
    }
    return `http://localhost:8080${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
  } catch {
    return defaultAvatar;
  }
};

const handleAvatarError = () => {
  return true;
};

const load = async () => {
  if (!noticeId.value) {
    if (!props.embedded) return ElMessage.warning("请输入公告ID");
    else return; // Embedded but no ID?
  }
  const res = await request.get("/notice/readTable", { params: { noticeId: noticeId.value, readStatus: readStatus.value, keyword: keyword.value, page: pageNum.value, size: pageSize.value } });
  if (res.code === "200") {
    list.value = res.data.list || [];
    total.value = res.data.total || 0;
  }
};

// Removed toDetail since it linked back to ReadStatus which is now the parent

const mark = async (row, status) => {
  await request.post("/notice/markRead", null, { params: { noticeId: row.noticeId, residenceId: row.residenceId, readStatus: status } });
  ElMessage.success(status === 1 ? "已标记为已读" : "已标记为未读");
  load();
};

onMounted(() => {
  if (props.embedded) {
    noticeId.value = props.initialNoticeId;
    if (props.initialReadStatus !== undefined) {
      readStatus.value = props.initialReadStatus;
    }
  }
  load();
});

// Watch for prop changes if dialog is reused
watch(() => props.initialNoticeId, (val) => {
  if (val) {
    noticeId.value = val;
    load();
  }
});

watch(() => props.initialReadStatus, (val) => {
  if (val !== undefined) {
    readStatus.value = val;
    load();
  }
});

</script>
