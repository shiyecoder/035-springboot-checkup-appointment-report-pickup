<template>
  <div class="portal">
    <header class="portal-top">
      <div class="logo"><span class="mark"></span> 康检通体检中心</div>
      <nav class="nav">
        <router-link to="/portal/packages" :class="{ on: $route.path === '/portal/packages' }">体检套餐</router-link>
        <router-link to="/portal/appointments" :class="{ on: $route.path === '/portal/appointments' }">我的预约</router-link>
        <router-link to="/portal/reports" :class="{ on: $route.path === '/portal/reports' }">我的报告</router-link>
      </nav>
      <div class="right">
        <el-badge :value="unread" :hidden="!unread">
          <span class="bell" @click="$router.push('/message')">✉</span>
        </el-badge>
        <span class="name">{{ user.realName }}</span>
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
    <main class="portal-wrap">
      <router-view />
    </main>
    <footer class="portal-foot">康检通体检中心 · 体检预约与报告领取管理系统 · 工作时间 07:30-16:30</footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const unread = ref(0)

function onCommand(cmd) {
  if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
    return
  }
  router.push('/profile')
}

onMounted(async () => {
  try {
    const d = await request.get('/auth/me')
    user.value = d.user
    localStorage.setItem('user', JSON.stringify(d.user))
    unread.value = await request.get('/message/unread')
  } catch (e) { /* 忽略：布局不阻塞页面 */ }
})
</script>

<style scoped>
.portal { min-height: 100vh; display: flex; flex-direction: column; background: var(--bg); }
.portal-top {
  height: 64px; background: #fff; border-bottom: 1px solid var(--line);
  display: flex; align-items: center; padding: 0 32px; gap: 32px; position: sticky; top: 0; z-index: 10;
}
.logo { font-size: 16px; font-weight: 600; color: var(--primary); display: flex; align-items: center; gap: 9px; }
.mark { width: 18px; height: 18px; border: 2px solid var(--secondary); border-radius: 4px; display: inline-block; }
.nav { display: flex; gap: 24px; font-size: 13.5px; }
.nav a { color: var(--text-2); text-decoration: none; padding-bottom: 4px; }
.nav a.on { color: var(--secondary); font-weight: 500; border-bottom: 2px solid var(--secondary); }
.right { margin-left: auto; display: flex; align-items: center; gap: 14px; font-size: 13px; color: var(--text-2); }
.bell { cursor: pointer; font-size: 16px; }
.avatar { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; cursor: pointer; border: 1px solid var(--line); }
.portal-wrap { width: 100%; max-width: 1180px; margin: 0 auto; padding: 24px 32px 48px; flex: 1; }
.portal-foot { text-align: center; font-size: 12px; color: var(--text-3); padding: 20px 0 28px; }
</style>
