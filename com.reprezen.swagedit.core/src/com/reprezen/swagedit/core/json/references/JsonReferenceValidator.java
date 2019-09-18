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
import static com.reprezen.swagedit.core.validation.Messages.error_invalid_reference_type;
import static com.reprezen.swagedit.core.validation.Messages.error_missing_reference;
import static com.reprezen.swagedit.core.validation.Messages.warning_simple_reference;
import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
import static org.eclipse.core.resources.IMarker.SEVERITY_WARNING;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.validation.JsonSchemaValidator;
import com.reprezen.swagedit.core.validation.SwaggerError;
import com.reprezen.swagedit.core.validation.SwaggerErrorFactory;

/**
 * JSON Reference Validator
 */
public class JsonReferenceValidator {

    private final JsonReferenceCollector collector;
    private final JsonReferenceFactory referenceFactory;
    private final JsonSchemaValidator schemaValidator;

    private final SwaggerErrorFactory errorFactory = new SwaggerErrorFactory();

    public JsonReferenceValidator(JsonSchemaValidator validator, JsonReferenceFactory factory) {
        this.referenceFactory = factory;
        this.collector = new JsonReferenceCollector(factory);
        this.schemaValidator = validator;
    }

    protected JsonSchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    /**
     * Returns a collection containing all errors being invalid JSON references present in the Swagger document.
     * 
     * @param baseURI
     * @param document
     * @return collection of errors
     */
    public Collection<? extends SwaggerError> validate(URI baseURI, JsonDocument doc, Model model) {
        return doValidate(baseURI, doc, collector.collect(baseURI, model));
    }

    /*
     * Runs various validation on a set of references. The set of references is given to us by the reference collector
     * in the form of a Map. The keys being the references and the values are a list of nodes that are sources of the
     * reference.
     * 
     * Having a set of reference that does not contain duplicates allow us to reduce the time validation takes. If a
     * validation fails, then an error is added to each of the reference sources.
     */
    protected Collection<? extends SwaggerError> doValidate(URI baseURI, JsonDocument doc,
            Map<JsonReference, List<AbstractNode>> references) {

        Set<SwaggerError> errors = new HashSet<>();

        for (JsonReference reference : references.keySet()) {
            if (reference instanceof JsonReference.SimpleReference) {
                errors.addAll(createReferenceError(doc, SEVERITY_WARNING, warning_simple_reference,
                        references.get(reference)));
            } else if (reference.isInvalid()) {
                errors.addAll(
                        createReferenceError(doc, SEVERITY_ERROR, error_invalid_reference, references.get(reference)));
            } else if (reference.isMissing(doc, baseURI)) {
                errors.addAll(createReferenceError(doc, SEVERITY_WARNING, error_missing_reference,
                        references.get(reference)));
            } else if (reference.containsWarning()) {
                errors.addAll(createReferenceError(doc, SEVERITY_WARNING, error_invalid_reference,
                        references.get(reference)));
            } else {
                errors.addAll(validateType(doc, baseURI, reference, references.get(reference)));
            }
        }

        return errors;
    }

    /**
     * This method checks that referenced objects are of expected type as defined in the schema.
     * 
     * @param doc
     *            current document
     * @param baseURI
     *            document base URI
     * @param reference
     *            actual reference
     * @param sources
     *            list of node being source of the reference
     * @param schemas
     *            cache of JSON schemas
     * @param errors
     *            current set of errors
     */
    protected Set<SwaggerError> validateType(JsonDocument doc, URI baseURI, JsonReference reference,
            Collection<AbstractNode> sources) {

        Set<SwaggerError> errors = new HashSet<>();

        JsonNode target = findTarget(doc, baseURI, reference);
        // To avoid performing even more cycles, the sources are grouped by their type.
        // Validation is done only once for each sources having same type.

        Map<String, List<AbstractNode>> sourceTypes = groupSourcesByType(sources);

        for (String type : sourceTypes.keySet()) {
            Set<JsonNode> report = schemaValidator.validate(target, type);
            if (!report.isEmpty()) {
                errors.addAll(createReferenceError(doc, SEVERITY_WARNING, error_invalid_reference_type, sources));
            }
        }

        return errors;
    }

    /*
     * Groups all source nodes by their JSON type.
     */
    protected Map<String, List<AbstractNode>> groupSourcesByType(Collection<AbstractNode> sources) {
        Map<String, List<AbstractNode>> result = new HashMap<>();
        for (AbstractNode source : sources) {
            if (source.getType() != null && source.getType().getPointer() != null) {
                String type = source.getType().getPointer().toString();

                if (result.containsKey(type)) {
                    result.get(type).add(source);
                } else {
                    List<AbstractNode> values = new ArrayList<>();
                    values.add(source);

                    result.put(type, values);
                }
            }
        }

        return result;
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

    protected Set<SwaggerError> createReferenceError(JsonDocument document, int severity, String message,
            Collection<AbstractNode> sources) {
        Set<SwaggerError> errors = new HashSet<>();
        for (AbstractNode source : sources) {
            errors.add(createReferenceError(document, severity, message, source));
        }
        return errors;
    }

    protected SwaggerError createReferenceError(JsonDocument document, int severity, String message,
            AbstractNode source) {
        int line = 1;
        AbstractNode ref = null;
        if (source != null) {
            ref = referenceFactory.getReferenceValue(source);
            // if (ref != null) {
            // Location location = ref != null ? ref.getStart() : source.getStart();
            // line = location.getLine() + 1;
            // }
        }
        // new SwaggerError(line, severity, message);
        return errorFactory.fromMessage(document, ref != null ? ref : source, severity, message);
    }

}
