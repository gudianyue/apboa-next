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
        @update:model-value="(v) => updateNode({ label: v })"
      />
    </PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :current-node-id="node.id"
      @update:model-value="(v) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <AFormItem label="循环变量名">
        <AInput
          :value="String(node.data.config?.loopVariable ?? 'loopIndex')"
          @update:value="(v) => updateConfig('loopVariable', v)"
        />
      </AFormItem>
      <AFormItem label="最大循环次数">
        <AInputNumber
          class="full-input"
          :value="Number(node.data.config?.maxIterations ?? 1000)"
          :min="1"
          @update:value="(v) => updateConfig('maxIterations', v)"
        />
      </AFormItem>
      <AFormItem label="终止表达式">
        <SmartCodeEditor
          :model-value="String(node.data.config?.terminationExpression || '')"
          language="txt"
          theme="light"
          height="100px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="false"
          placeholder='如 "item == null"'
          @update:model-value="(v) => updateConfig('terminationExpression', v)"
        />
      </AFormItem>
      <AFormItem label="迭代数据源变量">
        <AInput
          :value="String(node.data.config?.iterateDataSource || '')"
          placeholder="留空则使用纯计数循环"
          @update:value="(v) => updateConfig('iterateDataSource', v)"
        />
      </AFormItem>
      <AFormItem label="元素变量名">
        <AInput
          :value="String(node.data.config?.itemVariable ?? 'item')"
          @update:value="(v) => updateConfig('itemVariable', v)"
        />
      </AFormItem>
      <AFormItem label="子流程入口节点ID">
        <AInput
          :value="String(node.data.config?.entryNodeId || '')"
          @update:value="(v) => updateConfig('entryNodeId', v)"
        />
      </AFormItem>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.full-input {
  width: 100%;
}
</style>
