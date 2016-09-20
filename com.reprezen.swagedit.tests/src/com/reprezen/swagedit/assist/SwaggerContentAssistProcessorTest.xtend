package com.reprezen.swagedit.assist

import com.reprezen.swagedit.editor.SwaggerDocument
import java.util.ArrayList
import org.junit.Test

import static com.reprezen.swagedit.tests.utils.Cursors.*
import static org.hamcrest.core.IsCollectionContaining.*
import static org.junit.Assert.*

class SwaggerContentAssistProcessorTest {

	val processor = new SwaggerContentAssistProcessor() {
		override protected initTextMessages() {
			new ArrayList
		}

		override protected getContextTypeRegistry() {
			null
		}

		override protected geTemplateStore() {
			null
		}
	}

	@Test
	def shouldProvideAllRoot_OnEmptyDocument() {
		val document = new SwaggerDocument
		val test = setUpContentAssistTest(
			'''<1>''',
			document
		)

		val proposals = test.apply(processor, "1")
		val expected = #[
			"swagger:",
			"info:",
			"host:",
			"basePath:",
			"schemes:",
			"consumes:",
			"produces:",
			"paths:",
			"definitions:",
			"parameters:",
			"responses:",
			"security:",
			"securityDefinitions:",
			"tags:",
			"externalDocs:",
			"x-:"
		]

		assertEquals(expected.size, proposals.length)
		assertTrue(proposals.forall[it|expected.contains((it as StyledCompletionProposal).replacementString)])
	}

	@Test
	def shouldProvideEndOfWord() {
		val document = new SwaggerDocument
		val test = setUpContentAssistTest('''swa<1>''', document)

		val proposals = test.apply(processor, "1")
		assertEquals(1, proposals.length);

		val proposal = proposals.get(0)
		proposal.apply(document)
		assertEquals("swagger:", document.get())
		
		val test2 = setUpContentAssistTest('''s<1>''', document)

		val proposals2 = test2.apply(processor, "1")		
		assertEquals(1, proposals2.length);

		val proposal2 = proposals2.get(0)
		proposal2.apply(document)

		assertEquals("swagger:", document.get())
	}

	@Test
	def void test() {
		val document = new SwaggerDocument
		val test = setUpContentAssistTest('''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: Swagger Petstore
			  license:
			    <1>
		''', document)

		val proposals = test.apply(processor, "1")		
		assertThat(proposals.map[(it as StyledCompletionProposal).replacementString], 
			hasItems(
				"name:",
				"url:",
				"x-:"
			))
	}

	@Test
	def void test2() {
		val document = new SwaggerDocument
		val test = setUpContentAssistTest('''
			paths:
			  /pets:    
			    get:
			      summary: List all pets
			      operationId: listPets
			      tags:
			        - pets
			        - ds        
			      parameters:
			        <1>
			        - name: ad
			          <2>
			        - name: limit
			          in: query
			          <3>
		''', document)

		var proposals = test.apply(processor, "1")
		assertThat(proposals.map[(it as StyledCompletionProposal).replacementString], 
			hasItems(
				"-"
			))

		proposals = test.apply(processor, "2")
		assertThat(proposals.map[(it as StyledCompletionProposal).replacementString], 
			hasItems(
				"uniqueItems:",
				"format:",
				"default:",
				"maxItems:",
				"$ref:",
				"schema:",
				"maximum:",
				"required:",
				"collectionFormat:",
				"allowEmptyValue:",
				"minLength:",
				"maxLength:"			
			))
		
		proposals = test.apply(processor, "3")
		assertThat(proposals.map[(it as StyledCompletionProposal).replacementString], 
			hasItems(
				"uniqueItems:",
				"format:",
				"default:",
				"maxItems:",
				"$ref:",
				"schema:",
				"maximum:",
				"required:",
				"collectionFormat:",
				"allowEmptyValue:",
				"minLength:",
				"maxLength:"			
			))
	}

}