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

import static org.hamcrest.core.IsCollectionContaining.*
import static org.junit.Assert.*
import org.junit.Ignore

/**
 * Tests as documentation for #9 - User-friendly validation messages
 * The " #validation error marker" are placed right above the place where we expect to see a validation error. 
 * It's for human convenience only and will be ignored by the test.
 */
class ValidationMessageTest {

	val validator = new SwaggerValidator(null)
	val document = new SwaggerDocument

	@Test
	def testMessage_additionalItems_notAllowed() {
		// previous message 'instance type (integer) does not match any allowed primitive type (allowed: ["array"])'
		var expected = 'value of type integer is not allowed, value should be of type array'
		// parameters should contain an array of object
		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		  title: MyModel
		paths:
		  /p:
		    get:
		      #validation error marker
		      parameters: 2
		      responses:
		        '200':
		          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		
		assertEquals(1, errors.size)
		assertEquals(expected, errors.get(0).message)
	}

	@Test
	def testMessage_typeNoMatch() {
		// previous message 'instance type (integer) does not match any allowed primitive type (allowed: ["object"])'
		var expected = 'value of type integer is not allowed, value should be of type object'
		// responses should contain an object
		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		  title: MyModel
		paths:
		  /p:
		    get:     
		      #validation error marker
		      responses: 2
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		
		assertEquals(1, errors.size)
		assertEquals(expected, errors.get(0).message)
	}

	@Test
	def	testMessage_notInEnum() {
		// previous message 'instance value ("foo") not found in enum (possible values: ["http","https","ws","wss"])'
		val expected = 'value foo is not allowed, value should be one of "http", "https", "ws", "wss"'
		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		  title: Simple API
		#validation error marker
		schemes:
		  - http
		  - foo
		paths:
		  /:
		    get:
		      responses:
		        '200':
		          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		
		assertEquals(1, errors.size)
		assertEquals(expected, errors.get(0).message)
	}

	@Ignore
	@Test
	def testMessage_oneOf_fail() {
		// previous message 'instance failed to match exactly one schema (matched 0 out of 2)'

		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		  title: MyModel
		paths:
		  /p:
		    get:
		      responses:
		        '200':
		          #validation error marker
		          description: 200
		'''
		
		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(1, errors.size)
	}

	@Test
	def testMessage_additionalProperties_notAllowed() {
		// previous message 'object instance has properties which are not allowed by the schema: ["description"]'
		val expected = 'object has properties "description" which are not allowed'

		// description should be 2 spaces forward		
		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		  title: MyModel
		paths:
		  /p:
		    get:
		      responses:
		        #validation error marker
		        '200':
		        description: OK
		'''
		
		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertThat(errors.map[message], hasItems(expected))
	}

	@Test
	def testMessage_object_missingMembers() {
		val expected = 'object has missing required properties "title"'
		val content = '''
		swagger: '2.0'
		info:
		  version: 0.0.0
		paths:
		  /:
		    get:
		      responses:
		        '200':
		          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		
		assertEquals(1, errors.size)
		assertEquals(expected, errors.get(0).message)
	}

}