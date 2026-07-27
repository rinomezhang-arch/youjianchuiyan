<template>
  <el-dialog v-model="visible" width="900px" class="bk-dlg" :close-on-click-modal="false" :show-close="false" @opened="onOpened">
    <template #header>
      <div class="bk-header" @dblclick="onHeaderDblClick">
        <div class="bk-header-left">
          <div class="bk-header-icon">
            <el-icon><KnifeFork /></el-icon>
          </div>
          <div class="bk-header-text">
            <h2 class="bk-header-title">
              <!-- 新建：无预订信息 -->
              <template v-if="!isEdit">{{ t('booking.title') }}</template>
              <!-- 变更：有预订信息且编辑中 -->
              <template v-else-if="!readonly">变更预订<span class="bk-breathe">（变更中 · Modifying）</span></template>
              <!-- 只读查看：有预订信息 -->
              <template v-else>{{ t('booking.detail') }}</template>
            </h2>
            <div class="bk-header-meta">
              <span class="bk-header-meta-item">
                <span class="bk-header-meta-label">单号</span>
                <span class="bk-header-meta-val">{{ displayBookingId }}</span>
              </span>
              <span class="bk-header-meta-sep"></span>
              <span class="bk-header-meta-item">
                <span class="bk-header-meta-label">🔒 创建</span>
                <span class="bk-header-meta-val bk-created-time">{{ displayCreatedAt }}</span>
              </span>
            </div>
          </div>
        </div>
        <div class="bk-header-right">
          <!-- 订单状态徽章：已确定/未确定（有预订时始终显示） -->
          <span class="bk-status-badge" v-if="isEdit">
            <el-icon><Check /></el-icon>
            <template v-if="form.booking_status === 'confirmed' || form.booking_status === '已确定'">已确定 · Confirmed</template>
            <template v-else>未确定 · Pending</template>
          </span>
          <!-- 变更提醒：编辑模式 -->
          <div v-if="!readonly && isEdit" class="bk-change-alert">
            <el-icon><Edit /></el-icon>
            <span>变更中 · Modifying</span>
          </div>
          
          <button class="bk-close-btn" @click="visible = false" aria-label="关闭">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>
    </template>

    <!-- 标签页导航 -->
    <nav class="bk-tabs">
      <button
        v-for="tb in tabs"
        :key="tb.key"
        :class="['bk-tab', { active: activeTab === tb.key }]"
        @click="activeTab = tb.key"
      >
        {{ tb.label }}
        <span class="bk-tab-en">{{ tb.en }}</span>
      </button>
    </nav>

    <!-- 选中桌台信息回显区：多选时显示第一个选中台号 -->
    <div v-if="selectedTables.length > 0" class="bk-selected-summary">
      <div class="bk-summary-primary">
        <span class="bk-summary-label">主选桌台 · Primary:</span>
        <span class="bk-summary-value chip-primary" style="display:inline-flex;align-items:center;gap:4px;">
          {{ selectedTables[0].table_number || selectedTables[0].table_name || '未知' }}
        </span>
        <span v-if="selectedTables[0].table_area" class="bk-summary-area">
          {{ selectedTables[0].table_area }}
        </span>
        <span v-if="selectedTables[0].table_capacity" class="bk-summary-capacity">
          可容 {{ selectedTables[0].table_capacity }} 人
        </span>
      </div>
      <div v-if="selectedTables.length > 1" class="bk-summary-more">
        +{{ selectedTables.length - 1 }} 个桌台 · 共 {{ selectedTables.length }} 桌
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="bk-body" @dblclick="onBodyDblClick">
      <!-- ============ Tab 1: 预订时间 ============ -->
      <div v-show="activeTab === 'basic'" class="bk-basic">
        <!-- 表单区 -->
        <div class="bk-form-wrap">
          <!-- 上半区：基础信息 -->
          <div class="bk-form-section bk-form-section-top">
            <div class="bk-form-section-title">
              <span class="bk-form-section-dot"></span>
              <span>基础信息</span>
              <span class="bk-form-section-en">· Basic Info</span>
            </div>
            <!-- 餐别独立一行 -->
            <div class="bk-field bk-field-meal-row">
              <label class="bk-label">餐别 <span class="bk-label-en">· Meal</span></label>
              <div class="bk-meal-switch">
                <button
                  :class="['bk-meal-btn', { active: mealPeriod === 'lunch' }]"
                  @click="setMeal('lunch')"
                  :disabled="readonly"
                >午餐 · Lunch</button>
                <button
                  :class="['bk-meal-btn', { active: mealPeriod === 'dinner' }]"
                  @click="setMeal('dinner')"
                  :disabled="readonly"
                >晚餐 · Dinner</button>
              </div>
            </div>
            <div class="bk-form-grid bk-form-grid-top">
              <!-- 行1：日期 | 时间 | 预定员 | 部门 -->
              <div class="bk-field">
                <label class="bk-label">日期 <span class="bk-label-en">· Date</span></label>
                <el-date-picker v-model="form.booking_date" type="date" value-format="YYYY-MM-DD" :placeholder="`${t('booking.date')} · ${t('booking.dateEn')}`" :disabled-date="disabledPast" :disabled="readonly" class="bk-input" />
              </div>
              <div class="bk-field">
                <label class="bk-label">时间 <span class="bk-label-en">· Time</span></label>
                <el-select v-model="form.booking_time" :placeholder="`${t('booking.period')} · ${t('booking.periodEn')}`" :disabled="readonly" class="bk-input">
                  <el-option v-for="item in currentTimeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </div>
              <div class="bk-field">
                <label class="bk-label">预定员 <span class="bk-label-en">· Staff</span></label>
                <el-autocomplete v-model="form.staff_name" :fetch-suggestions="queryStaff" placeholder="预定员" @select="onStaffSelect" :disabled="readonly" class="bk-input" />
              </div>
              <div class="bk-field">
                <label class="bk-label">部门 <span class="bk-label-en">· Dept</span></label>
                <el-input v-model="form.staff_dept" placeholder="所属部门" disabled class="bk-input" />
              </div>

              <!-- 行2：客户来源 | 预定类型 | 统筹人姓名 | 统筹电话 -->
              <div class="bk-field">
                <label class="bk-label">客户来源 <span class="bk-label-en">· Source</span></label>
                <el-select v-model="form.source_type" :disabled="readonly" class="bk-input">
                  <el-option v-for="s in sourceOptions" :key="s.value" :label="`${s.label} · ${s.en}`" :value="s.value" />
                </el-select>
              </div>
              <div class="bk-field">
                <label class="bk-label">预定类型 <span class="bk-label-en">· Booking Type</span></label>
                <el-select v-model="form.booking_type" :disabled="readonly" class="bk-input">
                  <el-option v-for="b in bookingTypeOptions" :key="b.value" :label="`${b.label} · ${b.en}`" :value="b.value" />
                </el-select>
              </div>
              <div class="bk-field">
                <label class="bk-label">统筹人姓名 <span class="bk-label-en">· Coordinator</span></label>
                <el-input v-model="form.coordinator_name" placeholder="统筹人姓名" :disabled="readonly" class="bk-input" />
              </div>
              <div class="bk-field">
                <label class="bk-label">统筹电话 <span class="bk-label-en">· Coord Phone</span></label>
                <el-input v-model="form.coordinator_phone" placeholder="统筹人电话" :disabled="readonly" maxlength="11" class="bk-input" />
              </div>
            </div>
          </div>

          <!-- 分隔带 -->
          <div class="bk-form-divider">
            <span class="bk-form-divider-line"></span>
            <span class="bk-form-divider-ornament"></span>
            <span class="bk-form-divider-line"></span>
          </div>

          <!-- 下半区：客户信息 -->
          <div class="bk-form-section bk-form-section-bottom">
            <div class="bk-form-section-title">
              <span class="bk-form-section-dot"></span>
              <span>客户信息</span>
              <span class="bk-form-section-en">· Customer Info</span>
            </div>
            <div class="bk-form-grid bk-form-grid-bottom">
              <!-- 行3：客户姓名 | 客户手机 | 代订人姓名 | 代订人电话 -->
              <div class="bk-field">
                <label class="bk-label">客户姓名 <span class="bk-label-en">· Customer Name</span> <span class="bk-req">*</span></label>
                <el-autocomplete v-model="form.customer_name" :fetch-suggestions="queryCustomers" :placeholder="`${t('booking.customer.name')} · ${t('booking.customer.nameEn')}`" @select="onCustomerSelect" :disabled="readonly" class="bk-input" />
              </div>
              <div class="bk-field">
                <label class="bk-label">客户手机 <span class="bk-label-en">· Customer Phone</span> <span class="bk-req">*</span></label>
                <el-input v-model="form.customer_phone" :placeholder="`${t('booking.customer.phone')} · ${t('booking.customer.phoneEn')}`" :disabled="readonly" maxlength="11" class="bk-input" @input="onPhoneInput" />
              </div>
              <div class="bk-field" :class="{ 'bk-field-pending': form.booking_type === 'pending' }">
                <label class="bk-label">代订人姓名 <span class="bk-label-en">· Pending Name</span> <span v-if="form.booking_type === 'pending'" class="bk-req">*</span></label>
                <el-autocomplete v-model="form.pending_name" :fetch-suggestions="queryPendingCustomer" placeholder="代订人姓名" @select="onPendingCustomerSelect" :disabled="readonly || form.booking_type !== 'pending'" class="bk-input" />
              </div>
              <div class="bk-field" :class="{ 'bk-field-pending': form.booking_type === 'pending' }">
                <label class="bk-label">代订人电话 <span class="bk-label-en">· Pending Phone</span> <span v-if="form.booking_type === 'pending'" class="bk-req">*</span></label>
                <el-input v-model="form.pending_phone" placeholder="代订人电话" :disabled="readonly || form.booking_type !== 'pending'" maxlength="11" class="bk-input" />
              </div>

              <!-- 行4：宴席类型 | 定金 | 菜品总额 | 介绍人（条件） -->
              <div class="bk-field">
                <label class="bk-label">宴席类型 <span class="bk-label-en">· Occasion Type</span></label>
                <el-select v-model="form.occasion_type" :placeholder="`${t('booking.occasion.type')} · ${t('booking.occasion.typeEn')}`" :disabled="readonly" class="bk-input">
                  <el-option v-for="o in occasionOptions" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </div>
              <div class="bk-field">
                <label class="bk-label">定金 <span class="bk-label-en">· Deposit</span></label>
                <el-input v-model="form.deposit" :placeholder="`${t('booking.payment.deposit')} · ${t('booking.payment.depositEn')}`" type="number" :disabled="readonly" class="bk-input" />
              </div>
              <div class="bk-field">
                <label class="bk-label">菜品总额 <span class="bk-label-en">· Total Amount</span></label>
                <el-input :model-value="`¥${dishTotal}`" disabled class="bk-input" />
              </div>
              <div v-if="form.source_type === 'REF'" class="bk-field bk-field-ref">
                <label class="bk-label">介绍人 <span class="bk-label-en">· Referrer</span></label>
                <el-autocomplete v-model="form.referrer_name" :fetch-suggestions="queryReferrers" placeholder="介绍人姓名" @select="onReferrerSelect" :disabled="readonly" class="bk-input" />
              </div>
              <div v-if="form.source_type === 'REF'" class="bk-field bk-field-ref" style="grid-column: 1 / -1; grid-template-columns: repeat(2, 1fr);">
                <label class="bk-label">介绍人电话 <span class="bk-label-en">· Referrer Phone</span></label>
                <el-input v-model="form.referrer_phone" placeholder="介绍人电话" :disabled="readonly" maxlength="11" class="bk-input" />
              </div>
            </div>
          </div>
        </div>

        <!-- 客户消费提示面板 -->
        <div v-if="customerStats" class="bk-customer-hint">
          <div class="bk-customer-hint-header">
            <el-icon><User /></el-icon>
            <span>老客户 · {{ form.customer_name }}</span>
            <span class="bk-customer-hint-badge">VIP</span>
          </div>
          <div class="bk-customer-hint-grid">
            <div class="bk-customer-stat">
              <span class="bk-customer-stat-val">{{ customerStats.totalVisits }}</span>
              <span class="bk-customer-stat-label">累计预订</span>
            </div>
            <div class="bk-customer-stat">
              <span class="bk-customer-stat-val bk-customer-stat-amount">¥{{ customerStats.totalAmount.toLocaleString() }}</span>
              <span class="bk-customer-stat-label">消费总额</span>
            </div>
            <div class="bk-customer-stat">
              <span class="bk-customer-stat-val">{{ customerStats.lastVisitAmount > 0 ? '¥' + customerStats.lastVisitAmount.toLocaleString() : '-' }}</span>
              <span class="bk-customer-stat-label">上次消费</span>
            </div>
            <div class="bk-customer-stat">
              <span class="bk-customer-stat-val">{{ customerStats.lastVisit }}</span>
              <span class="bk-customer-stat-label">最近光顾</span>
            </div>
          </div>
          <div class="bk-customer-hint-foot">
            <span v-if="customerStats.topOccasion">常订：{{ customerStats.topOccasion }}</span>
            <button class="bk-customer-hint-btn" @click="activeTab = 'history'">查看完整历史 →</button>
          </div>
        </div>

        <!-- 桌台 & 备注 -->
        <div class="bk-block bk-block-compact">
          <div class="bk-compact-row">
            <div class="bk-block-title bk-compact-title">
              <h3>桌台配置 <span class="bk-title-en">· Seating</span></h3>
            </div>
            <div class="bk-stats-inline bk-compact-stats">
              <div class="bk-stat-mini"><span class="bk-stat-mini-label">桌数</span><span class="bk-stat-mini-val">{{ form.table_count }}</span></div>
              <span class="bk-stat-divider">+</span>
              <div class="bk-stat-mini"><span class="bk-stat-mini-label">备桌</span><span class="bk-stat-mini-val">{{ form.spare_tables }}</span></div>
              <span class="bk-stat-divider">×</span>
              <div class="bk-stat-mini"><span class="bk-stat-mini-label">人/桌</span><span class="bk-stat-mini-val">{{ form.guest_per_table }}</span></div>
              <span class="bk-stat-divider">=</span>
              <div class="bk-stat-mini bk-stat-mini-highlight"><span class="bk-stat-mini-label">合计</span><span class="bk-stat-mini-val">{{ totalGuests }}</span></div>
            </div>
          </div>
          <div class="bk-stat-inputs bk-stat-inputs-row bk-compact-inputs">
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">桌数</label>
              <el-input-number v-model="form.table_count" :min="1" :max="200" :disabled="readonly" class="bk-input" />
            </div>
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">备桌</label>
              <el-input-number v-model="form.spare_tables" :min="0" :max="50" :disabled="readonly" class="bk-input" />
            </div>
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">人/桌</label>
              <el-input-number v-model="form.guest_per_table" :min="1" :max="20" :disabled="readonly" class="bk-input" />
            </div>
          </div>
          <div class="bk-compact-divider"></div>
          <div class="bk-compact-row">
            <div class="bk-block-title bk-compact-title">
              <h3>已选桌台 <span class="bk-title-en">· Selected</span></h3>
            </div>
            <button class="bk-order-btn" @click="openDishDialog" :disabled="readonly">
              {{ hasDishes ? '已点菜 · Ordered' : '点菜 · Order' }}
            </button>
          </div>
          <div :class="['bk-chips bk-chips-compact', { 'has-tables': selectedTables.length > 0 }]">
            <span v-for="(tb, idx) in selectedTables" :key="tb.table_id" :class="['bk-chip', { 'chip-primary': idx === 0 }]">
              {{ tb.table_number || tb.table_name }}
            </span>
            <span v-if="selectedTables.length === 0" class="bk-empty">{{ t('booking.tables.noSelection') }}</span>
          </div>
          <div class="bk-compact-divider"></div>
          <div class="bk-field">
            <label class="bk-label">备注 <span class="bk-label-en">· Remark</span></label>
            <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="`${t('booking.remark')} · ${t('booking.remarkEn')}`" :disabled="readonly" class="bk-input" />
          </div>
        </div>
      </div>

      <!-- ============ Tab 2: 客户历史 ============ -->
      <div v-show="activeTab === 'history'" class="bk-tab-body">
        <!-- 消费报告摘要 -->
        <div v-if="customerStats" class="bk-history-report">
          <div class="bk-history-report-title">📊 消费报告 · Consumption Report</div>
          <div class="bk-history-report-grid">
            <div class="bk-history-stat">
              <div class="bk-history-stat-val">{{ customerStats.totalVisits }}</div>
              <div class="bk-history-stat-label">累计预订 · Total Bookings</div>
            </div>
            <div class="bk-history-stat">
              <div class="bk-history-stat-val">¥{{ customerStats.totalAmount.toLocaleString() }}</div>
              <div class="bk-history-stat-label">消费总额 · Total Spent</div>
            </div>
            <div class="bk-history-stat">
              <div class="bk-history-stat-val">{{ customerStats.completedVisits }}</div>
              <div class="bk-history-stat-label">已完成 · Completed</div>
            </div>
            <div class="bk-history-stat">
              <div class="bk-history-stat-val">¥{{ customerStats.totalDeposit.toLocaleString() }}</div>
              <div class="bk-history-stat-label">累计定金 · Total Deposit</div>
            </div>
          </div>
        </div>
        <div v-if="customerHistory.length > 0" class="bk-list">
          <div v-for="h in customerHistory" :key="h.booking_id" class="bk-list-item">
            <span class="bk-list-date">{{ h.booking_date }}</span>
            <span :class="['bk-list-status', h.booking_status]">{{ statusLabel(h.booking_status) }}</span>
            <span>{{ h.banquet_name || '宴席' }}</span>
            <span>{{ h.guest_count }}人</span>
            <span class="bk-list-amount">¥{{ h.total_amount }}</span>
          </div>
        </div>
        <el-empty v-else :description="`${t('booking.customer.history')} · ${t('booking.customer.historyEn')}`" />
      </div>

      <!-- ============ Tab 3: 菜单 ============ -->
      <div v-show="activeTab === 'menu'" class="bk-tab-body">
        <div v-if="dishOrderItems.length > 0" class="bk-list">
          <div v-for="d in dishOrderItems" :key="d.dishId" class="bk-list-item">
            <span>{{ d.dishName }}</span>
            <span>×{{ d.qty }}</span>
            <span class="bk-list-amount">¥{{ d.price * d.qty }}</span>
          </div>
          <div class="bk-list-total">{{ t('menu.total') }} · {{ t('menu.totalEn') }}：¥{{ dishTotal }}</div>
        </div>
        <el-empty :description="`${t('menu.noDishes')} · ${t('menu.noDishesEn')}`" />
      </div>

      <!-- ============ Tab 4: 变更记录 ============ -->
      <div v-show="activeTab === 'logs'" class="bk-tab-body" v-if="isEdit">
        <div v-if="changeLogs.length > 0" class="bk-list">
          <div v-for="log in changeLogs" :key="log.logId" class="bk-list-item bk-log">
            <div class="bk-log-head">
              <span class="bk-log-time">{{ formatLogTime(log.createdAt) }}</span>
              <span class="bk-log-op">{{ log.operatorName }}</span>
            </div>
            <!-- 字段级变更详情 -->
            <div v-if="log.changes && log.changes.length > 0" class="bk-log-changes">
              <div v-for="(c, i) in log.changes" :key="i" class="bk-log-change-row">
                <span class="bk-log-field">{{ c.fieldLabel }}:</span>
                <span class="bk-log-old">{{ c.oldValue || '空' }}</span>
                <span class="bk-log-arrow">→</span>
                <span class="bk-log-new">{{ c.newValue || '空' }}</span>
              </div>
            </div>
            <div v-else class="bk-log-detail">{{ log.detail || log.summary }}</div>
          </div>
        </div>
        <el-empty v-else :description="`${t('logs.noRecords')} · ${t('logs.noRecordsEn')}`" />
      </div>
    </div>

    <template #footer>
      <div class="bk-footer">
        <!-- 左侧：关闭/取消 -->
        <div class="bk-footer-left">
          <button v-if="readonly" class="bk-btn bk-btn-default" @click="doCancel" :disabled="loading">
            {{ t('booking.close') }} · {{ t('booking.closeEn') }}
          </button>
          <button v-else class="bk-btn bk-btn-default" @click="doCancel" :disabled="loading">
            {{ t('booking.cancel') }} · {{ t('booking.cancelEn') }}
          </button>
        </div>

        <!-- 右侧：操作按钮组 -->
        <div class="bk-footer-right">
          <!-- 只读模式：客户通知 + 厨房通知单 + 打印 -->
          <template v-if="readonly && isEdit">
            <button class="bk-btn bk-btn-notify" @click="openNotifyDialog">
              客户通知 · Notify
            </button>
            <button class="bk-btn bk-btn-kitchen" @click="emitPrint('kitchen_notice')">
              <el-icon><Printer /></el-icon>
              厨房通知单 · Kitchen
            </button>
            <template v-if="form.booking_status === 'confirmed'">
              <button class="bk-btn bk-btn-print" @click="emitPrint('confirmation')">
                <el-icon><Printer /></el-icon>
                打印确认单
              </button>
              <button class="bk-btn bk-btn-print" @click="emitPrint('table_sign')">
                <el-icon><Printer /></el-icon>
                打印桌签
              </button>
            </template>
            <template v-else-if="form.booking_status === 'cancelled'">
              <button class="bk-btn bk-btn-print" @click="emitPrint('cancellation')">
                <el-icon><Printer /></el-icon>
                打印取消单
              </button>
            </template>
          </template>

          <!-- 编辑模式：确定 + 打印按钮灰色禁用 -->
          <template v-else-if="!readonly">
            <button class="bk-btn bk-btn-default bk-btn-disabled" disabled title="请先保存后再打印">
              <el-icon><Printer /></el-icon>
              厨房通知单
            </button>
            <button class="bk-btn bk-btn-default bk-btn-disabled" disabled title="请先保存后再打印">
              <el-icon><Printer /></el-icon>
              打印确认单
            </button>
            <button class="bk-btn bk-btn-primary" @click="doSave" :disabled="loading">
              <el-icon v-if="!loading"><Check /></el-icon>
              {{ loading ? '保存中...' : '确定 · Confirm' }}
            </button>
          </template>

          <!-- 新建模式：确定按钮 -->
          <template v-else-if="!isEdit">
            <button class="bk-btn bk-btn-primary" @click="doSave" :disabled="loading">
              <el-icon v-if="!loading"><Check /></el-icon>
              {{ loading ? '保存中...' : '确定 · Confirm' }}
            </button>
          </template>
        </div>
      </div>
    </template>

    <!-- 客户通知对话框 -->
    <el-dialog v-model="notifyVisible" title="客户通知 · Customer Notification" width="50vw" :close-on-click-modal="false" custom-class="bk-notify-dialog">
      <div class="bk-notify-body">
        <div class="bk-notify-info">
          <span>单号：{{ displayBookingId }}</span>
          <span>日期：{{ form.booking_date }}</span>
          <span>时间：{{ form.booking_time }}</span>
          <span>桌台：{{ selectedTables.map(t => t.table_number || t.table_name).join('、') }}</span>
        </div>
        <div class="bk-notify-textarea-wrap">
          <textarea
            v-model="notifyContent"
            class="bk-notify-textarea"
            rows="16"
            placeholder="请输入通知内容..."
          ></textarea>
        </div>
        <div class="bk-notify-tips">
          <span>💡 可直接编辑修改，点击复制按钮可复制到剪贴板，发送给客户微信。</span>
        </div>
      </div>
      <template #footer>
        <div class="bk-notify-footer">
          <button class="bk-btn bk-btn-default" @click="notifyVisible = false">关闭 · Close</button>
          <button class="bk-btn bk-btn-primary" @click="copyNotifyContent">复制 · Copy</button>
          <button class="bk-btn bk-btn-notify" @click="copyNotifyAndClose">复制并关闭 · Copy & Close</button>
        </div>
      </template>
    </el-dialog>

    <DishOrderDialog v-if="dishVisible" v-model="dishVisible" :date="form.booking_date" :period="form.booking_time" :table-name="currentTableName" @confirmed="onDishConfirmed" />
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { KnifeFork, Check, Close, User, Grid, Edit, Printer } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import DishOrderDialog from './DishOrderDialog.vue'
import { searchCustomers } from '../api/customer'
import { createBooking, updateBooking } from '../api/booking'
import { getDictBatch, getStaffList, searchCustomersDict } from '../api/dict'
import { getTableOrders } from '../utils/menuStore'

