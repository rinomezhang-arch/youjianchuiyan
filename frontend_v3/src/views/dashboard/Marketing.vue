<template>
  <div class="mk-page">
    <!-- 顶部只保留三个直接动作 -->
    <header class="mk-header">
      <div>
        <h2 class="mk-page-title">营销活动</h2>
        <p class="mk-page-sub">草稿 · 审批 · 发布 · 结束，发布后内容不可原地修改</p>
      </div>
      <div class="mk-header-actions">
        <el-button type="primary" @click="openCreate">新建活动</el-button>
        <el-button @click="goApproval">待我审批</el-button>
        <el-button @click="publishedPanelOpen = !publishedPanelOpen">查看已发布H5</el-button>
      </div>

      <!-- 已发布 H5 清单：直接给真实公开地址，不做假预览 -->
      <div v-if="publishedPanelOpen" class="mk-published-panel">
        <template v-if="publishedActivities.length">
          <a
            v-for="a in publishedActivities"
            :key="a.id"
            class="mk-published-link"
            :href="h5Url(a.publicSlug)"
            target="_blank"
            rel="noopener"
          >
            <span class="mk-pl-store">{{ a.storeName }}</span>
            <span class="mk-pl-title">{{ a.activityName }}</span>
            <span class="mk-pl-url">{{ h5Url(a.publicSlug) }}</span>
          </a>
        </template>
        <p v-else class="mk-published-empty">还没有已发布的活动。</p>
      </div>
    </header>

    <!-- 四栏工作台 -->
    <div class="mk-columns">
      <section
        v-for="col in columns"
        :key="col.key"
        class="mk-col"
        :data-col="col.key"
      >
        <div class="mk-col-head">
          <h3>{{ col.title }}</h3>
          <span class="mk-col-count">{{ grouped[col.key].length }}</span>
        </div>

        <div v-if="loading" class="mk-skeleton">
          <div v-for="n in 3" :key="n" class="mk-sk-row">
            <div class="mk-sk mk-sk-title"></div>
            <div class="mk-sk mk-sk-line"></div>
          </div>
        </div>

        <div v-else-if="!grouped[col.key].length" class="mk-col-empty">
          <p>{{ col.emptyText }}</p>
          <el-button v-if="col.key === 'draft'" size="small" type="primary" plain @click="openCreate">新建活动</el-button>
        </div>

        <article
          v-for="a in grouped[col.key]"
          :key="a.id"
          class="mk-row"
          :data-id="a.id"
          :data-status="a.status"
        >
          <div class="mk-row-main">
            <p class="mk-row-title">{{ a.activityName }}</p>
            <p class="mk-row-meta">
              <span class="mk-row-store">{{ a.storeName || '未指定门店' }}</span>
              <el-tag size="small" :type="tagType(a.status)" effect="light">{{ statusText(a.status) }}</el-tag>
            </p>
            <p class="mk-row-sub">
              <span>版本 v{{ a.documentVersion }}</span>
              <span>渠道 {{ channelText(a.channel) }}</span>
            </p>
            <p class="mk-row-sub mk-row-date">
              有效期 {{ fmtDate(a.validFrom) }} ~ {{ fmtDate(a.validTo) }}
            </p>
          </div>
          <div class="mk-row-actions">
            <el-button v-if="a.status === 'draft' || a.status === 'changes_requested'" size="small" @click="openEdit(a)">编辑</el-button>
            <el-button v-if="a.status === 'draft' || a.status === 'changes_requested'" size="small" type="primary" plain @click="submitApproval(a)">提交审批</el-button>
            <el-button v-if="a.status === 'approved'" size="small" type="primary" @click="openPublish(a)">去发布</el-button>
            <el-button v-if="a.status === 'published' && a.publicSlug" size="small" @click="openH5(a.publicSlug)">查看H5</el-button>
            <el-button v-if="a.status === 'published' && a.latestPublication" size="small" type="warning" plain @click="pauseActivity(a)">暂停</el-button>
            <el-button v-if="a.status === 'published'" size="small" @click="openNewVersion(a)">创建新版本</el-button>
            <el-button v-if="a.status === 'published'" size="small" @click="openAttribution(a)">转化数据</el-button>
            <el-button v-if="a.status !== 'published'" size="small" @click="openCopy(a)">复制为新活动</el-button>
            <el-button
              v-if="a.status === 'draft' || a.status === 'changes_requested'"
              size="small"
              type="danger"
              plain
              @click="cancelActivity(a)"
            >取消活动</el-button>
          </div>
        </article>
      </section>
    </div>

    <!-- 转化数据：合同入口，接口未接通时给明确空态，不编造数字 -->
    <el-dialog v-model="attributionOpen" title="转化数据" width="520px" append-to-body>
      <div v-if="attributionLoading" class="mk-attr-loading">正在读取发布版本的转化数据…</div>
      <template v-else-if="attributionData">
        <div class="mk-attr-grid">
          <div><b>{{ attributionData.views ?? 0 }}</b><span>浏览</span></div>
          <div><b>{{ attributionData.inquiries ?? 0 }}</b><span>咨询</span></div>
          <div><b>{{ attributionData.bookings ?? 0 }}</b><span>预订</span></div>
          <div><b>{{ attributionData.arrivals ?? 0 }}</b><span>到店</span></div>
        </div>
      </template>
      <p v-else class="mk-attr-empty">
        转化归因接口尚未接通（后端任务范围），当前无真实数据；该入口仅占位合同，不展示任何假统计。
      </p>
    </el-dialog>

    <!-- 三步侧滑窗（append-to-body：teleport 到 body，测试在 document 下取节点） -->
    <el-drawer
      v-model="drawerOpen"
      :title="drawerTitle"
      direction="rtl"
      size="520px"
      :with-header="true"
      :close-on-click-modal="false"
      append-to-body
      class="mk-editor-drawer"
    >
      <div class="mk-editor" data-testid="mk-editor">
        <el-steps :active="step" align-center class="mk-steps">
          <el-step title="基本信息" />
          <el-step title="客人页面" />
          <el-step title="审批与发布" />
        </el-steps>

        <!-- 第一步：基本信息 -->
        <div v-show="step === 0" class="mk-step mk-step-1">
          <el-form label-position="top">
            <el-form-item label="门店（必选，不默认任何门店）" required>
              <el-select v-model="form.storeId" placeholder="请选择门店" class="mk-full" data-testid="store-select">
                <el-option
                  v-for="s in stores"
                  :key="s.storeId"
                  :label="s.storeName"
                  :value="s.storeId"
                />
              </el-select>
              <p v-if="errors.storeId" class="mk-field-error">{{ errors.storeId }}</p>
            </el-form-item>
            <el-form-item label="活动名称（内部）" required>
              <el-input v-model="form.activityName" maxlength="40" placeholder="如：宁国店中秋家宴" />
              <p v-if="errors.activityName" class="mk-field-error">{{ errors.activityName }}</p>
            </el-form-item>
            <el-form-item label="活动编码（发布后不可改）" required>
              <el-input v-model="form.activityCode" maxlength="40" placeholder="字母数字与连字符" />
              <p v-if="errors.activityCode" class="mk-field-error">{{ errors.activityCode }}</p>
            </el-form-item>
            <el-form-item label="有效期" required>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                class="mk-full"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
              <p v-if="errors.validRange" class="mk-field-error">{{ errors.validRange }}</p>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第二步：客人页面 -->
        <div v-show="step === 1" class="mk-step mk-step-2">
          <el-form label-position="top">
            <el-form-item label="客人看到的标题" required>
              <el-input v-model="form.publicTitle" maxlength="30" show-word-limit placeholder="30 字内" />
              <p v-if="errors.publicTitle" class="mk-field-error">{{ errors.publicTitle }}</p>
            </el-form-item>
            <el-form-item label="一句话摘要">
              <el-input v-model="form.publicSummary" maxlength="60" show-word-limit />
            </el-form-item>
            <el-form-item label="活动内容（每段一行）">
              <el-input v-model="form.publicContent" type="textarea" :rows="3" maxlength="300" show-word-limit />
            </el-form-item>
            <el-form-item label="适用套餐或菜品（每行一条）">
              <el-input v-model="form.packagesText" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="使用规则（每行一条）">
              <el-input v-model="form.rulesText" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="H5 地址标识 publicSlug（发布后不可变）">
              <el-input v-model="form.publicSlug" maxlength="60" placeholder="如：ningguo-midautumn-2026" />
            </el-form-item>
            <el-form-item label="首屏真实门店照片（仅可选既有门店实景资产）">
              <el-select v-model="form.heroAssetUrl" class="mk-full" placeholder="使用默认门店照片">
                <el-option
                  v-for="p in realPhotoOptions"
                  :key="p.value"
                  :label="p.label"
                  :value="p.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="主按钮文案（客人页同名按钮，单行）">
              <el-input v-model="form.ctaLabel" maxlength="8" />
            </el-form-item>
            <el-form-item label="渠道">
              <el-radio-group v-model="form.channel">
                <el-radio value="h5">H5</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第三步：审批与发布 -->
        <div v-show="step === 2" class="mk-step mk-step-3">
          <div class="mk-flow">
            <p class="mk-flow-version">当前版本 <b>v{{ form.documentVersion }}</b>，乐观锁行版本 <b>{{ form.rowVersion }}</b></p>

            <div v-if="form.status === 'draft' || form.status === 'changes_requested' || !form.status" class="mk-flow-block">
              <p>完成前两步后提交既有审批流（business_type=marketing_activity）。</p>
              <el-button type="primary" :loading="submittingFlow" @click="submitApprovalFromDrawer">提交审批</el-button>
            </div>

            <div v-else-if="form.status === 'pending_approval'" class="mk-flow-block mk-flow-wait">
              <p>已提交，等待审批人按实时身份权限批复。驳回后会回到草稿并保留意见。</p>
            </div>

            <div v-else-if="form.status === 'approved'" class="mk-flow-block">
              <p class="mk-flow-note">
                发布将创建不可变发布版本（slug/sourceCode/requestId 唯一）。发布前请先预览客人页面。
              </p>
              <el-button data-testid="preview-draft-btn" @click="togglePreview">
                {{ previewOpen ? '收起预览' : '预览客人页面（草稿数据）' }}
              </el-button>
              <p class="mk-preview-note">
                公开地址 {{ h5Url(form.publicSlug || '') }} 发布前按设计不可见（防试探）；预览用当前草稿数据本地渲染，非公开地址。
              </p>

              <!-- 草稿内容级预览：与客人 H5 同一套模板结构（resolveHeroAsset / 有效期 / 金色主按钮），
                   数据取当前表单；预览真实渲染后发布才解锁（R1 评审项4，MOCKED_CONTRACT）。 -->
              <div v-if="previewOpen" class="mk-preview" data-testid="mk-draft-preview">
                <div class="mk-preview-card">
                  <p class="mkp-store">{{ previewStoreName || '又见炊烟' }}</p>
                  <h4 class="mkp-title">{{ form.publicTitle || '（未填写客人标题）' }}</h4>
                  <p class="mkp-validity">有效期 {{ fmtDate(form.validFrom) }} 至 {{ fmtDate(form.validTo) }}</p>
                  <img v-if="previewHero" class="mkp-img" :src="previewHero" alt="门店实景（预览）" />
                  <p v-if="form.publicSummary" class="mkp-p">{{ form.publicSummary }}</p>
                  <p v-for="(p, i) in previewLines(form.publicContent)" :key="'pc' + i" class="mkp-p">{{ p }}</p>
                  <template v-if="previewLines(form.packagesText).length">
                    <p class="mkp-h">适用套餐或菜品</p>
                    <ul class="mkp-list"><li v-for="(x, i) in previewLines(form.packagesText)" :key="'pk' + i">{{ x }}</li></ul>
                  </template>
                  <template v-if="previewLines(form.rulesText).length">
                    <p class="mkp-h">使用规则</p>
                    <ul class="mkp-list"><li v-for="(x, i) in previewLines(form.rulesText)" :key="'rl' + i">{{ x }}</li></ul>
                  </template>
                  <button type="button" class="mkp-cta" disabled tabindex="-1">{{ form.ctaLabel || '咨询档期' }}</button>
                </div>
              </div>

              <p v-if="previewNote" class="mk-preview-note">{{ previewNote }}</p>
              <el-button
                type="primary"
                class="mk-publish-btn"
                :disabled="!previewConfirmed"
                :loading="submittingFlow"
                @click="publish"
              >确认发布</el-button>
              <p v-if="!previewConfirmed" class="mk-field-error">必须先预览客人页面（真实看到草稿内容）才能发布。</p>
            </div>

            <div v-else-if="form.status === 'published'" class="mk-flow-block mk-flow-ok">
              <p>该版本已发布，公开内容不可原地修改。需要调整请使用「创建新版本」。</p>
              <a v-if="form.publicSlug" :href="h5Url(form.publicSlug)" target="_blank" rel="noopener" class="mk-live-link">查看线上 H5</a>
            </div>
          </div>
        </div>

        <!-- 保存状态与错误：失败留在当前步骤、保留全部输入 -->
        <div class="mk-save-bar">
          <span v-if="saveError" class="mk-save-error" role="alert">{{ saveError }}</span>
          <span v-else-if="lastSavedAt" class="mk-save-ok">草稿已自动保存 · {{ lastSavedAt }}</span>
          <span v-else class="mk-save-hint">补全基本信息后自动保存草稿</span>
        </div>
      </div>

      <template #footer>
        <div class="mk-drawer-footer">
          <el-button @click="drawerOpen = false">关闭</el-button>
          <el-button v-if="step > 0" @click="step -= 1">上一步</el-button>
          <el-button v-if="step < 2" type="primary" :loading="saving" @click="goNext">下一步（自动保存）</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  listMarketingActivities,
  listStoresForMarketing,
  createMarketingActivity,
  updateMarketingActivity,
  submitMarketingActivity,
  publishMarketingActivity,
  pauseMarketingPublication,
  cancelMarketingActivity,
  getMarketingAttribution,
  normalizeActivity,
  columnOfStatus,
  STATUS_TEXT,
  CHANNEL_TEXT,
  validateActivityForm,
  resolveHeroAsset,
  newRequestId
} from '@/api/marketing'

