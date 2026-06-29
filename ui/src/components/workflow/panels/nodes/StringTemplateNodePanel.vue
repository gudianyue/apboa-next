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
      <AFormItem label="模板格式"
        ><ASegmented
          :value="node.data.config?.templateType || 'STRING'"
          :options="[
            { label: '字符串', value: 'STRING' },
            { label: 'Velocity', value: 'VELOCITY' },
          ]"
          @update:value="(v: any) => updateConfig('templateType', v)"
      /></AFormItem>
      <AFormItem label="模板内容" required
        ><SmartCodeEditor
          :model-value="String(node.data.config?.template || '')"
          language="txt"
          theme="light"
          height="200px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder="输入模板内容，Velocity 模式使用 ${变量} 语法"
          @update:model-value="(v: any) => updateConfig('template', v)"
      /></AFormItem>
    </PanelSection>
    <PanelSection title="输出说明"
      ><OutputDisplay :outputs="node.data.outputConfigs || []"
    /></PanelSection>
  </AForm>
</template>
