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
package com.reprezen.swagedit.openapi3.assist;

import com.google.common.collect.Lists;
import com.reprezen.swagedit.core.assist.JsonReferenceProposalProvider;
import com.reprezen.swagedit.openapi3.editor.OpenApi3ContentDescriber;

public class OpenApi3ReferenceProposalProvider extends JsonReferenceProposalProvider {

	public OpenApi3ReferenceProposalProvider() {
		super(OPEN_API3_CONTEXT_TYPES, OpenApi3ContentDescriber.CONTENT_TYPE_ID);
	}

	protected static final String SCHEMA_DEFINITION_REGEX = "^/components/schemas/(\\w+/)+\\$ref|.*schema/(\\w+/)?\\$ref";
	protected static final String PATH_ITEM_REGEX = "/paths/~1[^/]+/\\$ref";

	public static final ContextType SCHEMA_DEFINITION = new ContextType("components/schemas", "schemas",
			SCHEMA_DEFINITION_REGEX);
	public static final ContextType PATH_ITEM = new ContextType("paths", "path items", PATH_ITEM_REGEX);

	public static final ContextTypeCollection OPEN_API3_CONTEXT_TYPES = ContextType
			.newContentTypeCollection(Lists.newArrayList(SCHEMA_DEFINITION, PATH_ITEM));

}
