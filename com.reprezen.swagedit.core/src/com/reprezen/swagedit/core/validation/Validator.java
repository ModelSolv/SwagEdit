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

import static org.eclipse.core.resources.IMarker.SEVERITY_ERROR;
import static org.eclipse.core.resources.IMarker.SEVERITY_WARNING;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IFileEditorInput;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.parser.ParserException;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.json.references.JsonReference;
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.ArrayNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.model.ObjectNode;
import com.reprezen.swagedit.core.model.ValueNode;
import com.reprezen.swagedit.core.providers.ValidationProvider;
import com.reprezen.swagedit.core.utils.ExtensionUtils;

/**
 * This class contains methods for validating a Swagger YAML document.
 * 
 * Validation is done against the Swagger JSON Schema.
 * 
 * @see SwaggerError
 */
public abstract class Validator {

    private final JsonNode schemaRefTemplate = new ObjectMapper().createObjectNode() //
            .put("$ref", "#/definitions/schema");

    private final Set<ValidationProvider> providers;
    private final IPreferenceStore preferenceStore;
    private final SwaggerErrorFactory errorFactory = new SwaggerErrorFactory();

    public Validator(IPreferenceStore preferenceStore) {
        this.preferenceStore = preferenceStore;
        this.providers = ExtensionUtils.getValidationProviders();
    }

    public abstract JsonSchemaValidator getSchemaValidator();

    public abstract JsonReferenceValidator getReferenceValidator();

    protected IPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    protected SwaggerErrorFactory getErrorFactory() {
        return errorFactory;
    }

    /**
     * Returns a list or errors if validation fails.
     * 
     * This method accepts as input a swagger YAML document and validates it against the swagger JSON Schema.
     * 
     * @param content
     * @param editorInput
     *            current input
     * @return list or errors
     * @throws IOException
     * @throws ParserException
     */
    public Set<SwaggerError> validate(JsonDocument document, IFileEditorInput editorInput) {
        URI baseURI = editorInput != null ? editorInput.getFile().getLocationURI() : null;
        return validate(document, baseURI);
    }

    public Set<SwaggerError> validate(JsonDocument document, URI baseURI) {
        Set<SwaggerError> errors = new HashSet<>();

        JsonNode jsonContent = null;
        try {
            jsonContent = document.asJson();
        } catch (Exception e) {
            jsonContent = null;
        }

        if (jsonContent != null) {
            Node yaml = document.getYaml();
            Model model = document.getModel();
            if (yaml != null && model != null) {
                errors.addAll(getSchemaValidator().validate(document));
                errors.addAll(validateDocument(baseURI, document));
                errors.addAll(checkDuplicateKeys(document));
                errors.addAll(getReferenceValidator().validate(baseURI, document, model));
            }
        }

        return errors;
    }

    /**
     * Validates the model against with different rules that cannot be verified only by JSON schema validation.
     * 
     * @param baseURI
     * @param model
     * @return errors
     */
    protected Set<SwaggerError> validateDocument(URI baseURI, JsonDocument document) {
        final Set<SwaggerError> errors = new HashSet<>();
        final Model model = document.getModel();

        if (model != null && model.getRoot() != null) {
            for (AbstractNode node : model.allNodes()) {
                errors.addAll(executeModelValidation(document, node));
                // execute validation from each providers
                providers.forEach(provider -> {

                    if (provider.isActive(document)) {
                        Set<SwaggerError> result = provider.validate(document, baseURI, node);
                        if (result != null) {
                            errors.addAll(result);
                        }
                    }
                });
            }
        }
        return errors;
    }

    protected Set<SwaggerError> executeModelValidation(JsonDocument document, AbstractNode node) {
        Set<SwaggerError> errors = new HashSet<>();

        checkArrayTypeDefinition(document, node).ifPresent(errors::add);
        errors.addAll(checkObjectTypeDefinition(document, node));

        return errors;
    }

