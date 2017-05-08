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
package com.reprezen.swagedit.core.assist;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.reprezen.swagedit.core.json.references.JsonDocumentManager;
import com.reprezen.swagedit.core.utils.DocumentUtils;
import com.reprezen.swagedit.core.utils.SwaggerFileFinder;
import com.reprezen.swagedit.core.utils.SwaggerFileFinder.Scope;
import com.reprezen.swagedit.core.utils.URLUtils;
import com.reprezen.swagedit.core.validation.ValidationUtil;

/**
 * Completion proposal provider for JSON references.
 */
public class JsonReferenceProposalProvider {

    private final JsonDocumentManager manager = JsonDocumentManager.getInstance();
	private final ContextTypeCollection contextTypes;
	private final String fileContentType;
    
    public JsonReferenceProposalProvider(ContextTypeCollection contextTypes, String fileContentType) {
		this.contextTypes = contextTypes;
		this.fileContentType = fileContentType;
	}

    protected IFile getActiveFile() {
        return DocumentUtils.getActiveEditorInput().getFile();
    }
    
    protected ContextTypeCollection getContextTypes() {
    	return contextTypes;
    }

    /**
     * Returns collection of JSON reference proposals.
     * 
     * If the scope is local, it will only return JSON references from within the current document.
     * 
     * If the scope is project, it will return all JSON references from within the current document and from all
     * documents inside the same project.
     * 
     * If the scope is workspace, it will return all JSON references from within the current document and from all
     * documents inside the same workspace.
     * 
     * @param pointer
     * @param doc
     * @param scope
     * @return proposals
     */
    public Collection<Proposal> getProposals(JsonPointer pointer, JsonNode doc, Scope scope) {
        final ContextType type = contextTypes.get(pointer.toString());
        final IFile currentFile = getActiveFile();
        final IPath basePath = currentFile.getParent().getFullPath();
        final List<Proposal> proposals = Lists.newArrayList();

        if (scope == Scope.LOCAL) {
            proposals.addAll(collectProposals(doc, type.value(), null));
        } else {
            final SwaggerFileFinder fileFinder = new SwaggerFileFinder(fileContentType);

            for (IFile file : fileFinder.collectFiles(scope, currentFile)) {
                IPath relative = file.equals(currentFile) ? null : file.getFullPath().makeRelativeTo(basePath);
                JsonNode content = file.equals(currentFile) ? doc : manager.getDocument(file.getLocationURI());
                proposals.addAll(collectProposals(content, type.value(), relative));
            }
        }

        return proposals;
    }

    /**
     * Represents the different contexts for which a JSON reference may be computed. <br/>
     * The context type is determined by the pointer (path) on which the completion proposal has been activated.
     */
    public static class ContextType {
        public static final ContextType UNKNOWN = new ContextType(null, "", null);

        private final String value;
        private final String label;
		private final String regex;

        public ContextType(String value, String label, String regex) {
            this.value = value;
            this.label = label;
			this.regex = regex;
        }

        public String value() {
            return value;
        }

        public String label() {
            return label;
        }
        
        public static ContextTypeCollection newContentTypeCollection(Iterable<ContextType> contextTypes) {
        	return new ContextTypeCollection(contextTypes);
        }
        
		public static ContextTypeCollection emptyContentTypeCollection() {
			return new ContextTypeCollection(Collections.<ContextType>emptyList());
		}
    }
    
    public static class ContextTypeCollection {
    	
    	private final Iterable<ContextType> contextTypes;

		protected ContextTypeCollection(Iterable<ContextType> contextTypes) {
			this.contextTypes = contextTypes;
    	}
		
		public ContextType get(String path) {
			if (Strings.emptyToNull(path) == null) {
				return ContextType.UNKNOWN;
			}
			for (ContextType next : contextTypes) {
				if (path.matches(next.regex)) {
					return next;
				}
			}
			return ContextType.UNKNOWN;
		}
    }

    /**
     * Returns all proposals found in the document at the specified field.
     * 
     * @param document
     * @param fieldName
     * @param path
     * @return Collection of proposals
     */
    public Collection<Proposal> collectProposals(JsonNode document, String fieldName, IPath path) {
        final Collection<Proposal> results = Lists.newArrayList();
        if (fieldName == null) {
            return results;
        }

		final JsonNode parameters = ValidationUtil.findNode(fieldName, document);
        if (parameters == null) {
        	return results;
        }
        final String basePath = (path != null ? path.toString() : "") + "#/" + fieldName + "/";

        for (Iterator<String> it = parameters.fieldNames(); it.hasNext();) {
            String key = it.next();
            String value = basePath + key.replaceAll("/", "~1");
            String encoded = URLUtils.encodeURL(value);

            results.add(new Proposal("\"" + encoded + "\"", key, null, value));
        }

        return results;
    }

}
