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
package com.reprezen.swagedit.assist;

import com.google.common.collect.Lists;
import com.reprezen.swagedit.core.assist.JsonReferenceProposalProvider;
import com.reprezen.swagedit.editor.SwaggerContentDescriber;

/**
 * Completion proposal provider for JSON references.
 */
public class SwaggerReferenceProposalProvider extends JsonReferenceProposalProvider {

	public SwaggerReferenceProposalProvider() {
		super(SWAGGER_CONTEXT_TYPES, SwaggerContentDescriber.CONTENT_TYPE_ID);
	}

	protected static final String SCHEMA_DEFINITION_REGEX = "^/definitions/(\\w+/)+\\$ref|.*schema/(\\w+/)?\\$ref";
	protected static final String RESPONSE_REGEX = ".*responses/(\\d+|default)/\\$ref";
	protected static final String PARAMETER_REGEX = ".*/parameters/\\d+/\\$ref";
	protected static final String PATH_ITEM_REGEX = "/paths/~1[^/]+/\\$ref";
	

	public static final ContextType SCHEMA_DEFINITION = new ContextType("definitions", "schemas",
			SCHEMA_DEFINITION_REGEX);
	public static final ContextType PATH_ITEM = new ContextType("paths", "path items", PATH_ITEM_REGEX);
	public static final ContextType PATH_PARAMETER = new ContextType("parameters", "parameters", PARAMETER_REGEX);
	public static final ContextType PATH_RESPONSE = new ContextType("responses", "responses", RESPONSE_REGEX);

	public static final ContextTypeCollection SWAGGER_CONTEXT_TYPES = ContextType.newContentTypeCollection(Lists.newArrayList(SCHEMA_DEFINITION, PATH_ITEM, PATH_PARAMETER, PATH_RESPONSE));

}
