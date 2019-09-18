package com.reprezen.swagedit.core.hover;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.texteditor.DefaultMarkerAnnotationAccess;

import com.reprezen.swagedit.core.Messages;

public class QuickFixInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

    private Composite parent;
    private AnnotationInfo input;
    private Link focusControl;
    private final DefaultMarkerAnnotationAccess markerAnnotationAccess;

    public QuickFixInformationControl(Shell parentShell, boolean isResizable) {
        super(parentShell, isResizable);
        markerAnnotationAccess = new DefaultMarkerAnnotationAccess();
        create();
    }

    public QuickFixInformationControl(Shell parentShell, String statusText) {
        super(parentShell, statusText);
        markerAnnotationAccess = new DefaultMarkerAnnotationAccess();
        create();
    }

    @Override
    public void setFocus() {
        super.setFocus();
        if (focusControl != null)
            focusControl.setFocus();
    }

    @Override
    public final void setVisible(boolean visible) {
        if (!visible)
            disposeDeferredCreatedContent();
        super.setVisible(visible);
    }

    @Override
    public void setInput(Object input) {
        this.input = (AnnotationInfo) input;
        disposeDeferredCreatedContent();
        deferredCreateContent();
    }

    private void createAnnotationInformation(Composite parent, final Annotation annotation) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 2;
        layout.marginWidth = 2;
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);

        final Canvas canvas = new Canvas(composite, SWT.NO_FOCUS);
        GridData gridData = new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false);
        gridData.widthHint = 17;
        gridData.heightHint = 16;
        canvas.setLayoutData(gridData);
        canvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                e.gc.setFont(null);
                markerAnnotationAccess.paint(annotation, e.gc, canvas, new Rectangle(0, 0, 16, 16));
            }
        });

        StyledText text = new StyledText(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        text.setLayoutData(data);
        String annotationText = annotation.getText();
        if (annotationText != null)
            text.setText(annotationText);
    }

    private void createCompletionProposalsControl(Composite parent, ICompletionProposal[] proposals) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout2 = new GridLayout(1, false);
        layout2.marginHeight = 0;
        layout2.marginWidth = 0;
        layout2.verticalSpacing = 2;
        composite.setLayout(layout2);

        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        separator.setLayoutData(gridData);

        Label quickFixLabel = new Label(composite, SWT.NONE);
        GridData layoutData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        layoutData.horizontalIndent = 4;
        quickFixLabel.setLayoutData(layoutData);
        String text;
        if (proposals.length == 1) {
            text = Messages.quick_fix_hover_single_quick_fix;
        } else {
            text = NLS.bind(Messages.quick_fix_hover_multiple_quick_fixes, proposals.length);
        }
        quickFixLabel.setText(text);

        setColorAndFont(composite, parent.getForeground(), parent.getBackground(), JFaceResources.getDialogFont());
        createCompletionProposalsList(composite, proposals);
    }

    private void createCompletionProposalsList(Composite parent, ICompletionProposal[] proposals) {
        final ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        scrolledComposite.setLayoutData(gridData);
        scrolledComposite.setExpandVertical(false);
        scrolledComposite.setExpandHorizontal(false);

        Composite composite = new Composite(scrolledComposite, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginLeft = 5;
        layout.verticalSpacing = 2;
        composite.setLayout(layout);

        List<Link> list = new ArrayList<Link>();
        for (int i = 0; i < proposals.length; i++) {
            list.add(createCompletionProposalLink(composite, proposals[i], 1));
        }
        final Link[] links = list.toArray(new Link[list.size()]);

        scrolledComposite.setContent(composite);
        setColorAndFont(scrolledComposite, parent.getForeground(), parent.getBackground(),
                JFaceResources.getDialogFont());

        Point contentSize = composite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        composite.setSize(contentSize);

        Point constraints = getSizeConstraints();
        if (constraints != null && contentSize.x < constraints.x) {
            ScrollBar horizontalBar = scrolledComposite.getHorizontalBar();

            int scrollBarHeight;
            if (horizontalBar == null) {
                Point scrollSize = scrolledComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                scrollBarHeight = scrollSize.y - contentSize.y;
            } else {
                scrollBarHeight = horizontalBar.getSize().y;
            }
            gridData.heightHint = contentSize.y - scrollBarHeight;
        }

        focusControl = links[0];
        for (int i = 0; i < links.length; i++) {
            final int index = i;
            final Link link = links[index];
            link.addKeyListener(new KeyListener() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.keyCode) {
                    case SWT.ARROW_DOWN:
                        if (index + 1 < links.length) {
                            links[index + 1].setFocus();
                        }
                        break;
                    case SWT.ARROW_UP:
                        if (index > 0) {
                            links[index - 1].setFocus();
                        }
                        break;
                    default:
                        break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                }
            });

            link.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    int currentPosition = scrolledComposite.getOrigin().y;
                    int hight = scrolledComposite.getSize().y;
                    int linkPosition = link.getLocation().y;

                    if (linkPosition < currentPosition) {
                        if (linkPosition < 10)
                            linkPosition = 0;

                        scrolledComposite.setOrigin(0, linkPosition);
                    } else if (linkPosition + 20 > currentPosition + hight) {
                        scrolledComposite.setOrigin(0, linkPosition - hight + link.getSize().y);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                }
            });
        }
    }

    private Link createCompletionProposalLink(Composite parent, final ICompletionProposal proposal, int count) {
        final boolean isMultiFix = count > 1;
        if (isMultiFix) {
            new Label(parent, SWT.NONE); // spacer to fill image cell
            parent = new Composite(parent, SWT.NONE); // indented composite for multi-fix
            GridLayout layout = new GridLayout(2, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            parent.setLayout(layout);
        }

        Label proposalImage = new Label(parent, SWT.NONE);
        proposalImage.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        Image image = proposal.getImage();

        if (image != null) {
            proposalImage.setImage(image);
            proposalImage.addMouseListener(new MouseListener() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                }

                @Override
                public void mouseDown(MouseEvent e) {
                }

                @Override
                public void mouseUp(MouseEvent e) {
                    if (e.button == 1) {
                        apply(proposal, input.viewer, input.position.offset, isMultiFix);
                    }
                }

            });
        }

        Link proposalLink = new Link(parent, SWT.WRAP);
        GridData layoutData = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        String linkText;
        if (isMultiFix) {
            linkText = NLS.bind(Messages.quick_fix_hover_multiple_quick_fixes, String.valueOf(count));
        } else {
            linkText = proposal.getDisplayString();
        }
        proposalLink.setText("<a>" + linkText + "</a>");
        proposalLink.setLayoutData(layoutData);
        proposalLink.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                apply(proposal, input.viewer, input.position.offset, isMultiFix);
            }
        });
        return proposalLink;
    }

    private void apply(ICompletionProposal p, ITextViewer viewer, int offset, boolean isMultiFix) {
        // Focus needs to be in the text viewer, otherwise linked mode does not work
        dispose();

        IRewriteTarget target = null;
        try {
            IDocument document = viewer.getDocument();

            if (viewer instanceof ITextViewerExtension) {
                ITextViewerExtension extension = (ITextViewerExtension) viewer;
                target = extension.getRewriteTarget();
            }

            if (target != null)
                target.beginCompoundChange();

            if (p instanceof ICompletionProposalExtension2) {
                ICompletionProposalExtension2 e = (ICompletionProposalExtension2) p;
                e.apply(viewer, (char) 0, isMultiFix ? SWT.CONTROL : SWT.NONE, offset);
            } else if (p instanceof ICompletionProposalExtension) {
                ICompletionProposalExtension e = (ICompletionProposalExtension) p;
                e.apply(document, (char) 0, offset);
            } else {
                p.apply(document);
            }

            Point selection = p.getSelection(document);
            if (selection != null) {
                viewer.setSelectedRange(selection.x, selection.y);
                viewer.revealRange(selection.x, selection.y);
            }
        } finally {
            if (target != null)
                target.endCompoundChange();
        }
    }

    @Override
    public boolean hasContents() {
        return input != null;
    }

    private AnnotationInfo getAnnotationInfo() {
        return input;
    }

    protected void deferredCreateContent() {
        fillToolbar();

        createAnnotationInformation(this.parent, getAnnotationInfo().annotation);
        setColorAndFont(this.parent, this.parent.getForeground(), this.parent.getBackground(),
                JFaceResources.getDialogFont());

        ICompletionProposal[] proposals = getAnnotationInfo().getCompletionProposals();
        if (proposals.length > 0)
            createCompletionProposalsControl(this.parent, proposals);

        this.parent.layout(true);
    }

    protected void disposeDeferredCreatedContent() {
        Control[] children = this.parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            children[i].dispose();
        }
        ToolBarManager toolBarManager = getToolBarManager();
        if (toolBarManager != null)
            toolBarManager.removeAll();
    }

    @Override
    public Point computeSizeHint() {
        Point preferedSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);

        Point constrains = getSizeConstraints();
        if (constrains == null)
            return preferedSize;

        Point constrainedSize = getShell().computeSize(constrains.x, SWT.DEFAULT, true);

        int width = Math.min(preferedSize.x, constrainedSize.x);
        int height = Math.max(preferedSize.y, constrainedSize.y);

        return new Point(width, height);
    }

    protected void fillToolbar() {
        ToolBarManager toolBarManager = getToolBarManager();
        if (toolBarManager == null)
            return;
        // input.fillToolBar(toolBarManager, this);
        toolBarManager.update(true);
    }

    @Override
    protected void createContent(Composite parent) {
        this.parent = parent;
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.parent.setLayout(layout);
    }

    private void setColorAndFont(Control control, Color foreground, Color background, Font font) {
        control.setForeground(foreground);
        control.setBackground(background);
        control.setFont(font);

        if (control instanceof Composite) {
            Control[] children = ((Composite) control).getChildren();
            for (int i = 0; i < children.length; i++) {
                setColorAndFont(children[i], foreground, background, font);
            }
        }
    }

}
