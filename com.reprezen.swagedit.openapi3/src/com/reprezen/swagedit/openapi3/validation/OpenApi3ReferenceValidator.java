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
package com.reprezen.swagedit.openapi3.validation;

import static com.reprezen.swagedit.core.validation.Messages.error_invalid_operation_ref;
import static com.reprezen.swagedit.core.validation.Messages.error_invalid_reference_type;
import static org.eclipse.core.resources.IMarker.SEVERITY_WARNING;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.json.references.JsonReference;
import com.reprezen.swagedit.core.json.references.JsonReferenceFactory;
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.ValueNode;
import com.reprezen.swagedit.core.validation.JsonSchemaValidator;
import com.reprezen.swagedit.core.validation.SwaggerError;

public class OpenApi3ReferenceValidator extends JsonReferenceValidator {

    private final String linkTypePointer = "/definitions/linkOrReference";
    private final String operationTypePointer = "/definitions/operation";

    public OpenApi3ReferenceValidator(JsonSchemaValidator schemaValidator) {
        this(schemaValidator, new OpenApi3ReferenceFactory());
    }

    public OpenApi3ReferenceValidator(JsonSchemaValidator schemaValidator, OpenApi3ReferenceFactory factory) {
        super(schemaValidator, factory);
    }

    @Override
    protected Set<SwaggerError> validateType(JsonDocument doc, URI baseURI, JsonReference reference,
            Collection<AbstractNode> sources) {

        Set<SwaggerError> errors = new HashSet<>();
        Map<String, List<AbstractNode>> sourceTypes = groupSourcesByType(sources);
        JsonNode target = findTarget(doc, baseURI, reference);

        for (String type : sourceTypes.keySet()) {
            boolean isOperationValidation = linkTypePointer.equals(type);

            String ptr = isOperationValidation ? operationTypePointer.toString() : type;
            String message = isOperationValidation ? error_invalid_operation_ref : error_invalid_reference_type;

            Set<JsonNode> report = getSchemaValidator().validate(target, ptr);
            if (!report.isEmpty()) {
                errors.addAll(createReferenceError(doc, SEVERITY_WARNING, message, sources));
            }
        }

        return errors;
    }

    public static class OpenApi3ReferenceFactory extends JsonReferenceFactory {

        private static final String OPERATION_REF = "operationRef";

        @Override
        protected Boolean isReference(AbstractNode node) {
            return super.isReference(node) || node.get(OPERATION_REF) != null;
        }

        @Override
        protected ValueNode getReferenceValue(AbstractNode node) {
            ValueNode valueNode = super.getReferenceValue(node);
            if (valueNode == null) {
                AbstractNode other = node.get(OPERATION_REF);
                if (other != null && other.isValue()) {
                    valueNode = other.asValue();
                }
            }
            return valueNode;
        }

    }
}