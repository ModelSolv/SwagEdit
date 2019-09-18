/*******************************************************************************
 * Copyright © 2013, 2019 Modelsolv, Inc.
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of ModelSolv, Inc. See the file license.html in the root directory of
 * this project for further information.
 *******************************************************************************/
package com.reprezen.swagedit.core.validation;

import org.dadacoalition.yedit.YEditLog;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.editor.JsonEditor;

public class Markers {

    public static final String DOCUMENT_VERSION_MARKER = "kaizen.version.marker";

    public static void addMarker(JsonEditor editor, IFile target, SwaggerError error) {
        try {
            IDocumentProvider provider = editor.getDocumentProvider();
            JsonDocument document = (JsonDocument) provider.getDocument(editor.getEditorInput());

            IMarker marker = target.createMarker(IMarker.PROBLEM);
            error.asMarker(document, marker);

            if (!error.getMarkerAttributes().isEmpty()) {
                marker.setAttribute(DOCUMENT_VERSION_MARKER, document.getVersion().name());

                error.getMarkerAttributes().forEach((key, value) -> {
                    try {
                        marker.setAttribute(key, value);
                    } catch (CoreException e) {
                        YEditLog.logException(e);
                    }
                });
            }
        } catch (CoreException e) {
            YEditLog.logException(e);
            YEditLog.logger.warning("Failed to create marker for syntax error: \n" + e.toString());
        }
    }

}