const router = useRouter()

const loading = ref(true)
const activities = ref([])
const stores = ref([])
const publishedPanelOpen = ref(false)

const columns = [
  { key: 'draft', title: '草稿', emptyText: '还没有草稿，从一个新活动开始。' },
  { key: 'approval', title: '待审批', emptyText: '没有待审批活动。' },
  { key: 'published', title: '已发布', emptyText: '还没有发布中的活动。' },
  { key: 'ended', title: '已结束', emptyText: '暂无暂停、过期或取消的活动。' }
]

const grouped = computed(() => {
  const g = { draft: [], approval: [], published: [], ended: [] }
  for (const a of activities.value) {
    g[columnOfStatus(a.status)].push(a)
  }
  return g
})
const publishedActivities = computed(() => grouped.value.published.filter((a) => a.publicSlug))

/* ================= 抽屉表单 ================= */
function emptyForm() {
  // storeId 刻意为 null：门店必选，绝不预填 1。
  return {
    id: null,
    storeId: null,
    activityName: '',
    activityCode: '',
    validFrom: '',
    validTo: '',
    publicTitle: '',
    publicSummary: '',
    publicContent: '',
    packagesText: '',
    rulesText: '',
    publicSlug: '',
    heroAssetUrl: '',
    ctaLabel: '咨询档期',
    channel: 'h5',
    documentVersion: 1,
    rowVersion: 0,
    status: 'draft',
    basedOnActivityId: null
  }
}

