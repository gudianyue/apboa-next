<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import type { WorkflowFlowNode, WorkflowResourceMaps } from '@/types/workflow'

const props = defineProps<{
  node: WorkflowFlowNode
  nodes: WorkflowFlowNode[]
  resources: WorkflowResourceMaps
}>()
const emit = defineEmits<{ update: [node: WorkflowFlowNode] }>()

function updateNode(patch: Partial<WorkflowFlowNode['data']>) {
  emit('update', { ...props.node, data: { ...props.node.data, ...patch } })
}
function updateConfig(key: string, value: unknown) {
  updateNode({ config: { ...(props.node.data.config || {}), [key]: value } })
}
</script>

<template>
  <AForm layout="vertical">
    <PanelSection title="节点名称"
      ><NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(v) => updateNode({ label: v })"
    /></PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :current-node-id="node.id"
      @update:model-value="(v) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <AFormItem label="序列化模式"
        ><ASegmented
          :value="node.data.config?.mode || 'COMPACT'"
          :options="[
            { label: '紧凑', value: 'COMPACT' },
            { label: '美化', value: 'PRETTY' },
          ]"
          @update:value="(v) => updateConfig('mode', v)"
      /></AFormItem>
      <AFormItem label="格式"
        ><ASelect
          :value="node.data.config?.format || 'JSON'"
          :options="
            ['JSON', 'XML', 'YAML', 'BASE64', 'URL_ENCODED'].map((v) => ({ label: v, value: v }))
          "
          @update:value="(v) => updateConfig('format', v)"
      /></AFormItem>
      <AFormItem label="排除 null"
        ><ASwitch
          :checked="Boolean(node.data.config?.excludeNulls)"
          @update:checked="(v) => updateConfig('excludeNulls', v)"
      /></AFormItem>
      <AFormItem label="排除空字符串"
        ><ASwitch
          :checked="Boolean(node.data.config?.excludeEmptyStrings)"
          @update:checked="(v) => updateConfig('excludeEmptyStrings', v)"
      /></AFormItem>
      <AFormItem label="编码"
        ><AInput
          :value="String(node.data.config?.encoding ?? 'UTF-8')"
          @update:value="(v) => updateConfig('encoding', v)"
      /></AFormItem>
    </PanelSection>
    <PanelSection title="输出说明"
      ><OutputDisplay :outputs="node.data.outputConfigs || []"
    /></PanelSection>
  </AForm>
</template>
