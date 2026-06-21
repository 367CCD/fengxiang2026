<template>
  <div class="admin-layout">
    <!-- 顶部栏 -->
    <header class="admin-header">
      <div class="header-left">
        <span class="logo-icon">🛡️</span>
        <span class="logo-text">风享管理后台</span>
      </div>
      <div class="header-right">
        <el-dropdown trigger="click" @command="handleCommand">
          <span class="admin-info">
            <el-avatar :size="32" :src="store.avatar || undefined">{{ store.username?.charAt(0)?.toUpperCase() }}</el-avatar>
            <span class="admin-name">{{ store.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home">返回用户端</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="admin-body">
      <!-- 侧边栏 -->
      <aside class="admin-sidebar">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据看板</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/posts">
            <el-icon><Document /></el-icon>
            <span>内容管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/comments">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/reports">
            <el-icon><WarningFilled /></el-icon>
            <span>举报处理</span>
          </el-menu-item>
          <el-menu-item index="/admin/tags">
            <el-icon><CollectionTag /></el-icon>
            <span>标签管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notifications">
            <el-icon><Message /></el-icon>
            <span>通知管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/search">
            <el-icon><Search /></el-icon>
            <span>搜索运营</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="admin-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, UserFilled, Document, ChatDotRound, WarningFilled, CollectionTag, Message, DataAnalysis, Search } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const activeMenu = computed(() => route.path)

function handleCommand(cmd) {
  if (cmd === 'home') {
    router.push('/discover')
  } else if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
}

.admin-header {
  height: 56px;
  background: #1d1e2c;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
  z-index: 100;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logo-icon { font-size: 22px; }
.logo-text { font-size: 18px; font-weight: 600; letter-spacing: 1px; }

.header-right {
  display: flex;
  align-items: center;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #e0e0e0;
}
.admin-info:hover { color: #fff; }
.admin-name { font-size: 14px; }

.admin-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.admin-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-menu {
  border-right: none !important;
  padding-top: 8px;
}

.sidebar-menu .el-menu-item {
  height: 48px;
  line-height: 48px;
  font-size: 14px;
  margin: 2px 8px;
  border-radius: 6px;
}

.sidebar-menu .el-menu-item.is-active {
  background: #e8f0fe;
  color: #1a73e8;
  font-weight: 500;
}

.admin-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
