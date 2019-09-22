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

import java.util.Objects;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;

import com.fasterxml.jackson.core.JsonPointer;
import com.reprezen.swagedit.core.schema.TypeDefinition;

/**
 * Represents a Node inside a YAML/JSON document.
 * 
 * <br/>
 * 
 * Nodes can be either Values, Objects or Arrays. They can contain other elements or simply a value. They contain also
 * information about their location inside a document. A node can be given a type from a JSON schema.
 *
 */
public abstract class AbstractNode {

    private final Model model;
    private final JsonPointer pointer;
    private final AbstractNode parent;

    private String property;
    private TypeDefinition type;
    private Location start;
    private Location end;

    AbstractNode(Model model, AbstractNode parent, JsonPointer ptr) {
        this.model = model;
        this.parent = parent;
        this.pointer = ptr;
    }

    public Model getModel() {
        return model;
    }

    /**
     * Returns the child node that is contained at the given index.
     * 
     * @param pos
     * @return node
     */
    public AbstractNode get(int index) {
        return null;
    }

    /**
     * Returns the child node that is contained by the given property.
     * 
     * @param property
     * @return node
     */
    public AbstractNode get(String property) {
        return null;
    }

    /**
     * Returns true if the node is an object.
     * 
     * @return true if object
     */
    public boolean isObject() {
        return false;
    }

    /**
     * Returns true if the node is an array.
     * 
     * @return true if array
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Returns true if the node is a value
     * 
     * @return true if value
     */
    public boolean isValue() {
        return false;
    }

    /**
     * Returns the current node as an {@link ObjectNode} if it is an instance, or null otherwise.
     * 
     * @return node
     */
    public ObjectNode asObject() {
        return null;
    }

    /**
     * Returns the current node as an {@link ArrayNode} if it is an instance, or null otherwise.
     * 
     * @return node
     */
    public ArrayNode asArray() {
        return null;
    }

    /**
     * Returns the current node as an {@link ValueNode} if it is an instance, or null otherwise.
     * 
     * @return node
     */
    public ValueNode asValue() {
        return null;
    }

    /**
     * Returns the JSON pointer that identifies this node.
     * 
     * @return JSON pointer
     */
    public JsonPointer getPointer() {
        return pointer;
    }
    
    /**
     * 
     * @return JSON pointer as a string
     */
    public String getPointerString() {
        return getPointer().toString();
    }

    public void setType(TypeDefinition type) {
        this.type = type;
    }

    public TypeDefinition getType() {
        return type;
    }

    /**
     * Returns the parent node that contains this node, or null if the node is the root node.
     * 
     * @return parent node
     */
    public AbstractNode getParent() {
        return parent;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String name) {
        this.property = name;
    }

    /**
     * Returns the children elements of this node.
     * 
     * @return node's children
     */
    public AbstractNode[] elements() {
        return new AbstractNode[0];
    }

    /**
     * Returns the number of elements contained by this node.
     * 
     * @return size of children
     */
    public int size() {
        return elements().length;
    }

    public abstract String getText();

    /**
     * Returns the position of the node inside the given document. <br/>
     * The position matches the area that contains all the node's content.
     * 
     * @param document
     * @return position inside the document
     */
    public Position getPosition(IDocument document) {
        return getPosition(document, false);
    }
    
    /**
     * Returns the position of the node inside the given document. <br/>
     * The position matches the area that contains all the node's content.
     * 
     * @param document
     * @param selectEntireElement - the entire element will be selected if true, only the first line otherwise
     * @return position inside the document
     */
    public Position getPosition(IDocument document, boolean selectEntireElement) {
        int startLine = getStart().getLine();
        int offset = 0;
        int length = 0;

        int endLine = getEnd().getLine();
        int endCol = getEnd().getColumn();
        try {
            offset = document.getLineOffset(startLine);
            if (selectEntireElement) {
                length = (document.getLineOffset(endLine) + endCol) - offset;
            } else if (startLine < document.getNumberOfLines() - 1) {
                length = document.getLineOffset(startLine+1) - offset;
            } else {
                length = document.getLineLength(startLine);
            }
        } catch (BadLocationException e) {
            return new Position(0);
        }

        return new Position(Math.max(0, offset), length);
    }

    public void setStartLocation(Location start) {
        this.start = start;
    }

    public void setEndLocation(Location location) {
        this.end = location;
    }

    /**
     * Returns the start location of this node.
     * 
     * @return start location
     */
    public Location getStart() {
        return start;
    }

    /**
     * Returns the end location of this node.
     * 
     * @return end location
     */
    public Location getEnd() {
        return end;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((pointer == null) ? 0 : pointer.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractNode other = (AbstractNode) obj;

        return Objects.equals(getModel(), other.getModel()) && Objects.equals(getPointer(), other.getPointer());
    }

}
