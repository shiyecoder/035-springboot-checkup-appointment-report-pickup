import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由：按角色分三套外壳
 * - 管理端（管理员/前台/联络员）：侧栏布局
 * - 门户端（体检用户）：顶部导航布局
 */
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue'), meta: { public: true } },

  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    redirect: () => (localStorage.getItem('roleCode') === 'USER' ? '/portal/packages' : '/dashboard'),
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '运营看板', roles: ['ADMIN'] } },
      { path: 'package', name: 'package', component: () => import('../views/PackageList.vue'), meta: { title: '体检套餐', roles: ['ADMIN'] } },
      { path: 'item', name: 'item', component: () => import('../views/ItemList.vue'), meta: { title: '检查项目', roles: ['ADMIN'] } },
      { path: 'slot', name: 'slot', component: () => import('../views/SlotList.vue'), meta: { title: '时段容量', roles: ['ADMIN'] } },
      { path: 'appointment', name: 'appointment', component: () => import('../views/AppointmentList.vue'), meta: { title: '预约管理' } },
      { path: 'appointment/:id', name: 'appointmentDetail', component: () => import('../views/AppointmentDetail.vue'), meta: { title: '预约详情' } },
      { path: 'group', name: 'group', component: () => import('../views/GroupList.vue'), meta: { title: '团检管理' } },
      { path: 'report', name: 'report', component: () => import('../views/ReportList.vue'), meta: { title: '报告归档' } },
      { path: 'pickup', name: 'pickup', component: () => import('../views/PickupList.vue'), meta: { title: '报告领取' } },
      { path: 'crowd-rule', name: 'crowdRule', component: () => import('../views/CrowdRule.vue'), meta: { title: '拥挤度规则', roles: ['ADMIN'] } },
      { path: 'crowd-score', name: 'crowdScore', component: () => import('../views/CrowdScore.vue'), meta: { title: '计算留痕', roles: ['ADMIN'] } },
      { path: 'system/menu', name: 'menuMgmt', component: () => import('../views/MenuMgmt.vue'), meta: { title: '菜单管理', roles: ['ADMIN'] } },
      { path: 'system/role', name: 'roleMgmt', component: () => import('../views/RoleMgmt.vue'), meta: { title: '角色管理', roles: ['ADMIN'] } },
      { path: 'system/user', name: 'userMgmt', component: () => import('../views/UserMgmt.vue'), meta: { title: '用户管理', roles: ['ADMIN'] } },
      { path: 'message', name: 'message', component: () => import('../views/Message.vue'), meta: { title: '消息中心' } },
      { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  },

  {
    path: '/portal',
    component: () => import('../layouts/PortalLayout.vue'),
    children: [
      { path: 'packages', name: 'portalPackages', component: () => import('../views/portal/PortalPackages.vue'), meta: { title: '体检套餐' } },
      { path: 'booking/:id', name: 'portalBooking', component: () => import('../views/portal/PortalBooking.vue'), meta: { title: '选择时段' } },
      { path: 'appointments', name: 'portalAppointments', component: () => import('../views/portal/PortalAppointments.vue'), meta: { title: '我的预约' } },
      { path: 'reports', name: 'portalReports', component: () => import('../views/portal/PortalReports.vue'), meta: { title: '我的报告' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

/** 各角色落地页：进错端时自动带回自己的一侧，避免看到无权页面 */
const LANDING = { USER: '/portal/packages', ENTERPRISE: '/group', FRONT: '/appointment?tab=checkin', ADMIN: '/dashboard' }

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('roleCode')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return LANDING[role] || '/'
  // 运营看板只给院内人员，患者/企业联络员一律带回自己的首页
  if ((role === 'USER' || role === 'ENTERPRISE') && to.path === '/dashboard') {
    return LANDING[role]
  }
  return true
})

export default router
