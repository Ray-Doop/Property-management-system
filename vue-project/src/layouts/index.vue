<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside width="260px" class="layout-aside">
      <div class="logo-container">
        <img src="@/assets/imgs/logo.png" class="logo-img" alt="logo" />
        <div class="logo-text">
          <h1 class="app-title">智慧物业</h1>
          <span class="app-subtitle">管理中心</span>
        </div>
      </div>
      
      <el-scrollbar>
        <el-menu
          router
          :default-active="currentPath"
          class="sidebar-menu"
          :unique-opened="true"
          :collapse="false"
          background-color="transparent"
          text-color="var(--text-regular)"
          active-text-color="var(--primary-color)"
        >
          <div class="menu-section-label">主要功能</div>
          
          <el-sub-menu index="dashboard">
            <template #title>
              <el-icon><DataBoard /></el-icon>
              <span>数据看板</span>
            </template>
            <el-menu-item index="/dashboard">数据统计</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="system-notice">
            <template #title>
              <el-icon><Bell /></el-icon>
              <span>公告管理</span>
            </template>
            <el-menu-item index="/system/notice/list">公告列表</el-menu-item>
            <el-menu-item index="/system/notice/audit">公告审核</el-menu-item>
            <el-menu-item index="/system/notice/create">发布公告</el-menu-item>
          </el-sub-menu>

          <div class="menu-section-label">管理中心</div>

          <el-sub-menu index="system-user">
            <template #title>
              <el-icon><User /></el-icon>
              <span>组织人员</span>
            </template>
            <el-menu-item index="/system/user">用户列表</el-menu-item>
            <el-menu-item index="/system/role">角色管理</el-menu-item>
            <el-menu-item index="/system/employee">员工列表</el-menu-item>
            <el-menu-item index="/system/approval">用户审批</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="repair">
            <template #title>
              <el-icon><Tools /></el-icon>
              <span>报修服务</span>
            </template>
            <el-menu-item index="/repair/list">报修工单</el-menu-item>
            <el-menu-item index="/repair/feedback">服务评价</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="qrcode">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>智能通行</span>
            </template>
            <el-menu-item index="/travel/pass-list">通行码管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="community">
            <template #title>
              <el-icon><ChatLineSquare /></el-icon>
              <span>社区论坛</span>
            </template>
            <el-menu-item index="/forum/posts">帖子管理</el-menu-item>
            <el-menu-item index="/forum/comments">评论管理</el-menu-item>
            <el-menu-item index="/forum/mute">禁言管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="property">
            <template #title>
              <el-icon><Wallet /></el-icon>
              <span>物业收费</span>
            </template>
            <el-menu-item index="/pay/fee-list">费用清单</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="monitor">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>系统监控</span>
            </template>
            <el-menu-item index="/monitor/logininfor">登录日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <!-- Top Navigation -->
      <el-header height="64px" class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentRouteName }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <div class="user-profile">
            <el-dropdown trigger="click">
              <div class="avatar-wrapper">
                <el-avatar :size="36" :src="getImageUrl(userInfo?.avatarUrl || userInfo?.avatar)" class="user-avatar" @error="handleAvatarError">
                  <img :src="defaultAvatar" />
                </el-avatar>
                <div class="user-info">
                  <span class="username">{{ userInfo?.nickname || '管理员' }}</span>
                  <span class="role-badge">Admin</span>
                </div>
                <el-icon class="dropdown-icon"><CaretBottom /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu class="user-dropdown">
                  <el-dropdown-item @click="toPath('/profile')">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main class="layout-content">
        <div class="content-wrapper">
          <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  DataBoard,
  Bell,
  User,
  Tools,
  Grid,
  ChatLineSquare,
  Wallet,
  Monitor,
  SwitchButton,
  CaretBottom,
  HomeFilled
} from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import defaultAvatar from "@/assets/default-avatar.png";

const router = useRouter();
const route = useRoute();

const userInfo = ref(null);

