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
package com.reprezen.swagedit.openapi3.validation

import com.reprezen.swagedit.openapi3.editor.OpenApi3Document
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema
import java.net.URI
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import org.junit.Ignore

class NullValueValidationTest {

	val validator = ValidationHelper.validator()
	val document = new OpenApi3Document(new OpenApi3Schema)

	@Ignore
	@Test
	def void testErrorOnNullValueForType() {
		val content = '''
			openapi: "3.0.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			components:
			  schemas:
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
		assertThat(errors.head.getMessage(),
			containsString('The null value is not allowed for type, did you mean the "null" string (quoted)?'))

		assertThat(errors.head.offset, equalTo(document.getLineOffset(11)))
	}

	@Test
	def void testOkOnNullStringForType() {
		val content = '''
			openapi: "3.0.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			components:
			  schemas:
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
			openapi: "3.0.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			components:
			  schemas:
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
			openapi: "3.0.0"
			info:
			  title: Simple API overview
			  version: v2
			
			paths:
			  /myurl:
			    get:
			      responses:
			        200:
			          description: desc
			          content:
			            application/json:
			              example: |-
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
			openapi: "3.0.0"
			info:
			  version: 1.0.0
			  title: some definitions
			
			paths: {}
			
			components:
			  schemas:
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
		assertThat(errors.findFirst[true].offset, equalTo(document.getLineOffset(11)))
	}

}
