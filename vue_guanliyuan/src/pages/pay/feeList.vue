<template>
  <div class="page-container">
    <!-- Header / Filter Section -->
    <div class="header-section">
      <div class="title-area">
        <h2>物业费账单</h2>
        <p class="subtitle">管理和发布社区物业费账单</p>
      </div>
      <div class="filter-area">
        <div class="search-group">
          <el-input 
            v-model="queryParams.residenceId" 
            placeholder="搜索住户ID" 
            prefix-icon="Search"
            style="width: 180px" 
            clearable 
            @clear="loadBills"
            @keyup.enter="loadBills"
          />
          <el-date-picker 
            v-model="queryParams.month" 
            type="month" 
            placeholder="选择月份" 
            format="YYYY-MM" 
            value-format="YYYY-MM-DD" 
            style="width: 140px" 
            @change="loadBills"
          />
          <el-select 
            v-model="queryParams.status" 
            placeholder="状态" 
            clearable 
            style="width: 120px" 
            @change="loadBills"
          >
            <el-option label="全部" :value="null" />
            <el-option label="待支付" :value="1" />
            <el-option label="已支付" :value="2" />
            <el-option label="已取消" :value="0" />
            <el-option label="已关闭" :value="3" />
          </el-select>
          <el-button type="primary" @click="loadBills">搜索</el-button>
        </div>
        
        <div class="action-group">
           <el-button type="success" icon="Plus" @click="openPublishDialog">发布账单</el-button>
        </div>
      </div>
    </div>

    <!-- Bill Cards Grid -->
    <div class="card-grid" v-loading="loading">
      <el-empty v-if="tableData.length === 0" description="暂无账单数据" />
      <el-row :gutter="20" v-else>
        <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in tableData" :key="item.billNo">
          <div class="bill-card" @click="viewDetail(item.billNo)">
            <div class="card-header">
              <div class="bill-title" :title="item.title">{{ item.title }}</div>
              <el-tag :type="getStatusType(item.status)" size="small" effect="light" round>
                {{ getStatusLabel(item.status) }}
              </el-tag>
            </div>
            
            <div class="card-body">
              <div class="amount-area" :class="{'paid': item.status === 2}">
                <span class="currency">¥</span>
                <span class="amount">{{ item.amount }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">住户ID</span>
                <span class="value">{{ item.residenceId }}</span>
              </div>
              
              <div class="info-row">
                <span class="label">账期</span>
                <span class="value">{{ formatPeriod(item.periodStart) }} - {{ formatPeriod(item.periodEnd) }}</span>
              </div>
            </div>
            
            <div class="card-footer">
               <span class="bill-no">NO. {{ item.billNo }}</span>
               <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- Pagination -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <!-- Publish Dialog -->
    <el-dialog v-model="publishVisible" title="发布月度物业费" width="400px" align-center>
      <el-form :model="publishForm" label-width="80px" class="publish-form">
        <el-form-item label="选择月份">
          <el-date-picker 
            v-model="publishForm.month" 
            type="month" 
            value-format="YYYY-MM"
            placeholder="选择月份" 
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="单价">
           <el-input v-model="publishForm.unitPrice" type="number" placeholder="元/平方">
             <template #append>元/㎡</template>
           </el-input>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="publishForm.title" placeholder="例如：2026年2月物业费" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPublish" :loading="publishing">确认发布</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="账单详情" width="500px" align-center class="detail-dialog">
      <div v-if="detailData" class="detail-content">
        <div class="detail-header">
           <div class="detail-amount">¥ {{ detailData.amount }}</div>
           <el-tag :type="getStatusType(detailData.status)">{{ getStatusLabel(detailData.status) }}</el-tag>
        </div>
        
        <div class="detail-list">
          <div class="detail-item">
            <span class="label">账单标题</span>
            <span class="value">{{ detailData.title }}</span>
          </div>
          <div class="detail-item">
            <span class="label">订单号</span>
            <span class="value">{{ detailData.billNo }}</span>
          </div>
          <div class="detail-item">
            <span class="label">住户ID</span>
            <span class="value">{{ detailData.residenceId }}</span>
          </div>
          <div class="detail-item">
            <span class="label">账期范围</span>
            <span class="value">{{ formatDateTime(detailData.periodStart) }} ~ {{ formatDateTime(detailData.periodEnd) }}</span>
          </div>
          <div class="detail-item full" v-if="detailData.orderRemark">
            <span class="label">备注</span>
            <span class="value">{{ detailData.orderRemark }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue";
import request from "@/api/request";
import dayjs from "dayjs";
import { ElMessage } from "element-plus";
import { Search, Plus, ArrowRight } from "@element-plus/icons-vue";

const queryParams = ref({
  residenceId: "",
  month: "",
  status: null,
});

const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(12); // Use 12 for better grid alignment (divisible by 2, 3, 4)
const total = ref(0);
const loading = ref(false);

const detailVisible = ref(false);
const detailData = ref(null);

// Publish related
const publishVisible = ref(false);
const publishing = ref(false);
const publishForm = reactive({
  month: dayjs().format('YYYY-MM'),
  unitPrice: '2.5',
  title: dayjs().format('YYYY年M月物业费')
});

const getStatusType = (status) => {
  const map = {
    0: 'danger',
    1: 'warning',
    2: 'success',
    3: 'info'
  };
  return map[status] || 'info';
};

const getStatusLabel = (status) => {
  const map = {
    0: '已取消',
    1: '待支付',
    2: '已支付',
    3: '已关闭'
  };
  return map[status] || '未知';
};

const formatPeriod = (dateStr) => {
  if (!dateStr) return '';
  return dayjs(dateStr).format('MM-DD');
};

const formatDateTime = (dateStr) => {
  if (!dateStr) return '';
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss');
};

const openPublishDialog = () => {
  publishVisible.value = true
  publishForm.month = ''
  publishForm.unitPrice = 2.5
  publishForm.title = '物业费账单'
}

const confirmPublish = async () => {
  if (!publishForm.month || !publishForm.unitPrice || !publishForm.title) {
    return ElMessage.warning("请填写完整信息");
  }
  
  publishing.value = true;
  try {
    const res = await request.post("/api/fee/publishMonthly", publishForm);
    if (res.code === "200") {
      ElMessage.success("发布成功");
      publishVisible.value = false;
      loadBills();
    } else {
      ElMessage.error(res.msg || "发布失败");
    }
  } catch (e) {
    ElMessage.error("系统错误");
  } finally {
    publishing.value = false;
  }
};

const loadBills = async () => {
  loading.value = true;
  let url = "/api/fee/allBills"; // Default API

  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
  };

  if (queryParams.value.residenceId) {
    url = "/api/fee/billsById";
    params.residenceId = queryParams.value.residenceId;
  }

  if (queryParams.value.month) {
    url = "/api/fee/FilterMonth";
    params.periodStart = dayjs(queryParams.value.month).format("YYYYMMDD");
  }

  try {
    const res = await request({
      url,
      method: "get",
      params,
    });

    if (res.code === "200") {
      const list = res.data.list || [];
      tableData.value = queryParams.value.status != null
          ? list.filter((item) => item.status === queryParams.value.status)
          : list;
      total.value = res.data.total || 0;
    }
  } finally {
    loading.value = false;
  }
};

const viewDetail = async (billNo) => {
  const res = await request({
    url: "/api/fee/billDetail",
    method: "get",
    params: { billNo },
  });

  if (res.code === "200") {
    detailData.value = res.data;
    detailVisible.value = true;
  }
};

const handlePageChange = (newPage) => {
  pageNum.value = newPage;
  loadBills();
};

const publishQuick = async () => {
  const res = await request.post("/api/fee/publishQuick");
  if (res.code === "200") {
    ElMessage.success("已发布示例账单");
    loadBills();
  }
};

loadBills();
</script>

<style scoped>
.page-container {
  padding: 20px;
  max-width: 1600px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.title-area h2 {
  font-size: 24px;
  margin: 0 0 8px 0;
  color: #1f2937;
}

.title-area .subtitle {
  margin: 0;
  color: #6b7280;
  font-size: 14px;
}

.filter-area {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.search-group {
  display: flex;
  gap: 12px;
  background: #fff;
  padding: 4px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.action-group {
  display: flex;
  gap: 12px;
}

/* Card Grid Styles */
.bill-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}

.bill-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 20px -8px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.bill-title {
  font-weight: 600;
  color: #374151;
  font-size: 15px;
  line-height: 1.4;
  flex: 1;
  margin-right: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.amount-area {
  margin-bottom: 16px;
  color: #374151;
}

.amount-area.paid {
  color: #10b981;
}

.amount-area .currency {
  font-size: 14px;
  font-weight: 500;
  margin-right: 2px;
}

.amount-area .amount {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}

.info-row .label {
  color: #9ca3af;
}

.info-row .value {
  color: #4b5563;
  font-weight: 500;
}

.card-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #9ca3af;
  font-size: 12px;
}

.arrow-icon {
  opacity: 0;
  transform: translateX(-5px);
  transition: all 0.2s ease;
}

.bill-card:hover .arrow-icon {
  opacity: 1;
  transform: translateX(0);
  color: #3b82f6;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

/* Detail Dialog Styles */
.detail-header {
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid #f3f4f6;
  margin-bottom: 20px;
}

.detail-amount {
  font-size: 36px;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 8px;
}

.detail-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item.full {
  grid-column: span 2;
}

.detail-item .label {
  font-size: 12px;
  color: #9ca3af;
}

.detail-item .value {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

@media (max-width: 768px) {
  .header-section {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-area {
    width: 100%;
  }
  
  .search-group {
    flex-direction: column;
    width: 100%;
  }
  
  .search-group .el-input,
  .search-group .el-select,
  .search-group .el-date-picker {
    width: 100% !important;
  }
}
</style>
