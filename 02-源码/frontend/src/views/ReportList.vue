<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="报告归档"
      subtitle="同一预约重复上传自动递增版本，用户仅可见最新有效版"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="140"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="预约编号" clearable />
      </template>
      <template #versionNo="{ row }">
        <el-tag size="small" :type="row.isLatest ? 'success' : 'info'">V{{ row.versionNo }}</el-tag>
      </template>
      <template #filePath="{ row }">
        <span class="muted">已存于服务器（不暴露真实路径）</span>
      </template>
      <template #createTime="{ row }"><span class="num">{{ formatTime(row.createTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="download(row)">下载</el-button>
        <el-button link type="primary" @click="goApp(row)">查看单据</el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { formatTime } from '../utils/format'

const router = useRouter()
const tableRef = ref(null)
const query = reactive({ keyword: '' })
const columns = [
  { prop: 'appId', label: '预约 ID', width: 90, align: 'right', num: true },
  { prop: 'versionNo', label: '版本', width: 80 },
  { prop: 'fileName', label: '文件名', minWidth: 220 },
  { prop: 'filePath', label: '存储', minWidth: 200 },
  { prop: 'issueDate', label: '签发日期', width: 120, num: true },
  { prop: 'doctorName', label: '总检医师', width: 110 },
  { prop: 'createTime', label: '归档时间', minWidth: 160 }
]

function fetchPage(params) {
  return request.get('/report/page', { params: { ...params, ...query } })
}

function goApp(row) {
  router.push(`/appointment/${row.appId}`)
}

async function download(row) {
  const { value } = await ElMessageBox.prompt('管理员下载报告同样需要二次校验，请输入体检人身份证后 6 位', '身份校验', {
    inputPlaceholder: '身份证后 6 位'
  })
  const res = await fetch(`/api/report/${row.id}/download?idCardTail=${encodeURIComponent(value)}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
  })
  if (!res.ok) return ElMessage.error('下载失败，请检查身份校验信息')
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = row.fileName || 'report.pdf'
  a.click()
}
</script>
