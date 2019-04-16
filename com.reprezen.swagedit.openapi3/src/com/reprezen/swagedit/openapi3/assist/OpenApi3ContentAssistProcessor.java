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
package com.reprezen.swagedit.openapi3.assist;

import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import com.reprezen.swagedit.core.assist.JsonContentAssistProcessor;
import com.reprezen.swagedit.core.assist.JsonProposalProvider;
import com.reprezen.swagedit.core.assist.JsonReferenceProposalProvider;
import com.reprezen.swagedit.core.assist.ext.MediaTypeContentAssistExt;
import com.reprezen.swagedit.core.assist.ext.ResponseCodeContentAssistExt;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.schema.CompositeSchema;
import com.reprezen.swagedit.openapi3.Activator;
import com.reprezen.swagedit.openapi3.assist.ext.CallbacksContentAssistExt;
import com.reprezen.swagedit.openapi3.assist.ext.ParameterInContentAssistExt;
import com.reprezen.swagedit.openapi3.assist.ext.SchemaFormatContentAssistExt;
import com.reprezen.swagedit.openapi3.assist.ext.SchemaTypeContentAssistExt;

public class OpenApi3ContentAssistProcessor extends JsonContentAssistProcessor {
    
    private static final JsonProposalProvider proposalProvider = new JsonProposalProvider(
            new CallbacksContentAssistExt(), //
            new SchemaTypeContentAssistExt(), //
            new SchemaFormatContentAssistExt(), //
            new ParameterInContentAssistExt(), //
            new ResponseCodeContentAssistExt(), //
			new MediaTypeContentAssistExt());

	public OpenApi3ContentAssistProcessor(ContentAssistant ca) {
        super(ca, proposalProvider, new OpenApi3ReferenceProposalProvider(), new OpenApi3ExampleProposalProvider());
	}

    public OpenApi3ContentAssistProcessor(ContentAssistant ca, CompositeSchema schema) {
        super(ca, proposalProvider, new OpenApi3ReferenceProposalProvider(schema), new OpenApi3ExampleProposalProvider());
    }
    
    public OpenApi3ContentAssistProcessor(ContentAssistant ca, JsonReferenceProposalProvider referenceProposalProvider) {
        super(ca, proposalProvider, referenceProposalProvider, new OpenApi3ExampleProposalProvider());
    } 
    
	@Override
	protected TemplateStore getTemplateStore() {
        return Activator.getDefault().getTemplateStore();
	}

	@Override
	protected ContextTypeRegistry getContextTypeRegistry() {
        return Activator.getDefault().getContextTypeRegistry();
	}

    @Override
    protected String getContextTypeId(Model model, String path) {
        TemplateContextType contextType = Activator.getDefault().getOpenApi3ContextTypeProvider().getContextType(model, path);
        return contextType != null ? contextType.getId() : null;
    }

}
