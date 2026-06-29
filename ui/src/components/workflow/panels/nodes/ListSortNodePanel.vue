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
    <PanelSection title="节点名称"
      ><NodeNameInput
        :model-value="node.data.label"
        @update:model-value="(v: any) => updateNode({ label: v })"
    /></PanelSection>
    <InputBindingSection
      :model-value="node.data.inputConfigs"
      :nodes="nodes"
      :current-node-id="node.id"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <AFormItem label="表达式引擎"
        ><ASelect
          :value="node.data.config?.evaluatorType || 'GROOVY'"
          :options="[{ label: 'Groovy', value: 'GROOVY' }]"
          @update:value="(v: any) => updateConfig('evaluatorType', v)"
      /></AFormItem>
      <AFormItem label="排序表达式" required
        ><SmartCodeEditor
          :model-value="String(node.data.config?.condition || '')"
          language="txt"
          theme="light"
          height="120px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="false"
          placeholder="提取排序字段的表达式"
          @update:model-value="(v: any) => updateConfig('condition', v)"
      /></AFormItem>
      <AFormItem label="排序方向"
        ><ASegmented
          :value="node.data.config?.direction || 'ASC'"
          :options="[
            { label: '升序', value: 'ASC' },
            { label: '降序', value: 'DESC' },
          ]"
          @update:value="(v: any) => updateConfig('direction', v)"
      /></AFormItem>
      <AFormItem label="空值靠前"
        ><ASwitch
          :checked="Boolean(node.data.config?.nullFirst)"
          @update:checked="(v: any) => updateConfig('nullFirst', v)"
      /></AFormItem>
      <AFormItem label="严格模式"
        ><ASwitch
          :checked="Boolean(node.data.config?.strictMode)"
          @update:checked="(v: any) => updateConfig('strictMode', v)"
      /></AFormItem>
    </PanelSection>
    <PanelSection title="输出说明"
      ><OutputDisplay :outputs="node.data.outputConfigs || []"
    /></PanelSection>
  </AForm>
</template>
