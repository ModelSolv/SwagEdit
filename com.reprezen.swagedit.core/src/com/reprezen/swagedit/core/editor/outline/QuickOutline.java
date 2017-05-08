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
package com.reprezen.swagedit.core.editor.outline;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.ShowInContext;

import com.google.common.base.Strings;
import com.reprezen.swagedit.core.Messages;
import com.reprezen.swagedit.core.editor.JsonEditor;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.schema.CompositeSchema;
import com.reprezen.swagedit.core.utils.DocumentUtils;
import com.reprezen.swagedit.core.utils.SwaggerFileFinder;
import com.reprezen.swagedit.core.utils.SwaggerFileFinder.Scope;

public abstract class QuickOutline extends PopupDialog
        implements IInformationControl, IInformationControlExtension, IInformationControlExtension2 {

    public static final String COMMAND_ID = "com.reprezen.swagedit.core.commands.quickoutline";

    private Scope currentScope = Scope.LOCAL;
    private TreeViewer treeViewer;
    private JsonEditor editor;
    private Text filterText;
    private TriggerSequence triggerSequence;
    private String bindingKey;
    private Timer filterTimer = null;

	private final String fileContentType;

    abstract protected CompositeSchema getSchema();

    public QuickOutline(Shell parent, JsonEditor editor, String fileContentType) {
        super(parent, PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, true, true, true, true, true, null, null);
		this.fileContentType = fileContentType;

        IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
        this.bindingKey = bindingService.getBestActiveBindingFormattedFor(COMMAND_ID);
        this.triggerSequence = bindingService.getBestActiveBindingFor(COMMAND_ID);
        this.editor = editor;

        setInfoText(statusMessage());
        create();
    }

    @Override
    protected Control createTitleControl(Composite parent) {
        filterText = new Text(parent, SWT.NONE);
        Dialog.applyDialogFont(filterText);

        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.CENTER;
        filterText.setLayoutData(data);

        filterText.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (isInvocationEvent(e)) {
                    e.doit = false;
                    handleMultiView();
                } else if (e.keyCode == SWT.CR) {
                    handleSelection();
                    QuickOutline.this.close();
                } else if (e.keyCode == SWT.ARROW_DOWN) {
                    treeViewer.getTree().setFocus();
                } else if (e.keyCode == SWT.ARROW_UP) {
                    treeViewer.getTree().setFocus();
                } else if (e.character == SWT.ESC) {
                    QuickOutline.this.close();
                }
            }

            public void keyReleased(KeyEvent e) {
                // do nothing
            }
        });

        filterText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                // refresh tree to apply filter

                if (filterTimer != null) {
                    filterTimer.cancel();
                }
                // reset the timer each time there is a
                // text modification, so that only the last
                // one will be executed.
                filterTimer = new Timer();
                filterTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (filterText.isDisposed()) {
                            return;
                        }

                        // Make sure we access the text in the correct thread.
                        filterText.getDisplay().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                // refreshing the tree will execute the filter.
                                if (Strings.emptyToNull(filterText.getText()) == null) {
                                    treeViewer.refresh();
                                    treeViewer.collapseAll();
                                } else {
                                    treeViewer.refresh();
                                    TreeItem[] items = treeViewer.getTree().getItems();
                                    if (items != null && items.length > 0) {
                                        treeViewer.getTree().setSelection(items[0]);
                                        treeViewer.getTree().showItem(items[0]);
                                    } else {
                                        treeViewer.setSelection(StructuredSelection.EMPTY);
                                    }
                                    treeViewer.expandAll();
                                }
                            }
                        });
                    }
                }, 500);
            }
        });
        return filterText;
    }

    protected void handleMultiView() {
        currentScope = currentScope.next();
        SwaggerFileFinder fileFinder = new SwaggerFileFinder(fileContentType);
        IEditorInput input = editor.getEditorInput();

        IFile currentFile = null;
        if (input instanceof IFileEditorInput) {
            currentFile = ((IFileEditorInput) input).getFile();
        }

        Iterable<IFile> files = fileFinder.collectFiles(currentScope, currentFile);
        setInfoText(statusMessage());

        if (currentScope == Scope.LOCAL) {
            treeViewer.setAutoExpandLevel(2);
        } else {
            treeViewer.setAutoExpandLevel(0);
        }

        setInput(Model.parseYaml(files, getSchema()));
    }

    protected String statusMessage() {
        switch (currentScope) {
        case PROJECT:
            return String.format(Messages.outline_proposal_workspace, bindingKey);
        case WORKSPACE:
            return String.format(Messages.outline_proposal_local, bindingKey);
        default: // LOCAL
            return String.format(Messages.outline_proposal_project, bindingKey);
        }
    }

    protected boolean isInvocationEvent(KeyEvent e) {
        int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
        KeySequence keySequence = KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));

        return keySequence.startsWith(triggerSequence, true);
    }

    protected TreeViewer createTreeViewer(Composite parent) {
        final Tree tree = new Tree(parent, SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = tree.getItemHeight();
        tree.setLayoutData(gd);

        final TreeViewer treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(new OutlineContentProvider());
        treeViewer.setLabelProvider(new OutlineStyledLabelProvider());
        treeViewer.addFilter(new NamePatternFilter());
        // Using ALL_LEVELS will cause editor to hang on large specs
        treeViewer.setAutoExpandLevel(2);
        treeViewer.setUseHashlookup(true);

        tree.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (isInvocationEvent(e)) {
                    e.doit = false;
                    handleMultiView();
                } else if (e.keyCode == SWT.CR) {
                    handleSelection();
                    QuickOutline.this.close();
                }
            }
        });
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                super.mouseDown(e);
            }

            public void mouseUp(MouseEvent e) {
                if (tree.getSelectionCount() < 1) {
                    return;
                }
                if (e.button != 1) {
                    return;
                }

                if (tree.equals(e.getSource())) {
                    Object o = tree.getItem(new Point(e.x, e.y));
                    TreeItem selection = tree.getSelection()[0];
                    if (selection.equals(o)) {
                        handleSelection();
                    }
                }
            }
        });
        tree.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                // do nothing
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                handleSelection();
            }
        });

        return treeViewer;
    }

    protected void handleSelection() {
        ITreeSelection selection = (ITreeSelection) treeViewer.getSelection();

        if (selection != null) {
            Object element = selection.getFirstElement();

            if (element instanceof AbstractNode) {
                Model model = ((AbstractNode) element).getModel();

                if (model.getPath() != null) {
                    DocumentUtils.openAndReveal(model.getPath(), selection);
                } else {
                    editor.show(new ShowInContext(null, selection));
                }
            }
        }
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        treeViewer = createTreeViewer(parent);
        return treeViewer.getControl();
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof Model) {
            Model model = (Model) input;
            if (model.getPath() == null) {
                IFile currentFile = null;
                IEditorInput editorInput = editor.getEditorInput();

                if (editorInput instanceof IFileEditorInput) {
                    currentFile = ((IFileEditorInput) editorInput).getFile();
                }
                if (currentFile != null) {
                    model.setPath(currentFile.getFullPath());
                }
            }
        }

        treeViewer.setInput(input);
        treeViewer.setSelection(null, true);
    }

    @Override
    public boolean hasContents() {
        if (treeViewer == null || treeViewer.getInput() == null) {
            return false;
        }
        return true;
    }

    @Override
    public void setInformation(String information) {
    }

    @Override
    public void setSizeConstraints(int maxWidth, int maxHeight) {
        getShell().setSize(maxWidth, maxHeight);
    }

    @Override
    public Point computeSizeHint() {
        return getShell().getSize();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            open();
        } else {
            saveDialogBounds(getShell());
            getShell().setVisible(false);
        }
    }

    @Override
    public void setSize(int width, int height) {
        getShell().setSize(width, height);
    }

    @Override
    public void setLocation(Point location) {
        getShell().setLocation(location);
    }

    @Override
    public void dispose() {
        close();
    }

    @Override
    public void addDisposeListener(DisposeListener listener) {
        getShell().addDisposeListener(listener);
    }

    @Override
    public void removeDisposeListener(DisposeListener listener) {
        getShell().removeDisposeListener(listener);
    }

    @Override
    public void setForegroundColor(Color foreground) {
        applyForegroundColor(foreground, getContents());
    }

    @Override
    public void setBackgroundColor(Color background) {
        applyBackgroundColor(background, getContents());
    }

    @Override
    public boolean isFocusControl() {
        return treeViewer.getTree().isFocusControl() || filterText.isFocusControl();
    }

    @Override
    public void setFocus() {
        getShell().forceFocus();
        if (filterText != null) {
            filterText.setFocus();
        }
    }

    @Override
    public void addFocusListener(FocusListener listener) {
        getShell().addFocusListener(listener);
    }

    @Override
    public void removeFocusListener(FocusListener listener) {
        getShell().removeFocusListener(listener);
    }

    private boolean matchesFilter(Object element) {
        if (treeViewer == null) {
            return true;
        }

        if (element instanceof AbstractNode) {
            String matchName = ((AbstractNode) element).getText();
            String text = filterText.getText();

            if (Strings.emptyToNull(text) == null) {
                return true;
            }

            if (matchName != null) {
                return matchName.toLowerCase().contains(text.toLowerCase());
            }
        }

        return false;
    }

    public class NamePatternFilter extends ViewerFilter {

        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            if (viewer == null || matchesFilter(element)) {
                return true;
            }

            return hasUnfilteredChild((TreeViewer) viewer, element);
        }

        /*
         * Returns true if one of it's children is a filter element.
         */
        private boolean hasUnfilteredChild(TreeViewer viewer, Object element) {
            Object[] children = ((OutlineContentProvider) viewer.getContentProvider()).getChildren(element);
            for (Object o : children) {
                if (select(viewer, element, o)) {
                    return true;
                }
            }
            return false;
        }
    }

}
