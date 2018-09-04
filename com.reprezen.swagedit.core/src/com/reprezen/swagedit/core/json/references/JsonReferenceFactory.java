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

import static com.reprezen.swagedit.core.json.references.JsonReference.PROPERTY;

import java.net.URI;

import org.yaml.snakeyaml.nodes.ScalarNode;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.utils.StringUtils;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.model.ValueNode;
import com.reprezen.swagedit.core.utils.URLUtils;

/**
 * JSON Reference Factory
 * 
 * This class should be used to instantiate JSONReferences.
 * 
 */
public class JsonReferenceFactory {

    public JsonReference create(AbstractNode node) {
        if (node == null) {
            return new JsonReference(null, null, false, false, false, node);
        }

        ValueNode value = getReferenceValue(node);
        if (value != null) {
            return doCreate((String) value.getValue(), value);
        } else {
            return null;
        }
    }

    public JsonReference create(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return new JsonReference(null, null, false, false, false, node);
        }

        return doCreate(getReferenceValue(node), node);
    }

    public JsonReference create(ScalarNode node) {
        if (node == null) {
            return new JsonReference(null, null, false, false, false, node);
        }

        return doCreate(node.getValue(), node);
    }

    /**
     * Returns a simple reference if the value node points to a definition inside the same document.
     * 
     * @param baseURI
     * @param value
     * @return reference
     */
    public JsonReference createSimpleReference(URI baseURI, AbstractNode valueNode) {
        if (valueNode == null || valueNode.isArray() || valueNode.isObject()) {
            return null;
        }

        final Object value = valueNode.asValue().getValue();
        if (!(value instanceof String)) {
            return null;
        }

        String stringValue = (String) value;
        if (StringUtils.emptyToNull(stringValue) == null || stringValue.startsWith("#") || stringValue.contains("/")) {
            return null;
        }

        final Model model = valueNode.getModel();
        if (model != null) {
            JsonPointer ptr = JsonPointer.compile("/definitions/" + value);
            AbstractNode target = model.find(ptr);
            if (target != null) {
                return new JsonReference.SimpleReference(baseURI, ptr, valueNode);
            }
        }

        return null;
    }

    public JsonReference doCreate(String value, Object source) {
        String notNull = StringUtils.nullToEmpty(value);

        URI uri;
        try {
            uri = URI.create(notNull);
        } catch (NullPointerException | IllegalArgumentException e) {
            // try to encode illegal characters, e.g. curly braces
            try {
                uri = URI.create(URLUtils.encodeURL(notNull));
            } catch (NullPointerException | IllegalArgumentException e2) {
                return new JsonReference(null, null, false, false, false, source);
            }
        }

        String fragment = uri.getFragment();
        JsonPointer pointer = null;
        try {
            // Pointer fails to resolve if ends with /
            if (fragment != null && fragment.length() > 1 && fragment.endsWith("/")) {
                fragment = fragment.substring(0, fragment.length() - 1);
            }

            pointer = JsonPointer.compile(StringUtils.emptyToNull(fragment));
        } catch (IllegalArgumentException e) {
            // let the pointer be null
        }

        uri = uri.normalize();
        boolean absolute = uri.isAbsolute();
        boolean local = !absolute && uri.getPath().isEmpty();
        // should warn when using curly braces
        boolean warnings = notNull.contains("{") || uri.toString().contains("}");

        return new JsonReference(uri, pointer, absolute, local, warnings, source);
    }

    protected Boolean isReference(AbstractNode node) {
        return JsonReference.isReference(node);
    }

    protected ValueNode getReferenceValue(AbstractNode node) {
        if (node.isValue()) {
            return node.asValue();
        }
        AbstractNode value = node.get(PROPERTY);
        if (value != null && value.isValue()) {
            return value.asValue();
        }
        return null;
    }

    protected String getReferenceValue(JsonNode node) {
        return node.isTextual() ? node.asText() : node.get(PROPERTY).asText();
    }
}
