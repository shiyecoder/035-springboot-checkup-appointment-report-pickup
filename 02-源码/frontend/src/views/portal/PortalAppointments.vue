<template>
  <div>
    <div class="tabs">
      <span v-for="t in tabs" :key="t.value" class="tab" :class="{ on: status === t.value }" @click="switchTab(t.value)">
        {{ t.label }}
      </span>
    </div>

    <div class="panel">
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="appNo" label="预约编号" min-width="170">
          <template #default="{ row }">
            <el-button link type="primary" class="num" @click="detail(row)">{{ row.appNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="packageName" label="套餐" min-width="180" />
        <el-table-column label="预约日期 / 时段" min-width="170">
          <template #default="{ row }">
            <div class="num">{{ row.bookingDate }}</div>
            <div class="muted num">{{ row.timeRange }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报告" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.reportStatus === 'READY' ? 'warning' : row.reportStatus === 'PICKED' ? 'success' : 'info'">
              {{ REPORT[row.reportStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实收" width="110" align="right">
          <template #default="{ row }"><span class="num">{{ money(row.amountPaid) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="detail(row)">详情</el-button>
            <el-button v-if="['PENDING_CONFIRM','WAIT_PAY'].includes(row.status)" link type="warning"
                       @click="cancel(row)">取消</el-button>
            <el-button v-if="row.reportStatus === 'READY'" link type="success"
                       @click="$router.push('/portal/reports')">查看报告</el-button>
          </template>
        </el-table-column>
        <template #empty><div class="empty-tip">暂无预约记录，可先到「体检套餐」选择套餐预约。</div></template>
      </el-table>
      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="pageNum" :page-size="10" :total="total"
                       layout="prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'
import { formatTime, money, statusText, statusType } from '../../utils/format'

const tabs = [
  { label: '全部', value: '' },
  { label: '待到检', value: 'WAIT_CHECKIN' },
  { label: '体检中', value: 'CHECKING' },
  { label: '待报告', value: 'WAIT_REPORT' },
  { label: '待领取', value: 'WAIT_PICKUP' },
  { label: '已完成', value: 'COMPLETED' }
]
const REPORT = { NONE: '未生成', PENDING: '出具中', READY: '待领取', PICKED: '已领取' }
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const status = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const d = await request.get('/appointment/page', {
      params: { pageNum: pageNum.value, pageSize: 10, status: status.value }
    })
    rows.value = d.records || []
    total.value = d.total
  } finally {
    loading.value = false
  }
}

function switchTab(v) {
  status.value = v
  pageNum.value = 1
  load()
}

function detail(row) {
  ElMessageBox.alert(
    `<div style="line-height:2;font-size:13px">
      <div>套餐：${row.packageName}</div>
      <div>体检日期：${row.bookingDate} ${row.timeRange}</div>
      <div>当前状态：${statusText(row.status)}</div>
      <div>报告状态：${REPORT[row.reportStatus]}</div>
      <div>应收 / 实收：${money(row.amountDue)} / ${money(row.amountPaid)}</div>
      <div>创建时间：${formatTime(row.createTime)}</div>
      ${row.remark ? `<div>备注：${row.remark}</div>` : ''}
    </div>`,
    '预约详情',
    { dangerouslyUseHTMLString: true }
  )
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('取消预约需填写原因（不少于 5 个字），名额将立即释放', '取消预约', {
    inputPlaceholder: '如：临时出差无法到检'
  })
  await request.post(`/appointment/${row.id}/transition`, { target: 'CANCELLED', reason: value })
  ElMessage.success('已取消，名额已释放')
  load()
}

onMounted(load)
</script>

<style scoped>
.tabs { display: flex; gap: 6px; margin-bottom: 14px; flex-wrap: wrap; }
.tab {
  height: 32px; padding: 0 14px; border: 1px solid var(--line); background: #fff;
  border-radius: 6px; display: flex; align-items: center; font-size: 13px; color: var(--text-2); cursor: pointer;
}
.tab.on { background: var(--primary); border-color: var(--primary); color: #fff; }
.pager { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding-top: 14px; }
</style>