const currentPath = computed(() => route.path);
const currentRouteName = computed(() => route.meta.title || '当前页面');

// Get User Info
const getUserInfoFromStorage = () => {
  try {
    const storedUser = localStorage.getItem("code_user");
    if (storedUser) {
      const parsedUser = JSON.parse(storedUser);
      if (parsedUser.token) {
        if (parsedUser.avatar && !parsedUser.avatarUrl) {
          parsedUser.avatarUrl = parsedUser.avatar;
        }
        userInfo.value = parsedUser;
      } else {
        localStorage.removeItem("code_user");
        router.push('/login');
      }
    }
  } catch {
    localStorage.removeItem("code_user");
  }
};

const getImageUrl = (url) => {
  if (!url) return defaultAvatar;
  try {
    const token = userInfo.value?.token || "";
    // Check if url is absolute
    if (url.startsWith('http')) {
       return `${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
    }
    // If relative, assume it's from backend
    return `http://localhost:8080${url}${url.includes("?") ? "&" : "?"}token=${encodeURIComponent(token)}`;
  } catch {
    return defaultAvatar;
  }
};

const toPath = (path) => {
  router.push(path);
};

const handleLogout = () => {
  ElMessageBox.confirm("确定要退出登录吗？", "提示", {
    confirmButtonText: "退出",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    localStorage.removeItem("code_user");
    localStorage.removeItem("token");
    router.push("/login");
    ElMessage.success("已安全退出");
  });
};

const handleAvatarError = () => {
  return true;
};

onMounted(() => {
  getUserInfoFromStorage();
});
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background-color: var(--bg-color);
}

/* Sidebar Styling */
.layout-aside {
  background-color: var(--surface-color);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 20;
  box-shadow: var(--shadow-sm);
}

.logo-container {
  height: 80px;
  display: flex;
  align-items: center;
  padding: 0 24px;
}

.logo-img {
  width: 36px;
  height: 36px;
  margin-right: 12px;
  filter: drop-shadow(0 4px 6px rgba(99, 102, 241, 0.3));
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.app-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin: 0;
  line-height: 1.2;
  letter-spacing: -0.025em;
}

.app-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.sidebar-menu {
  border-right: none;
  padding: 8px 16px;
}

.menu-section-label {
  padding: 16px 12px 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-placeholder);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

:deep(.el-menu-item), :deep(.el-sub-menu__title) {
  height: 44px;
  line-height: 44px;
  margin: 4px 0;
  border-radius: var(--radius-sm);
  color: var(--text-regular);
  font-weight: 500;
}

:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
  background-color: var(--bg-color);
  color: var(--primary-color);
}

:deep(.el-menu-item.is-active) {
  background-color: var(--primary-subtle);
  color: var(--primary-color);
  font-weight: 600;
}

/* Header Styling */
.layout-header {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: var(--shadow-sm);
}

.header-left {
  display: flex;
  align-items: center;
}

.user-profile {
  cursor: pointer;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  padding: 4px 8px 4px 4px;
  border-radius: 24px;
  border: 1px solid transparent;
  transition: all 0.2s;
}

.avatar-wrapper:hover {
  background-color: var(--bg-color);
  border-color: var(--border-light);
}

.user-avatar {
  border: 2px solid var(--surface-color);
  box-shadow: var(--shadow-sm);
}

.user-info {
  margin: 0 10px;
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.username {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
}

.role-badge {
  font-size: 10px;
  color: var(--text-secondary);
}

.dropdown-icon {
  color: var(--text-placeholder);
  font-size: 12px;
}

/* Main Content Styling */
.layout-content {
  padding: 0;
  background-color: var(--bg-color);
  overflow-x: hidden;
}

.content-wrapper {
  width: 100%;
  margin: 0 auto;
  padding: 24px 32px;
  min-height: calc(100vh - 64px);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .layout-aside {
    width: 0 !important;
    overflow: hidden;
  }
  
  .layout-header {
    padding: 0 16px;
  }
  
  .content-wrapper {
    padding: 16px;
  }
}
</style>
