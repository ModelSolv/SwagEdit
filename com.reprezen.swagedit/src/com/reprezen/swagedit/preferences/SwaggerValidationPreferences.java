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
package com.reprezen.swagedit.preferences;

import static com.reprezen.swagedit.preferences.SwaggerPreferenceConstants.EXAMPLE_VALIDATION;
import static com.reprezen.swagedit.preferences.SwaggerPreferenceConstants.VALIDATION_REF_SECURITY_DEFINITIONS_OBJECT;
import static com.reprezen.swagedit.preferences.SwaggerPreferenceConstants.VALIDATION_REF_SECURITY_REQUIREMENTS_ARRAY;
import static com.reprezen.swagedit.preferences.SwaggerPreferenceConstants.VALIDATION_REF_SECURITY_REQUIREMENT_OBJECT;
import static com.reprezen.swagedit.preferences.SwaggerPreferenceConstants.VALIDATION_REF_SECURITY_SCHEME_OBJECT;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.reprezen.swagedit.Activator;

public class SwaggerValidationPreferences extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage, IPropertyChangeListener {

    public SwaggerValidationPreferences() {
        // GRID is needed because we are not attaching the editor fields directly to FieldEditorParent, but to its child
        super(FieldEditorPreferencePage.GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("Swagger preferences for validation");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite composite = new Composite(getFieldEditorParent(), SWT.NONE);
        GridLayoutFactory.fillDefaults().applyTo(composite);
        GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(composite);

        // Example validation

        addField(new BooleanFieldEditor(EXAMPLE_VALIDATION, "Enable examples validation", composite));

        // IMPORTANT: FieldEditorPreferencePage does not work very well with complex layouts and nested widgets.
        // Consider switching to com.modelsolv.reprezen.generators.ui.preferences.GroupedFieldEditorPreferencePage from
        // the RepreZen repo before modifying it
        Composite refValidationComposite = new Composite(composite, SWT.BORDER);
        GridLayoutFactory.fillDefaults().margins(5, 5).applyTo(refValidationComposite);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(refValidationComposite);

        Label header = new Label(refValidationComposite, SWT.NONE);
        header.setText("Allow JSON references in additional contexts:");

        Composite fieldComposite = new Composite(refValidationComposite, SWT.NONE);
        GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(fieldComposite);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(fieldComposite);

        addField(
                new BooleanFieldEditor(VALIDATION_REF_SECURITY_DEFINITIONS_OBJECT, "Security Definitions Object", fieldComposite));
        addField(new BooleanFieldEditor(VALIDATION_REF_SECURITY_SCHEME_OBJECT, "Security Scheme Object", fieldComposite));
        addField(
                new BooleanFieldEditor(VALIDATION_REF_SECURITY_REQUIREMENTS_ARRAY, "Security Requirements Array", fieldComposite));
        addField(
                new BooleanFieldEditor(VALIDATION_REF_SECURITY_REQUIREMENT_OBJECT, "Security Requirement Object", fieldComposite));
    }

}
