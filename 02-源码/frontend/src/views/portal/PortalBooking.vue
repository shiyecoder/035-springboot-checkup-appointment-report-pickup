<template>
  <div class="booking">
    <div class="steps">
      <div class="step on"><b>1</b>选择套餐</div>
      <div class="step on"><b>2</b>选择时段</div>
      <div class="step"><b>3</b>填写信息</div>
      <div class="step"><b>4</b>确认预约</div>
    </div>

    <div class="panel">
      <div class="panel-h">
        <h3>{{ pkg.packageName || '套餐详情' }}</h3>
        <span class="sub">{{ pkg.packageNo }} · {{ pkg.crowd }} · {{ pkg.itemCount }} 个项目 · 约 {{ pkg.durationMin }} 分钟</span>
        <div class="right"><span class="price num">{{ money(pkg.discountPrice) }}</span></div>
      </div>

      <div class="dates">
        <el-radio-group v-model="date" @change="loadSlots">
          <el-radio-button v-for="d in dates" :key="d.value" :label="d.value">{{ d.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <div class="crowd">
        <div v-for="s in slots" :key="s.id" class="slot" :class="{ on: slotId === s.id, disabled: s.remain === 0 }"
             @click="pick(s)">
          <div class="sh">
            <b class="num">{{ s.timeRange }}</b>
            <el-tag size="small" :type="tagType(s.loadRate)">{{ tagText(s.loadRate) }}</el-tag>
          </div>
          <div class="bar"><i :style="{ width: s.loadRate + '%' }" :class="barClass(s.loadRate)"></i></div>
          <div class="muted">已约 {{ s.booked }} / {{ s.quota }} 人 · 余 {{ s.remain }} 个名额</div>
        </div>
        <div v-if="!slots.length" class="empty-tip">该日期暂未放号，请选择其他日期。</div>
      </div>
    </div>

    <div class="cols">
      <div class="panel">
        <div class="panel-h"><h3>填写预约信息</h3></div>
        <el-form :model="form" label-width="100px">
          <el-form-item label="体检人" required><el-input v-model="form.contactName" /></el-form-item>
          <el-form-item label="手机号" required><el-input v-model="form.contactPhone" /></el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="form.idCard" placeholder="用于身份校验，系统仅保存脱敏摘要" />
          </el-form-item>
          <el-form-item label="特殊需求">
            <el-input v-model="form.specialNeed" type="textarea" :rows="2" placeholder="如行动不便、需轮椅等" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-h"><h3>时段拥挤度建议</h3><span class="sub">辅助参考，不替代您的决定</span></div>
        <div v-if="score" class="score-box">
          <div class="score-line">
            <b class="num">{{ score.totalScore }}</b>
            <el-tag :type="LEVEL[score.level]?.type">{{ LEVEL[score.level]?.text }}</el-tag>
            <span class="muted">规则 v{{ score.ruleVersion }}</span>
          </div>
          <div class="muted mt8">主要影响因素：{{ score.mainFactors }}</div>
          <div class="muted">未满足条件：{{ score.unmetConditions }}</div>
          <div class="muted">可改进项：{{ score.improveTip }}</div>
        </div>
        <div v-else class="empty-tip">选择时段后显示推荐依据。</div>

        <el-button type="primary" class="submit" :loading="submitting" @click="submit">
          确认预约并锁定名额
        </el-button>
        <div class="muted mt8">提交后名额锁定 30 分钟，超时未确认自动释放。</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { LEVEL, money } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const pkg = ref({})
const dates = ref([])
const date = ref('')
const slots = ref([])
const slotId = ref(null)
const score = ref(null)
const submitting = ref(false)
const form = reactive({ contactName: '', contactPhone: '', idCard: '', specialNeed: '' })

function tagType(load) {
  return load >= 85 ? 'danger' : load >= 60 ? 'warning' : 'success'
}
function tagText(load) {
  return load >= 85 ? '名额紧张' : load >= 60 ? '较拥挤' : '推荐'
}
function barClass(load) {
  return load >= 85 ? 'hi' : load >= 60 ? 'mid' : ''
}

async function loadSlots() {
  slots.value = await request.get('/slot/available', { params: { date: date.value } })
  slotId.value = null
  score.value = null
}

async function pick(s) {
  if (s.remain === 0) return ElMessage.warning('该时段名额已满')
  slotId.value = s.id
  score.value = await request.post('/crowd/evaluate', { slotId: s.id, preview: true })
}

async function submit() {
  if (!slotId.value) return ElMessage.warning('请先选择体检时段')
  if (!form.contactName || !form.contactPhone) return ElMessage.warning('请填写体检人与手机号')
  submitting.value = true
  try {
    const d = await request.post('/appointment', {
      packageId: pkg.value.id, slotId: slotId.value, ...form
    })
    ElMessage.success(`预约成功，编号 ${d.appNo}，请等待前台审核`)
    router.push('/portal/appointments')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const d = await request.get(`/package/${route.params.id}`)
  pkg.value = d.package
  const user = JSON.parse(localStorage.getItem('user') || '{}')
  form.contactName = user.realName || ''
  form.contactPhone = user.phone || ''
  // 未来 10 天可选日期
  const list = []
  for (let i = 2; i < 10; i++) {
    const dt = new Date(Date.now() + i * 86400000)
    const v = dt.toISOString().slice(0, 10)
    const w = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][dt.getDay()]
    list.push({ value: v, label: `${v.slice(5)} ${w}` })
  }
  dates.value = list
  date.value = list[0].value
  await loadSlots()
})
</script>

<style scoped>
.steps { display: flex; margin-bottom: 16px; }
.step {
  flex: 1; display: flex; align-items: center; gap: 10px; padding: 12px 16px;
  background: #fff; border: 1px solid var(--line); font-size: 13px; color: var(--text-3);
}
.step:first-child { border-radius: 6px 0 0 6px; }
.step:last-child { border-radius: 0 6px 6px 0; }
.step.on { background: #EEF5F7; border-color: var(--secondary); color: var(--secondary); font-weight: 500; }
.step b { width: 20px; height: 20px; border-radius: 50%; background: #DCE5E9; color: #fff; display: grid; place-items: center; font-size: 11px; }
.step.on b { background: var(--secondary); }
.price { color: var(--danger); font-size: 20px; font-weight: 600; }
.dates { margin-bottom: 16px; }
.crowd { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.slot { border: 1px solid var(--line); border-radius: 6px; padding: 12px 14px; cursor: pointer; background: #fff; }
.slot.on { border-color: var(--secondary); background: #F4FAFB; box-shadow: inset 0 0 0 1px var(--secondary); }
.slot.disabled { opacity: 0.5; cursor: not-allowed; }
.sh { display: flex; align-items: center; justify-content: space-between; }
.sh b { font-size: 14px; }
.bar { height: 6px; background: #EDF2F4; border-radius: 3px; margin: 10px 0 6px; overflow: hidden; }
.bar i { display: block; height: 100%; background: var(--secondary); }
.bar i.mid { background: var(--warn); }
.bar i.hi { background: var(--danger); }
.cols { display: grid; grid-template-columns: 1.1fr 0.9fr; gap: 16px; margin-top: 16px; }
.score-line { display: flex; align-items: baseline; gap: 10px; }
.score-line b { font-size: 32px; color: var(--primary); }
.score-box { background: #F7FAFB; border: 1px solid var(--line); border-radius: 6px; padding: 14px; }
.submit { width: 100%; margin-top: 14px; }
</style>
