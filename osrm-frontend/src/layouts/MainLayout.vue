<template>
  <div class="layout">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="sidebar-brand" @click="router.push('/')">
        <div class="brand-icon">
          <el-icon :size="20"><Box /></el-icon>
        </div>
        <span v-if="!isCollapsed" class="brand-name">OSRM</span>
      </div>

      <nav class="sidebar-nav">
        <template v-for="menu in visibleMenus" :key="menu.path">
          <router-link
            v-if="!menu.children"
            :to="menu.path"
            class="nav-item"
            :class="{ active: isActive(menu.path) }"
          >
            <el-icon :size="18"><component :is="menu.icon" /></el-icon>
            <span v-if="!isCollapsed" class="nav-label">{{ menu.title }}</span>
          </router-link>

          <div v-else>
            <div
              class="nav-item nav-group"
              :class="{ active: isActive(menu.path), collapsed: isCollapsed }"
              @click="toggleGroup(menu.path)"
            >
              <el-icon :size="18"><component :is="menu.icon" /></el-icon>
              <span v-if="!isCollapsed" class="nav-label">{{ menu.title }}</span>
              <el-icon v-if="!isCollapsed" class="nav-arrow" :class="{ expanded: expandedGroups.includes(menu.path) }">
                <ArrowDown />
              </el-icon>
            </div>
            <div v-show="expandedGroups.includes(menu.path) && !isCollapsed" class="nav-group-items">
              <template v-for="section in menu.children" :key="section.title">
                <div v-if="section.title" class="nav-section">{{ section.title }}</div>
                <router-link
                  v-for="item in section.items"
                  :key="item.path"
                  :to="item.path"
                  class="nav-item nav-child"
                  :class="{ active: route.path === item.path }"
                >
                  <span class="nav-label">{{ item.title }}</span>
                </router-link>
              </template>
            </div>
          </div>
        </template>
      </nav>

      <div class="sidebar-bottom">
        <div class="nav-item collapse-trigger" @click="toggleSidebar">
          <el-icon :size="18"><Fold v-if="!isCollapsed" /><Expand v-else /></el-icon>
          <span v-if="!isCollapsed" class="nav-label">收起侧边栏</span>
        </div>
      </div>
    </aside>

    <!-- Main area -->
    <div class="main-area">
      <!-- Top bar -->
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentMenuTitle">{{ currentMenuTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <!-- Theme Toggle -->
          <el-button
            circle
            size="small"
            class="theme-toggle"
            @click="toggleTheme"
          >
            <el-icon :size="16">
              <Moon v-if="isDark" />
              <Sunny v-else />
            </el-icon>
          </el-button>

          <el-dropdown @command="handleCommand">
            <div class="user-trigger">
              <el-avatar :size="28" class="user-avatar">{{ userInitials }}</el-avatar>
              <span class="user-name">{{ authStore.userInfo?.realName || authStore.userInfo?.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
                <el-dropdown-item command="settings"><el-icon><Setting /></el-icon>系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Content -->
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import {
  Box, ArrowDown, User, Setting, SwitchButton, Fold, Expand,
  HomeFilled, List, Plus, Collection, FolderOpened, CollectionTag,
  UserFilled, Edit, Timer, Clock, MapLocation, Key, Files,
  ShoppingCart, OfficeBuilding, Tools, Moon, Sunny
} from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useTheme()

const isCollapsed = ref(false)
const expandedGroups = ref<string[]>(['/software', '/subscription'])

const toggleSidebar = () => { isCollapsed.value = !isCollapsed.value }
const toggleGroup = (path: string) => {
  const idx = expandedGroups.value.indexOf(path)
  if (idx > -1) expandedGroups.value.splice(idx, 1)
  else expandedGroups.value.push(path)
}

const menus = [
  { title: '首页', path: '/', icon: 'HomeFilled' },
  {
    title: '软件管理', path: '/software', icon: 'Box',
    permission: 'package:read',
    children: [
      { title: '软件包', items: [
        { title: '软件包列表', path: '/software/packages', permission: 'package:read' },
        { title: '新增软件包', path: '/software/packages/create', permission: 'package:create' }
      ]},
      { title: '分类', items: [
        { title: '软件分类', path: '/software/categories', permission: 'package:read' },
        { title: '标签管理', path: '/software/tags', permission: 'package:read' }
      ]}
    ]
  },
  {
    title: '订购管理', path: '/subscription', icon: 'ShoppingCart',
    children: [
      { title: '', items: [
        { title: '我的订购', path: '/subscription/my', permission: 'subscription:read' },
        { title: '申请订购', path: '/subscription/apply', permission: 'subscription:create' },
        { title: '待审批', path: '/subscription/pending', permission: 'subscription:approve' },
        { title: '审批历史', path: '/subscription/history', permission: 'subscription:read' }
      ]}
    ]
  },
  {
    title: '业务系统', path: '/business', icon: 'OfficeBuilding',
    permission: 'business-system:read',
    children: [
      { title: '', items: [
        { title: '业务系统列表', path: '/business/systems', permission: 'business-system:read' },
        { title: '域管理', path: '/business/domains', permission: 'system:manage' }
      ]}
    ]
  },
  {
    title: '系统管理', path: '/system', icon: 'Tools',
    children: [
      { title: '用户权限', items: [
        { title: '用户管理', path: '/system/users', permission: 'user:read' },
        { title: '角色管理', path: '/system/roles', permission: 'role:read' },
        { title: '权限管理', path: '/system/permissions', permission: 'permission:read' }
      ]},
      { title: '基础配置', items: [
        { title: '存储配置', path: '/system/storage', permission: 'storage:read' },
        { title: '系统设置', path: '/system/settings', permission: 'system:manage' },
        { title: '使用统计', path: '/stats', permission: 'system:manage' }
      ]},
      { title: '使用追踪', items: [
        { title: '关联图', path: '/tracking/relationship-graph', permission: 'system:manage' }
      ]}
    ]
  }
]

// Filter menus based on user permissions
const visibleMenus = computed(() => {
  const userPermissions = authStore.userInfo?.permissions || []

  const hasPermission = (perm?: string) => {
    if (!perm) return false // 没有权限要求的菜单默认不显示
    return userPermissions.includes(perm)
  }

  const filterItems = (items: any[]) => {
    return items.filter(item => hasPermission(item.permission))
  }

  return menus.filter(menu => {
    if (menu.children) {
      // 有子菜单的：过滤子菜单项，只显示用户有权限的
      const filteredChildren = menu.children.map((section: any) => ({
        ...section,
        items: filterItems(section.items)
      })).filter((section: any) => section.items.length > 0)

      // 只有当有至少一个可用的子菜单项时才显示父菜单
      if (filteredChildren.length === 0) return false
      menu.children = filteredChildren
      return true
    } else {
      // 没有子菜单的叶子菜单
      return hasPermission(menu.permission)
    }
  })
})

const isActive = (path: string) => path === '/' ? route.path === '/' : route.path.startsWith(path)

const currentMenuTitle = computed(() => {
  for (const menu of visibleMenus.value) {
    if (menu.path !== '/' && route.path.startsWith(menu.path)) return menu.title
    if (menu.children) {
      for (const section of menu.children) {
        for (const item of section.items) {
          if (route.path === item.path) return item.title
        }
      }
    }
  }
  return ''
})

const userInitials = computed(() => {
  const name = authStore.userInfo?.realName || authStore.userInfo?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const handleCommand = async (command: string) => {
  if (command === 'profile') router.push('/profile')
  else if (command === 'settings') router.push('/system/settings')
  else if (command === 'logout') { await authStore.logout(); router.push('/landing') }
}
</script>

<style scoped lang="scss">
.layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-page);
}

// ===== Sidebar =====
.sidebar {
  width: var(--sidebar-width);
  background: var(--color-bg-sidebar);
  border-right: 1px solid var(--color-sidebar-border);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-slow);
  flex-shrink: 0;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;

  &.collapsed {
    width: 64px;
  }
}

.sidebar-brand {
  height: var(--header-height);
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: 0 var(--space-lg);
  border-bottom: 1px solid var(--color-sidebar-border);
  cursor: pointer;

  .brand-icon {
    width: 32px;
    height: 32px;
    background: var(--color-primary);
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    flex-shrink: 0;
  }

  .brand-name {
    font-size: var(--font-size-xl);
    font-weight: var(--font-weight-bold);
    color: var(--color-sidebar-text-active);
    letter-spacing: -0.5px;
  }
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-sm) 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) var(--space-md);
  margin: 1px var(--space-sm);
  border-radius: var(--radius-md);
  color: var(--color-sidebar-text);
  text-decoration: none;
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  user-select: none;

  &:hover {
    background: var(--color-sidebar-bg-hover);
    color: var(--color-sidebar-text-active);
  }

  &.active {
    background: var(--color-sidebar-bg-active);
    color: var(--color-sidebar-text-active);
    font-weight: var(--font-weight-medium);
  }

  .el-icon {
    flex-shrink: 0;
  }
}

