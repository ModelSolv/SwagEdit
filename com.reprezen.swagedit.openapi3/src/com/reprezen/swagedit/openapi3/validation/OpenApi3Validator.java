/*******************************************************************************
 *  Copyright (c) 2016 ModelSolv, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 *  Contributors:
 *     ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.openapi3.validation;

import static com.reprezen.swagedit.openapi3.preferences.OpenApi3PreferenceConstants.ADVANCED_VALIDATION;
import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.jsonoverlay.PositionInfo;
import com.reprezen.jsonoverlay.PositionInfo.PositionEndpoint;
import com.reprezen.kaizen.oasparser.OpenApi3Parser;
import com.reprezen.kaizen.oasparser.model3.OpenApi3;
import com.reprezen.kaizen.oasparser.val.ValidationResults.Severity;
import com.reprezen.kaizen.oasparser.val.ValidationResults.ValidationItem;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.ArrayNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.validation.JsonSchemaValidator;
import com.reprezen.swagedit.core.validation.Messages;
import com.reprezen.swagedit.core.validation.SwaggerError;
import com.reprezen.swagedit.core.validation.Validator;
import com.reprezen.swagedit.openapi3.Activator;
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema;

public class OpenApi3Validator extends Validator {

    public static class OpenApi3SchemaValidator extends JsonSchemaValidator {

        public OpenApi3SchemaValidator(JsonNode schema, Map<String, JsonNode> preloadSchemas) {
            super(schema, preloadSchemas);
        }
    }

    private final JsonPointer operationPointer = JsonPointer.compile("/definitions/operation");
    private final JsonPointer securityPointer = JsonPointer.compile("/components/securitySchemes");

    private final JsonNode schema;
    private final Map<String, JsonNode> preloadedSchemas;

    private JsonSchemaValidator schemaValidator;
    private JsonReferenceValidator referenceValidator;

    public OpenApi3Validator(Map<String, JsonNode> preloadedSchemas) {
        this(preloadedSchemas, null);
    }

    public OpenApi3Validator(Map<String, JsonNode> preloadedSchemas, IPreferenceStore preferenceStore) {
        super(preferenceStore);

        this.preloadedSchemas = preloadedSchemas;
        this.schema = preloadedSchemas.get(OpenApi3Schema.URL);
    }

    protected boolean isAdvancedValidation() {
        return getPreferenceStore() != null && getPreferenceStore().getBoolean(ADVANCED_VALIDATION);
    }

    @Override
    public JsonReferenceValidator getReferenceValidator() {
        if (referenceValidator == null) {
            referenceValidator = new OpenApi3ReferenceValidator(getSchemaValidator());
        }
        return referenceValidator;
    }

    @Override
    public JsonSchemaValidator getSchemaValidator() {
        if (schemaValidator == null) {
            schemaValidator = new OpenApi3SchemaValidator(schema, preloadedSchemas);
        }
        return schemaValidator;
    }

    @Override
    public Set<SwaggerError> validate(JsonDocument document, URI baseURI) {
        final Set<SwaggerError> errors = super.validate(document, baseURI);
        final long nbOfErrors = errors.stream().filter(e -> e.getLevel() == SEVERITY_ERROR).count();

        // Advanced validation is enable if no errors are detected (does not include
        // warnings and infos),
        // and option in UI is enable.
        if (isAdvancedValidation() && nbOfErrors == 0) {
            try {
                OpenApi3 result = new OpenApi3Parser().parse(document.get(), baseURI.toURL(), true);

                for (ValidationItem item : result.getValidationResults().getItems()) {
                    PositionInfo pos = item.getPositionInfo();
                    if (pos != null) {
                        PositionEndpoint start = pos.getStart();
                        PositionEndpoint end = pos.getEnd();

                        int offset = document.getLineOffset(start.getLine() - 1) + (start.getColumn() - 1);
                        int length = (document.getLineOffset(end.getLine() - 1) + end.getColumn() - 1) - offset;

                        errors.add(new SwaggerError(getSeverity(item.getSeverity()), offset, length, item.getMsg()));
                    } else {
                        errors.add(new SwaggerError(getSeverity(item.getSeverity()), 0, 0, item.getMsg()));
                    }
                }
            } catch (Exception e) {
                Activator.getDefault().getLog()
                        .log(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getLocalizedMessage()));
            }
        }

        return errors;
    }

    private int getSeverity(Severity severity) {
        switch (severity) {
        case ERROR:
            return IMarker.SEVERITY_ERROR;
        case WARNING:
            return IMarker.SEVERITY_WARNING;
        default:
            return IMarker.SEVERITY_INFO;
        }
    }

    @Override
    protected Set<SwaggerError> executeModelValidation(JsonDocument document, AbstractNode node) {
        Set<SwaggerError> errors = super.executeModelValidation(document, node);

        validateOperationIdReferences(document, node, errors);
        validateSecuritySchemeReferences(document, node, errors);
        validateParameters(document, node, errors);

        return errors;
    }

    private void validateSecuritySchemeReferences(JsonDocument doc, AbstractNode node, Set<SwaggerError> errors) {
        Model model = doc.getModel();

        if (node.getPointerString().matches(".*/security/\\d+")) {
            AbstractNode securitySchemes = model.find(securityPointer);

            if (node.isObject()) {
                for (String field : node.asObject().fieldNames()) {
                    AbstractNode securityScheme = securitySchemes != null ? securitySchemes.get(field) : null;

                    if (securityScheme == null) {
                        String message = Messages.error_invalid_security_scheme;

                        errors.add(error(doc, node.get(field), IMarker.SEVERITY_ERROR, message));
                    } else {
                        validateSecuritySchemeScopes(doc, node, field, securityScheme, errors);
                    }
                }
            }
        }
    }

    private List<String> oauthScopes = Arrays.asList("oauth2", "openIdConnect");

    private void validateSecuritySchemeScopes(JsonDocument document, AbstractNode node, String name,
            AbstractNode securityScheme, Set<SwaggerError> errors) {
        String type = getType(securityScheme);
        if (type == null) {
            return;
        }

        boolean shouldHaveScopes = oauthScopes.contains(type);
        List<String> scopes = getSecurityScopes(securityScheme);

        AbstractNode values = node.get(name);
        if (values.isArray()) {
            ArrayNode scopeValues = values.asArray();

            // The scope names MUST be empty for Security Scheme types other than 'oauth2' and 'openIdConnect'
            if (scopeValues.size() > 0 && !shouldHaveScopes) {
                String message = String.format(Messages.error_scope_should_be_empty, name, type, name);

                errors.add(error(document, node.get(name), IMarker.SEVERITY_ERROR, message));
            } else {
                if (type.equals("oauth2")) {
                    for (AbstractNode scope : scopeValues.elements()) {
                        try {
                            String scopeName = (String) scope.asValue().getValue();
                            if (!scopes.contains(scopeName)) {
                                String message = String.format(Messages.error_invalid_scope_reference, scopeName, name);

                                errors.add(error(document, scope, IMarker.SEVERITY_ERROR, message));
                            }
                        } catch (Exception e) {
                            // Invalid scope name type.
                            // No need to create an error, it will be handle by the schema validation.
                        }
                    }
                }
            }
        }
    }

    private String getType(AbstractNode securityScheme) {
        AbstractNode type = securityScheme.get("type");
        if (type == null) {
            return null;
        }

        return (String) type.asValue().getValue();
    }

    private List<String> getSecurityScopes(AbstractNode securityScheme) {
        List<String> scopes = new ArrayList<>();

        try {
            AbstractNode flows = securityScheme.get("flows");
            for (AbstractNode flow : flows.elements()) {
                AbstractNode values = flow.get("scopes");
                if (values != null && values.isObject()) {
                    scopes.addAll(values.asObject().fieldNames());
                }
            }
        } catch (Exception e) {
            // could be a NPE, let's just return the scopes we have so far.
        }
        return scopes;
    }

    protected void validateOperationIdReferences(JsonDocument document, AbstractNode node, Set<SwaggerError> errors) {
        Model model = document.getModel();
        JsonPointer schemaPointer = JsonPointer.compile("/definitions/link/properties/operationId");

        if (node != null && node.getType() != null && schemaPointer.equals(node.getType().getPointer())) {
            List<AbstractNode> nodes = model.findByType(operationPointer);
            Iterator<AbstractNode> it = nodes.iterator();

            boolean found = false;
            while (it.hasNext() && !found) {
                AbstractNode current = it.next();
                AbstractNode value = current.get("operationId");

                found = value != null && Objects.equals(node.asValue().getValue(), value.asValue().getValue());
            }

            if (!found) {
                errors.add(error(document, node, IMarker.SEVERITY_ERROR, Messages.error_invalid_operation_id));
            }
        }
    }

    protected void validateParameters(JsonDocument document, AbstractNode node, Set<SwaggerError> errors) {
        final JsonPointer pointer = JsonPointer.compile("/definitions/parameterOrReference");

        if (node != null && node.getType() != null && pointer.equals(node.getType().getPointer())) {
            // validation parameter location value
            if (node.isObject() && node.asObject().get("in") != null) {
                AbstractNode valueNode = node.asObject().get("in");
                try {
                    Object value = valueNode.asValue().getValue();

                    if (!Arrays.asList("query", "header", "path", "cookie").contains(value)) {
                        errors.add(error(document, valueNode, IMarker.SEVERITY_ERROR,
                                Messages.error_invalid_parameter_location));
                    }
                } catch (Exception e) {
                    errors.add(error(document, valueNode, IMarker.SEVERITY_ERROR,
                            Messages.error_invalid_parameter_location));
                }
            }
        }
    }
}
