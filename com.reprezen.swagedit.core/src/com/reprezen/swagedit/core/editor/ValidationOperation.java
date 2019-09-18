/*******************************************************************************
 * Copyright (c) 2018 ModelSolv, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.core.editor;

import java.util.Set;

import org.dadacoalition.yedit.YEditLog;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.yaml.snakeyaml.error.YAMLException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.reprezen.swagedit.core.validation.Markers;
import com.reprezen.swagedit.core.validation.SwaggerError;
import com.reprezen.swagedit.core.validation.SwaggerErrorFactory;
import com.reprezen.swagedit.core.validation.Validator;

public class ValidationOperation implements IWorkspaceRunnable {

    private final Validator validator;

    private final IEditorInput editorInput;
    private final IDocumentProvider documentProvider;
    private final boolean parseFileContents;
    private final JsonEditor editor;

    private final SwaggerErrorFactory factory = new SwaggerErrorFactory();

    public ValidationOperation(Validator validator, JsonEditor editor, boolean parseFileContents) {
        this.editor = editor;
        this.validator = validator;
        this.editorInput = editor.getEditorInput();
        this.documentProvider = editor.getDocumentProvider();
        this.parseFileContents = parseFileContents;
    }

    @Override
    public void run(IProgressMonitor monitor) throws CoreException {
        // if the file is not part of a workspace it does not seems that it is a
        // IFileEditorInput
        // but instead a FileStoreEditorInput. Unclear if markers are valid for
        // such files.
        if (!(editorInput instanceof IFileEditorInput)) {
            YEditLog.logError("Marking errors not supported for files outside of a project.");
            YEditLog.logger.info("editorInput is not a part of a project.");
            return;
        }

        final IDocument document = documentProvider.getDocument(editorInput);
        if (document instanceof JsonDocument) {
            SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
            final IFileEditorInput fileEditorInput = (IFileEditorInput) editorInput;
            final IFile file = fileEditorInput.getFile();

            if (parseFileContents) {
                // force parsing of yaml to init parsing errors
                // subMonitor.split() should NOT be executed before this code
                // as it checks for job cancellation and we want to be sure that
                // the document is parsed on opening
                ((JsonDocument) document).onChange();
            }
            if (subMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            subMonitor.newChild(20);

            JsonEditor.clearMarkers(file);
            if (subMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            subMonitor.newChild(30);

            validateYaml(file, (JsonDocument) document);
            if (subMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            subMonitor.newChild(20);

            validateSwagger(file, (JsonDocument) document, fileEditorInput);
            if (subMonitor.isCanceled()) {
                throw new OperationCanceledException();
            }
            subMonitor.newChild(30);
        }
    }

    protected void validateYaml(IFile file, JsonDocument document) {
        if (document.getYamlError() instanceof YAMLException) {
            Markers.addMarker(editor, file, //
                    factory.newYamlError(document, (YAMLException) document.getYamlError()));
        }
        if (document.getJsonError() instanceof JsonProcessingException) {
            Markers.addMarker(editor, file, //
                    factory.newJsonError((JsonProcessingException) document.getJsonError()));
        }
    }

    protected void validateSwagger(IFile file, JsonDocument document, IFileEditorInput editorInput) {
        final Set<SwaggerError> errors = validator.validate(document, editorInput);

        for (SwaggerError error : errors) {
            Markers.addMarker(editor, file, error);
        }
    }

    public IEditorInput getEditorInput() {
        return editorInput;
    }

}