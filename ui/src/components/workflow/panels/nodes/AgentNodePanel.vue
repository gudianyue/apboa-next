<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import PanelSection from '../shared/PanelSection.vue'
import FormatterGuideModal from '../shared/FormatterGuideModal.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import ConfigCodeEditor from '@/components/editor/ConfigCodeEditor.vue'
import ExtendConfigEditor, { type ExtendConfigData } from '@/components/model/ExtendConfigEditor.vue'
import * as modelApi from '@/api/model'
import * as skillApi from '@/api/skill'
import * as toolApi from '@/api/tool'
import * as mcpApi from '@/api/mcp'
import { RoutePaths } from '@/router/constants.ts'
import { countCommonElements } from '@/utils/tools'
import {
  getMcpConnectionStatusColor,
  getMcpConnectionStatusText,
  getMcpUnavailableReason,
} from '@/composables/useMcpPresentation'
import type {
  McpServerVO,
  McpToolVO,
  ModelConfigVO,
  ModelProviderVO,
  SkillPackageVO,
  ToolVO,
} from '@/types'
import { McpActivationStatus, McpToolExposureMode } from '@/types'
import type { WorkflowFlowEdge, WorkflowFlowNode, WorkflowResourceMaps } from '@/types/workflow'

interface AgentNodeMcpConfig {
  mcpServerId: string
  exposureMode: McpToolExposureMode
  mcpToolIds: string[]
}

interface AgentModelParamsOverride {
  temperature?: number
  topP?: number
  topK?: number
  maxTokens?: number
  repeatPenalty?: number
  seed?: string | number
  streaming?: boolean
  extendConfig?: ExtendConfigData | null
}

interface AgentNodeConfig {
  modelConfigId?: string
  modelParamsOverrideEnabled?: boolean
  modelParamsOverride?: AgentModelParamsOverride
  formatterType?: 'STRING' | 'VELOCITY'
  systemPrompt?: string
  userPrompt?: string
  skillPackageIds?: string[]
  toolIds?: string[]
  mcps?: AgentNodeMcpConfig[]
  maxIterations?: number
  structuredOutputEnabled?: boolean
  structuredOutput?: Record<string, unknown>
}

const props = defineProps<{
  node: WorkflowFlowNode
  nodes: WorkflowFlowNode[]
  edges: WorkflowFlowEdge[]
  resources: WorkflowResourceMaps
}>()

const emit = defineEmits<{ update: [node: WorkflowFlowNode] }>()

const panelRoot = ref<HTMLElement>()
const isEditorMaximized = ref(false)
const loading = ref(false)
const modelProviders = ref<ModelProviderVO[]>([])
const allModels = ref<ModelConfigVO[]>([])
const toolCategories = ref<string[]>([])
const allTools = ref<ToolVO[]>([])
const skillCategories = ref<string[]>([])
const allSkills = ref<SkillPackageVO[]>([])
const allMcpServers = ref<McpServerVO[]>([])
const mcpToolMap = ref<Record<string, McpToolVO[]>>({})
const toolLoadingMap = ref<Record<string, boolean>>({})
const selectedProviderId = ref('')
const modelParamsForm = ref<AgentModelParamsOverride>({})
const structuredOutputText = ref('{}')
const structuredOutputError = ref('')

const config = computed(() => (props.node.data.config || {}) as AgentNodeConfig)

const formatterOptions = [
  { label: '字符串', value: 'STRING' },
  { label: 'Velocity', value: 'VELOCITY' },
]

const exposureModeOptions = [
  { label: '继承全局', value: McpToolExposureMode.ALL_GLOBAL },
  { label: '局部选择', value: McpToolExposureMode.SELECTED_ONLY },
]

const providerOptions = computed(() =>
  modelProviders.value.map((provider) => ({
    label: provider.name,
    value: String(provider.id),
  })),
)

const currentModels = computed(() => {
  if (!selectedProviderId.value) {
    const selected = allModels.value.find((model) => String(model.id) === String(config.value.modelConfigId || ''))
    selectedProviderId.value = selected ? String(selected.providerId) : String(allModels.value[0]?.providerId || '')
  }
  return allModels.value.filter((model) => String(model.providerId) === selectedProviderId.value)
})

