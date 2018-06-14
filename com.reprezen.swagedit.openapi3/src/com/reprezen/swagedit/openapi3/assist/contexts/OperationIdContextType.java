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
package com.reprezen.swagedit.openapi3.assist.contexts;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;

import com.fasterxml.jackson.core.JsonPointer;
import com.google.common.collect.Lists;
import com.reprezen.swagedit.core.assist.Proposal;
import com.reprezen.swagedit.core.assist.contexts.SchemaContextType;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.schema.CompositeSchema;

/**
 * ContextType that collects proposals from operations ids.
 */
public class OperationIdContextType extends SchemaContextType {

    private final JsonPointer operationPointer = JsonPointer.compile("/definitions/operation");

    public OperationIdContextType(CompositeSchema schema, String regex) {
        super(schema, "operationId", "operationId", regex, true);
    }

    @Override
    public Collection<Proposal> collectProposals(JsonDocument document, IPath path) {
        final Collection<Proposal> results = Lists.newArrayList();
        // final List<AbstractNode> nodes = model.findByType(operationPointer);
        //
        // for (AbstractNode node : nodes) {
        // AbstractNode value = node.get("operationId");
        // if (value != null && value.asValue().getValue() instanceof String) {
        // String key = (String) value.asValue().getValue();
        //
        // results.add(new Proposal(key, key, null, value.getProperty()));
        // }
        // }
        return results;
    }
}