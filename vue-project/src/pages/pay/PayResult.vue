<template>
  <div class="result-page">
    <div class="result-card">
      <div class="title">支付结果</div>
      <div class="status" :class="statusClass">{{ message }}</div>
      <div class="order" v-if="orderNo">订单号：{{ orderNo }}</div>
      <div class="actions">
        <button class="primary" @click="goHome">返回首页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter } from "vue-router";
import request from "@/api/request.js";

const router = useRouter();
const status = ref("loading");
const message = ref("正在查询支付结果...");
const orderNo = ref("");

const statusClass = computed(() => {
  if (status.value === "success") return "success";
  if (status.value === "pending") return "pending";
  if (status.value === "error") return "error";
  return "";
});

const goHome = () => {
  router.push("/dashboard");
};

onMounted(async () => {
  const params = new URLSearchParams(window.location.search);
  orderNo.value = params.get("out_trade_no") || "";
  if (!orderNo.value) {
    status.value = "error";
    message.value = "未找到订单号";
    return;
  }
  try {
    const res = await request.get("/api/pay/return/alipay", {
      params: { out_trade_no: orderNo.value },
    });
    if (res && typeof res.status !== "undefined") {
      if (res.status === 2) {
        status.value = "success";
        message.value = "支付成功";
      } else if (res.status === 0) {
        status.value = "pending";
        message.value = "支付处理中";
      } else {
        status.value = "error";
        message.value = "未支付";
      }
    } else {
      status.value = "error";
      message.value = "查询失败";
    }
  } catch {
    status.value = "error";
    message.value = "查询失败";
  }
});
</script>

<style scoped>
.result-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f7fb;
  padding: 24px;
}

.result-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08);
  padding: 28px 24px;
  text-align: center;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 12px;
}

.status {
  font-size: 18px;
  margin-bottom: 10px;
  color: #374151;
}

.status.success {
  color: #16a34a;
}

.status.pending {
  color: #d97706;
}

.status.error {
  color: #dc2626;
}

.order {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.actions {
  display: flex;
  justify-content: center;
}

.primary {
  border: none;
  background: #3b82f6;
  color: #fff;
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.primary:hover {
  background: #2563eb;
}
</style>
