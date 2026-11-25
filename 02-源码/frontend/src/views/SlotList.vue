<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="时段容量"
      subtitle="每个日期的上午三段，个人与团体名额独立；关闭后不再放号"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="150"
    >
      <template #filters>
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                        start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option label="放号中" value="OPEN" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </template>
      <template #toolbar>
        <el-button @click="generate">生成未来 7 天时段</el-button>
      </template>
      <template #slotDate="{ row }"><span class="num">{{ row.slotDate }}</span></template>
      <template #timeRange="{ row }"><span class="num">{{ row.timeRange }}</span></template>
      <template #quota="{ row }">
        <span class="num">{{ row.personalBooked }} / {{ row.personalQuota }}</span>
        <span class="muted"> · 团体 {{ row.groupBooked }} / {{ row.groupQuota }}</span>
      </template>
      <template #load="{ row }">
        <el-progress
          :percentage="loadRate(row)"
          :stroke-width="8"
          :color="loadRate(row) >= 85 ? '#B03035' : loadRate(row) >= 60 ? '#B4761F' : '#2D7A58'"
        />
      </template>
      <template #status="{ row }">
        <el-tag size="small" :type="row.status === 'OPEN' ? 'success' : 'info'">
          {{ row.status === 'OPEN' ? '放号中' : '已关闭' }}
        </el-tag>
      </template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="edit(row)">调整名额</el-button>
        <el-button link :type="row.status === 'OPEN' ? 'danger' : 'success'" @click="toggle(row)">
          {{ row.status === 'OPEN' ? '关闭' : '开放' }}
        </el-button>
      </template>
    </DataTable>

    <el-dialog v-model="visible" title="调整名额" width="460px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="日期时段"><span class="num">{{ form.slotDate }} {{ form.timeRange }}</span></el-form-item>
        <el-form-item label="个人名额">
          <el-input-number v-model="form.personalQuota" :min="form.personalBooked" />
          <span class="muted" style="margin-left: 8px">已约 {{ form.personalBooked }}</span>
        </el-form-item>
        <el-form-item label="团体名额">
          <el-input-number v-model="form.groupQuota" :min="form.groupBooked" />
          <span class="muted" style="margin-left: 8px">已约 {{ form.groupBooked }}</span>
        </el-form-item>
        <el-form-item label="科室接待量">
          <el-input-number v-model="form.deptCapacity" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'

const tableRef = ref(null)
const query = reactive({ status: '' })
const range = ref([])
const visible = ref(false)
const form = reactive({ id: null, slotDate: '', timeRange: '', personalQuota: 0, groupQuota: 0, personalBooked: 0, groupBooked: 0, deptCapacity: 0 })

const columns = [
  { prop: 'slotDate', label: '日期', width: 120 },
  { prop: 'timeRange', label: '时段', width: 130 },
  { prop: 'quota', label: '个人 / 团体名额（已约）', minWidth: 200 },
  { prop: 'deptCapacity', label: '科室接待量', width: 110, align: 'right', num: true },
  { prop: 'load', label: '负载', minWidth: 160 },
  { prop: 'status', label: '状态', width: 100 }
]

function loadRate(row) {
  const quota = row.personalQuota + row.groupQuota
  if (!quota) return 0
  return Math.round(((row.personalBooked + row.groupBooked) * 100) / quota)
}

function fetchPage(params) {
  return request.get('/slot/page', {
    params: { ...params, ...query, dateFrom: range.value?.[0] || '', dateTo: range.value?.[1] || '' }
  })
}

function edit(row) {
  Object.assign(form, row)
  visible.value = true
}

async function save() {
  await request.put(`/slot/${form.id}`, form)
  ElMessage.success('已保存')
  visible.value = false
  tableRef.value.load()
}

async function toggle(row) {
  await request.put(`/slot/${row.id}`, { ...row, status: row.status === 'OPEN' ? 'CLOSED' : 'OPEN' })
  ElMessage.success('状态已更新')
  tableRef.value.load()
}

async function generate() {
  const d = await request.post('/slot/generate', { days: 7 })
  ElMessage.success(`已新增 ${d.created} 个时段`)
  tableRef.value.load()
}
</script>
