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

const formatterOptions = [
  { label: '普通字符串', value: 'STRING' },
  { label: 'Jackson JSON', value: 'JACKSON' },
  { label: 'Velocity 模板', value: 'VELOCITY' },
]
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
      <div class="config-desc">向 Kafka、RabbitMQ 或 RocketMQ 推送消息。</div>
      <AFormItem label="MQ 实例" required>
        <WorkflowResourceSelect
          :model-value="String(node.data.config?.mqId || '')"
          resource-type="mq"
          :resources="resources"
          @update:model-value="(v: any) => updateConfig('mqId', v)"
        />
      </AFormItem>
      <AFormItem label="Topic / Queue" required>
        <AInput
          :value="String(node.data.config?.topicOrQueue || '')"
          placeholder="Kafka/RocketMQ 为 topic，RabbitMQ 为 queue"
          @update:value="(v: any) => updateConfig('topicOrQueue', v)"
        />
      </AFormItem>
      <AFormItem label="消息 Key">
        <AInput
          :value="String(node.data.config?.key || '')"
          placeholder="Kafka 分区键、RabbitMQ routing key、RocketMQ tag"
          @update:value="(v: any) => updateConfig('key', v)"
        />
      </AFormItem>
      <AFormItem label="消息内容">
        <SmartCodeEditor
          :model-value="String(node.data.config?.message || '')"
          language="json"
          theme="light"
          height="160px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder='如 {"key":"value"}'
          @update:model-value="(v: any) => updateConfig('message', v)"
        />
      </AFormItem>
      <AFormItem label="消息模板">
        <SmartCodeEditor
          :model-value="String(node.data.config?.messageTemplate || '')"
          language="txt"
          theme="light"
          height="120px"
          :show-change-language="false"
          :show-theme-toggle="false"
          :show-fullscreen="true"
          placeholder="优先级高于消息内容"
          @update:model-value="(v: any) => updateConfig('messageTemplate', v)"
        />
      </AFormItem>
      <AFormItem label="模板格式">
        <ASelect
          :value="node.data.config?.templateType || 'STRING'"
          :options="formatterOptions"
          @update:value="(v: any) => updateConfig('templateType', v)"
        />
      </AFormItem>
    </PanelSection>
    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.config-desc {
  margin-bottom: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f6f8fa;
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.6;
}
</style>
