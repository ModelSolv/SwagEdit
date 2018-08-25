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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonPointer;

public class ObjectNode extends AbstractNode {

    private final Map<String, AbstractNode> elements = new LinkedHashMap<>();

    ObjectNode(Model model, AbstractNode parent, JsonPointer ptr) {
        super(model, parent, ptr);
    }

    @Override
    public AbstractNode get(int pos) {
        return elements()[pos];
    }

    @Override
    public AbstractNode get(String property) {
        return elements.get(property);
    }

    public AbstractNode put(String property, AbstractNode value) {
        this.elements.put(property, value);
        return this;
    }

    public Collection<String> fieldNames() {
        return elements.keySet();
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public ObjectNode asObject() {
        return this;
    }

    @Override
    public AbstractNode[] elements() {
        return elements.values().toArray(new AbstractNode[elements.size()]);
    }

    @Override
    public String getText() {
        return getProperty() == null ? "" : getProperty();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}