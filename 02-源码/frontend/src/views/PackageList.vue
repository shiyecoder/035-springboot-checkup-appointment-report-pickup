<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="体检套餐"
      :subtitle="`共 ${totalText} 个套餐，封面图均为本地上传文件`"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="200"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="套餐编号 / 名称" clearable />
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option label="已上架" value="ON_SHELF" />
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已下架" value="OFF_SHELF" />
        </el-select>
        <el-select v-model="query.crowd" placeholder="全部适用人群" clearable>
          <el-option v-for="c in crowds" :key="c" :label="c" :value="c" />
        </el-select>
      </template>

      <template #toolbar>
        <el-button @click="exportCsv">导出当前筛选</el-button>
        <el-button type="primary" @click="openCreate">+ 新建套餐</el-button>
      </template>

      <template #coverImage="{ row }">
        <el-image
          v-if="row.coverImage"
          :src="row.coverImage"
          :preview-src-list="[row.coverImage]"
          fit="cover"
          style="width: 54px; height: 40px; border-radius: 4px"
          preview-teleported
        />
        <span v-else class="muted">未上传</span>
      </template>
      <template #packageName="{ row }">
        <div>{{ row.packageName }}</div>
        <div class="muted">{{ row.crowd }} · {{ row.itemCount }} 项 · {{ row.durationMin }} 分钟</div>
      </template>
      <template #originalPrice="{ row }"><span class="num">{{ money(row.originalPrice) }}</span></template>
      <template #discountPrice="{ row }"><span class="num" style="color: var(--danger)">{{ money(row.discountPrice) }}</span></template>
      <template #status="{ row }">
        <el-tag :type="PACKAGE_STATUS[row.status]?.type || 'info'" size="small">
          {{ PACKAGE_STATUS[row.status]?.text || row.status }}
        </el-tag>
      </template>
      <template #createTime="{ row }"><span class="num">{{ formatTime(row.createTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="view(row)">详情</el-button>
        <el-button link type="primary" @click="edit(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template>
    </DataTable>

    <el-drawer v-model="drawer" :title="form.id ? '编辑套餐' : '新建套餐'" size="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="套餐编号" required>
          <el-input v-model="form.packageNo" :disabled="!!form.id" placeholder="如 PKG-2026-021" />
        </el-form-item>
        <el-form-item label="套餐名称" required><el-input v-model="form.packageName" /></el-form-item>
        <el-form-item label="适用人群" required>
          <el-select v-model="form.crowd" style="width: 100%">
            <el-option v-for="c in crowds" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优惠价">
          <el-input-number v-model="form.discountPrice" :min="0" :precision="2" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计时长">
          <el-input-number v-model="form.durationMin" :min="0" :step="5" style="width: 100%" />
          <span class="muted" style="margin-left: 8px">分钟</span>
        </el-form-item>
        <el-form-item label="封面图片">
          <el-upload
            :action="uploadUrl"
            :headers="uploadHeaders"
            name="file"
            :query="{ bizType: 'package' }"
            :show-file-list="false"
            :on-success="onCoverOk"
            accept="image/*"
          >
            <img v-if="form.coverImage" :src="form.coverImage" class="cover-preview" />
            <el-button v-else>选择图片上传</el-button>
          </el-upload>
          <div class="muted mt8">仅支持本地上传，系统保存相对路径，不使用外链</div>
        </el-form-item>
        <el-form-item label="套餐状态">
          <el-radio-group v-model="form.status">
            <el-radio label="ON_SHELF">上架</el-radio>
            <el-radio label="DRAFT">草稿</el-radio>
            <el-radio label="OFF_SHELF">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="检查项目" required>
          <el-select v-model="form.itemIds" multiple filterable style="width: 100%" placeholder="选择项目">
            <el-option v-for="it in items" :key="it.id" :label="`${it.itemName}（${it.dept}）`" :value="it.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约说明">
          <el-input v-model="form.bookingNotice" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="detailVisible" title="套餐详情" width="720px">
      <div v-if="detail.package">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="套餐编号">{{ detail.package.packageNo }}</el-descriptions-item>
          <el-descriptions-item label="套餐名称">{{ detail.package.packageName }}</el-descriptions-item>
          <el-descriptions-item label="适用人群">{{ detail.package.crowd }}</el-descriptions-item>
          <el-descriptions-item label="原价">{{ money(detail.package.originalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="优惠价">{{ money(detail.package.discountPrice) }}</el-descriptions-item>
          <el-descriptions-item label="预计时长">{{ detail.package.durationMin }} 分钟</el-descriptions-item>
          <el-descriptions-item label="执行科室" :span="2">{{ detail.package.deptSummary }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ PACKAGE_STATUS[detail.package.status]?.text }}</el-descriptions-item>
          <el-descriptions-item label="预约说明" :span="3">{{ detail.package.bookingNotice }}</el-descriptions-item>
        </el-descriptions>
        <div class="panel-h mt16"><h3>包含项目（{{ detail.items?.length }} 项）</h3></div>
        <el-table :data="detail.items" border size="small" max-height="300">
          <el-table-column prop="itemNo" label="项目编号" width="110" />
          <el-table-column prop="itemName" label="项目名称" min-width="140" />
          <el-table-column prop="dept" label="科室" width="100" />
          <el-table-column prop="preRequirement" label="前置要求" min-width="140" />
          <el-table-column prop="refPrice" label="参考价" width="90" align="right" />
          <el-table-column prop="durationMin" label="时长" width="80" align="right" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { PACKAGE_STATUS, formatTime, money } from '../utils/format'

const tableRef = ref(null)
const totalText = ref('0')
const query = reactive({ keyword: '', status: '', crowd: '' })
const crowds = ['入职体检', '18-45 岁', '25-50 岁', '50 岁以上', '25-55 岁女性', '18-60 岁', '30-65 岁']
const drawer = ref(false)
const saving = ref(false)
const detailVisible = ref(false)
const detail = ref({})
const items = ref([])

const uploadUrl = '/api/upload'
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('token')}` }

const columns = [
  { prop: 'coverImage', label: '封面', width: 80 },
  { prop: 'packageNo', label: '套餐编号', minWidth: 140, num: true },
  { prop: 'packageName', label: '套餐名称', minWidth: 220 },
  { prop: 'deptSummary', label: '执行科室', minWidth: 160 },
  { prop: 'originalPrice', label: '原价', minWidth: 110, align: 'right' },
  { prop: 'discountPrice', label: '优惠价', minWidth: 110, align: 'right' },
  { prop: 'itemCount', label: '项目数', width: 80, align: 'right' },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'createTime', label: '创建时间', minWidth: 180 }
]

const form = reactive({
  id: null, packageNo: '', packageName: '', crowd: '', originalPrice: 0, discountPrice: 0,
  durationMin: 60, coverImage: '', status: 'DRAFT', itemIds: [], bookingNotice: ''
})

async function fetchPage(params) {
  const data = await request.get('/package/page', { params })
  totalText.value = data.total
  return data
}

function resetForm() {
  Object.assign(form, {
    id: null, packageNo: '', packageName: '', crowd: '', originalPrice: 0, discountPrice: 0,
    durationMin: 60, coverImage: '', status: 'DRAFT', itemIds: [], bookingNotice: ''
  })
}

function openCreate() {
  resetForm()
  drawer.value = true
}

async function edit(row) {
  resetForm()
  const d = await request.get(`/package/${row.id}`)
  Object.assign(form, d.package, { itemIds: (d.items || []).map((i) => i.id) })
  drawer.value = true
}

async function view(row) {
  detail.value = await request.get(`/package/${row.id}`)
  detailVisible.value = true
}

function onCoverOk(res) {
  if (res.code === 200) {
    form.coverImage = res.data.url
    ElMessage.success('封面已上传')
  } else {
    ElMessage.error(res.msg)
  }
}

async function save() {
  if (!form.packageName || !form.crowd) return ElMessage.warning('请填写套餐名称与适用人群')
  if (!form.itemIds.length) return ElMessage.warning('请至少选择一个检查项目')
  saving.value = true
  try {
    if (form.id) {
      await request.put(`/package/${form.id}`, form)
    } else {
      await request.post('/package', form)
    }
    ElMessage.success('保存成功')
    drawer.value = false
    tableRef.value.load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除套餐「${row.packageName}」？该操作不可撤销。`, '删除确认', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
  })
  await request.delete(`/package/${row.id}`)
  ElMessage.success('已删除')
  tableRef.value.load()
}

function exportCsv() {
  ElMessage.success('导出任务已创建，完成后可在下载中心查看')
}

onMounted(async () => {
  items.value = await request.get('/item/all')
})
</script>

<style scoped>
.cover-preview { width: 120px; height: 84px; object-fit: cover; border-radius: 6px; border: 1px solid var(--line); }
</style>
