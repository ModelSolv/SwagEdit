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
    
	protected static final String COMPONENT_NAME_REGEX = "[\\w\\.\\-]+";
	
    protected static final String SCHEMA_COMPONENT_REGEX = "^/components/schemas/(\\w+/)+";
    protected static final String INLINE_SCHEMA_REGEX = ".*schema/(\\w+/)*";
	protected static final String SCHEMA_DEFINITION_REGEX = SCHEMA_COMPONENT_REGEX + "\\$ref" + "|" + INLINE_SCHEMA_REGEX + "\\$ref";
    protected static final String PATH_ITEM_REGEX = "/paths/~1[^/]+/\\$ref"// in paths object
            // e.g. "/components/callbacks/myWebhook/$request.body#~1url/$ref"
            + "|/components/callbacks/" + COMPONENT_NAME_REGEX + "/[^/]+/\\$ref"; // in callbacks
    protected static final String PARAMETER_REGEX = ".*/parameters/\\d+/\\$ref";
    protected static final String RESPONSE_REGEX = ".*/responses/([0-9X]{3}|default)/\\$ref";
    protected static final String REQUEST_BODY_REGEX = ".*/requestBody/\\$ref";
    protected static final String LINK_REGEX = ".*/links/"+COMPONENT_NAME_REGEX +"/\\$ref";
    protected static final String LINK_OPERATIONID_REGEX = ".*/links/" + COMPONENT_NAME_REGEX + "/operationId";
    protected static final String EXAMPLE_REGEX = ".*/examples/"+COMPONENT_NAME_REGEX +"/\\$ref";
    protected static final String SCHEMA_EXAMPLE_REGEX = SCHEMA_COMPONENT_REGEX + "example/\\$ref" + "|"
            + INLINE_SCHEMA_REGEX + "example/\\$ref";
    protected static final String HEADER_REGEX = ".*/headers/"+COMPONENT_NAME_REGEX +"/\\$ref";
    protected static final String CALLBACK_REGEX = ".*/callbacks/"+COMPONENT_NAME_REGEX +"/\\$ref";
   
	public static final ContextType SCHEMA_DEFINITION = new ContextType("components/schemas", "schemas",
			SCHEMA_DEFINITION_REGEX);
	public static final ContextType PATH_ITEM = new ContextType("paths", "path items", PATH_ITEM_REGEX);
    public static final ContextType PATH_PARAMETER = new ContextType("components/parameters", "parameters",
            PARAMETER_REGEX);
    public static final ContextType PATH_RESPONSE = new ContextType("components/responses", "responses",
            RESPONSE_REGEX);
    public static final ContextType PATH_REQUEST_BODY = new ContextType("components/requestBodies", "requestBody",
            REQUEST_BODY_REGEX);
    public static final ContextType PATH_LINK = new ContextType("components/links", "link", LINK_REGEX);
    public static final ContextType EXAMPLE = new ContextType("components/examples", "examples", EXAMPLE_REGEX);
    //public static final ContextType SCHEMA_EXAMPLE = new ContextType("its/not/example/component", "example", SCHEMA_EXAMPLE_REGEX);
    public static final ContextType HEADER = new ContextType("components/headers", "header", HEADER_REGEX);
    public static final ContextType CALLBACK = new ContextType("components/callbacks", "callback", CALLBACK_REGEX);
    public static final ContextType PATH_LINK_OPERATION_ID = new ContextType("components/links/", "operationId",
            CALLBACK_REGEX);
    public static final ContextType LINK_OPERATIONID = new ContextTypeValue("operationId", "operationId",
            LINK_OPERATIONID_REGEX, true);

    public static final ContextTypeCollection OPEN_API3_CONTEXT_TYPES = ContextType
            .newContentTypeCollection(Lists.newArrayList( //
                  //  SCHEMA_EXAMPLE, // should go before schema definition
                    SCHEMA_DEFINITION, //
                    PATH_ITEM, //
                    PATH_PARAMETER, //
                    PATH_RESPONSE, //
                    PATH_REQUEST_BODY, //
                    PATH_LINK, //
                    EXAMPLE, //
                    HEADER, //
                    CALLBACK, //
                    LINK_OPERATIONID));

}
