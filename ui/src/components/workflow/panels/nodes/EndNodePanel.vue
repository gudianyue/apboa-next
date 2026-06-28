<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import type { WorkflowFlowNode, WorkflowResourceMaps } from '@/types/workflow'

const props = defineProps<{
  node: WorkflowFlowNode
  nodes: WorkflowFlowNode[]
  resources: WorkflowResourceMaps
}>()

const emit = defineEmits<{
  update: [node: WorkflowFlowNode]
}>()

function updateNode(patch: Partial<WorkflowFlowNode['data']>) {
  emit('update', { ...props.node, data: { ...props.node.data, ...patch } })
}

function updateConfig(key: string, value: unknown) {
  updateNode({ config: { ...(props.node.data.config || {}), [key]: value } })
}

const formatterOptions = [
  { label: '普通字符串', value: 'STRING' },
  { label: 'Jackson JSON', value: 'JACKSON' },
  { label: 'Velocity 模板', value: 'VELOCITY' },
]
</script>

<template>
  <AForm layout="vertical">
    <PanelSection title="节点名称">
      <NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(value: string) => updateNode({ label: value })"
      />
    </PanelSection>

    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :current-node-id="node.id"
      @update:model-value="(value) => updateNode({ inputConfigs: value })"
    />

    <PanelSection title="节点配置">
      <AFormItem label="响应模板" required>
        <SmartCodeEditor
          :model-value="String(node.data.config?.responseTemplate ?? '${input}')"
          language="json"
          theme="light"
          height="220px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder="如 ${input}，支持 Velocity 变量"
          @update:model-value="(value: string) => updateConfig('responseTemplate', value)"
        />
        <template #extra>
          <span class="field-help">使用 ${变量名} 引用上游节点输出。</span>
        </template>
      </AFormItem>

      <AFormItem label="响应模板格式">
        <ASelect
          :value="node.data.config?.formatterType || 'JACKSON'"
          :options="formatterOptions"
          @update:value="(value: string) => updateConfig('formatterType', value)"
        />
      </AFormItem>
    </PanelSection>

    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.field-help {
  display: block;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.5;
}
</style>
