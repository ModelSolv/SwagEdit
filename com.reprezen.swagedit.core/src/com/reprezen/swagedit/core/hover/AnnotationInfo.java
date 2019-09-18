package com.reprezen.swagedit.core.hover;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.Annotation;

public class AnnotationInfo {
    public final Annotation annotation;
    public final Position position;
    public final ITextViewer viewer;
    public final ICompletionProposal[] proposals;

    public AnnotationInfo(Annotation annotation, Position position, ITextViewer textViewer,
            ICompletionProposal[] proposals) {
        this.annotation = annotation;
        this.position = position;
        this.viewer = textViewer;
        this.proposals = proposals;
    }

    public ICompletionProposal[] getCompletionProposals() {
        return proposals;
    }

}
