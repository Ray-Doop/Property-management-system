<template>
  <div class="dashboard-container">
    <div class="section-header">
      <h2 class="section-title">数据概览</h2>
      <el-button type="primary" plain @click="refreshData" :loading="loading">
        <el-icon class="el-icon--left"><Refresh /></el-icon>刷新数据
      </el-button>
    </div>

    <!-- 统计卡片区 -->
    <el-row :gutter="24" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" :lg="4.8" v-for="(item, index) in statsItems" :key="index">
        <div class="modern-card stats-card">
          <div class="stats-icon-wrapper" :style="{ background: item.bg, color: item.color }">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div class="stats-info">
            <div class="stats-label">{{ item.label }}</div>
            <div class="stats-value">
              <span class="number">{{ item.value || 0 }}</span>
              <span class="unit" v-if="item.unit">{{ item.unit }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="24" class="chart-row">
      <el-col :span="24">
        <div class="modern-card chart-card">
          <div class="card-header">
            <div class="header-title">
              <span class="title">数据趋势分析</span>
              <span class="subtitle">近期的系统活跃度与各项指标走势</span>
            </div>
            <el-radio-group v-model="trendRange" size="small" @change="loadTrend">
              <el-radio-button value="7">近7天</el-radio-button>
              <el-radio-button value="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
          <div class="card-body">
            <div id="stats-chart" class="chart-container"></div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from "vue";
import * as echarts from "echarts";
import request from "@/api/request.js";
import { User, UserFilled, Document, ChatDotRound, Bicycle, Refresh } from "@element-plus/icons-vue";

const loading = ref(false);
const overview = reactive({});
const trendRange = ref("7");
let chartInstance = null;

const statsItems = computed(() => [
  { 
    label: "用户总数", 
    value: overview.userCount, 
    icon: "User", 
    bg: "var(--primary-subtle)",
    color: "var(--primary-color)"
  },
  { 
    label: "管理员", 
    value: overview.adminCount, 
    icon: "UserFilled", 
    bg: "#f3e8ff",
    color: "#9333ea"
  },
  { 
    label: "帖子总数", 
    value: overview.postCount, 
    icon: "Document", 
    bg: "#ffedd5",
    color: "#f97316"
  },
  { 
    label: "评论互动", 
    value: overview.commentCount, 
    icon: "ChatDotRound", 
    bg: "#dcfce7",
    color: "#16a34a"
  },
  { 
    label: "出行码申请数量", 
    value: overview.travelIssuedToday, 
    icon: "Bicycle", 
    bg: "#ccfbf1",
    color: "#0d9488"
  }
]);

const refreshData = async () => {
  loading.value = true;
  await Promise.all([loadOverview(), loadTrend()]);
  loading.value = false;
};

const loadOverview = async () => {
  try {
    const res = await request.get("/admin/stats/overview");
    if (res.code === "200") {
      Object.assign(overview, res.data || {});
    }
  } catch (e) {
    console.error("Failed to load overview", e);
  }
};

const loadTrend = async () => {
  try {
    const res = await request.get("/admin/stats/trend", {
      params: { days: trendRange.value },
    });
    
    if (res.code !== "200") return;
    
    const data = res.data || {};
    const dates = (data.login || []).map((i) => i.date);
    
    const option = {
      tooltip: { 
        trigger: "axis",
        backgroundColor: 'rgba(255, 255, 255, 0.95)',
        borderColor: '#e2e8f0',
        padding: [12, 16],
        textStyle: { color: '#0f172a', fontSize: 13 },
        extraCssText: 'box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); border-radius: 8px;'
      },
      legend: { 
        data: ["登录人数", "发帖量", "评论量", "出行码使用"],
        bottom: 0,
        icon: "circle",
        itemGap: 24,
        textStyle: { color: '#64748b' }
      },
      grid: {
        left: '2%',
        right: '2%',
        bottom: '10%',
        top: '5%',
        containLabel: true
      },
      xAxis: { 
        type: "category", 
        data: dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b', margin: 16 },
        axisTick: { show: false }
      },
      yAxis: { 
        type: "value",
        splitLine: { lineStyle: { type: 'dashed', color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' }
      },
      series: [
        {
          name: "登录人数",
          type: "line",
          smooth: true,
          showSymbol: false,
          symbolSize: 8,
          data: (data.login || []).map((i) => i.cnt),
          itemStyle: { color: '#6366f1' },
          lineStyle: { width: 3 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(99, 102, 241, 0.2)' },
              { offset: 1, color: 'rgba(99, 102, 241, 0)' }
            ])
          }
        },
        {
          name: "发帖量",
          type: "line",
          smooth: true,
          showSymbol: false,
          data: (data.post || []).map((i) => i.cnt),
          itemStyle: { color: '#f59e0b' },
          lineStyle: { width: 3 }
        },
        {
          name: "评论量",
          type: "line",
          smooth: true,
          showSymbol: false,
          data: (data.comment || []).map((i) => i.cnt),
          itemStyle: { color: '#10b981' },
          lineStyle: { width: 3 }
        },
        {
          name: "出行码使用",
          type: "line",
          smooth: true,
          showSymbol: false,
          data: (data.travel || []).map((i) => i.cnt),
          itemStyle: { color: '#06b6d4' },
          lineStyle: { width: 3 }
        },
      ],
    };
    
    if (!chartInstance) {
      const chartDom = document.getElementById("stats-chart");
      if (chartDom) {
        chartInstance = echarts.init(chartDom);
      }
    }
    chartInstance?.setOption(option);
  } catch (e) {
    console.error(e);
  }
};

onMounted(async () => {
  await refreshData();
  window.addEventListener("resize", () => chartInstance?.resize());
});
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.stats-row {
  margin-bottom: 24px;
}

.stats-card {
  display: flex;
  align-items: center;
  padding: 24px;
  height: 100%;
  border: 1px solid var(--border-light);
  background: var(--surface-color);
}

.stats-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 26px;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.stats-card:hover .stats-icon-wrapper {
  transform: scale(1.1);
}

.stats-info {
  flex: 1;
  overflow: hidden;
}

.stats-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.stats-value {
  display: flex;
  align-items: baseline;
}

.stats-value .number {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1;
  letter-spacing: -0.02em;
}

.stats-value .unit {
  font-size: 13px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.chart-card {
  background: var(--surface-color);
  padding: 0;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  flex-direction: column;
}

.card-header .title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 1.2;
}

.card-header .subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.card-body {
  padding: 24px;
}

.chart-container {
  height: 400px;
  width: 100%;
}

@media (max-width: 768px) {
  .stats-row .el-col {
    margin-bottom: 16px;
  }
}
</style>
