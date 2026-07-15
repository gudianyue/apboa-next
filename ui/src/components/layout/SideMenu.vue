<script setup lang="ts">
/**
 * 侧边栏菜单组件
 *
 * @author huxuehao
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Component } from 'vue'
import {
  DashboardOutlined,
  RobotOutlined,
  ApartmentOutlined,
  DatabaseOutlined,
  ApiOutlined,
  AppstoreOutlined,
  CloudServerOutlined,
  ToolOutlined,
  LoginOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
  AuditOutlined,
  KeyOutlined,
  MonitorOutlined,
  CloudUploadOutlined,
  UserOutlined,
  TeamOutlined,
  GlobalOutlined,
  SettingOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

/**
 * 菜单项类型定义
 */
interface MenuItem {
  key: string
  label: string
  icon?: Component
  path: string
  type: 'menu' | 'category'
}

/**
 * 菜单配置
 */
const menuConfig: MenuItem[] = [
  // 工作台
  {
    key: 'dashboard',
    label: '工作台',
    icon: DashboardOutlined,
    path: '/dashboard',
    type: 'menu'
  },
  // 开发分类
  {
    key: 'dev-category',
    label: '开发',
    path: '',
    type: 'category'
  },
  {
    key: 'agent',
    label: '智能体',
    icon: RobotOutlined,
    path: '/agent',
    type: 'menu'
  },
  {
    key: 'workflow',
    label: '工作流',
    icon: ApartmentOutlined,
    path: '/workflow',
    type: 'menu'
  },
  {
    key: 'knowledge',
    label: '知识库',
    icon: DatabaseOutlined,
    path: '/knowledge',
    type: 'menu'
  },
  // 资源分类
  {
    key: 'resource-category',
    label: '资源',
    path: '',
    type: 'category'
  },
  {
    key: 'model',
    label: '模型',
    icon: ApiOutlined,
    path: '/model',
    type: 'menu'
  },
  {
    key: 'skill',
    label: '技能',
    icon: AppstoreOutlined,
    path: '/skill',
    type: 'menu'
  },
  {
    key: 'mcp',
    label: 'MCP',
    icon: CloudServerOutlined,
    path: '/mcp',
    type: 'menu'
  },
  {
    key: 'tool',
    label: '工具',
    icon: ToolOutlined,
    path: '/tool',
    type: 'menu'
  },
  {
    key: 'hook',
    label: '扩展',
    icon: LoginOutlined,
    path: '/hook',
    type: 'menu'
  },
  {
    key: 'prompt',
    label: '提示词',
    icon: FileTextOutlined,
    path: '/prompt',
    type: 'menu'
  },
  {
    key: 'sensitive',
    label: '敏感词',
    icon: SafetyCertificateOutlined,
    path: '/sensitive',
    type: 'menu'
  },
  // 审查分类
  // {
  //   key: 'review-category',
  //   label: '审查',
  //   path: '',
  //   type: 'category'
  // },
  // {
  //   key: 'review-agent',
  //   label: '智能体',
  //   icon: AuditOutlined,
  //   path: '/review/agent',
  //   type: 'menu'
  // },
  // {
  //   key: 'review-workflow',
  //   label: '工作流',
  //   icon: AuditOutlined,
  //   path: '/review/workflow',
  //   type: 'menu'
  // }
]

/**
 * 当前激活的菜单项
 */
const activeMenu = computed(() => {
  const path = route.path
  // 找到匹配的菜单项
  const menuItem = menuConfig.find(item => {
    if (item.type !== 'menu') return false
    // 精确匹配或前缀匹配
    return path === item.path || path.startsWith(item.path + '/')
  })
  return menuItem?.key || ''
})

/**
 * 处理菜单点击
 */
const handleMenuClick = (item: MenuItem) => {
  if (item.type === 'menu' && item.path) {
    router.push(item.path)
  }
}
</script>

<template>
  <div class="side-menu-wrapper">
    <div class="side-menu">
      <template v-for="item in menuConfig" :key="item.key">
        <!-- 分类 -->
        <div v-if="item.type === 'category'" class="menu-category">
          {{ item.label }}
        </div>
        <!-- 菜单项 -->
        <div
          v-else
          class="menu-item"
          :class="{ active: activeMenu === item.key }"
          @click="handleMenuClick(item)"
        >
          <component
            v-if="item.icon"
            :is="item.icon"
            class="menu-icon"
          />
          <span class="menu-label">{{ item.label }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.side-menu {
  padding: 4px 12px;
}

.menu-category {
  font-size: 11px;
  color: #999;
  padding: 16px 6px 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 500;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.2s ease;
  margin: 2px 0;

  &:hover {
    background-color: #E9EAEA;
    color: #666;
  }

  &.active {
    color: #000000;
    background-color: #E9EAEA;

    .menu-icon {
      color: #000000;
    }
  }
}

.menu-icon {
  font-size: 16px;
  color: #999;
  flex-shrink: 0;
  transition: color 0.2s ease;

  .menu-item:hover & {
    color: #666;
  }

  .menu-item.active & {
    color: #000000;
  }
}

.menu-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
