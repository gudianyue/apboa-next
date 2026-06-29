<script setup lang="ts">
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import WorkflowResourceSelect from '@/components/workflow/fields/WorkflowResourceSelect.vue'
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

function stringify(v: unknown) {
  return v === undefined || v === null ? '' : typeof v === 'string' ? v : JSON.stringify(v, null, 2)
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
      <AFormItem label="缓存实例" required>
        <WorkflowResourceSelect
          :model-value="String(node.data.config?.cacheId || '')"
          resource-type="cache"
          :resources="resources"
          @update:model-value="(v: any) => updateConfig('cacheId', v)"
        />
      </AFormItem>
      <AFormItem label="缓存键" required>
        <AInput
          :value="String(node.data.config?.key || '')"
          placeholder="例如 user:${userId}"
          @update:value="(v: any) => updateConfig('key', v)"
        />
        <template #extra><span class="field-help">支持 Velocity 变量语法。</span></template>
      </AFormItem>
      <AFormItem label="缓存值" required>
        <SmartCodeEditor
          :model-value="stringify(node.data.config?.value)"
          language="json"
          theme="light"
          height="180px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder="支持字符串、数字、对象或数组。"
          @update:model-value="
            (v: string) => {
              try {
                updateConfig('value', JSON.parse(v))
              } catch {
                updateConfig('value', v)
              }
            }
          "
        />
      </AFormItem>
      <AFormItem label="过期时间(秒)">
        <AInputNumber
          class="full-input"
          :value="Number(node.data.config?.expire ?? 0)"
          :min="0"
          placeholder="0 表示不过期"
          @update:value="(v: any) => updateConfig('expire', v ?? 0)"
        />
      </AFormItem>
      <AFormItem label="模板格式">
        <ASelect
          :value="node.data.config?.formatterType || 'VELOCITY'"
          :options="[
            { label: '普通字符串', value: 'STRING' },
            { label: 'Jackson JSON', value: 'JACKSON' },
            { label: 'Velocity 模板', value: 'VELOCITY' },
          ]"
          @update:value="(v: any) => updateConfig('formatterType', v)"
        />
      </AFormItem>
    </PanelSection>

    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.field-help {
  display: block;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.5;
}

.full-input {
  width: 100%;
}
</style>
