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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.google.common.base.Strings;
import com.reprezen.swagedit.core.Activator;
import com.reprezen.swagedit.core.Activator.Icons;

public class StyledCompletionProposal
        implements ICompletionProposal, ICompletionProposalExtension5, ICompletionProposalExtension6 {

    private final int replacementOffset;
    private final String replacementString;
    private final StyledString label;
    private final String description;
    /** Lower-cased prefix - content assist typeahead should be case-insensitive */
    private final String prefix;

    public StyledCompletionProposal(String replacement, StyledString label, String prefix, String description,
            int offset) {
        this.label = label;
        this.replacementString = replacement;
        this.prefix = prefix != null ? prefix.toLowerCase() : null;
        this.replacementOffset = offset;
        this.description = description;
    }

    @Override
    public StyledString getStyledDisplayString() {
        return label;
    }

    @Override
    public void apply(IDocument document) {
        int length = 0;
        int offset = replacementOffset;
        String text = replacementString;

        if (Strings.emptyToNull(prefix) != null) {
            if (replacementString.toLowerCase().startsWith(prefix)) {
                text = replacementString.substring(prefix.length());
            } else if (replacementString.toLowerCase().contains(prefix)) {
                offset = replacementOffset - prefix.length();
                length = prefix.length();
            }
        }

        try {
            document.replace(offset, length, text);
        } catch (BadLocationException x) {
            // ignore
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        int offset = replacementOffset;

        if (Strings.emptyToNull(prefix) != null) {
            if (replacementString.toLowerCase().startsWith(prefix)) {
                offset = replacementOffset - prefix.length();
            } else if (replacementString.toLowerCase().contains(prefix)) {
                offset = replacementOffset - prefix.length();
            }
        }

        int cursorPosition = offset + replacementString.length();

        return new Point(cursorPosition, 0);
    }

    @Override
    public String getAdditionalProposalInfo() {
        return description;
    }

    @Override
    public String getDisplayString() {
        return label.getString();
    }

	@Override
	public Image getImage() {
		return Activator.getDefault().getImage(Icons.assist_item);
	}

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    public String getReplacementString() {
        return replacementString;
    }

    @Override
    public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
        return description;
    }
}