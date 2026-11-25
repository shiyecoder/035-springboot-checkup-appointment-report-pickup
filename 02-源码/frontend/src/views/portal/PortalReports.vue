<template>
  <div>
    <div class="panel">
      <div class="panel-h">
        <h3>我的报告</h3>
        <span class="sub">报告文件不暴露真实路径，在线下载需二次身份校验并记录访问日志</span>
      </div>

      <div class="notice">
        隐私提示：为保护个人健康信息，在线下载前需再次输入身份证后 6 位。每次下载都会记录时间、操作人与访问来源。
      </div>

      <div v-for="r in reports" :key="r.id" class="rp">
        <div class="ic">PDF</div>
        <div class="info">
          <h4>{{ r.fileName || '体检报告' }}</h4>
          <p class="muted">
            签发日期 <span class="num">{{ r.issueDate }}</span> · 第 {{ r.versionNo }} 版
            <el-tag v-if="r.isLatest" size="small" type="success" style="margin-left: 6px">最新有效</el-tag>
          </p>
        </div>
        <el-button type="primary" size="small" @click="openVerify(r)">身份校验并下载</el-button>
      </div>
      <div v-if="!reports.length" class="empty-tip">暂无可下载的报告。报告一般在体检后 3 个工作日内出具。</div>
    </div>

    <div class="panel">
      <div class="panel-h"><h3>领取记录</h3><span class="sub">含本人下载、现场领取与委托代领</span></div>
      <el-table :data="pickups" border size="small">
        <el-table-column prop="appId" label="预约 ID" width="100" align="right" />
        <el-table-column label="领取方式" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.pickupType === 'ENTRUST' ? 'warning' : 'success'">
              {{ PICKUP_TYPE[row.pickupType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pickerName" label="领取人" width="120" />
        <el-table-column prop="relation" label="关系" width="100" />
        <el-table-column label="领取时间" min-width="170">
          <template #default="{ row }"><span class="num">{{ formatTime(row.pickupTime) }}</span></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" />
        <template #empty><div class="empty-tip">暂无领取记录。</div></template>
      </el-table>
    </div>

    <el-dialog v-model="verifyVisible" title="身份二次校验" width="440px">
      <div class="muted" style="margin-bottom: 12px">请输入体检人身份证后 6 位以完成校验</div>
      <el-input v-model="idCardTail" maxlength="6" placeholder="身份证后 6 位" />
      <template #footer>
        <el-button @click="verifyVisible = false">取消</el-button>
        <el-button type="primary" @click="doDownload">校验并下载</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { PICKUP_TYPE, formatTime } from '../../utils/format'

const reports = ref([])
const pickups = ref([])
const verifyVisible = ref(false)
const idCardTail = ref('')
const cur = ref(null)

async function load() {
  const d = await request.get('/appointment/page', { params: { pageNum: 1, pageSize: 50 } })
  const all = d.records || []
  const list = []
  for (const a of all) {
    if (!['READY', 'PICKED', 'PENDING'].includes(a.reportStatus)) continue
    if (a.reportStatus === 'PENDING') continue
    const detail = await request.get(`/appointment/${a.id}`)
    ;(detail.reports || []).forEach((r) => list.push({ ...r, appNo: a.appNo }))
  }
  reports.value = list
  const p = await request.get('/pickup/page', { params: { pageNum: 1, pageSize: 20 } })
  pickups.value = p.records || []
}

function openVerify(r) {
  cur.value = r
  idCardTail.value = ''
  verifyVisible.value = true
}

async function doDownload() {
  if (idCardTail.value.length < 4) return ElMessage.warning('请输入身份证后 6 位')
  const res = await fetch(`/api/report/${cur.value.id}/download?idCardTail=${encodeURIComponent(idCardTail.value)}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
  })
  if (!res.ok) {
    const txt = await res.text()
    let msg = '身份校验未通过'
    try { msg = JSON.parse(txt).msg || msg } catch (e) { /* 保留默认提示 */ }
    return ElMessage.error(msg)
  }
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = cur.value.fileName || 'report.pdf'
  a.click()
  ElMessage.success('下载成功，本次访问已记录审计日志')
  verifyVisible.value = false
}

onMounted(load)
</script>

<style scoped>
.notice {
  background: #FBF4E6; border: 1px solid #EBD3A8; border-radius: 6px; padding: 12px 16px;
  font-size: 12.5px; color: #7A5416; line-height: 1.8; margin-bottom: 12px;
}
.rp { display: flex; align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid #EDF2F4; }
.rp .ic {
  width: 40px; height: 48px; border: 1px solid #BBD3DC; border-radius: 4px;
  background: #EEF5F7; display: grid; place-items: center; color: var(--accent); font-size: 11px;
}
.rp .info { flex: 1; }
.rp h4 { font-size: 14px; margin: 0; }
.rp p { font-size: 12px; margin: 4px 0 0; }
</style>
