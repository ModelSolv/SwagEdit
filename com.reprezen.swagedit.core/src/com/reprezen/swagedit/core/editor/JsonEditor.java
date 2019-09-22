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
package com.reprezen.swagedit.core.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dadacoalition.yedit.YEditLog;
import org.dadacoalition.yedit.editor.IDocumentIdleListener;
import org.dadacoalition.yedit.editor.YEdit;
import org.dadacoalition.yedit.editor.YEditSourceViewerConfiguration;
import org.dadacoalition.yedit.preferences.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.swt.IFocusService;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.reprezen.swagedit.core.editor.outline.JsonContentOutlinePage;
import com.reprezen.swagedit.core.handlers.OpenQuickOutlineHandler;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.validation.Validator;

/**
 * SwagEdit editor.
 * 
 */
public abstract class JsonEditor extends YEdit implements IShowInSource, IShowInTarget {

    public static final String CONTEXT = "com.reprezen.swagedit.context";

    private ProjectionSupport projectionSupport;
    private Map<AbstractNode, ProjectionAnnotation> oldProjectionAnnotations = new HashMap<>();;
    private ProjectionAnnotationModel annotationModel;
    private Composite topPanel;
    protected JsonSourceViewerConfiguration sourceViewerConfiguration;

    private final IDocumentListener changeListener = new IDocumentListener() {
        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            if (event.getDocument() instanceof JsonDocument) {
                final JsonDocument document = (JsonDocument) event.getDocument();

                document.onChange();
                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (contentOutline != null) {
                            // depends on the results of document.onChange()
                            contentOutline.setInput(getEditorInput());
                        }
                    }
                });
                // depends on the results of document.onChange()
                runValidate(false);
            }
        }
    };

    /*
     * This listener is added to the preference store when the editor is initialized. It listens to changes to color
     * preferences. Once a color change happens, the editor is re-initialized. It also handles changes in validation
     * preferences
     */
    protected final IPropertyChangeListener preferenceChangeListener = new JsonPreferenceChangeListener();

    private JsonContentOutlinePage contentOutline;

    public JsonEditor(JsonDocumentProvider documentProvider, IPreferenceStore preferenceStore) {
        super();
        setPreferenceStore(preferenceStore);
        setDocumentProvider(documentProvider);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        super.init(site, input);
        IContextService contextService = (IContextService) site.getService(IContextService.class);
        contextService.activateContext(CONTEXT);
    }

    @Override
    protected void initializeEditor() {
        super.initializeEditor();
        setHelpContextId(CONTEXT);
        setSourceViewerConfiguration(createSourceViewerConfiguration());
    }

    protected abstract YEditSourceViewerConfiguration createSourceViewerConfiguration();

    @Override
    protected void doSetInput(IEditorInput input) throws CoreException {
        if (input != null) {
            super.doSetInput(input);

            IDocument document = getDocumentProvider().getDocument(getEditorInput());
            if (document != null) {
                document.addDocumentListener(changeListener);
                // validate content before editor opens
                runValidate(true);
            }
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(Class required) {
        Object adapter = super.getAdapter(required);
        if (IContentOutlinePage.class.equals(required)) {
            if (contentOutline == null) {
                contentOutline = new JsonContentOutlinePage(getDocumentProvider(), this);
                if (getEditorInput() != null) {
                    contentOutline.setInput(getEditorInput());
                }
            }
            adapter = contentOutline;
        }

        return adapter;
    }

    public ProjectionViewer getProjectionViewer() {
        return (ProjectionViewer) getSourceViewer();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        ProjectionViewer viewer = getProjectionViewer();

        projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
        projectionSupport.install();

        // turn projection mode on
        viewer.doOperation(ProjectionViewer.TOGGLE);

        annotationModel = viewer.getProjectionAnnotationModel();
        getPreferenceStore().addPropertyChangeListener(preferenceChangeListener);
    }

    @Override
    public void dispose() {
        // preference store is removed in AbstractTextEditor.dispose()
        getPreferenceStore().removePropertyChangeListener(preferenceChangeListener);
        super.dispose();
    }

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginHeight = 0;
        compositeLayout.marginWidth = 0;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;
        composite.setLayout(compositeLayout);

        topPanel = new Composite(composite, SWT.NONE);
        topPanel.setLayout(new StackLayout());
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite editorComposite = new Composite(composite, SWT.NONE);
        editorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
        fillLayout.marginHeight = 0;
        fillLayout.marginWidth = 0;
        fillLayout.spacing = 0;
        editorComposite.setLayout(fillLayout);

        ISourceViewer result = doCreateSourceViewer(editorComposite, ruler, styles);

        return result;
    }

    /**
     * SwaggerEditor provides additional hidden panel on top of the source viewer, where external integrations can put
     * their UI.
     * <p/>
     * The panel is only a placeholder, that is:
     * <ul>
     * <li>it is not visible by default</li>
     * <li>it has a {@link StackLayout} and expect single composite to be created per contribution</li>
     * <li>if there are more than one contributors, it is their responsibility to manage {@link StackLayout#topControl}
     * </li>
     * </ul>
     */
    public Composite getTopPanel() {
        return topPanel;
    }

    protected ISourceViewer doCreateSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        ProjectionViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(),
                styles) {

            private IInformationPresenter outlinePresenter;

            @Override
            public void doOperation(int operation) {
                if (operation == OpenQuickOutlineHandler.QUICK_OUTLINE && outlinePresenter != null) {
                    outlinePresenter.showInformation();
                    return;
                }
                super.doOperation(operation);
            }

            @Override
            public boolean canDoOperation(int operation) {
                if (operation == OpenQuickOutlineHandler.QUICK_OUTLINE && outlinePresenter != null) {
                    return true;
                }
                return super.canDoOperation(operation);
            }

            @Override
            public void configure(SourceViewerConfiguration configuration) {
                super.configure(configuration);

                if (configuration instanceof JsonSourceViewerConfiguration) {
                    JsonSourceViewerConfiguration c = (JsonSourceViewerConfiguration) configuration;
                    outlinePresenter = c.getOutlinePresenter(this);

                    if (outlinePresenter != null) {
                        outlinePresenter.install(this);
                    }
                }
            }
        };

        IFocusService focusService = (IFocusService) PlatformUI.getWorkbench().getService(IFocusService.class);
        if (focusService != null) {
            focusService.addFocusTracker(viewer.getTextWidget(), "com.reprezen.swagedit.editor.sourceViewer");
        }

        viewer.getTextWidget().addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // detectOutlineLocationChanged();
            }
        });

        viewer.getTextWidget().setData(ISourceViewer.class.getName(), viewer);

        getSourceViewerDecorationSupport(viewer);

        return viewer;
    }

    public void updateFoldingStructure(Iterable<AbstractNode> newNodes, JsonDocument document) {
        final Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
        final Map<AbstractNode, ProjectionAnnotation> node2Annotations = new HashMap<>();
        for (AbstractNode node : newNodes) {
            if (node.isObject()) {
                Position position = node.getPosition(document, true);
                boolean isCollapsed = false;
                if (oldProjectionAnnotations.get(node) != null) {
                    isCollapsed = oldProjectionAnnotations.get(node).isCollapsed();
                }
                ProjectionAnnotation newAnnotation = new ProjectionAnnotation(isCollapsed);
                newAnnotations.put(newAnnotation, position);
                node2Annotations.put(node, newAnnotation);
            }
        }
        annotationModel.modifyAnnotations(oldProjectionAnnotations.values().toArray(new Annotation[0]), newAnnotations, null);
        oldProjectionAnnotations = node2Annotations;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // ZEN-3411 SwaggerEditor: changes are not saved on closing file
        // only save, no validation
        hack_AbstractTextEditor_doSave(monitor);
        // batch all marker changes into a single delta for ZEN-2736 Refresh live views on swagedit error changes
        new SafeWorkspaceJob("Do save") {
            @Override
            public IStatus doRunInWorkspace(final IProgressMonitor jobMonitor) throws CoreException {

                // need to run it in UI thread because AbstractTextEditor.doSave needs it in
                // TextViewer.setEditable()
                Shell shell = getSite().getShell();
                if (shell != null && !shell.isDisposed()) {
                    shell.getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            // save + validate
                            JsonEditor.super.doSave(jobMonitor);
                        }
                    });
                }
                // ZEN-4351 Invalid Thread Access Exception thrown when saving documents
                // Don't use the same monitor for the validation as it makes checks for monitor.isCancelled()
                // The monitor passed to the doSave() operation is `EventLoopProgressMonitor` whose operations
                // must run in the Main thread
                runValidate(false);
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public void doSaveAs() {
        // ZEN-3411 SwaggerEditor: changes are not saved on closing file
        // SaveAs only, no validation
        performSaveAs(getProgressMonitor());
        // batch all marker changes into a single delta for ZEN-2736 Refresh live views on swagedit error changes
        new SafeWorkspaceJob("Do save as") {
            @Override
            public IStatus doRunInWorkspace(IProgressMonitor monitor) throws CoreException {
                // AbstractDecoratedTextEditor.performSaveAs() invoked by doSaveAs() needs to be executed in SWT thread
                Shell shell = getSite().getShell();
                if (shell != null && !shell.isDisposed()) {
                    shell.getDisplay().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            // save + validate
                            JsonEditor.super.doSaveAs();
                        }
                    });
                }
                // ZEN-4351 Invalid Thread Access Exception thrown when saving documents
                // Don't use the same monitor for the validation as it makes checks for monitor.isCancelled()
                // The monitor passed to the doSave() operation is `EventLoopProgressMonitor` whose operations
                // must run in the Main thread
                runValidate(false);
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    /*
     * Copy of AbstractTextEditor#doSave(IProgressMonitor) which is shadowed by YEdit. Saves the file, but does NOT
     * perform any validation. The validation should be done in a workspace job
     */
    private void hack_AbstractTextEditor_doSave(IProgressMonitor progressMonitor) {

        IDocumentProvider p = getDocumentProvider();
        if (p == null)
            return;

        if (p.isDeleted(getEditorInput())) {

            if (isSaveAsAllowed()) {

                /*
                 * 1GEUSSR: ITPUI:ALL - User should never loose changes made in the editors. Changed Behavior to make
                 * sure that if called inside a regular save (because of deletion of input element) there is a way to
                 * report back to the caller.
                 */
                performSaveAs(progressMonitor);

            } else {

                Shell shell = getSite().getShell();
                String title = "Cannot Save";
                String msg = "The file has been deleted or is not accessible.";
                MessageDialog.openError(shell, title, msg);
            }

        } else {
            updateState(getEditorInput());
            validateState(getEditorInput());
            performSave(false, progressMonitor);
        }
    }

    protected ValidationOperation createValidationOperation(boolean parseFileContents) {
        return new ValidationOperation(createValidator(), this, parseFileContents);
    }

    protected void runValidate(final boolean onOpen) {
        ValidationOperation validationOperation = createValidationOperation(onOpen);
        SafeWorkspaceJob validationJob = new SafeWorkspaceJob("Update KaiZen Editor validation markers") {
            @Override
            public IStatus doRunInWorkspace(IProgressMonitor monitor) throws CoreException {
                validationOperation.run(monitor);
                return Status.OK_STATUS;
            }

            @Override
            public boolean belongsTo(Object family) {
                if (family instanceof ValidationOperation) {
                    return getEditorInput().equals(((ValidationOperation) family).getEditorInput());
                }
                return false;
            }
        };
        Job.getJobManager().cancel(validationOperation);
        validationJob.schedule();
    }

    protected static void clearMarkers(IFile file) {
        int depth = IResource.DEPTH_INFINITE;
        try {
            file.deleteMarkers(IMarker.PROBLEM, true, depth);
        } catch (CoreException e) {
            YEditLog.logException(e);
            YEditLog.logger.warning("Failed to delete markers:\n" + e.toString());
        }
    }

    public void redrawViewer() {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                if (getSourceViewer() != null) {
                    getSourceViewer().getTextWidget().redraw();
                }
            }
        });
    }

    @Override
    public void addDocumentIdleListener(IDocumentIdleListener listener) {
        super.addDocumentIdleListener(listener);
    }

    /**
     * 
     * WorkspaceJob which does not show an error dialog in case of an exception, but reports it to the error log
     */
    protected abstract class SafeWorkspaceJob extends WorkspaceJob {

        public SafeWorkspaceJob(String name) {
            super(name);
            setPriority(Job.INTERACTIVE);
            IEditorInput editorInput = JsonEditor.this.getEditorInput();
            if (editorInput != null && editorInput instanceof FileEditorInput) {
                setRule(((FileEditorInput) editorInput).getFile());
            }
        }

        @Override
        public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
            try {
                return doRunInWorkspace(monitor);
            } catch (CoreException e) {
                return e.getStatus();
            } catch (OperationCanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (Exception e) {
                // in case of an exception the Worker treats it with
                // an ERROR status in org.eclipse.core.internal.jobs.Worker.handleException(InternalJob, Throwable)
                // and shows a modal dialog by WorkbenchStatusDialogManagerImpl
                YEditLog.logException(e);
                return Status.CANCEL_STATUS;
            } finally {
                monitor.done();
            }
        }

        protected abstract IStatus doRunInWorkspace(IProgressMonitor monitor) throws CoreException;
    }

    @Override
    protected void initializeKeyBindingScopes() {
        setKeyBindingScopes(new String[] { CONTEXT });
    }

    @Override
    public boolean show(ShowInContext context) {
        ISelection selection = context.getSelection();

        if (selection instanceof IStructuredSelection) {
            Object selected = ((IStructuredSelection) selection).getFirstElement();

            if (selected instanceof AbstractNode) {
                Position position = ((AbstractNode) selected).getPosition(getSourceViewer().getDocument());
                selectAndReveal(position.getOffset(), position.getLength());
                return true;
            }
        }

        return false;
    }

    @Override
    public ShowInContext getShowInContext() {
        return new ShowInContext(getEditorInput(), new StructuredSelection());
    }

    protected abstract Validator createValidator();

    private class JsonPreferenceChangeListener implements IPropertyChangeListener {

        private final List<String> colorPreferenceKeys = new ArrayList<>();
        {
            colorPreferenceKeys.add(PreferenceConstants.COLOR_COMMENT);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_COMMENT);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_COMMENT);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_COMMENT);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_KEY);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_KEY);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_KEY);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_KEY);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_SCALAR);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_SCALAR);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_SCALAR);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_SCALAR);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_DEFAULT);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_DEFAULT);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_DEFAULT);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_DEFAULT);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_DOCUMENT);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_DOCUMENT);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_DOCUMENT);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_DOCUMENT);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_ANCHOR);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_ANCHOR);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_ANCHOR);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_ANCHOR);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_ALIAS);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_ALIAS);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_ALIAS);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_ALIAS);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_TAG_PROPERTY);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_TAG_PROPERTY);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_TAG_PROPERTY);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_TAG_PROPERTY);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_INDICATOR_CHARACTER);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_INDICATOR_CHARACTER);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_INDICATOR_CHARACTER);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_INDICATOR_CHARACTER);

            colorPreferenceKeys.add(PreferenceConstants.COLOR_CONSTANT);
            colorPreferenceKeys.add(PreferenceConstants.BOLD_CONSTANT);
            colorPreferenceKeys.add(PreferenceConstants.ITALIC_CONSTANT);
            colorPreferenceKeys.add(PreferenceConstants.UNDERLINE_CONSTANT);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (colorPreferenceKeys.contains(event.getProperty())) {
                if (getSourceViewer() instanceof SourceViewer) {
                    ((SourceViewer) getSourceViewer()).unconfigure();
                    initializeEditor();
                    getSourceViewer().configure(sourceViewerConfiguration);
                }
            }
        }
    };

}