const toolCategoryList = computed(() => {
  if (toolCategories.value.length > 0) return toolCategories.value
  return [...new Set(allTools.value.map((tool) => tool.category || '默认'))]
})

const skillCategoryList = computed(() => {
  if (skillCategories.value.length > 0) return skillCategories.value
  return [...new Set(allSkills.value.map((skill) => skill.category || '默认'))]
})

const toolsByCategory = computed(() => {
  const groups: Record<string, ToolVO[]> = {}
  toolCategoryList.value.forEach((category) => {
    groups[category] = allTools.value.filter((tool) => (tool.category || '默认') === category)
  })
  return groups
})

const skillsByCategory = computed(() => {
  const groups: Record<string, SkillPackageVO[]> = {}
  skillCategoryList.value.forEach((category) => {
    groups[category] = allSkills.value.filter((skill) => (skill.category || '默认') === category)
  })
  return groups
})

const selectedToolIds = computed(() => arrayConfig('toolIds'))
const selectedSkillIds = computed(() => arrayConfig('skillPackageIds'))
const mcpBindings = computed(() => (Array.isArray(config.value.mcps) ? config.value.mcps : []))
const selectedMcpIds = computed(() => new Set(mcpBindings.value.map((item) => String(item.mcpServerId))))

const mcpProtocols = computed(() => [...new Set(allMcpServers.value.map((server) => String(server.protocol || 'MCP')))])

const mcpServersByProtocol = computed(() => {
  const groups: Record<string, McpServerVO[]> = {}
  mcpProtocols.value.forEach((protocol) => {
    groups[protocol] = allMcpServers.value.filter((server) => String(server.protocol || 'MCP') === protocol)
  })
  return groups
})

const showFixedSystemMessage = computed(() => {
  const model = allModels.value.find((item) => String(item.id) === String(config.value.modelConfigId || ''))
  if (!model) return false
  const provider = modelProviders.value.find((item) => String(item.id) === String(model.providerId))
  return String(provider?.type || '') === 'OPEN_AI'
})

const extendConfigForm = computed({
  get: () => (modelParamsForm.value.extendConfig as ExtendConfigData) || null,
  set: (value: ExtendConfigData | null) => {
    modelParamsForm.value = { ...modelParamsForm.value, extendConfig: value }
  },
})

function updateNode(patch: Partial<WorkflowFlowNode['data']>) {
  emit('update', { ...props.node, data: { ...props.node.data, ...patch } })
}

function updateConfig(key: keyof AgentNodeConfig, value: unknown) {
  updateNode({ config: { ...(props.node.data.config || {}), [key]: value } })
}

function arrayConfig(key: 'toolIds' | 'skillPackageIds') {
  const value = config.value[key]
  return Array.isArray(value) ? value.map((item) => String(item)) : []
}

function onEditorMaximizeChange(value: boolean) {
  isEditorMaximized.value = value
}

async function loadModelProviders() {
  const response = await modelApi.providerPage({ page: 1, size: 100, enabled: true })
  modelProviders.value = response.data.data.records || []
}

async function loadAllModels() {
  const response = await modelApi.configPage({ page: 1, size: 1000, enabled: true })
  allModels.value = response.data.data.records || []
}

async function loadToolCategories() {
  const response = await toolApi.listCategories()
  toolCategories.value = response.data.data || []
}

async function loadAllTools() {
  const response = await toolApi.page({ page: 1, size: 1000, enabled: true })
  allTools.value = response.data.data.records || []
}

async function loadSkillCategories() {
  const response = await skillApi.listCategories()
  skillCategories.value = response.data.data || []
}

async function loadAllSkills() {
  const response = await skillApi.page({ page: 1, size: 1000, enabled: true })
  allSkills.value = response.data.data.records || []
}

async function loadAllMcpServers() {
  const response = await mcpApi.page({ page: 1, size: 1000 })
  allMcpServers.value = response.data.data.records || []
}

