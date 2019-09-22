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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;

import com.reprezen.swagedit.core.model.Model;

public class JsonReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    private IDocument document;
    private JsonEditor editor;

    @Override
    public void setProgressMonitor(IProgressMonitor monitor) {
    }

    @Override
    public void initialReconcile() {
        calculatePositions();
        if (editor != null) {
            editor.redrawViewer();
        }
    }

    @Override
    public void setDocument(IDocument document) {
        this.document = document;
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        initialReconcile();
    }

    @Override
    public void reconcile(IRegion partition) {
        initialReconcile();
    }

    protected void calculatePositions() {
        if (!(document instanceof JsonDocument))
            return;

        final Model model = ((JsonDocument) document).getModel();
    
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                editor.updateFoldingStructure(model.allNodes(), (JsonDocument) document);
            }
        });
    }

    public void setEditor(JsonEditor editor) {
        this.editor = editor;
    }

}
