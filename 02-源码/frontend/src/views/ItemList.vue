<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="检查项目"
      subtitle="项目单价、执行科室、前置要求与性别年龄限制共同决定可预约套餐"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="140"
    >
      <template #filters>
        <el-input v-model="query.keyword" placeholder="项目编号 / 名称" clearable />
        <el-select v-model="query.dept" placeholder="全部科室" clearable>
          <el-option v-for="d in depts" :key="d" :label="d" :value="d" />
        </el-select>
      </template>
      <template #toolbar>
        <el-button type="primary" @click="open()">+ 新增项目</el-button>
      </template>
      <template #refPrice="{ row }"><span class="num">{{ money(row.refPrice) }}</span></template>
      <template #genderLimit="{ row }">
        <el-tag size="small" :type="row.genderLimit === 'ALL' ? 'info' : 'warning'">
          {{ row.genderLimit === 'ALL' ? '不限' : row.genderLimit === 'F' ? '女性' : '男性' }}
        </el-tag>
      </template>
      <template #ageRange="{ row }"><span class="num">{{ row.ageMin }}-{{ row.ageMax }} 岁</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="open(row)">编辑</el-button>
        <el-button link type="danger" @click="remove(row)">删除</el-button>
      </template>
    </DataTable>

    <el-dialog v-model="visible" :title="form.id ? '编辑检查项目' : '新增检查项目'" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="项目编号"><el-input v-model="form.itemNo" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="项目名称" required><el-input v-model="form.itemName" /></el-form-item>
        <el-form-item label="执行科室" required>
          <el-select v-model="form.dept" style="width: 100%">
            <el-option v-for="d in depts" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="前置要求"><el-input v-model="form.preRequirement" placeholder="如：空腹 8 小时" /></el-form-item>
        <el-form-item label="性别限制">
          <el-radio-group v-model="form.genderLimit">
            <el-radio label="ALL">不限</el-radio>
            <el-radio label="M">男性</el-radio>
            <el-radio label="F">女性</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年龄范围">
          <el-input-number v-model="form.ageMin" :min="0" :max="120" /> 至
          <el-input-number v-model="form.ageMax" :min="0" :max="120" />
        </el-form-item>
        <el-form-item label="参考价">
          <el-input-number v-model="form.refPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="执行时长">
          <el-input-number v-model="form.durationMin" :min="0" />
          <span class="muted" style="margin-left: 8px">分钟</span>
        </el-form-item>
        <el-form-item label="注意事项"><el-input v-model="form.notice" type="textarea" :rows="2" /></el-form-item>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { money } from '../utils/format'

const tableRef = ref(null)
const query = reactive({ keyword: '', dept: '' })
const visible = ref(false)
const depts = ['内科', '外科', '眼科', '耳鼻喉科', '口腔科', '检验科', '影像科', '妇科', '功能科']
const columns = [
  { prop: 'itemNo', label: '项目编号', minWidth: 110, num: true },
  { prop: 'itemName', label: '项目名称', minWidth: 160 },
  { prop: 'dept', label: '执行科室', width: 100 },
  { prop: 'preRequirement', label: '前置要求', minWidth: 140 },
  { prop: 'genderLimit', label: '性别限制', width: 100 },
  { prop: 'ageRange', label: '年龄范围', minWidth: 110 },
  { prop: 'refPrice', label: '参考价', width: 100, align: 'right' },
  { prop: 'durationMin', label: '时长(分钟)', width: 100, align: 'right' },
  { prop: 'notice', label: '注意事项', minWidth: 180 }
]
const form = reactive({ id: null, itemNo: '', itemName: '', dept: '', preRequirement: '', genderLimit: 'ALL', ageMin: 0, ageMax: 120, refPrice: 0, durationMin: 10, notice: '' })

function fetchPage(params) {
  return request.get('/item/page', { params: { ...params, ...query } })
}

function open(row) {
  Object.assign(form, row || { id: null, itemNo: '', itemName: '', dept: '', preRequirement: '', genderLimit: 'ALL', ageMin: 0, ageMax: 120, refPrice: 0, durationMin: 10, notice: '' })
  visible.value = true
}

async function save() {
  if (!form.itemName) return ElMessage.warning('请填写项目名称')
  if (form.ageMax < form.ageMin) return ElMessage.warning('年龄上限不能小于下限')
  await request.post('/item', form)
  ElMessage.success('保存成功')
  visible.value = false
  tableRef.value.load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除检查项目「${row.itemName}」？`, '删除确认', { type: 'warning' })
  await request.delete(`/item/${row.id}`)
  ElMessage.success('已删除')
  tableRef.value.load()
}
</script>
