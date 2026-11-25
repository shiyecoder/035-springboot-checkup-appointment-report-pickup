<template>
  <div class="page">
    <div class="cols">
      <div class="panel">
        <div class="panel-h"><h3>个人资料</h3><span class="sub">头像上传后同步到右上角</span></div>
        <div class="avatar-row">
          <img class="avatar" :src="form.avatar || '/uploads/seed/avatar/01.jpg'" alt="头像" />
          <div>
            <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :query="{ bizType: 'avatar' }"
                       :show-file-list="false" :on-success="onAvatarOk" accept="image/*">
              <el-button>更换头像</el-button>
            </el-upload>
            <div class="muted mt8">支持 jpg / png，建议 400×400 以上</div>
          </div>
        </div>
        <el-form :model="form" label-width="90px" class="mt16">
          <el-form-item label="账号"><el-input v-model="form.username" disabled /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="角色">
            <el-tag>{{ ROLE_TEXT[form.roleCode] || form.roleCode }}</el-tag>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="save">保存资料</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-h"><h3>修改密码</h3><span class="sub">密码使用 BCrypt 加密存储</span></div>
        <el-form :model="pwd" label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button type="primary" @click="changePwd">修改密码</el-button>
          </el-form-item>
        </el-form>
        <div class="muted mt16" style="line-height: 1.9">
          安全提示：连续登录失败 5 次将锁定 10 分钟；任何接口均不返回密码字段。
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const uploadUrl = '/api/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }
const ROLE_TEXT = { ADMIN: '体检管理员', FRONT: '前台工作人员', ENTERPRISE: '企业联络员', USER: '体检用户' }
const form = reactive({ username: '', realName: '', phone: '', avatar: '', roleCode: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function onAvatarOk(res) {
  if (res.code === 200) {
    form.avatar = res.data.url
    ElMessage.success('头像已上传，请保存')
  }
}

async function load() {
  const d = await request.get('/auth/me')
  Object.assign(form, d.user)
}

async function save() {
  await request.put('/auth/profile', { realName: form.realName, phone: form.phone, avatar: form.avatar })
  const d = await request.get('/auth/me')
  localStorage.setItem('user', JSON.stringify(d.user))
  ElMessage.success('资料已保存')
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) return ElMessage.warning('请填写密码')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await request.put('/auth/password', pwd)
  ElMessage.success('密码已修改，请重新登录')
  localStorage.removeItem('token')
  location.href = '/login'
}

onMounted(load)
</script>

<style scoped>
.cols { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.avatar-row { display: flex; gap: 18px; align-items: center; }
.avatar { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; border: 1px solid var(--line); }
</style>