const props = defineProps({
  modelValue: Boolean,
  date: String,
  tableId: Number,
  tableNumber: String,
  tableName: String,
  booking: Object,
  tableIds: Array,
  tableNames: Array,
  tableAreas: Array,
  tableCapacities: Array
})
const emit = defineEmits(['update:modelValue', 'saved', 'date-change', 'period-change', 'print'])

const { t } = useI18n()

const tabs = [
  { key: 'basic', label: '预订时间', en: 'Booking Time' },
  { key: 'history', label: '客户历史', en: 'Customer History' },
  { key: 'menu', label: '菜单', en: 'Menu' },
  { key: 'logs', label: '变更记录', en: 'Changes' }
]

// ===== 数据字典（从数据库加载） =====
const occasionOptions = ref([])
const sourceOptions = ref([])
const bookingTypeOptions = ref([])
const statusOptions = ref([])
const timeOptions = ref({ lunch: [], dinner: [] })

// 硬编码兜底：当 API 不可用时使用，保证页面可用
const FALLBACK = {
  occasion_type: [
    { item_value: 'a_la_carte', item_label: '零点', item_en: 'A la Carte' },
    { item_value: 'wedding', item_label: '婚宴', item_en: 'Wedding' },
    { item_value: 'birthday', item_label: '生日宴', item_en: 'Birthday' },
    { item_value: 'engagement', item_label: '订婚宴', item_en: 'Engagement' },
    { item_value: 'baby_born', item_label: '满月宴', item_en: 'Baby Born' },
    { item_value: 'graduation', item_label: '谢师宴', item_en: 'Graduation' },
    { item_value: 'help_wine', item_label: '帮忙酒', item_en: 'Help Wine' },
    { item_value: 'house_move', item_label: '乔迁宴', item_en: 'House Move' },
    { item_value: 'promotion', item_label: '升迁宴', item_en: 'Promotion' },
    { item_value: 'reunion', item_label: '团圆宴', item_en: 'Reunion' },
    { item_value: 'thanksgiving', item_label: '答谢宴', item_en: 'Thanksgiving' },
    { item_value: 'year_end', item_label: '尾牙宴', item_en: 'Year End' }
  ],
  source_type: [
    { item_value: 'WALKIN', item_label: '上门散客', item_en: 'Walk-in' },
    { item_value: 'OTA', item_label: '线上客户', item_en: 'OTA' },
    { item_value: 'CORP', item_label: '企业协议', item_en: 'Corporate' },
    { item_value: 'TOUR', item_label: '旅游团队', item_en: 'Tour Group' },
    { item_value: 'CONF', item_label: '会议团队', item_en: 'Conference' },
    { item_value: 'DIRECT', item_label: '自有客户', item_en: 'Direct' },
    { item_value: 'REF', item_label: '朋友介绍', item_en: 'Referral' },
    { item_value: 'LOCAL', item_label: '周边社区', item_en: 'Local' },
    { item_value: 'PER', item_label: '亲朋好友', item_en: 'Personal' }
  ],
  booking_type: [
    { item_value: 'direct', item_label: '直接预定', item_en: 'Direct' },
    { item_value: 'pending', item_label: '客户代订', item_en: 'Pending' }
  ],
  booking_status: [
    { item_value: 'pending', item_label: '待确认', item_en: 'Pending' },
    { item_value: 'confirmed', item_label: '已确认', item_en: 'Confirmed' },
    { item_value: 'completed', item_label: '已完成', item_en: 'Completed' },
    { item_value: 'cancelled', item_label: '已取消', item_en: 'Cancelled' }
  ],
  time_slot: [
    { item_value: 'lunch_11:00', item_label: '11:00', item_en: 'Lunch' },
    { item_value: 'lunch_11:30', item_label: '11:30', item_en: 'Lunch' },
    { item_value: 'lunch_12:00', item_label: '12:00', item_en: 'Lunch' },
    { item_value: 'dinner_17:00', item_label: '17:00', item_en: 'Dinner' },
    { item_value: 'dinner_17:30', item_label: '17:30', item_en: 'Dinner' },
    { item_value: 'dinner_18:00', item_label: '18:00', item_en: 'Dinner' },
    { item_value: 'dinner_18:30', item_label: '18:30', item_en: 'Dinner' },
    { item_value: 'dinner_19:00', item_label: '19:00', item_en: 'Dinner' }
  ]
}

