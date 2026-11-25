<template>
  <div class="layout">
    <aside class="side">
      <div class="brand">
        <span class="dot"></span>
        <span>{{ roleName }}</span>
      </div>
      <nav>
        <template v-for="g in groups" :key="g.id">
          <div v-if="g.children.length" class="grp">{{ g.menuName }}</div>
          <router-link
            v-for="c in (g.children.length ? g.children : [g])"
            :key="c.id"
            :to="resolvePath(c.path)"
            :class="['item', { on: isActive(c.path) }]"
          >
            <span class="ico">{{ c.icon }}</span>{{ c.menuName }}
          </router-link>
        </template>
      </nav>
      <div class="foot">康检通体检中心 · v1.0</div>
    </aside>

    <div class="main">
      <header class="top">
        <div class="crumb">
          {{ crumbGroup }} / <b>{{ route.meta.title || '工作台' }}</b>
        </div>
        <div class="right">
          <el-badge :value="unread" :hidden="!unread" class="bell">
            <span class="ico-btn" @click="$router.push('/message')">✉</span>
          </el-badge>
          <span class="who">{{ user.realName }}</span>
          <el-dropdown @command="onCommand">
            <img class="avatar" :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <section class="content">
        <router-view />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'

const route = useRoute()
const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const menus = ref([])
const unread = ref(0)

const ROLE_NAME = { ADMIN: '体检管理员', FRONT: '前台工作台', ENTERPRISE: '企业团检服务' }
const roleName = computed(() => ROLE_NAME[user.value.roleCode] || '工作台')

/** 后端菜单路径 → 前端已注册路由（前台/团检入口复用业务列表页） */
const PATH_MAP = {
  '/front/checkin': '/appointment?tab=checkin',
  '/front/payment': '/appointment?tab=payment',
  '/front/audit': '/appointment?tab=audit',
  '/enterprise/batch': '/group',
  '/enterprise/member': '/group?tab=member'
}

function resolvePath(p) {
  return PATH_MAP[p] || p || '/'
}

function isActive(p) {
  const target = (PATH_MAP[p] || p || '').split('?')[0]
  return route.path === target
}

/** 后端返回的是扁平菜单（parent_id 描述层级），这里按路由可用性拼成「目录 + 子项」 */
const KNOWN = ['/dashboard', '/package', '/item', '/slot', '/appointment', '/group',
  '/report', '/pickup', '/crowd-rule', '/crowd-score', '/system/menu', '/system/role',
  '/system/user', '/message', '/profile']

function usablePath(p) {
  return p && (KNOWN.includes(p) || PATH_MAP[p])
}

const groups = computed(() => {
  const roots = menus.value.filter((m) => !m.parentId || m.parentId === 0)
  const out = []
  for (const r of roots) {
    const children = menus.value
      .filter((m) => m.parentId === r.id)
      .filter((m) => usablePath(m.path))
      .sort((a, b) => (a.sort || 0) - (b.sort || 0))
    if (children.length) {
      out.push({ ...r, children })
    } else if (usablePath(r.path)) {
      // 没有子项的一级菜单（如门户端的「我的报告」）直接当一个入口渲染
      out.push({ ...r, children: [] })
    }
  }
  return out
})

const crumbGroup = computed(() => {
  const hit = groups.value.find((g) =>
    g.children.length ? g.children.some((c) => isActive(c.path)) : isActive(g.path))
  return hit ? hit.menuName : '工作台'
})

async function loadMe() {
  const data = await request.get('/auth/me')
  user.value = data.user
  localStorage.setItem('user', JSON.stringify(data.user))
  localStorage.setItem('roleCode', data.user.roleCode)
  menus.value = data.menus || []
}

async function loadUnread() {
  try {
    unread.value = await request.get('/message/unread')
  } catch (e) {
    unread.value = 0
  }
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
    return
  }
  router.push('/profile')
}

onMounted(() => {
  loadMe().catch(() => {})
  loadUnread()
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.side {
  width: 220px; flex: none; background: var(--primary); color: #c9d8e0;
  display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh;
}
.brand {
  height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1); color: #fff; font-weight: 600; font-size: 15px;
}
.brand .dot { width: 8px; height: 8px; border-radius: 50%; background: #4FB3C4; }
nav { padding: 8px 0; flex: 1; overflow-y: auto; }
.grp { padding: 14px 18px 6px; font-size: 11px; letter-spacing: 0.08em; color: #7d94a1; }
.item {
  display: flex; align-items: center; gap: 10px; height: 40px; padding: 0 18px;
  color: #c9d8e0; font-size: 13.5px; text-decoration: none;
}
.item:hover { background: rgba(255, 255, 255, 0.06); color: #fff; }
.item.on { background: var(--secondary); color: #fff; border-left: 3px solid #7FD4E3; padding-left: 15px; }
.ico { width: 16px; text-align: center; opacity: 0.9; }
.foot { padding: 12px 18px; font-size: 11px; color: #6b8491; border-top: 1px solid rgba(255,255,255,.08); }
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.top {
  height: 56px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; padding: 0 24px; position: sticky; top: 0; z-index: 10;
}
.crumb { font-size: 13px; color: var(--text-2); }
.crumb b { color: var(--text); font-weight: 500; }
.top .right { margin-left: auto; display: flex; align-items: center; gap: 16px; font-size: 13px; color: var(--text-2); }
.ico-btn { cursor: pointer; font-size: 16px; }
.who { color: var(--text); }
.avatar { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; cursor: pointer; border: 1px solid var(--line); }
.content { flex: 1; background: var(--bg); }
</style>
