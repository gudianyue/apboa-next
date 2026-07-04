<script setup lang="ts">
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
      :max-bindings="1"
      :readonly-name="true"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <div class="config-row">
        <span class="config-row-label">格式</span>
        <ASelect
          :value="node.data.config?.format || 'JSON'"
          :options="
            ['JSON', 'XML', 'YAML', 'BASE64', 'URL_ENCODED'].map((v: any) => ({ label: v, value: v }))
          "
          style="width: 160px"
          @update:value="(v: any) => updateConfig('format', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">排除 null</span>
        <ASwitch
          :checked="Boolean(node.data.config?.excludeNulls)"
          @update:checked="(v: any) => updateConfig('excludeNulls', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">编码</span>
        <BlurInput
          :model-value="String(node.data.config?.encoding ?? 'UTF-8')"
          style="width: 160px"
          @update:model-value="(v: any) => updateConfig('encoding', v)"
        />
      </div>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.config-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.config-row-label {
  flex-shrink: 0;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.88);
}
</style>
