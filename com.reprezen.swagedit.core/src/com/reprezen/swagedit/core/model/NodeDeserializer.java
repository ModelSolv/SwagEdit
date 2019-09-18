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
package com.reprezen.swagedit.core.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class NodeDeserializer extends JsonDeserializer<AbstractNode> {

    public static final String ATTRIBUTE_MODEL = "model";
    public static final String ATTRIBUTE_PARENT = "parent";
    public static final String ATTRIBUTE_POINTER = "pointer";

    @Override
    public AbstractNode deserialize(JsonParser p, DeserializationContext context)
            throws IOException, JsonProcessingException {

        JsonLocation startLocation = p.getTokenLocation();
        if (p.getCurrentToken() == JsonToken.FIELD_NAME) {
            p.nextToken();
        }

        switch (p.getCurrentToken()) {
        case START_OBJECT:
            return deserializeObjectNode(p, context, startLocation);
        case START_ARRAY:
            return deserializeArrayNode(p, context, startLocation);
        default:
            return deserializeValueNode(p, context, startLocation);
        }
    }

    protected ObjectNode deserializeObjectNode(JsonParser p, DeserializationContext context, JsonLocation startLocation)
            throws IllegalArgumentException, IOException {

        final Model model = (Model) context.getAttribute(ATTRIBUTE_MODEL);
        final AbstractNode parent = (AbstractNode) context.getAttribute(ATTRIBUTE_PARENT);
        final JsonPointer ptr = (JsonPointer) context.getAttribute(ATTRIBUTE_POINTER);

        final ObjectNode node = model.objectNode(parent, ptr);
        node.setStartLocation(createLocation(startLocation));

        JsonLocation last = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String name = p.getCurrentName();

            JsonPointer pp = JsonPointer.compile(ptr.toString() + "/" + name.replaceAll("/", "~1"));
            context.setAttribute(ATTRIBUTE_PARENT, node);
            context.setAttribute(ATTRIBUTE_POINTER, pp);

            AbstractNode v = deserialize(p, context);
            v.setProperty(name);
            node.put(name, v);
            last = p.getTokenLocation();
        }

        node.setEndLocation(createLocation(last != null ? last : p.getCurrentLocation()));
        return node;
    }

    protected ArrayNode deserializeArrayNode(JsonParser p, DeserializationContext context, JsonLocation startLocation)
            throws IOException {
        final Model model = (Model) context.getAttribute(ATTRIBUTE_MODEL);
        final AbstractNode parent = (AbstractNode) context.getAttribute(ATTRIBUTE_PARENT);
        final JsonPointer ptr = (JsonPointer) context.getAttribute(ATTRIBUTE_POINTER);

        ArrayNode node = model.arrayNode(parent, ptr);

        int i = 0;
        while (p.nextToken() != JsonToken.END_ARRAY) {
            JsonPointer pp = JsonPointer.compile(ptr.toString() + "/" + i);

            context.setAttribute(ATTRIBUTE_PARENT, node);
            context.setAttribute(ATTRIBUTE_POINTER, pp);

            AbstractNode v = deserialize(p, context);

            node.add(v);
            i++;
        }

        node.setStartLocation(createLocation(startLocation));
        node.setEndLocation(createLocation(p.getCurrentLocation()));
        return node;
    }

    protected ValueNode deserializeValueNode(JsonParser p, DeserializationContext context, JsonLocation startLocation)
            throws IOException {
        final Model model = (Model) context.getAttribute(ATTRIBUTE_MODEL);
        final AbstractNode parent = (AbstractNode) context.getAttribute(ATTRIBUTE_PARENT);
        final JsonPointer ptr = (JsonPointer) context.getAttribute(ATTRIBUTE_POINTER);

        Object v = context.readValue(p, Object.class);

        ValueNode node = model.valueNode(parent, ptr, v);
        node.setStartLocation(createLocation(startLocation));
        node.setEndLocation(createLocation(p.getCurrentLocation()));

        return node;
    }

    private Location createLocation(JsonLocation json) {
        return new Location(json.getLineNr() - 1, json.getColumnNr() - 1);
    }
}
