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
package com.reprezen.swagedit.validation

import com.reprezen.swagedit.editor.SwaggerDocument
import java.net.URI
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import org.junit.Ignore

class NullValueValidationTest {

	val validator = new SwaggerValidator(null)
	val SwaggerDocument document = new SwaggerDocument

	@Ignore
	@Test
	def void testErrorOnNullValueForType() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			definitions:
			
			  Phones:
			    type: object
			    properties:
			      phoneId:
			        type: null
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertThat(errors.findFirst[true].getMessage(),
			containsString('The null value is not allowed for type, did you mean the "null" string (quoted)?'))
		assertThat(errors.findFirst[true].offset, equalTo(document.getLineOffset(14)))
	}

	@Test
	def void testOkOnNullStringForType() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			definitions:
			
			  Phones:
			    type: object
			    properties:
			      phoneId:
			        type: "null"
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testOkOnNullValueInVendorExtension() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			definitions:
			
			  Phones:
			    type: object
			    properties:
			      phoneId:
			        type: string
			        x-null-prop: null
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testOkOnNullValueInExample() {
		val content = '''
			swagger: "2.0"
			info:
			  title: Simple API overview
			  version: v2
			  
			paths:
			  /myurl:
			    get:
			      responses:
			        200:
			          description: desc
			          examples:
			            application/json: |-
			              {
			                  "nullProperty": null
			              }
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Ignore
	@Test
	def void testErrorOnNullValueForDescription() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			definitions:
			
			  Phones:
			    type: object
			    properties:
			      phoneId:
			        type: string
			        description: null
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertThat(errors.findFirst[true].getMessage(), containsString("value of type null is not allowed"))
		assertThat(errors.findFirst[true].offset, equalTo(document.getLineOffset(15)))
	}

}
