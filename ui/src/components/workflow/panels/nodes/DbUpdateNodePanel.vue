<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import WorkflowResourceSelect from '@/components/workflow/fields/WorkflowResourceSelect.vue'
import WorkflowArrayEditors from '@/components/workflow/fields/WorkflowArrayEditors.vue'
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

const dbParamTypeOptions = [
  'STRING',
  'INTEGER',
  'INT',
  'LONG',
  'DOUBLE',
  'FLOAT',
  'BOOLEAN',
  'BOOL',
].map((v: any) => ({ label: v, value: v }))
const formatterOptions = [
  { label: '普通字符串', value: 'STRING' },
  { label: 'Jackson JSON', value: 'JACKSON' },
  { label: 'Velocity 模板', value: 'VELOCITY' },
]
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
      <AFormItem label="数据源" required
        ><WorkflowResourceSelect
          :model-value="String(node.data.config?.datasourceId || '')"
          resource-type="datasource"
          :resources="resources"
          @update:model-value="(v: any) => updateConfig('datasourceId', v)"
      /></AFormItem>
      <AFormItem label="SQL 语句" required
        ><SmartCodeEditor
          :model-value="String(node.data.config?.sql || '')"
          language="txt"
          theme="light"
          height="180px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder="使用 ? 作为参数占位符"
          @update:model-value="(v: any) => updateConfig('sql', v)"
        /><template #extra
          ><span class="field-help">SQL 本体不会整体模板替换，参数值支持模板替换。</span></template
        ></AFormItem
      >
      <AFormItem label="SQL 参数"
        ><WorkflowArrayEditors
          :model-value="node.data.config?.params"
          type="dbParams"
          :options="dbParamTypeOptions"
          @update:model-value="(v: any) => updateConfig('params', v)"
      /></AFormItem>
      <AFormItem label="参数模板格式"
        ><ASelect
          :value="node.data.config?.formatterType || 'VELOCITY'"
          :options="formatterOptions"
          @update:value="(v: any) => updateConfig('formatterType', v)"
      /></AFormItem>
    </PanelSection>
    <PanelSection title="输出说明"
      ><OutputDisplay :outputs="node.data.outputConfigs || []"
    /></PanelSection>
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