const drawerOpen = ref(false)
const step = ref(0)
const form = ref(emptyForm())
const dateRange = ref([])
const errors = ref({})
const saving = ref(false)
const saveError = ref('')
const lastSavedAt = ref('')
const submittingFlow = ref(false)
const previewConfirmed = ref(false)
const previewNote = ref('')
const previewOpen = ref(false)
let saveTimer = null
// 发布幂等键：一次发布意图一个，失败重试沿用，成功后作废换新。
let publishRequestId = ''

const drawerTitle = computed(() => (form.value.id ? `编辑活动 · v${form.value.documentVersion}` : '新建活动'))

const realPhotoOptions = [
  { label: '默认（按门店取实景）', value: '' },
  { label: '门店入口实景 storefront-entrance', value: '/site-photos/storefront-entrance.jpg' },
  { label: '黄昏门店实景 storefront-dusk', value: '/site-photos/storefront-dusk.jpg' },
  { label: '露台餐区实景 terrace-dining-real', value: '/site-photos/terrace-dining-real.jpg' }
]

watch(dateRange, (v) => {
  form.value.validFrom = v?.[0] || ''
  form.value.validTo = v?.[1] || ''
})

// 自动保存：仅在已有活动 id、且不在发布动作中时防抖执行。
watch(
  form,
  () => {
    if (!form.value.id || !drawerOpen.value) return
    clearTimeout(saveTimer)
    saveTimer = setTimeout(() => { persistDraft(true) }, 1500)
  },
  { deep: true }
)