    /**
     * This method checks that the node if an array type definitions includes an items field.
     * 
     * @param errors
     * @param model
     */
    protected Optional<SwaggerError> checkArrayTypeDefinition(JsonDocument document, AbstractNode node) {
        if (hasArrayType(node)) {
            AbstractNode items = node.get("items");
            if (items == null) {
                return Optional.of(error(document, node, SEVERITY_ERROR, Messages.error_array_missing_items));
            } else {
                if (!items.isObject()) {
                    return Optional
                            .of(error(document, items, SEVERITY_ERROR, Messages.error_array_items_should_be_object));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Returns true if the node is an array type definition
     * 
     * @param node
     * @return true if array definition
     */
    protected boolean hasArrayType(AbstractNode node) {
        if (node.isObject() && node.get("type") instanceof ValueNode) {
            ValueNode typeValue = node.get("type").asValue();
            return typeValue.getValue() != null && "array".equalsIgnoreCase(typeValue.getValue().toString());
        }
        return false;
    }

    /**
     * Validates an object type definition.
     * 
     * @param errors
     * @param node
     */
    protected Set<SwaggerError> checkObjectTypeDefinition(JsonDocument document, AbstractNode node) {
        Set<SwaggerError> errors = new HashSet<>();

        if (node instanceof ObjectNode) {
            JsonPointer ptr = node.getPointer();
            if (ptr != null && ValidationUtil.isInDefinition(ptr.toString())) {
                checkMissingType(document, node).ifPresent(errors::add);
                errors.addAll(checkMissingRequiredProperties(document, node));
            }
        }

        return errors;
    }

    /**
     * This method checks that the node if an object definition includes a type field.
     * 
     * @param errors
     * @param node
     */
    protected Optional<SwaggerError> checkMissingType(JsonDocument document, AbstractNode node) {
        // object
        if (node.get("properties") != null) {
            // bypass this node, it is a property whose name is `properties`

            if ("properties".equals(node.getProperty())) {
                return Optional.empty();
            }

            if (node.get("type") == null) {
                return Optional.of(error(document, node, SEVERITY_WARNING, Messages.error_object_type_missing));
            } else {
                AbstractNode typeValue = node.get("type");
                if (!(typeValue instanceof ValueNode) || !Objects.equals("object", typeValue.asValue().getValue())) {
                    return Optional.of(error(document, node, SEVERITY_ERROR, Messages.error_wrong_type));
                }
            }
        } else if (isSchemaDefinition(node) && node.get("type") == null) {
            return Optional.of(error(document, node, SEVERITY_WARNING, Messages.error_type_missing));
        }

        return Optional.empty();
    }

    private boolean isSchemaDefinition(AbstractNode node) {
        // need to use getContent() because asJson() returns resolvedValue is some
        // subclasses
        return schemaRefTemplate.equals(node.getType().getContent()) //
                && node.get(JsonReference.PROPERTY) == null //
                && node.get("allOf") == null;
    }

    /**
     * This method checks that the required values for the object type definition contains only valid properties.
     * 
     * @param errors
     * @param node
     */
    protected Set<SwaggerError> checkMissingRequiredProperties(JsonDocument document, AbstractNode node) {
        Set<SwaggerError> errors = new HashSet<>();

        if (node.get("required") instanceof ArrayNode) {
            ArrayNode required = node.get("required").asArray();

            AbstractNode properties = node.get("properties");
            if (properties == null) {
                errors.add(error(document, node, SEVERITY_WARNING, Messages.warning_missing_properties));
            } else {
                for (AbstractNode prop : required.elements()) {
                    if (prop instanceof ValueNode) {
                        ValueNode valueNode = prop.asValue();
                        String value = valueNode.getValue().toString();

                        if (properties.get(value) == null) {
                            errors.add(error(document, valueNode, SEVERITY_WARNING,
                                    String.format(Messages.warning_required_properties, value)));
                        }
                    }
                }
            }
        }

        return errors;
    }

    protected SwaggerError error(JsonDocument document, AbstractNode node, int level, String message) {
        // return new SwaggerError(node.getStart().getLine() + 1, level, message);
        return errorFactory.fromMessage(document, node, level, message);
    }

    /*
     * Finds all duplicate keys in all objects present in the YAML document.
     */
    protected Set<SwaggerError> checkDuplicateKeys(JsonDocument document) {
        Node root = document.getYaml();
        Map<Pair<Node, String>, Set<Node>> acc = new HashMap<>();

        collectDuplicates(root, acc);

        Set<SwaggerError> errors = new HashSet<>();
        for (Pair<Node, String> key : acc.keySet()) {
            Set<Node> duplicates = acc.get(key);

            if (duplicates.size() > 1) {
                for (Node duplicate : duplicates) {
                    errors.add(createDuplicateError(document, key.getValue(), duplicate));
                }
            }
        }

        return errors;
    }

    /*
     * This method iterates through the YAML tree to collect the pairs of Node x String representing an object and one
     * of it's keys. Each pair is associated to a Set of Nodes which contains all nodes being a key to the pair's Node
     * and having for value the pair's key. Once the iteration is done, the resulting map should be traversed. Each pair
     * having more than one element in its associated Set are duplicate keys.
     */
    protected void collectDuplicates(Node parent, Map<Pair<Node, String>, Set<Node>> acc) {
        switch (parent.getNodeId()) {
        case mapping: {
            for (NodeTuple value : ((MappingNode) parent).getValue()) {
                Node keyNode = value.getKeyNode();

                if (keyNode.getNodeId() == NodeId.scalar) {
                    Pair<Node, String> key = Pair.of(parent, ((ScalarNode) keyNode).getValue());
                    acc.putIfAbsent(key, new HashSet<>());
                    acc.get(key).add(keyNode);
                }

                collectDuplicates(value.getValueNode(), acc);
            }
        }
            break;
        case sequence: {
            for (Node value : ((SequenceNode) parent).getValue()) {
                collectDuplicates(value, acc);
            }
        }
            break;
        default:
            break;
        }
    }

    protected SwaggerError createDuplicateError(JsonDocument document, String key, Node node) {
        return errorFactory.fromNode(document, node, SEVERITY_WARNING,
                String.format(Messages.error_duplicate_keys, key));
    }

}
