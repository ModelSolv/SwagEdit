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

import java.net.URI;

import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.utils.StringUtils;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.ObjectNode;
import com.reprezen.swagedit.core.model.ValueNode;

/**
 * Represents a JSON reference as defined by https://tools.ietf.org/html/draft-pbryan-zyp-json-ref-03.
 * 
 * A JSON reference is made of a valid URI and of a JSON Pointer (https://tools.ietf.org/html/rfc6901).
 * 
 */
public class JsonReference {

    public static final String PROPERTY = "$ref";

    /**
     * Represents an unqualified reference, this class is used to support deprecated references.
     */
    public static class SimpleReference extends JsonReference {

        SimpleReference(URI baseURI, JsonPointer ptr, Object source) {
            super(URI.create(baseURI + "#" + ptr.toString()), ptr, false, true, false, source);
        }

    }

    private final URI uri;
    private final JsonPointer pointer;
    private final boolean absolute;
    private final boolean local;
    private final Object source;
    private final boolean containsWarning;

    private JsonNode resolved;
    private JsonDocumentManager manager = JsonDocumentManager.getInstance();

    JsonReference(URI uri, JsonPointer pointer, boolean absolute, boolean local, boolean containsWarning,
            Object source) {
        this.uri = uri;
        this.pointer = pointer;
        this.absolute = absolute;
        this.local = local;
        this.source = source;
        this.containsWarning = containsWarning;
    }

    public void setDocumentManager(JsonDocumentManager manager) {
        this.manager = manager;
    }

    /**
     * Returns true if the reference was constructed from an invalid string, e.g. it contains invalid characters.
     * 
     * @return true if is invalid URI.
     */
    public boolean isInvalid() {
        return uri == null || pointer == null;
    }

    /**
     * Returns true if the reference cannot be resolved and the pointer points to an inexistent element.
     * 
     * @param document
     * @param baseURI
     * @return true if the reference can be resolved.
     */
    public boolean isMissing(JsonDocument document, URI baseURI) {
        if (isInvalid()) {
            return false;
        }

        JsonNode resolved = resolve(document, baseURI);
        return resolved == null || resolved.isMissingNode();
    }

    /**
     * Returns the node that is referenced by this reference.
     * 
     * If the resolution of the referenced node fails, this method returns null. If the pointer does not points to an
     * existing node, this method will return a missing node (see JsonNode.isMissingNode()).
     * 
     * @param document
     * @param baseURI
     * @return referenced node
     */
    public JsonNode resolve(JsonDocument document, URI baseURI) {
        if (resolved == null) {
            JsonNode doc = getDocument(document, baseURI);
            if (doc != null) {
                try {
                    resolved = doc.at(pointer);
                } catch (Exception e) {
                    resolved = null;
                }
            }
        }

        return resolved;
    }

    protected URI resolveURI(URI baseURI) {
        if (baseURI == null || absolute) {
            return getUri();
        } else {
            try {
                return baseURI.resolve(getUri());
            } catch (NullPointerException e) {
                return null;
            }
        }
    }

    public JsonPointer getPointer() {
        return pointer;
    }

    public URI getUri() {
        return uri;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public boolean containsWarning() {
        return containsWarning;
    }

    public Object getSource() {
        return source;
    }

    /**
     * Returns true if the argument can be identified as a JSON reference node.
     * 
     * A node is considered a reference if it is an object node and has a field named $ref having a textual value.
     * 
     * @param node
     * @return true if a reference node
     */
    public static boolean isReference(JsonNode value) {
        return value != null && value.isObject() && value.has(PROPERTY) && value.get(PROPERTY).isTextual();
    }

    public static boolean isReference(AbstractNode value) {
        return value != null && value.isObject() && value.get(PROPERTY) != null;
    }

    public static JsonPointer getPointer(JsonNode node) {
        JsonNode value = node.get(PROPERTY);

        if (value != null) {
            return createPointer(value.asText());
        } else {
            return createPointer(null);
        }
    }

    private static JsonPointer createPointer(String text) {
        if (StringUtils.emptyToNull(text) == null) {
            return JsonPointer.compile("");
        }

        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        return JsonPointer.compile(text);
    }

    /**
     * Returns true if the argument can be identified as a JSON reference node.
     * 
     * @param tuple
     * @return true if a reference node
     */
    public static boolean isReference(NodeTuple tuple) {
        if (tuple.getKeyNode().getNodeId() == NodeId.scalar) {
            String value = ((ScalarNode) tuple.getKeyNode()).getValue();

            return JsonReference.PROPERTY.equals(value) && tuple.getValueNode().getNodeId() == NodeId.scalar;
        }
        return false;
    }

    public static JsonPointer getPointer(ObjectNode node) {
        ValueNode value = node.get(PROPERTY).asValue();

        if (value != null) {
            return createPointer((String) value.getValue());
        } else {
            return createPointer(null);
        }
    }

    /**
     * Returns the JSON document that contains the node referenced by this reference.
     * 
     * @param document
     * @param baseURI
     * @return referenced node
     */
    public JsonNode getDocument(JsonDocument document, URI baseURI) {
        if (isLocal()) {
            return document.asJson();
        } else {
            return manager.getDocument(resolveURI(baseURI));
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pointer == null) ? 0 : pointer.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JsonReference)) {
            return false;
        }
        JsonReference other = (JsonReference) obj;
        if (pointer == null) {
            if (other.pointer != null) {
                return false;
            }
        } else if (!pointer.equals(other.pointer)) {
            return false;
        }
        if (uri == null) {
            if (other.uri != null) {
                return false;
            }
        } else if (!uri.equals(other.uri)) {
            return false;
        }
        return true;
    }

}
