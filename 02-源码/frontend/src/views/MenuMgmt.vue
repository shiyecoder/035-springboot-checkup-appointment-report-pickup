<template>
  <div class="page">
    <div class="panel">
      <div class="panel-h">
        <h3>菜单管理</h3>
        <span class="sub">维护系统菜单与权限标识；「为角色分配菜单」时本页不会出现在可选列表中</span>
        <div class="right"><el-button type="primary" @click="open()">+ 新增菜单</el-button></div>
      </div>
      <el-table :data="tree" row-key="id" border default-expand-all
                :tree-props="{ children: 'children' }" size="small">
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由" min-width="180" />
        <el-table-column prop="perm" label="权限标识" min-width="160" />
        <el-table-column prop="menuType" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.menuType === 'M' ? 'info' : 'success'">
              {{ row.menuType === 'M' ? '目录' : '菜单' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="right" />
        <el-table-column label="显示" width="80">
          <template #default="{ row }">{{ row.visible ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑菜单' : '新增菜单'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="上级菜单">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="顶级菜单" :value="0" />
            <el-option v-for="m in tree" :key="m.id" :label="m.menuName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" required><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="路由"><el-input v-model="form.path" placeholder="/xxx" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.perm" placeholder="如 package:list" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.menuType">
            <el-radio label="M">目录</el-radio>
            <el-radio label="C">菜单</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const tree = ref([])
const visible = ref(false)
const form = reactive({ id: null, parentId: 0, menuName: '', path: '', icon: '', perm: '', menuType: 'C', sort: 10, visible: 1 })

async function load() {
  const flat = await request.get('/system/menu/list')
  const parents = flat.filter((m) => m.parentId === 0)
  tree.value = parents.map((p) => ({ ...p, children: flat.filter((c) => c.parentId === p.id) }))
}

function open(row) {
  Object.assign(form, row || { id: null, parentId: 0, menuName: '', path: '', icon: '', perm: '', menuType: 'C', sort: 10, visible: 1 })
  visible.value = true
}

async function save() {
  if (!form.menuName) return ElMessage.warning('请填写菜单名称')
  await request.post('/system/menu', form)
  ElMessage.success('已保存')
  visible.value = false
  load()
}

onMounted(load)
</script>
