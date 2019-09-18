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
package com.reprezen.swagedit.core.quickfix;

import static com.reprezen.swagedit.core.preferences.KaiZenPreferencesUtils.getTabWidth;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.osgi.framework.Bundle;

import com.reprezen.swagedit.core.validation.Messages;

public class QuickFixer implements IMarkerResolutionGenerator2 {

    @Override
    public IMarkerResolution[] getResolutions(IMarker marker) {
        if (isMissingObjectType(marker)) {
            return new IMarkerResolution[] { new FixMissingObjectType() };
        }
        return new IMarkerResolution[0];
    }

    @Override
    public boolean hasResolutions(IMarker marker) {
        return isMissingObjectType(marker);
    }

    private boolean isMissingObjectType(IMarker marker) {
        try {
            return Messages.error_object_type_missing.equals(marker.getAttribute(IMarker.MESSAGE));
        } catch (CoreException e) {
            return false;
        }
    }

    public static class FixMissingObjectType extends TextDocumentMarkerResolution {
        private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)\\S.*", Pattern.DOTALL);

        public String getLabel() {
            return "Set schema definition to object type";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public Image getImage() {
            Bundle bundle = Platform.getBundle("org.eclipse.ui.editors");
            URL url = FileLocator.find(bundle, new Path("icons/full/obj16/quick_fix_warning_obj.png"), null);
            ImageDescriptor imageDesc = ImageDescriptor.createFromURL(url);
            Image image = imageDesc.createImage();
            return image;
        }

        @Override
        public IRegion processFix(IDocument document, IMarker marker) throws CoreException {
            int offset = (int) marker.getAttribute(IMarker.CHAR_START);

            try {

                int line = document.getLineOfOffset(offset);
                String indent = getIndent(document, line);
                IRegion lineRegion = document.getLineInformation(line);

                int endOfLineOffset = lineRegion.getOffset() + lineRegion.getLength();

                // should be fine for first and last lines in the doc as well
                String replacementText = indent + "type: object";
                String delim = TextUtilities.getDefaultLineDelimiter(document);
                document.replace(endOfLineOffset, 0, delim + replacementText);
                return new Region(endOfLineOffset + delim.length(), replacementText.length());

            } catch (BadLocationException e) {
                throw new CoreException(createStatus(e, "Cannot process the IMarker"));
            }
        }

        protected String getIndent(IDocument document, int line) throws BadLocationException {
            String content = document.get(document.getLineOffset(line), document.getLineLength(line));

            Matcher m = WHITESPACE_PATTERN.matcher(content);
            String definitionIndent = m.matches() ? m.group(1) : "";

            StringBuilder indent = new StringBuilder();
            indent.append(definitionIndent);
            IntStream.range(0, getTabWidth()).forEach(el -> indent.append(" "));

            return indent.toString();
        }

    }

}
