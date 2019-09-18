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

import com.reprezen.swagedit.core.validation.Messages
import com.reprezen.swagedit.editor.SwaggerDocument
import java.io.IOException
import java.net.URI
import org.eclipse.core.resources.IMarker
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

class ValidatorTest {

	val validator = new SwaggerValidator(null)
	val document = new SwaggerDocument

	@Test
	def shouldNotReturnErrorsIfDocumentIsValid() throws IOException {

		// valid document
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def shouldReturnSingleErrorIfMissingRootProperty() throws IOException {

		// missing property paths
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.get(0)
		assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		assertEquals(0, error.offset)
	}

	@Test
	def shouldReturnSingleErrorIfTypeOfPropertyIsIncorrect() throws IOException {

		// incorrect value type for property paths
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths: 'Hello'
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.get(0)
		assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		assertEquals(4, document.getLineOfOffset(error.offset))
	}

	@Test
	def shouldReturnSingleErrorIfTypeOfDeepPropertyIsIncorrect() throws IOException {

		// invalid responses type
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses: 'Hello'
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.get(0)
		assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		assertEquals(7, document.getLineOfOffset(error.offset))
	}

	@Test
	def shouldReturnSingleErrorIfInvalidResponseCode() throws IOException {

		// invalid response code
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '0':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())

		val error = errors.get(0)
		assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		assertEquals(8, document.getLineOfOffset(error.offset))
	}

	@Test
	def shouldReturnErrorForInvalidScheme() throws IOException {

		// invalid scheme foo
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
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
		assertEquals(1, errors.size())

		val error = errors.get(0)
		assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		assertEquals(6, document.getLineOfOffset(error.offset))
	}

	@Test
	def void shouldReturnCorrectErrorPositionOnPathWithHierarchy() throws IOException {

		// invalid property schema
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			          schema:
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(3, errors.size())
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(8, 9, 10))
		assertThat(errors.map[level], hasItems(IMarker.SEVERITY_ERROR))
	}

	@Test
	def void shouldWarnOnDuplicateKeys() {
		val content = '''
			swagger: '2.0'
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(2, errors.size())

		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(0, 1))
		assertThat(errors.map[level], hasItems(IMarker.SEVERITY_WARNING))
		assertThat(errors.map[message], hasItems(String.format(Messages.error_duplicate_keys, "swagger")))
	}

	@Test
	def void shouldNotWarnOnDuplicateKeys_InDifferentObjects() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			    put:
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldWarnOnDuplicateKeys_InsideObjects() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  version: 1.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(2, errors.size())

		val error = errors.head
		assertEquals(2, document.getLineOfOffset(error.offset))
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(String.format(Messages.error_duplicate_keys, "version"), error.message)
	}

	@Test
	def void shouldWarnOnDuplicateKeys_InsideItems() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			         - name: bar
			           in: path
			           in: path
			           type: string
			           required: true
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(2, errors.size())

		val error = errors.head
		assertEquals(error.offset, document.getLineOffset(10))
		assertEquals(error.level, IMarker.SEVERITY_WARNING)
		assertEquals(error.message, String.format(Messages.error_duplicate_keys, "in"))
	}

	@Test
	def void shouldWarnOnDuplicateKeys_InsidePaths() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			      responses:
			        '201':
			          description: OK
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)

		assertEquals(2, errors.size())

		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(7, 10))
		assertThat(errors.map[level], hasItems(IMarker.SEVERITY_WARNING))
		assertThat(errors.map[message], hasItems(String.format(Messages.error_duplicate_keys, "responses")))
	}

	@Test
	def void correctAliasAfterAnchor() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: Uber parameters
			
			paths: {}
			
			parameters:  
			
			  law:
			    name: law_applicability
			    in: query
			    description: scope of a law
			    required: false
			    type: string
			    enum: &scope_values
			    - GLOBAL
			    - REGIONAL
			    - LOCAL
			
			  brand:
			    name: brand_reach
			    in: query
			    description: where a brand name is used
			    type: string
			    enum: *scope_values
		'''

		document.set(content)
		document.onChange()

		assertThat(document.yamlError, nullValue)
		assertThat(document.jsonError, nullValue)

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void correctAliasBeforeAnchor() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: Uber parameters
			
			paths: {}
			
			parameters:  
			
			  brand:
			    name: brand_reach
			    in: query
			    description: where a brand name is used
			    type: string
			    enum: *scope_values
			
			law:
			    name: law_applicability
			    in: query
			    description: scope of a law
			    required: false
			    type: string
			    enum: &scope_values
			    - GLOBAL
			    - REGIONAL
			    - LOCAL
			
		'''

		document.set(content)
		document.onChange()

		assertThat(document.yamlError, notNullValue)
		assertThat(document.yamlError.message,
			equalTo(
				"found undefined alias scope_values\n in 'reader', line 15, column 11:\n        enum: *scope_values\n              ^\n"))

		assertThat(document.jsonError, nullValue)

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void invlaidAlias() {
		val content = '''
			swagger: "2.0"
			info:
			  version: 1.0.0
			  title: Uber parameters
			
			paths: {}
			
			parameters:  
			
			  law:
			    name: law_applicability
			    in: query
			    description: scope of a law
			    required: false
			    type: string
			    enum: &scope_values
			    - GLOBAL
			    - REGIONAL
			    - LOCAL
			
			  brand:
			    name: brand_reach
			    in: query
			    description: where a brand name is used
			    type: string
			    enum: *scope_values_BROKEN
		'''

		document.set(content)
		document.onChange()

		assertThat(document.yamlError, notNullValue)
		assertThat(document.yamlError.message,
			equalTo(
				"found undefined alias scope_values_BROKEN\n in 'reader', line 26, column 11:\n        enum: *scope_values_BROKEN\n              ^\n"))

		assertThat(document.jsonError, nullValue)

		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())

	}

	@Test
	def void testArrayWithItemsAreValid() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
			  Pets:
			    type: array
			    items: 
			      type: string
		'''

		document.set(content)
		document.onChange()
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testArrayWithMissingItemsAreNotValid() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
			  Pets:
			    type: array
			  Foo:
			    type: object
			    properties:
			      bar:
			        type: array
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(2, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_array_missing_items)])
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(11, 16))
	}

	@Test
	def void testValidateMissingTypeInDefinitions() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
			  Foo:
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
	def void testValidateWrongTypeDefinition() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
			  Foo:
			    type: string
			    properties:
			      bar:
			        type: string
		'''

		document.set(content)
		document.onChange()

		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertEquals(Messages.error_wrong_type, errors.get(0).message)
	}

	@Test
	def void testValidateMissingRequiredProperties() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
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
			swagger: '2.0'
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
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
		assertThat(errors.map[document.getLineOfOffset(offset)], hasItems(13))
	}

	@Test
	def void testObjectWithPropertyNamedProperties_ShouldBeValid() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
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
	def void testValidationShouldPass_IfRefIsCorrectType() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/responses/ok"
			
			responses:
			  ok:
			    description: Ok
			definitions:
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/definitions/Foo"
			
			responses:
			  ok:
			    description: Ok
			definitions:
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  {}
			definitions:
			  Foo:
			    type: array
			    items:
			      $ref: "#/definitions/Bar"
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /resource:
			    get:
			      responses:
			        '200':
			          description: OK
			definitions:
			  Foo:
			    type: array
			    items:
			      $ref: "#/paths/~1resource"
			  Bar:
			    type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
	}
}
