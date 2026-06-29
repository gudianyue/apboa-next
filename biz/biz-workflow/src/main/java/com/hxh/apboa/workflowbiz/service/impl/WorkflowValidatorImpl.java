package com.hxh.apboa.workflowbiz.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hxh.apboa.node.base.Node;
import com.hxh.apboa.node.base.NodeType;
import com.hxh.apboa.node.base.context.NodeContext;
import com.hxh.apboa.node.base.verify.VerifyFail;
import com.hxh.apboa.node.base.verify.VerifyResult;
import com.hxh.apboa.workflow.run.RunWorkflow;
import com.hxh.apboa.workflowbiz.core.WorkflowDefinitionCompiler;
import com.hxh.apboa.workflowbiz.service.WorkflowValidator;
import com.hxh.apboa.workflowbiz.vo.WorkflowValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkflowValidatorImpl implements WorkflowValidator {
    private final ObjectMapper objectMapper;
    private final WorkflowDefinitionCompiler compiler;

    @Override
    public WorkflowValidationResult validate(Object definition) {
        WorkflowValidationResult result = new WorkflowValidationResult();
        JsonNode root = objectMapper.valueToTree(definition);
        JsonNode nodes = root.path("nodes");
        JsonNode edges = root.path("edges");

        if (!nodes.isArray() || nodes.isEmpty()) {
            result.addError(null, "nodes", "workflow.nodes must contain at least START and END nodes");
            return result;
        }
        if (!edges.isMissingNode() && !edges.isArray()) {
            result.addError(null, "edges", "workflow.edges must be an array");
        }

        Set<String> nodeIds = new HashSet<>();
        Map<String, JsonNode> nodeById = new HashMap<>();
        Set<String> startNodeIds = new HashSet<>();
        Set<String> endNodeIds = new HashSet<>();
        int startCount = 0;
        int endCount = 0;
        for (JsonNode node : nodes) {
            String id = text(node, "id");
            String typeText = text(node, "type");
            if (id == null || id.isBlank()) {
                result.addError(null, "id", "node id is required");
                continue;
            }
            if (!nodeIds.add(id)) {
                result.addError(id, "id", "node id is duplicated");
            }
            nodeById.put(id, node);
            try {
                NodeType type = NodeType.valueOf(typeText);
                if (!WorkflowDefinitionCompiler.supportedTypes().contains(type)) {
                    result.addError(id, "type", "node type is not supported: " + typeText);
                }
                if (type == NodeType.START) {
                    startCount++;
                    startNodeIds.add(id);
                }
                if (type == NodeType.END) {
                    endCount++;
                    endNodeIds.add(id);
                }
            } catch (Exception e) {
                result.addError(id, "type", "node type is invalid: " + typeText);
            }
        }
        if (startCount != 1) {
            result.addError(null, "nodes", "workflow must contain exactly one START node");
        }
        if (endCount < 1) {
            result.addError(null, "nodes", "workflow must contain at least one END node");
        }
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        nodeIds.forEach(id -> {
            adjacency.put(id, new ArrayList<>());
            indegree.put(id, 0);
        });
        if (edges.isArray()) {
            Set<String> edgeIds = new HashSet<>();
            for (JsonNode edge : edges) {
                String id = text(edge, "id");
                String source = text(edge, "source");
                String target = text(edge, "target");
                if (id == null || id.isBlank()) {
                    result.addError(null, "edges", "edge id is required");
                } else if (!edgeIds.add(id)) {
                    result.addError(id, "id", "edge id is duplicated");
                }
                if (source == null || !nodeIds.contains(source)) {
                    result.addError(id, "source", "edge source node does not exist");
                }
                if (target == null || !nodeIds.contains(target)) {
                    result.addError(id, "target", "edge target node does not exist");
                }
                if (source != null && target != null && nodeIds.contains(source) && nodeIds.contains(target)) {
                    adjacency.get(source).add(target);
                    indegree.put(target, indegree.get(target) + 1);
                }
            }
        }
        validateGraph(result, nodeIds, startNodeIds, endNodeIds, adjacency, indegree);
        validateInputReferences(result, nodeById, nodeIds);
        if (!result.isValid()) {
            return result;
        }

        try {
            RunWorkflow workflow = compiler.compile("validate", definition);
            NodeContext context = new NodeContext("validate");
            for (Node node : workflow.getNodes()) {
                VerifyResult verifyResult = node.verifyConfig(java.util.Map.of());
                if (!verifyResult.isValid()) {
                    for (VerifyFail error : verifyResult.getErrors()) {
                        result.addError(node.getId(), error.getField(), error.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            result.addError(null, "definition", e.getMessage());
        }
        return result;
    }

    private void validateGraph(WorkflowValidationResult result,
                               Set<String> nodeIds,
                               Set<String> startNodeIds,
                               Set<String> endNodeIds,
                               Map<String, List<String>> adjacency,
                               Map<String, Integer> indegree) {
        for (String startId : startNodeIds) {
            if (indegree.getOrDefault(startId, 0) > 0) {
                result.addError(startId, "edges", "START node cannot have incoming edges");
            }
            if (adjacency.getOrDefault(startId, List.of()).isEmpty()) {
                result.addError(startId, "edges", "START node must connect to at least one node");
            }
        }
        for (String endId : endNodeIds) {
            if (!adjacency.getOrDefault(endId, List.of()).isEmpty()) {
                result.addError(endId, "edges", "END node cannot have outgoing edges");
            }
            if (indegree.getOrDefault(endId, 0) == 0) {
                result.addError(endId, "edges", "END node must have at least one incoming edge");
            }
        }

        Set<String> reachable = reachableFrom(startNodeIds, adjacency);
        for (String nodeId : nodeIds) {
            if (!reachable.contains(nodeId)) {
                result.addError(nodeId, "edges", "node is not reachable from START");
            }
        }

        Set<String> canReachEnd = reverseReachableFrom(endNodeIds, adjacency);
        for (String nodeId : nodeIds) {
            if (!canReachEnd.contains(nodeId)) {
                result.addError(nodeId, "edges", "node cannot reach any END node");
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String nodeId : nodeIds) {
            if (hasCycle(nodeId, adjacency, visiting, visited)) {
                result.addWarning(nodeId, "edges", "workflow contains a cycle; confirm that loop conditions can exit");
                break;
            }
        }
    }

    private void validateInputReferences(WorkflowValidationResult result, Map<String, JsonNode> nodeById, Set<String> nodeIds) {
        for (Map.Entry<String, JsonNode> entry : nodeById.entrySet()) {
            JsonNode inputs = entry.getValue().path("inputConfigs");
            if (!inputs.isArray()) {
                continue;
            }
            Set<String> inputNames = new HashSet<>();
            for (JsonNode input : inputs) {
                String name = text(input, "name");
                if (name == null || name.isBlank()) {
                    result.addError(entry.getKey(), "inputConfigs", "input name is required");
                } else if (!inputNames.add(name)) {
                    result.addError(entry.getKey(), "inputConfigs." + name, "input name is duplicated");
                }
                String classify = firstText(input, "classify", "sourceType");
                if (classify == null || classify.isBlank() || "NODE_OUTPUT".equals(classify)) {
                    String sourceNodeId = firstText(input, "sourceNodeId", "nodeId");
                    if (sourceNodeId == null || sourceNodeId.isBlank()) {
                        result.addError(entry.getKey(), "inputConfigs." + name, "source node is required");
                    } else if (!nodeIds.contains(sourceNodeId)) {
                        result.addError(entry.getKey(), "inputConfigs." + name, "source node does not exist: " + sourceNodeId);
                    }
                    String outputName = firstText(input, "sourceOutputName", "outputName");
                    if (outputName == null || outputName.isBlank()) {
                        result.addError(entry.getKey(), "inputConfigs." + name, "source output name is required");
                    }
                } else if ("VARIABLE".equals(classify) && isBlank(text(input, "variableName"))) {
                    result.addError(entry.getKey(), "inputConfigs." + name, "variable name is required");
                } else if ("EXPRESSION".equals(classify) && isBlank(text(input, "expression"))) {
                    result.addError(entry.getKey(), "inputConfigs." + name, "expression is required");
                }
            }
        }
    }

    private Set<String> reachableFrom(Set<String> startIds, Map<String, List<String>> adjacency) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>(startIds);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!visited.add(current)) {
                continue;
            }
            queue.addAll(adjacency.getOrDefault(current, List.of()));
        }
        return visited;
    }

    private Set<String> reverseReachableFrom(Set<String> endIds, Map<String, List<String>> adjacency) {
        Map<String, List<String>> reverse = new HashMap<>();
        adjacency.forEach((source, targets) -> {
            reverse.computeIfAbsent(source, ignored -> new ArrayList<>());
            for (String target : targets) {
                reverse.computeIfAbsent(target, ignored -> new ArrayList<>()).add(source);
            }
        });
        return reachableFrom(endIds, reverse);
    }

    private boolean hasCycle(String nodeId, Map<String, List<String>> adjacency, Set<String> visiting, Set<String> visited) {
        if (visited.contains(nodeId)) {
            return false;
        }
        if (!visiting.add(nodeId)) {
            return true;
        }
        for (String next : adjacency.getOrDefault(nodeId, List.of())) {
            if (hasCycle(next, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        return false;
    }

    private String firstText(JsonNode node, String first, String second) {
        String firstValue = text(node, first);
        return isBlank(firstValue) ? text(node, second) : firstValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
