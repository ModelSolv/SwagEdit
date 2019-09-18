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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

import com.reprezen.swagedit.core.Activator;
import com.reprezen.swagedit.core.editor.JsonDocument;

public class SwaggerError {

    private final String message;
    private final Map<String, Object> markerAttributes = new HashMap<>();

    private int level = IMarker.SEVERITY_WARNING;
    private int offset = 0;
    private int length = 0;

    public SwaggerError(int level, int offet, int length, String message, Map<String, Object> markerAttributes) {
        this.level = level;
        this.offset = offet;
        this.length = length;
        this.message = message;
        this.markerAttributes.putAll(markerAttributes != null ? markerAttributes : Collections.emptyMap());
    }

    public SwaggerError(int level, int offset, int length, String message) {
        this(level, offset, length, message, Collections.emptyMap());
    }

    public String getMessage() {
        return message;
    }

    public int getLevel() {
        return level;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public Map<String, Object> getMarkerAttributes() {
        return markerAttributes;
    }

    @Override
    public String toString() {
        return getMessage();
    }

    public IMarker asMarker(JsonDocument document, IMarker marker) {
        try {
            marker.setAttribute(IMarker.SEVERITY, getLevel());
            marker.setAttribute(IMarker.MESSAGE, getMessage());
            marker.setAttribute(IMarker.CHAR_START, getOffset());
            marker.setAttribute(IMarker.CHAR_END, getOffset() + getLength());
            marker.setAttribute(IMarker.LINE_NUMBER, document.getLineOfOffset(getOffset()));
        } catch (CoreException | BadLocationException e) {
            Activator.getDefault().logError(e.getMessage(), e);
        }

        return marker;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, offset, length, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SwaggerError other = (SwaggerError) obj;
        return level == other.level && offset == other.offset && length == other.length
                && Objects.equals(message, other.message);
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
