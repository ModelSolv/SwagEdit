package com.reprezen.swagedit.validation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator
import com.reprezen.swagedit.core.validation.Messages
import com.reprezen.swagedit.editor.SwaggerDocument
import com.reprezen.swagedit.mocks.Mocks
import com.reprezen.swagedit.validation.SwaggerValidator.SwaggerSchemaValidator
import java.net.URI
import java.util.Map
import org.eclipse.core.resources.IMarker
import org.junit.Test

import static org.junit.Assert.*

class ReferenceValidatorTest {

	val document = new SwaggerDocument

	def validator(Map<URI, JsonNode> entries) {
		new JsonReferenceValidator(new SwaggerSchemaValidator, Mocks.mockJsonReferenceFactory(entries))				
	}

	@Test
	def void shouldValidateReference_To_ValidType() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/parameters/Valid'
			      responses:
			        '200':
			          description: OK
			parameters:
			  Valid:
			    name: Valid
			    in: query
			    type: string
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/definitions/Valid")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldValidateReference_To_InvalidCorrect() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/definitions/Valid'
			      responses:
			        '200':
			          description: OK
			definitions:
			  Valid:
			    type: string
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/definitions/Valid")
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/definitions/Invalid'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/definitions/Invalid")
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: '#/parameters/bar'
			      responses:
			        '200':
			          description: OK
			parameters:
			  bar:
			    name: bar
			    in: path
			    required: true
			    type: string
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/parameters/bar")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldWarnOnInvalidCharacters() {
		val content = '''
			swagger: '2.0'
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
			swagger: '2.0'
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
			swagger: '2.0'
		'''

		val content = '''
			swagger: '2.0'
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
		assertEquals(Messages.error_invalid_reference_type, error.message)
	}

	@Test
	def void should_Not_ValidateReference_To_Invalid_ExternalFile() {
		val content = '''
			swagger: '2.0'
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
			parameters:
			  foo:
			    name: foo
			    in: query
			    type: string
		'''

		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: 'other.yaml#/parameters/foo'
			      responses:
			        '200':
			          description: OK
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val otherURI = URI.create("other.yaml#/parameters/foo")
		val errors = validator(#{otherURI -> other.asJson}).validate(baseURI, document, document.model)

		assertEquals(0, errors.size())
	}

	@Test
	def void shouldValidateReference_To_ExternalFileFragmentWithInvalidType() {
		val other = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
		'''

		val content = '''
			swagger: '2.0'
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
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
		'''

		val content = '''
			swagger: '2.0'
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
			swagger: '2.0'
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
	def void should_warn_on_simple_reference() {
		val content = '''
			swagger: '2.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /foo/{bar}:
			    get:
			      parameters:
			        - $ref: Valid
			      responses:
			        '200':
			          description: OK
			definitions:
			  Valid:
			    type: string
		'''

		document.set(content)
		val baseURI = new URI(null, null, null)
		val resolvedURI = new URI(null, null, "/definitions/Valid")
		val errors = validator(#{resolvedURI -> document.asJson}).validate(baseURI, document, document.model)

		assertEquals(1, errors.size())
		assertEquals(Messages.warning_simple_reference, errors.get(0).message)
	}

	def asJson(String string) {
		new ObjectMapper(new YAMLFactory).readTree(string)
	}

}
