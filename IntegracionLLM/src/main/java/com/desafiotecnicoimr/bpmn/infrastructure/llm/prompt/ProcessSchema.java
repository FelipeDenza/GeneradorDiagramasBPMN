package com.desafiotecnicoimr.bpmn.infrastructure.llm.prompt;

import org.springframework.stereotype.Component;

@Component
public class ProcessSchema {

    private static final String JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "additionalProperties": false,
              "required": ["isProcessDescription", "reason", "process"],
              "properties": {
                "isProcessDescription": {"type": "boolean"},
                "reason": {"type": ["string", "null"], "maxLength": 240},
                "process": {
                  "type": ["object", "null"],
                  "additionalProperties": false,
                  "required": ["processName", "nodes", "flows"],
                  "properties": {
                    "processName": {"type": "string", "minLength": 1, "maxLength": 120},
                    "nodes": {
                      "type": "array",
                      "minItems": 1,
                      "maxItems": 30,
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["key", "type", "name"],
                        "properties": {
                          "key": {"type": "string", "pattern": "^[A-Za-z][A-Za-z0-9_-]{0,63}$"},
                          "type": {
                            "type": "string",
                            "enum": ["START_EVENT", "END_EVENT", "TASK", "EXCLUSIVE_GATEWAY", "PARALLEL_GATEWAY"]
                          },
                          "name": {"type": ["string", "null"], "maxLength": 120}
                        }
                      }
                    },
                    "flows": {
                      "type": "array",
                      "maxItems": 60,
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["source", "target", "name"],
                        "properties": {
                          "source": {"type": "string"},
                          "target": {"type": "string"},
                          "name": {"type": ["string", "null"], "maxLength": 120}
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    public String value() {
        return JSON_SCHEMA;
    }
}
