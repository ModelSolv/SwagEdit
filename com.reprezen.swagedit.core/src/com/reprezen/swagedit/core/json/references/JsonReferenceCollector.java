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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;

/**
 * Collector of JSON references present in a JSON or YAML document.
 * 
 * This class can be used to obtain all JSONReference present inside a JSON or YAML document.
 * 
 */
public class JsonReferenceCollector {

    private final JsonReferenceFactory factory;

    public JsonReferenceCollector(JsonReferenceFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns all reference nodes that can be found in the JSON document.
     * 
     * @param baseURI
     * @param model
     * @return all reference nodes
     */
    public Map<JsonReference, List<AbstractNode>> collect(URI baseURI, Model model) {
        final Map<JsonReference, List<AbstractNode>> references = new HashMap<>();

        for (AbstractNode node : model.allNodes()) {
            if (factory.isReference(node)) {
                JsonReference reference = factory.createSimpleReference(baseURI, node.get(PROPERTY));
                if (reference == null) {
                    reference = factory.create(node);
                }

                if (reference != null) {
                    if (references.containsKey(reference)) {
                        references.get(reference).add(node);
                    } else {
                        List<AbstractNode> values = new ArrayList<>();
                        values.add(node);

                        references.put(reference, values);
                    }
                }
            }
        }

        return references;
    }
}