// 从数据库批量加载字典数据
async function loadDictData() {
  try {
    const codes = 'occasion_type,source_type,booking_type,booking_status,time_slot'
    const res = await getDictBatch(codes)
    const data = res.data || res || {}

    occasionOptions.value = (data.occasion_type || FALLBACK.occasion_type).map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    sourceOptions.value = (data.source_type || FALLBACK.source_type).map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    bookingTypeOptions.value = (data.booking_type || FALLBACK.booking_type).map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    statusOptions.value = (data.booking_status || FALLBACK.booking_status).map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))

    // 解析 time_slot 为 lunch/dinner 两组
    const rawSlots = data.time_slot || FALLBACK.time_slot
    const lunch = []
    const dinner = []
    for (const s of rawSlots) {
      const v = s.item_value
      const label = s.item_label
      const timeVal = v.replace(/^(lunch_|dinner_)/, '') + ':00'
      if (v.startsWith('lunch_')) {
        lunch.push({ label, value: timeVal })
      } else if (v.startsWith('dinner_')) {
        dinner.push({ label, value: timeVal })
      }
    }
    timeOptions.value = { lunch, dinner }
  } catch (e) {
    console.warn('字典数据加载失败，使用兜底数据:', e)
    // 使用兜底数据
    occasionOptions.value = FALLBACK.occasion_type.map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    sourceOptions.value = FALLBACK.source_type.map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    bookingTypeOptions.value = FALLBACK.booking_type.map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    statusOptions.value = FALLBACK.booking_status.map(d => ({
      label: d.item_label, value: d.item_value, en: d.item_en
    }))
    timeOptions.value = {
      lunch: [
        { label: '11:00', value: '11:00:00' },
        { label: '11:30', value: '11:30:00' },
        { label: '12:00', value: '12:00:00' }
      ],
      dinner: [
        { label: '17:00', value: '17:00:00' },
        { label: '17:30', value: '17:30:00' },
        { label: '18:00', value: '18:00:00' },
        { label: '18:30', value: '18:30:00' },
        { label: '19:00', value: '19:00:00' }
      ]
    }
  }
}

const mealPeriod = ref('dinner')
const currentTimeOptions = computed(() => timeOptions.value[mealPeriod.value] || [])

function setMeal(m) {
  const oldMeal = mealPeriod.value
  mealPeriod.value = m
  const options = timeOptions.value[m]
  if (options && options.length > 0) {
    form.value.booking_time = options[0].value
  }
  if (visible.value && !readonly.value && selectedTables.value.length > 0) {
    emit('period-change', m, oldMeal, form.value.booking_date)
  }
}

const visible = ref(props.modelValue)
const activeTab = ref('basic')
const loading = ref(false)
const customerHistory = ref([])
const dishVisible = ref(false)
const currentTableName = ref('')
const readonly = ref(false)

// 客户通知相关
const notifyVisible = ref(false)
const notifyContent = ref('')

function buildNotifyTemplate() {
  const tables = selectedTables.value
  const tableNames = tables.map(t => t.table_name || t.table_number || '未知桌台').join('、')
  const tableArea = tables[0]?.table_area || ''
  const date = form.value.booking_date || ''
  const time = form.value.booking_time || ''
  // 从数据库字典 occasionOptions 中查找宴席类型标签
  const occasionItem = occasionOptions.value.find(o => o.value === form.value.occasion_type)
  const occasion = occasionItem?.label || '宴会'
  const customerName = form.value.customer_name || '客户'
  const customerPhone = form.value.customer_phone || ''
  
  return `🎊 亲爱的${customerName}，您好！\n\n${date} ${time} ${occasion}已为您安排妥当，恭候光临！\n\n▫️ 桌台：${tableNames}\n${tableArea ? '▫️ 区域：' + tableArea + '\n' : ''}📱 联系电话：${customerPhone}\n\n📍 又见炊烟·宴会预定\n━━━━━━━━━━━━\n🏠 2个宴会厅 | 2~20桌灵活接待\n🏠 2~22位包厢 | 各种台型\n💍 订婚酒 · 🤝 帮忙酒\n🎂 十周岁宴 · 🍼 满月酒\n🏡 乔迁酒 · 🎉 开业酒\n📚 升学宴 · 🎓 谢师宴\n🙏 答谢宴 · 📋 订货会\n👥 年会 · 团建 · 旅行团\n\n🍶 订喜酒即赠古井贡酒，喜上加喜！\n☎欢迎致电咨询。\n365天营业，随时恭候 ❤️`
}

function openNotifyDialog() {
  notifyContent.value = buildNotifyTemplate()
  notifyVisible.value = true
}

async function copyNotifyContent() {
  try {
    await navigator.clipboard.writeText(notifyContent.value)
    ElMessage.success('已复制到剪贴板 · Copied to clipboard')
  } catch (e) {
    ElMessage.error('复制失败，请手动选择复制 · Copy failed')
  }
}

async function copyNotifyAndClose() {
  await copyNotifyContent()
  notifyVisible.value = false
}

const form = ref(emptyForm())
const isEdit = computed(() => !!props.booking)

// 显示用的单号和创建时间：永远保证有值，绝不显示空
const displayBookingId = computed({
  get() {
    if (!form.value.booking_id) {
      form.value.booking_id = generateBookingId()
    }
    return form.value.booking_id
  },
  set(val) {
    form.value.booking_id = val
  }
})
const displayCreatedAt = computed({
  get() {
    if (!form.value.created_at) {
      form.value.created_at = getCurrentDateTime()
    }
    return form.value.created_at
  },
  set(val) {
    form.value.created_at = val
  }
})

function emptyForm() {
  // 从 localStorage 取当前登录用户信息（userStore 异步加载，这里先兜底）
  const staffName = localStorage.getItem('staffName') || ''
  const staffDept = localStorage.getItem('staffDept') || ''
  const staffId = localStorage.getItem('staffId') || null
  return {
    booking_id: generateBookingId(),
    created_at: getCurrentDateTime(),
    staff_id: staffId,
    staff_name: staffName,
    staff_dept: staffDept,
    booking_date: '', booking_time: '',
    booking_type: 'direct',
    customer_name: '', customer_phone: '',
    pending_name: '', pending_phone: '',
    source_type: 'WALKIN', referrer_name: '', referrer_phone: '',
    coordinator_name: '', coordinator_phone: '',
    occasion_type: 'wedding', guest_per_table: 10, table_count: 1, spare_tables: 0,
    deposit: '', booking_status: 'confirmed', remark: ''
  }
}

const selectedTables = ref([])
const dishOrderItems = ref([])
const changeLogs = ref([])

const totalGuests = computed(() =>
  (form.value.table_count + form.value.spare_tables) * form.value.guest_per_table
)
const hasDishes = computed(() => dishOrderItems.value.length > 0)
const dishTotal = computed(() =>
  dishOrderItems.value.reduce((sum, d) => sum + d.price * d.qty, 0)
)

function disabledPast(date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d.getTime() < Date.now() - 864e5
}

// ===== 预定员（员工表） =====

function onStaffSelect(item) {
  form.value.staff_name = item.value
  form.value.staff_dept = item.department || ''
}

async function queryStaff(qs, cb) {
  try {
    const res = await getStaffList(qs)
    const data = res.data || []
    const rows = Array.isArray(data) ? data : []
    cb(rows.map(s => ({
      value: s.staff_name || s.staffName,
      department: s.department,
      phone: s.phone,
      id: s.staff_id || s.staffId
    })))
  } catch { cb([]) }
}

// ===== 客户模糊搜索（客户/代订人/介绍人 共用） =====

function onCustomerSelect(item) {
  form.value.customer_name = item.value
  form.value.customer_phone = item.phone || ''
  loadCustomerHistory(item.id)
}

function onPendingCustomerSelect(item) {
  form.value.pending_name = item.value
  form.value.pending_phone = item.phone || ''
}

function onReferrerSelect(item) {
  form.value.referrer_name = item.value
  form.value.referrer_phone = item.phone || ''
}

async function queryCustomers(qs, cb) {
  if (!qs || qs.length < 1) return cb([])
  try {
    const res = await searchCustomersDict(qs)
    const data = res.data?.list || res.data?.rows || res.data || []
    const rows = Array.isArray(data) ? data : []
    cb(rows.map(c => ({
      value: c.customer_name || c.customerName || c.name,
      phone: c.customer_phone || c.customerPhone || c.phone,
      id: c.customer_id || c.customerId || c.id
    })))
  } catch { cb([]) }
}

async function queryPendingCustomer(qs, cb) {
  if (!qs || qs.length < 1) return cb([])
  try {
    const res = await searchCustomersDict(qs)
    const data = res.data?.list || res.data?.rows || res.data || []
    const rows = Array.isArray(data) ? data : []
    cb(rows.map(c => ({
      value: c.customer_name || c.customerName || c.name,
      phone: c.customer_phone || c.customerPhone || c.phone,
      id: c.customer_id || c.customerId || c.id
    })))
  } catch { cb([]) }
}

