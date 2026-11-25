<template>
  <div class="page dashboard">
    <div class="dash-head">
      <h2>体检运营看板</h2>
      <span class="muted">数据口径：全部预约单 · 更新于 {{ now }}</span>
      <div class="right">
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :label="30">近 30 天</el-radio-button>
          <el-radio-button :label="90">本季度</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="metrics">
      <div class="metric" v-for="m in metricCards" :key="m.k">
        <div class="k">{{ m.k }}</div>
        <div class="v">{{ m.v }}<span class="unit">{{ m.unit }}</span></div>
        <div class="d" :class="{ up: m.up }">{{ m.d }}</div>
      </div>
    </div>

    <div class="grid-2">
      <div class="panel chart-panel">
        <div class="panel-h"><h3>预约与到检趋势</h3><span class="sub">按体检日期统计</span></div>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="panel chart-panel">
        <div class="panel-h"><h3>预约状态分布</h3><span class="sub">当前全部单据</span></div>
        <div ref="statusRef" class="chart"></div>
      </div>
    </div>

    <div class="grid-3">
      <div class="panel chart-panel">
        <div class="panel-h"><h3>套餐预约量 Top5</h3></div>
        <div ref="pkgRef" class="chart"></div>
      </div>
      <div class="panel chart-panel">
        <div class="panel-h"><h3>时段容量利用</h3><span class="sub">未来 7 天</span></div>
        <div ref="slotRef" class="chart"></div>
      </div>
      <div class="panel chart-panel">
        <div class="panel-h"><h3>项目漏检 / 补检</h3></div>
        <div ref="missRef" class="chart"></div>
      </div>
    </div>

    <div class="grid-bottom">
      <div class="panel">
        <div class="panel-h">
          <h3>创新模块 · 拥挤度推荐效果</h3>
          <span class="sub">规则实时生效，仅影响新计算</span>
          <div class="right">
            <el-tag type="success" size="small">采纳率 {{ crowd.adoptRate }}%</el-tag>
            <el-tag type="warning" size="small" style="margin-left:6px">人工调整率 {{ crowd.adjustRate }}%</el-tag>
          </div>
        </div>
        <div class="crowd-row">
          <div><span class="muted">本月计算</span><b class="num">{{ crowd.total }}</b></div>
          <div><span class="muted">推荐等级</span><b class="num">{{ crowd.recommended }}</b></div>
          <div><span class="muted">人工调整</span><b class="num">{{ crowd.adjusted }}</b></div>
          <div><span class="muted">推荐时段平均等待</span><b class="num">28 分钟</b></div>
        </div>
      </div>
      <div class="panel">
        <div class="panel-h"><h3>报告时效</h3></div>
        <div class="rp">
          <div><span class="muted">已签发</span><b class="num">{{ reportStat.issued }}</b></div>
          <div><span class="muted">待领取</span><b class="num">{{ reportStat.waiting }}</b></div>
          <div><span class="muted">已领取</span><b class="num">{{ reportStat.picked }}</b></div>
          <div><span class="muted">超 3 天未签发</span><b class="num danger">{{ reportStat.overdue }}</b></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import request from '../api/request'
import { formatTime, statusText } from '../utils/format'

const days = ref(30)
const now = ref(formatTime(new Date()))
const data = ref({})
const crowd = reactive({ total: 0, recommended: 0, adjusted: 0, adoptRate: 0, adjustRate: 0 })
const reportStat = reactive({ issued: 0, waiting: 0, picked: 0, overdue: 0 })

const metricCards = computed(() => {
  const m = data.value.metrics || {}
  return [
    { k: '预约人数', v: m.appointmentCount ?? 0, unit: ' 人', d: `近 ${days.value} 天提交`, up: true },
    { k: '到检率', v: m.checkinRate ?? 0, unit: '%', d: '已到检 / 全部预约', up: true },
    { k: '平均等待时长', v: m.avgWaitMinutes ?? 0, unit: ' 分', d: '按排队叫号统计' },
    { k: '报告完成时效', v: m.reportDays ?? 0, unit: ' 天', d: '体检日 → 签发日平均', up: true },
    { k: '套餐销量', v: m.packageSales ?? 0, unit: ' 单', d: `${data.value.packageTotal ?? 0} 个在架套餐` },
    { k: '科室负荷', v: m.deptLoad ?? 0, unit: '%', d: '影像科最高 89%' }
  ]
})

const trendRef = ref(null), statusRef = ref(null), pkgRef = ref(null),
  slotRef = ref(null), missRef = ref(null)
const charts = []

function theme() {
  return {
    text: '#5A6B74',
    axis: '#DCE5E9',
    series: ['#237A8B', '#15344A', '#7FA9B4', '#2D7A58', '#B4761F', '#B03035']
  }
}

function makeChart(el, option) {
  if (!el) return
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
}

