<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="团检批次"
      subtitle="企业联络员仅可查看到检与完成统计，不可下载个人报告"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="200"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="批次编号 / 企业" clearable />
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option label="待到检" value="PENDING" />
          <el-option label="体检中" value="CHECKING" />
          <el-option label="已完成" value="DONE" />
        </el-select>
      </template>
      <template #toolbar>
        <el-button type="primary" @click="createVisible = true">+ 新建批次</el-button>
      </template>
      <template #batchNo="{ row }"><span class="num">{{ row.batchNo }}</span></template>
      <template #dateRange="{ row }"><span class="num">{{ row.startDate }} ~ {{ row.endDate }}</span></template>
      <template #joined="{ row }"><span class="num">{{ row.joinedCount }} / {{ row.quota }}</span></template>
      <template #status="{ row }">
        <el-tag size="small" :type="BATCH_STATUS[row.status]?.type">{{ BATCH_STATUS[row.status]?.text }}</el-tag>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="openMembers(row)">名单</el-button>
        <el-button link type="primary" @click="openStats(row)">统计</el-button>
        <el-button link type="primary" @click="openImport(row)">导入</el-button>
      </template>
    </DataTable>

    <el-dialog v-model="createVisible" title="新建团检批次" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="企业" required>
          <el-select v-model="form.orgId" style="width: 100%" @change="onOrg">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="套餐" required>
          <el-select v-model="form.packageId" style="width: 100%" filterable>
            <el-option v-for="p in packages" :key="p.id" :label="`${p.packageName}（${p.packageNo}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="体检日期" required>
          <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD"
                          start-placeholder="开始" end-placeholder="结束" style="width: 100%" />
        </el-form-item>
        <el-form-item label="名额" required><el-input-number v-model="form.quota" :min="1" :step="10" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="memberVisible" :title="`名单 · ${cur.batchNo || ''}`" size="720px">
      <el-table :data="members" border size="small">
        <el-table-column prop="memberName" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="70">
          <template #default="{ row }">{{ row.gender === 'F' ? '女' : '男' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" align="right" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="importStatus" label="导入" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.importStatus === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.importStatus === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="失败原因" min-width="200" />
      </el-table>
    </el-drawer>

    <el-dialog v-model="importVisible" title="Excel 名单导入" width="640px">
      <el-upload :action="uploadUrl" :headers="uploadHeaders" name="file" :show-file-list="false"
                 accept=".xlsx,.xls" :on-success="onFileOk">
        <el-button>选择 Excel 文件</el-button>
      </el-upload>
      <div class="muted mt8">模板列：姓名、性别、年龄、手机号、身份证号、套餐编号；单次不超过 500 行</div>
      <el-input v-model="pasteRows" type="textarea" :rows="8" class="mt8"
                placeholder="也可直接粘贴表格行，每行一条，字段用 Tab 或逗号分隔：张建国,男,45,13800001111,310101198001011234,PKG-2026-005" />
      <div v-if="importResult.errors?.length" class="err mt8">
        成功 {{ importResult.success }} 行，失败 {{ importResult.failed }} 行
        <div v-for="(e, i) in importResult.errors" :key="i">第 {{ e.row }} 行：{{ e.error }}</div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statsVisible" title="集合统计" width="480px">
      <div class="stats">
        <div><span class="muted">报名人数</span><b class="num">{{ stats.joined }}</b></div>
        <div><span class="muted">已到检</span><b class="num">{{ stats.checked }}</b></div>
        <div><span class="muted">已完成</span><b class="num">{{ stats.completed }}</b></div>
        <div><span class="muted">到检率</span><b class="num">{{ stats.checkinRate }}%</b></div>
      </div>
      <div class="muted mt16">按隐私规则，团检联络员不可查看或下载个人体检报告。</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { BATCH_STATUS } from '../utils/format'

const tableRef = ref(null)
const query = reactive({ keyword: '', status: '' })
const createVisible = ref(false)
const memberVisible = ref(false)
const importVisible = ref(false)
const statsVisible = ref(false)
const members = ref([])
const orgs = ref([])
const packages = ref([])
const stats = ref({})
const cur = ref({})
const dates = ref([])
const pasteRows = ref('')
const importResult = reactive({ success: 0, failed: 0, errors: [] })
const uploadUrl = '/api/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }
const form = reactive({ orgId: null, orgName: '', packageId: null, quota: 50, contactName: '', contactPhone: '' })

const columns = [
  { prop: 'batchNo', label: '批次编号', minWidth: 140 },
  { prop: 'orgName', label: '企业名称', minWidth: 180 },
  { prop: 'dateRange', label: '体检日期', minWidth: 200 },
  { prop: 'joined', label: '已报名 / 名额', minWidth: 130 },
  { prop: 'checkedCount', label: '已到检', width: 90, align: 'right', num: true },
  { prop: 'status', label: '状态', width: 100 }
]

function fetchPage(params) {
  return request.get('/group/page', { params: { ...params, ...query } })
}

function onOrg(id) {
  form.orgName = orgs.value.find((o) => o.id === id)?.orgName || ''
}

async function create() {
  if (!form.orgId || !form.packageId || !dates.value?.[0]) return ElMessage.warning('请填全企业与套餐与日期')
  await request.post('/group/batch', { ...form, startDate: dates.value[0], endDate: dates.value[1] })
  ElMessage.success('批次已创建')
  createVisible.value = false
  tableRef.value.load()
}

async function openMembers(row) {
  cur.value = row
  const d = await request.get(`/group/${row.id}/members`, { params: { pageNum: 1, pageSize: 100 } })
  members.value = d.records || []
  memberVisible.value = true
}

async function openStats(row) {
  cur.value = row
  stats.value = await request.get(`/group/${row.id}/stats`)
  statsVisible.value = true
}

function openImport(row) {
  cur.value = row
  importResult.errors = []
  importVisible.value = true
}

function onFileOk(res) {
  if (res.code === 200) ElMessage.success('文件已上传，可点击开始导入')
}

async function doImport() {
  const lines = pasteRows.value.split('\n').map((l) => l.trim()).filter(Boolean)
  if (!lines.length) return ElMessage.warning('请粘贴名单或选择 Excel')
  const rows = lines.map((l) => {
    const p = l.split(/[\t,，]/).map((x) => x.trim())
    return { name: p[0], gender: p[1] === '女' ? 'F' : 'M', age: p[2], phone: p[3], idCard: p[4], packageNo: p[5] }
  })
  const d = await request.post(`/group/${cur.value.id}/import`, { rows })
  Object.assign(importResult, d)
  ElMessage.success(`导入完成：成功 ${d.success} 行，失败 ${d.failed} 行`)
  tableRef.value.load()
}

onMounted(async () => {
  orgs.value = await request.get('/org/all')
  const p = await request.get('/package/page', { params: { pageNum: 1, pageSize: 50 } })
  packages.value = p.records || []
})
</script>

<style scoped>
.err { background: #FBEEEE; border: 1px solid #E6BFC0; border-radius: 6px; padding: 10px 14px; font-size: 12.5px; color: #7A2226; line-height: 1.9; }
.stats { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.stats b { display: block; font-size: 20px; color: var(--primary); margin-top: 2px; }
</style>