async function queryReferrers(qs, cb) {
  if (!qs || qs.length < 1) return cb([])
  try {
    const res = await searchCustomersDict(qs)
    const data = res.data?.list || res.data?.rows || res.data || []
    const rows = Array.isArray(data) ? data : []
    cb(rows.map(c => ({
      value: c.customer_name || c.customerName || c.name,
      phone: c.customer_phone || c.customerPhone || c.phone,
      id: c.customer_id || c.customerId || c.id
    })))
  } catch { cb([]) }
}

async function loadCustomerHistory(customerId) {
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/bookings?customer_id=' + encodeURIComponent(customerId), {
      credentials: 'include',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const json = await res.json()
    customerHistory.value = json.data?.rows || json.data || []
  } catch { customerHistory.value = [] }
}

// 客户消费统计
const customerStats = computed(() => {
  if (!customerHistory.value || customerHistory.value.length === 0) return null
  const records = customerHistory.value
  const totalVisits = records.length
  const completedRecords = records.filter(r => r.booking_status === 'completed')
  const totalAmount = completedRecords.reduce((sum, r) => sum + (Number(r.total_amount) || 0), 0)
  const lastVisit = records
    .filter(r => r.booking_status === 'completed')
    .sort((a, b) => (b.booking_date || '').localeCompare(a.booking_date || ''))[0]
  const depositRecords = records.filter(r => Number(r.deposit) > 0)
  const totalDeposit = depositRecords.reduce((sum, r) => sum + (Number(r.deposit) || 0), 0)
  const occasions = {}
  records.forEach(r => {
    if (r.banquet_name) occasions[r.banquet_name] = (occasions[r.banquet_name] || 0) + 1
  })
  const topOccasion = Object.entries(occasions).sort((a, b) => b[1] - a[1])[0]
  return {
    totalVisits,
    completedVisits: completedRecords.length,
    totalAmount,
    lastVisit: lastVisit ? lastVisit.booking_date + ' ' + (lastVisit.booking_time || '') : '-',
    lastVisitAmount: lastVisit ? Number(lastVisit.total_amount) || 0 : 0,
    totalDeposit,
    topOccasion: topOccasion ? topOccasion[0] : ''
  }
})

let phoneSearchTimer = null
function onPhoneInput() {
  const phone = form.value.customer_phone?.trim()
  if (!phone || phone.length < 3) return
  if (phoneSearchTimer) clearTimeout(phoneSearchTimer)
  phoneSearchTimer = setTimeout(async () => {
    try {
      const res = await searchCustomers({ q: phone })
      const data = res.data?.list || res.data?.rows || res.data || []
      const matched = Array.isArray(data) ? data.find(c => {
        const cp = (c.customerPhone || c.customer_phone || c.phone || '').toString()
        return cp === phone || cp.includes(phone)
      }) : null
      if (matched) {
        form.value.customer_name = matched.customerName || matched.customer_name || matched.name || form.value.customer_name
        const cid = matched.customerId || matched.customer_id || matched.id
        if (cid) loadCustomerHistory(cid)
      } else {
        customerHistory.value = []
      }
    } catch {}
  }, 400)
}

function statusLabel(s) {
  const m = { pending: '待确认', confirmed: '已确认', completed: '已完成', cancelled: '已取消' }
  return m[s] || s
}
function statusLabelEn(s) {
  const m = { pending: 'Pending', confirmed: 'Confirmed', completed: 'Completed', cancelled: 'Cancelled' }
  return m[s] || s
}

function loadDishOrders() {
  const date = form.value.booking_date
  const period = form.value.booking_time
  const tableName = selectedTables.value.map(t => t.table_name || t.table_number).join('、')
  dishOrderItems.value = []
  if (date && period && tableName) {
    const tableOrders = getTableOrders(date, period, tableName)
    dishOrderItems.value = tableOrders.map(o => ({
      dishId: o.dishCode,
      dishName: o.dishCode,
      qty: o.qty,
      price: 0,
      remark: o.remark || ''
    }))
  }
}

function openDishDialog() {
  currentTableName.value = selectedTables.value.map(t => t.table_name || t.table_number).join('、')
  dishVisible.value = true
  activeTab.value = 'menu'
}

function onDishConfirmed() {
  dishVisible.value = false
  loadDishOrders()
}

function autoRemark() {
  const tables = form.value.table_count
  const spare = form.value.spare_tables
  const total = totalGuests.value
  form.value.remark = tables + '桌备' + spare + '桌，共' + total + '人'
}

async function doSave() {
  if (!validateBooking()) return
  loading.value = true
  const firstTable = selectedTables.value[0]
  
  const body = {
    customer_name: form.value.customer_name.trim(),
    customer_phone: form.value.customer_phone.trim(),
    booking_date: fmtDateForApi(form.value.booking_date),
    booking_time: form.value.booking_time,
    booking_type: form.value.booking_type || 'direct',
    pending_name: form.value.pending_name || '',
    pending_phone: form.value.pending_phone || '',
    source_type: form.value.source_type || 'WALKIN',
    referrer_name: form.value.referrer_name || '',
    referrer_phone: form.value.referrer_phone || '',
    staff_name: form.value.staff_name || '',
    staff_dept: form.value.staff_dept || '',
    coordinator_name: form.value.coordinator_name || '',
    coordinator_phone: form.value.coordinator_phone || '',
    guest_count: totalGuests.value,
    table_count: form.value.table_count,
    spare_tables: form.value.spare_tables,
    occasion_type: form.value.occasion_type,
    deposit: form.value.deposit ? parseFloat(form.value.deposit) : null,
    remark: form.value.remark || '',
    booking_status: form.value.booking_status || 'confirmed',
    table_ids: selectedTables.value.map(t => t.table_id).filter(Boolean),
    table_names: selectedTables.value.map(t => t.table_name || t.table_number || ''),
    guest_per_table: form.value.guest_per_table || 10,
    banquet_name: form.value.banquet_name || '',
    payment_status: form.value.payment_status || 'unpaid'
  }

  try {
    const editId = props.booking?.id || props.booking?.bookingId || props.booking?.booking_id
    let res
    
    if (editId) {
      res = await updateBooking(editId, body)
    } else {
      res = await createBooking(body)
    }

    if (res.code === 200) {
      const bookingId = res.data?.bookingId || res.data?.order_no || res.data?.id || res.data?.booking_id || ''
      ElMessage.success(bookingId ? '保存成功！单号：' + bookingId : '保存成功')
      dishOrderItems.value = []
      emit('saved', res.data)
      // 跳出询问是否关闭
      try {
        await ElMessageBox.confirm(
          '保存成功，是否关闭当前预订窗口？\nBooking saved successfully. Close this window?',
          '保存成功 · Saved',
          { confirmButtonText: '关闭 · Close', cancelButtonText: '继续编辑 · Keep Editing', type: 'success' }
        )
        visible.value = false
      } catch {
        // 用户选择继续编辑，切换到只读模式
        readonly.value = true
      }
    } else {
      // API返回错误，前端降级保存
      console.warn('API保存失败，使用前端降级:', res)
      const fr = await frontendSave(editId, body)
      if (fr?.success) {
        try {
          await ElMessageBox.confirm(
            '保存成功，是否关闭当前预订窗口？\nBooking saved. Close this window?',
            '保存成功 · Saved',
            { confirmButtonText: '关闭 · Close', cancelButtonText: '继续编辑 · Keep Editing', type: 'success' }
          )
          visible.value = false
        } catch {
          readonly.value = true
        }
      }
    }
  } catch (e) {
    console.error('Save error:', e)
    // API异常，前端降级保存
    const fr = await frontendSave(props.booking?.booking_id || props.booking?.id || null, body)
    if (fr?.success) {
      try {
        await ElMessageBox.confirm(
          '保存成功，是否关闭当前预订窗口？\nBooking saved. Close this window?',
          '保存成功 · Saved',
          { confirmButtonText: '关闭 · Close', cancelButtonText: '继续编辑 · Keep Editing', type: 'success' }
        )
        visible.value = false
      } catch {
        readonly.value = true
      }
    }
  } finally {
    loading.value = false
  }
}

function doCancel() { visible.value = false }

// 前端降级保存：当后端API不可用时，使用localStorage保存
// 多桌台场景：所有选中桌台都应用同一份预订变更
async function frontendSave(existingId, body) {
  const STORAGE_KEY = 'tboard_local_bookings'
  const CHANGES_KEY = 'tboard_booking_changes'
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    const localBookings = saved ? JSON.parse(saved) : {}

    const date = body.booking_date
    const bookingTime = body.booking_time || ''
    const hour = parseInt(String(bookingTime).split(':')[0] || '18')
    // 与boardPeriod保持一致：lunch→morning, dinner→afternoon
    const period = hour < 15 ? 'morning' : 'afternoon'

    const tableIds = body.table_ids || []
    const tableInfoList = selectedTables.value

    // 生成或使用现有booking ID（变更后单号刷新：仅新建时生成新单号，编辑保留原单号）
    const isNew = !existingId
    const bookingId = existingId || ('LOCAL_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6))

    // 收集旧值用于变更记录（编辑场景）
    let preservedCreatedAt = ''
    const oldFields = {}
    if (existingId) {
          for (const tableId of tableIds) {
            if (!tableId) continue
            const key = `${tableId}_${date}_${period}`
            const old = localBookings[key]
            if (old) {
              preservedCreatedAt = old.created_at || old.createdAt || preservedCreatedAt
              oldFields.customer_name = old.customer_name
              oldFields.customer_phone = old.customer_phone
              oldFields.booking_type = old.booking_type || 'direct'
              oldFields.pending_name = old.pending_name || ''
              oldFields.pending_phone = old.pending_phone || ''
              oldFields.source_type = old.source_type || 'WALKIN'
              oldFields.referrer_name = old.referrer_name || ''
              oldFields.referrer_phone = old.referrer_phone || ''
              oldFields.staff_name = old.staff_name || ''
              oldFields.staff_dept = old.staff_dept || ''
              oldFields.coordinator_name = old.coordinator_name || ''
              oldFields.coordinator_phone = old.coordinator_phone || ''
              oldFields.booking_time = old.booking_time
              oldFields.guest_count = old.guest_count
              oldFields.table_count = old.table_count
              oldFields.banquet_name = old.banquet_name
              oldFields.occasion_type = old.occasion_type
              oldFields.deposit = old.deposit
              oldFields.remark = old.remark
              break
            }
          }
      // 若从表单中能取到已有的创建时间，优先使用
      if (!preservedCreatedAt && form.value.created_at) {
        preservedCreatedAt = form.value.created_at
      }
    }

    // 多桌台循环写入：所有选中桌台都应用同一份预订变更
    for (let i = 0; i < tableIds.length; i++) {
      const tableId = tableIds[i]
      if (!tableId) continue
      const info = tableInfoList[i] || {}
      const key = `${tableId}_${date}_${period}`
      localBookings[key] = {
        ...body,
        booking_id: bookingId,
        table_id: tableId,
        table_number: info.table_number || info.table_name || '',
        table_area: info.table_area || '',
        table_capacity: info.table_capacity || null,
        date: date,
        period: period,
        created_at: existingId ? (preservedCreatedAt || form.value.created_at || '') : (form.value.created_at || new Date().toISOString()),
        staff_name: form.value.staff_name || '',
        _local: true,
        _updatedAt: new Date().toISOString()
      }
    }

    localStorage.setItem(STORAGE_KEY, JSON.stringify(localBookings))

    // 变更记录：编辑且未删除时，记录变更字段到变更信息表（localStorage 降级存储）
    if (existingId) {
      const changes = []
      const fieldLabels = {
        customer_name: '客户姓名',
        customer_phone: '手机号',
        booking_type: '预定类型',
        pending_name: '代订人姓名',
        pending_phone: '代订人电话',
        source_type: '客户来源',
        referrer_name: '介绍人姓名',
        referrer_phone: '介绍人电话',
        staff_name: '预定员',
        staff_dept: '部门',
        coordinator_name: '统筹人姓名',
        coordinator_phone: '统筹电话',
        booking_time: '预订时间',
        guest_count: '人数',
        table_count: '桌数',
        banquet_name: '宴会名称',
        occasion_type: '类型',
        deposit: '定金',
        remark: '备注'
      }
      for (const k of Object.keys(fieldLabels)) {
        const oldV = oldFields[k] ?? ''
        const newV = body[k] ?? ''
        if (String(oldV) !== String(newV)) {
          changes.push({
            field: k,
            fieldLabel: fieldLabels[k],
            oldValue: oldV,
            newValue: newV
          })
        }
      }
      if (changes.length > 0) {
        try {
          const allChanges = JSON.parse(localStorage.getItem(CHANGES_KEY) || '{}')
          if (!allChanges[bookingId]) allChanges[bookingId] = []
          allChanges[bookingId].push({
            logId: 'LOG_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
            createdAt: new Date().toISOString(),
            operatorName: localStorage.getItem('staffName') || '系统',
            changes: changes,
            summary: changes.map(c => `${c.fieldLabel}: ${c.oldValue || '空'} → ${c.newValue || '空'}`).join('；')
          })
          localStorage.setItem(CHANGES_KEY, JSON.stringify(allChanges))
          // 刷新变更记录显示
          changeLogs.value = allChanges[bookingId]
        } catch (e) {
          console.warn('变更记录保存失败:', e)
        }
      }
    }

    // 变更后订单单号刷新：更新表单中的单号
    form.value.booking_id = bookingId

    ElMessage.success('保存成功 · Saved')
    dishOrderItems.value = []
    emit('saved', { booking_id: bookingId, ...body, _local: true })
    // 返回成功，由调用方处理是否关闭
    return { success: true, bookingId }
  } catch (e) {
    console.error('前端保存失败:', e)
    ElMessageBox.alert('保存失败：' + (e.message || '未知错误'), 'Save Failed', {
      confirmButtonText: '我知道了 · OK',
      appendToBody: true,
      customClass: 'bk-message-box'
    })
    return { success: false }
  }
}
function enterEditMode() { readonly.value = false }

