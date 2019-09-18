/*******************************************************************************
 * Copyright (c) 2016 ModelSolv, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.core.validation;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Stream.of;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.dadacoalition.yedit.YEditLog;
import org.eclipse.core.resources.IMarker;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfigurationBuilder;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.reprezen.swagedit.core.Activator;
import com.reprezen.swagedit.core.editor.JsonDocument;

public class JsonSchemaValidator {

    private final LoadingConfiguration loadingConfiguration;
    private final JsonSchemaFactory factory;
    private final SwaggerErrorFactory errorFactory = new SwaggerErrorFactory();
    private final JsonNode schema;

    public JsonSchemaValidator(JsonNode schema, Map<String, JsonNode> preloadSchemas) {
        this.schema = schema;
        this.loadingConfiguration = getLoadingConfiguration(preloadSchemas);
        this.factory = JsonSchemaFactory.newBuilder() //
                .setLoadingConfiguration(loadingConfiguration) //
                .freeze();
    }

    private LoadingConfiguration getLoadingConfiguration(Map<String, JsonNode> preloadSchemas) {
        LoadingConfigurationBuilder loadingConfigurationBuilder = LoadingConfiguration.newBuilder();
        for (String nextSchemaUri : preloadSchemas.keySet()) {
            loadingConfigurationBuilder.preloadSchema(nextSchemaUri, preloadSchemas.get(nextSchemaUri));
        }
        return loadingConfigurationBuilder.freeze();
    }

    private Stream<JsonNode> asStream(Iterator<? extends JsonNode> it) {
        return StreamSupport.stream(spliteratorUnknownSize(it, ORDERED), false);
    }

    private Stream<JsonNode> asReportStream(Iterator<? extends ProcessingMessage> it) {
        return StreamSupport.stream(spliteratorUnknownSize(it, ORDERED), false).map(e -> e.asJson());
    }

    private final Function<JsonNode, Stream<JsonNode>> flattenReports() {
        return message -> {
            if (message.has("nrSchemas") && message.get("nrSchemas").asInt() > 1) {
                return asStream(message.get("reports").elements()).flatMap(flattenReports());
            } else {
                return message.isArray() ? asStream(message.elements()).flatMap(flattenReports()) : of(message);
            }
        };
    }

    public Set<SwaggerError> validate(JsonDocument document) {
        final Set<SwaggerError> errors = new HashSet<>();

        JsonSchema jsonSchema = null;
        try {
            jsonSchema = factory.getJsonSchema(schema);
        } catch (ProcessingException e) {
            YEditLog.logException(e);
            return errors;
        }

        try {
            ProcessingReport report = jsonSchema.validate(document.asJson(), true);

            errors.addAll(asReportStream(report.iterator()) //
                    .flatMap(flattenReports()) //
                    .map(m -> errorFactory.fromSchemaReport(document, m)) //
                    .collect(Collectors.toList()));

        } catch (ProcessingException e) {
            errors.add(new SwaggerError(IMarker.SEVERITY_ERROR, 0, 0,
                    errorFactory.getMessageProcessor().rewriteMessage(e.getProcessingMessage().asJson())));
        }

        return errors;
    }

    public Set<JsonNode> validate(JsonNode instance) {
        JsonSchema jsonSchema = null;
        try {
            jsonSchema = factory.getJsonSchema(schema);
        } catch (ProcessingException e) {
            Activator.getDefault().logError(e.getLocalizedMessage(), e);
            return null;
        }

        return doValidate(jsonSchema, instance);
    }

    public Set<JsonNode> validate(JsonNode instance, String schemaPointer) {
        JsonSchema jsonSchema = null;
        try {
            jsonSchema = factory.getJsonSchema(schema, schemaPointer);
        } catch (ProcessingException e) {
            Activator.getDefault().logError(e.getLocalizedMessage(), e);
            return null;
        }

        return doValidate(jsonSchema, instance);
    }

    private Set<JsonNode> doValidate(JsonSchema schema, JsonNode instance) {
        Set<JsonNode> errors = new HashSet<>();
        try {
            schema.validate(instance, true).forEach(message -> {
                errors.add(message.asJson());
            });
        } catch (ProcessingException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }
        return errors;
    }

    public static int getLevel(JsonNode message) {
        if (message == null || !message.has("level")) {
            return IMarker.SEVERITY_INFO;
        }

        switch (message.get("level").asText()) {
        case "error":
        case "fatal":
            return IMarker.SEVERITY_ERROR;
        case "warning":
            return IMarker.SEVERITY_WARNING;
        default:
            return IMarker.SEVERITY_INFO;
        }
    }
}
