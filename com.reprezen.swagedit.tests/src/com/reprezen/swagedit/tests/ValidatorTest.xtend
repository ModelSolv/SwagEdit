package com.reprezen.swagedit.tests

import com.reprezen.swagedit.editor.SwaggerDocument
import com.reprezen.swagedit.validation.Validator
import java.io.IOException
import org.eclipse.core.resources.IMarker
import org.junit.Assert
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

class ValidatorTest {

	val validator = new Validator
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
		val errors = validator.validate(document)
		Assert.assertEquals(0, errors.size())
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
		val errors = validator.validate(document)
		Assert.assertEquals(1, errors.size())

		val error = errors.get(0)
		Assert.assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		Assert.assertEquals(1, error.getLine())
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
		val errors = validator.validate(document)
		Assert.assertEquals(1, errors.size())

		val error = errors.get(0)
		Assert.assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		Assert.assertEquals(5, error.getLine())
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
		val errors = validator.validate(document)
		Assert.assertEquals(1, errors.size())

		val error = errors.get(0)
		Assert.assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		Assert.assertEquals(8, error.getLine())
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
		val errors = validator.validate(document)
		Assert.assertEquals(1, errors.size())

		val error = errors.get(0)
		Assert.assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		Assert.assertEquals(9, error.getLine())
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
		val errors = validator.validate(document)
		Assert.assertEquals(1, errors.size())

		val error = errors.get(0)
		Assert.assertEquals(IMarker.SEVERITY_ERROR, error.getLevel())
		Assert.assertEquals(5, error.getLine())
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
		val errors = validator.validate(document)

		assertEquals(5, errors.size())

		errors.forEach [
			assertTrue(it.line == 10 || it.line == 11)
			assertEquals(IMarker.SEVERITY_ERROR, it.level)
		]
	}
}

