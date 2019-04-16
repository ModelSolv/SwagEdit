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
package com.reprezen.swagedit.core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.ITextEditor;

public class DocumentUtils {

    /**
     * Returns the currently active editor.
     * 
     * @return editor
     */
    public static FileEditorInput getActiveEditorInput() {
        IEditorInput input = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor()
                .getEditorInput();

        return input instanceof FileEditorInput ? (FileEditorInput) input : null;
    }
    
    public static URI getActiveEditorInputURI() {
    	final FileEditorInput activeEditorInput = getActiveEditorInput();
    	return activeEditorInput != null ? activeEditorInput.getURI() : null;
    }

    /**
     * Returns the swagger document if exists for the given path.
     * 
     * @param path
     * @return document
     * @throws IOException 
     */
    public static String getDocumentContent(IPath path) throws IOException {
        if (path == null || !path.getFileExtension().matches("ya?ml")) {
            return null;
        }

        InputStream content = null;
        IFile file = getWorkspaceFile(path);
        if (file == null) {
            IFileStore store = getExternalFile(path);
            if (store != null) {
                try {
                    content = store.openInputStream(EFS.NONE, null);
                } catch (CoreException e) {
                    content = null;
                }
            }
        } else if (file.exists()) {
            try {
                content = file.getContents();
            } catch (CoreException e) {
                content = null;
            }
        }

        if (content == null) {
            return null;
        }

        return StringUtils.toString(content);
    }

    /**
     * @param uri
     *            - URI, representing an absolute path
     * @return
     */
    public static IFile getWorkspaceFile(URI uri) {
        return getWorkspaceFile(new Path(uri.getPath()));
    }

    /**
     * @param path
     *            - absolute path to the element
     * @return
     */
    public static IFile getWorkspaceFile(IPath path) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

        try {
            return root.getFileForLocation(path);
        } catch (Exception e) {
            return null;
        }
    }

    public static IFileStore getExternalFile(IPath path) {
        IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
        IFileInfo fileInfo = fileStore.fetchInfo();

        return fileInfo != null && fileInfo.exists() ? fileStore : null;
    }

    /**
     * Opens the editor for the given file and reveal the given region.
     * 
     * @param file
     * @param region
     */
    public static void openAndReveal(IFile file, IRegion region) {
        final IEditorPart editor = openEditor(file);
        if (editor instanceof ITextEditor) {
            if (region != null) {
                ((ITextEditor) editor).selectAndReveal(region.getOffset(), region.getLength());
            }
        }
    }

    /**
     * Opens the editor for the file located at the given path and reveal the selection.
     * 
     * @param path
     * @param selection
     */
    public static void openAndReveal(IPath path, ISelection selection) {
        final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final IFile file = root.getFile(path);
        final IEditorPart editor = openEditor(file);

        if (editor instanceof IShowInTarget) {
            IShowInTarget showIn = (IShowInTarget) editor;
            showIn.show(new ShowInContext(null, selection));
        }
    }

    protected static IEditorPart openEditor(IFile file) {
        final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            return IDE.openEditor(page, file);
        } catch (PartInitException e) {
            return null;
        }
    }
}
