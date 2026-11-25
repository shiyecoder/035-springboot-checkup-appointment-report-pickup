<template>
  <div class="page">
    <div class="panel">
      <div class="panel-h">
        <h3>拥挤度规则配置</h3>
        <span class="sub">权重合计必须等于 100%，保存后生成新版本并立即生效（仅影响新计算）</span>
        <div class="right">
          <el-button @click="preview">预览计算</el-button>
          <el-button type="primary" :loading="saving" @click="save">保存为新版本</el-button>
        </div>
      </div>

      <div class="rule-grid">
        <div>
          <div class="wrow" v-for="(label, key) in labels" :key="key">
            <span class="n">{{ label }}</span>
            <el-input-number v-model="weights[key]" :min="0" :max="100" :step="1" controls-position="right" />
            <span class="tip">{{ tips[key] }}</span>
          </div>
          <div class="total">
            <span class="muted">合计</span>
            <b class="num" :style="{ color: totalWeight === 100 ? '#2D7A58' : '#B03035' }">{{ totalWeight }}%</b>
            <span class="muted">阈值：≤{{ thresholds.recommend }} 推荐 / ≤{{ thresholds.normal }} 一般 / 其余不推荐</span>
          </div>
          <div class="total mt8">
            <span class="muted">推荐阈值</span>
            <el-input-number v-model="thresholds.recommend" :min="10" :max="90" size="small" />
            <span class="muted">一般阈值</span>
            <el-input-number v-model="thresholds.normal" :min="20" :max="99" size="small" />
          </div>
        </div>
        <div>
          <div class="muted" style="margin-bottom: 8px">版本历史</div>
          <div v-for="r in history" :key="r.id" class="ver">
            <el-tag size="small" :type="r.status === 'ACTIVE' ? 'success' : 'info'">
              {{ r.status === 'ACTIVE' ? '生效中' : '历史' }}
            </el-tag>
            <b>v{{ r.versionNo }}</b>
            <span class="muted num">{{ formatTime(r.effectiveTime) }}</span>
          </div>
          <div class="muted mt16" style="line-height: 1.9">
            修改权重不会回溯历史留痕；如需让旧数据按新规则计算，请在「计算留痕」页选择「按当前规则重算」。
          </div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-h"><h3>试算结果</h3><span class="sub">选择时段后按当前权重实时试算，不落库</span></div>
      <div class="flex">
        <el-select v-model="testSlot" placeholder="选择时段试算" style="width: 320px">
          <el-option v-for="s in slots" :key="s.id" :label="`${s.slotDate} ${s.timeRange}（${s.booked}/${s.quota}）`" :value="s.id" />
        </el-select>
        <el-button type="primary" @click="preview">试算</el-button>
      </div>
      <div v-if="result" class="result mt16">
        <div class="score"><b class="num">{{ result.totalScore }}</b>
          <el-tag :type="LEVEL[result.level]?.type">{{ LEVEL[result.level]?.text }}</el-tag></div>
        <div class="muted mt8">主要影响因素：{{ result.mainFactors }}</div>
        <div class="muted">未满足条件：{{ result.unmetConditions }}</div>
        <div class="muted">可改进项：{{ result.improveTip }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { LEVEL, formatTime } from '../utils/format'

const labels = {
  deptCapacity: '科室容量',
  itemMix: '项目组合',
  slotLoad: '时段预约量',
  duration: '预计时长',
  arrivalRate: '历史到检率',
  deviceDown: '设备停用情况'
}
const tips = {
  deptCapacity: '各科室当日接待量与已排班医生数',
  itemMix: '套餐项目跨科室数量与串科室次数',
  slotLoad: '该时段已锁号人数 / 名额上限',
  duration: '套餐项目执行时长合计',
  arrivalRate: '近 90 天同时段到检率，越高越拥挤',
  deviceDown: '涉及设备停用则加权惩罚'
}
const weights = reactive({})
const thresholds = reactive({ recommend: 40, normal: 70 })
const history = ref([])
const slots = ref([])
const testSlot = ref(null)
const result = ref(null)
const saving = ref(false)

const totalWeight = computed(() => Object.values(weights).reduce((a, b) => a + (Number(b) || 0), 0))

async function load() {
  const d = await request.get('/crowd/rule')
  Object.assign(weights, d.weights)
  Object.assign(thresholds, d.thresholds)
  history.value = await request.get('/crowd/rule/history')
}

async function save() {
  if (totalWeight.value !== 100) return ElMessage.warning(`权重合计必须等于 100%，当前 ${totalWeight.value}%`)
  saving.value = true
  try {
    await request.post('/crowd/rule', { weights, thresholds })
    ElMessage.success('已保存为新版本并生效')
    load()
  } finally {
    saving.value = false
  }
}

async function preview() {
  if (!testSlot.value) return ElMessage.warning('请先选择试算时段')
  result.value = await request.post('/crowd/evaluate', { slotId: testSlot.value, preview: true })
}

onMounted(async () => {
  await load()
  const d = await request.get('/slot/page', { params: { pageNum: 1, pageSize: 30 } })
  slots.value = (d.records || []).map((s) => ({
    id: s.id, slotDate: s.slotDate, timeRange: s.timeRange,
    booked: s.personalBooked + s.groupBooked, quota: s.personalQuota + s.groupQuota
  }))
  if (slots.value.length) testSlot.value = slots.value[0].id
})
</script>

<style scoped>
.rule-grid { display: grid; grid-template-columns: 1.3fr 0.7fr; gap: 28px; }
.wrow { display: flex; align-items: center; gap: 14px; padding: 9px 0; border-bottom: 1px dashed #EDF2F4; }
.wrow .n { width: 110px; color: var(--text-2); font-size: 13px; }
.wrow .tip { font-size: 11.5px; color: var(--text-3); }
.total { display: flex; align-items: center; gap: 12px; font-size: 13px; margin-top: 14px; }
.total b { font-size: 16px; }
.ver { display: flex; align-items: center; gap: 10px; padding: 9px 0; border-bottom: 1px solid #EDF2F4; font-size: 12.5px; }
.score { display: flex; align-items: baseline; gap: 10px; }
.score b { font-size: 30px; color: var(--primary); }
</style>
