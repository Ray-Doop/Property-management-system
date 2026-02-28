import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
const Login = () => import('@/pages/login/index.vue')
const NotFound = () => import('@/pages/error/404.vue')
const Layout = () => import('@/layouts/index.vue')
const Dashboard = () => import('@/pages/dashboard/index.vue')
const DashboardHome = () => import('@/pages/dashboard/home/index.vue')
const NoticeCreate = () => import('@/pages/system/notice/Create.vue')
const NoticeSummary = () => import('@/pages/system/notice/Summary.vue')
const NoticeAudit = () => import('@/pages/system/notice/Audit.vue')
const UserList = () => import('@/pages/system/user/index.vue')
const RoleManage = () => import('@/pages/system/role/index.vue')
const EmployeeList = () => import('@/pages/system/employee/index.vue')
const Approval = () => import('@/pages/system/approval/index.vue')
const PostList = () => import('@/pages/forum/PostList.vue')
const PostDetail = () => import('@/pages/forum/PostDetail.vue')
const CommentList = () => import('@/pages/forum/Commentlist.vue')
const MuteManage = () => import('@/pages/forum/MuteManage.vue')
const RepairOrder = () => import('@/pages/repair/RepairOrder.vue')
const RepairFeedback = () => import('@/pages/repair/Feedback.vue')
const FeeList = () => import('@/pages/pay/feeList.vue')
const PayResult = () => import('@/pages/pay/PayResult.vue')
const TravelLog = () => import('@/pages/travel/TravelLog.vue')
const TravelPassList = () => import('@/pages/travel/TravelPassList.vue')
const ActivityLog = () => import('@/pages/monitor/logininfor/index.vue')
const AdminProfile = () => import('@/pages/profile/index.vue')
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/login',
      component: Login,
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/pay/result',
      component: PayResult,
      meta: { title: '支付结果', requiresAuth: false }
    },
    {
      path: '/',
      component: Layout,
      meta: { title: '管理员后台', requiresAuth: true },
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          component: Dashboard,
          meta: { title: '数据统计', requiresAuth: true }
        },
        {
          path: 'profile',
          component: AdminProfile,
          meta: { title: '个人信息', requiresAuth: true }
        },
        {
          path: 'travel/pass-list',
          component: TravelPassList,
          meta: { title: '出行码列表', requiresAuth: true }
        },
        // {
        //   path: 'travel/log',
        //   component: TravelLog,
        //   meta: { title: '出行码使用情况', requiresAuth: true }
        // },
        {
          path: 'system/notice/create',
          component: NoticeCreate,
          meta: { title: '发布公告', requiresAuth: true }
        },
        {
          path: 'system/notice/list',
          component: NoticeSummary,
          meta: { title: '公告列表', requiresAuth: true }
        },
        {
          path: 'system/notice/audit',
          component: NoticeAudit,
          meta: { title: '公告审核', requiresAuth: true }
        },
        {
          path: 'system/user',
          component: UserList,
          meta: { title: '用户列表', requiresAuth: true }
        },
        {
          path: 'system/role',
          component: RoleManage,
          meta: { title: '角色管理', requiresAuth: true }
        },
        {
          path: 'system/employee',
          component: EmployeeList,
          meta: { title: '员工列表', requiresAuth: true }
        },
        {
          path: 'system/approval',
          component: Approval,
          meta: { title: '审核列表', requiresAuth: true }
        },
        {
          path: 'forum/posts',
          component: PostList,
          meta: { title: '帖子列表', requiresAuth: true }
        },
        {
          path: 'forum/posts/:id',
          component: PostDetail,
          props: true,
          meta: { title: '帖子详情', requiresAuth: true }
        },
        {
          path: 'forum/comments',
          component: CommentList,
          meta: { title: '评论列表', requiresAuth: true }
        },
        {
          path: 'forum/mute',
          component: MuteManage,
          meta: { title: '禁言用户', requiresAuth: true }
        },
        {
          path: 'repair/list',
          component: RepairOrder,
          meta: { title: '报修列表', requiresAuth: true }
        },
        {
          path: 'repair/feedback',
          component: RepairFeedback,
          meta: { title: '报修评价', requiresAuth: true }
        },
        {
          path: 'pay/fee-list',
          component: FeeList,
          meta: { title: '物业费列表', requiresAuth: true }
        },
        {
          path: 'monitor/logininfor',
          component: ActivityLog,
          meta: { title: '登录日志', requiresAuth: true }
        },
      ]
    },
    {
      path: '/:pathMatch(.*)',
      name: 'NotFound',
      component: NotFound,
      meta: { title: '404' }
    }
  ]
})

// 全局前置守卫（关键逻辑优化）
router.beforeEach((to, from, next) => {
  // 设置页面标题（优化：避免title为空）
  document.title = to.meta.title ? `${to.meta.title} - 智慧社区` : '智慧社区'

  // 获取登录状态（根据你的实际存储方式调整，示例用localStorage）
  const stored = localStorage.getItem('code_user') || localStorage.getItem('userInfo')
  const isLoggedIn = !!stored
  let role = ''
  try { role = JSON.parse(stored || '{}').role || '' } catch {}

  // 情况1：需要登录但未登录 → 重定向到登录页，并记录目标路径
  if (to.meta.requiresAuth && !isLoggedIn) {
    ElMessage.warning('请先登录')
    next({
      path: '/login',
      query: { redirect: to.fullPath } // 传递完整目标路径（含查询参数）
    })
    return
  }

  if (to.meta.requiresAuth && !['ADMIN', 'SUPER_ADMIN', 'OPERATOR'].includes(role)) {
    ElMessage.error('无权限访问管理员后台')
    next({ path: '/login' })
    return
  }

  if (to.path === '/login' && isLoggedIn) {
    ElMessage.info('您已登录，即将跳转到首页')
    next({ path: '/dashboard' })
    return
  }

  // 情况3：其他情况（无需登录或已登录访问受限页面）→ 正常放行
  next()
})

export default router
