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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import com.reprezen.swagedit.core.Activator;
import com.reprezen.swagedit.core.quickfix.TextDocumentMarkerResolution;
import com.reprezen.swagedit.core.utils.ExtensionUtils;

public class JsonQuickAssistProcessor implements IQuickAssistProcessor {

    private String errorMessage;
    private Set<IMarkerResolutionGenerator2> generators;

    public JsonQuickAssistProcessor() {
        this.generators = ExtensionUtils.getMarkerResolutionGenerators();
    }

    @Override
    public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    protected Stream<MarkerResolutionProposal> getMarkerResolution(IMarker marker, ISourceViewer viewer) {
        return generators.stream() //
                .flatMap(generator -> Stream.of(generator.getResolutions(marker))) //
                .map(resolution -> new MarkerResolutionProposal(marker, resolution, viewer));
    }

    @Override
    public boolean canFix(Annotation annotation) {
        if (annotation.isMarkedDeleted()) {
            return false;
        }
        if (annotation instanceof MarkerAnnotation) {
            MarkerAnnotation markerAnnotation = (MarkerAnnotation) annotation;
            if (!markerAnnotation.isQuickFixableStateSet()) {
                markerAnnotation.setQuickFixable(
                        generators.stream().anyMatch(e -> e.hasResolutions(markerAnnotation.getMarker())));
            }
            return markerAnnotation.isQuickFixable();
        }
        return false;
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
        List<IMarker> markers;
        try {
            markers = getMarkersFor(invocationContext.getSourceViewer(), invocationContext.getOffset());
        } catch (BadLocationException e) {
            errorMessage = e.getMessage();
            return new ICompletionProposal[0];
        }

        Collection<MarkerResolutionProposal> result = markers.stream() //
                .flatMap(marker -> getMarkerResolution(marker, invocationContext.getSourceViewer())) //
                // This is how we return only distinct values.
                // Without it we would have multiple quickfixes for the example preferences when multiple markers are
                // on the same line.
                .collect(Collectors.toMap(proposal -> proposal.getDisplayString(), proposal -> proposal, (u, v) -> u)) //
                .values();

        return result.toArray(new ICompletionProposal[result.size()]);
    }

    protected List<IMarker> getMarkersFor(ISourceViewer sourceViewer, int offset) throws BadLocationException {
        final IDocument document = sourceViewer.getDocument();

        int line = document.getLineOfOffset(offset);
        int lineOffset = document.getLineOffset(line);

        String delim = document.getLineDelimiter(line);
        int delimLength = delim != null ? delim.length() : 0;
        int lineLength = document.getLineLength(line) - delimLength;

        return getMarkersFor(sourceViewer, lineOffset, lineLength);
    }

    protected List<IMarker> getMarkersFor(ISourceViewer sourceViewer, int lineOffset, int lineLength) {
        List<IMarker> result = new ArrayList<>();
        IAnnotationModel annotationModel = sourceViewer.getAnnotationModel();
        Iterator<?> annotationIter = annotationModel.getAnnotationIterator();

        while (annotationIter.hasNext()) {
            Object annotation = annotationIter.next();

            if (annotation instanceof MarkerAnnotation) {
                MarkerAnnotation markerAnnotation = (MarkerAnnotation) annotation;
                IMarker marker = markerAnnotation.getMarker();
                Position markerPosition = annotationModel.getPosition(markerAnnotation);
                if (markerPosition != null && markerPosition.overlapsWith(lineOffset, lineLength)) {
                    result.add(marker);
                }
            }
        }
        return result;
    }

    public static class MarkerResolutionProposal implements ICompletionProposal {

        private final IMarker marker;
        private final IMarkerResolution markerResolution;
        private final ISourceViewer sourceViewer;

        public MarkerResolutionProposal(IMarker marker, IMarkerResolution markerResolution,
                ISourceViewer sourceViewer) {
            this.marker = marker;
            this.markerResolution = markerResolution;
            this.sourceViewer = sourceViewer;
        }

        @Override
        public void apply(IDocument document) {
            if (markerResolution instanceof TextDocumentMarkerResolution) {
                try {
                    IRegion region = ((TextDocumentMarkerResolution) markerResolution).processFix(document, marker);
                    if (region != null) {
                        sourceViewer.setSelectedRange(region.getOffset(), region.getLength());
                    }
                } catch (CoreException e) {
                    Activator.getDefault().getLog().log(e.getStatus());
                }
            } else {
                markerResolution.run(marker);
            }
        }

        @Override
        public Point getSelection(IDocument document) {
            return null;
        }

        @Override
        public String getAdditionalProposalInfo() {
            if (markerResolution instanceof IMarkerResolution2) {
                return ((IMarkerResolution2) markerResolution).getDescription();
            }
            return null;
        }

        @Override
        public String getDisplayString() {
            return markerResolution.getLabel();
        }

        @Override
        public Image getImage() {
            if (markerResolution instanceof IMarkerResolution2) {
                return ((IMarkerResolution2) markerResolution).getImage();
            }
            return null;
        }

        @Override
        public IContextInformation getContextInformation() {
            if (markerResolution instanceof IMarkerResolution2) {
                IMarkerResolution2 mr2 = (IMarkerResolution2) markerResolution;
                String displayString = mr2.getDescription() == null ? mr2.getLabel() : mr2.getDescription();

                return new ContextInformation(mr2.getImage(), mr2.getLabel(), displayString);
            }
            return null;
        }
    }

}