<template>
  <div>
    <div class="panel">
      <div class="panel-h">
        <h3>{{ title }}</h3>
        <span class="sub">{{ subtitle }}</span>
        <div class="right"><slot name="toolbar" /></div>
      </div>

      <div class="filters">
        <slot name="filters" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="Refresh" @click="reset">重置</el-button>
      </div>

      <el-table
        :data="rows"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{ background: '#F7FAFB', color: '#5A6B74', fontWeight: 500 }"
      >
        <el-table-column
          v-for="c in columns"
          :key="c.prop"
          :prop="c.prop"
          :label="c.label"
          :min-width="c.minWidth || 110"
          :width="c.width"
          :align="c.align || 'left'"
          :class-name="c.num ? 'num' : ''"
        >
          <template #default="{ row }">
            <slot :name="c.prop" :row="row">
              <span :class="{ num: c.num }">{{ c.format ? c.format(row[c.prop], row) : (row[c.prop] ?? '-') }}</span>
            </slot>
          </template>
        </el-table-column>
        <el-table-column v-if="$slots.actions" label="操作" :width="actionWidth" fixed="right" align="center">
          <template #default="{ row }"><slot name="actions" :row="row" /></template>
        </el-table-column>
        <template #empty>
          <div class="empty-tip">当前筛选条件下无记录，可清空筛选条件后重试。</div>
        </template>
      </el-table>

      <div class="pager">
        <span class="muted">共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next, jumper"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  fetch: { type: Function, required: true },
  actionWidth: { type: Number, default: 180 },
  query: { type: Object, default: () => ({}) }
})

const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await props.fetch({ pageNum: pageNum.value, pageSize: pageSize.value, ...props.query })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

/** 切换筛选条件后页码回到第 1 页（文档要求） */
function search() {
  pageNum.value = 1
  load()
}

function reset() {
  pageNum.value = 1
  pageSize.value = 10
  load()
}

defineExpose({ load, search })
onMounted(load)
</script>

<style scoped>
.pager { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding-top: 14px; }
</style>
