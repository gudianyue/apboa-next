/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.spring.boot.agui.mvc;

import com.hxh.apboa.agent.service.ChatSessionService;
import com.hxh.apboa.common.config.auth.ChatKeyAccess;
import com.hxh.apboa.common.config.auth.SkAccess;
import com.hxh.apboa.common.entity.ChatSession;
import io.agentscope.core.agui.model.RunAgentInput;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for AG-UI protocol endpoints.
 *
 * <p>This controller exposes the AG-UI run endpoints for Spring MVC applications.
 * It delegates the actual processing to {@link AguiMvcController}.
 */
@RestController
public class AguiRestController {

    private final AguiMvcController aguiMvcController;
    private final ChatSessionService chatSessionService;
    private final String pathPrefix;
    private final boolean enablePathRouting;

    /**
     * Creates a new AguiRestController.
     *
     * @param aguiMvcController The AG-UI MVC controller
     * @param pathPrefix The path prefix for endpoints
     * @param enablePathRouting Whether to enable path variable routing
     */
    public AguiRestController(
            AguiMvcController aguiMvcController,
            ChatSessionService chatSessionService,
            String pathPrefix,
            boolean enablePathRouting) {
        this.aguiMvcController = aguiMvcController;
        this.chatSessionService = chatSessionService;
        this.pathPrefix = pathPrefix;
        this.enablePathRouting = enablePathRouting;
    }

    /**
     * Handle an AG-UI run request.
     *
     * <p>Agent ID is resolved from (in priority order):
     * <ol>
     *   <li>HTTP header (configurable, default: X-Agent-Id)</li>
     *   <li>forwardedProps.agentId in request body</li>
     *   <li>config.defaultAgentId</li>
     *   <li>"default"</li>
     * </ol>
     *
     * @param input The run agent input
     * @param agentIdHeader The agent ID from HTTP header (optional)
     * @return An SseEmitter for streaming AG-UI events
     */
    @SkAccess
    @ChatKeyAccess
    @PostMapping(
            value = "${agentscope.agui.path-prefix:/agui}/run",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter run(
            @RequestBody RunAgentInput input,
            @RequestHeader(
                            value = "${agentscope.agui.agent-id-header:X-Agent-Id}",
                            required = false)
                    String agentIdHeader) {
        return aguiMvcController.handle(input, agentIdHeader);
    }

    /**
     * Handle an AG-UI run request with agent ID in the URL path.
     *
     * <p>The path variable takes highest priority for agent resolution.
     *
     * @param agentId The agent ID from path variable
     * @param input The run agent input
     * @param agentIdHeader The agent ID from HTTP header (optional)
     * @return An SseEmitter for streaming AG-UI events
     */
    @SkAccess
    @ChatKeyAccess
    @PostMapping(
            value = "${agentscope.agui.path-prefix:/agui}/run/{agentId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runWithAgentId(
            @PathVariable("agentId") String agentId,
            @RequestBody RunAgentInput input,
            @RequestHeader(
                            value = "${agentscope.agui.agent-id-header:X-Agent-Id}",
                            required = false)
                    String agentIdHeader) {
        ChatSession chatSession = chatSessionService.getById(input.getThreadId());
        if (chatSession == null ||
                (chatSession.getMessageTable() != null && !chatSession.getMessageTable().isEmpty())) {
            throw new IllegalArgumentException("当前会话已经归档，不支持继续对话");
        }

        return aguiMvcController.handleWithAgentId(input, agentIdHeader, agentId);
    }

    /**
     * SSE 重连端点：断线后重新获取事件流。
     *
     * @param threadId 会话 ID
     * @return SseEmitter 回放缓冲区事件后接续实时流
     */
    @SkAccess
    @ChatKeyAccess
    @GetMapping(
            value = "${agentscope.agui.path-prefix:/agui}/reconnect/{threadId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reconnect(@PathVariable("threadId") String threadId) {
        return aguiMvcController.reconnect(threadId);
    }

    /**
     * 运行状态查询端点。
     *
     * @param threadId 会话 ID
     * @return { running: boolean }
     */
    @SkAccess
    @ChatKeyAccess
    @GetMapping(value = "${agentscope.agui.path-prefix:/agui}/status/{threadId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> getStatus(@PathVariable("threadId") String threadId) {
        boolean running = aguiMvcController.getStatus(threadId);
        return ResponseEntity.ok(Map.of("running", running));
    }

    /**
     * 强制停止端点。
     *
     * @param threadId 会话 ID
     * @return { stopped: true }
     */
    @SkAccess
    @ChatKeyAccess
    @PostMapping(value = "${agentscope.agui.path-prefix:/agui}/stop/{threadId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> stop(@PathVariable("threadId") String threadId) {
        aguiMvcController.stop(threadId);
        return ResponseEntity.ok(Map.of("stopped", true));
    }

    /**
     * 活跃运行列表端点。
     *
     * @return 所有活跃运行的 threadId 列表
     */
    @SkAccess
    @ChatKeyAccess
    @GetMapping(value = "${agentscope.agui.path-prefix:/agui}/active-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<String>> getActiveRuns() {
        return ResponseEntity.ok(aguiMvcController.getActiveRuns());
    }
}
