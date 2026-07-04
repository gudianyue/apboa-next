<script setup lang="ts">
import { computed } from 'vue'
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

const mode = computed(() => props.node.data.config?.mode as string)
const isKeyValue = computed(() => mode.value === 'KEY_VALUE')
const isMultiDelimiter = computed(() => mode.value === 'MULTIPLE_DELIMITERS')
const showDelimiter = computed(
  () => mode.value === 'SIMPLE' || mode.value === 'REGEX' || mode.value === 'FIXED_LENGTH',
)
const delimiterLabel = computed(() => {
  switch (mode.value) {
    case 'SIMPLE':
      return '分隔符'
    case 'REGEX':
      return '正则表达式'
    case 'FIXED_LENGTH':
      return '分割长度'
    default:
      return '分隔符'
  }
})
const processingResult = computed(
  () => Boolean(props.node.data.config?.processingResult ?? true),
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
      :edges="edges"
      :current-node-id="node.id"
      :max-bindings="1"
      :readonly-name="true"
      @update:model-value="(v: any) => updateNode({ inputConfigs: v })"
    />
    <PanelSection title="节点配置">
      <div class="config-row">
        <span class="config-row-label">分割模式</span>
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
          style="width: 160px"
          @update:value="(v: any) => updateConfig('mode', v)"
        />
      </div>
      <div v-if="showDelimiter" class="config-row">
        <span class="config-row-label">{{ delimiterLabel }}</span>
        <BlurInput
          :model-value="String(node.data.config?.delimiter || '')"
          style="width: 160px"
          @update:model-value="(v: any) => updateConfig('delimiter', v)"
        />
      </div>
      <AFormItem v-if="isMultiDelimiter" label="多个分隔符">
        <WorkflowArrayEditors
          :model-value="node.data.config?.delimiters"
          type="stringList"
          @update:model-value="(v: any) => updateConfig('delimiters', v)"
        />
      </AFormItem>
      <div class="config-row">
        <span class="config-row-label">去除首尾空白</span>
        <ASwitch
          :checked="Boolean(node.data.config?.trimParts ?? true)"
          @update:checked="(v: any) => updateConfig('trimParts', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">移除空字符串</span>
        <ASwitch
          :checked="Boolean(node.data.config?.removeEmpty ?? true)"
          @update:checked="(v: any) => updateConfig('removeEmpty', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">Split limit</span>
        <AInputNumber
          :value="Number(node.data.config?.limit ?? -1)"
          style="width: 160px"
          @update:value="(v: any) => updateConfig('limit', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">最大结果数</span>
        <AInputNumber
          :value="Number(node.data.config?.maxResults ?? -1)"
          style="width: 160px"
          @update:value="(v: any) => updateConfig('maxResults', v)"
        />
      </div>
      <div class="config-row">
        <span class="config-row-label">处理分割结果</span>
        <ASwitch
          :checked="processingResult"
          @update:checked="(v: any) => updateConfig('processingResult', v)"
        />
      </div>
      <div v-if="processingResult" class="config-row">
        <span class="config-row-label">结果前缀</span>
        <BlurInput
          :model-value="String(node.data.config?.prefix || '')"
          style="width: 160px"
          @update:model-value="(v: any) => updateConfig('prefix', v)"
        />
      </div>
      <div v-if="processingResult" class="config-row">
        <span class="config-row-label">结果后缀</span>
        <BlurInput
          :model-value="String(node.data.config?.suffix || '')"
          style="width: 160px"
          @update:model-value="(v: any) => updateConfig('suffix', v)"
        />
      </div>
      <template v-if="isKeyValue">
        <div class="config-row">
          <span class="config-row-label">键值分隔符</span>
          <BlurInput
            :model-value="String(node.data.config?.keyValueDelimiter ?? '=')"
            style="width: 160px"
            @update:model-value="(v: any) => updateConfig('keyValueDelimiter', v)"
          />
        </div>
        <div class="config-row">
          <span class="config-row-label">键值输出格式</span>
          <ASelect
            :value="node.data.config?.keyValueOutputFormat"
            :options="[
              { label: 'key: value', value: 'COLON_SEPARATED' },
              { label: 'key=value', value: 'EQUALS_SEPARATED' },
              { label: 'JSON 对象', value: 'JSON_OBJECT' },
              { label: 'key -> value', value: 'MAP_ENTRY' },
              { label: '自定义', value: 'CUSTOM' },
            ]"
            style="width: 140px"
            @update:value="(v: any) => updateConfig('keyValueOutputFormat', v)"
          />
        </div>
        <div
          v-if="(node.data.config?.keyValueOutputFormat as string) === 'CUSTOM'"
          class="config-row"
        >
          <span class="config-row-label">自定义格式</span>
          <BlurInput
            :model-value="String(node.data.config?.keyValueCustomFormat || '')"
            placeholder="%s===>%s"
            style="width: 160px"
            @update:model-value="(v: any) => updateConfig('keyValueCustomFormat', v)"
          />
        </div>
      </template>
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
