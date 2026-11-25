<template>
  <div class="page">
    <DataTable
      ref="tableRef"
      title="报告领取"
      subtitle="本人下载 / 现场领取 / 委托代领，全部留痕可追溯"
      :columns="columns"
      :fetch="fetchPage"
      :query="query"
      :action-width="100"
    >
      <template #filters>
        <el-select v-model="query.pickupType" placeholder="全部领取方式" clearable>
          <el-option v-for="(v, k) in PICKUP_TYPE" :key="k" :label="v" :value="k" />
        </el-select>
      </template>
      <template #pickupType="{ row }">
        <el-tag size="small" :type="row.pickupType === 'ENTRUST' ? 'warning' : row.pickupType === 'ONLINE' ? 'primary' : 'success'">
          {{ PICKUP_TYPE[row.pickupType] }}
        </el-tag>
      </template>
      <template #pickupTime="{ row }"><span class="num">{{ formatTime(row.pickupTime) }}</span></template>
      <template #actions="{ row }">
        <el-button link type="primary" @click="$router.push(`/appointment/${row.appId}`)">查看单据</el-button>
      </template>
    </DataTable>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import DataTable from '../components/DataTable.vue'
import request from '../api/request'
import { PICKUP_TYPE, formatTime } from '../utils/format'

const tableRef = ref(null)
const query = reactive({ pickupType: '' })
const columns = [
  { prop: 'appId', label: '预约 ID', width: 90, align: 'right', num: true },
  { prop: 'pickupType', label: '领取方式', width: 110 },
  { prop: 'pickerName', label: '领取人', width: 110 },
  { prop: 'relation', label: '与体检人关系', width: 120 },
  { prop: 'pickerPhone', label: '联系电话', width: 140, num: true },
  { prop: 'pickupTime', label: '领取时间', minWidth: 170 },
  { prop: 'remark', label: '备注', minWidth: 200 }
]

function fetchPage(params) {
  return request.get('/pickup/page', { params: { ...params, ...query } })
}
</script>
