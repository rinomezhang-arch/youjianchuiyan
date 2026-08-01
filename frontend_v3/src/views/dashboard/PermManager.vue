<template>
  <div class="perm-manager-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">权限管理 · Permission Management</h2>
        <p class="page-subtitle">角色权限 · 访问控制 · 安全设置</p>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索员工姓名、工号"
        prefix-icon="Search"
        clearable
        class="search-input"
        @input="onSearch"
      />
    </div>

    <!-- 员工列表表格 -->
    <div class="staff-table-card">
      <table class="staff-table">
        <thead>
          <tr>
            <th>员工姓名</th>
            <th>角色</th>
            <th>部门</th>
            <th>权限级别</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in filteredStaff" :key="s.staffId">
            <td>
              <div class="staff-name-cell">
                <span class="staff-avatar">{{ (s.staffName || '?').charAt(0) }}</span>
                <div class="staff-name-info">
                  <span class="staff-name">{{ s.staffName }}</span>
                  <span class="staff-account">{{ s.staffAccount }}</span>
                </div>
              </div>
            </td>
            <td><span class="role-tag">{{ s.roleName || '未设置' }}</span></td>
            <td><span class="dept-text">{{ s.department || '未分配' }}</span></td>
            <td>
              <span :class="['perm-tag', 'perm-' + s.permissionLevel]">
                {{ levelName(s.permissionLevel) }}
              </span>
            </td>
            <td>
              <button class="action-btn" @click="openEdit(s)">角色权限 →</button>
            </td>
          </tr>
        </tbody>
      </table>
      <el-empty v-if="filteredStaff.length === 0" description="无匹配员工" />
    </div>

    <!-- 权限编辑弹窗 -->
    <el-dialog
      v-model="showEditor"
      :title="'编辑权限 - ' + editingStaff?.staffName"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form v-if="editingStaff" label-width="100px" class="perm-form">
        <el-form-item label="权限等级">
          <el-radio-group v-model="editForm.permissionLevel">
            <el-radio-button :value="1">普通员工</el-radio-button>
            <el-radio-button :value="2">主管</el-radio-button>
            <el-radio-button :value="3">部门负责人</el-radio-button>
            <el-radio-button :value="4">门店经理</el-radio-button>
            <el-radio-button :value="99">超级管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="所属部门">
          <el-select v-model="editForm.deptId" placeholder="选择部门" clearable style="width:100%">
            <el