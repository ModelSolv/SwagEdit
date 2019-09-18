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
package com.reprezen.swagedit.core.assist;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.swagedit.core.assist.ext.ContentAssistExt;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.schema.ArrayTypeDefinition;
import com.reprezen.swagedit.core.schema.ComplexTypeDefinition;
import com.reprezen.swagedit.core.schema.JsonType;
import com.reprezen.swagedit.core.schema.MultipleTypeDefinition;
import com.reprezen.swagedit.core.schema.ObjectTypeDefinition;
import com.reprezen.swagedit.core.schema.ReferenceTypeDefinition;
import com.reprezen.swagedit.core.schema.TypeDefinition;
import com.reprezen.swagedit.core.utils.StringUtils;

/**
 * Provider of completion proposals.
 */
public class JsonProposalProvider {

    private final List<ContentAssistExt> extensions;

    public JsonProposalProvider() {
        this.extensions = Collections.emptyList();
    }

    public JsonProposalProvider(ContentAssistExt... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    /**
     * Returns all proposals for the node inside the given model located at the given pointer. Only proposals that
     * starts with the given prefix will be returned.
     * 
     * @param pointer
     * @param model
     * @param prefix
     * @return proposals
     */
    public Collection<ProposalDescriptor> getProposals(JsonPointer pointer, Model model, String prefix) {
        final AbstractNode node = model.find(pointer);
        if (node == null) {
            return Collections.emptyList();
        }
        return getProposals(node.getType(), node, prefix);
    }

    /**
     * Returns all proposals for the node inside the given model located at the given pointer.
     * 
     * @param pointer
     * @param model
     * @return proposals
     */
    public Collection<ProposalDescriptor> getProposals(JsonPointer pointer, Model model) {
        return getProposals(pointer, model, null);
    }

    /**
     * Returns all proposals for the current node.
     * 
     * @param node
     * @return proposals
     */
    public Collection<ProposalDescriptor> getProposals(AbstractNode node) {
        return getProposals(node.getType(), node, null);
    }

    protected Collection<ProposalDescriptor> getProposals(TypeDefinition type, AbstractNode node, String prefix) {
        if (type instanceof ReferenceTypeDefinition) {
            type = ((ReferenceTypeDefinition) type).resolve();
        }

        ContentAssistExt ext = findExtension(type);
        if (ext != null) {
            return ext.getProposals(type, node, prefix);
        }

        switch (type.getType()) {
        case STRING:
        case INTEGER:
        case NUMBER:
            return createPrimitiveProposals(type);
        case BOOLEAN:
            return createBooleanProposals(type);
        case ENUM:
            return createEnumProposals(type, node);
        case ARRAY:
            return createArrayProposals((ArrayTypeDefinition) type, node);
        case OBJECT:
            return createObjectProposals((ObjectTypeDefinition) type, node, prefix);
        case ALL_OF:
        case ANY_OF:
        case ONE_OF:
            return createComplextTypeProposals((ComplexTypeDefinition) type, node, prefix);
        case UNDEFINED:
            Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();
            if (type instanceof MultipleTypeDefinition) {
                for (TypeDefinition currentType : ((MultipleTypeDefinition) type).getMultipleTypes()) {
                    proposals.addAll(getProposals(currentType, node, prefix));
                }
            }
            return proposals;
        }
        return Collections.emptyList();
    }

    protected Collection<ProposalDescriptor> createPrimitiveProposals(TypeDefinition type) {
        String label;
        if (type.getType() == JsonType.UNDEFINED) {
            label = type.getContainingProperty();
        } else {
            label = type.getType().getValue();
        }

        return Arrays.asList(new ProposalDescriptor("").replacementString("").description(type.getDescription()).type(label));
    }

    protected Collection<ProposalDescriptor> createBooleanProposals(TypeDefinition type) {
        Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();

        String labelType = type.getType().getValue();

        proposals.add(new ProposalDescriptor("true").replacementString("true").description(type.getDescription()).type(labelType));
        proposals.add(new ProposalDescriptor("false").replacementString("false").description(type.getDescription()).type(labelType));

        return proposals;
    }

    protected ProposalDescriptor createPropertyProposal(String key, TypeDefinition type) {
        if (type == null) {
            return null;
        }

        if (key == "null") {
            key = "\"" + key + "\"";
        }

        String labelType;
        if (Objects.equals(key, type.getContainingProperty())) {
            labelType = type.getType().getValue();
        } else {
            labelType = type.getContainingProperty();
        }

        return new ProposalDescriptor(key).replacementString(key + ":").description(type.getDescription()).type(labelType);
    }

    protected Collection<ProposalDescriptor> createObjectProposals(ObjectTypeDefinition type, AbstractNode element,
            String prefix) {
        final Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();

        for (String property : type.getProperties().keySet()) {
            ProposalDescriptor proposal = createPropertyProposal(property, type.getProperties().get(property));
            if (proposal != null) {
                if (StringUtils.emptyToNull(prefix) != null && property.startsWith(prefix)) {
                    proposals.add(proposal);
                } else if (element.get(property) == null) {
                    proposals.add(proposal);
                }
            }
        }

        for (String property : type.getPatternProperties().keySet()) {
            TypeDefinition typeDef = type.getPatternProperties().get(property);

            if (property.startsWith("^")) {
                property = property.substring(1);
            }

            ProposalDescriptor proposal = createPropertyProposal(property, typeDef);
            if (proposal != null) {
                proposals.add(proposal);
            }
        }

        if (type.getAdditionalProperties() != null) {
            String elementTitle = type.getLabel();
            String elementName = elementTitle != null? elementTitle : type.getAdditionalProperties().getLabel();
            if (elementName != null) {
                elementName = String.format("(%s name)", elementName);
                proposals.add(new ProposalDescriptor(elementName).replacementString(elementName + ":").selection(elementName));
            }
        }
        if (proposals.isEmpty()) {
            proposals.add(new ProposalDescriptor("_key_").replacementString("_key_" + ":"));
        }
        return proposals;
    }

    protected Collection<ProposalDescriptor> createArrayProposals(ArrayTypeDefinition type, AbstractNode node) {
        Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();

        if (type.itemsType != null && type.itemsType.getType() == JsonType.ENUM) {
            String labelType = type.itemsType.getContainingProperty();

            for (String literal : enumLiterals(type.itemsType)) {
                proposals.add(new ProposalDescriptor(literal).replacementString("- " + literal).description(type.getDescription()).type(labelType));
            }
        } else {
            proposals.add(new ProposalDescriptor("-").replacementString("-").description(type.getDescription()).type("array item"));
        }

        return proposals;
    }

    protected Collection<ProposalDescriptor> createComplextTypeProposals(ComplexTypeDefinition type, AbstractNode node,
            String prefix) {
        final Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();

        for (TypeDefinition definition : type.getComplexTypes()) {
            proposals.addAll(getProposals(definition, node, prefix));
        }

        return proposals;
    }

    protected Collection<String> enumLiterals(TypeDefinition type) {
        Collection<String> literals = new LinkedHashSet<>();
        for (JsonNode literal : type.asJson().get("enum")) {
            literals.add(literal.asText());
        }
        return literals;
    }

    protected Collection<ProposalDescriptor> createEnumProposals(TypeDefinition type, AbstractNode node) {
        final Collection<ProposalDescriptor> proposals = new LinkedHashSet<>();
        final String subType = type.asJson().has("type") ? //
                type.asJson().get("type").asText() : //
                null;

        String replStr;
        for (String literal : enumLiterals(type)) {
            // if the type of array is string and
            // current value is a number, it should be put
            // into quotes to avoid validation issues
        	
            if ((NumberUtils.isNumber(literal) && "string".equals(subType)) || "null".equals(literal)) {
                replStr = "\"" + literal + "\"";
            } else {
                replStr = literal;
            }

            String labelType = type.getType().getValue();

            proposals.add(new ProposalDescriptor(literal).replacementString(replStr).description(type.getDescription()).type(labelType));
        }

        return proposals;
    }

    protected ContentAssistExt findExtension(TypeDefinition type) {
        ContentAssistExt ext = null;
        Iterator<ContentAssistExt> it = extensions.iterator();
        while (ext == null && it.hasNext()) {
            ContentAssistExt current = it.next();
            if (current.canProvideContentAssist(type)) {
                ext = current;
            }
        }
        return ext;
    }
}
