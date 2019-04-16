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
package com.reprezen.swagedit.core.assist;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.core.JsonPointer;
import com.reprezen.swagedit.core.assist.contexts.ContextType;
import com.reprezen.swagedit.core.assist.contexts.ContextTypeCollection;
import com.reprezen.swagedit.core.assist.exampleprovider.ExampleProvider;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.utils.ExtensionUtils;

/**
 * Completion proposal provider for JSON examples.
 */
public class JsonExampleProposalProvider {

	public static final String ID = "com.reprezen.swagedit.exampleprovider";
	protected static final String SCHEMA_FIELD_NAME = "schema";
	private final ContextTypeCollection contextTypes;

	public JsonExampleProposalProvider(ContextTypeCollection contextTypes) {
		this.contextTypes = contextTypes;
	}

	public boolean canProvideProposal(Model model, JsonPointer pointer) {
		return pointer != null && contextTypes.get(model, pointer) != ContextType.UNKNOWN;
	}

	public Collection<ProposalDescriptor> getProposals(JsonPointer pointer, JsonDocument document) {
		return Collections.emptyList();
	}

	protected ExampleProvider getExampleDataProvider() {
		final ExampleProvider defaultExampleProvider = (JsonPointer jsonPointer, JsonDocument document,
				URI uri) -> "example:"; 

		final Set<ExampleProvider> exampleProviders = ExtensionUtils.getExampleProviders();
		if (exampleProviders != null && !exampleProviders.isEmpty()) {
			return exampleProviders.iterator().next();
		} else {
			return defaultExampleProvider;
		}
	}

}
