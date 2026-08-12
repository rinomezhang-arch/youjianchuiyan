<template>
  <div class="tags-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">标签管理 · Tags</h2>
        <p class="page-subtitle">菜品标签 · 口味标签 · 特征标签</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" @click="openAddTag">+ 新增标签</el-button>
      </div>
    </div>
    <div class="tag-group-tabs">
      <div v-for="group in tagGroups" :key="group.key" :class="['group-tab', { active: activeGroup === group.key }]" @click="activeGroup = group.key">
        <span>{{ group.label }}</span>
        <span class="group-count">{{ getTagsByGroup(group.key).length }}</span>
      </div>
    </div>
    <div class="tags-container">
      <div class="tags-grid">
        <div v-for="tag in currentTags" :key="tag.id" class="tag-item" :style="{ '--tag-color': tag.color || '#2D4A3E' }">
          <div class="tag-badge">
            <span class="tag-dot"></span>
            <span class="tag-name">{{ tag.name }}</span>
            <span class="tag-name-en" v-if="tag.nameEn">{{ tag.nameEn }}</span>
          </div>
          <div class="tag-count">{{ tag.dishCount || 0 }}道菜</div>
          <div class="tag-actions">
            <el-button text size="small" @click="editTag(tag)">编辑</el-button>
            <el-button text size="small" type="danger" @click="deleteTag(tag)">删除</el-button>
          </div>
        </div>
        <div class="tag-add-placeholder" @click="openAddTag">
          <span>添加标签</span>
        </div>
      </div>
    </div>
    <el-dialog v-model="showDialog" :title="editing ? '编辑标签' : '新增标签'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标签名称" required><el-input v-model="form.name" placeholder="请输入标签名称" maxlength="20" /></el-form-item>
        <el-form-item label="英文名"><el-input v-model="form.nameEn" placeholder="English name" /></el-form-item>
        <el-form-item label="标签分组">
          <el-select v-model="form.group" class="full-width">
            <el-option v-for="g in tagGroups" :key="g.key" :label="g.label" :value="g.key" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签颜色">
          <div class="color-picker-row">
            <div v-for="color in presetColors" :key="color" :class="['color-swatch', { selected: form.color === color }]" :style="{ background: color }" @click="form.color = color" />
            <el-color-picker v-model="form.color" />
          </div>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const activeGroup = ref('taste')
const showDialog = ref(false)
const editing = ref(false)
const allTags = ref([])

const tagGroups = [
  { key: 'taste', label: '口味标签' },
  { key: 'feature', label: '特征标签' },
  { key: 'allergy', label: '过敏原' },
  { key: 'diet', label: '饮食类型' },
  { key: 'cook', label: '烹饪方式' }
]

const presetColors = ['#2D4A3E', '#4A7C59', '#C4A35A', '#C25555', '#5B7B8A', '#8B5E3C', '#6B4C8A', '#D4A853']

const form = ref({ id: '', name: '', nameEn: '', group: 'taste', color: '#2D4A3E', sort: 0 })

const currentTags = computed(() => getTagsByGroup(activeGroup.value))

function getTagsByGroup(group) {
  return allTags.value.filter(t => t.group === group)
}

async function fetchTags() {
  try {
    const res = await request.get('/tags')
    if (res.data) allTags.value = res.data
  } catch (e) {
    console.error('Failed to fetch tags:', e)
  }
}

function openAddTag() {
  editing.value = false
  form.value = { id: '', name: '', nameEn: '', group: activeGroup.value, color: '#2D4A3E', sort: 0 }
  showDialog.value = true
}

function editTag(tag) {
  editing.value = true
  form.value = { ...tag }
  showDialog.value = true
}

async function saveTag() {
  if (!form.value.name) { ElMessage.warning('请输入标签名称'); return }
  try {
    const res = editing.value
      ? await request.put(`/tags/${form.value.id}`, form.value)
      : await request.post('/tags', form.value)
    if (res.code === 200) { ElMessage.success('保存成功'); showDialog.value = false; fetchTags() }
  } catch (e) {
    const newTag = { ...form.value, id: Date.now(), dishCount: 0 }
    if (editing.value) {
      const idx = allTags.value.findIndex(t => t.id === form.value.id)
      if (idx >= 0) allTags.value[idx] = newTag
    } else {
      allTags.value.push(newTag)
    }
    ElMessage.success('已保存（本地）')
    showDialog.value = false
  }
}

async function deleteTag(tag) {
  try {
    await ElMessageBox.confirm(`确定删除标签"${tag.name}"？`, '确认删除', { type: 'warning' })
    allTags.value = allTags.value.filter(t => t.id !== tag.id)
    ElMessage.success('已删除')
  } catch (e) { /* cancel */ }
}

onMounted(fetchTags)
</script>

<style scoped>
.tags-page { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.tag-group-tabs { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.group-tab { display: flex; align-items: center; gap: 6px; padding: 10px 18px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; transition: var(--transition); font-size: 13px; font-weight: 500; }
.group-tab:hover { border-color: var(--color-accent); }
.group-tab.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
.group-icon { font-size: 16px; }
.group-count { padding: 1px 8px; border-radius: 10px; font-size: 11px; background: rgba(0,0,0,0.1); }
.group-tab.active .group-count { background: rgba(255,255,255,0.2); }
.tags-container { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 20px; }
.tags-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.tag-item { display: flex; align-items: center; gap: 10px; padding: 12px 16px; background: var(--color-bg-alt); border-radius: var(--radius-md); transition: var(--transition); }
.tag-item:hover { transform: translateX(4px); background: rgba(196, 163, 90, 0.06); }
.tag-badge { display: flex; align-items: center; gap: 8px; flex: 1; }
.tag-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--tag-color); flex-shrink: 0; }
.tag-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.tag-name-en { font-size: 11px; color: var(--color-text-muted); }
.tag-count { font-size: 11px; color: var(--color-text-muted); }
.tag-actions { display: flex; gap: 2px; opacity: 0; transition: opacity 0.2s; }
.tag-item:hover .tag-actions { opacity: 1; }
.tag-add-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 20px; border: 2px dashed var(--color-border); border-radius: var(--radius-md); cursor: pointer; color: var(--color-text-muted); font-size: 13px; transition: all 0.2s; min-height: 60px; }
.tag-add-placeholder:hover { border-color: var(--color-accent); color: var(--color-accent-dark); }
.full-width { width: 100%; }
.color-picker-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.color-swatch { width: 28px; height: 28px; border-radius: 50%; cursor: pointer; border: 2px solid transparent; transition: all 0.2s; }
.color-swatch:hover { transform: scale(1.1); }
.color-swatch.selected { border-color: var(--color-text); box-shadow: 0 0 0 2px var(--color-bg), 0 0 0 4px var(--color-text); }
</style>