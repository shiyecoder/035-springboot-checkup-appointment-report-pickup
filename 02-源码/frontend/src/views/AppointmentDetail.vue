<template>
  <div class="page" v-loading="loading">
    <div class="panel head" v-if="app.id">
      <div class="l1">
        <b class="num">{{ app.appNo }}</b>
        <el-tag :type="statusType(app.status)" size="small">{{ statusText(app.status) }}</el-tag>
        <el-tag size="small" type="info">{{ app.batchId ? '团体预约' : '个人预约' }}</el-tag>
        <div class="right actions">
          <el-button v-if="app.status === 'PENDING_CONFIRM'" size="small" type="primary" @click="transition('WAIT_PAY')">审核通过</el-button>
          <el-button v-if="['PENDING_CONFIRM','WAIT_PAY'].includes(app.status)" size="small" @click="payVisible = true">收费登记</el-button>
          <el-button v-if="app.status === 'WAIT_CHECKIN'" size="small" type="primary" @click="transition('CHECKING')">核验签到</el-button>
          <el-button v-if="app.status === 'CHECKING'" size="small" type="primary" @click="transition('WAIT_REPORT')">结束检查</el-button>
          <el-button v-if="app.status === 'WAIT_REPORT'" size="small" type="primary" @click="reportVisible = true">上传报告</el-button>
          <el-button v-if="app.status === 'WAIT_PICKUP'" size="small" type="primary" @click="pickupVisible = true">登记领取</el-button>
          <el-button v-if="['WAIT_CHECKIN','WAIT_PAY'].includes(app.status)" size="small" @click="rescheduleVisible = true">改期</el-button>
          <el-button v-if="['PENDING_CONFIRM','WAIT_PAY','WAIT_CHECKIN'].includes(app.status)" size="small" type="danger" plain @click="cancel">取消预约</el-button>
        </div>
      </div>
      <div class="l2">
        <div>体检人<span>{{ app.contactName }}</span></div>
        <div>手机<span class="num">{{ app.contactPhone }}</span></div>
        <div>套餐<span>{{ app.packageName }}</span></div>
        <div>体检日期<span class="num">{{ app.bookingDate }} {{ app.timeRange }}</span></div>
        <div>应收<span class="num">{{ money(app.amountDue) }}</span></div>
        <div>实收<span class="num">{{ money(app.amountPaid) }}</span></div>
        <div>报告<span>{{ REPORT_TEXT[app.reportStatus] }}</span></div>
        <div>创建<span class="num">{{ formatTime(app.createTime) }}</span></div>
      </div>
    </div>

    <div class="cols">
      <div>
        <div class="panel">
          <div class="panel-h"><h3>项目执行进度</h3>
            <span class="sub">共 {{ progress.length }} 项 · 已检 {{ countBy('DONE') }} · 待检 {{ countBy('WAITING') }} · 需补检 {{ countBy('SUPPLEMENT') }}</span>
          </div>
          <div class="items">
            <div v-for="p in progress" :key="p.id" class="item-card">
              <div class="ih">
                <b>{{ p.itemName }}</b>
                <el-tag size="small" :type="PROGRESS_STATUS[p.status]?.type">{{ PROGRESS_STATUS[p.status]?.text }}</el-tag>
              </div>
              <div class="muted">{{ p.dept }}<span v-if="p.checkTime"> · {{ formatTime(p.checkTime) }}</span></div>
              <div v-if="p.remark" class="muted remark">{{ p.remark }}</div>
              <div v-if="p.supplementDate" class="muted">补检日期：{{ p.supplementDate }}</div>
              <div class="ops">
                <el-button v-if="app.status === 'CHECKING'" link type="primary" size="small" @click="mark(p, 'DONE')">标记已检</el-button>
                <el-dropdown v-if="app.status === 'CHECKING'" trigger="click" @command="(c) => mark(p, c)">
                  <el-button link type="primary" size="small">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="GIVEN_UP">登记放弃</el-dropdown-item>
                      <el-dropdown-item command="SUPPLEMENT">登记补检</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
            <div v-if="!progress.length" class="empty-tip">尚未签到，签到后自动生成项目清单。</div>
          </div>
        </div>

        <div class="panel mt16">
          <div class="panel-h"><h3>报告版本</h3><span class="sub">普通用户仅可见最新有效版</span></div>
          <el-table :data="reports" border size="small">
            <el-table-column prop="versionNo" label="版本" width="70" align="center" />
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="issueDate" label="签发日期" width="120" />
            <el-table-column prop="doctorName" label="总检医师" width="100" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="row.isLatest ? 'success' : 'info'">{{ row.isLatest ? '最新' : '历史' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="download(row)">下载</el-button>
              </template>
            </el-table-column>
            <template #empty><div class="empty-tip">报告尚未归档。</div></template>
          </el-table>
        </div>
      </div>

      <div>
        <div class="panel" v-if="crowd">
          <div class="panel-h"><h3>时段拥挤度</h3><span class="sub">规则 v{{ crowd.ruleVersion }}</span></div>
          <div class="score-line">
            <b class="num">{{ crowd.adjustedScore || crowd.totalScore }}</b>
            <el-tag :type="LEVEL[crowd.level]?.type" size="small">{{ LEVEL[crowd.level]?.text }}</el-tag>
            <span v-if="crowd.adjustedScore" class="muted">原计算 {{ crowd.totalScore }}（已人工调整）</span>
          </div>
          <div class="muted mt8">主要因素：{{ crowd.mainFactors }}</div>
          <div class="muted">未满足条件：{{ crowd.unmetConditions }}</div>
          <div class="muted">可改进项：{{ crowd.improveTip }}</div>
          <el-button class="mt8" size="small" @click="$router.push('/crowd-score')">查看计算留痕</el-button>
        </div>

        <div class="panel mt16">
          <div class="panel-h"><h3>操作时间轴</h3></div>
          <ul class="tl">
            <li v-for="(t, i) in timeline" :key="i">
              <div class="t">{{ t.time }}</div>
              <div class="b">{{ t.title }}</div>
              <div class="s">{{ t.desc }}</div>
            </li>
          </ul>
        </div>

        <div class="panel mt16">
          <div class="panel-h"><h3>缴费记录</h3></div>
          <div v-for="p in payments" :key="p.id" class="pay-line">
            <div>
              <div>应收 {{ money(p.receivable) }} · 减免 {{ money(p.discountAmount) }}</div>
              <div class="muted">实收 {{ money(p.received) }} · {{ PAY_METHOD[p.payMethod] }} · {{ formatTime(p.createTime) }}</div>
            </div>
            <el-image v-if="p.voucherImage" :src="p.voucherImage" :preview-src-list="[p.voucherImage]"
                      fit="cover" style="width: 46px; height: 34px; border-radius: 4px" preview-teleported />
          </div>
          <div v-if="!payments.length" class="empty-tip">暂无缴费记录。</div>
        </div>
      </div>
    </div>

    <!-- 收费登记 -->
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
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPay">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上传报告 -->
    <el-dialog v-model="reportVisible" title="上传体检报告" width="520px">
      <el-form :model="report" label-width="100px">
        <el-form-item label="报告文件" required>
          <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :query="{ bizType: 'report' }"
                     :show-file-list="false" accept=".pdf" :on-success="onReportOk">
            <el-button>选择 PDF 上传</el-button>
          </el-upload>
          <span v-if="report.filePath" class="muted" style="margin-left: 8px">{{ report.fileName }}</span>
        </el-form-item>
        <el-form-item label="签发日期">
          <el-date-picker v-model="report.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="总检医师"><el-input v-model="report.doctorName" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">归档并通知领取</el-button>
      </template>
    </el-dialog>

    <!-- 登记领取 -->
    <el-dialog v-model="pickupVisible" title="报告领取登记" width="520px">
      <el-form :model="pickup" label-width="100px">
        <el-form-item label="领取方式">
          <el-radio-group v-model="pickup.pickupType">
            <el-radio label="SELF">现场领取</el-radio>
            <el-radio label="ONLINE">本人下载</el-radio>
            <el-radio label="ENTRUST">委托代领</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="pickup.pickupType === 'ENTRUST'">
          <el-form-item label="领取人" required><el-input v-model="pickup.pickerName" /></el-form-item>
          <el-form-item label="与体检人关系">
            <el-select v-model="pickup.relation" style="width: 100%">
              <el-option label="配偶" value="配偶" /><el-option label="子女" value="子女" />
              <el-option label="父母" value="父母" /><el-option label="同事" value="同事" />
            </el-select>
          </el-form-item>
          <el-form-item label="授权书" required>
            <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :query="{ bizType: 'auth' }"
                       :show-file-list="false" :on-success="(r) => (pickup.authFile = r.data?.url)">
              <el-button>上传授权书</el-button>
            </el-upload>
            <span v-if="pickup.authFile" class="muted" style="margin-left: 8px">已上传</span>
          </el-form-item>
        </template>
        <el-form-item label="备注"><el-input v-model="pickup.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pickupVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPickup">确认领取</el-button>
      </template>
    </el-dialog>

    <!-- 改期 -->
    <el-dialog v-model="rescheduleVisible" title="改期" width="520px">
      <el-form label-width="100px">
        <el-form-item label="原时段"><span class="num">{{ app.bookingDate }} {{ app.timeRange }}</span></el-form-item>
        <el-form-item label="目标时段">
          <el-select v-model="newSlotId" style="width: 100%" placeholder="选择新的时段">
            <el-option v-for="s in slots" :key="s.id" :label="`${s.slotDate} ${s.timeRange}（余 ${s.remain}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="改期原因" required>
          <el-input v-model="reason" type="textarea" :rows="2" placeholder="不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rescheduleVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReschedule">确认改期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import {
  LEVEL, PAY_METHOD, PROGRESS_STATUS, formatTime, money, statusText, statusType
} from '../utils/format'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const app = ref({})
const progress = ref([])
const reports = ref([])
const payments = ref([])
const timeline = ref([])
const crowd = ref(null)
const slots = ref([])
const newSlotId = ref(null)
const reason = ref('')
const payVisible = ref(false)
const reportVisible = ref(false)
const pickupVisible = ref(false)
const rescheduleVisible = ref(false)
const uploadUrl = '/api/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }
const REPORT_TEXT = { NONE: '未生成', PENDING: '出具中', READY: '待领取', PICKED: '已领取' }

const pay = reactive({ receivable: 0, discountAmount: 0, received: 0, payMethod: 'WECHAT', voucherImage: '' })
const report = reactive({ filePath: '', fileName: '', issueDate: '', doctorName: '李国强' })
const pickup = reactive({ pickupType: 'SELF', pickerName: '', relation: '配偶', authFile: '', remark: '' })

function countBy(s) {
  return progress.value.filter((p) => p.status === s).length
}

async function load() {
  loading.value = true
  try {
    const d = await request.get(`/appointment/${route.params.id}`)
    app.value = d.appointment
    progress.value = d.progress || []
    reports.value = d.reports || []
    payments.value = d.payments || []
    timeline.value = d.timeline || []
    crowd.value = d.crowd
    pay.receivable = Number(app.value.amountDue || 0)
    pay.received = Number(app.value.amountDue || 0)
  } finally {
    loading.value = false
  }
}

async function transition(target) {
  let body = { target, reason: '前台按流程办理' }
  if (target === 'CANCELLED') {
    const { value } = await ElMessageBox.prompt('请填写取消原因（不少于 5 个字）', '取消预约', {
      inputPlaceholder: '如：体检人临时出差'
    })
    body = { target, reason: value }
  }
  await request.post(`/appointment/${app.value.id}/transition`, body)
  ElMessage.success('状态已更新')
  load()
}

async function cancel() {
  const { value } = await ElMessageBox.prompt('请填写取消原因（不少于 5 个字），名额将立即释放', '取消预约', {
    inputPlaceholder: '如：体检人临时出差'
  })
  await request.post(`/appointment/${app.value.id}/transition`, { target: 'CANCELLED', reason: value })
  ElMessage.success('已取消')
  load()
}

async function mark(p, status) {
  let remark = null
  let supplementDate = null
  if (status === 'GIVEN_UP') {
    const r = await ElMessageBox.prompt('放弃该检查项目需填写原因（不少于 5 个字）', '登记放弃', {
      inputPlaceholder: '如：体检人自愿放弃该项检查'
    })
    remark = r.value
  }
  if (status === 'SUPPLEMENT') {
    const r = await ElMessageBox.prompt('请填写补检日期（yyyy-MM-dd）', '登记补检', {
      inputPlaceholder: '2026-11-20'
    })
    supplementDate = r.value
    remark = '当日漏检，改约补检'
  }
  await request.put(`/appointment/progress/${p.id}`, { status, remark, supplementDate })
  ElMessage.success('已更新')
  load()
}

async function submitPay() {
  await request.post(`/appointment/${app.value.id}/payment`, pay)
  ElMessage.success('收费登记完成')
  payVisible.value = false
  load()
}

function onReportOk(res) {
  if (res.code === 200) {
    report.filePath = res.data.url
    report.fileName = res.data.fileName
    ElMessage.success('报告已上传')
  }
}

async function submitReport() {
  if (!report.filePath) return ElMessage.warning('请先上传报告文件')
  await request.post(`/report/${app.value.id}`, report)
  await request.post(`/appointment/${app.value.id}/transition`, { target: 'WAIT_PICKUP', reason: '报告已归档' })
  ElMessage.success('报告已归档，已通知体检人领取')
  reportVisible.value = false
  load()
}

async function submitPickup() {
  if (pickup.pickupType === 'ENTRUST' && (!pickup.pickerName || !pickup.authFile)) {
    return ElMessage.warning('委托代领需填写领取人并上传授权书')
  }
  await request.post(`/pickup/${app.value.id}`, pickup)
  ElMessage.success('领取登记完成')
  pickupVisible.value = false
  load()
}

async function submitReschedule() {
  if (!newSlotId.value) return ElMessage.warning('请选择目标时段')
  await request.post(`/appointment/${app.value.id}/reschedule`, { slotId: newSlotId.value, reason: reason.value })
  ElMessage.success('改期成功，原名额已释放')
  rescheduleVisible.value = false
  load()
}

async function download(row) {
  const { value } = await ElMessageBox.prompt('为保护隐私，请再次输入体检人身份证后 6 位', '身份校验', {
    inputPlaceholder: '身份证后 6 位'
  })
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/report/${row.id}/download?idCardTail=${encodeURIComponent(value)}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (!res.ok) {
    const msg = await res.text()
    return ElMessage.error('下载失败：' + msg.slice(0, 80))
  }
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName || 'report.pdf'
  a.click()
  URL.revokeObjectURL(url)
}

watch(rescheduleVisible, async (v) => {
  if (!v) return
  const today = new Date()
  const from = today.toISOString().slice(0, 10)
  const to = new Date(today.getTime() + 10 * 86400000).toISOString().slice(0, 10)
  const data = await request.get('/slot/page', { params: { pageNum: 1, pageSize: 50, dateFrom: from, dateTo: to } })
  slots.value = (data.records || []).filter((s) => s.status === 'OPEN').map((s) => ({
    id: s.id,
    slotDate: s.slotDate,
    timeRange: s.timeRange,
    remain: (s.personalQuota + s.groupQuota) - (s.personalBooked + s.groupBooked)
  }))
})

onMounted(load)
</script>

<style scoped>
.head .l1 { display: flex; align-items: center; gap: 10px; }
.head .l1 b { font-size: 16px; color: var(--primary); }
.head .actions { display: flex; gap: 8px; }
.head .l2 { display: flex; gap: 30px; flex-wrap: wrap; margin-top: 14px; font-size: 12.5px; color: var(--text-2); }
.head .l2 span { color: var(--text); margin-left: 6px; }
.cols { display: grid; grid-template-columns: 1.45fr 1fr; gap: 16px; margin-top: 16px; }
.items { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.item-card { border: 1px solid var(--line); border-radius: 6px; padding: 10px 12px; }
.item-card .ih { display: flex; align-items: center; justify-content: space-between; }
.item-card .ih b { font-size: 13px; }
.item-card .m { font-size: 12px; }
.item-card .remark { color: var(--warn); }
.item-card .ops { display: flex; gap: 4px; margin-top: 4px; }
.score-line { display: flex; align-items: baseline; gap: 10px; }
.score-line b { font-size: 30px; color: var(--primary); }
.pay-line { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #EDF2F4; font-size: 13px; }
</style>