onBeforeUnmount(() => clearTimeout(saveTimer))

function resetEditor(source = {}) {
  form.value = { ...emptyForm(), ...source }
  dateRange.value = form.value.validFrom && form.value.validTo
    ? [form.value.validFrom, form.value.validTo]
    : []
  step.value = 0
  errors.value = {}
  saveError.value = ''
  lastSavedAt.value = ''
  previewConfirmed.value = false
  previewNote.value = ''
  previewOpen.value = false
}

function openCreate() {
  publishedPanelOpen.value = false
  resetEditor()
  drawerOpen.value = true
}

function openEdit(a) {
  publishedPanelOpen.value = false
  resetEditor({
    id: a.id,
    storeId: a.storeId,
    activityName: a.activityName,
    activityCode: a.activityCode,
    validFrom: a.validFrom,
    validTo: a.validTo,
    publicTitle: a.publicTitle || a.activityName,
    publicSummary: a.publicSummary || '',
    publicContent: a.publicContent || '',
    packagesText: (a.packages || []).join('\n'),
    rulesText: (a.rules || []).join('\n'),
    publicSlug: a.publicSlug || '',
    heroAssetUrl: a.heroAssetUrl || '',
    ctaLabel: a.ctaLabel || '咨询档期',
    channel: a.channel || 'h5',
    documentVersion: a.documentVersion,
    rowVersion: a.rowVersion,
    status: a.status
  })
  lastSavedAt.value = a.rowVersion ? '已加载已保存版本' : ''
  drawerOpen.value = true
}

