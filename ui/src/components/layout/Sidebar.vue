<script setup lang="ts">
/**
 * 侧边栏组件
 *
 * @author huxuehao
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAccountStore } from '@/stores'
import { RoutePaths } from '@/router/constants'
import SideMenu from './SideMenu.vue'
import {
  TeamOutlined,
  SwapOutlined,
  PlusOutlined,
  LogoutOutlined,
  SettingOutlined,
  BookOutlined
} from '@ant-design/icons-vue'
import { Modal, message } from 'ant-design-vue'
import SettingsModal from '@/components/settings/SettingsModal.vue'
import type { TenantInfo } from '@/types'
import { ref, h } from 'vue'

const route = useRoute()
const router = useRouter()
const {
  logout,
  userInfo,
  currentTenant,
  availableTenants,
  switchTenant
} = useAccountStore()

/** 系统设置模态窗显示状态 */
const settingsVisible = ref(false)

/** 租户切换加载中 */
const switchingTenant = ref(false)

/**
 * 用户名首字母(用于头像)
 */
const avatarText = computed(() => {
  if (!userInfo?.username) return '?'
  return userInfo.username.charAt(0).toUpperCase()
})

/**
 * 当前租户显示名称
 */
const currentTenantName = computed(() => {
  return currentTenant?.tenantName || currentTenant?.tenantCode || '未选择'
})

/**
 * 切换租户
 */
async function handleSwitchTenant(tenant: TenantInfo) {
  if (tenant.tenantId === currentTenant?.tenantId) return
  Modal.confirm({
    title: '切换工作空间',
    icon: null,
    content: `确认切换到「${tenant.tenantName}」？切换后当前页面将刷新。`,
    onOk: async () => {
      try {
        switchingTenant.value = true
        await switchTenant({ tenantId: tenant.tenantId })
        message.success('已切换工作空间')
        location.reload()
      } catch {
        message.error('切换失败，请重试')
      } finally {
        switchingTenant.value = false
      }
    }
  })
}

/**
 * 打开发现租户页面
 */
function openTenantDiscovery() {
  settingsVisible.value = true
}

/**
 * 打开文档
 */
const openMarkdownDoc = () => {
  window.open('/doc.html#/', '_blank')
}

/**
 * 退出登录
 */
const handleLogout = () => {
  Modal.confirm({
    title: '确认',
    icon: null,
    content: '确认退出当前系统,是否继续?',
    onOk: async () => {
      await logout()
      await router.push(RoutePaths.LOGIN)
    }
  })
}
</script>

<template>
  <div class="sidebar flex flex-col">
    <!-- 顶部Logo区域 -->
    <div class="sidebar-logo">
      <div class="logo-container flex items-center gap-sm">
        <img src="@/assets/images/logo/logo.png" alt="logo" width="38" style="border-radius: 50%; background-color: white; padding: 4px">
        <span class="project-name">Apboa</span>
      </div>
    </div>

    <!-- 租户切换区域 -->
    <div class="sidebar-tenant" v-if="availableTenants.length > 0">
      <div class="tenant-switcher" @click="openTenantDiscovery">
        <div class="tenant-info flex items-center gap-xs">
          <TeamOutlined />
          <span class="tenant-name">{{ currentTenantName }}</span>
        </div>
        <SwapOutlined class="tenant-icon" />
      </div>
    </div>

    <!-- 菜单区域 -->
    <div class="sidebar-menu flex-1">
      <SideMenu />
    </div>

    <!-- 定制菜单区域 -->
    <div class="sidebar-custom">
      <div class="custom-item" @click="openMarkdownDoc">
        <BookOutlined />
        <span>文档</span>
      </div>
      <div class="custom-item" @click="settingsVisible = true">
        <SettingOutlined />
        <span>设置</span>
      </div>
    </div>

    <!-- 底部用户区域 -->
    <div class="sidebar-user">
      <div class="user-info flex items-center gap-sm">
        <div class="user-avatar">
          {{ avatarText }}
        </div>
        <div class="user-details">
          <div class="user-name">{{ userInfo?.username || '未登录' }}</div>
          <div class="user-role">{{ userInfo?.tenantRole || '普通用户' }}</div>
        </div>
        <div class="user-actions">
          <LogoutOutlined @click="handleLogout" class="logout-icon" />
        </div>
      </div>
    </div>

    <!-- 系统设置模态窗 -->
    <AModal
      v-model:open="settingsVisible"
      wrap-class-name="full-modal"
      :footer="null"
      :destroyOnClose="true"
      :width="'100%'"
    >
      <SettingsModal />
    </AModal>
  </div>
</template>

<style scoped lang="scss">
.sidebar {
  width: 260px;
  height: 100vh;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-logo {
  padding: 16px 20px;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.project-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.sidebar-tenant {
  padding: 12px 16px;
}

.tenant-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: #1677ff;
    background-color: #f5f8ff;
  }
}

.tenant-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
}

.tenant-name {
  font-size: 13px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tenant-icon {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.sidebar-custom {
  padding: 12px 16px;
}

.custom-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.2s ease;

  &:hover {
    background-color: #e7e7e7;
    color: #1a1a1a;
  }
}

.sidebar-user {
  padding: 16px 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1677ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.user-actions {
  flex-shrink: 0;
}

.logout-icon {
  font-size: 16px;
  color: #999;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.2s ease;

  &:hover {
    color: #ff4d4f;
    background-color: #ffe3e2;
  }
}

/* 滚动条样式 */
.sidebar-menu {
  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background-color: #d9d9d9;
    border-radius: 4px;
  }
}
</style>
