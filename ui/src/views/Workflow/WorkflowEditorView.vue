<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import WorkflowCanvasViewport from '@/components/workflow/editor/WorkflowCanvasViewport.vue'
import WorkflowCanvasToolbar from '@/components/workflow/editor/WorkflowCanvasToolbar.vue'
import WorkflowTopActions from '@/components/workflow/editor/WorkflowTopActions.vue'
import WorkflowTopLeft from '@/components/workflow/editor/WorkflowTopLeft.vue'
import NodeLibraryPopover from '@/components/workflow/library/NodeLibraryPopover.vue'
import WorkflowConfigPanel from '@/components/workflow/panels/WorkflowConfigPanel.vue'
import WorkflowRunDock from '@/components/workflow/panels/WorkflowRunDock.vue'
import WorkflowValidationPanel from '@/components/workflow/panels/WorkflowValidationPanel.vue'
import WorkflowPublishModal from '@/components/workflow/version/WorkflowPublishModal.vue'
import WorkflowVersionModal from '@/components/workflow/version/WorkflowVersionModal.vue'
import WorkflowNodeContextMenu from '@/components/workflow/context-menu/WorkflowNodeContextMenu.vue'
import { useWorkflowStore } from '@/stores'
import * as workflowApi from '@/api/workflow'
import {
  cloneDefaultConfig,
  cloneDefaultInputs,
  cloneDefaultOutputs,
  getWorkflowNodeSchema,
} from '@/config/workflow/nodeSchemas'
import type {
  Workflow,
  WorkflowDefinition,
  WorkflowFlowEdge,
  WorkflowFlowNode,
  WorkflowNodeDefinition,
  WorkflowNodeSchema,
  WorkflowResourceMaps,
  WorkflowRunRequest,
  WorkflowValidationResult,
} from '@/types/workflow'

type CanvasRef = InstanceType<typeof WorkflowCanvasViewport>

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()

const workflow = ref<Workflow>({})
const nodes = ref<WorkflowFlowNode[]>([])
const edges = ref<WorkflowFlowEdge[]>([])
const selectedNodeId = ref<string | null>(null)
const saving = ref(false)
const running = ref(false)
const locked = ref(false)
const libraryOpen = ref(false)
const libraryAnchorX = ref<number | undefined>(undefined)
const libraryAnchorY = ref<number | undefined>(undefined)
const pendingSourceNodeId = ref<string | null>(null)
const pendingSourceHandle = ref<string>('output')
const runDockOpen = ref(false)
const versionModalOpen = ref(false)
const validationPanelOpen = ref(false)
const validationResult = ref<WorkflowValidationResult | null>(null)
const publishModalOpen = ref(false)
const publishing = ref(false)
const runInput = ref('{\n  "params": [],\n  "variables": {}\n}')
const canvasRef = ref<CanvasRef | null>(null)
const resources = ref<WorkflowResourceMaps>({ caches: [], datasources: [], mqs: [] })
const history = ref<WorkflowDefinition[]>([])
const future = ref<WorkflowDefinition[]>([])
const contextMenu = ref({ open: false, nodeId: '', x: 0, y: 0 })

const workflowId = computed(() => String(route.params.id || ''))
const selectedNode = computed(() => nodes.value.find((item) => item.id === selectedNodeId.value) || null)
const canUndo = computed(() => history.value.length > 0)
const canRedo = computed(() => future.value.length > 0)
const nodeNames = computed(() =>
  nodes.value.reduce<Record<string, string>>((acc, node) => {
    acc[node.id] = node.data.label || node.id
    return acc
  }, {}),
)

onMounted(async () => {
  await loadResources()
  if (workflowId.value) {
    await loadWorkflow(workflowId.value)
  } else {
    workflow.value = {
      name: '未命名工作流',
      status: 'DRAFT',
      version: '0',
      locked: 0,
      enabled: true,
      config: defaultDefinition(),
    }
    loadDefinition(workflow.value.config || defaultDefinition())
  }
})

