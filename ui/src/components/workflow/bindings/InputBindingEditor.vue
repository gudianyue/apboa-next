<script setup lang="ts">
import { computed } from 'vue'
import { QuestionCircleOutlined } from '@ant-design/icons-vue'
import BlurInput from '@/components/workflow/panels/shared/BlurInput.vue'
import type { WorkflowFlowNode, WorkflowInputConfig } from '@/types/workflow'

const sourceTypeOptions = [
  { label: '常量', value: 'CONSTANT' as const, description: '直接填写固定值，支持字符串或 JSON 格式' },
  { label: '变量', value: 'VARIABLE' as const, description: '引用工作流全局变量，运行时动态注入' },
  { label: '节点输出', value: 'NODE_OUTPUT' as const, description: '引用其他节点的输出结果，构建节点间数据流' },
  { label: '表达式', value: 'EXPRESSION' as const, description: '使用 GroovyShell 表达式动态计算值' },
]

const props = defineProps<{
  modelValue?: WorkflowInputConfig[]
  nodes: WorkflowFlowNode[]
  currentNodeId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: WorkflowInputConfig[]]
}>()

const bindings = computed(() => (props.modelValue?.length ? props.modelValue : [{ name: 'input', sourceType: 'NODE_OUTPUT' as const }]))

const nodeOptions = computed(() =>
  props.nodes
    .filter((node) => node.id !== props.currentNodeId)
    .map((node) => ({
      label: `${node.data.label} · ${node.data.schema?.title || node.data.type}`,
      value: node.id,
      node,
    })),
)

function update(index: number, patch: Partial<WorkflowInputConfig>) {
  const next = bindings.value.map((item) => ({ ...item }))
  next[index] = { name: next[index]?.name || 'input', sourceType: next[index]?.sourceType || 'NODE_OUTPUT', ...next[index], ...patch }
  emit('update:modelValue', next)
}

function addBinding() {
  emit('update:modelValue', [...bindings.value, { name: `input${bindings.value.length + 1}`, sourceType: 'NODE_OUTPUT' }])
}

function removeBinding(index: number) {
  emit('update:modelValue', bindings.value.filter((_, itemIndex) => itemIndex !== index))
}
</script>

<template>
  <div class="binding-editor">
    <div v-for="(binding, index) in bindings" :key="index" class="binding-card">
      <div class="binding-head">
        <BlurInput
          :model-value="binding.name"
          placeholder="输入名"
          @update:model-value="(value: string) => update(index, { name: value })"
        />
        <AButton v-if="bindings.length > 1" danger type="text" @click="removeBinding(index)">删除</AButton>
      </div>

      <div class="source-type-segmented">
        <ATooltip
          v-for="opt in sourceTypeOptions"
          :key="opt.value"
          :title="opt.description"
          placement="topLeft"
          :overlay-inner-style="{ maxWidth: '170px' }"
        >
          <button
            :class="['segmented-option', { active: binding.sourceType === opt.value }]"
            type="button"
            @click="update(index, { sourceType: opt.value })"
          >
            <span class="segmented-label">{{ opt.label }}</span>
            <QuestionCircleOutlined class="segmented-help" />
          </button>
        </ATooltip>
      </div>

      <ATextarea
        v-if="binding.sourceType === 'CONSTANT'"
        :rows="1"
        :model-value="typeof binding.value === 'string' ? binding.value : JSON.stringify(binding.value ?? '', null, 2)"
        placeholder="常量值，可填写字符串或 JSON"
        @update:model-value="(value: string) => update(index, { value })"
      />

      <BlurInput
        v-else-if="binding.sourceType === 'VARIABLE'"
        :model-value="binding.variableName"
        placeholder="全局变量名"
        @update:model-value="(value: string) => update(index, { variableName: value })"
      />

      <div v-else-if="binding.sourceType === 'NODE_OUTPUT'" class="node-output-grid">
        <ASelect
          show-search
          :model-value="binding.nodeId"
          :options="nodeOptions"
          placeholder="选择任意前置或已存在节点"
          @update:model-value="(value: string) => update(index, { nodeId: value })"
        />
        <BlurInput
          :model-value="binding.outputName || 'output'"
          placeholder="输出名"
          @update:model-value="(value: string) => update(index, { outputName: value })"
        />
      </div>

      <ATextarea
        v-else
        :rows="1"
        :model-value="typeof binding.value === 'string' ? binding.value : JSON.stringify(binding.value ?? '', null, 2)"
        placeholder="支持编写GroovyShell表单式"
        @update:model-value="(value: string) => update(index, { value })"
      />
    </div>

    <AButton block size="small" class="add-binding" @click="addBinding">添加输入绑定</AButton>
  </div>
</template>

<style scoped lang="scss">
.binding-editor {
  display: grid;
  gap: 10px;
}

.binding-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  background: #fff;
}

.binding-head,
.node-output-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: center;
}

.node-output-grid {
  grid-template-columns: minmax(0, 1fr) 110px;
}

.source-type-segmented {
  display: flex;
  border-radius: 6px;
  background: #F2F4F7;
  padding: 2px;
}

.segmented-option {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 4px 6px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  transition: all 0.2s;
  line-height: 1.4;

  &:hover {
    color: rgba(0, 0, 0, 0.85);
  }

  &.active {
    background: #fff;
    color: rgba(0, 0, 0, 0.88);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  }
}

.segmented-help {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.25);
  transition: color 0.2s;

  .segmented-option:hover & {
    color: rgba(0, 0, 0, 0.45);
  }
}

.add-binding {
  border-style: dashed;
}
</style>
