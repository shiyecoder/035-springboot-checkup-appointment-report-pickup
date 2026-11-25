<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="体检预约与报告"
      subtitle="按状态与体检日期筛选；待处理优先、更新时间倒序"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="230"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="预约编号 / 姓名 / 手机号" clearable />
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option v-for="(v, k) in APPOINTMENT_STATUS" :key="k" :label="v.text" :value="k" />
        </el-select>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="体检开始"
          end-placeholder="体检结束"
          style="width: 260px"
        />
      </template>

      <template #toolbar>
        <el-radio-group v-model="quick" size="small" @change="applyQuick">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="TODO">待我处理</el-radio-button>
          <el-radio-button label="PENDING_CONFIRM,WAIT_PAY">待审核缴费</el-radio-button>
          <el-radio-button label="WAIT_CHECKIN">待到检</el-radio-button>
          <el-radio-button label="COMPLETED">已完成</el-radio-button>
        </el-radio-group>
      </template>

      <template #appNo="{ row }">
        <el-button link type="primary" class="num" @click="go(row)">{{ row.appNo }}</el-button>
      </template>
      <template #status="{ row }">
        <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
      </template>
      <template #reportStatus="{ row }">
        <span>{{ REPORT_TEXT[row.reportStatus] || '-' }}</span>
      </template>
      <template #amountPaid="{ row }"><span class="num">{{ money(row.amountPaid) }}</span></template>
      <template #bookingDate="{ row }">
        <div class="num">{{ row.bookingDate }}</div>
        <div class="muted num">{{ row.timeRange }}</div>
      </template>
      <template #updateTime="{ row }"><span class="num">{{ formatTime(row.updateTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="go(row)">详情</el-button>
        <el-button v-if="row.status === 'PENDING_CONFIRM'" link type="success" @click="quickAudit(row)">审核</el-button>
        <el-button v-if="row.status === 'WAIT_CHECKIN'" link type="success" @click="checkin(row)">签到</el-button>
        <el-button v-if="['PENDING_CONFIRM','WAIT_PAY','WAIT_CHECKIN'].includes(row.status)" link type="warning" @click="cancel(row)">取消</el-button>
      </template>
    </DataTable>

    <el-dialog v-model="payVisible" title="收费登记" width="480px">
      <el-form :model="pay" label-width="100px">
        <el-form-item label="应收金额"><el-input-number v-model="pay.receivable" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="减免金额"><el-input-number v-model="pay.discountAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="实收金额"><el-input-number v-model="pay.received" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="支付方式">
          <el-select v-model="pay.payMethod" style="width: 100%">
            <el-option v-for="(v, k) in PAY_METHOD" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费凭证">
          <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :query="{ bizType: 'voucher' }"
                     :show-file-list="false" :on-success="(r) => (pay.voucherImage = r.data?.url)">
            <el-button>上传凭证</el-button>
          </el-upload>
          <span v-if="pay.voucherImage" class="muted" style="margin-left: 8px">已上传</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPay">保存并转待到检</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { APPOINTMENT_STATUS, PAY_METHOD, formatTime, money, statusText, statusType } from '../utils/format'

const router = useRouter()
const route = useRoute()
const tableRef = ref(null)
const query = reactive({ keyword: '', status: '', dateFrom: '', dateTo: '' })
const range = ref([])
const quick = ref('ALL')
const payVisible = ref(false)
const curApp = ref(null)
const uploadUrl = '/api/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }
const pay = reactive({ receivable: 0, discountAmount: 0, received: 0, payMethod: 'WECHAT', voucherImage: '' })

const REPORT_TEXT = { NONE: '未生成', PENDING: '出具中', READY: '待领取', PICKED: '已领取' }

const columns = [
  { prop: 'appNo', label: '预约编号', minWidth: 170 },
  { prop: 'contactName', label: '体检人', minWidth: 110 },
  { prop: 'packageName', label: '套餐', minWidth: 180 },
  { prop: 'bookingDate', label: '体检日期 / 时段', minWidth: 160 },
  { prop: 'status', label: '预约状态', width: 100 },
  { prop: 'reportStatus', label: '报告状态', width: 100 },
  { prop: 'amountPaid', label: '实收', width: 100, align: 'right' },
  { prop: 'updateTime', label: '更新时间', minWidth: 160 }
]

async function fetchPage(params) {
  query.dateFrom = range.value?.[0] || ''
  query.dateTo = range.value?.[1] || ''
  return request.get('/appointment/page', { params: { ...params, ...query } })
}

function applyQuick(v) {
  query.status = v === 'ALL' ? '' : v === 'TODO' ? 'PENDING_CONFIRM,WAIT_PAY,WAIT_CHECKIN,CHECKING,WAIT_REPORT' : v
  tableRef.value.search()
}

function go(row) {
  router.push(`/appointment/${row.id}`)
}

async function quickAudit(row) {
  await request.post(`/appointment/${row.id}/transition`, { target: 'WAIT_PAY', reason: '前台审核通过，等待缴费' })
  ElMessage.success('已审核通过，状态转为待缴费')
  tableRef.value.load()
}

function checkin(row) {
  curApp.value = row
  pay.receivable = Number(row.amountDue || 0)
  pay.discountAmount = 0
  pay.received = Number(row.amountDue || 0)
  pay.payMethod = 'WECHAT'
  pay.voucherImage = ''
  payVisible.value = true
}

async function submitPay() {
  await request.post(`/appointment/${curApp.value.id}/payment`, pay)
  await request.post(`/appointment/${curApp.value.id}/transition`, { target: 'WAIT_CHECKIN', reason: '已登记收费' })
  ElMessage.success('收费登记完成')
  payVisible.value = false
  tableRef.value.load()
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('取消预约需填写原因（不少于 5 个字），名额将立即释放。', '取消预约', {
    inputPlaceholder: '如：体检人临时出差', confirmButtonText: '确认取消', cancelButtonText: '返回'
  })
  await request.post(`/appointment/${row.id}/transition`, { target: 'CANCELLED', reason: value })
  ElMessage.success('已取消，名额已释放')
  tableRef.value.load()
}

onMounted(() => {
  const tab = route.query.tab
  if (tab === 'audit') {
    quick.value = 'PENDING_CONFIRM,WAIT_PAY'
    applyQuick(quick.value)
  } else if (tab === 'checkin') {
    quick.value = 'WAIT_CHECKIN'
    applyQuick(quick.value)
  } else if (tab === 'payment') {
    quick.value = 'PENDING_CONFIRM,WAIT_PAY'
    applyQuick(quick.value)
  }
})
</script>

<script>
export default { name: 'AppointmentList' }
</script>
