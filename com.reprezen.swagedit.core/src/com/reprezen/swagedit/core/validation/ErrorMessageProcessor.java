package com.reprezen.swagedit.core.validation;

import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;

public class ErrorMessageProcessor {

    public String rewriteMessage(JsonNode error) {
        if (error == null) {
            return "";
        }

        if (!error.has("keyword")) {
            return error.has("message") ? error.get("message").asText() : "";
        }

        switch (error.get("keyword").asText()) {
        case "type":
            return rewriteTypeError(error);
        case "enum":
            return rewriteEnumError(error);
        case "additionalProperties":
            return rewriteAdditionalProperties(error);
        case "required":
            return rewriteRequiredProperties(error);
        default:
            return error.get("message").asText();
        }
    }

    protected String rewriteRequiredProperties(JsonNode error) {
        JsonNode missing = error.get("missing");

        final StringJoiner missingStringJoiner = new StringJoiner(", ");
        missing.forEach(it -> missingStringJoiner.add(it.toString()));

        return String.format(Messages.error_missing_property, missingStringJoiner.toString());
    }

    protected String rewriteAdditionalProperties(JsonNode error) {
        final JsonNode unwanted = error.get("unwanted");
        final StringJoiner unwantedStringJoiner = new StringJoiner(", ");
        unwanted.forEach(it -> unwantedStringJoiner.add(it.toString()));

        return String.format(Messages.error_additional_properties_not_allowed, unwantedStringJoiner.toString());
    }

    protected String rewriteTypeError(JsonNode error) {
        final JsonNode found = error.get("found");
        final JsonNode expected = error.get("expected");

        String expect;
        if (expected.isArray()) {
            expect = expected.get(0).asText();
        } else {
            expect = expected.asText();
        }

        if ("null".equals(found.asText())) {
            String pointer = ValidationUtil.getInstancePointer(error);
            if (pointer != null && pointer.endsWith("/type")) {
                return Messages.error_nullType;
            }
        }
        return String.format(Messages.error_typeNoMatch, found.asText(), expect);
    }

    protected String rewriteEnumError(JsonNode error) {
        final JsonNode value = error.get("value");
        final JsonNode enums = error.get("enum");
        final StringJoiner enumStringJoiner = new StringJoiner(", ");
        enums.forEach(it -> enumStringJoiner.add(it.toString()));

        return String.format(Messages.error_notInEnum, value.asText(), enumStringJoiner.toString());
    }
}