function emitPrint(type) {
  const bookingTables = selectedTables.value.map(t => ({
    table_name: t.table_name || t.table_number || '-',
    table_number: t.table_number || t.table_name || '-'
  }))

  let phone = form.value.customer_phone || '-'
  if (phone.length === 11) phone = phone.slice(0, 3) + '****' + phone.slice(7)

  const printData = {
    booking_id: form.value.booking_id || '-',
    booking_date: form.value.booking_date || '-',
    booking_time: form.value.booking_time || '',
    customer_name: form.value.customer_name || '-',
    customer_phone: phone,
    occasion_type: form.value.occasion_type || 'a_la_carte',
    table_count: form.value.table_count || 1,
    spare_tables: form.value.spare_tables || 0,
    guest_per_table: form.value.guest_per_table || 10,
    deposit: form.value.deposit || '',
    remark: form.value.remark || '',
    booking_tables: bookingTables,
    booking_status: form.value.booking_status || 'confirmed'
  }
  emit('print', { type, data: printData })
}

function updateSelectedTables(tables) {
  if (!tables || !Array.isArray(tables)) {
    selectedTables.value = []
    return false
  }
  if (selectedTables.value.length === 0) return false
  const selectedIds = selectedTables.value.map(t => t.table_id)
  const newSelected = []
  const bookedNames = []
  selectedIds.forEach(id => {
    const found = tables.find(t => t.table_id === id)
    if (!found) return
    if (found.booking) {
      bookedNames.push(found.table_name || found.table_number || '未知桌台')
    } else {
      newSelected.push(found)
    }
  })
  if (bookedNames.length > 0) {
    const names = bookedNames.join('、')
    ElMessageBox.alert(
      `「${names}」在选择的日期/时间已有预订，请选择其他桌台。\nThe selected table(s) are already booked for the chosen date/time. Please choose another.`,
      '桌台已被预订 · Table Already Booked',
      { confirmButtonText: '我知道了 · OK', customClass: 'bk-message-box' }
    )
    return true
  } else {
    selectedTables.value = newSelected
    return false
  }
}

function revertDate(oldDate) {
  const stopWatch = watch(() => form.value.booking_date, () => {
    stopWatch()
  })
  form.value.booking_date = oldDate
}

function revertPeriod(oldPeriod) {
  const stopWatch = watch(() => mealPeriod.value, () => {
    stopWatch()
  })
  mealPeriod.value = oldPeriod
  const options = timeOptions[oldPeriod]
  if (options && options.length > 0) {
    form.value.booking_time = options[0].value
  }
}

function getCurrentPeriod() {
  return mealPeriod.value
}

function validateBooking() {
  const errors = []
  
  if (!form.value.booking_id || !form.value.booking_id.trim()) {
    errors.push('订单编号未生成，请重新打开窗口')
  }
  if (!form.value.created_at) {
    errors.push('创建时间未生成')
  }
  if (!form.value.staff_name || !form.value.staff_name.trim()) {
    errors.push('经手人未登录系统')
  }
  if (!form.value.booking_date) {
    errors.push('日期未选择')
  }
  if (!form.value.booking_time) {
    errors.push('时段未选择')
  }
  if (!form.value.customer_name || !form.value.customer_name.trim()) {
    errors.push('客户姓名为空')
  }
  if (!form.value.customer_phone || !form.value.customer_phone.trim()) {
    errors.push('手机号为空')
  } else if (!/^1[3-9]\d{9}$/.test(form.value.customer_phone.trim())) {
    errors.push('手机号格式不正确（应为11位数字，以1开头）')
  }
  if (selectedTables.value.length === 0) {
    errors.push('未选择桌台')
  }
  if (!form.value.table_count || form.value.table_count < 1) {
    errors.push('桌数不能小于1')
  }
  if (!form.value.guest_per_table || form.value.guest_per_table < 1) {
    errors.push('每桌人数不能小于1')
  }
  if (!form.value.occasion_type) {
    errors.push('宴席类型未选择')
  }
  if (form.value.booking_type === 'pending') {
    if (!form.value.pending_name || !form.value.pending_name.trim()) {
      errors.push('代订人姓名必填')
    }
    if (!form.value.pending_phone || !form.value.pending_phone.trim()) {
      errors.push('代订人电话必填')
    } else if (!/^1[3-9]\d{9}$/.test(form.value.pending_phone.trim())) {
      errors.push('代订人电话格式不正确')
    }
  }
  if (form.value.source_type === 'REF') {
    if (!form.value.referrer_name || !form.value.referrer_name.trim()) {
      errors.push('朋友介绍时介绍人姓名必填')
    }
    if (!form.value.referrer_phone || !form.value.referrer_phone.trim()) {
      errors.push('朋友介绍时介绍人电话必填')
    } else if (!/^1[3-9]\d{9}$/.test(form.value.referrer_phone.trim())) {
      errors.push('介绍人电话格式不正确')
    }
  }
  if (form.value.deposit && isNaN(parseFloat(form.value.deposit))) {
    errors.push('定金不是有效数字')
  }
  
  if (errors.length > 0) {
    const reason = errors.map((e, i) => `${i + 1}. ${e}`).join('\n')
    ElMessageBox.alert(reason, '无法保存 · Unable to Save', {
      confirmButtonText: '我知道了 · OK',
      appendToBody: true,
      customClass: 'bk-message-box'
    })
    return false
  }
  return true
}

function generateBookingId() {
  const now = new Date()
  const date = now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, '0') +
    String(now.getDate()).padStart(2, '0')
  const time = String(now.getHours()).padStart(2, '0') +
    String(now.getMinutes()).padStart(2, '0') +
    String(now.getSeconds()).padStart(2, '0')
  const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  return `BK${date}${time}${random}`
}

function getCurrentDateTime() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (!val) return

  const b = props.booking
  const existingId = b?.id || b?.bookingId || b?.booking_id || b?.order_no || ''
  readonly.value = !!existingId
  const isNewBooking = !existingId

  // ===== 第一步：确保单号和创建时间一定有值 =====
  // 新建预订：立即生成单号和创建时间（先占上，后面不会再变）
  if (isNewBooking) {
    if (!form.value.booking_id) form.value.booking_id = generateBookingId()
    if (!form.value.created_at) form.value.created_at = getCurrentDateTime()
  }

  // ===== 第二步：加载数据字典 =====
  if (occasionOptions.value.length === 0) {
    await loadDictData()
  }

  // ===== 第三步：填充表单数据 =====
  if (isNewBooking) {
    // 新建预订：初始化表单（单号和创建时间已生成，这里补充其他字段）
    fillFormFromBooking(b)
  } else {
    // 编辑预订：优先从后端拉取详情
    try {
      const token = localStorage.getItem('token') || ''
      const res = await fetch('/api/bookings/' + existingId, {
        credentials: 'include',
        headers: { 'Authorization': 'Bearer ' + token }
      })
      const data = await res.json()
      if (data.code === 200 && data.data) {
        const d = data.data.booking || data.data
        let rawTime = d.time_slot || d.booking_time || d.bookingTime || ''
        if (rawTime && rawTime.length === 5) rawTime = rawTime + ':00'
        const hour = parseInt(rawTime.split(':')[0] || '18')
        mealPeriod.value = hour < 15 ? 'lunch' : 'dinner'

        const occType = d.banquet_type || d.occasion_type || d.occasionType
        const validOccasions = occasionOptions.value.map(o => o.value)
        const finalOccasion = occType && validOccasions.includes(occType) ? occType : (occasionOptions.value[0]?.value || 'a_la_carte')

        form.value = {
          booking_id: d.order_no || d.id || d.booking_id || d.bookingId || existingId,
          created_at: d.created_at || d.createdAt || getCurrentDateTime(),
          staff_name: d.created_by || d.staffName || d.staff_name || '',
          staff_dept: d.staff_dept || d.staffDept || '',
          coordinator_name: d.coordinator_name || d.coordinatorName || '',
          coordinator_phone: d.coordinator_phone || d.coordinatorPhone || '',
          booking_date: d.booking_date || d.bookingDate || '',
          booking_time: rawTime,
          booking_type: d.booking_type || d.bookingType || 'direct',
          customer_name: d.customer_name || d.customerName || '',
          customer_phone: d.customer_phone || d.customerPhone || '',
          pending_name: d.pending_name || d.pendingName || '',
          pending_phone: d.pending_phone || d.pendingPhone || '',
          source_type: d.source_type || d.sourceType || 'WALKIN',
          referrer_name: d.referrer_name || d.referrerName || '',
          referrer_phone: d.referrer_phone || d.referrerPhone || '',
          occasion_type: finalOccasion,
          guest_per_table: d.guest_per_table || d.guestPerTable || 10,
          table_count: d.table_count || d.tableCount || 1,
          spare_tables: d.spare_tables || d.spareTables || 0,
          deposit: d.deposit || '',
          booking_status: d.status || d.booking_status || d.bookingStatus || (statusOptions.value[0]?.value || 'confirmed'),
          remark: d.remarks || d.remark || ''
        }

        const tables = data.data.tables || data.data.booking_tables || d.booking_tables || d.bookingTables || []
        if (tables.length > 0) {
          selectedTables.value = tables.map(bt => ({
            table_id: bt.table_id || bt.tableId,
            table_number: bt.table_number || bt.tableNumber || bt.table_name || bt.tableName || '',
            table_name: bt.table_name || bt.tableName || bt.table_number || bt.tableNumber || '',
            table_area: bt.table_area || bt.area_name || '',
            table_capacity: bt.table_capacity || bt.capacity || null
          }))
        } else if (props.tableIds && props.tableIds.length > 0) {
          const names = props.tableNames || []
          const areas = props.tableAreas || []
          const caps = props.tableCapacities || []
          selectedTables.value = props.tableIds.map((id, i) => ({
            table_id: id, table_number: names[i] || '', table_name: names[i] || '',
            table_area: areas[i] || '', table_capacity: caps[i] || null
          }))
        } else if (d.table_id || d.tableId) {
          selectedTables.value = [{
            table_id: d.table_id || d.tableId,
            table_number: d.table_name || d.tableName || '',
            table_name: d.table_name || d.tableName || '',
            table_area: d.table_area || '', table_capacity: d.table_capacity || null
          }]
        } else { selectedTables.value = [] }
      } else {
        fillFormFromBooking(b)
      }
    } catch (e) {
      console.error('拉取预订详情失败:', e)
      fillFormFromBooking(b)
    }
  }

  // ===== 第四步：确保预定员是当前登录用户 =====
  if (isNewBooking && !form.value.staff_name) {
    await ensureCurrentUser()
  }

  activeTab.value = 'basic'
  customerHistory.value = []
  loadDishOrders()
})

