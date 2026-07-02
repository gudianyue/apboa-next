<script setup lang="ts">
import { computed } from 'vue'
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import BlurInput from '../shared/BlurInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import type { WorkflowFlowEdge, WorkflowFlowNode, WorkflowResourceMaps } from '@/types/workflow'

const props = defineProps<{
  node: WorkflowFlowNode
  nodes: WorkflowFlowNode[]
  edges: WorkflowFlowEdge[]
  resources: WorkflowResourceMaps
}>()
const emit = defineEmits<{ update: [node: WorkflowFlowNode] }>()

function updateNode(patch: Partial<WorkflowFlowNode['data']>) {
  emit('update', { ...props.node, data: { ...props.node.data, ...patch } })
}
function updateConfig(key: string, value: unknown) {
  updateNode({ config: { ...(props.node.data.config || {}), [key]: value } })
}

const isString = computed(() => (props.node.data.config?.strategy as string) === 'STRING')
</script>

<template>
  <AForm layout="vertical">
    <PanelSection title="节点名称"
      ><NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(v: any) => updateNode({ label: v })"
    /></PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :edges="edges"
      :current-node-id="node.id"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <AFormItem label="聚合策略"
        ><ASegmented
          :value="node.data.config?.strategy || 'MAP'"
          :options="[
            { label: '数组', value: 'ARRAY' },
            { label: 'Map', value: 'MAP' },
            { label: '字符串', value: 'STRING' },
          ]"
          @update:value="(v: any) => updateConfig('strategy', v)"
      /></AFormItem>
      <AFormItem label="排除空值"
        ><ASwitch
          :checked="Boolean(node.data.config?.excludeNull)"
          @update:checked="(v: any) => updateConfig('excludeNull', v)"
      /></AFormItem>
      <AFormItem v-if="isString" label="字符串拼接符"
        ><BlurInput
          :model-value="String(node.data.config?.splicingSymbol || '')"
          @update:model-value="(v: any) => updateConfig('splicingSymbol', v)"
      /></AFormItem>
    </PanelSection>
    <PanelSection title="输出说明"
      ><OutputDisplay :outputs="node.data.outputConfigs || []"
    /></PanelSection>
  </AForm>
</template>
