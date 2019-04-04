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

import static org.eclipse.ui.IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

import com.fasterxml.jackson.core.JsonPointer;
import com.reprezen.swagedit.core.Activator;
import com.reprezen.swagedit.core.Activator.Icons;
import com.reprezen.swagedit.core.assist.contexts.ContextType;
import com.reprezen.swagedit.core.editor.JsonDocument;
import com.reprezen.swagedit.core.json.references.Messages;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.templates.SwaggerTemplateContext;
import com.reprezen.swagedit.core.utils.StringUtils;
import com.reprezen.swagedit.core.utils.SwaggerFileFinder.Scope;

/**
 * This class provides basic content assist based on keywords used by the swagger schema.
 */
public abstract class JsonContentAssistProcessor extends TemplateCompletionProcessor
        implements IContentAssistProcessor, ICompletionListener {

   private final JsonProposalProvider proposalProvider;
   private final JsonReferenceProposalProvider referenceProposalProvider;
   private final JsonExampleProposalProvider exampleProposalProvider;
   private final ContentAssistant contentAssistant;
   
    /**
     * The pointer that helps us locate the current position of the cursor inside the document.
     * 
     */
    private JsonPointer currentPath = null;

    private Model currentModel = null;

    private int currentOffset = -1;

    /**
     * Current position scope use to retrieve JSON Reference proposals.
     */
    private Scope currentScope = Scope.LOCAL;

    /**
     * True if the proposal is activated on a JSON reference.
     */
    private boolean isRefCompletion = false;

    private String[] textMessages;
    
	public JsonContentAssistProcessor(ContentAssistant ca, String fileContentType) {
		this(ca, new JsonProposalProvider(),
				new JsonReferenceProposalProvider(ContextType.emptyContentTypeCollection(), fileContentType),
				new JsonExampleProposalProvider(ContextType.emptyContentTypeCollection()));
	}

	public JsonContentAssistProcessor(ContentAssistant ca, JsonProposalProvider proposalProvider,
			JsonReferenceProposalProvider referenceProposalProvider,
			JsonExampleProposalProvider exampleProposalProvider) {
		this.contentAssistant = ca;
		this.proposalProvider = proposalProvider;
		this.referenceProposalProvider = referenceProposalProvider;
		this.exampleProposalProvider = exampleProposalProvider;
		this.textMessages = initTextMessages();
	}
    
    protected abstract TemplateStore getTemplateStore();

    protected abstract ContextTypeRegistry getContextTypeRegistry();
    
    protected abstract String getContextTypeId(Model model, String path);


    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
        if (!(viewer.getDocument() instanceof JsonDocument)) {
            return super.computeCompletionProposals(viewer, documentOffset);
        }

        maybeSwitchScope(documentOffset);

        final JsonDocument document = (JsonDocument) viewer.getDocument();
        final ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();
        int line = 0, lineOffset = 0, column = 0;
        try {
            line = document.getLineOfOffset(documentOffset);
            lineOffset = document.getLineOffset(line);
            column = selection.getOffset() - lineOffset;
        } catch (BadLocationException e) {
        }
        
        final String prefix = extractPrefix(viewer, documentOffset);
        // we have to remove the length of
        // the prefix to obtain the correct
        // column to resolve the path
        if (!prefix.isEmpty()) {
            column -= prefix.length();
        }

        currentModel = document.getModel(documentOffset - prefix.length());
        currentPath = currentModel.getPath(line, column);
        isRefCompletion = referenceProposalProvider.canProvideProposal(currentModel, currentPath);

        Collection<ProposalDescriptor> kaizenProposals;
        if (isRefCompletion) {
            updateStatus();
            kaizenProposals = referenceProposalProvider.getProposals(currentPath, document, currentScope);
        } else {
        	final boolean isExampleCompletion = exampleProposalProvider.canProvideProposal(currentModel, currentPath);
        	if (isExampleCompletion) {
        		//TODO: Update or clear status ???????
        		kaizenProposals = exampleProposalProvider.getProposals(currentPath, document, currentScope);
        	} else {
        		clearStatus();
        		kaizenProposals = proposalProvider.getProposals(currentPath, currentModel, prefix);
        	}
        	
        }
   
        final Collection<ICompletionProposal> proposals = getCompletionProposals(kaizenProposals, prefix, documentOffset, selection.getText());
        // compute template proposals only if not trying to propose references
        if (!isRefCompletion) {
            List<ICompletionProposal> templateProposals = filterTemplateProposals(
                    super.computeCompletionProposals(viewer, documentOffset), prefix);
            if (!templateProposals.isEmpty()) {
                proposals.addAll(templateProposals);
            }
        }

        return proposals.toArray(new ICompletionProposal[proposals.size()]);
    }

    /*
     * Returns template proposals that contain the current prefix if present, otherwise returns all template proposals.
     */
    private List<ICompletionProposal> filterTemplateProposals(ICompletionProposal[] templateProposals, String prefix) {
        if (templateProposals != null && templateProposals.length > 0) {
            List<ICompletionProposal> proposals = Arrays.asList(templateProposals);
            if (!prefix.isEmpty()) {
                return proposals.stream() //
                        .filter(e -> e.getDisplayString().toLowerCase().contains(prefix.toLowerCase())) //
                        .collect(Collectors.toList());
            }
            return proposals;
        } else {
            return Collections.emptyList();
        }
    }

    private void maybeSwitchScope(int documentOffset) {
        // computeCompletionProposals() is called on Ctrl+space or on typing a new character
        if (isRefCompletion && (currentOffset == documentOffset)) {
            currentScope = currentScope.next();
        }
        currentOffset = documentOffset;
    }

    protected void updateStatus() {
        if (contentAssistant != null) {
            if (textMessages == null) {
                textMessages = initTextMessages();
            }
            contentAssistant.setStatusLineVisible(true);
            contentAssistant.setStatusMessage(textMessages[currentScope.getValue()]);
        }
    }

    protected void clearStatus() {
        if (contentAssistant != null) {
            contentAssistant.setStatusLineVisible(false);
        }
    }

    protected String[] initTextMessages() {
		IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		String bindingKey = bindingService.getBestActiveBindingFormattedFor(EDIT_CONTENT_ASSIST);
        ContextType contextType = currentModel != null
                ? referenceProposalProvider.getContextTypes().get(currentModel, getCurrentPath())
                : null;
		String context = contextType != null ? contextType.label() : "";

		return new String[] { //
				String.format(Messages.content_assist_proposal_project, bindingKey, context),
				String.format(Messages.content_assist_proposal_workspace, bindingKey, context),
				String.format(Messages.content_assist_proposal_local, bindingKey, context) };
	}

    protected Collection<ICompletionProposal> getCompletionProposals(Collection<ProposalDescriptor> proposals,
            String prefix, int offset, String selection) {
        final List<ICompletionProposal> result = new ArrayList<>();

        prefix = StringUtils.emptyToNull(prefix);

        for (ProposalDescriptor proposal : proposals) {
            int selectionLength = selection == null ? 0 : selection.length();
            StyledCompletionProposal styledProposal = StyledCompletionProposal.create(proposal, prefix, offset,
                    selectionLength);
            if (styledProposal != null) {
                result.add(styledProposal);
            }
        }

        return result;
    }

    @Override
    protected String extractPrefix(ITextViewer viewer, int offset) {
        int i = offset;
        IDocument document = viewer.getDocument();
        if (i > document.getLength())
            return ""; //$NON-NLS-1$

        try {
            while (i > 0) {
                char ch = document.getChar(i - 1);
                if (Character.isWhitespace(ch))
                    break;
                i--;
            }

            return document.get(i, offset - i);
        } catch (BadLocationException e) {
            return ""; //$NON-NLS-1$
        }
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {};
    }

    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

	@Override
	protected ICompletionProposal createProposal(Template template, TemplateContext context, IRegion region,
			int relevance) {
        if (context instanceof DocumentTemplateContext) {
            context = new SwaggerTemplateContext((DocumentTemplateContext) context);
        }
		return new StyledTemplateProposal(template, context, region, getImage(template), getTemplateLabel(template),
				relevance);
	}

    @Override
    protected Template[] getTemplates(String contextTypeId) {
        return getTemplateStore().getTemplates();
    }

    @Override
    protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
        String contextType = getContextTypeId(currentModel, currentPath.toString());
        ContextTypeRegistry registry = getContextTypeRegistry();
        if (registry != null) {
            return registry.getContextType(contextType);
        } else {
            return null;
        }
    }

    @Override
    protected Image getImage(Template template) {
       return Activator.getDefault().getImage(Icons.template_item);
    }

    protected StyledString getTemplateLabel(Template template) {
        Styler nameStyle = new StyledString.Styler() {
            @Override
            public void applyStyles(TextStyle textStyle) {
                textStyle.foreground = new Color(Display.getCurrent(), new RGB(80, 80, 255));
            }
        };
        Styler descriptionStyle = new StyledString.Styler() {
            @Override
            public void applyStyles(TextStyle textStyle) {
                textStyle.foreground = new Color(Display.getCurrent(), new RGB(120, 120, 120));
            }
        };

        return new StyledString(template.getName(), nameStyle).append(": ", descriptionStyle)
                .append(template.getDescription(), descriptionStyle);
    }

    @Override
    public void assistSessionStarted(ContentAssistEvent event) {
        resetScope();
    }

    @Override
    public void assistSessionEnded(ContentAssistEvent event) {
        resetScope();
    }

    private void resetScope() {
        currentScope = Scope.LOCAL;
        isRefCompletion = false;
        textMessages = null;
        currentOffset = -1;
    }
    
    protected JsonPointer getCurrentPath() {
    	return currentPath;
    }

    @Override
    public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
    }

}