package com.reprezen.swagedit.core.hover;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;

import com.reprezen.swagedit.core.assist.JsonQuickAssistProcessor;

public class ProblemTextHover extends AbstractProblemHover
        implements ITextHover, ITextHoverExtension, ITextHoverExtension2 {

    public ProblemTextHover(ISourceViewer sourceViewer) {
        super(sourceViewer);
    }

    @Override
    public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
        int lineNumber = 0;
        try {
            lineNumber = textViewer.getDocument().getLineOfOffset(hoverRegion.getOffset());
        } catch (final BadLocationException e) {
            return null;
        }

        List<Annotation> annotations = getAnnotations(lineNumber, hoverRegion.getOffset());
        JsonQuickAssistProcessor processor = new JsonQuickAssistProcessor();

        AnnotationInfo result = annotations.stream() //
                .filter(ann -> ann.getText() != null) //
                .map(ann -> {
                    Position position = getAnnotationModel().getPosition(ann);
                    ICompletionProposal[] proposals = processor
                            .computeQuickAssistProposals(new IQuickAssistInvocationContext() {
                                @Override
                                public ISourceViewer getSourceViewer() {
                                    return ProblemTextHover.this.getSourceViewer();
                                }

                                @Override
                                public int getOffset() {
                                    return hoverRegion.getOffset();
                                }

                                @Override
                                public int getLength() {
                                    return hoverRegion.getLength();
                                }
                            });
                    return new AnnotationInfo(ann, position, textViewer, proposals);
                }) //
                   // We return only 1 error to avoid UI complexities
                   // Once user fixes this error, the others will be displayed
                .findFirst() //
                .orElse(null);

        return result;
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        return null;
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        int lineNumber = 0;
        try {
            lineNumber = textViewer.getDocument().getLineOfOffset(offset);
        } catch (BadLocationException e) {
            return null;
        }

        List<Annotation> annotations = getAnnotations(lineNumber, offset);
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                Position position = getSourceViewer().getAnnotationModel().getPosition(annotation);
                if (position != null) {
                    final int start = position.getOffset();
                    return new Region(start, position.getLength());
                }
            }
        }
        return null;
    }

}
