/*******************************************************************************
 *  Copyright (c) 2016 ModelSolv, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 *  Contributors:
 *     ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.openapi3.templates;

import java.util.List;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.templates.SchemaBasedTemplateContextType;

public class OpenApi3ContextType extends TemplateContextType {
    
    private static final String PATH_ITEM_REGEX = "/paths/~1[^/]+";
    // we can use a ? here as both 'PATH_ITEM_REGEX + "/parameters$"' and
    // 'PATH_ITEM_REGEX + "/[^/]+/parameters$"' are supported
    private static final String PARAMETERS_LIST_REGEX = PATH_ITEM_REGEX + "/([^/]+/)?parameters";

    private final String regex;

    public OpenApi3ContextType(String name, String regex) {
        super("com.reprezen.swagedit.openapi3.templates." + name, name);
        this.regex = regex;
        addGlobalResolvers();
    }

    private void addGlobalResolvers() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }

    public static class RootContextType extends OpenApi3ContextType {
        public RootContextType() {
            super("root", "");
        }
    }

    public static class ContactContextType extends OpenApi3ContextType {
        public ContactContextType() {
            super("info.contact", "/info/contact");
        }
    }

    public static class PathsContextType extends OpenApi3ContextType {
        public PathsContextType() {
            super("paths", "/paths");
        }

    }

    public static class PathItemContextType extends OpenApi3ContextType {
        public PathItemContextType() {
            super("path_item", PATH_ITEM_REGEX + "$");
        }
    }

    public static class SchemasContextType extends OpenApi3ContextType {
        public SchemasContextType() {
            super("components.schemas", "/components/schemas");
        }
    }

    public static class SchemaContextType extends SchemaBasedTemplateContextType {
        public SchemaContextType() {
            super("com.reprezen.swagedit.openapi3.templates.schema", "schema", "/definitions/schema");
        }
    }
    
    public static class ParameterObjectContextType extends OpenApi3ContextType {
        public ParameterObjectContextType() {
            super("parameter_object", PARAMETERS_LIST_REGEX + "/\\d+$" + "|"//
                    + "/components/parameters/[^/]+$");
        }
    }

    public static class ResponseObjectContextType extends OpenApi3ContextType {
        public ResponseObjectContextType() {
            super("responses", "/components/responses|" + //
                    PATH_ITEM_REGEX + "/[^/]+/responses$");
        }
    }

    public static class ResponseContentContextType extends OpenApi3ContextType {
        public ResponseContentContextType() {
            super("response", PATH_ITEM_REGEX + "/[^/]+/responses/\\d\\d\\d");
        }
    }

    public static class ComponentsObjectContextType extends OpenApi3ContextType {
        public ComponentsObjectContextType() {
            super("components", "/components");
        }
    }

    public static class CallbacksObjectContextType extends OpenApi3ContextType {
        public CallbacksObjectContextType() {
            super("callbacks", PATH_ITEM_REGEX + "/[^/]+/callbacks|/components/callbacks");
        }
    }

    public static class RequestBodyObjectContextType extends OpenApi3ContextType {
        public RequestBodyObjectContextType() {
            super("requestBody", ".*/requestBody");
        }
    }

    private static List<TemplateContextType> allContextTypes = Lists.<TemplateContextType>newArrayList( //
            new RootContextType(), //
            new ContactContextType(), //
            new PathsContextType(), //
            new PathItemContextType(), //
            new SchemasContextType(), //
            new ParameterObjectContextType(), //
            new ResponseObjectContextType(), //
            new ResponseContentContextType(), //
            new ComponentsObjectContextType(), //
            new CallbacksObjectContextType(), //
            new RequestBodyObjectContextType(), //
            new SchemaContextType());

    public static List<TemplateContextType> allContextTypes() {
        return allContextTypes;
    }
    
    public static List<String> allContextTypeIds() {
        return Lists.transform(allContextTypes, new Function<TemplateContextType, String>() {

            @Override
            public String apply(TemplateContextType input) {
                return input.getId();
            }

        });
    }

    public static TemplateContextType getContextType(final Model model, final String path) {
        
        final String normalizedPath = (path != null && path.endsWith("/")) ? path.substring(0, path.length() - 1)
                : path;
        if (normalizedPath == null || normalizedPath.isEmpty() || "/".equals(normalizedPath)) {
            return new RootContextType();
        }
        return Iterables.getFirst(Iterables.filter(allContextTypes, new Predicate<TemplateContextType>() {

            @Override
            public boolean apply(TemplateContextType input) {
                if (input instanceof OpenApi3ContextType) {
                    return normalizedPath.matches(((OpenApi3ContextType)input).regex);
                    
                }
                if (input instanceof SchemaBasedTemplateContextType) {
                    return ((SchemaBasedTemplateContextType)input).matches(model, path);
                }
                return false;
            }

        }), null);
    }

}