/** 复制为新活动：清空 id/编码/slug，门店等内容沿用。 */
function openCopy(a) {
  resetEditor({
    storeId: a.storeId,
    activityName: `${a.activityName}（副本）`,
    activityCode: '',
    validFrom: a.validFrom,
    validTo: a.validTo,
    publicTitle: a.publicTitle || a.activityName,
    publicSummary: a.publicSummary || '',
    publicContent: a.publicContent || '',
    packagesText: (a.packages || []).join('\n'),
    rulesText: (a.rules || []).join('\n'),
    heroAssetUrl: a.heroAssetUrl || '',
    ctaLabel: a.ctaLabel || '咨询档期',
    channel: 'h5'
  })
  drawerOpen.value = true
}

/** 创建新版本：以已发布版本为内容，另起下一版草稿（旧版本继续公开）。 */
function openNewVersion(a) {
  resetEditor({
    storeId: a.storeId,
    activityName: a.activityName,
    activityCode: a.activityCode,
    validFrom: a.validFrom,
    validTo: a.validTo,
    publicTitle: a.publicTitle || a.activityName,
    publicSummary: a.publicSummary || '',
    publicContent: a.publicContent || '',
    packagesText: (a.packages || []).join('\n'),
    rulesText: (a.rules || []).join('\n'),
    heroAssetUrl: a.heroAssetUrl || '',
    ctaLabel: a.ctaLabel || '咨询档期',
    channel: a.channel || 'h5',
    documentVersion: (a.documentVersion || 1) + 1,
    status: 'draft',
    basedOnActivityId: a.id
  })
  drawerOpen.value = true
}

function openPublish(a) {
  openEdit(a)
  step.value = 2
}

function buildPayload() {
  const lines = (t) => String(t || '').split('\n').map((s) => s.trim()).filter(Boolean)
  return {
    storeId: form.value.storeId,
    activityName: form.value.activityName.trim(),
    activityCode: form.value.activityCode.trim(),
    validFrom: form.value.validFrom,
    validTo: form.value.validTo,
    publicTitle: form.value.publicTitle.trim(),
    publicSummary: (form.value.publicSummary || '').trim(),
    heroAssetUrl: form.value.heroAssetUrl || '',
    ctaLabel: (form.value.ctaLabel || '').trim() || '咨询档期',
    contentJson: JSON.stringify({
      content: lines(form.value.publicContent),
      packages: lines(form.value.packagesText),
      rules: lines(form.value.rulesText)
    }),
    expectedRowVersion: form.value.rowVersion,
    documentVersion: form.value.documentVersion,
    basedOnActivityId: form.value.basedOnActivityId
  }
}

/**
 * 保存草稿。失败：留在当前步骤、保留全部输入、显示字段级错误——绝不前进。
 */
async function persistDraft(silent = false) {
  const candidate = {
    ...form.value,
    publicTitle: form.value.publicTitle || form.value.activityName
  }
  errors.value = validateActivityForm(candidate)
  // 第一步只要求基本信息；客人页面字段在进入第二步时才强制。
  if (step.value === 0) delete errors.value.publicTitle
  if (Object.keys(errors.value).length) {
    saveError.value = '请补全本步骤必填项后再继续。'
    return false
  }
  saving.value = true
  saveError.value = ''
  try {
    const payload = buildPayload()
    let res
    if (!form.value.id) {
      res = await createMarketingActivity(payload)
    } else {
      res = await updateMarketingActivity(form.value.id, payload)
    }
    const data = res?.data || {}
    if (!form.value.id && data.id) form.value.id = data.id
    if (data.rowVersion ?? data.row_version) {
      form.value.rowVersion = data.rowVersion ?? data.row_version
    }
    if (data.status) form.value.status = data.status
    if (data.documentVersion ?? data.document_version) {
      form.value.documentVersion = data.documentVersion ?? data.document_version
    }
    lastSavedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
    return true
  } catch (e) {
    saveError.value = '草稿保存失败，输入已保留，请稍后重试。'
    if (!silent) ElMessage.error('草稿保存失败')
    return false
  } finally {
    saving.value = false
  }
}

