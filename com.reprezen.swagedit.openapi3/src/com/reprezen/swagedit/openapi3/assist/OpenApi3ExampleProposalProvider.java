/*******************************************************************************
 * Copyright (c) 2019 ModelSolv, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.openapi3.assist;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.reprezen.swagedit.core.assist.JsonExampleProposalProvider;
import com.reprezen.swagedit.core.assist.ProposalDescriptor;
import com.reprezen.swagedit.core.assist.contexts.ContextType;
import com.reprezen.swagedit.core.assist.contexts.ContextTypeCollection;
import com.reprezen.swagedit.core.assist.contexts.RegexContextType;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.utils.DocumentUtils;
import com.reprezen.swagedit.core.utils.ModelUtils;

public class OpenApi3ExampleProposalProvider extends JsonExampleProposalProvider {

	private static final JsonPointer COMPONENTS_NODE_POINTER = JsonPointer.compile("/definitions/components");

	private static final ContextTypeCollection EXAMPLE_CONTEXT_TYPES;

	static {
		final List<ContextType> contentTypes = Arrays.asList(//
				new RegexContextType("Generate Example", "Example", ".*/example$"),
				new RegexContextType("Generate Example", "Example", ".*/examples/\\S+/value$")//
		);
		EXAMPLE_CONTEXT_TYPES = ContextType.newContentTypeCollection(contentTypes);
	}

	public OpenApi3ExampleProposalProvider(ContextTypeCollection contextTypes) {
		super(EXAMPLE_CONTEXT_TYPES);
	}

	public OpenApi3ExampleProposalProvider() {
		this(EXAMPLE_CONTEXT_TYPES);
	}

	@Override
	public Collection<ProposalDescriptor> getProposals(JsonPointer pointer, JsonDocument document) {
		final AbstractNode nodeAtPointer = document.getModel().find(pointer);

		JsonPointer jsonPointer = null;
		if (insideComponentsNode(nodeAtPointer)) {
			final AbstractNode schemaNode = nodeAtPointer.getParent();
			jsonPointer = schemaNode.getPointer();
		} else {
			final Optional<AbstractNode> parentNode = ModelUtils.findParentContainingField(//
					nodeAtPointer, SCHEMA_FIELD_NAME);
			if (parentNode.isPresent()) {
				final AbstractNode schemaNode = parentNode.get().get(SCHEMA_FIELD_NAME);
				jsonPointer = schemaNode.getPointer();
			} else {
				//TODO: Add code to handle this case
				jsonPointer = null; // schema filed not found in parent
			}
		}

		final String exampleData = getExampleDataProvider().getData(jsonPointer, document, DocumentUtils.getActiveEditorInputURI());
		return Arrays.asList(new ProposalDescriptor("Generate Example:").replacementString(exampleData).type("string"));
	}

	private boolean insideComponentsNode(AbstractNode node) {
		return ModelUtils.findParent(node, COMPONENTS_NODE_POINTER).isPresent();
	}

}
