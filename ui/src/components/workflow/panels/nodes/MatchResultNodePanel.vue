<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import BlurInput from '../shared/BlurInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import WorkflowArrayEditors from '@/components/workflow/fields/WorkflowArrayEditors.vue'
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
</script>

<template>
  <AForm layout="vertical">
    <PanelSection title="节点名称">
      <NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(v: any) => updateNode({ label: v })"
      />
    </PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :edges="edges"
      :current-node-id="node.id"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <AFormItem label="匹配方式">
        <ASegmented
          :value="node.data.config?.matchType || 'EQUALS'"
          :options="[
            { label: '等于', value: 'EQUALS' },
            { label: '包含', value: 'CONTAINS' },
          ]"
          @update:value="(v: any) => updateConfig('matchType', v)"
        />
      </AFormItem>
      <AFormItem label="区分大小写">
        <ASwitch
          :checked="Boolean(node.data.config?.caseSensitive ?? true)"
          @update:checked="(v: any) => updateConfig('caseSensitive', v)"
        />
      </AFormItem>
      <AFormItem label="匹配项">
        <WorkflowArrayEditors
          :model-value="node.data.config?.matches"
          type="matchList"
          @update:model-value="(v: any) => updateConfig('matches', v)"
        />
      </AFormItem>
      <AFormItem label="默认节点ID">
        <BlurInput
          :model-value="String(node.data.config?.defaultNextNodeId || '')"
          placeholder="无匹配时执行的节点"
          @update:model-value="(v: any) => updateConfig('defaultNextNodeId', v)"
        />
      </AFormItem>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>