async function ensureCurrentUser() {
  try {
    const userStore = useUserStore()
    if (!userStore.initialized) {
      await userStore.init()
    }
    let u = userStore.userInfo || {}
    // 如果 userInfo 为空，直接用 fetch 调 /auth/me 作为降级
    if (!u.staffName && !u.staff_name && !u.name && !u.username) {
      try {
        const token = localStorage.getItem('token') || ''
        const resp = await fetch('/api/auth/me', {
          credentials: 'include',
          headers: { 'Authorization': 'Bearer ' + token }
        })
        if (resp.ok) {
          const data = await resp.json()
          if (data.code === 200 && data.data) {
            u = data.data
            // 同步保存到 localStorage 供后续使用
            if (u.staffName || u.name) localStorage.setItem('staffName', u.staffName || u.name)
            if (u.department || u.dept || u.deptName) localStorage.setItem('staffDept', u.department || u.dept || u.deptName)
            if (u.staffId || u.id) localStorage.setItem('staffId', u.staffId || u.id)
          }
        }
      } catch (e2) {
        console.warn('降级获取用户信息失败:', e2)
      }
    }
    const userName = u.staffName || u.staff_name || u.name || u.userName || u.username
      || localStorage.getItem('staffName') || localStorage.getItem('userName') || ''
    const userDept = u.department || u.dept || u.deptName || u.dept_name
      || localStorage.getItem('staffDept') || ''
    const userId = u.staffId || u.staff_id || u.id
      || localStorage.getItem('staffId') || null
    if (userName && !form.value.staff_name) {
      form.value.staff_name = userName
      form.value.staff_dept = userDept
      if (userId) form.value.staff_id = userId
    }
  } catch (e) {
    console.warn('获取当前用户信息失败:', e)
  }
}

function fillFormFromBooking(b) {
  if (b) {
    let rawTime = b.time_slot || b.booking_time || b.bookingTime || '18:00:00'
    if (rawTime && rawTime.length === 5) rawTime = rawTime + ':00'
    const hour = parseInt(rawTime.split(':')[0] || '18')
    mealPeriod.value = hour < 15 ? 'lunch' : 'dinner'
    
    const occType = b.banquet_type || b.occasion_type || b.occasionType
    const validOccasions = occasionOptions.value.map(o => o.value)
    const finalOccasion = occType && validOccasions.includes(occType) ? occType : (occasionOptions.value[0]?.value || 'a_la_carte')
    
    const validSources = sourceOptions.value.map(o => o.value)
    const finalSource = b.source_type && validSources.includes(b.source_type) ? b.source_type
      : b.sourceType && validSources.includes(b.sourceType) ? b.sourceType
      : (sourceOptions.value[0]?.value || 'WALKIN')
    
    const validBookingTypes = bookingTypeOptions.value.map(o => o.value)
    const finalBookingType = b.booking_type && validBookingTypes.includes(b.booking_type) ? b.booking_type
      : (bookingTypeOptions.value[0]?.value || 'direct')
    
    const validStatuses = statusOptions.value.map(o => o.value)
    const finalStatus = b.status && validStatuses.includes(b.status) ? b.status
      : b.booking_status && validStatuses.includes(b.booking_status) ? b.booking_status
      : b.bookingStatus && validStatuses.includes(b.bookingStatus) ? b.bookingStatus
      : (statusOptions.value[0]?.value || 'confirmed')
    
    const userStore = useUserStore()
    const u = userStore.userInfo || {}
    const currentUserName = u.staffName || u.staff_name || u.name || u.userName || u.username
      || localStorage.getItem('staffName') || localStorage.getItem('userName') || ''
    const currentUserDept = u.department || u.dept || u.deptName || u.dept_name
      || localStorage.getItem('staffDept') || ''
    const currentStaffId = u.staffId || u.staff_id || u.id
      || localStorage.getItem('staffId') || null
    
    const bookingStaffName = b.created_by || b.staffName || b.staff_name || ''
    const bookingStaffDept = b.staff_dept || b.staffDept || ''
    
    form.value = {
      booking_id: b.order_no || b.id || b.booking_id || b.bookingId || generateBookingId(),
      created_at: b.created_at || b.createdAt || getCurrentDateTime(),
      staff_id: b.staff_id || b.staffId || currentStaffId,
      staff_name: bookingStaffName || currentUserName,
      staff_dept: bookingStaffDept || currentUserDept,
      coordinator_name: b.coordinator_name || b.coordinatorName || '',
      coordinator_phone: b.coordinator_phone || b.coordinatorPhone || '',
      booking_date: b.booking_date || b.bookingDate || '',
      booking_time: rawTime,
      booking_type: finalBookingType,
      customer_name: b.customer_name || b.customerName || '',
      customer_phone: b.customer_phone || b.customerPhone || '',
      pending_name: b.pending_name || b.pendingName || '',
      pending_phone: b.pending_phone || b.pendingPhone || '',
      source_type: finalSource,
      referrer_name: b.referrer_name || b.referrerName || '',
      referrer_phone: b.referrer_phone || b.referrerPhone || '',
      occasion_type: finalOccasion,
      guest_per_table: b.guest_per_table || b.guestPerTable || 10,
      table_count: b.table_count || b.tableCount || 1,
      spare_tables: b.spare_tables || b.spareTables || 0,
      deposit: b.deposit || '',
      booking_status: finalStatus,
      remark: b.remarks || b.remark || ''
    }
    const tables = b.booking_tables || b.bookingTables || []
    if (tables.length > 0) {
      selectedTables.value = tables.map(bt => ({
        table_id: bt.table_id || bt.tableId,
        table_number: bt.table_number || bt.tableNumber || bt.table_name || bt.tableName || '',
        table_name: bt.table_name || bt.tableName || bt.table_number || bt.tableNumber || '',
        table_area: bt.table_area || bt.area_name || '',
        table_capacity: bt.table_capacity || bt.capacity || null
      }))
    } else if (b.table_id || b.tableId || props.tableIds?.length > 0) {
      // 多选场景：优先使用 props.tableIds（来自父组件选中的所有桌台）
      // 单选场景：使用 booking 中的单个 table_id
      const fallbackNames = props.tableNames || []
      const fallbackAreas = props.tableAreas || []
      const fallbackCapacities = props.tableCapacities || []
      if (props.tableIds && props.tableIds.length > 0) {
        // 多选：用 props.tableIds 构建完整列表
        const primaryId = b.table_id || b.tableId
        selectedTables.value = props.tableIds.map((id, i) => ({
          table_id: id,
          table_number: fallbackNames[i] || '',
          table_name: fallbackNames[i] || '',
          table_area: fallbackAreas[i] || '',
          table_capacity: fallbackCapacities[i] || null
        }))
      } else {
        // 单选：只有一个桌台
        selectedTables.value = [{
          table_id: b.table_id || b.tableId,
          table_number: b.table_number || b.table_name || b.tableName || fallbackNames[0] || '',
          table_name: b.table_name || b.tableName || b.table_number || fallbackNames[0] || '',
          table_area: b.table_area || fallbackAreas[0] || '',
          table_capacity: b.table_capacity || fallbackCapacities[0] || null
        }]
      }
    } else if (b.tableNames) {
      const names = typeof b.tableNames === 'string' ? b.tableNames.split(',') : b.tableNames
      const areas = (Array.isArray(b.tableAreas) ? b.tableAreas : [])
      const caps = (Array.isArray(b.tableCapacities) ? b.tableCapacities : [])
      selectedTables.value = names.map((n, i) => ({
        table_id: (Array.isArray(b.tableIds) ? b.tableIds[i] : null) || null,
        table_number: n.trim(),
        table_name: n.trim(),
        table_area: areas[i] || '',
        table_capacity: caps[i] || null
      }))
    } else if (props.tableIds && props.tableIds.length > 0) {
      // 如果booking没有table信息，使用props
      const names = props.tableNames || []
      const areas = props.tableAreas || []
      const caps = props.tableCapacities || []
      selectedTables.value = props.tableIds.map((id, i) => ({
        table_id: id,
        table_number: names[i] || '',
        table_name: names[i] || '',
        table_area: areas[i] || '',
        table_capacity: caps[i] || null
      }))
    } else { selectedTables.value = [] }
  } else {
    const userStore = useUserStore()
    const u = userStore.userInfo || {}
    const currentUserName = u.staffName || u.staff_name || u.name || u.userName || u.username
      || localStorage.getItem('staffName') || localStorage.getItem('userName') || ''
    const currentUserDept = u.department || u.dept || u.deptName || u.dept_name
      || localStorage.getItem('staffDept') || ''
    const currentStaffId = u.staffId || u.staff_id || u.id
      || localStorage.getItem('staffId') || null
    
    const defaultOccasion = occasionOptions.value[0]?.value || 'a_la_carte'
    const defaultSource = sourceOptions.value[0]?.value || 'WALKIN'
    const defaultBookingType = bookingTypeOptions.value[0]?.value || 'direct'
    const defaultStatus = statusOptions.value[0]?.value || 'confirmed'
    
    mealPeriod.value = 'dinner'
    form.value = {
      booking_id: generateBookingId(),
      created_at: getCurrentDateTime(),
      booking_date: props.date || new Date().toISOString().split('T')[0],
      booking_time: '18:00:00',
      booking_type: defaultBookingType,
      staff_id: currentStaffId,
      staff_name: currentUserName,
      staff_dept: currentUserDept,
      coordinator_name: '',
      coordinator_phone: '',
      customer_name: '',
      customer_phone: '',
      pending_name: '',
      pending_phone: '',
      source_type: defaultSource,
      referrer_name: '',
      referrer_phone: '',
      occasion_type: defaultOccasion,
      guest_per_table: 10,
      table_count: 1,
      spare_tables: 0,
      deposit: '',
      booking_status: defaultStatus,
      remark: ''
    }
    // 优先使用 props.tableIds/tableNames（多选）
    if (props.tableIds && props.tableIds.length > 0) {
      const names = props.tableNames || []
      const areas = props.tableAreas || []
      const capacities = props.tableCapacities || []
      selectedTables.value = props.tableIds.map((id, i) => ({
        table_id: id,
        table_number: names[i] || '',
        table_name: names[i] || '',
        table_area: areas[i] || '',
        table_capacity: capacities[i] || null
      }))
      // 更新桌数
      form.value.table_count = selectedTables.value.length
    } else if (props.tableId) {
      selectedTables.value = [{
        table_id: props.tableId,
        table_number: props.tableNumber || props.tableName || '',
        table_name: props.tableName || props.tableNumber || '',
        table_area: '',
        table_capacity: null
      }]
    } else { selectedTables.value = [] }
  }
}

watch(visible, (v) => emit('update:modelValue', v))

function fmtDateForApi(d) {
  if (!d) return ''
  if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}/.test(d)) {
    return d.slice(0, 10)
  }
  const date = new Date(d)
  if (isNaN(date.getTime())) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

watch(() => form.value.booking_date, (newDate, oldDate) => {
  if (!newDate || newDate === oldDate) return
  if (!visible.value || readonly.value) return
  emit('date-change', fmtDateForApi(newDate))
})
watch(() => [form.value.table_count, form.value.spare_tables, form.value.guest_per_table], autoRemark)

watch(() => [props.tableId, props.tableNumber, props.tableName], ([id, num, name]) => {
  if (id && visible.value) {
    selectedTables.value = [{
      table_id: id,
      table_number: num || name || '',
      table_name: name || num || ''
    }]
  }
})

watch(() => props.tableIds, (ids) => {
  if (ids && ids.length > 0 && visible.value) {
    const names = props.tableNames || []
    const areas = props.tableAreas || []
    const caps = props.tableCapacities || []
    selectedTables.value = ids.map((id, i) => ({
      table_id: id,
      table_number: names[i] || '',
      table_name: names[i] || '',
      table_area: areas[i] || '',
      table_capacity: caps[i] || null
    }))
    form.value.table_count = ids.length
  }
}, { deep: true })

function onOpened() {
  // 新建预订时，确保单号和创建时间已生成
  if (!isEdit.value) {
    if (!form.value.booking_id) form.value.booking_id = generateBookingId()
    if (!form.value.created_at) form.value.created_at = getCurrentDateTime()
  }
  if (!form.value.booking_date) form.value.booking_date = new Date().toISOString().split('T')[0]
  if (!form.value.booking_time) form.value.booking_time = '18:00:00'
  if (isEdit.value && form.value.booking_id) loadChangeLogs(form.value.booking_id)
}

// 双击解锁：在header或body上双击进入编辑模式
function onHeaderDblClick() {
  if (readonly.value) {
    enterEditMode()
    ElMessage.success('已解锁编辑模式 · Edit mode unlocked')
  }
}
function onBodyDblClick() {
  if (readonly.value) {
    enterEditMode()
    ElMessage.success('已解锁编辑模式 · Edit mode unlocked')
  }
}

// 区域样式判断：包房红/其他墨绿
function areaClass(areaName) {
  if (!areaName) return ''
  // 首选台号标记：第一个选中桌台用红色（由调用方判断是否为首选）
  return ''
}

// 判断是否为首选台号（第一个选中的）- 已移除首选标记
function isPrimaryIndex(index) {
  return index === 0
}

async function loadChangeLogs(bookingId) {
  // 优先从 localStorage 降级存储读取变更记录
  try {
    const CHANGES_KEY = 'tboard_booking_changes'
    const allChanges = JSON.parse(localStorage.getItem(CHANGES_KEY) || '{}')
    if (allChanges[bookingId] && allChanges[bookingId].length > 0) {
      changeLogs.value = allChanges[bookingId]
      return
    }
  } catch { /* ignore */ }

  // 后端 API 尝试
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/bookings/' + bookingId + '/logs', {
      credentials: 'include',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const json = await res.json()
    changeLogs.value = json.data?.rows || json.data || []
  } catch { changeLogs.value = [] }
}

function formatLogTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

defineExpose({ updateSelectedTables, revertDate, revertPeriod, getCurrentPeriod })
</script>

<style>
.bk-dlg .el-dialog {
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 40px 80px -20px rgba(45, 74, 62, 0.25);
  animation: bkModalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes bkModalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.bk-dlg .el-overlay {
  backdrop-filter: blur(6px);
  background: rgba(0, 0, 0, 0.35);
}
.bk-dlg .el-dialog__header {
  padding: 0;
  margin: 0;
}
.bk-dlg .el-dialog__headerbtn { display: none; }
.bk-dlg .el-dialog__body {
  padding: 0;
  background: oklch(1 0.004 95);
}
.bk-dlg .el-dialog__footer {
  padding: 0;
  background: transparent;
}

/* ============ 头部 ============ */
.bk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: oklch(0.38 0.055 160);
  padding: 14px 18px;
  color: oklch(0.98 0.01 95);
}
.bk-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-header-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  background: oklch(0.98 0.01 95 / 0.1);
  box-shadow: 0 0 0 1px oklch(0.98 0.01 95 / 0.15) inset;
  font-size: 16px;
}
.bk-header-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}
.bk-header-sub {
  font-size: 11px;
  color: oklch(0.98 0.01 95 / 0.7);
  margin: 2px 0 0;
}
.bk-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: oklch(0.75 0.11 75);
  color: oklch(0.28 0.03 70);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}
.bk-change-alert {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: oklch(0.97 0.05 45);
  color: oklch(0.55 0.18 45);
  padding: 5px 14px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid oklch(0.85 0.10 45);
  animation: bk-pulse 2s ease-in-out infinite;
}
.bk-breathe {
  display: inline-block;
  animation: bk-pulse 2s ease-in-out infinite;
  color: oklch(0.55 0.18 45);
  font-weight: 500;
}
@keyframes bk-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.03); }
}
.bk-close-btn {
  background: transparent;
  border: none;
  color: oklch(0.98 0.01 95 / 0.8);
  cursor: pointer;
  padding: 3px;
  border-radius: 0.375rem;
  font-size: 16px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}
.bk-close-btn:hover {
  background: oklch(0.98 0.01 95 / 0.1);
  color: oklch(0.98 0.01 95);
}

/* ============ 顶部操作按钮组 ============ */
.bk-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bk-btn-header {
  padding: 6px 14px !important;
  font-size: 12px !important;
}

/* 双击编辑提示 */
.bk-edit-hint {
  font-size: 11px;
  font-weight: 400;
  color: oklch(0.98 0.01 95 / 0.6);
  margin-left: 8px;
  cursor: pointer;
}
.bk-edit-hint:hover {
  color: oklch(0.75 0.11 75);
}
.bk-header {
  cursor: default;
  user-select: none;
}
.bk-header:hover {
  cursor: pointer;
}

/* ============ 选中桌台回显区 ============ */
.bk-selected-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px;
  background: oklch(0.955 0.012 120 / 0.5);
  border-bottom: 1px solid oklch(0.9 0.012 120);
  font-size: 12px;
}
.bk-summary-primary {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-summary-label {
  font-weight: 600;
  color: oklch(0.24 0.02 160);
}
.bk-summary-value {
  font-weight: 700;
  color: oklch(0.38 0.055 160);
  font-size: 12px;
  font-family: inherit;
  padding: 6px 12px;
  border-radius: 6px;
}
.chip-primary {
  background: #C0392B !important;
  color: #fff !important;
  border-color: #C0392B !important;
  font-weight: 700;
  font-size: 12px;
  padding: 6px 12px;
  border-radius: 6px;
  letter-spacing: normal;
}
.chip-primary-badge {
  display: inline-block;
  background: #fff;
  color: #C0392B;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 4px;
  border-radius: 3px;
  margin-right: 4px;
}
.chip-primary .chip-primary-badge {
  background: #fff;
  color: #C0392B;
}
.bk-summary-area {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  background: oklch(0.94 0.012 150);
  color: oklch(0.35 0.02 150);
}
.bk-summary-capacity {
  color: oklch(0.52 0.02 150);
}
.bk-summary-more {
  color: oklch(0.52 0.02 150);
  font-weight: 500;
}

/* ============ 标签页 ============ */
.bk-tabs {
  display: flex;
  gap: 2px;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.4);
  padding: 0 10px;
}
.bk-tab {
  position: relative;
  padding: 7px 10px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.52 0.02 150);
  transition: color 0.2s;
}
.bk-tab:hover { color: oklch(0.24 0.02 160); }
.bk-tab.active { color: oklch(0.38 0.055 160); }
.bk-tab-en {
  margin-left: 3px;
  font-size: 11px;
  color: oklch(0.52 0.02 150);
}
.bk-tab.active .bk-tab-en { color: oklch(0.52 0.02 150); }
.bk-tab.active::after {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 0;
  height: 2px;
  border-radius: 9999px;
  background: oklch(0.38 0.055 160);
}

/* ============ 主体 ============ */
.bk-body {
  max-height: 65vh;
  overflow-y: auto;
  padding: 16px 18px;
}
.bk-blocks {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.bk-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bk-block-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: oklch(0.38 0.055 160);
}
.bk-block-title h3 {
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 0;
}
.bk-title-en {
  font-weight: 400;
  color: oklch(0.52 0.02 150);
  font-size: 11px;
}
.bk-block-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bk-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.bk-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

/* ============ 字段 ============ */
.bk-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.bk-field-pending {
  padding: 8px 10px;
  background: oklch(0.97 0.03 85);
  border: 1px dashed oklch(0.65 0.15 85);
  border-radius: 8px;
  transition: all 0.2s;
}
.bk-field-pending .bk-label {
  color: oklch(0.55 0.15 85);
}
.bk-field-ref {
  padding: 8px 10px;
  background: oklch(0.97 0.03 200);
  border: 1px dashed oklch(0.55 0.15 200);
  border-radius: 8px;
  transition: all 0.2s;
}
.bk-field-ref .bk-label {
  color: oklch(0.45 0.15 200);
}
.bk-label {
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
}
.bk-label-en {
  font-size: 11px;
  font-weight: 400;
  color: oklch(0.52 0.02 150);
}
.bk-req {
  color: oklch(0.75 0.11 75);
  margin-left: 2px;
}
.bk-input {
  width: 100%;
}
.bk-input .el-input__wrapper,
.bk-input .el-select .el-input__wrapper,
.bk-field .el-input__wrapper,
.bk-field .el-select .el-input__wrapper,
.bk-field .el-autocomplete .el-input__wrapper {
  border-radius: 0.375rem !important;
  box-shadow: 0 0 0 1px oklch(0.9 0.012 120) inset !important;
  background: oklch(1 0.004 95) !important;
}
.bk-input .el-input__wrapper:hover,
.bk-field .el-input__wrapper:hover,
.bk-field .el-select .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px oklch(0.75 0.11 75) inset !important;
}
.bk-input .el-input__wrapper.is-focus,
.bk-field .el-input__wrapper.is-focus,
.bk-field .el-select .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px oklch(0.38 0.055 160) inset !important;
}
.bk-field .el-input__inner,
.bk-field .el-select .el-input__inner,
.bk-field .el-autocomplete .el-input__inner,
.bk-field .el-date-editor .el-input__inner {
  font-size: 13px;
  font-family: inherit;
  height: 34px;
  color: oklch(0.24 0.02 160);
}
.bk-field .el-textarea__inner {
  border-radius: 0.375rem;
  font-size: 13px;
  font-family: inherit;
  border-color: oklch(0.9 0.012 120);
  color: oklch(0.24 0.02 160);
  background: oklch(1 0.004 95);
  line-height: 1.5;
  min-height: 60px;
}

/* ============ 餐别切换 ============ */
.bk-field-meal-row {
  flex-direction: row;
  align-items: center;
  gap: 10px;
  padding: 4px 0 8px;
  border-bottom: 1px dashed oklch(0.9 0.012 120);
  margin-bottom: 6px;
}
.bk-field-meal-row .bk-label {
  flex-shrink: 0;
}
.bk-field-meal-row .bk-meal-switch {
  flex-direction: row;
  flex: 1;
  gap: 4px;
  padding: 3px;
  min-height: 32px;
  height: auto;
}
.bk-field-meal-row .bk-meal-btn {
  flex: 1;
  padding: 8px 12px;
  font-size: 13px;
  min-height: 28px;
}
.bk-meal-switch {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.5);
  border-radius: 0.5rem;
  padding: 6px 4px;
  height: 100%;
}
.bk-meal-btn {
  flex: 1;
  width: 100%;
  padding: 10px 8px;
  border: none;
  background: oklch(0.97 0.008 120);
  border-radius: 0.375rem;
  font-size: 13px;
  font-weight: 600;
  color: oklch(0.52 0.02 150);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bk-meal-btn:hover { color: oklch(0.24 0.02 160); }
.bk-meal-btn.active {
  background: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}
.bk-meal-btn:disabled { cursor: not-allowed; opacity: 0.6; }

/* ============ 统计卡片 ============ */
.bk-stat {
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.5rem;
  padding: 10px 14px;
  text-align: center;
}
.bk-stat-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: oklch(0.52 0.02 150);
  margin: 0;
  text-transform: uppercase;
}
.bk-stat-value {
  font-size: 20px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 4px 0 0;
  font-family: 'Noto Serif SC', serif;
}
.bk-stat-highlight {
  border-color: oklch(0.38 0.055 160 / 0.2);
  background: oklch(0.38 0.055 160 / 0.05);
}
.bk-stat-highlight .bk-stat-label { color: oklch(0.38 0.055 160 / 0.7); }
.bk-stat-highlight .bk-stat-value { color: oklch(0.38 0.055 160); }

.bk-stat-inputs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

/* ============ 已选桌台 ============ */
.bk-order-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.5rem;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  cursor: pointer;
  transition: background 0.2s;
}
.bk-order-btn:hover { background: oklch(0.955 0.012 120); }
.bk-order-btn:disabled { cursor: not-allowed; opacity: 0.6; }
.bk-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.3);
  border-radius: 0.75rem;
  padding: 12px;
}
.bk-chips.has-tables {
  animation: bk-chips-breathe 3s ease-in-out infinite;
}
@keyframes bk-chips-breathe {
  0%, 100% {
    border-color: oklch(0.9 0.012 120);
    box-shadow: 0 0 0 0 oklch(0.55 0.15 150 / 0);
  }
  50% {
    border-color: oklch(0.7 0.08 150);
    box-shadow: 0 0 0 4px oklch(0.55 0.15 150 / 0.08);
  }
}
.bk-chip {
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.375rem;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  display: inline-flex;
  align-items: center;
}
.bk-chip.chip-primary {
  background: #C0392B !important;
  color: #fff !important;
  border-color: #C0392B !important;
  font-weight: 700;
}
.bk-empty {
  color: oklch(0.52 0.02 150);
  font-size: 13px;
}

/* ============ 信息条 ============ */
.bk-info-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: oklch(0.955 0.012 120 / 0.4);
  border-radius: 0.5rem;
  padding: 12px 16px;
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.bk-info-val {
  font-weight: 500;
  color: oklch(0.24 0.02 160);
}

/* ============ 列表 ============ */
.bk-tab-body { padding: 24px; }
.bk-list { max-height: 400px; overflow-y: auto; }
.bk-list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 12px;
  font-size: 13px;
  color: oklch(0.24 0.02 160);
  border-bottom: 1px dashed oklch(0.9 0.012 120);
}
.bk-list-item:hover { background: oklch(0.955 0.012 120); }
.bk-list-date { color: oklch(0.34 0.04 160); min-width: 90px; }
.bk-list-status { font-size: 11px; padding: 2px 10px; border-radius: 9999px; font-weight: 500; }
.bk-list-status.pending { background: oklch(0.955 0.012 120); color: oklch(0.34 0.04 160); }
.bk-list-status.confirmed { background: oklch(0.955 0.012 120); color: oklch(0.38 0.055 160); }
.bk-list-status.completed { background: oklch(0.955 0.012 120); color: oklch(0.38 0.055 160); }
.bk-list-status.cancelled { background: oklch(0.955 0.012 120); color: oklch(0.577 0.19 27.325); }
.bk-list-amount { margin-left: auto; color: oklch(0.75 0.11 75); font-weight: 600; }
.bk-list-total { text-align: right; padding: 12px; font-weight: 600; color: oklch(0.75 0.11 75); font-size: 14px; border-top: 1px solid oklch(0.9 0.012 120); }
.bk-log { flex-direction: column; align-items: flex-start; gap: 6px; padding: 12px; border-left: 3px solid oklch(0.75 0.11 75); }
.bk-log-head { display: flex; gap: 14px; font-size: 12px; color: oklch(0.52 0.02 150); }
.bk-log-op { color: oklch(0.24 0.02 160); font-weight: 500; }
.bk-log-detail { font-size: 13px; color: oklch(0.34 0.04 160); line-height: 1.5; }
.bk-log-changes { margin-top: 6px; display: flex; flex-direction: column; gap: 4px; }
.bk-log-change-row { display: flex; align-items: center; gap: 6px; font-size: 12px; }
.bk-log-field { color: oklch(0.4 0.02 150); min-width: 70px; font-weight: 500; }
.bk-log-old { color: oklch(0.52 0.02 150); text-decoration: line-through; }
.bk-log-arrow { color: oklch(0.52 0.02 150); }
.bk-log-new { color: oklch(0.4 0.13 50); font-weight: 600; }