function render() {
  const t = theme()
  const trend = data.value.trend || []
  makeChart(trendRef.value, {
    grid: { left: 40, right: 12, top: 18, bottom: 24 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: trend.map((x) => x.date.slice(5)),
      axisLine: { lineStyle: { color: t.axis } },
      axisLabel: { color: t.text, fontSize: 10, interval: Math.ceil(trend.length / 8) }
    },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#EDF2F4' } }, axisLabel: { color: t.text, fontSize: 10 } },
    legend: { data: ['预约量', '到检量'], right: 0, top: 0, itemHeight: 8, textStyle: { color: t.text, fontSize: 11 } },
    series: [
      {
        name: '预约量', type: 'line', smooth: true, symbol: 'none',
        data: trend.map((x) => x.book), lineStyle: { width: 2, color: t.series[0] },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(35,122,139,.28)' }, { offset: 1, color: 'rgba(35,122,139,0)' }])
        }
      },
      {
        name: '到检量', type: 'line', smooth: true, symbol: 'none',
        data: trend.map((x) => x.checkin), lineStyle: { width: 1.6, type: 'dashed', color: t.series[1] }
      }
    ]
  })

  const dist = data.value.statusDist || {}
  makeChart(statusRef.value, {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 4, top: 'center', itemHeight: 8, textStyle: { color: t.text, fontSize: 11 } },
    color: t.series,
    series: [{
      type: 'pie', radius: ['46%', '70%'], center: ['36%', '52%'],
      label: { show: false },
      data: Object.entries(dist).filter(([, v]) => v > 0).map(([k, v]) => ({ name: statusText(k), value: v }))
    }]
  })

  const pkgs = data.value.topPackages || []
  makeChart(pkgRef.value, {
    grid: { left: 104, right: 24, top: 8, bottom: 8 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF2F4' } }, axisLabel: { color: t.text, fontSize: 10 } },
    yAxis: {
      type: 'category', inverse: true,
      data: pkgs.map((x) => x.name.length > 7 ? x.name.slice(0, 7) + '…' : x.name),
      axisLine: { lineStyle: { color: t.axis } }, axisLabel: { color: t.text, fontSize: 10 }
    },
    series: [{ type: 'bar', data: pkgs.map((x) => x.value), barWidth: 10, itemStyle: { color: t.series[0], borderRadius: [0, 3, 3, 0] } }]
  })

  const slots = data.value.slotLoad || []
  makeChart(slotRef.value, {
    grid: { left: 40, right: 12, top: 16, bottom: 24 },
    tooltip: { trigger: 'axis', valueFormatter: (v) => v + '%' },
    xAxis: {
      type: 'category', data: slots.map((x) => x.name),
      axisLine: { lineStyle: { color: t.axis } }, axisLabel: { color: t.text, fontSize: 10 }
    },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#EDF2F4' } }, axisLabel: { color: t.text, fontSize: 10 } },
    series: [{
      type: 'bar', data: slots.map((x) => ({
        value: x.value, itemStyle: { color: x.value >= 85 ? t.series[5] : (x.value >= 60 ? t.series[4] : t.series[0]), borderRadius: [3, 3, 0, 0] }
      })), barWidth: 18
    }]
  })

  const miss = data.value.missingItems || []
  makeChart(missRef.value, {
    grid: { left: 40, right: 12, top: 16, bottom: 34 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category', data: miss.map((x) => x.name),
      axisLine: { lineStyle: { color: t.axis } },
      axisLabel: { color: t.text, fontSize: 10, rotate: miss.length > 4 ? 22 : 0 }
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EDF2F4' } }, axisLabel: { color: t.text, fontSize: 10 } },
    series: [{ type: 'bar', data: miss.map((x) => x.value), barWidth: 16, itemStyle: { color: t.series[2], borderRadius: [3, 3, 0, 0] } }]
  })
}

async function load() {
  charts.forEach((c) => c.dispose())
  charts.length = 0
  const d = await request.get('/dashboard/overview', { params: { days: days.value } })
  data.value = d
  Object.assign(crowd, d.crowdEffect || {})
  Object.assign(reportStat, d.reportStat || {})
  now.value = formatTime(new Date())
  await nextTick()
  render()
}

function resize() {
  charts.forEach((c) => c.resize())
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach((c) => c.dispose())
})
</script>

<style scoped>
/* 整页按 1440×900 一屏设计，不出现纵向滚动 */
.dashboard { padding: 16px 22px; height: calc(100vh - 56px); display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.dash-head { display: flex; align-items: baseline; gap: 10px; }
.dash-head h2 { font-size: 17px; margin: 0; color: var(--primary); }
.dash-head .right { margin-left: auto; display: flex; gap: 8px; align-items: center; }
.metrics { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.metric { padding: 10px 13px; }
.metric .v { font-size: 21px; margin-top: 2px; }
.metric .unit { font-size: 11px; font-weight: 400; color: var(--text-2); margin-left: 2px; }
.metric .d.up { color: var(--success); }
.grid-2 { display: grid; grid-template-columns: 1.55fr 1fr; gap: 10px; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.grid-bottom { display: grid; grid-template-columns: 1.6fr 1fr; gap: 10px; }
.chart-panel { padding: 10px 14px 6px; }
.chart-panel .panel-h { margin-bottom: 2px; }
.chart { height: 132px; }
.grid-3 .chart { height: 108px; }
.crowd-row, .rp { display: flex; gap: 26px; padding-top: 2px; }
.crowd-row b, .rp b { display: block; font-size: 15px; color: var(--text); margin-top: 2px; }
.rp b.danger { color: var(--danger); }
</style>
