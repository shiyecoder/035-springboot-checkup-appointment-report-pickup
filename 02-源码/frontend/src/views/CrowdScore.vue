<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="计算留痕"
      subtitle="每次计算保存输入快照、规则版本与明细分，结果可追溯、可复算"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="200"
    >
      <template #filters>
        <el-input v-model="query.scoreNo" placeholder="计算编号" clearable />
        <el-select v-model="query.level" placeholder="全部等级" clearable>
          <el-option v-for="(v, k) in LEVEL" :key="k" :label="v.text" :value="k" />
        </el-select>
      </template>
      <template #scoreNo="{ row }"><span class="num">{{ row.scoreNo }}</span></template>
      <template #totalScore="{ row }"><span class="num">{{ row.totalScore }}</span></template>
      <template #level="{ row }">
        <el-tag size="small" :type="LEVEL[row.level]?.type">{{ LEVEL[row.level]?.text }}</el-tag>
      </template>
      <template #adjustedScore="{ row }">
        <span v-if="row.adjustedScore" class="num" style="color: var(--warn)">{{ row.adjustedScore }}</span>
        <span v-else class="muted">未调整</span>
      </template>
      <template #createTime="{ row }"><span class="num">{{ formatTime(row.createTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="openDetail(row)">明细</el-button>
        <el-button link type="warning" @click="openAdjust(row)">人工调整</el-button>
      </template>
    </DataTable>

    <el-drawer v-model="detailVisible" title="计算明细" size="620px">
      <div v-if="detail.id">
        <div class="score">
          <b class="num">{{ detail.totalScore }}</b>
          <el-tag :type="LEVEL[detail.level]?.type">{{ LEVEL[detail.level]?.text }}</el-tag>
          <span class="muted">规则 v{{ detail.ruleVersion }} · {{ formatTime(detail.createTime) }}</span>
        </div>
        <div class="panel-h mt16"><h3>各项得分</h3></div>
        <el-table :data="factorRows" border size="small">
          <el-table-column prop="name" label="因素" width="120" />
          <el-table-column prop="weight" label="权重" width="80" align="right" />
          <el-table-column prop="value" label="归一化得分" width="120" align="right" />
          <el-table-column prop="contribution" label="贡献值" width="110" align="right" />
          <el-table-column label="说明">
            <template #default="{ row }">
              <span :class="row.missing ? 'muted' : ''">{{ row.missing ? '数据缺失，未参与计算' : '已参与加权' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="panel-h mt16"><h3>输入快照</h3></div>
        <pre class="snap">{{ prettySnapshot }}</pre>
        <div class="panel-h mt16"><h3>结果解释</h3></div>
        <div class="explain">
          <div><span class="muted">主要影响因素：</span>{{ detail.mainFactors }}</div>
          <div><span class="muted">未满足条件：</span>{{ detail.unmetConditions }}</div>
          <div><span class="muted">可改进项：</span>{{ detail.improveTip }}</div>
          <div v-if="detail.adjustedScore">
            <span class="muted">人工调整：</span>{{ detail.adjustedScore }}（{{ detail.adjustReason }}）
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="adjustVisible" title="人工调整" width="480px">
      <el-form label-width="110px">
        <el-form-item label="原始计算结果"><span class="num">{{ cur.totalScore }}（{{ LEVEL[cur.level]?.text }}）</span></el-form-item>
        <el-form-item label="调整后分数"><el-input-number v-model="adjusted" :min="0" :max="100" :precision="1" /></el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="adjustReason" type="textarea" :rows="3" placeholder="不少于 5 个字，将写入留痕" />
        </el-form-item>
      </el-form>
      <div class="muted">原始计算结果不会被覆盖，只新增调整记录。</div>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="doAdjust">提交调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { LEVEL, formatTime } from '../utils/format'

const tableRef = ref(null)
const query = reactive({ scoreNo: '', level: '' })
const detailVisible = ref(false)
const detail = ref({})
const adjustVisible = ref(false)
const cur = ref({})
const adjusted = ref(0)
const adjustReason = ref('')

const columns = [
  { prop: 'scoreNo', label: '计算编号', minWidth: 180 },
  { prop: 'appId', label: '预约 ID', width: 90, align: 'right', num: true },
  { prop: 'slotId', label: '时段 ID', width: 90, align: 'right', num: true },
  { prop: 'totalScore', label: '综合拥挤度', width: 120, align: 'right' },
  { prop: 'level', label: '结果', width: 90 },
  { prop: 'ruleVersion', label: '规则版本', width: 90, num: true },
  { prop: 'adjustedScore', label: '人工调整', width: 100, align: 'right' },
  { prop: 'createTime', label: '生成时间', minWidth: 160 }
]

const factorRows = computed(() => {
  try {
    return JSON.parse(detail.value.detailJson || '[]')
  } catch (e) {
    return []
  }
})

const prettySnapshot = computed(() => {
  try {
    return JSON.stringify(JSON.parse(detail.value.inputSnapshot || '{}'), null, 2)
  } catch (e) {
    return detail.value.inputSnapshot || ''
  }
})

function fetchPage(params) {
  return request.get('/crowd/score/page', { params: { ...params, ...query } })
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

function openAdjust(row) {
  cur.value = row
  adjusted.value = Number(row.adjustedScore || row.totalScore)
  adjustReason.value = ''
  adjustVisible.value = true
}

async function doAdjust() {
  if (adjustReason.value.trim().length < 5) return ElMessage.warning('调整原因不少于 5 个字')
  await request.post(`/crowd/score/${cur.value.id}/adjust`, {
    adjustedScore: adjusted.value, reason: adjustReason.value
  })
  ElMessage.success('已记录人工调整')
  adjustVisible.value = false
  tableRef.value.load()
}
</script>

<style scoped>
.score { display: flex; align-items: baseline; gap: 12px; }
.score b { font-size: 32px; color: var(--primary); }
.snap { background: #F7FAFB; border: 1px solid var(--line); border-radius: 6px; padding: 12px; font-size: 11.5px; line-height: 1.8; overflow: auto; max-height: 280px; }
.explain div { font-size: 13px; line-height: 2; }
</style>
