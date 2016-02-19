package com.reprezen.swagedit.tests;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static com.reprezen.swagedit.templates.SwaggerContextType.getContentType;

import org.junit.Test;

import com.reprezen.swagedit.templates.SwaggerContextType.ParametersContextType;
import com.reprezen.swagedit.templates.SwaggerContextType.PathItemContextType;
import com.reprezen.swagedit.templates.SwaggerContextType.ResponsesContextType;
import com.reprezen.swagedit.templates.SwaggerContextType.SchemaContextType;
import com.reprezen.swagedit.templates.SwaggerContextType.SecurityDefContextType;;

public class CodeTemplateContextTest {

	@Test
	public void testPathItem() throws Exception {
		assertThat(getContentType(":paths:/petstore"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pet-store"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets/{id}"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets/{pet-id}"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/my-pets/{pet-id}"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/my-pets/v1/{pet-id}"), equalTo(PathItemContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets"), equalTo(PathItemContextType.CONTEXT_ID));

		// tests for #Templates defined in the Path context show in other
		// contexts
		assertThat(getContentType(":paths:"), not(equalTo(PathItemContextType.CONTEXT_ID)));
		assertThat(getContentType(":paths:/pets:get"), not(equalTo(PathItemContextType.CONTEXT_ID)));
		assertThat(getContentType(":paths:/pets:get:responses"), not(equalTo(PathItemContextType.CONTEXT_ID)));
	}

	@Test
	public void testSecurityDef() throws Exception {
		assertThat(getContentType(":securityDefinitions"), equalTo(SecurityDefContextType.CONTEXT_ID));
	}

	@Test
	public void testResponses() throws Exception {
		assertThat(getContentType(":responses"), equalTo(ResponsesContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/resource:get:responses"), equalTo(ResponsesContextType.CONTEXT_ID));
	}

	@Test
	public void testParameters() throws Exception {
		assertThat(getContentType(":paths:/taxFilings/{id}:get:parameters:@0:"),
				equalTo(ParametersContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/taxFilings/{id}:get:parameters"), equalTo(ParametersContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/resource:parameters:"), equalTo(ParametersContextType.CONTEXT_ID));
		assertThat(getContentType(":parameters:skipParam:"), equalTo(ParametersContextType.CONTEXT_ID));
	}

	@Test
	public void testSchema() throws Exception {
		assertThat(getContentType(":definitions:Pet:"), equalTo(SchemaContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets/{id}:delete:responses:default:schema:"),
				equalTo(SchemaContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets/{id}:get:responses:200:schema:"),
				equalTo(SchemaContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets:post:parameters:@0:schema:"), equalTo(SchemaContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets:post:parameters:@0:schema:properties:name"),
				equalTo(SchemaContextType.CONTEXT_ID));
		assertThat(getContentType(":paths:/pets:post:parameters:@0:schema:items"),
				equalTo(SchemaContextType.CONTEXT_ID));
	}

}
