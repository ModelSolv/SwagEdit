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
package com.reprezen.swagedit.core.json.references;

import static com.reprezen.swagedit.core.validation.Messages.error_invalid_reference;
import static com.reprezen.swagedit.core.validation.Messages.error_missing_reference;
import static com.reprezen.swagedit.core.validation.Messages.warning_simple_reference;
import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
import static org.eclipse.core.resources.IMarker.SEVERITY_WARNING;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.Sets;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.validation.SwaggerError;

/**
 * JSON Reference Validator
 */
public class JsonReferenceValidator {

    private final JsonReferenceCollector collector;
    protected JsonSchemaFactory factory = null;

    public JsonReferenceValidator(JsonReferenceFactory factory) {
        this.collector = new JsonReferenceCollector(factory);
    }

    public void setFactory(JsonSchemaFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns a collection containing all errors being invalid JSON references present in the Swagger document.
     * 
     * @param baseURI
     * @param document
     * @return collection of errors
     */
    public Collection<? extends SwaggerError> validate(URI baseURI, JsonDocument doc) {
        return doValidate(baseURI, doc, collector.collect(baseURI, doc));
    }

    protected Collection<? extends SwaggerError> doValidate(URI baseURI, JsonDocument doc,
            Map<JsonNode, JsonReference> references) {

        Set<SwaggerError> errors = Sets.newHashSet();
        for (JsonNode node : references.keySet()) {
            JsonReference reference = references.get(node);

            if (reference instanceof JsonReference.SimpleReference) {
                errors.add(createReferenceError(SEVERITY_WARNING, warning_simple_reference, reference));
            } else if (reference.isInvalid()) {
                errors.add(createReferenceError(SEVERITY_ERROR, error_invalid_reference, reference));
            } else if (reference.isMissing(doc, baseURI)) {
                errors.add(createReferenceError(SEVERITY_WARNING, error_missing_reference, reference));
            } else if (reference.containsWarning()) {
                errors.add(createReferenceError(SEVERITY_WARNING, error_invalid_reference, reference));
            } else {
                validateType(doc, baseURI, node, reference, errors);
            }
        }
        return errors;
    }

    /**
     * This method checks that referenced objects are of expected type as defined in the schema.
     * 
     * @param doc
     *            current document
     * @param node
     *            node holding the reference
     * @param reference
     *            actual reference
     * @param errors
     *            current set of errors
     */
    protected void validateType(JsonDocument doc, URI baseURI, JsonNode node, JsonReference reference,
            Set<SwaggerError> errors) {

        JsonNode target = findTarget(doc, baseURI, reference);
        // TypeDefinition type = node.getType();
        //
        // ProcessingReport report;
        // if (factory != null) {
        // try {
        // JsonSchema jsonSchema = factory.getJsonSchema(doc.getSchema().asJson(), type.getPointer().toString());
        // report = jsonSchema.validate(target);
        //
        // if (!report.isSuccess()) {
        // errors.add(createReferenceError(SEVERITY_WARNING, error_invalid_reference_type, reference));
        // }
        // } catch (ProcessingException e) {
        // errors.add(createReferenceError(SEVERITY_WARNING, error_invalid_reference_type, reference));
        // }
        // }
    }

    protected JsonNode findTarget(JsonDocument doc, URI baseURI, JsonReference reference) {
        JsonNode valueNode = null;

        if (!reference.getUri().equals(baseURI)) {
            // Try to load the referenced node from an external document
            JsonNode externalDoc = reference.getDocument(doc, baseURI);

            if (externalDoc != null) {
                try {
                    valueNode = externalDoc.at(reference.getPointer());
                } catch (Exception e) {
                    // fail to parse the model or the pointer
                }
            }
        } else {
            valueNode = doc.asJson().at(reference.getPointer());
        }

        return valueNode;
    }

    protected SwaggerError createReferenceError(int severity, String message, JsonReference reference) {
        Object source = reference.getSource();
        int line;
        // if (source instanceof AbstractNode) {
        // line = ((AbstractNode) source).getStart().getLine() + 1;
        // } else {
            line = 1;
        // }

        return new SwaggerError(line, severity, message);
    }

}
