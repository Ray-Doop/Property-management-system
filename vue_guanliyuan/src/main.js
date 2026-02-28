// -------------------------- 关键修正：使用标准 npm 包导入路径 --------------------------
import { createApp } from 'vue' // 正确：从 'vue' 包导入（无需写缓存路径）
import App from './App.vue' // 正确：相对路径（或根据项目结构调整）
import router from './router/index.js' // 正确：相对路径
import './assets/css/global.css' // 正确：相对路径

// 导入 Element Plus 核心库（正确方式）
import ElementPlus from 'element-plus' 
// 导入 Element Plus 样式（正确方式）
import 'element-plus/dist/index.css'; 
// 导入中文语言包（正确方式）
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 导入 Element Plus 图标库（正确方式）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// -------------------------- 逻辑顺序修正 --------------------------
// 先创建 Vue 应用实例（必须！）
const app = createApp(App)

// 注册 Element Plus 图标（全局注册）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 使用 Element Plus 插件（传入中文配置）
app.use(ElementPlus, {
  locale: zhCn,
})

// 使用路由
app.use(router)

// 最后挂载应用
app.mount('#app')