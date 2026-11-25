<template>
  <div class="page">
    <div class="panel">
      <div class="panel-h">
        <h3>角色管理</h3>
        <span class="sub">四个固定角色；数据范围决定 service 层可见的数据边界</span>
      </div>
      <el-table :data="rows" border size="small">
        <el-table-column prop="role.roleCode" label="角色编码" width="130" />
        <el-table-column prop="role.roleName" label="角色名称" width="140" />
        <el-table-column prop="role.dataScope" label="数据范围" width="110">
          <template #default="{ row }">
            <el-tag size="small">{{ SCOPE[row.role.dataScope] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role.remark" label="职责说明" min-width="260" />
        <el-table-column label="已授权菜单" width="120" align="right">
          <template #default="{ row }"><span class="num">{{ row.menuIds.length }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">分配菜单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="visible" :title="`为「${cur.role?.roleName || ''}」分配菜单`" size="520px">
      <div class="muted">勾选后保存，该角色下次登录即可看到对应菜单。列表已排除「菜单管理」本身。</div>
      <el-tree
        ref="treeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :default-checked-keys="cur.menuIds || []"
        :props="{ label: 'menuName', children: 'children' }"
        class="mt16"
      />
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存授权</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const rows = ref([])
const visible = ref(false)
const cur = ref({})
const treeRef = ref(null)
const menuTree = ref([])
const SCOPE = { ALL: '全部数据', DEPT: '本部门', ORG: '本企业', SELF: '仅本人' }

async function load() {
  rows.value = await request.get('/system/roles')
}

async function open(row) {
  cur.value = row
  if (!menuTree.value.length) {
    // forAssign=true：后端已排除「菜单管理」
    menuTree.value = await request.get('/system/menus', { params: { forAssign: true } })
  }
  visible.value = true
}

async function save() {
  const checked = treeRef.value.getCheckedKeys()
  const half = treeRef.value.getHalfCheckedKeys()
  await request.post(`/system/role/${cur.value.role.id}/menus`, { menuIds: [...checked, ...half] })
  ElMessage.success('授权已保存')
  visible.value = false
  load()
}

onMounted(load)
</script>
