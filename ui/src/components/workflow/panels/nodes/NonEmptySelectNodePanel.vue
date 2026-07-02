<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import BlurInput from '../shared/BlurInput.vue'
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
    <PanelSection title="节点名称">
      <NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(v: any) => updateNode({ label: v })"
      />
    </PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :current-node-id="node.id"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <div class="config-desc">从多个候选输入中选择第一个或最后一个非空值。</div>
      <AFormItem label="选择策略">
        <ASegmented
          :value="node.data.config?.strategy || 'FIRST'"
          :options="[
            { label: '第一个', value: 'FIRST' },
            { label: '最后一个', value: 'LAST' },
          ]"
          @update:value="(v: any) => updateConfig('strategy', v)"
        />
      </AFormItem>
      <AFormItem label="默认节点ID">
        <BlurInput
          :model-value="String(node.data.config?.defaultNextNodeId || '')"
          @update:model-value="(v: any) => updateConfig('defaultNextNodeId', v)"
        />
      </AFormItem>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.config-desc {
  margin-bottom: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f6f8fa;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.6;
}
</style>
