package com.reprezen.swagedit.core.hover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ILineDiffInfo;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

public class AbstractProblemHover {

    private final ISourceViewer sourceViewer;

    public AbstractProblemHover(ISourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    protected ISourceViewer getSourceViewer() {
        return sourceViewer;
    }

    protected boolean isLineDiffInfo(Annotation annotation) {
        return annotation instanceof ILineDiffInfo;
    }

    protected IAnnotationModel getAnnotationModel() {
        return sourceViewer.getAnnotationModel();
    }

    protected IDocument getDocument() {
        return sourceViewer.getDocument();
    }

    private boolean isHandled(Annotation annotation) {
        return true;
    }

    private IInformationControlCreator presenterControlCreator;
    private HoverControlCreator hoverControlCreator;

    private static final class PresenterControlCreator extends AbstractReusableInformationControlCreator {

        @Override
        public IInformationControl doCreateInformationControl(Shell parent) {
            // DIFF: do not show toolbar in hover, no configuration supported (2)
            // return new AnnotationInformationControl(parent, new ToolBarManager(SWT.FLAT));
            return new QuickFixInformationControl(parent, true);
        }
    }

    private static final class HoverControlCreator extends AbstractReusableInformationControlCreator {
        private final IInformationControlCreator presenterControlCreator;

        public HoverControlCreator(IInformationControlCreator presenterControlCreator) {
            this.presenterControlCreator = presenterControlCreator;
        }

        @Override
        public IInformationControl doCreateInformationControl(Shell parent) {
            return new QuickFixInformationControl(parent, EditorsUI.getTooltipAffordanceString()) {

                @Override
                public IInformationControlCreator getInformationPresenterControlCreator() {
                    return presenterControlCreator;
                }
            };
        }

        @Override
        public boolean canReuse(IInformationControl control) {
            if (!super.canReuse(control))
                return false;

            if (control instanceof IInformationControlExtension4)
                ((IInformationControlExtension4) control).setStatusText(EditorsUI.getTooltipAffordanceString());

            return true;
        }
    }

    public IInformationControlCreator getHoverControlCreator() {
        if (hoverControlCreator == null)
            hoverControlCreator = new HoverControlCreator(getInformationPresenterControlCreator());
        return hoverControlCreator;
    }

    public IInformationControlCreator getInformationPresenterControlCreator() {
        if (presenterControlCreator == null)
            presenterControlCreator = new PresenterControlCreator();
        return presenterControlCreator;
    }

    public List<Annotation> getAnnotations(final int lineNumber, final int offset) {
        if (getAnnotationModel() == null) {
            return Collections.emptyList();
        }

        final Iterator<?> iterator = getAnnotationModel().getAnnotationIterator();
        List<Annotation> result = new ArrayList<>();
        while (iterator.hasNext()) {
            final Annotation annotation = (Annotation) iterator.next();
            if (isHandled(annotation)) {
                Position position = getAnnotationModel().getPosition(annotation);
                if (position != null) {
                    final int start = position.getOffset();
                    final int end = start + position.getLength();

                    if (offset > 0 && !(start <= offset && offset <= end)) {
                        continue;
                    }
                    try {
                        int startLine = getDocument().getLineOfOffset(start);
                        if (lineNumber != startLine) {
                            continue;
                        }
                    } catch (final Exception x) {
                        continue;
                    }
                    if (!isLineDiffInfo(annotation)) {
                        result.add(annotation);
                    }
                }
            }
        }
        return result;
    }

}
