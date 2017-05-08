package com.reprezen.swagedit.openapi3.editor;

import org.dadacoalition.yedit.editor.YEditSourceViewerConfiguration;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector;
import org.eclipse.jface.text.source.ISourceViewer;

import com.reprezen.swagedit.core.assist.JsonContentAssistProcessor;
import com.reprezen.swagedit.core.editor.JsonEditor;
import com.reprezen.swagedit.core.editor.JsonSourceViewerConfiguration;
import com.reprezen.swagedit.core.schema.CompositeSchema;
import com.reprezen.swagedit.openapi3.Activator;
import com.reprezen.swagedit.openapi3.assist.OpenApi3ContentAssistProcessor;
import com.reprezen.swagedit.openapi3.hyperlinks.OpenApi3ReferenceHyperlinkDetector;

public class OpenApi3Editor extends JsonEditor {

	public static final String ID = "com.reprezen.swagedit.openapi3.editor";

	public OpenApi3Editor() {
		super(new OpenApi3DocumentProvider(), Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected YEditSourceViewerConfiguration createSourceViewerConfiguration() {
		sourceViewerConfiguration = new OpenApi3SourceViewerConfiguration();
		sourceViewerConfiguration.setEditor(this);
		return sourceViewerConfiguration;
	}

	public static class OpenApi3SourceViewerConfiguration extends JsonSourceViewerConfiguration {

		public OpenApi3SourceViewerConfiguration() {
			super(Activator.getDefault().getPreferenceStore());
		}

		@Override
		protected JsonContentAssistProcessor createContentAssistProcessor(ContentAssistant ca) {
			return new OpenApi3ContentAssistProcessor(ca);
		}

		@Override
		public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
			return new IHyperlinkDetector[] { new URLHyperlinkDetector(), new OpenApi3ReferenceHyperlinkDetector() };
		}
		
		@Override
		protected CompositeSchema getSchema() {
			return Activator.getDefault().getSchema();
		}

		@Override
		protected IInformationControlCreator getOutlineInformationControlCreator() {
			return getOutlineInformationControlCreator(OpenApi3ContentDescriber.CONTENT_TYPE_ID);
		}

	}

}