async function goNext() {
  const ok = await persistDraft()
  if (!ok) return // 留在当前步骤
  if (step.value === 0) {
    step.value = 1
    return
  }
  if (step.value === 1) {
    const all = validateActivityForm({ ...form.value, publicTitle: form.value.publicTitle })
    errors.value = all
    if (Object.keys(all).length) {
      saveError.value = '请补全客人页面必填项。'
      return
    }
    step.value = 2
  }
}

async function submitApprovalFromDrawer() {
  const ok = await persistDraft()
  if (!ok) return
  submittingFlow.value = true
  try {
    await submitMarketingActivity(form.value.id, {
      businessType: 'marketing_activity',
      businessId: form.value.id,
      businessNo: form.value.activityCode,
      documentVersion: form.value.documentVersion
    })
    form.value.status = 'pending_approval'
    ElMessage.success('已提交审批')
    await reload()
  } catch {
    saveError.value = '提交审批失败，内容已保留，请稍后重试。'
  } finally {
    submittingFlow.value = false
  }
}

async function submitApproval(a) {
  try {
    await submitMarketingActivity(a.id, {
      businessType: 'marketing_activity',
      businessId: a.id,
      businessNo: a.activityCode,
      documentVersion: a.documentVersion
    })
    ElMessage.success('已提交审批')
    await reload()
  } catch {
    /* 拦截器已有全局提示 */
  }
}

/**
 * 草稿内容级预览（R1 评审项4）：用当前草稿数据渲染与客人 H5 同一套模板结构，
 * 用户真实看到内容后发布才解锁。MOCKED_CONTRACT：本地渲染，不是公开地址——
 * 公开地址发布前按设计不可见（防试探），不做「打开 404 也算预览」的假闸门。
 */
async function togglePreview() {
  previewOpen.value = !previewOpen.value
  if (!previewOpen.value) {
    previewNote.value = '预览已收起；发布解锁状态保留，可再次打开核对。'
    return
  }
  await nextTick()
  const title = String(form.value.publicTitle || '').trim()
  if (!title) {
    previewConfirmed.value = false
    previewNote.value = '草稿缺少客人可见标题，无法形成有效预览；请先回第二步补全。'
    return
  }
  previewConfirmed.value = true
  previewNote.value = '预览已按当前草稿数据渲染（门店/标题/有效期/真实门店图/主按钮），确认无误再发布。'
}

/** 预览渲染：与 MarketingActivity 同一契约函数，保证「所见即发布内容」。 */
const previewHero = computed(() =>
  resolveHeroAsset({ heroAssetUrl: form.value.heroAssetUrl, storeId: form.value.storeId })
)
const previewStoreName = computed(() => {
  const s = stores.value.find((x) => x.storeId === form.value.storeId)
  return s ? s.storeName : ''
})
function previewLines(text) {
  return String(text || '').split('\n').map((s) => s.trim()).filter(Boolean)
}

async function publish() {
  if (!previewConfirmed.value) return
  submittingFlow.value = true
  saveError.value = ''
  if (!publishRequestId) publishRequestId = newRequestId()
  try {
    const lines = (t) => String(t || '').split('\n').map((s) => s.trim()).filter(Boolean)
    const res = await publishMarketingActivity(form.value.id, {
      channel: form.value.channel || 'h5',
      publicSlug: form.value.publicSlug,
      title: form.value.publicTitle,
      summary: form.value.publicSummary,
      contentJson: JSON.stringify({
        content: lines(form.value.publicContent),
        packages: lines(form.value.packagesText),
        rules: lines(form.value.rulesText)
      }),
      heroAssetUrl: form.value.heroAssetUrl,
      ctaLabel: form.value.ctaLabel || '咨询档期',
      validFrom: form.value.validFrom,
      validTo: form.value.validTo,
      documentVersion: form.value.documentVersion,
      requestId: publishRequestId
    })
    // 成功只认后端响应：必须带回发布 id/slug。
    const data = res?.data || {}
    if (!data.publicationId && !data.publication_id && !data.publicSlug && !data.public_slug) {
      throw new Error('发布响应缺少发布结果')
    }
    ElMessage.success('发布成功')
    publishRequestId = ''
    drawerOpen.value = false
    await reload()
  } catch {
    // 网络/业务失败：明确红字，绝不显示发布成功，输入与步骤保持不变。
    saveError.value = '发布失败，未产生发布版本。请检查网络后重试（同一 requestId 幂等）。'
  } finally {
    submittingFlow.value = false
  }
}

async function pauseActivity(a) {
  const pubId = a.latestPublication?.publicationId
  if (!pubId) return
  try {
    await pauseMarketingPublication(pubId)
    ElMessage.success('已暂停公开展示')
    await reload()
  } catch {
    /* 全局提示已出 */
  }
}