.nav-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-arrow {
  font-size: 12px !important;
  transition: transform var(--transition-fast);

  &.expanded {
    transform: rotate(180deg);
  }
}

.nav-group-items {
  padding: var(--space-xs) 0;
}

.nav-section {
  padding: var(--space-lg) var(--space-lg) var(--space-xs) var(--space-xl);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.nav-child {
  padding-left: calc(var(--space-lg) + 28px);
  font-size: var(--font-size-sm);
}

.sidebar-bottom {
  border-top: 1px solid var(--color-border);
  padding: var(--space-sm) 0;
}

.collapse-trigger {
  .el-icon {
    color: var(--color-text-tertiary);
  }
}

// ===== Main Area =====
.main-area {
  flex: 1;
  margin-left: var(--sidebar-width);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  transition: margin-left var(--transition-slow);

  .sidebar.collapsed + & {
    margin-left: 64px;
  }
}

.topbar {
  height: var(--header-height);
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-2xl);
  position: sticky;
  top: 0;
  z-index: 50;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.theme-toggle {
  border: none !important;
  background: transparent !important;
  color: var(--color-text-secondary) !important;

  &:hover {
    background: var(--color-bg-card-hover) !important;
    color: var(--color-text-primary) !important;
  }
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);

  &:hover {
    background: var(--color-bg-card-hover);
  }
}

.user-avatar {
  background: var(--color-primary);
  color: white;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}

.user-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

  .content {
    flex: 1;
    padding: var(--space-lg);
    overflow-y: auto;
  }

// Element Plus menu dark mode support
:deep(.el-menu) {
  background-color: transparent !important;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  color: var(--color-sidebar-text) !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--color-sidebar-bg-hover) !important;
}

:deep(.el-menu-item.is-active) {
  background-color: var(--color-sidebar-bg-active) !important;
  color: var(--color-sidebar-text-active) !important;
}
</style>
