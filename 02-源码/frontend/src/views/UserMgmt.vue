<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="用户管理"
      subtitle="每类角色至少两个账号，用于验证本人 / 本部门 / 跨部门 / 管理员四种数据范围"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="120"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="账号 / 姓名" clearable />
        <el-select v-model="query.roleCode" placeholder="全部角色" clearable>
          <el-option label="体检管理员" value="ADMIN" />
          <el-option label="前台工作人员" value="FRONT" />
          <el-option label="企业联络员" value="ENTERPRISE" />
          <el-option label="体检用户" value="USER" />
        </el-select>
      </template>
      <template #avatar="{ row }">
        <img :src="row.avatar || '/uploads/seed/avatar/01.jpg'" class="av" alt="头像" />
      </template>
      <template #roleCode="{ row }">
        <el-tag size="small" :type="row.roleCode === 'ADMIN' ? 'danger' : row.roleCode === 'FRONT' ? 'warning' : row.roleCode === 'ENTERPRISE' ? 'primary' : 'info'">
          {{ ROLE[row.roleCode] }}
        </el-tag>
      </template>
      <template #status="{ row }">
        <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '停用' }}</el-tag>
      </template>
      <template #createTime="{ row }"><span class="num">{{ formatTime(row.createTime) }}</span></template>
      <template #lastLoginTime="{ row }"><span class="num">{{ formatTime(row.lastLoginTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">
          {{ row.status === 1 ? '停用' : '启用' }}
        </el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { formatTime } from '../utils/format'

const tableRef = ref(null)
const query = reactive({ keyword: '', roleCode: '' })
const ROLE = { ADMIN: '体检管理员', FRONT: '前台', ENTERPRISE: '企业联络员', USER: '体检用户' }
const columns = [
  { prop: 'avatar', label: '头像', width: 70 },
  { prop: 'username', label: '账号', minWidth: 110, num: true },
  { prop: 'realName', label: '姓名', width: 100 },
  { prop: 'phone', label: '手机号', width: 140, num: true },
  { prop: 'roleCode', label: '角色', width: 120 },
  { prop: 'dept', label: '科室/部门', width: 120 },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'lastLoginTime', label: '最近登录', minWidth: 160 },
  { prop: 'createTime', label: '创建时间', minWidth: 160 }
]

function fetchPage(params) {
  return request.get('/system/user/page', { params: { ...params, ...query } })
}

async function toggle(row) {
  await request.put(`/system/user/${row.id}/status`, { status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态已更新')
  tableRef.value.load()
}
</script>

<style scoped>
.av { width: 30px; height: 30px; border-radius: 50%; object-fit: cover; }
</style>