async function loadResources() {
  const [caches, datasources, mqs] = await Promise.all([
    workflowApi.enabledCaches(),
    workflowApi.enabledDatasources(),
    workflowApi.enabledMqs(),
  ])
  resources.value = {
    caches: caches.data.data || [],
    datasources: datasources.data.data || [],
    mqs: mqs.data.data || [],
  }
}

async function loadWorkflow(id: string) {
  const response = await workflowApi.workflowDetail(id)
  workflow.value = response.data.data.workflow || {}
  locked.value = Boolean(workflow.value.locked)
  loadDefinition(workflow.value.config || defaultDefinition())
  await nextTick()
  canvasRef.value?.fitAll()
}

async function refreshWorkflowDetail(reloadCanvas = false) {
  if (!workflow.value.id) return
  const response = await workflowApi.workflowDetail(workflow.value.id)
  workflow.value = response.data.data.workflow || workflow.value
  locked.value = Boolean(workflow.value.locked)
  if (reloadCanvas) {
    loadDefinition(workflow.value.config || defaultDefinition())
    await nextTick()
    canvasRef.value?.fitAll()
  }
}

function loadDefinition(definition: WorkflowDefinition) {
  nodes.value = (definition.nodes || []).map(toFlowNode)
  edges.value = (definition.edges || []).map((edge) => ({ ...edge, type: 'default' }))
  history.value = []
  future.value = []
}

function restoreDefinition(definition: WorkflowDefinition) {
  nodes.value = (definition.nodes || []).map(toFlowNode)
  edges.value = (definition.edges || []).map((edge) => ({ ...edge, type: 'default' }))
}

function toFlowNode(node: WorkflowNodeDefinition): WorkflowFlowNode {
  const schema = getWorkflowNodeSchema(node.type)
  return {
    id: node.id,
    type: 'workflow',
    position: node.position,
    data: {
      type: node.type,
      label: node.name || schema?.title || node.type,
      description: schema?.description,
      status: 'IDLE',
      config: { ...(schema?.defaultConfig || {}), ...(node.config || {}) },
      inputConfigs: node.inputConfigs || schema?.inputConfigs || [],
      outputConfigs: node.outputConfigs || cloneDefaultOutputs(schema || getWorkflowNodeSchema('END')!, node.id),
      schema,
      resources: resources.value,
    },
  }
}

function toDefinition(): WorkflowDefinition {
  return {
    nodes: nodes.value.map((node) => ({
      id: node.id,
      type: node.data.type,
      name: node.data.label,
      position: node.position,
      config: node.data.config || {},
      inputConfigs: node.data.inputConfigs || [],
      outputConfigs: node.data.outputConfigs || [{ name: 'output', fromNodeId: node.id }],
      ui: {},
    })),
    edges: edges.value.map((edge) => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle || 'output',
      targetHandle: edge.targetHandle || 'input',
      label: String(edge.label || ''),
    })),
    viewport: { x: 0, y: 0, zoom: 1 },
    metadata: {
      schemaVersion: '1.0',
      nodeVersion: '1.0',
      updatedAt: new Date().toISOString(),
    },
  }
}

function defaultDefinition(): WorkflowDefinition {
  const start = createDefinitionNode('start', getWorkflowNodeSchema('START')!, { x: 120, y: 180 })
  const end = createDefinitionNode('end', getWorkflowNodeSchema('END')!, { x: 520, y: 180 })
  end.inputConfigs = [{ name: 'input', sourceType: 'NODE_OUTPUT', nodeId: 'start', outputName: 'output' }]
  return {
    nodes: [start, end],
    edges: [{ id: 'edge-start-end', source: 'start', target: 'end', sourceHandle: 'output', targetHandle: 'input', label: '' }],
    viewport: { x: 0, y: 0, zoom: 1 },
    metadata: { schemaVersion: '1.0', nodeVersion: '1.0', updatedAt: new Date().toISOString() },
  }
}

