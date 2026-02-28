<template>
  <div class="page-shell">
    <div class="modern-card repair-card">
      <div class="section-header">
        <div class="left">
          <h2 class="section-title">报修工单</h2>
          <p class="section-subtitle">集中管理与派单处理</p>
        </div>
        <div class="right">
          <div class="total-chip">共 {{ total }} 条</div>
        </div>
      </div>

      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="选择状态" clearable @change="loadOrders">
          <el-option label="未分配" value="未分配" />
          <el-option label="已分配" value="已分配" />
          <el-option label="维修中" value="维修中" />
          <el-option label="已完成" value="已完成" />
        </el-select>

        <el-select v-model="priorityFilter" placeholder="选择优先级" clearable @change="loadOrders">
          <el-option label="紧急" value="紧急" />
          <el-option label="普通" value="普通" />
        </el-select>

        <el-select v-model="sortOrder" placeholder="时间排序" clearable @change="loadOrders">
          <el-option label="时间从新到旧" value="desc" />
          <el-option label="时间从旧到新" value="asc" />
        </el-select>

        <el-button type="primary" @click="loadOrders">搜索</el-button>
        <el-button @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="orders" class="modern-table">
      <el-table-column prop="orderId" label="工单ID" width="80" />
      <el-table-column prop="userId" label="报修人ID" width="100" />
      <el-table-column prop="categoryName" label="维修类别" width="120" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />

      <!-- 状态列 -->
      <el-table-column prop="status" label="状态" width="120">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" effect="light" class="status-tag">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 优先级列 -->
      <el-table-column prop="priority" label="优先级" width="100">
        <template #default="scope">
          <el-tag :type="priorityTagType(scope.row.priority)" effect="light" class="priority-tag">
            {{ priorityLabel(scope.row.priority) }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 预约上门时间 -->
      <el-table-column prop="appointmentTime" label="预约上门时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.appointmentTime) }}</template>
      </el-table-column>

      <el-table-column prop="createdTime" label="报修时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.createdTime) }}</template>
      </el-table-column>

      <el-table-column prop="updatedTime" label="更新时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.updatedTime) }}</template>
      </el-table-column>

      <el-table-column label="操作" fixed="right" width="240">
        <template #default="scope">
          <div class="action-group">
            <el-button size="small" type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button
              size="small"
              type="success"
              :disabled="!canAssign(scope.row.status)"
              @click="openAssign(scope.row)"
            >
              {{ assignActionLabel(scope.row.status) }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="!canCancel(scope.row.status)"
              @click="cancelOrder(scope.row.orderId)"
            >
              取消工单
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="loadOrders"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="800px" class="modern-dialog detail-dialog" destroy-on-close>
      <!-- 状态步骤条 -->
      <div class="step-container">
        <el-steps :active="getStepActive(detail.status)" finish-status="success" align-center>
          <el-step title="已报修" :description="formatTime(detail.createdTime)" />
          <el-step title="已指派" :description="detail.assignedTime ? formatTime(detail.assignedTime) : (detail.status === '待处理' ? '等待指派' : '-')" />
          <el-step title="维修中" description="上门维修" />
          <el-step title="已完成" :description="detail.finishedTime ? formatTime(detail.finishedTime) : '-'" />
        </el-steps>
      </div>

      <el-tabs type="border-card" class="detail-tabs">
        <!-- 基本信息 Tab -->
        <el-tab-pane label="工单信息">
          <div class="info-section">
            <el-descriptions title="基本信息" :column="2" border>
              <el-descriptions-item label="工单ID">{{ detail.orderId }}</el-descriptions-item>
              <el-descriptions-item label="维修类别">
                <el-tag effect="plain">{{ detail.categoryName }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="statusTagType(detail.status)" effect="dark">{{ detail.status }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="优先级">
                <el-tag :type="priorityTagType(detail.priority)">{{ priorityLabel(detail.priority) }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="报修时间">{{ formatTime(detail.createdTime) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatTime(detail.updatedTime) }}</el-descriptions-item>
              <el-descriptions-item label="预约上门">{{ formatTime(detail.appointmentTime) }}</el-descriptions-item>
            </el-descriptions>

            <el-divider />

            <el-descriptions title="人员信息" :column="2" border>
              <el-descriptions-item label="报修人">{{ detail.name || detail.userId }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
              <el-descriptions-item label="地址">{{ `${detail.buildingNo || '-'}栋${detail.unitNo || '-'}单元${detail.roomNo || '-'}室` }}</el-descriptions-item>
              <el-descriptions-item label="接单人">
                <div v-if="detail.assignedWorker" class="worker-tag">
                  <el-avatar :size="24" :src="detail.workerAvatar">{{ (detail.workerName || '工').charAt(0) }}</el-avatar>
                  <span class="worker-name">{{ detail.workerName || '维修员' }}</span>
                </div>
                <span v-else class="text-gray">未指派</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>

        <!-- 详细描述与附件 Tab -->
        <el-tab-pane label="描述与附件">
          <div class="desc-section">
            <div class="section-title">故障描述</div>
            <div class="desc-content">{{ detail.description }}</div>
            
            <div class="section-title" style="margin-top: 20px;">附件列表</div>
            <div class="attachment-list" v-if="detail.files && detail.files.length">
              <div v-for="(file, index) in detail.files" :key="index" class="attachment-item">
                <el-image 
                  v-if="isImage(file.fileUrl)"
                  :src="file.fileUrl" 
                  :preview-src-list="detail.files.map(f => f.fileUrl)"
                  fit="cover"
                  class="attachment-img"
                />
                <video v-else :src="file.fileUrl" controls class="attachment-video"></video>
              </div>
            </div>
            <el-empty v-else description="暂无附件" :image-size="60" />
          </div>
        </el-tab-pane>

        <!-- 评价信息 Tab -->
        <el-tab-pane label="服务评价" v-if="detail.status === '已完成' || detail.status === '已取消'">
          <div class="eval-section" v-if="detail.rating">
            <div class="eval-card">
              <div class="eval-header">
                <span class="user-name">{{ detail.name || '用户' }}</span>
                <el-rate v-model="detail.rating" disabled show-score text-color="#ff9900" />
                <span class="eval-time">{{ formatTime(detail.evalTime) }}</span>
              </div>
              <div class="eval-content">{{ detail.evaluation || '用户未填写文字评价' }}</div>
            </div>

            <div class="reply-card" v-if="detail.replyContent">
              <div class="reply-header">
                <el-tag size="small" type="success">官方回复</el-tag>
                <span class="reply-time">{{ formatTime(detail.replyTime) }}</span>
              </div>
              <div class="reply-content">{{ detail.replyContent }}</div>
            </div>
            <el-empty v-else-if="!detail.replyContent" description="暂无回复" :image-size="60" />
          </div>
          <el-empty v-else description="暂无评价" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button
            type="danger"
            v-if="canCancel(detail.status)"
            @click="cancelOrder(detail.orderId)"
          >
            取消工单
          </el-button>
          <el-button
            type="primary"
            v-if="canAssign(detail.status)"
            @click="openAssign(detail)"
          >
            {{ assignActionLabel(detail.status) }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分配人员弹窗 -->
    <el-dialog v-model="assignVisible" :title="assignDialogTitle" width="450px" class="modern-dialog">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="选择维修人员">
          <el-select v-model="assignForm.workerId" placeholder="请选择维修人员" style="width: 300px">
            <el-option
              v-for="worker in availableWorkers"
              :key="worker.employeeId"
              :label="`${worker.nickname || worker.username} (${worker.specialty})`"
              :value="String(worker.employeeId)"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确认分配</el-button>
      </template>
    </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/api/request";

const orders = ref([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const statusFilter = ref("");
const priorityFilter = ref("");
const sortOrder = ref("");

const categories = ref([]);
const detailVisible = ref(false);
const detail = ref({});

const assignVisible = ref(false);
const assignDialogTitle = ref("分配维修人员");
const assignForm = ref({ workerId: "", orderId: null, visitingTime: null });
const availableWorkers = ref([]);

// 状态优先级UI映射
const statusTagType = (status) => {
  switch (status) {
    case "待处理": return "info";
    case "已指派": return "primary";
    case "维修中": return "warning";
    case "已完成": return "success";
    case "已取消": return "danger";
    default: return "info";
  }
};

const priorityLabel = (priority) => {
  if (priority === "紧急" || priority === "urgent") return "紧急";
  if (priority === "普通" || priority === "normal") return "普通";
  return priority || "-";
};

const toBackendPriority = (priority) => {
  if (priority === "紧急" || priority === "urgent") return "紧急";
  if (priority === "普通" || priority === "normal") return "普通";
  return priority;
};

const toBackendStatus = (status) => {
  if (status === "未分配") return "待处理";
  if (status === "已分配") return "已指派";
  return status;
};

const priorityTagType = (priority) => {
  const label = priorityLabel(priority);
  if (label === "紧急") return "danger";
  if (label === "普通") return "info";
  return "info";
};

// 判断操作权限
const canAssign = (status) => ["待处理", "已指派", "维修中"].includes(status);
const canCancel = (status) => ["待处理", "已指派", "维修中"].includes(status);
const assignActionLabel = (status) => (status === "待处理" ? "分配人员" : "更换人员");

// 步骤条映射
const getStepActive = (status) => {
  if (status === "待处理" || status === "未分配") return 1;
  if (status === "已指派" || status === "已分配") return 2;
  if (status === "维修中") return 3;
  if (status === "已完成") return 4;
  return 0;
};

// 判断是否为图片
const isImage = (url) => {
  if (!url) return false;
  const ext = url.split('.').pop().toLowerCase();
  return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext);
};

// 加载类别
const loadCategories = async () => {
  const res = await request.get("/repair/categories");
  if (res.code === "200") categories.value = res.data;
};

// 加载工单
const loadOrders = async () => {
  const params = {
    page: page.value,
    size: size.value,
    status: statusFilter.value ? toBackendStatus(statusFilter.value) : undefined,
    priority: priorityFilter.value ? toBackendPriority(priorityFilter.value) : undefined
  };
  const res = await request.get("/repair/allRepair", { params });
  if (res.code === "200") {
    const list = res.data.list.map((o) => {
      const category = categories.value.find((c) => c.categoryId === o.categoryId);
      return { ...o, categoryName: category ? category.categoryName : "未知类别" };
    });
    if (sortOrder.value) {
      const factor = sortOrder.value === "asc" ? 1 : -1;
      list.sort((a, b) => {
        const timeA = new Date(a.updatedTime || a.createdTime || 0).getTime();
        const timeB = new Date(b.updatedTime || b.createdTime || 0).getTime();
        return (timeA - timeB) * factor;
      });
    }
    orders.value = list;
    total.value = res.data.total;
  }
};

// 打开详情
const openDetail = async (row) => {
  const res = await request.get(`/repair/detail/${row.orderId}`);
  if (res.code === "200") {
    const category = categories.value.find(c => c.categoryId === res.data.categoryId);
    detail.value = { ...res.data, categoryName: category ? category.categoryName : "未知类别" };
    detailVisible.value = true;
  }
};

// 打开分配
const openAssign = async (row) => {
  assignForm.value = { workerId: row.assignedWorker ? String(row.assignedWorker) : "", orderId: row.orderId, visitingTime: row.appointmentTime };
  assignDialogTitle.value = row.status === "待处理" ? "分配维修人员" : "更换维修人员";
  const res = await request.get("/repair/findWorkers", {
    params: { categoryId: row.categoryId, appointmentTime: row.appointmentTime }
  });
  availableWorkers.value = res.code === "200" ? res.data : [];
  assignVisible.value = true;
};

// 提交分配
const submitAssign = async () => {
  if (!assignForm.value.workerId) return ElMessage.warning("请选择维修人员");

  const adminId = JSON.parse(localStorage.getItem("code_user") || "{}").adminId;
  if (!adminId) return ElMessage.error("管理员信息获取失败");

  const formatLocalDateTime = (date) => {
    if (!date) return null;
    const pad = (num) => String(num).padStart(2, "0");
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());
    const seconds = pad(date.getSeconds());
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  };

  const payload = {
    orderId: assignForm.value.orderId,
    workerId: Number(assignForm.value.workerId),
    assignedBy: adminId,
    assignedTime: formatLocalDateTime(new Date()),
    visitingTime: assignForm.value.visitingTime
  };
  const res = await request.post("/repair/dispatchOrder", payload);
  if (res.code === "200") {
    ElMessage.success("分配成功");
    assignVisible.value = false;
    loadOrders();
  }
};

// 取消工单
const cancelOrder = (orderId) => {
  ElMessageBox.confirm("确定要取消该工单吗？", "提示", { type: "warning" }).then(async () => {
    await request.post(`/repair/cancel/${orderId}`);
    ElMessage.success("工单已取消");
    loadOrders();
  });
};

// 重置筛选
const resetFilter = () => {
  statusFilter.value = "";
  priorityFilter.value = "";
  sortOrder.value = "";
  loadOrders();
};

// 时间格式化
const formatTime = (time) => (!time ? "-" : new Date(time).toLocaleString());

onMounted(async () => {
  await loadCategories();
  await loadOrders();
});
</script>

<style scoped>
.page-shell {
  padding: 0;
}

.repair-card {
  padding: 24px;
}

.detail-dialog :deep(.el-dialog__header) {
  margin-bottom: 0;
  padding-bottom: 0;
}

.detail-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.step-container {
  padding: 20px 0;
  margin-bottom: 10px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.detail-tabs {
  min-height: 400px;
}

.info-section, .desc-section, .eval-section {
  padding: 10px;
}

.worker-tag {
  display: flex;
  align-items: center;
  gap: 8px;
}

.worker-name {
  font-weight: 500;
}

.desc-content {
  line-height: 1.6;
  color: #333;
  font-size: 14px;
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  min-height: 100px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  border-left: 4px solid #409EFF;
  padding-left: 10px;
}

.attachment-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.attachment-item {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #eee;
}

.attachment-img, .attachment-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.eval-card, .reply-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
}

.eval-card {
  background-color: #fff;
}

.reply-card {
  background-color: #f0f9eb;
  border-color: #e1f3d8;
  margin-top: 15px;
  margin-left: 20px;
}

.eval-header, .reply-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.user-name {
  font-weight: 600;
  font-size: 14px;
}

.eval-time, .reply-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.eval-content, .reply-content {
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.right {
  display: flex;
  align-items: center;
}

.total-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--primary-subtle);
  color: var(--primary-color);
  font-weight: 600;
  font-size: 12px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  background: var(--bg-color);
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-bar :deep(.el-select) {
  width: 180px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.modern-table :deep(.el-table__cell) {
  padding: 10px 8px;
}

.modern-table :deep(th.el-table__cell) {
  background: var(--bg-color);
  color: var(--text-secondary);
  font-weight: 600;
}

.modern-table :deep(.el-table__row) {
  transition: background 0.2s ease;
}

.modern-table :deep(.el-table__row:hover) {
  background: var(--primary-subtle);
}

.status-tag,
.priority-tag {
  border-radius: 999px;
  padding: 4px 10px;
  font-weight: 600;
  border: none;
}
</style>
