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
package com.reprezen.swagedit.openapi3.editor;

import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.openapi3.Activator;
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema;

public class OpenApi3Document extends JsonDocument {

	public OpenApi3Document() {
        this(Activator.getDefault().getSchema());
	}

    public OpenApi3Document(OpenApi3Schema schema) {
        // `new YAMLMapper()` was replaced by `new ObjectMapper(new YAMLFactory())`.
        // It's done to eliminate an initialization exception here -
        // Jackson Dataformat library does not declare an OSGi dependency on Jackson Databind which is needed to
        // initialize YAMLMapper (it extends ObjectMapper from Jackson Databind).
        super(schema);
    }

}