/* ============ 底部 ============ */
.bk-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-top: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.3);
  padding: 16px 24px;
}
.bk-footer-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-footer-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 0.5rem;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid transparent;
}
.bk-btn:disabled { cursor: not-allowed; opacity: 0.6; }
.bk-btn-default {
  border-color: oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.24 0.02 160);
}
.bk-btn-default:hover {
  background: oklch(0.955 0.012 120);
  transform: translateY(-1px);
}
.bk-btn-primary {
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  font-weight: 600;
  box-shadow: 0 6px 14px rgba(45, 74, 62, 0.15);
}
.bk-btn-primary:hover {
  background: oklch(0.34 0.04 160);
  border-color: oklch(0.34 0.04 160);
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 12px 24px rgba(45, 74, 62, 0.2);
}
.bk-btn-primary:active {
  transform: scale(0.97);
}

/* ============ 横版三行三列布局 ============ */
.bk-basic {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 顶部信息条 */
.bk-info-top {
  margin: 0 !important;
  border-radius: 0.5rem;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.4);
  padding: 10px 16px;
}
.bk-info-top .bk-info-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bk-info-top .bk-info-label {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.bk-info-top .bk-info-val {
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
}
.bk-info-top .bk-info-sep {
  width: 1px;
  height: 14px;
  background: oklch(0.9 0.012 120);
}

/* 表单整体容器 */
.bk-form-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 每个表单区域 */
.bk-form-section {
  padding: 8px 12px;
  background: oklch(0.99 0.003 95);
  border: 1px solid oklch(0.92 0.012 120);
  border-radius: 0.75rem;
  position: relative;
}

.bk-form-section-top {
  border-bottom-left-radius: 0.375rem;
  border-bottom-right-radius: 0.375rem;
}

.bk-form-section-bottom {
  border-top-left-radius: 0.375rem;
  border-top-right-radius: 0.375rem;
}

/* 区域小标题 */
.bk-form-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.38 0.055 160);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.bk-form-section-dot {
  width: 4px;
  height: 12px;
  background: linear-gradient(to bottom, oklch(0.55 0.15 150), oklch(0.38 0.055 160));
  border-radius: 2px;
}

.bk-form-section-en {
  font-size: 11px;
  font-weight: 400;
  color: oklch(0.55 0.02 150);
}

/* 三行三列表单网格 */
.bk-form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px 14px;
  align-items: start;
}

.bk-form-grid-top {
  grid-template-rows: auto auto;
}

.bk-form-grid-bottom {
  grid-template-rows: auto auto;
}

.bk-field-spacer {
  visibility: hidden;
}

/* 块间距压缩 */
.bk-basic .bk-block {
  padding: 10px 14px;
  margin: 0;
}
.bk-basic .bk-block-title {
  margin-bottom: 6px;
}
.bk-basic .bk-block-title h3 {
  font-size: 13px;
}

/* 桌台配置：标题+统计一行 */
.bk-block-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.bk-stats-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bk-stat-mini {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 0.375rem;
  background: oklch(0.955 0.012 120 / 0.5);
}
.bk-stat-mini-label {
  font-size: 11px;
  color: oklch(0.52 0.02 150);
}
.bk-stat-mini-val {
  font-size: 14px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  font-family: 'Noto Serif SC', serif;
}
.bk-stat-mini-highlight {
  background: oklch(0.38 0.055 160 / 0.1);
}
.bk-stat-mini-highlight .bk-stat-mini-label { color: oklch(0.38 0.055 160 / 0.7); }
.bk-stat-mini-highlight .bk-stat-mini-val { color: oklch(0.38 0.055 160); }
.bk-stat-divider {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
  font-weight: 600;
}

.bk-form-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 8px;
}
.bk-form-divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, oklch(0.82 0.02 150), transparent);
}
.bk-form-divider-ornament {
  width: 10px;
  height: 10px;
  background: oklch(0.55 0.15 150);
  border-radius: 50%;
  box-shadow: 0 0 0 3px oklch(0.98 0.01 95), 0 0 8px oklch(0.55 0.15 150 / 0.3);
  flex-shrink: 0;
}

/* 数量输入框一行 */
.bk-stat-inputs-row {
  margin-top: 6px;
  gap: 10px;
}
.bk-field-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bk-label-inline {
  font-size: 12px;
  color: oklch(0.34 0.04 160);
  white-space: nowrap;
  width: auto;
  margin-bottom: 0;
}

/* 紧凑合并块 */
.bk-block-compact {
  padding: 10px 12px !important;
  gap: 6px !important;
}
.bk-compact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.bk-compact-title h3 {
  font-size: 12px !important;
}
.bk-compact-stats .bk-stat-mini {
  padding: 1px 6px;
}
.bk-compact-stats .bk-stat-mini-val {
  font-size: 13px;
}
.bk-compact-inputs {
  margin-top: 4px;
}
.bk-compact-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, oklch(0.9 0.012 120), transparent);
  margin: 2px 0;
}
.bk-chips-compact {
  padding: 8px 10px;
  min-height: 36px;
}

/* 页眉元信息 */
.bk-header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.bk-header-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: oklch(0.52 0.02 150);
}
.bk-header-meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.bk-header-meta-label {
  color: oklch(0.55 0.02 150);
}
.bk-header-meta-val {
  color: oklch(0.38 0.055 160);
  font-weight: 500;
  font-family: 'Noto Serif SC', serif;
}
.bk-header-meta-sep {
  width: 1px;
  height: 10px;
  background: oklch(0.88 0.012 120);
}

/* 标签文字缩小 */
.bk-label {
  font-size: 12px;
}

/* 输入框高度统一 */
.bk-input .el-input__wrapper,
.bk-input .el-select__wrapper,
.bk-input .el-date-editor.el-input__wrapper {
  padding: 0 12px !important;
  min-height: 34px !important;
}
.bk-input .el-input__inner {
  height: 34px;
  font-size: 13px;
  font-family: inherit;
  color: oklch(0.24 0.02 160);
}
.bk-input .el-input-number {
  width: 100%;
}
.bk-input .el-input-number .el-input__wrapper {
  height: 34px;
  min-height: 34px;
}
.bk-input textarea.el-input__inner {
  padding: 8px 12px;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.5;
}

/* 底部body padding */
.bk-body {
  padding: 10px 14px;
  max-height: 62vh;
}

/* ============ 打印按钮组 ============ */
.bk-footer-print {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.bk-btn-print {
  background: oklch(0.55 0.12 250);
  border-color: oklch(0.55 0.12 250);
  color: oklch(0.98 0.01 95);
  font-weight: 500;
  padding: 8px 16px;
  font-size: 13px;
}
.bk-btn-print:hover {
  background: oklch(0.50 0.14 250);
  border-color: oklch(0.50 0.14 250);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.15);
}
.bk-btn-print:active {
  transform: scale(0.97);
}
.bk-btn-notify {
  background: oklch(0.65 0.18 145);
  border-color: oklch(0.65 0.18 145);
  color: oklch(0.98 0.01 95);
  font-weight: 500;
  padding: 8px 16px;
  font-size: 13px;
}
.bk-btn-notify:hover {
  background: oklch(0.58 0.20 145);
  border-color: oklch(0.58 0.20 145);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(34, 197, 94, 0.2);
}
.bk-btn-notify:active {
  transform: scale(0.97);
}
.bk-btn-kitchen {
  background: oklch(0.60 0.15 80);
  border-color: oklch(0.60 0.15 80);
  color: oklch(0.98 0.01 95);
  font-weight: 500;
  padding: 8px 16px;
  font-size: 13px;
}
.bk-btn-kitchen:hover {
  background: oklch(0.54 0.17 80);
  border-color: oklch(0.54 0.17 80);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(245, 158, 11, 0.2);
}
.bk-btn-kitchen:active {
  transform: scale(0.97);
}
.bk-btn-disabled {
  background: oklch(0.92 0.01 95) !important;
  border-color: oklch(0.85 0.01 95) !important;
  color: oklch(0.65 0.01 95) !important;
  cursor: not-allowed !important;
  opacity: 0.7;
}
.bk-btn-disabled:hover {
  transform: none !important;
  box-shadow: none !important;
}
.bk-notify-dialog .bk-notify-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.bk-notify-info {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 14px;
  background: oklch(0.95 0.02 95);
  border-radius: 8px;
  font-size: 13px;
  color: oklch(0.40 0.02 250);
}
.bk-notify-textarea-wrap {
  position: relative;
}
.bk-notify-textarea {
  width: 100%;
  min-height: 320px;
  padding: 14px 16px;
  border: 1px solid oklch(0.85 0.02 95);
  border-radius: 10px;
  font-family: 'PingFang SC', 'Microsoft YaHei', monospace;
  font-size: 14px;
  line-height: 1.8;
  color: oklch(0.25 0.02 250);
  resize: vertical;
  background: oklch(0.99 0.005 95);
  transition: border-color 0.2s, box-shadow 0.2s;
  white-space: pre-wrap;
}
.bk-notify-textarea:focus {
  outline: none;
  border-color: oklch(0.65 0.18 145);
  box-shadow: 0 0 0 3px oklch(0.65 0.18 145 / 0.15);
}
.bk-notify-textarea::placeholder {
  color: oklch(0.70 0.02 95);
}
.bk-notify-tips {
  font-size: 12px;
  color: oklch(0.60 0.02 250);
  padding: 0 4px;
}
.bk-notify-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.bk-customer-hint {
  margin: 12px 0;
  padding: 14px 16px;
  background: linear-gradient(135deg, oklch(0.95 0.03 145), oklch(0.97 0.02 95));
  border: 1px solid oklch(0.80 0.03 145);
  border-radius: 12px;
  position: relative;
}
.bk-customer-hint-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: oklch(0.35 0.08 145);
  margin-bottom: 10px;
}
.bk-customer-hint-badge {
  background: oklch(0.65 0.18 45);
  color: white;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.bk-customer-hint-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.bk-customer-stat {
  background: white;
  padding: 10px 12px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}
.bk-customer-stat-val {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: oklch(0.30 0.02 250);
  line-height: 1.3;
}
.bk-customer-stat-amount {
  color: oklch(0.55 0.18 145);
}
.bk-customer-stat-label {
  display: block;
  font-size: 11px;
  color: oklch(0.55 0.02 95);
  margin-top: 2px;
}
.bk-customer-hint-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed oklch(0.85 0.02 95);
  font-size: 12px;
  color: oklch(0.50 0.02 250);
}
.bk-customer-hint-btn {
  background: none;
  border: 1px solid oklch(0.65 0.18 145);
  color: oklch(0.55 0.18 145);
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.bk-customer-hint-btn:hover {
  background: oklch(0.65 0.18 145);
  color: white;
}
.bk-history-report {
  background: linear-gradient(135deg, oklch(0.97 0.02 250), oklch(0.98 0.01 95));
  border: 1px solid oklch(0.82 0.02 250);
  border-radius: 12px;
  padding: 14px 18px;
  margin-bottom: 16px;
}
.bk-history-report-title {
  font-size: 14px;
  font-weight: 600;
  color: oklch(0.30 0.04 250);
  margin-bottom: 12px;
}
.bk-history-report-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.bk-history-stat {
  background: white;
  border-radius: 10px;
  padding: 12px 14px;
  text-align: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
}
.bk-history-stat-val {
  font-size: 18px;
  font-weight: 700;
  color: oklch(0.35 0.08 250);
  line-height: 1.3;
}
.bk-history-stat-label {
  font-size: 11px;
  color: oklch(0.55 0.02 95);
  margin-top: 4px;
}
</style>