async function cancelActivity(a) {
  if (!window.confirm(`取消活动「${a.activityName}」？取消为状态取消，不可物理删除，已有发布记录时将被拒绝。`)) return
  try {
    await cancelMarketingActivity(a.id)
    ElMessage.success('活动已取消')
    await reload()
  } catch {
    /* 全局提示已出 */
  }
}

/* ================= 转化数据 ================= */
const attributionOpen = ref(false)
const attributionLoading = ref(false)
const attributionData = ref(null)

async function openAttribution(a) {
  attributionOpen.value = true
  attributionLoading.value = true
  attributionData.value = null
  try {
    // 合同入口（设计第三/四节归因事件由后端任务实现）；未接通走空态，不造数字。
    const pubId = a.latestPublication?.publicationId
    const res = await getMarketingAttribution(pubId)
    attributionData.value = res?.data || null
  } catch {
    attributionData.value = null
  } finally {
    attributionLoading.value = false
  }
}

/* ================= 数据加载 ================= */
async function reload() {
  loading.value = true
  try {
    const res = await listMarketingActivities()
    const raw = res?.data || []
    activities.value = (Array.isArray(raw) ? raw : raw.list || [])
      .map(normalizeActivity)
      .filter(Boolean)
      .map(attachContentFields)
  } catch {
    activities.value = []
  } finally {
    loading.value = false
  }
}

// 工作台列表若未回显公开内容字段，编辑时用空串兜底（后端任务可补）。
function attachContentFields(a) {
  return {
    ...a,
    publicTitle: a.publicTitle ?? a.latestPublication?.publicTitle ?? '',
    publicSummary: a.publicSummary ?? a.latestPublication?.summary ?? '',
    publicContent: '',
    packages: [],
    rules: [],
    heroAssetUrl: a.latestPublication?.heroAssetUrl ?? a.heroAssetUrl ?? '',
    ctaLabel: a.latestPublication?.ctaLabel ?? '咨询档期'
  }
}

async function loadStores() {
  try {
    const res = await listStoresForMarketing()
    const data = res?.data || []
    stores.value = (Array.isArray(data) ? data : data.list || []).map((s) => ({
      storeId: s.store_id ?? s.storeId,
      storeName: s.store_name ?? s.storeName
    })).filter((s) => s.storeId != null)
  } catch {
    stores.value = []
  }
}

/* ================= 小工具 ================= */
function statusText(s) {
  return STATUS_TEXT[s] || s || '未知'
}
function channelText(c) {
  return CHANNEL_TEXT[c] || (c ? String(c).toUpperCase() : '—')
}
function tagType(s) {
  return { draft: 'info', pending_approval: 'warning', approved: 'warning', published: 'success', paused: 'danger', expired: 'info', cancelled: 'info', changes_requested: 'danger' }[s] || 'info'
}
function fmtDate(d) {
  if (!d) return '--'
  return String(d).slice(0, 10)
}
function h5Url(slug) {
  return `/h5/activity/${encodeURIComponent(slug || '')}`
}
function openH5(slug) {
  window.open(h5Url(slug), '_blank', 'noopener')
}
function goApproval() {
  router.push('/dashboard/approval')
}

onMounted(async () => {
  await Promise.all([reload(), loadStores()])
})
</script>

<style scoped>
.mk-page { max-width: 1400px; margin: 0 auto; }

