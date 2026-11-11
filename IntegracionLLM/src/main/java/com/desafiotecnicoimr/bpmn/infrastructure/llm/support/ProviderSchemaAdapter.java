package com.desafiotecnicoimr.bpmn.infrastructure.llm.support;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Set;

public final class ProviderSchemaAdapter {

    private static final Set<String> NON_PORTABLE_KEYWORDS = Set.of(
            "$schema",
            "minLength",
            "maxLength",
            "pattern"
    );

    private ProviderSchemaAdapter() {
    }

    public static JsonNode portableSchema(ObjectMapper objectMapper, String schemaJson) throws JacksonException {
        JsonNode schema = objectMapper.readTree(schemaJson).deepCopy();
        removeNonPortableKeywords(schema);
        return schema;
    }

    public static JsonNode geminiSchema(ObjectMapper objectMapper, String schemaJson) throws JacksonException {
        JsonNode schema = portableSchema(objectMapper, schemaJson);
        removeGeminiArrayLimits(schema);
        return schema;
    }

    private static void removeGeminiArrayLimits(JsonNode node) {
        if (node instanceof ObjectNode objectNode
                && node.path("type").isTextual()
                && "array".equals(node.path("type").asText())) {
            // Gemini supports these bounds, but compiling them into constrained
            // decoding increases schema complexity. ProcessValidator enforces
            // the same limits after generation.
            objectNode.remove("minItems");
            objectNode.remove("maxItems");
        }
        node.forEach(ProviderSchemaAdapter::removeGeminiArrayLimits);
    }

    private static void removeNonPortableKeywords(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            NON_PORTABLE_KEYWORDS.forEach(objectNode::remove);
            objectNode.properties().forEach(entry -> removeNonPortableKeywords(entry.getValue()));
            return;
        }
        node.forEach(ProviderSchemaAdapter::removeNonPortableKeywords);
    }
}