function createDefinitionNode(id: string, schema: WorkflowNodeSchema, position: { x: number; y: number }): WorkflowNodeDefinition {
  return {
    id,
    type: schema.type,
    name: schema.title,
    position,
    config: cloneDefaultConfig(schema),
    inputConfigs: cloneDefaultInputs(schema),
    outputConfigs: cloneDefaultOutputs(schema, id),
    ui: {},
  }
}

function snapshot() {
  history.value.push(toDefinition())
  if (history.value.length > 50) history.value.shift()
  future.value = []
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value))
}

function addNode(schema: WorkflowNodeSchema) {
  if (locked.value) {
    message.warning('画布已锁定，无法添加节点')
    return
  }
  snapshot()
  const id = `${schema.type.toLowerCase()}-${Date.now()}`
  const sourceId = pendingSourceNodeId.value
  const sourceHandle = pendingSourceHandle.value || 'output'
  const sourceNode = sourceId ? nodes.value.find((n) => n.id === sourceId) : null
  const position = sourceNode
    ? { x: sourceNode.position.x + 320, y: sourceNode.position.y }
    : canvasRef.value?.addAtCenter() || { x: 240, y: 240 }
  nodes.value.push(toFlowNode(createDefinitionNode(id, schema, position)))
  if (sourceId) {
    edges.value.push({
      id: `edge-${sourceId}-${id}-${Date.now()}`,
      source: sourceId,
      target: id,
      sourceHandle,
      targetHandle: 'input',
      type: 'default',
    })
    pendingSourceNodeId.value = null
    pendingSourceHandle.value = 'output'
    libraryAnchorX.value = undefined
    libraryAnchorY.value = undefined
  }
  selectedNodeId.value = id
}

function updateNode(node: WorkflowFlowNode) {
  snapshot()
  nodes.value = nodes.value.map((item) => (item.id === node.id ? { ...node, data: { ...node.data, resources: resources.value } } : item))
}

function deleteNode(nodeId: string) {
  if (locked.value) {
    message.warning('画布已锁定，无法删除节点')
    return
  }
  snapshot()
  nodes.value = nodes.value.filter((node) => node.id !== nodeId)
  edges.value = edges.value.filter((edge) => edge.source !== nodeId && edge.target !== nodeId)
  if (selectedNodeId.value === nodeId) selectedNodeId.value = null
  closeContextMenu()
}

function copyNode(nodeId: string) {
  const source = nodes.value.find((node) => node.id === nodeId)
  if (!source || locked.value) return
  snapshot()
  const id = `${source.data.type.toLowerCase()}-${Date.now()}`
  nodes.value.push({
    ...source,
    id,
    position: { x: source.position.x + 40, y: source.position.y + 40 },
    data: {
      ...source.data,
      config: clone(source.data.config),
      inputConfigs: clone(source.data.inputConfigs || []),
      label: `${source.data.label} 副本`,
      outputConfigs: cloneDefaultOutputs(source.data.schema!, id),
      resources: resources.value,
    },
  })
  selectedNodeId.value = id
  closeContextMenu()
}

function autoLayout() {
  if (!nodes.value.length || locked.value) return
  snapshot()
  const indegree = new Map(nodes.value.map((node) => [node.id, 0]))
  edges.value.forEach((edge) => indegree.set(edge.target, (indegree.get(edge.target) || 0) + 1))
  const levels = new Map<string, number>()
  const queue = nodes.value.filter((node) => (indegree.get(node.id) || 0) === 0).map((node) => node.id)
  queue.forEach((id) => levels.set(id, 0))
  while (queue.length) {
    const id = queue.shift()!
    const level = levels.get(id) || 0
    edges.value.filter((edge) => edge.source === id).forEach((edge) => {
      const nextLevel = Math.max(levels.get(edge.target) || 0, level + 1)
      levels.set(edge.target, nextLevel)
      indegree.set(edge.target, (indegree.get(edge.target) || 0) - 1)
      if ((indegree.get(edge.target) || 0) === 0) queue.push(edge.target)
    })
  }
  const buckets = new Map<number, WorkflowFlowNode[]>()
  nodes.value.forEach((node) => {
    const level = levels.get(node.id) || 0
    buckets.set(level, [...(buckets.get(level) || []), node])
  })
  nodes.value = nodes.value.map((node) => {
    const level = levels.get(node.id) || 0
    const bucket = buckets.get(level) || []
    const index = bucket.findIndex((item) => item.id === node.id)
    return { ...node, position: { x: 120 + level * 320, y: 120 + index * 180 } }
  })
  nextTick(() => canvasRef.value?.fitAll())
}