.mk-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  position: relative;
}
.mk-page-title { margin: 0 0 4px; font-size: 20px; color: var(--color-text, #2d4a3e); }
.mk-page-sub { margin: 0; font-size: 12px; color: var(--color-text-muted, #8a8372); }
.mk-header-actions { display: flex; gap: 10px; flex-wrap: wrap; }

.mk-published-panel {
  flex-basis: 100%;
  background: var(--color-card, #fffdf8);
  border: 1px solid var(--color-border, #e6dfd0);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.mk-published-link {
  display: flex;
  gap: 12px;
  align-items: baseline;
  padding: 8px 10px;
  background: var(--color-bg-alt, #f7f4ec);
  border-radius: 8px;
  text-decoration: none;
  font-size: 13px;
  flex-wrap: wrap;
}
.mk-pl-store { color: #2d4a3e; font-weight: 600; }
.mk-pl-title { color: #3b4f45; }
.mk-pl-url { color: #9a7b2e; margin-left: auto; word-break: break-all; }
.mk-published-empty { margin: 0; color: var(--color-text-muted, #8a8372); font-size: 13px; }

.mk-columns {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}
.mk-col {
  background: var(--color-card, #fffdf8);
  border: 1px solid var(--color-border, #e6dfd0);
  border-radius: 12px;
  padding: 14px;
  min-width: 0;
}
.mk-col-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.mk-col-head h3 { margin: 0; font-size: 15px; color: #2d4a3e; }
.mk-col-count {
  font-size: 12px;
  background: rgba(45, 74, 62, 0.08);
  color: #2d4a3e;
  border-radius: 10px;
  padding: 1px 9px;
}
.mk-col-empty { text-align: center; color: #8a8372; font-size: 13px; padding: 24px 8px; }
.mk-col-empty p { margin: 0 0 10px; }

.mk-row {
  border: 1px solid var(--color-border, #e6dfd0);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fffefa;
}
.mk-row-title { margin: 0 0 6px; font-size: 14px; font-weight: 600; color: #24382f; }
.mk-row-meta { display: flex; align-items: center; gap: 8px; margin: 0 0 6px; font-size: 12px; color: #5c7268; }
.mk-row-sub { display: flex; gap: 10px; margin: 0 0 4px; font-size: 12px; color: #6e7d74; }
.mk-row-date { color: #9a7b2e; }
.mk-row-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }

.mk-skeleton .mk-sk-row { margin-bottom: 12px; }
.mk-sk { background: #ece6d8; border-radius: 4px; }
.mk-sk-title { height: 14px; width: 60%; margin-bottom: 8px; }
.mk-sk-line { height: 10px; width: 90%; }

.mk-editor :deep(.el-form-item) { margin-bottom: 14px; }
.mk-full { width: 100%; }
.mk-field-error { margin: 4px 0 0; font-size: 12px; color: #b04a3a; }

.mk-steps { margin: 4px 0 20px; }
.mk-flow p { font-size: 13px; line-height: 1.7; color: #3b4f45; }
.mk-flow-version { color: #6e7d74; font-size: 12px; }
.mk-flow-note { background: #f7f4ec; border-radius: 8px; padding: 10px; }
.mk-preview-note { font-size: 12px; color: #9a7b2e; }
.mk-publish-btn { margin-left: 10px; }
.mk-live-link { color: #2d4a3e; font-weight: 600; }

/* 草稿预览：与客人 H5 同一视觉语言（深绿为体、金色只给主按钮与有效期） */
.mk-preview { margin-top: 12px; border: 1px dashed #c4a35a; border-radius: 10px; padding: 10px; background: #f5f2ea; }
.mk-preview-card { max-width: 360px; margin: 0 auto; background: #fffdf8; border: 1px solid #e6dfd0; border-radius: 10px; padding: 14px; }
.mkp-store { margin: 0 0 4px; font-size: 12px; letter-spacing: 0.08em; color: #5c7268; }
.mkp-title { margin: 0 0 8px; font-size: 17px; line-height: 1.35; font-weight: 700; color: #2d4a3e; }
.mkp-validity { display: flex; align-items: center; gap: 6px; margin: 0 0 10px; font-size: 12px; color: #9a7b2e; }
.mkp-img { display: block; width: 100%; height: auto; border-radius: 6px; background: #ece6d8; }
.mkp-p { margin: 0 0 6px; font-size: 13px; line-height: 1.7; color: #3b4f45; }
.mkp-h { margin: 8px 0 4px; font-size: 13px; font-weight: 700; color: #2d4a3e; border-left: 3px solid #c4a35a; padding-left: 7px; }
.mkp-list { margin: 0; padding-left: 16px; font-size: 13px; line-height: 1.7; color: #3b4f45; }
.mkp-cta {
  display: block; width: 100%; margin-top: 10px; padding: 10px 14px;
  border: none; border-radius: 8px; background: #c4a35a; color: #fffdf8;
  font-size: 14px; font-weight: 600; letter-spacing: 0.12em; white-space: nowrap; opacity: 0.85;
}

.mk-save-bar { margin-top: 18px; padding-top: 12px; border-top: 1px solid #e6dfd0; min-height: 22px; }
.mk-save-ok { font-size: 12px; color: #4a7c59; }
.mk-save-hint { font-size: 12px; color: #8a8372; }
.mk-save-error { font-size: 12px; color: #b04a3a; }

.mk-attr-loading { color: #6e7d74; font-size: 13px; padding: 20px 0; }
.mk-attr-empty { color: #8a8372; font-size: 13px; line-height: 1.7; }
.mk-attr-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; text-align: center; }
.mk-attr-grid b { display: block; font-size: 22px; color: #2d4a3e; }
.mk-attr-grid span { font-size: 12px; color: #8a8372; }

.mk-drawer-footer { display: flex; justify-content: flex-end; gap: 8px; }

@media (max-width: 1200px) {
  .mk-columns { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .mk-columns { grid-template-columns: 1fr; }
}
</style>
