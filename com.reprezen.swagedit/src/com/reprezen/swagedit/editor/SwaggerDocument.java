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
package com.reprezen.swagedit.editor;

import com.reprezen.swagedit.Activator;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.schema.SwaggerSchema;

/**
 * SwaggerDocument
 * 
 */
public class SwaggerDocument extends JsonDocument {

    public SwaggerDocument() {
        super(Activator.getDefault() != null ? Activator.getDefault().getSchema() : new SwaggerSchema());
    }

    @Override
    public Version getVersion() {
        return Version.SWAGGER;
    }

}
