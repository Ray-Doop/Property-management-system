<template>
  <div class="page-container">
    <div class="modern-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">帖子管理</h2>
          <p class="section-subtitle">集中管理社区帖子、置顶与加精</p>
        </div>
        <div class="right">
          <el-button type="primary" icon="Refresh" @click="load">刷新列表</el-button>
        </div>
      </div>

      <div class="filter-area">
        <div class="filter-left">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索帖子标题"
            prefix-icon="Search"
            class="search-input"
            clearable
            @clear="load"
            @keyup.enter="searchByTitle"
          />
          <el-select
            v-model="queryParams.plateId"
            placeholder="全部板块"
            class="plate-select"
            clearable
            @change="switchSection"
          >
            <el-option
              v-for="item in plateOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-button type="primary" @click="searchByTitle">搜索</el-button>
        </div>
        <div class="filter-right">
          <el-radio-group v-model="queryParams.sort" @change="setSort" size="default">
            <el-radio-button label="newest">最新</el-radio-button>
            <el-radio-button label="hot">最热</el-radio-button>
            <el-radio-button label="collect">收藏</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-table :data="sortedPosts" v-loading="loading" stripe class="modern-table">
        <el-table-column label="帖子信息" min-width="300">
          <template #default="{ row }">
            <div class="post-info-cell">
              <span class="post-title" @click="goToDetail(row.postId)">{{ row.title }}</span>
              <div class="post-tags">
                <el-tag v-if="row.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
                <el-tag v-if="row.isEssence" type="warning" size="small" effect="dark">加精</el-tag>
                <el-tag effect="plain" type="info" size="small">{{ getSectionName(row.sectionId) }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="作者" width="180">
          <template #default="{ row }">
            <div class="author-info">
              <el-avatar :size="32" :src="row.avatarUrl || defaultAvatar" class="author-avatar" />
              <div class="author-text">
                <div class="author-name">{{ row.nickname || row.userId }}</div>
                <div class="author-id">ID: {{ row.userId }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="发布时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right" align="right">
          <template #default="{ row }">
            <el-button 
              :type="row.isTop ? 'warning' : 'primary'" 
              link 
              @click="handleTop(row)"
            >
              {{ row.isTop ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button 
              :type="row.isEssence ? 'warning' : 'success'" 
              link 
              @click="handleExquisite(row)"
            >
              {{ row.isEssence ? '取消加精' : '加精' }}
            </el-button>
            <el-button 
              type="danger" 
              link 
              @click="handleDelete(row.postId)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="data.pageNum"
          v-model:page-size="data.pageSize"
          :total="data.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Search, Refresh } from "@element-plus/icons-vue";
import request from "@/api/request";

const defaultAvatar = "http://localhost:8080/files/download/img.jpg";

const router = useRouter();
const loading = ref(false);

// 查询条件
const queryParams = reactive({ keyword: "", plateId: null, sort: "newest" });

// 帖子数据
const data = reactive({ tableData: [], pageNum: 1, pageSize: 10, total: 0 });

// 板块选项
const plateOptions = ref([
  { id: 1, name: "邻里交流" },
  { id: 2, name: "二手交易" },
  { id: 3, name: "社区活动" },
  { id: 4, name: "宠物交流" },
  { id: 5, name: "家政服务" },
]);

// 工具函数
const getSectionName = (id) => {
  const plateMap = {
    1: "邻里交流",
    2: "二手交易",
    3: "社区活动",
    4: "宠物交流",
    5: "家政服务",
  };
  return plateMap[id] || "未知";
};
const formatTime = (time) => (time ? new Date(time).toLocaleString() : "");

const sortedPosts = computed(() => {
  return [...data.tableData].sort((a, b) => {
    if (a.isTop && !b.isTop) return -1;
    if (!a.isTop && b.isTop) return 1;
    return 0;
  });
});

const setSort = (type) => {
  // type might be event if from radio-group change, but v-model handles it
  // Actually el-radio-group emits label value
  if (queryParams.sort === 'hot') showHottest();
  else if (queryParams.sort === 'collect') showHotByCollect();
  else load();
};

// 跳转帖子详情
const goToDetail = (postId) => {
  router.push(`/forum/posts/${postId}`);
};

// 删除帖子
const handleDelete = async (postId) => {
  try {
    await ElMessageBox.confirm("确认删除该帖子吗？此操作不可恢复", "警告", { 
      confirmButtonText: "确定",
      cancelButtonText: "取消",
      type: "warning" 
    });
    const res = await request.delete("/Forum/delMyPost", { params: { postId } });
    if (res.code === "200") {
      ElMessage.success("删除成功");
      load();
    } else {
      ElMessage.error(res.msg || "删除失败");
    }
  } catch (e) {}
};

// 置顶 / 取消置顶
const handleTop = async (post) => {
  try {
    const api = post.isTop ? "/Forum/cancelTop" : "/Forum/top";
    const res = await request.post(api, null, { params: { postId: post.postId } });
    if (res.code === "200") {
      ElMessage.success(post.isTop ? "已取消置顶" : "置顶成功");
      load();
    } else {
      ElMessage.error(res.msg || "操作失败");
    }
  } catch (e) {
    ElMessage.error("请求失败");
  }
};

// 加精 / 取消加精
const handleExquisite = async (post) => {
  try {
    const api = post.isEssence ? "/Forum/cancelExquisite" : "/Forum/exquisite";
    const res = await request.post(api, null, { params: { postId: post.postId } });
    if (res.code === "200") {
      ElMessage.success(post.isEssence ? "已取消加精" : "加精成功");
      load();
    } else {
      ElMessage.error(res.msg || "操作失败");
    }
  } catch (e) {
    ElMessage.error("请求失败");
  }
};

// 加载帖子列表
const load = async () => {
  loading.value = true;
  try {
    const res = await request.get("/Forum/SelectPage", {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        title: queryParams.keyword,
        sort: queryParams.sort,
        t: Date.now(),
      },
    });
    data.tableData = res.data?.list || [];
    data.total = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("加载数据失败");
  } finally {
    loading.value = false;
  }
};

// 搜索标题
const searchByTitle = async () => {
  data.pageNum = 1;
  load(); 
};

// 切换板块
const switchSection = async () => {
  data.pageNum = 1;
  if (!queryParams.plateId) {
    load();
    return;
  }
  loading.value = true;
  try {
    const res = await request.get("/Forum/SwitchSection", {
      params: {
        sectionId: queryParams.plateId,
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        t: Date.now(),
      },
    });
    data.tableData = res.data?.list || [];
    data.total = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("切换板块失败");
  } finally {
    loading.value = false;
  }
};

const showHottest = async () => {
  loading.value = true;
  try {
    const res = await request.get("/Forum/SelectPageHot", {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        title: queryParams.keyword,
        sort: queryParams.sort,
        t: Date.now(),
      },
    });
    data.tableData = res.data?.list || [];
    data.total = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("加载数据失败");
  } finally {
    loading.value = false;
  }
};

const showHotByCollect = async () => {
  loading.value = true;
  try {
    const res = await request.get("/Forum/selectPageHotByCollect", {
      params: {
        pageNum: data.pageNum,
        pageSize: data.pageSize,
        title: queryParams.keyword,
        sort: queryParams.sort,
        t: Date.now(),
      },
    });
    data.tableData = res.data?.list || [];
    data.total = res.data?.total || 0;
  } catch (e) {
    ElMessage.error("加载数据失败");
  } finally {
    loading.value = false;
  }
};

// 分页
const handleSizeChange = (size) => {
  data.pageSize = size;
  data.pageNum = 1;
  load();
};
const handleCurrentChange = (page) => {
  data.pageNum = page;
  load();
};

onMounted(() => {
  load();
});
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

.filter-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 16px;
}

.filter-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 240px;
}

.plate-select {
  width: 160px;
}

.post-info-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.post-title {
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  font-size: 15px;
  transition: color 0.2s;
}

.post-title:hover {
  color: var(--el-color-primary);
}

.post-tags {
  display: flex;
  gap: 6px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-avatar {
  border: 1px solid var(--border-light);
}

.author-text {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 14px;
  color: var(--text-main);
  font-weight: 500;
}

.author-id {
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}
</style>