async function handleModelChange(modelId: string) {
  updateConfig('modelConfigId', modelId)
  if (config.value.modelParamsOverrideEnabled) {
    await loadModelParams(modelId)
  }
}

async function loadModelParams(modelId: string) {
  const response = await modelApi.configDetail(modelId)
  const model = response.data.data
  const extendConfig = normalizeExtendConfig(model.extendConfig as ExtendConfigData | null | undefined)
  modelParamsForm.value = {
    temperature: model.temperature,
    topP: model.topP,
    topK: model.topK,
    maxTokens: model.maxTokens,
    repeatPenalty: model.repeatPenalty,
    seed: model.seed,
    streaming: model.streaming,
    extendConfig,
  }
}

function normalizeExtendConfig(value: ExtendConfigData | null | undefined) {
  if (!value || typeof value !== 'object') return null
  return {
    headers: value.headers || {},
    queryParams: value.queryParams || {},
    bodyParams: value.bodyParams || {},
    fixedSystemMessage: value.fixedSystemMessage ?? false,
  }
}

async function handleOverrideToggle(checked: boolean) {
  updateConfig('modelParamsOverrideEnabled', checked)
  if (checked && config.value.modelConfigId) {
    await loadModelParams(String(config.value.modelConfigId))
  } else {
    modelParamsForm.value = {}
    updateConfig('modelParamsOverride', {})
  }
}

function handleToolChange(toolId: string, checked: boolean) {
  const next = new Set(selectedToolIds.value)
  checked ? next.add(toolId) : next.delete(toolId)
  updateConfig('toolIds', [...next])
}

function handleSkillChange(skillId: string, checked: boolean) {
  const next = new Set(selectedSkillIds.value)
  checked ? next.add(skillId) : next.delete(skillId)
  updateConfig('skillPackageIds', [...next])
}

function getBinding(mcpId: string) {
  return mcpBindings.value.find((item) => String(item.mcpServerId) === mcpId)
}

function isMcpSelected(mcpId: string) {
  return selectedMcpIds.value.has(mcpId)
}

function isMcpRuntimeAvailable(mcp: McpServerVO) {
  return Boolean(mcp.enabled)
    && mcp.activationStatus === McpActivationStatus.ACTIVE
    && (mcp.availableToolCount || 0) > 0
}

function isMcpSelectable(mcp: McpServerVO) {
  return isMcpRuntimeAvailable(mcp) || isMcpSelected(String(mcp.id))
}

function updateMcpBindings(next: AgentNodeMcpConfig[]) {
  updateConfig('mcps', next)
}

function handleMcpChange(mcpId: string, checked: boolean) {
  const current = [...mcpBindings.value]
  if (checked) {
    if (!current.some((item) => String(item.mcpServerId) === mcpId)) {
      current.push({ mcpServerId: mcpId, exposureMode: McpToolExposureMode.ALL_GLOBAL, mcpToolIds: [] })
    }
  } else {
    updateMcpBindings(current.filter((item) => String(item.mcpServerId) !== mcpId))
    return
  }
  updateMcpBindings(current)
}

async function handleExposureModeChange(mcpId: string, exposureMode: McpToolExposureMode) {
  const next = mcpBindings.value.map((item) =>
    String(item.mcpServerId) === mcpId ? { ...item, exposureMode } : item,
  )
  updateMcpBindings(next)
  if (exposureMode === McpToolExposureMode.SELECTED_ONLY) {
    await loadMcpTools(mcpId)
  }
}

function getMcpTools(mcpId: string) {
  return mcpToolMap.value[mcpId] || []
}

function isToolLoading(mcpId: string) {
  return Boolean(toolLoadingMap.value[mcpId])
}

function isMcpToolAvailable(tool: McpToolVO) {
  return Boolean(tool.enabled) && !tool.missing
}

function isMcpToolSelected(mcpId: string, toolId: string) {
  return getBinding(mcpId)?.mcpToolIds?.map((item) => String(item)).includes(toolId) || false
}

