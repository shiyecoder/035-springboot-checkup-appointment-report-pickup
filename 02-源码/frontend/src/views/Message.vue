<template>
  <div class="page">
    <div class="panel">
      <div class="panel-h">
        <h3>消息中心</h3>
        <span class="sub">待审批、报告可领取等消息均带业务编号，可跳转详情</span>
        <div class="right">
          <el-radio-group v-model="readFlag" size="small" @change="load">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="0">未读</el-radio-button>
            <el-radio-button :label="1">已读</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-for="m in rows" :key="m.id" class="msg" :class="{ unread: m.readFlag === 0 }">
        <div class="dot"></div>
        <div class="body">
          <div class="t">{{ m.title }}</div>
          <div class="c">{{ m.content }}</div>
          <div class="m muted">
            <span class="num">{{ formatTime(m.createTime) }}</span>
            <span v-if="m.bizNo" class="num biz" @click="go(m)">{{ m.bizNo }}</span>
          </div>
        </div>
        <el-button v-if="m.readFlag === 0" size="small" @click="markRead(m)">标为已读</el-button>
      </div>
      <div v-if="!rows.length" class="empty-tip">暂无消息。</div>

      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="pageNum" :page-size="10" :total="total" layout="prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../api/request'
import { formatTime } from '../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const readFlag = ref(null)

async function load() {
  const params = { pageNum: pageNum.value, pageSize: 10 }
  if (readFlag.value !== null) params.readFlag = readFlag.value
  const d = await request.get('/message/page', { params })
  rows.value = d.records || []
  total.value = d.total
}

async function markRead(m) {
  await request.put(`/message/${m.id}/read`)
  load()
}

function go(m) {
  if (m.bizType === 'REPORT') router.push('/report')
  else router.push('/appointment')
}

onMounted(load)
</script>

<style scoped>
.msg { display: flex; gap: 12px; padding: 14px 4px; border-bottom: 1px solid #EDF2F4; align-items: flex-start; }
.msg .dot { width: 6px; height: 6px; border-radius: 50%; background: #DCE5E9; margin-top: 8px; flex: none; }
.msg.unread .dot { background: var(--danger); }
.msg .body { flex: 1; }
.msg .t { font-size: 13.5px; font-weight: 500; }
.msg .c { font-size: 12.5px; color: var(--text-2); margin-top: 3px; }
.msg .m { margin-top: 4px; display: flex; gap: 14px; }
.biz { color: var(--accent); cursor: pointer; }
.pager { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding-top: 14px; }
</style>