function undo() {
  const previous = history.value.pop()
  if (!previous) return
  future.value.push(toDefinition())
  restoreDefinition(previous)
}

function redo() {
  const next = future.value.pop()
  if (!next) return
  history.value.push(toDefinition())
  restoreDefinition(next)
}

async function saveWorkflow() {
  if (!workflow.value.id && !workflowId.value) {
    const response = await workflowApi.workflowSave({
      ...workflow.value,
      config: toDefinition(),
      status: workflow.value.status || 'DRAFT',
      version: workflow.value.version || '0',
    })
    workflow.value = response.data.data
    store.upsertWorkflow(workflow.value)
    store.markListDirty()
    message.success('工作流已创建')
    if (workflow.value.id) await router.replace(`/workflow/${workflow.value.id}/edit`)
    return
  }
  if (!workflow.value.id) return
  saving.value = true
  try {
    workflow.value.config = toDefinition()
    await workflowApi.workflowUpdate(workflow.value)
    await refreshWorkflowDetail(false)
    store.upsertWorkflow(workflow.value)
    store.markListDirty()
    message.success('已保存')
  } finally {
    saving.value = false
  }
}

async function validateWorkflow() {
  await saveWorkflow()
  if (!workflow.value.id) return false
  const result = await store.validate(workflow.value.id)
  validationResult.value = result
  validationPanelOpen.value = true
  runDockOpen.value = false
  selectedNodeId.value = null
  markValidation(result.valid, result.errors)
  if (result.valid) {
    message.success(result.warnings?.length ? '校验通过，请关注提醒项' : '校验通过')
  } else {
    message.error('校验失败，请查看校验结果')
  }
  return result.valid
}

async function publishWorkflow() {
  const valid = await validateWorkflow()
  if (!valid || !workflow.value.id) return
  publishModalOpen.value = true
}

async function submitPublish(remark?: string) {
  if (!workflow.value.id) return
  publishing.value = true
  try {
    const response = await workflowApi.workflowPublish(workflow.value.id, remark)
    workflow.value.status = 'PUBLISHED'
    workflow.value.version = response.data.data.version
    await refreshWorkflowDetail(false)
    store.upsertWorkflow(workflow.value)
    store.markListDirty()
    publishModalOpen.value = false
    message.success('发布成功')
  } finally {
    publishing.value = false
  }
}

function openDebugPanel() {
  runDockOpen.value = true
}

async function debugRun() {
  if (!workflow.value.id) {
    await saveWorkflow()
  } else {
    await saveWorkflow()
  }
  if (!workflow.value.id) return
  running.value = true
  try {
    let payload: WorkflowRunRequest
    try {
      payload = JSON.parse(runInput.value || '{}') as WorkflowRunRequest
    } catch {
      message.error('调试输入必须是合法 JSON')
      return
    }
    const inputError = validateDebugPayload(payload)
    if (inputError) {
      message.error(inputError)
      return
    }
    const result = await store.debugRun(workflow.value.id, payload)
    const statusByNode = new Map(result.nodeExecutions.map((item) => [item.nodeId, item.status]))
    nodes.value = nodes.value.map((node) => ({
      ...node,
      data: { ...node.data, status: statusByNode.get(node.id) || node.data.status },
    }))
    message.success(result.run.status === 'SUCCESS' ? '调试运行成功' : '调试运行结束，请查看结果')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '调试运行失败')
  } finally {
    running.value = false
  }
}

