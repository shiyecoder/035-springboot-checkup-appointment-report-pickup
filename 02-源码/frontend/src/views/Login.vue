<template>
  <div class="login-page">
    <div class="bg-layer"></div>
    <div class="card">
      <section class="left">
        <div class="logo"><span class="d">体</span> 康检通 · 体检中心业务平台</div>
        <h1>预约、到检、报告<br />一条线管到底</h1>
        <p>套餐展示 · 时段容量 · 个人与团检预约<br />线下缴费 · 到检执行 · 报告归档与领取</p>
        <div class="kv">
          <div><b>28</b>检查项目</div>
          <div><b>16</b>在架套餐</div>
          <div><b>4</b>角色分权</div>
        </div>
        <svg class="ecg" viewBox="0 0 500 120" preserveAspectRatio="none">
          <path d="M0 84h120l14-3 10-38 12 66 14-42 10 17h86l16-3 10-30 12 54 13-34 12 13h181" />
        </svg>
      </section>

      <section class="right">
        <h2>{{ mode === 'login' ? '登录工作台' : '注册体检账号' }}</h2>
        <div class="sub">{{ mode === 'login' ? '请使用院方分配的账号，或使用手机号注册后预约体检' : '注册后可在线选择套餐、预约时段并领取报告' }}</div>

        <div class="tabs">
          <span :class="{ on: mode === 'login' }" @click="mode = 'login'">账号登录</span>
          <span :class="{ on: mode === 'register' }" @click="mode = 'register'">注册账号</span>
        </div>

        <el-form v-if="mode === 'login'" :model="form" @submit.prevent>
          <el-input v-model="form.username" size="large" placeholder="请输入账号" :prefix-icon="User" />
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码"
                    :prefix-icon="Lock" @keyup.enter="doLogin" />
          <div class="row">
            <el-checkbox v-model="remember">记住账号</el-checkbox>
            <span class="link">忘记密码？请联系前台 021-64880000</span>
          </div>
          <el-button class="submit" type="primary" :loading="loading" @click="doLogin">登 录</el-button>
        </el-form>

        <el-form v-else :model="reg" @submit.prevent>
          <el-input v-model="reg.username" size="large" placeholder="账号（至少 3 位）" :prefix-icon="User" />
          <el-input v-model="reg.realName" size="large" placeholder="真实姓名" />
          <el-input v-model="reg.phone" size="large" placeholder="手机号" />
          <el-input v-model="reg.password" size="large" type="password" show-password placeholder="密码（至少 6 位）"
                    :prefix-icon="Lock" @keyup.enter="doRegister" />
          <el-button class="submit" type="primary" :loading="loading" @click="doRegister">注 册 并 登 录</el-button>
        </el-form>

        <div class="hint">
          演示账号：管理员 admin / admin123<br />
          前台 front01 / 123456 · 企业联络员 ent01 / 123456 · 体检用户 user01 / 123456
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
/** 各角色的首页：患者去门户、联络员去团检、前台去执行台、管理员去看板 */
const LANDING = {
  USER: '/portal/packages',
  ENTERPRISE: '/group',
  FRONT: '/appointment?tab=checkin',
  ADMIN: '/dashboard'
}
const mode = ref('login')
const loading = ref(false)
const remember = ref(true)
const form = reactive({ username: localStorage.getItem('lastUser') || 'admin', password: '' })
const reg = reactive({ username: '', realName: '', phone: '', password: '' })

function afterLogin(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  localStorage.setItem('roleCode', data.user.roleCode)
  localStorage.setItem('lastUser', data.user.username)
  ElMessage.success(`欢迎回来，${data.user.realName}`)
  router.push(LANDING[data.user.roleCode] || '/dashboard')
}

async function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    const data = await request.post('/auth/login', form)
    afterLogin(data)
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!reg.username || !reg.password || !reg.realName) return ElMessage.warning('请填写完整信息')
  loading.value = true
  try {
    await request.post('/auth/register', reg)
    const data = await request.post('/auth/login', { username: reg.username, password: reg.password })
    afterLogin(data)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: grid; place-items: center; position: relative; overflow: hidden;
  background: #EAF1F3;
}
/* 主题背景图：本地生成的体检场景，断网可用 */
.bg-layer {
  position: absolute; inset: 0; background: url('/uploads/seed/bg/login.jpg') center/cover no-repeat;
  filter: saturate(0.85);
}
.bg-layer::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(120deg, rgba(21, 52, 74, 0.72), rgba(35, 122, 139, 0.42) 55%, rgba(243, 247, 248, 0.18));
}
.card {
  position: relative; z-index: 2; width: 980px; max-width: 94vw; height: 566px;
  display: grid; grid-template-columns: 1.05fr 0.95fr; background: #fff; border-radius: 10px;
  overflow: hidden; box-shadow: 0 22px 60px rgba(10, 32, 46, 0.35);
}
.left {
  position: relative; padding: 44px 42px; color: #fff; display: flex; flex-direction: column;
  background: linear-gradient(155deg, #15344A, #1E4A61 55%, #237A8B);
}
.logo { display: flex; align-items: center; gap: 10px; font-size: 15px; }
.logo .d {
  width: 22px; height: 22px; border: 2px solid #7FD4E3; border-radius: 6px;
  display: grid; place-items: center; font-size: 12px;
}
.left h1 { font-size: 28px; line-height: 1.35; margin: auto 0 0; font-weight: 600; letter-spacing: 0.5px; }
.left p { color: #B6CFD9; font-size: 13px; margin: 14px 0 0; line-height: 1.9; }
.kv { display: flex; gap: 28px; margin-top: 28px; }
.kv div { font-size: 12px; color: #9FBCC8; }
.kv b { display: block; font-size: 20px; color: #fff; font-family: "SF Mono", monospace; margin-bottom: 2px; }
.ecg { position: absolute; left: 0; right: 0; bottom: 0; height: 118px; opacity: 0.36; }
.ecg path { stroke: #7FD4E3; stroke-width: 1.6; fill: none; }
.right { padding: 50px 56px; display: flex; flex-direction: column; justify-content: center; }
.right h2 { font-size: 20px; margin: 0; color: var(--primary); }
.sub { font-size: 12.5px; color: var(--text-3); margin: 6px 0 22px; line-height: 1.7; }
.tabs { display: flex; gap: 24px; border-bottom: 1px solid var(--line); margin-bottom: 20px; }
.tabs span { padding-bottom: 10px; font-size: 14px; color: var(--text-2); cursor: pointer; }
.tabs span.on { color: var(--secondary); font-weight: 500; border-bottom: 2px solid var(--secondary); }
.el-input { margin-bottom: 14px; }
.row { display: flex; align-items: center; justify-content: space-between; font-size: 12.5px; margin-bottom: 18px; }
.link { color: var(--text-3); }
.submit { width: 100%; height: 42px; font-size: 14px; letter-spacing: 4px; }
.hint {
  margin-top: 18px; font-size: 12px; color: var(--text-3); line-height: 1.9;
  background: #F7FAFB; border: 1px dashed var(--line); border-radius: 6px; padding: 10px 12px;
}
</style>
