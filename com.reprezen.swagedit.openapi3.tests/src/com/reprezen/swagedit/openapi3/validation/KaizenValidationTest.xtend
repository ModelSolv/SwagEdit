package com.reprezen.swagedit.openapi3.validation

import com.reprezen.swagedit.openapi3.editor.OpenApi3Document
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Test

import static org.junit.Assert.*
import org.junit.Ignore

class KaizenValidationTest {

	val validator = ValidationHelper.validator(true)
	val document = new OpenApi3Document(new OpenApi3Schema)

	@Ignore
	@Test
	def void testValidation_OnMInvalidType() {
		val resource = Paths.get("resources", "tests", "validation_type.yaml")

		document.set(new String(Files.readAllBytes(resource)))

		val errors = validator.validate(document, resource.toUri)
		assertEquals(1, errors.size())
	}
}
