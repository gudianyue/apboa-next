<script setup lang="ts">
import InputBindingEditor from '@/components/workflow/bindings/InputBindingEditor.vue'
import PanelSection from './PanelSection.vue'
import type { WorkflowFlowNode, WorkflowInputConfig } from '@/types/workflow'

defineProps<{
  modelValue?: WorkflowInputConfig[]
  nodes: WorkflowFlowNode[]
  currentNodeId?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: WorkflowInputConfig[]]
}>()
</script>

<template>
  <PanelSection title="输入绑定">
    <InputBindingEditor
      :model-value="modelValue"
      :nodes="nodes"
      :current-node-id="currentNodeId"
      @update:model-value="(value) => emit('update:modelValue', value)"
    />
  </PanelSection>
</template>