function isMcpToolSelectable(mcpId: string, tool: McpToolVO) {
  return isMcpToolAvailable(tool) || isMcpToolSelected(mcpId, String(tool.id))
}

async function loadMcpTools(mcpId: string) {
  if (mcpToolMap.value[mcpId]) return
  toolLoadingMap.value = { ...toolLoadingMap.value, [mcpId]: true }
  try {
    const response = await mcpApi.listTools(mcpId)
    mcpToolMap.value = { ...mcpToolMap.value, [mcpId]: response.data.data || [] }
  } finally {
    toolLoadingMap.value = { ...toolLoadingMap.value, [mcpId]: false }
  }
}

function handleMcpToolChange(mcpId: string, toolId: string, checked: boolean) {
  const next = mcpBindings.value.map((item) => {
    if (String(item.mcpServerId) !== mcpId) return item
    const ids = new Set((item.mcpToolIds || []).map((id) => String(id)))
    checked ? ids.add(toolId) : ids.delete(toolId)
    return { ...item, mcpToolIds: [...ids] }
  })
  updateMcpBindings(next)
}

function handleStructuredOutputToggle(checked: boolean) {
  updateConfig('structuredOutputEnabled', checked)
  if (checked) {
    syncStructuredOutputText(config.value.structuredOutput || {})
  }
}

function syncStructuredOutputText(value: unknown) {
  structuredOutputText.value = JSON.stringify(value && typeof value === 'object' ? value : {}, null, 2)
  structuredOutputError.value = ''
}

function handleStructuredOutputChange(value: string) {
  structuredOutputText.value = value
  try {
    const parsed = JSON.parse(value || '{}')
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      structuredOutputError.value = '结构化输出必须是 JSON 对象'
      return
    }
    structuredOutputError.value = ''
    updateConfig('structuredOutput', parsed)
  } catch {
    structuredOutputError.value = 'JSON 格式不正确'
  }
}

async function preloadSelectedMcpTools() {
  const targets = mcpBindings.value
    .filter((item) => item.exposureMode === McpToolExposureMode.SELECTED_ONLY)
    .map((item) => String(item.mcpServerId))
  await Promise.all(targets.map((id) => loadMcpTools(id)))
}

watch(modelParamsForm, (value) => {
  if (config.value.modelParamsOverrideEnabled) {
    updateConfig('modelParamsOverride', { ...value })
  }
}, { deep: true })

watch(() => config.value.structuredOutput, (value) => {
  if (!structuredOutputError.value) {
    syncStructuredOutputText(value || {})
  }
}, { deep: true })

