<script setup lang="ts">
import { computed } from 'vue'
import PanelSection from '../shared/PanelSection.vue'
import NodeNameInput from '../shared/NodeNameInput.vue'
import InputBindingSection from '../shared/InputBindingSection.vue'
import OutputDisplay from '../shared/OutputDisplay.vue'
import WorkflowArrayEditors from '@/components/workflow/fields/WorkflowArrayEditors.vue'
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

const mode = computed(() => props.node.data.config?.mode as string)
const isKeyValue = computed(() => mode.value === 'KEY_VALUE')
const isMultiDelimiter = computed(() => mode.value === 'MULTIPLE_DELIMITERS')
const showDelimiter = computed(
  () => mode.value === 'SIMPLE' || mode.value === 'REGEX' || mode.value === 'FIXED_LENGTH',
)
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
      <AFormItem label="分割模式">
        <ASelect
          :value="node.data.config?.mode"
          :options="[
            { label: '简单分隔符', value: 'SIMPLE' },
            { label: '正则', value: 'REGEX' },
            { label: '固定长度', value: 'FIXED_LENGTH' },
            { label: '换行', value: 'LINE_BREAK' },
            { label: '键值对', value: 'KEY_VALUE' },
            { label: '多个分隔符', value: 'MULTIPLE_DELIMITERS' },
          ]"
          @update:value="(v: any) => updateConfig('mode', v)"
        />
      </AFormItem>
      <AFormItem v-if="showDelimiter" label="分隔符/正则/长度">
        <AInput
          :value="String(node.data.config?.delimiter || '')"
          @update:value="(v: any) => updateConfig('delimiter', v)"
        />
      </AFormItem>
      <AFormItem v-if="isMultiDelimiter" label="多个分隔符">
        <WorkflowArrayEditors
          :model-value="node.data.config?.delimiters"
          type="stringList"
          @update:model-value="(v: any) => updateConfig('delimiters', v)"
        />
      </AFormItem>
      <AFormItem label="去除首尾空白">
        <ASwitch
          :checked="Boolean(node.data.config?.trimParts ?? true)"
          @update:checked="(v: any) => updateConfig('trimParts', v)"
        />
      </AFormItem>
      <AFormItem label="移除空字符串">
        <ASwitch
          :checked="Boolean(node.data.config?.removeEmpty ?? true)"
          @update:checked="(v: any) => updateConfig('removeEmpty', v)"
        />
      </AFormItem>
      <AFormItem label="Split limit">
        <AInputNumber
          class="full-input"
          :value="Number(node.data.config?.limit ?? -1)"
          @update:value="(v: any) => updateConfig('limit', v)"
        />
      </AFormItem>
      <AFormItem label="最大结果数">
        <AInputNumber
          class="full-input"
          :value="Number(node.data.config?.maxResults ?? -1)"
          @update:value="(v: any) => updateConfig('maxResults', v)"
        />
      </AFormItem>
      <AFormItem label="处理分割结果">
        <ASwitch
          :checked="Boolean(node.data.config?.processingResult ?? true)"
          @update:checked="(v: any) => updateConfig('processingResult', v)"
        />
      </AFormItem>
      <AFormItem label="结果前缀">
        <AInput
          :value="String(node.data.config?.prefix || '')"
          @update:value="(v: any) => updateConfig('prefix', v)"
        />
      </AFormItem>
      <AFormItem label="结果后缀">
        <AInput
          :value="String(node.data.config?.suffix || '')"
          @update:value="(v: any) => updateConfig('suffix', v)"
        />
      </AFormItem>
      <template v-if="isKeyValue">
        <AFormItem label="键值分隔符">
          <AInput
            :value="String(node.data.config?.keyValueDelimiter ?? '=')"
            @update:value="(v: any) => updateConfig('keyValueDelimiter', v)"
          />
        </AFormItem>
        <AFormItem label="键值输出格式">
          <ASelect
            :value="node.data.config?.keyValueOutputFormat"
            :options="[
              { label: 'key: value', value: 'COLON_SEPARATED' },
              { label: 'key=value', value: 'EQUALS_SEPARATED' },
              { label: 'JSON 对象', value: 'JSON_OBJECT' },
              { label: 'key -> value', value: 'MAP_ENTRY' },
              { label: '自定义', value: 'CUSTOM' },
            ]"
            @update:value="(v: any) => updateConfig('keyValueOutputFormat', v)"
          />
        </AFormItem>
        <AFormItem
          v-if="(node.data.config?.keyValueOutputFormat as string) === 'CUSTOM'"
          label="自定义格式"
        >
          <AInput
            :value="String(node.data.config?.keyValueCustomFormat || '')"
            placeholder="%s===>%s"
            @update:value="(v: any) => updateConfig('keyValueCustomFormat', v)"
          />
        </AFormItem>
      </template>
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