function validateDebugPayload(payload: WorkflowRunRequest) {
  const params = Array.isArray(payload.params) ? payload.params : []
  const values = new Map(params.map((item) => [item.name, item.value]))
  const start = nodes.value.find((node) => node.data.type === 'START')
  const startParams = Array.isArray(start?.data.config?.params) ? start.data.config.params as Array<Record<string, unknown>> : []
  for (const param of startParams) {
    const name = String(param.name || '')
    if (!name) continue
    const value = values.get(name)
    const type = String(param.type || 'String')
    if (param.required && (value === undefined || value === null || String(value).trim() === '')) {
      return `${name} 为必填参数`
    }
    if (value === undefined || value === null || String(value).trim() === '') continue
    if (type === 'Object' || type === 'Array') {
      try {
        const parsed = typeof value === 'string' ? JSON.parse(value) : value
        if (type === 'Array' && !Array.isArray(parsed)) return `${name} 必须是 JSON 数组`
        if (type === 'Object' && (Array.isArray(parsed) || typeof parsed !== 'object' || parsed === null)) return `${name} 必须是 JSON 对象`
      } catch {
        return `${name} 不是合法 JSON`
      }
    }
  }
  if (payload.variables && (Array.isArray(payload.variables) || typeof payload.variables !== 'object')) {
    return 'variables 必须是 JSON 对象'
  }
  return ''
}

function markValidation(valid: boolean, errors: unknown[]) {
  const errorMap = new Map<string, string[]>()
  errors.forEach((item) => {
    if (typeof item === 'object' && item && 'nodeId' in item) {
      const nodeId = String((item as { nodeId?: string }).nodeId || '')
      const msg = String((item as { message?: string }).message || '配置错误')
      if (nodeId) errorMap.set(nodeId, [...(errorMap.get(nodeId) || []), msg])
    }
  })
  nodes.value = nodes.value.map((node) => ({
    ...node,
    data: {
      ...node.data,
      status: valid ? 'IDLE' : errorMap.has(node.id) ? 'INVALID' : node.data.status,
      errors: errorMap.get(node.id) || [],
    },
  }))
  const firstInvalid = nodes.value.find((node) => errorMap.has(node.id))
  if (firstInvalid) selectedNodeId.value = firstInvalid.id
}

function showVersions() {
  if (!workflow.value.id) return
  versionModalOpen.value = true
}

async function handleVersionLoaded(nextWorkflow: Workflow) {
  workflow.value = nextWorkflow
  locked.value = Boolean(workflow.value.locked)
  loadDefinition(workflow.value.config || defaultDefinition())
  await nextTick()
  canvasRef.value?.fitAll()
  store.upsertWorkflow(workflow.value)
  store.markListDirty()
}

function openContextMenu(payload: { nodeId: string; x: number; y: number }) {
  contextMenu.value = { open: true, ...payload }
}

function closeContextMenu() {
  contextMenu.value.open = false
}

function clearPendingAdd() {
  pendingSourceNodeId.value = null
  pendingSourceHandle.value = 'output'
  libraryAnchorX.value = undefined
  libraryAnchorY.value = undefined
}

function closeLibrary() {
  libraryOpen.value = false
  clearPendingAdd()
}

function toggleLibrary() {
  if (libraryOpen.value) {
    closeLibrary()
    return
  }
  clearPendingAdd()
  libraryOpen.value = true
}

function openLibraryFromNode(payload: { sourceNodeId: string; sourceHandle: string; x: number; y: number }) {
  pendingSourceNodeId.value = payload.sourceNodeId
  pendingSourceHandle.value = payload.sourceHandle || 'output'
  libraryAnchorX.value = payload.x
  libraryAnchorY.value = payload.y
  selectedNodeId.value = null
  libraryOpen.value = true
}

