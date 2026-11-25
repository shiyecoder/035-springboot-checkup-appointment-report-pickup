<template>
  <div class="portal-wrap-inner">
    <div class="hero">
      <div class="hero-l">
        <h1>选择适合您的体检套餐</h1>
        <p>2026 年度个人健康体检开放预约，可预约未来 10 天内的上午时段。团检请由企业联络员统一建批。</p>
      </div>
      <div class="hero-r">
        <div><b class="num">{{ total }}</b><span>在架套餐</span></div>
        <div><b class="num">3</b><span>每日时段</span></div>
        <div><b class="num">4</b><span>类角色服务</span></div>
      </div>
    </div>

    <div class="filters-bar">
      <el-input v-model="query.keyword" placeholder="搜索套餐名称 / 编号" clearable style="width: 260px" />
      <el-select v-model="query.crowd" placeholder="全部人群" clearable style="width: 200px">
        <el-option v-for="c in crowds" :key="c" :label="c" :value="c" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <div v-loading="loading" class="cards">
      <div v-for="p in rows" :key="p.id" class="pkg-card">
        <div class="cover">
          <img v-if="p.coverImage" :src="p.coverImage" :alt="p.packageName" />
          <div v-else class="ph">套餐封面</div>
        </div>
        <div class="body">
          <h4>{{ p.packageName }}</h4>
          <div class="meta">
            <span>{{ p.crowd }}</span><span>{{ p.itemCount }} 个项目</span><span>约 {{ p.durationMin }} 分钟</span>
          </div>
          <div class="price">
            <b class="num">{{ money(p.discountPrice) }}</b>
            <s class="num">{{ money(p.originalPrice) }}</s>
          </div>
          <div class="foot">
            <el-tag size="small" type="success">可预约</el-tag>
            <el-button type="primary" size="small" @click="book(p)">立即预约</el-button>
          </div>
        </div>
      </div>
    </div>
    <div v-if="!rows.length && !loading" class="empty-tip">当前筛选条件下暂无可预约套餐。</div>

    <div class="pager-row">
      <span class="muted">共 {{ total }} 个套餐</span>
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total"
                     layout="prev, pager, next" background @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { money } from '../../utils/format'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(8)
const loading = ref(false)
const query = reactive({ keyword: '', crowd: '' })
const crowds = ['18-45 岁', '25-50 岁', '50 岁以上', '25-55 岁女性', '18-60 岁', '12-18 岁', '65 岁以上']

async function load() {
  loading.value = true
  try {
    const d = await request.get('/package/page', { params: { pageNum: pageNum.value, pageSize: pageSize.value, ...query } })
    rows.value = d.records || []
    total.value = d.total
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

function reset() {
  query.keyword = ''
  query.crowd = ''
  search()
}

function book(p) {
  router.push(`/portal/booking/${p.id}`)
}

onMounted(load)
</script>

<style scoped>
.hero {
  background: #fff; border: 1px solid var(--line); border-radius: 8px; padding: 22px 24px;
  display: flex; align-items: center; gap: 24px; margin-bottom: 16px;
}
.hero h1 { font-size: 18px; margin: 0 0 8px; color: var(--primary); }
.hero p { font-size: 13px; color: var(--text-2); margin: 0; line-height: 1.7; }
.hero-r { margin-left: auto; display: flex; gap: 28px; }
.hero-r b { display: block; font-size: 22px; color: var(--primary); }
.hero-r span { font-size: 12px; color: var(--text-3); }
.filters-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.pkg-card { background: #fff; border: 1px solid var(--line); border-radius: 8px; overflow: hidden; }
.cover { height: 132px; background: #EEF4F6; }
.cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover .ph { height: 100%; display: grid; place-items: center; color: #9FB4BD; font-size: 12px; }
.body { padding: 14px 16px; }
.body h4 { font-size: 14.5px; margin: 0 0 6px; }
.meta { display: flex; gap: 10px; font-size: 12px; color: var(--text-3); flex-wrap: wrap; }
.price { margin: 10px 0; display: flex; align-items: baseline; gap: 8px; }
.price b { color: var(--danger); font-size: 18px; }
.price s { color: var(--text-3); font-size: 12px; }
.foot { display: flex; align-items: center; justify-content: space-between; }
.pager-row { display: flex; align-items: center; justify-content: flex-end; gap: 12px; padding-top: 16px; }
</style>
