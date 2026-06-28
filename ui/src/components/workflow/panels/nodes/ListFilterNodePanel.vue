<script setup lang="ts">
import { computed } from 'vue'
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import WorkflowCompareToEditor from '@/components/workflow/fields/WorkflowCompareToEditor.vue'
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

const isSimple = computed(() => (props.node.data.config?.mode as string) !== 'EXPRESSION')
const simpleSymbolOptions = [
  { label: '等于', value: 'EQ' },
  { label: '不等于', value: 'NE' },
  { label: '大于', value: 'GT' },
  { label: '小于', value: 'LT' },
  { label: '大于等于', value: 'GE' },
  { label: '小于等于', value: 'LE' },
  { label: '包含', value: 'CONTAINS' },
  { label: '不包含', value: 'NOT_CONTAINS' },
  { label: '开头匹配', value: 'STARTS_WITH' },
  { label: '结尾匹配', value: 'ENDS_WITH' },
  { label: '严格等于', value: 'EQUALS' },
  { label: '严格不等于', value: 'NOT_EQUALS' },
  { label: '为 true', value: 'IS_TRUE' },
  { label: '为 false', value: 'IS_FALSE' },
]
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
      <AFormItem label="过滤模式">
        <ASegmented
          :value="node.data.config?.mode || 'SIMPLE'"
          :options="[
            { label: '简单', value: 'SIMPLE' },
            { label: '表达式', value: 'EXPRESSION' },
          ]"
          @update:value="(v) => updateConfig('mode', v)"
        />
      </AFormItem>
      <template v-if="isSimple">
        <AFormItem label="简单类型">
          <ASelect
            :value="node.data.config?.supportType"
            :options="[
              { label: '字符串', value: 'STRING' },
              { label: '数字', value: 'NUMBER' },
              { label: '布尔', value: 'BOOLEAN' },
            ]"
            @update:value="(v) => updateConfig('supportType', v)"
          />
        </AFormItem>
        <AFormItem label="元素为空时保留">
          <ASwitch
            :checked="Boolean(node.data.config?.itemIsNullUse)"
            @update:checked="(v) => updateConfig('itemIsNullUse', v)"
          />
        </AFormItem>
        <AFormItem label="简单运算符">
          <ASelect
            show-search
            :value="node.data.config?.simpleSymbol"
            :options="simpleSymbolOptions"
            @update:value="(v) => updateConfig('simpleSymbol', v)"
          />
        </AFormItem>
        <AFormItem label="过滤条件" required>
          <AInput
            :value="String(node.data.config?.condition || '')"
            placeholder="item 或 item.子元素"
            @update:value="(v) => updateConfig('condition', v)"
          />
        </AFormItem>
        <AFormItem label="比较值">
          <WorkflowCompareToEditor
            :model-value="node.data.config?.compareTo"
            :nodes="nodes"
            :current-node-id="node.id"
            @update:model-value="(v) => updateConfig('compareTo', v)"
          />
        </AFormItem>
      </template>
      <template v-else>
        <AFormItem label="表达式引擎">
          <ASelect
            :value="node.data.config?.evaluatorType || 'GROOVY'"
            :options="[{ label: 'Groovy', value: 'GROOVY' }]"
            @update:value="(v) => updateConfig('evaluatorType', v)"
          />
        </AFormItem>
        <AFormItem label="过滤条件" required>
          <SmartCodeEditor
            :model-value="String(node.data.config?.condition || '')"
            language="txt"
            theme="light"
            height="160px"
            :show-change-language="false"
            :show-theme-toggle="false"
            :show-fullscreen="true"
            placeholder="变量名仅可为 item"
            @update:model-value="(v) => updateConfig('condition', v)"
          />
        </AFormItem>
      </template>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>
