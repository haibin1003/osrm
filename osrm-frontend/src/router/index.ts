import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/modules/auth'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/landing',
    name: 'Landing',
    component: () => import('@/views/landing/Index.vue'),
    meta: { public: true }
  },
  {
    path: '/browse',
    component: () => import('@/layouts/PublicLayout.vue'),
    meta: { public: true },
    children: [
      {
        path: '',
        name: 'BrowseSoftware',
        component: () => import('@/views/portal/SoftwarePortal.vue'),
        meta: { public: true }
      },
      {
        path: 'software/:id',
        name: 'BrowseDetail',
        component: () => import('@/views/portal/SoftwareDetail.vue'),
        meta: { public: true }
      }
    ]
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: () => {
      const token = localStorage.getItem('accessToken')
      return token ? '/home' : '/landing'
    },
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Index.vue'),
        meta: { title: '首页' }
      },
      // 软件管理
      {
        path: 'software/packages',
        name: 'SoftwarePackages',
        component: () => import('@/views/software/Packages.vue'),
        meta: { title: '软件包列表' }
      },
      {
        path: 'software/packages/create',
        name: 'PackageCreate',
        component: () => import('@/views/software/PackageCreate.vue'),
        meta: { title: '新增软件包' }
      },
      {
        path: 'software/versions',
        name: 'VersionManagement',
        component: () => import('@/views/software/VersionManagement.vue'),
        meta: { title: '版本管理' }
      },
      {
        path: 'software/categories',
        name: 'Categories',
        component: () => import('@/views/software/Categories.vue'),
        meta: { title: '软件分类' }
      },
      {
        path: 'software/tags',
        name: 'Tags',
        component: () => import('@/views/software/Tags.vue'),
        meta: { title: '标签管理' }
      },
      // 订购管理
      {
        path: 'subscription/my',
        name: 'MySubscriptions',
        component: () => import('@/views/subscription/MySubscriptions.vue'),
        meta: { title: '我的订购' }
      },
      {
        path: 'subscription/apply',
        name: 'ApplySubscription',
        component: () => import('@/views/subscription/ApplySubscription.vue'),
        meta: { title: '申请订购' }
      },
      {
        path: 'subscription/pending',
        name: 'PendingApproval',
        component: () => import('@/views/subscription/PendingApproval.vue'),
        meta: { title: '待审批', permissions: ['subscription:approve'] }
      },
      {
        path: 'subscription/history',
        name: 'ApprovalHistory',
        component: () => import('@/views/subscription/ApprovalHistory.vue'),
        meta: { title: '审批历史' }
      },
      // 存量登记
      {
        path: 'inventory/my',
        name: 'MyInventory',
        component: () => import('@/views/inventory/MyInventory.vue'),
        meta: { title: '我的存量', permissions: ['inventory:read'] }
      },
      {
        path: 'inventory/create',
        name: 'CreateInventory',
        component: () => import('@/views/inventory/CreateInventory.vue'),
        meta: { title: '存量登记', permissions: ['inventory:create'] }
      },
      {
        path: 'inventory/pending',
        name: 'PendingInventory',
        component: () => import('@/views/inventory/PendingInventory.vue'),
        meta: { title: '存量审批', permissions: ['inventory:approve'] }
      },
      {
        path: 'inventory/manage',
        name: 'InventoryManage',
        component: () => import('@/views/inventory/InventoryManage.vue'),
        meta: { title: '存量管理', permissions: ['inventory:manage'] }
      },
      // 业务系统
      {
        path: 'business/systems',
        name: 'BusinessSystems',
        component: () => import('@/views/business/Systems.vue'),
        meta: { title: '业务系统列表' }
      },

      // 门户（访客可访问）
      {
        path: 'portal',
        name: 'SoftwarePortal',
        component: () => import('@/views/portal/SoftwarePortal.vue'),
        meta: { title: '软件门户', public: true }
      },
      {
        path: 'portal/software/:id',
        name: 'SoftwareDetail',
        component: () => import('@/views/portal/SoftwareDetail.vue'),
        meta: { title: '软件详情', public: true }
      },
      {
        path: 'business/domains',
        name: 'Domains',
        component: () => import('@/views/business/Domains.vue'),
        meta: { title: '域管理' }
      },
      // 用户中心
      {
        path: 'system/users',
        name: 'UserManagement',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { title: '用户管理', permissions: ['user:read'] }
      },
      {
        path: 'system/roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/RoleManagement.vue'),
        meta: { title: '角色管理', permissions: ['role:read'] }
      },
      {
        path: 'system/permissions',
        name: 'PermissionManagement',
        component: () => import('@/views/system/PermissionManagement.vue'),
        meta: { title: '权限管理', permissions: ['permission:read'] }
      },
      {
        path: 'system/storage',
        name: 'StorageBackend',
        component: () => import('@/views/storage/Index.vue'),
        meta: { title: '存储配置', permissions: ['storage:read'] }
      },
      {
        path: 'system/storage/create',
        name: 'StorageBackendCreate',
        component: () => import('@/views/storage/Create.vue'),
        meta: { title: '新增存储后端' }
      },
      {
        path: 'system/storage/:id/edit',
        name: 'StorageBackendEdit',
        component: () => import('@/views/storage/Edit.vue'),
        meta: { title: '编辑存储后端' }
      },
      {
        path: 'system/storage/:id',
        name: 'StorageBackendDetail',
        component: () => import('@/views/storage/Detail.vue'),
        meta: { title: '存储后端详情' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'system/settings',
        name: 'Settings',
        component: () => import('@/views/settings/Index.vue'),
        meta: { title: '系统设置' }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/Index.vue'),
        meta: { title: '使用统计' }
      },
      {
        path: 'tracking/relationship-graph',
        name: 'RelationshipGraph',
        component: () => import('@/views/tracking/RelationshipGraph.vue'),
        meta: { title: '系统软件关联图' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    next()
    return
  }

  if (!authStore.isAuthenticated) {
    next('/login')
    return
  }

  // Check role permissions if specified
  if (to.meta.roles && to.meta.roles.length > 0) {
    const hasRequiredRole = to.meta.roles.some((role: string) => authStore.hasRole(role))
    if (!hasRequiredRole) {
      next('/home')
      return
    }
  }

  // Check permission if specified
  if (to.meta.permissions && to.meta.permissions.length > 0) {
    const hasRequiredPermission = to.meta.permissions.some((perm: string) => authStore.hasPermission(perm))
    if (!hasRequiredPermission) {
      next('/home')
      return
    }
  }

  next()
})

export default router