onMounted(async () => {
  syncStructuredOutputText(config.value.structuredOutput || {})
  if (config.value.modelParamsOverride && Object.keys(config.value.modelParamsOverride).length > 0) {
    modelParamsForm.value = {
      ...config.value.modelParamsOverride,
      extendConfig: normalizeExtendConfig(config.value.modelParamsOverride.extendConfig),
    }
  }

  try {
    loading.value = true
    await Promise.all([
      loadModelProviders(),
      loadAllModels(),
      loadToolCategories(),
      loadAllTools(),
      loadSkillCategories(),
      loadAllSkills(),
      loadAllMcpServers(),
    ])
    await preloadSelectedMcpTools()
  } catch (error) {
    message.error('加载 Agent 节点配置资源失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div ref="panelRoot" class="agent-node-panel" :class="{ 'editor-maximized': isEditorMaximized }">
    <ASpin :spinning="loading">
      <AForm layout="vertical">
        <PanelSection title="节点名称">
          <NodeNameInput
            :model-value="node.data.label"
            @update:model-value="(value: any) => updateNode({ label: value })"
          />
        </PanelSection>

        <InputBindingSection
          :model-value="node.data.inputConfigs"
          :nodes="nodes"
          :edges="edges"
          :current-node-id="node.id"
          @update:model-value="(value: any) => updateNode({ inputConfigs: value })"
        />

        <PanelSection title="模型与提示词">
          <AFormItem label="模型配置" required>
            <template v-if="providerOptions.length > 0">
              <ASegmented
                v-model:value="selectedProviderId"
                :options="providerOptions"
                class="provider-tabs"
              />
              <ARadioGroup
                :value="config.modelConfigId"
                class="model-radio-group"
                @update:value="(value: any) => handleModelChange(String(value))"
              >
                <div v-if="currentModels.length > 0" class="model-grid">
                  <ARadio
                    v-for="model in currentModels"
                    :key="model.id"
                    :value="String(model.id)"
                    class="model-radio"
                  >
                    <div class="model-info">
                      <div class="model-name" :title="model.name">{{ model.name }}</div>
                      <div class="model-desc" :title="model.description || model.modelId">
                        {{ model.description || model.modelId }}
                      </div>
                    </div>
                  </ARadio>
                </div>
                <AEmpty v-else description="当前供应商暂无可用模型" />
              </ARadioGroup>
            </template>
            <div v-else class="empty-action">
              <AButton type="text">未配置模型</AButton>
              <AButton type="link" :href="`/#/${RoutePaths.MODEL}`" target="_blank">去配置</AButton>
              <AButton type="link" @click="loadModelProviders(); loadAllModels()">刷新</AButton>
            </div>
          </AFormItem>

          <div class="config-row">
            <span class="config-row-label">提示词模板</span>
            <div class="formatter-selector">
              <FormatterGuideModal />
              <ASelect
                :value="config.formatterType || 'STRING'"
                :options="formatterOptions"
                style="width: 130px"
                @update:value="(value: any) => updateConfig('formatterType', value)"
              />
            </div>
          </div>

          <AFormItem label="系统提示词" required>
            <ConfigCodeEditor
              :model-value="String(config.systemPrompt || '')"
              language="txt"
              placeholder="输入系统提示词，可使用 ${input} 引用输入绑定"
              height="180px"
              :maximize-target="panelRoot"
              @update:model-value="(value: string) => updateConfig('systemPrompt', value)"
              @maximize-change="onEditorMaximizeChange"
            />
          </AFormItem>

          <AFormItem label="用户提示词" required>
            <ConfigCodeEditor
              :model-value="String(config.userPrompt || '')"
              language="txt"
              placeholder="输入用户提示词，可使用 ${input} 引用输入绑定"
              height="180px"
              :maximize-target="panelRoot"
              @update:model-value="(value: string) => updateConfig('userPrompt', value)"
              @maximize-change="onEditorMaximizeChange"
            />
          </AFormItem>
        </PanelSection>

        <PanelSection title="参数覆盖">
          <div class="switch-row">
            <span class="switch-label">启用参数覆盖</span>
            <ASwitch
              :checked="Boolean(config.modelParamsOverrideEnabled)"
              :disabled="!config.modelConfigId"
              @update:checked="(value: any) => handleOverrideToggle(Boolean(value))"
            />
          </div>

          <div v-if="config.modelParamsOverrideEnabled" class="params-grid">
            <AFormItem label="Temperature">
              <AInputNumber v-model:value="modelParamsForm.temperature" :min="0" :max="2" :step="0.1" />
            </AFormItem>
            <AFormItem label="Top P">
              <AInputNumber v-model:value="modelParamsForm.topP" :min="0" :max="1" :step="0.1" />
            </AFormItem>
            <AFormItem label="Top K">
              <AInputNumber v-model:value="modelParamsForm.topK" :min="0" :max="100" />
            </AFormItem>
            <AFormItem label="Max Tokens">
              <AInputNumber v-model:value="modelParamsForm.maxTokens" :min="1" :max="1000000" />
            </AFormItem>
            <AFormItem label="Repeat Penalty">
              <AInputNumber v-model:value="modelParamsForm.repeatPenalty" :min="0" :max="2" :step="0.1" />
            </AFormItem>
            <AFormItem label="Seed">
              <AInput v-model:value="modelParamsForm.seed" placeholder="留空表示随机" />
            </AFormItem>
            <AFormItem label="Streaming">
              <ASwitch v-model:checked="modelParamsForm.streaming" />
            </AFormItem>
          </div>

          <div v-if="config.modelParamsOverrideEnabled" class="extend-config">
            <AFormItem label="扩展配置">
              <ExtendConfigEditor
                v-model="extendConfigForm"
                compact
                :show-fixed-system-message="showFixedSystemMessage"
              />
            </AFormItem>
          </div>
        </PanelSection>

        <PanelSection title="能力选择">
          <ACollapse ghost class="ability-collapse">
            <ACollapsePanel key="skills" :header="`技能包 ${selectedSkillIds.length}/${allSkills.length}`">
              <ACollapse v-if="skillCategoryList.length > 0">
                <ACollapsePanel
                  v-for="category in skillCategoryList"
                  :key="category"
                  :header="`${category} (${countCommonElements(skillsByCategory[category]?.map((item) => String(item.id)) || [], selectedSkillIds)}/${skillsByCategory[category]?.length || 0})`"
                >
                  <div class="checkbox-grid">
                    <ACheckbox
                      v-for="skill in skillsByCategory[category]"
                      :key="skill.id"
                      :checked="selectedSkillIds.includes(String(skill.id))"
                      class="checkbox-item"
                      @change="(event: any) => handleSkillChange(String(skill.id), event.target.checked)"
                    >
                      <div class="item-info">
                        <div class="item-name" :title="skill.name">{{ skill.name }}</div>
                        <div class="item-desc" :title="skill.description">{{ skill.description || '暂无描述' }}</div>
                      </div>
                    </ACheckbox>
                  </div>
                </ACollapsePanel>
              </ACollapse>
              <div v-else class="empty-action">
                <AButton type="text">未配置技能包</AButton>
                <AButton type="link" :href="`/#/${RoutePaths.SKILL}`" target="_blank">去配置</AButton>
                <AButton type="link" @click="loadSkillCategories(); loadAllSkills()">刷新</AButton>
              </div>
            </ACollapsePanel>

            <ACollapsePanel key="tools" :header="`工具 ${selectedToolIds.length}/${allTools.length}`">
              <ACollapse v-if="toolCategoryList.length > 0">
                <ACollapsePanel
                  v-for="category in toolCategoryList"
                  :key="category"
                  :header="`${category} (${countCommonElements(toolsByCategory[category]?.map((item) => String(item.id)) || [], selectedToolIds)}/${toolsByCategory[category]?.length || 0})`"
                >
                  <div class="checkbox-grid">
                    <ACheckbox
                      v-for="tool in toolsByCategory[category]"
                      :key="tool.id"
                      :checked="selectedToolIds.includes(String(tool.id))"
                      class="checkbox-item"
                      @change="(event: any) => handleToolChange(String(tool.id), event.target.checked)"
                    >
                      <div class="item-info">
                        <div class="item-name" :title="tool.name">{{ tool.name }}</div>
                        <div class="item-desc" :title="tool.description">{{ tool.description || tool.toolId }}</div>
                      </div>
                    </ACheckbox>
                  </div>
                </ACollapsePanel>
              </ACollapse>
              <div v-else class="empty-action">
                <AButton type="text">未配置工具</AButton>
                <AButton type="link" :href="`/#/${RoutePaths.TOOL}`" target="_blank">去配置</AButton>
                <AButton type="link" @click="loadToolCategories(); loadAllTools()">刷新</AButton>
              </div>
            </ACollapsePanel>

            <ACollapsePanel key="mcps" :header="`MCP ${mcpBindings.length}/${allMcpServers.length}`">
              <ACollapse v-if="mcpProtocols.length > 0">
                <ACollapsePanel
                  v-for="protocol in mcpProtocols"
                  :key="protocol"
                  :header="`${protocol} (${countCommonElements((mcpServersByProtocol[protocol] || []).map((item) => String(item.id)), [...selectedMcpIds])}/${mcpServersByProtocol[protocol]?.length || 0})`"
                >
                  <div class="mcp-grid">
                    <div
                      v-for="mcp in mcpServersByProtocol[protocol]"
                      :key="mcp.id"
                      class="mcp-item"
                      :class="{ selected: isMcpSelected(String(mcp.id)), unavailable: !isMcpRuntimeAvailable(mcp) }"
                    >
                      <div class="mcp-item-header">
                        <ACheckbox
                          :checked="isMcpSelected(String(mcp.id))"
                          :disabled="!isMcpSelectable(mcp)"
                          @change="(event: any) => handleMcpChange(String(mcp.id), event.target.checked)"
                        >
                          <div class="item-info">
                            <div class="item-name" :title="mcp.name">{{ mcp.name }}</div>
                            <div class="item-desc" :title="mcp.description">{{ mcp.description || '暂无描述' }}</div>
                          </div>
                        </ACheckbox>
                        <div class="mcp-tag-group">
                          <ATag color="default" :bordered="false">{{ mcp.protocol }}</ATag>
                          <ATag :bordered="false" :color="getMcpConnectionStatusColor(mcp)">
                            {{ getMcpConnectionStatusText(mcp) }}
                          </ATag>
                          <ATag :bordered="false" color="processing">可用 {{ mcp.availableToolCount || 0 }}</ATag>
                        </div>
                      </div>

                      <div v-if="isMcpSelected(String(mcp.id))" class="mcp-binding-panel">
                        <div class="binding-row">
                          <span class="binding-label">工具暴露</span>
                          <ASegmented
                            :value="getBinding(String(mcp.id))?.exposureMode || McpToolExposureMode.ALL_GLOBAL"
                            :options="exposureModeOptions"
                            size="small"
                            @change="(value: any) => handleExposureModeChange(String(mcp.id), value)"
                          />
                        </div>
                        <div v-if="!isMcpRuntimeAvailable(mcp)" class="binding-tip warning">
                          {{ getMcpUnavailableReason(mcp) }}
                        </div>

                        <template v-if="getBinding(String(mcp.id))?.exposureMode === McpToolExposureMode.SELECTED_ONLY">
                          <ASpin :spinning="isToolLoading(String(mcp.id))">
                            <AButton
                              v-if="!mcpToolMap[String(mcp.id)]"
                              type="link"
                              class="tool-load-action"
                              @click="loadMcpTools(String(mcp.id))"
                            >
                              加载工具目录
                            </AButton>
                            <AEmpty
                              v-else-if="!getMcpTools(String(mcp.id)).length"
                              description="暂无工具目录"
                            />
                            <div v-else class="tool-grid">
                              <div
                                v-for="tool in getMcpTools(String(mcp.id))"
                                :key="tool.id"
                                class="tool-item"
                                :class="{ unavailable: !isMcpToolAvailable(tool) }"
                              >
                                <ACheckbox
                                  :checked="isMcpToolSelected(String(mcp.id), String(tool.id))"
                                  :disabled="!isMcpToolSelectable(String(mcp.id), tool)"
                                  @change="(event: any) => handleMcpToolChange(String(mcp.id), String(tool.id), event.target.checked)"
                                >
                                  <div class="item-info">
                                    <div class="item-name" :title="tool.toolName">{{ tool.toolName }}</div>
                                    <div class="item-desc" :title="tool.description">{{ tool.description || '暂无描述' }}</div>
                                  </div>
                                </ACheckbox>
                              </div>
                            </div>
                          </ASpin>
                        </template>
                      </div>
                    </div>
                  </div>
                </ACollapsePanel>
              </ACollapse>
              <div v-else class="empty-action">
                <AButton type="text">未配置 MCP 服务</AButton>
                <AButton type="link" :href="`/#/${RoutePaths.MCP}`" target="_blank">去配置</AButton>
                <AButton type="link" @click="loadAllMcpServers">刷新</AButton>
              </div>
            </ACollapsePanel>
          </ACollapse>
        </PanelSection>

        <PanelSection title="执行与输出">
          <div class="config-row">
            <span class="config-row-label">最大迭代次数</span>
            <AInputNumber
              :value="Number(config.maxIterations ?? 5)"
              :min="1"
              style="width: 130px"
              @update:value="(value: any) => updateConfig('maxIterations', value)"
            />
          </div>
          <div class="switch-row">
            <span class="switch-label">结构化输出</span>
            <ASwitch
              :checked="Boolean(config.structuredOutputEnabled)"
              @update:checked="(value: any) => handleStructuredOutputToggle(Boolean(value))"
            />
          </div>
          <div v-if="config.structuredOutputEnabled" class="structured-editor">
            <ConfigCodeEditor
              :model-value="structuredOutputText"
              language="json"
              placeholder="{ &quot;type&quot;: &quot;object&quot;, &quot;properties&quot;: {} }"
              height="220px"
              :maximize-target="panelRoot"
              @update:model-value="handleStructuredOutputChange"
              @maximize-change="onEditorMaximizeChange"
            />
            <AAlert
              v-if="structuredOutputError"
              class="json-error"
              type="warning"
              show-icon
              :message="structuredOutputError"
            />
          </div>
        </PanelSection>

        <PanelSection title="输出说明">
          <OutputDisplay :outputs="node.data.outputConfigs || []" />
        </PanelSection>
      </AForm>
    </ASpin>
  </div>
</template>

<style scoped lang="scss">
.agent-node-panel {
  position: relative;

  &.editor-maximized {
    height: 100%;
    overflow: hidden;
  }
}

.provider-tabs {
  max-width: 100%;
  margin-bottom: 12px;
  background: #f2f4f7;
}

.model-radio-group {
  width: 100%;
}

.model-grid,
.checkbox-grid,
.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.model-radio,
.checkbox-item,
.tool-item {
  width: 100%;
  min-width: 0;
  margin: 0 !important;
  padding: 10px;
  border: 1px solid var(--color-border-base);
  border-radius: 8px;
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.model-info,
.item-info {
  min-width: 0;
}

.model-name,
.item-name {
  min-width: 0;
  overflow: hidden;
  color: #262626;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-desc,
.item-desc {
  min-width: 0;
  overflow: hidden;
  margin-top: 4px;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-row,
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 32px;
  margin-bottom: 12px;
}

.config-row-label,
.switch-label {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.88);
  font-size: 14px;
}

.formatter-selector {
  display: flex;
  align-items: center;
  gap: 6px;
}

.params-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  padding: 12px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;

  :deep(.ant-input-number),
  :deep(.ant-input) {
    width: 100%;
  }

  :deep(.ant-form-item) {
    margin-bottom: 0;
  }
}

.extend-config {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
}

.ability-collapse {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fafafa;

  :deep(.ant-collapse-header) {
    padding: 10px 14px !important;
    font-size: 13px;
    font-weight: 600;
  }

  :deep(.ant-collapse-content-box) {
    padding: 0 14px 12px !important;
  }
}

.mcp-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mcp-item {
  padding: 12px;
  border: 1px solid var(--color-border-base);
  border-radius: 8px;
  transition: all var(--transition-base);

  &.selected {
    border-color: rgba(15, 116, 255, 0.4);
    background: rgba(15, 116, 255, 0.03);
  }

  &.unavailable {
    background: rgba(15, 23, 42, 0.02);
  }
}

.mcp-item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.mcp-tag-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.mcp-binding-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed rgba(15, 23, 42, 0.08);
}

.binding-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.binding-label {
  font-weight: 600;
}

.binding-tip {
  color: var(--color-text-secondary);
  font-size: 12px;

  &.warning {
    color: #d48806;
  }
}

.tool-load-action {
  padding-left: 0;
}

.structured-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.json-error {
  border-radius: 6px;
}

.empty-action {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  color: #8c8c8c;
}

@media (max-width: 720px) {
  .params-grid,
  .model-grid,
  .checkbox-grid,
  .tool-grid {
    grid-template-columns: 1fr;
  }

  .mcp-item-header {
    flex-direction: column;
  }

  .mcp-tag-group {
    justify-content: flex-start;
  }
}
</style>