function focusNode(nodeId: string) {
  validationPanelOpen.value = false
  selectedNodeId.value = nodeId
  canvasRef.value?.fitNode(nodeId)
}
</script>

<template>
  <main class="workflow-editor-shell">
    <WorkflowCanvasViewport
      ref="canvasRef"
      v-model:nodes="nodes"
      v-model:edges="edges"
      :locked="locked"
      @select-node="selectedNodeId = $event"
      @node-context="openContextMenu"
      @pane-click="closeContextMenu"
      @show-library="openLibraryFromNode"
    />

    <WorkflowTopLeft
      :title="workflow.name || ''"
      :status="workflow.status"
      :version="workflow.version"
      :saving="saving"
      @back="router.push('/workflow')"
      @update-title="(value) => (workflow.name = value)"
    />

    <WorkflowTopActions
      :saving="saving"
      :running="running"
      @save="saveWorkflow"
      @validate="validateWorkflow"
      @publish="publishWorkflow"
      @debug="openDebugPanel"
      @versions="showVersions"
    />

    <WorkflowCanvasToolbar
      :locked="locked"
      :can-undo="canUndo"
      :can-redo="canRedo"
      :has-nodes="nodes.length > 0"
      :library-open="libraryOpen"
      @add-node="toggleLibrary"
      @fit="canvasRef?.fitAll()"
      @zoom-in="canvasRef?.zoomInCanvas()"
      @zoom-out="canvasRef?.zoomOutCanvas()"
      @reset-zoom="canvasRef?.resetZoom()"
      @toggle-lock="locked = !locked"
      @undo="undo"
      @redo="redo"
      @layout="autoLayout"
      @clear-selection="selectedNodeId = null"
    />

    <NodeLibraryPopover :open="libraryOpen" :anchor-x="libraryAnchorX" :anchor-y="libraryAnchorY" @close="closeLibrary" @add="addNode" />

    <div v-if="libraryOpen" class="popover-mask" @click="closeLibrary" />

    <WorkflowConfigPanel
      :node="selectedNode"
      :nodes="nodes"
      :resources="resources"
      @update="updateNode"
      @close="selectedNodeId = null"
    />

    <WorkflowValidationPanel
      :open="validationPanelOpen"
      :result="validationResult"
      :node-names="nodeNames"
      @close="validationPanelOpen = false"
      @focus-node="focusNode"
    />

    <WorkflowRunDock
      v-model:input-text="runInput"
      :open="runDockOpen"
      :result="store.lastRun"
      :nodes="nodes"
      :loading="running"
      @run="debugRun"
      @close="runDockOpen = false"
      @focus-node="focusNode"
    />

    <WorkflowVersionModal
      v-model:open="versionModalOpen"
      :workflow-id="workflow.id"
      :workflow-name="workflow.name"
      :current-version="workflow.version"
      :status="workflow.status"
      @loaded="handleVersionLoaded"
    />

    <WorkflowPublishModal
      v-model:open="publishModalOpen"
      :workflow-name="workflow.name"
      :loading="publishing"
      @publish="submitPublish"
    />

    <WorkflowNodeContextMenu
      :open="contextMenu.open"
      :x="contextMenu.x"
      :y="contextMenu.y"
      @edit="selectedNodeId = contextMenu.nodeId; closeContextMenu()"
      @rename="selectedNodeId = contextMenu.nodeId; closeContextMenu()"
      @copy="copyNode(contextMenu.nodeId)"
      @delete="deleteNode(contextMenu.nodeId)"
      @fit="focusNode(contextMenu.nodeId); closeContextMenu()"
      @logs="runDockOpen = true; closeContextMenu()"
    />
  </main>
</template>

<style scoped lang="scss">
.workflow-editor-shell {
  position: relative;
  width: 100%;
  height: calc(100vh - 55px);
  overflow: hidden;
  background: #f7f8fa;
}

.popover-mask {
  position: absolute;
  inset: 0;
  z-index: 1;
}
</style>
