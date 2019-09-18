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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.reprezen.swagedit.core.validation.Messages
import com.reprezen.swagedit.openapi3.editor.OpenApi3Document
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema
import com.reprezen.swagedit.openapi3.utils.Mocks
import java.net.URI
import java.util.Map
import org.eclipse.core.resources.IMarker
import org.junit.Test

import static org.junit.Assert.*

class ReferenceValidatorTest {

	val document = new OpenApi3Document(new OpenApi3Schema)

	def validator(Map<URI, JsonNode> entries) {
		val schemaValidator = ValidationHelper.schemaValidator
		val validator = new OpenApi3ReferenceValidator(schemaValidator, Mocks.mockJsonReferenceFactory(entries))		
		validator
	}

	@Test
	def void shouldValidateReference_To_ValidType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/components/parameters/Valid'
			      responses:
			        '200':
			          description: OK
			components:
			  parameters:
			    Valid:
			      name: Valid
			      in: query
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/components/parameters/Valid")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldValidateReference_To_InvalidCorrect() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/components/schemas/Valid'
			      responses:
			        '200':
			          description: OK
			components:
			  schemas:
			    Valid:
			      type: string
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/components/schemas/Valid")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_invalid_reference_type, error.message)
	}

	@Test
	def void shouldValidateReference_To_InvalidDefinition() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/components/parameters/Invalid'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/components/parameters/Invalid")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void shouldValidateReference_To_ValidPath() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/components/parameters/bar'
			      responses:
			        '200':
			          description: OK
			components:
			  parameters:
			    bar:
			      name: bar
			      in: path
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/components/parameters/bar")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldWarnOnInvalidCharacters() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/paths/~1foo~1{bar}'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/paths/~1foo~1{bar}")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(error.message, Messages.error_invalid_reference)
	}

	@Test
	def void shouldValidateReference_To_InvalidPath() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/paths/~1foo'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/paths/~1foo")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void shouldValidateReference_To_ExternalFile() {
		val other = '''
			openapi: '3.0.0'
		'''

		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml")
		val errors = validator(#{otherURI -> other.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(error.message, Messages.error_invalid_reference_type)
	}

	@Test
	def void should_Not_ValidateReference_To_Invalid_ExternalFile() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml")
		val errors = validator(#{otherURI -> null}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void shouldValidateReference_To_ExternalFileWithValidType() {
		val other = '''
			components:
			  parameters:
			    foo:
			      name: foo
			      in: query
		'''

		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml#/components/parameters/foo'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml#/components/parameters/foo")
		val errors = validator(#{otherURI -> other.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldValidateReference_To_ExternalFileFragmentWithInvalidType() {
		val other = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
		'''

		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml#/info/version'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml#/info/version")
		val errors = validator(#{otherURI -> other.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(error.message, Messages.error_invalid_reference_type)
	}

	@Test
	def void should_Not_ValidateReference_To_ExternalFile_WithInvalidFragment() {
		val other = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
		'''

		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml#/info/foo'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml#/info/foo")
		val errors = validator(#{otherURI -> other.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void should_ProduceError_If_URI_is_Invalid() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'contains space ref.yaml#/info/foo'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val errors = validator(#{}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 8)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void shouldValidateOperationRef() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /test:
			    get:
			      responses:
			        '200':
			          description: OK
			components:
			  links:
			    test:
			      operationRef: "#/paths/~1test/get"
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val errors = validator(#{}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldValidateOperationRefIfInvalid() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /test:
			    get:
			      responses:
			        '200':
			          description: OK
			components:
			  links:
			    test:
			      operationRef: "#/paths/~1foo"
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val errors = validator(#{}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 13)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_missing_reference, error.message)
	}

	@Test
	def void shouldValidateOperationRefIfInvalidType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /test:
			    get:
			      responses:
			        '200':
			          description: OK
			components:
			  links:
			    test:
			      operationRef: "#/paths/~1test"
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val errors = validator(#{}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())

		val error = errors.head
		assertEquals(document.getLineOfOffset(error.offset), 13)
		assertEquals(IMarker.SEVERITY_WARNING, error.level)
		assertEquals(Messages.error_invalid_operation_ref, error.message)
	}

	def asJson(String string) {
		new ObjectMapper(new YAMLFactory).readTree(string)
	}

}
