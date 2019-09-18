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
package com.reprezen.swagedit.openapi3.validation

import com.reprezen.swagedit.core.validation.Messages
import com.reprezen.swagedit.openapi3.editor.OpenApi3Document
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema
import java.net.URI
import org.eclipse.core.resources.IMarker
import org.eclipse.xtext.xbase.lib.Functions.Function1
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*
import org.junit.Ignore

class ValidatorTest {

	val validator = ValidationHelper.validator()
	val document = new OpenApi3Document(new OpenApi3Schema)

	@Test
	def void testValidationShouldPass_IfRefIsCorrectType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/components/responses/ok"
			components:
			  responses:
			    ok:
			      description: Ok
			  schemas:
			    Foo:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_IfRefIsNotCorrectType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/components/schemas/Foo"
			components:
			  responses:
			    ok:
			      description: Ok
			  schemas:
			    Foo:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
	}

	@Test
	def void testValidationShouldPass_ForArrays() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  {}
			components:
			  schemas:
			    Foo:
			      type: array
			      items:
			        $ref: "#/components/schemas/Bar"
			    Bar:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_ForArraysInWrongItemType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  {}
			components:
			  responses:
			    ok:
			      headers: 
			        bearer:
			          description: Bearer
			      description: Ok
			  schemas:
			    Foo:
			      type: array
			      items:
			        $ref: "#/components/responses/ok"
			    Bar:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
	}

	@Test
	def void testValidationShouldFail_ForInvalidPointers() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			    MyType1:
			      type: object
			      properties:
			        property:
			          $ref: "INVALID"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 12)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void testValidationShouldFail_refNotJson() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			    MyType1:
			      type: object
			      properties:
			        property:
			          $ref: "https://www.reprezen.com/#"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		// Update with #353 Validation of external $ref property values should show error on unexpected object type"
		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 12)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Ignore
	@Test
	def void testValidationShouldFail_pathInNotJson() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			  
			    MyType1:
			      properties:
			        property:
			          $ref: "https://www.reprezen.com/#components/schemas/Pet"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		// Update with #353 Validation of external $ref property values should show error on unexpected object type"
		assertEquals(2, errors.size())
	}

	@Test
	def void testValidationShouldPass_LinksOperationIdIsOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationId: opId
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_LinksOperationIdNotOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationId: fail
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_operation_id)])
	}

	@Test
	def void testValidationShouldPass_LinksOperationRefIsOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationRef: "#/paths/~1/get/"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_LinksOperationRefNotOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationRef: "#/paths/~1"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 14)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_invalid_operation_ref, error.message)
	}

	@Test
	def void testValidationShouldPass_WhenSecuritySchemesAreAbsent() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			security:
			  - open: []
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 5)
		assertEquals(IMarker.SEVERITY_ERROR, error.level)
		assertEquals(Messages.error_invalid_security_scheme, error.message)
	}

	@Test
	def void testValidationShouldPassInPaths_WhenSecuritySchemesAreAbsent() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      security:
			        - open: []
			      responses:
			        200:
			          description: Ok
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 9)
		assertEquals(IMarker.SEVERITY_ERROR, error.level)
		assertEquals(Messages.error_invalid_security_scheme, error.message)
	}

	@Test
	def void testValidationShouldPass_SecuritySchemes() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      security:
			        - open: []
			      responses:
			        200:
			          description: Ok
			components: 
			  securitySchemes:
			    open:
			      type: basic
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_SecuritySchemes() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      security:
			        - foo: []
			      responses:
			        200:
			          description: Ok
			components: 
			  securitySchemes:
			    open:
			      type: basic
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[shouldHaveInvalidReferenceType()])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(9))
	}

	@Test
	def void testValidationParameterIn_ShouldFail() {
		val content = '''
			openapi: 3.0.0
			info: 
			  title: Example
			  version: 1.0.0
			paths:
			  /:
			    post:
			      parameters:
			        - name: foo
			          in: headerzzz
			          required: true
			          schema:
			            type: string
			      responses:
			        '200':
			          description: Ok
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 9)
		assertEquals( IMarker.SEVERITY_ERROR, error.level)
		assertEquals(Messages.error_invalid_parameter_location,error.message)
	}

	@Test
	def void testValidationParameterIn() {
		val content = '''
			openapi: 3.0.0
			info: 
			  title: Example
			  version: 1.0.0
			paths:
			  /:
			    post:
			      parameters:
			        - name: foo
			          in: query
			          required: true
			          schema:
			            type: string
			      responses:
			        '200':
			          description: Ok
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(0, errors.size())
	}

	@Test
	def void testValidateMissingRequiredProperties() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths: {}
			components:
			  schemas:
			    Foo:
			      type: object
			      properties:
			        bar:
			          type: string
			      required:
			        - baz
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertEquals(String.format(Messages.warning_required_properties, "baz"), errors.get(0).message)
	}

	@Test
	def void testValidateInlineSchemas() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo:
			    get:
			      description: ok
			      responses:
			        '200':
			          description: OK
			          content:
			            application/json:
			              schema:
			                properties:
			                  bar:
			                    type: string
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertEquals(Messages.error_object_type_missing, errors.get(0).message)
	}

	@Test
	def void testArrayWithItemsIsInvalid() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			components:
			  schemas:
			    Pets:
			      type: array
			      items:
			        - type: string		 
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_array_items_should_be_object)])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(14))
	}

	@Test
	def void testObjectWithPropertyNamedProperties_ShouldBeValid() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			components:
			  schemas:
			    Pets:
			      type: object
			      properties:
			        properties:
			          type: object
			          properties:
			            name: 
			              type: string
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void validateOAuthSecuritySchemes() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      security:
			        - oauth:
			            - write:pets
			            - read:pets
			      responses:
			        '200':
			          description: OK
			components:
			  securitySchemes:
			    oauth:
			      type: oauth2
			      flows: 
			        implicit:
			          authorizationUrl: https://example.com/api/oauth/dialog
			          scopes:
			            write:pets: modify pets in your account
			            read:pets: read your pets
		'''

		document.set(content)

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void validateOAuthSecuritySchemes_WithWrongScope() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      security:
			        - oauth:
			            - write:pets
			            - foo
			      responses:
			        '200':
			          description: OK
			components:
			  securitySchemes:
			    oauth:
			      type: oauth2
			      flows: 
			        implicit:
			          authorizationUrl: https://example.com/api/oauth/dialog
			          scopes:
			            write:pets: modify pets in your account
			            read:pets: read your pets
		'''

		document.set(content)

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[shouldBeInvalidScopeReference("foo", "oauth")])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(10))
	}

	@Test
	def void validateNonOAuthSecuritySchemes() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      security:
			        - basic: []
			      responses:
			        '200':
			          description: OK
			components:
			  securitySchemes:
			    basic:
			      type: http
		'''

		document.set(content)

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void validateNonOAuthSecuritySchemes_WithWrongScope() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      security:
			        - basic:
			          - foo
			      responses:
			        '200':
			          description: OK
			components:
			  securitySchemes:
			    basic:
			      type: http
		'''

		document.set(content)

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[shouldBeEmptyMessage("basic", "http")])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(8))
	}

	@Test
	def void testPointerWithSpecialCharacters() {
		val content = '''		
		openapi: "3.0.0"
		info:
		  version: "1.0.0"
		  title: Test API
		paths:
		  /test/{id}:
		    get:
		      responses:
		        '200':
		          description: OK
		          content:
		            application/json:
		              schema:
		                $ref: "#/paths/~1test~1%7Bid%7D"
		'''
		
		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(13))
	}

	private def shouldHaveInvalidReferenceType(String actual) {
		expectedMessage(Messages.error_invalid_security_scheme).apply(actual)
	}

	private def shouldBeInvalidScopeReference(String actual, String scope, String name) {
		expectedMessage(Messages.error_invalid_scope_reference, scope, name).apply(actual)
	}

	private def shouldBeEmptyMessage(String actual, String name, String type) {
		expectedMessage(Messages.error_scope_should_be_empty, name, type, name).apply(actual)
	}

	private def (String)=>Boolean expectedMessage(String message, String... args) {
		new Function1<String, Boolean> {
			override apply(String actual) {
				actual.equals(String.format(message, args))
			}
		}
	}

}
