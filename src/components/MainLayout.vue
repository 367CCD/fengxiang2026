<template>  <div class="layout">    <!-- 顶部搜索栏 -->    <header class="top-header">      <div class="header-inner">        <router-link to="/discover" class="logo">          <span class="logo-icon">📖</span>          <span class="logo-text">风享</span>        </router-link>        <div class="search-box">          <el-input            v-model="searchKeyword"            placeholder="搜索感兴趣的内容..."            :prefix-icon="Search"            size="large"            clearable            @keyup.enter="doSearch" @clear="doSearch"          />        </div>        <div class="header-actions">          <el-button class="publish-btn" type="primary" round @click="goPublish">            <el-icon style="margin-right:4px"><Plus /></el-icon>发布          </el-button>          <el-dropdown trigger="click" @command="handleUserCommand">            <span class="user-avatar">              <el-avatar :size="36" :src="store.avatar || undefined">                {{ store.username?.charAt(0)?.toUpperCase() }}              </el-avatar>              <span class="user-name-text">{{ store.username }}</span>            </span>            <template #dropdown>              <el-dropdown-menu>                <el-dropdown-item command="profile">个人中心</el-dropdown-item>                <el-dropdown-item command="logout">退出登录</el-dropdown-item>              </el-dropdown-menu>            </template>
          </el-dropdown>        </div>      </div>    </header>    <div class="layout-body">      <!-- 左侧导航 -->      <aside class="sidebar">        <el-menu          :default-active="activeMenu"          router          class="sidebar-menu"        >          <el-menu-item index="/discover">            <el-icon><Compass /></el-icon>            <span>发现</span>          </el-menu-item>          <el-menu-item index="/publish">            <el-icon><Edit /></el-icon>            <span>发布</span>          </el-menu-item>          <el-menu-item index="/notifications">            <el-icon><Bell /></el-icon>            <span>通知</span>            <el-badge v-if="store.unreadCount > 0" :value="store.unreadCount" class="notif-badge" />          </el-menu-item>          <el-menu-item index="/profile">            <el-icon><User /></el-icon>            <span>我的</span>          </el-menu-item>          <template v-if="store.userInfo?.role === 1">            <div class="menu-divider"></div>            <div class="menu-section-title">管理后台</div>            <el-menu-item index="/admin/dashboard">              <el-icon><Setting /></el-icon>              <span>管理后台</span>            </el-menu-item>          </template>        </el-menu>      </aside>      <!-- 主内容区 -->      <main class="main-content">        <router-view />      </main>    </div>  </div></template>

<script setup>import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Plus, Compass, Edit, Bell, User, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { notificationApi } from '../api'
const router = useRouter()
const route = useRoute()
const store = useUserStore()
const searchKeyword = ref('')
const activeMenu = computed(() => route.path);
function goPublish() {  router.push('/publish')}
function doSearch() {  const kw = searchKeyword.value.trim();if (kw) {    router.push({ path: '/discover', query: { q: kw } })  } else {    router.push({ path: '/discover' })  }}
function handleUserCommand(command) {  if (command === 'profile') {    router.push('/profile')  } else if (command === 'logout') {    store.logout();    router.push('/login')  }}
onMounted(() => {  if (store.isLoggedIn) {    store.fetchProfile();
    store.fetchUnreadCount() } });
</script>

<style scoped>.layout {  height: 100vh;  display: flex;  flex-direction: column;}
.top-header {  height: var(--header-height);  background: #fff;  border-bottom: 1px solid var(--border-color);  display: flex;  align-items: center;  position: fixed;  top: 0;  left: 0;  right: 0;  z-index: 100;}
.header-inner {  width: 100%;  max-width: 1200px;  margin: 0 auto;  display: flex;  align-items: center;  gap: 20px;  padding: 0 20px;}
.logo {  display: flex;  align-items: center;  gap: 6px;  text-decoration: none;  color: var(--primary-color);  font-weight: bold;}
.logo-icon { font-size: 28px; }
.logo-text { font-size: 20px; }
.search-box {  flex: 1;  max-width: 500px;}
.header-actions {  display: flex;  align-items: center;  gap: 12px;}
.publish-btn {  font-weight: 500;}
.user-avatar {  cursor: pointer;}
.layout-body {  display: flex;  margin-top: var(--header-height);  height: calc(100vh - var(--header-height));}
.sidebar {  width: var(--sidebar-width);  background: #fff;  border-right: 1px solid var(--border-color);  flex-shrink: 0;  overflow-y: auto;  position: fixed;  top: var(--header-height);  left: 0;  bottom: 0;  z-index: 50;}
.sidebar-menu {  border-right: none !important;  padding-top: 12px;}
.sidebar-menu .el-menu-item {  height: 50px;  line-height: 50px;  font-size: 15px;  margin: 4px 8px;  border-radius: 8px;}
.sidebar-menu .el-menu-item.is-active {  background: #fff0f0;  color: var(--primary-color);  font-weight: 500;}
.sidebar-menu .el-menu-item:hover {  background: #f5f5f5;}
.notif-badge {  margin-left: auto;}
.main-content {  flex: 1;  margin-left: var(--sidebar-width);  padding: 20px;  overflow-y: auto;  max-width: calc(100% - var(--sidebar-width));}
.menu-divider {  height: 1px;  background: var(--border-color);  margin: 8px 16px;}
.menu-section-title {  font-size: 12px;  color: var(--text-secondary);  padding: 8px 20px 4px;  letter-spacing: 1px;}</style>
