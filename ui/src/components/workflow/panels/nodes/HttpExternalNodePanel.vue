<script setup lang="ts">
import { ref } from 'vue'
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

function getRequest() {
  return (
    (props.node.data.config?.request as Record<string, unknown>) || {
      method: 'GET',
      contentType: 'JSON',
      pathParams: [],
      queryParams: [],
      headers: [],
      body: '',
    }
  )
}
function updateRequest(key: string, value: unknown) {
  updateConfig('request', { ...getRequest(), [key]: value })
}

const advancedOpen = ref(false)
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
      <div class="http-method-line">
        <ASelect
          style="width: 120px"
          :value="getRequest().method || 'GET'"
          :options="
            ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS'].map((v: any) => ({
              label: v,
              value: v,
            }))
          "
          @update:value="(v: any) => updateRequest('method', v)"
        />
        <AInput
          :value="String(getRequest().url || '')"
          placeholder="https://api.example.com/users/${id}"
          @update:value="(v: any) => updateRequest('url', v)"
        />
      </div>

      <AFormItem label="Content-Type">
        <ASelect
          :value="getRequest().contentType || 'JSON'"
          :options="[
            { label: 'JSON', value: 'JSON' },
            { label: 'Form UrlEncoded', value: 'FORM_URLENCODED' },
            { label: 'Form Data', value: 'FORM_DATA' },
            { label: 'XML', value: 'XML' },
            { label: 'Text Plain', value: 'TEXT_PLAIN' },
            { label: 'Octet Stream', value: 'OCTET_STREAM' },
          ]"
          @update:value="(v: any) => updateRequest('contentType', v)"
        />
      </AFormItem>

      <ACollapse ghost>
        <ACollapsePanel key="path" header="路径参数">
          <WorkflowArrayEditors
            :model-value="getRequest().pathParams"
            type="keyValue"
            @update:model-value="(v: any) => updateRequest('pathParams', v)"
          />
        </ACollapsePanel>
        <ACollapsePanel key="query" header="Query 参数">
          <WorkflowArrayEditors
            :model-value="getRequest().queryParams"
            type="keyValue"
            @update:model-value="(v: any) => updateRequest('queryParams', v)"
          />
        </ACollapsePanel>
        <ACollapsePanel key="headers" header="请求头">
          <WorkflowArrayEditors
            :model-value="getRequest().headers"
            type="keyValue"
            @update:model-value="(v: any) => updateRequest('headers', v)"
          />
        </ACollapsePanel>
      </ACollapse>

      <AFormItem label="Request Body">
        <ATextarea
          :value="
            typeof getRequest().body === 'string'
              ? String(getRequest().body)
              : JSON.stringify(getRequest().body ?? '', null, 2)
          "
          :auto-size="{ minRows: 4, maxRows: 12 }"
          placeholder="请求 Body，支持 JSON 字符串或对象"
          @update:value="
            (v: any) => {
              try {
                updateRequest('body', JSON.parse(v))
              } catch {
                updateRequest('body', v)
              }
            }
          "
        />
      </AFormItem>

      <div class="advanced-toggle" @click="advancedOpen = !advancedOpen">
        <span>高级设置</span>
        <span class="toggle-arrow" :class="{ open: advancedOpen }">&#9662;</span>
      </div>

      <div v-show="advancedOpen" class="advanced-panel">
        <AFormItem label="请求模板格式">
          <ASelect
            :value="node.data.config?.formatterType || 'STRING'"
            :options="formatterOptions"
            @update:value="(v: any) => updateConfig('formatterType', v)"
          />
        </AFormItem>
        <AFormItem label="连接超时(秒)">
          <AInputNumber
            class="full-input"
            :value="Number(node.data.config?.connectTimeout ?? 10)"
            :min="1"
            @update:value="(v: any) => updateConfig('connectTimeout', v)"
          />
        </AFormItem>
        <AFormItem label="读取超时(秒)">
          <AInputNumber
            class="full-input"
            :value="Number(node.data.config?.readTimeout ?? 30)"
            :min="1"
            @update:value="(v: any) => updateConfig('readTimeout', v)"
          />
        </AFormItem>
        <AFormItem label="写入超时(秒)">
          <AInputNumber
            class="full-input"
            :value="Number(node.data.config?.writeTimeout ?? 30)"
            :min="1"
            @update:value="(v: any) => updateConfig('writeTimeout', v)"
          />
        </AFormItem>
        <AFormItem label="最大重试次数">
          <AInputNumber
            class="full-input"
            :value="Number(node.data.config?.maxRetries ?? 3)"
            :min="0"
            @update:value="(v: any) => updateConfig('maxRetries', v)"
          />
        </AFormItem>
        <AFormItem label="重试状态码">
          <WorkflowArrayEditors
            :model-value="node.data.config?.retryStatusCodes"
            type="stringList"
            @update:model-value="(v: any) => updateConfig('retryStatusCodes', v)"
          />
        </AFormItem>
        <AFormItem label="跟随重定向">
          <ASwitch
            :checked="Boolean(node.data.config?.followRedirects ?? true)"
            @update:checked="(v: any) => updateConfig('followRedirects', v)"
          />
        </AFormItem>
        <AFormItem label="同步执行">
          <ASwitch
            :checked="Boolean(node.data.config?.syncExecute ?? true)"
            @update:checked="(v: any) => updateConfig('syncExecute', v)"
          />
        </AFormItem>
        <AFormItem label="响应体转 JSON">
          <ASwitch
            :checked="Boolean(node.data.config?.bodyToObject ?? true)"
            @update:checked="(v: any) => updateConfig('bodyToObject', v)"
          />
        </AFormItem>
      </div>
    </PanelSection>

    <PanelSection title="输出说明">
      <OutputDisplay :outputs="node.data.outputConfigs || []" />
    </PanelSection>
  </AForm>
</template>

<style scoped lang="scss">
.http-method-line {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.full-input {
  width: 100%;
}
.advanced-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-top: 1px solid #f0f0f0;
  cursor: pointer;
  color: #595959;
  font-size: 13px;
  font-weight: 600;
  user-select: none;
  .toggle-arrow {
    transition: transform 0.2s;
    &.open {
      transform: rotate(180deg);
    }
  }
}
.advanced-panel {
  display: grid;
  gap: 2px;
  padding-top: 4px;
}
@media (max-width: 720px) {
  .http-method-line {
    grid-template-columns: 1fr;
  }
}
</style>
