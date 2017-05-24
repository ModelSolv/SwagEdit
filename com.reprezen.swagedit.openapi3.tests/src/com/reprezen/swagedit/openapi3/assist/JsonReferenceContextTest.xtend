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
package com.reprezen.swagedit.openapi3.assist

import org.junit.Test

import static org.junit.Assert.*

class JsonReferenceContextTest {

	@Test
	def void parameter_in_path_item() {
		assertTrue("/paths/~1pets/parameters/0/$ref".matches(OpenApi3ReferenceProposalProvider.PARAMETER_REGEX))
	}

	@Test
	def void parameter_in_operation() {
		assertTrue("/paths/~1pets/get/parameters/0/$ref".matches(OpenApi3ReferenceProposalProvider.PARAMETER_REGEX))
	}

	@Test
	def void requestBody_in_callback() {
		assertTrue("/components/callbacks/myWebhook/$request.body#~1url/post/requestBody/$ref".matches(OpenApi3ReferenceProposalProvider.REQUEST_BODY_REGEX))
	}

	@Test
	def void requestBody_in_operation() {
		assertTrue("/paths/~1pets/post/requestBody/$ref".matches(OpenApi3ReferenceProposalProvider.REQUEST_BODY_REGEX))
	}

	@Test
	def void path_item_in_paths_object() {
		assertTrue("/paths/~1pets/$ref".matches(OpenApi3ReferenceProposalProvider.PATH_ITEM_REGEX))
	}

	@Test
	def void path_item_in_callback() {
		assertTrue(
			"/components/callbacks/myWebhook/$request.body#~1url/$ref".matches(
				OpenApi3ReferenceProposalProvider.PATH_ITEM_REGEX))
	}
	
	@Test
	def void headers_in_response() {
		assertTrue(
			"/paths/~1pets/get/responses/200/headers/X-Rate-Limit-Reset/$ref".matches(
				OpenApi3ReferenceProposalProvider.HEADER_REGEX))
	}

	@Test
	def void headers_in_link() {
		assertTrue(
			"/components/links/UserRepositories/headers/X-Rate-Limit-Reset/$ref".matches(
				OpenApi3ReferenceProposalProvider.HEADER_REGEX))
	}

	@Test
	def void callbacks_in_operation() {
		assertTrue(
			"/paths/~1pets/get/callbacks/myWebhook/$ref".matches(OpenApi3ReferenceProposalProvider.CALLBACK_REGEX))
	}

	@Test
	def void example_in_parameter_schema() {
		assertTrue(
			"/paths/~1pets~1{id}/parameters/0/schema/example/$ref".matches(
				OpenApi3ReferenceProposalProvider.SCHEMA_EXAMPLE_REGEX))
	}

	@Test
	def void examples_in_requestBody() {
		assertTrue(
			"/paths/~1pets~1{id}/post/requestBody/content/application~1json/examples/pizza/$ref".matches(
				OpenApi3ReferenceProposalProvider.EXAMPLE_REGEX))
	}

	@Test
	def void examples_in_response() {
		assertTrue(
			"/paths/~1pets~1{id}/post/responses/200/content/application~1json/examples/confirmation-success/$ref".
				matches(OpenApi3ReferenceProposalProvider.EXAMPLE_REGEX))
	}

	@Test
	def void example_in_property() {
		assertTrue(
			"/components/schemas/Pet/properties/name/example/$ref".matches(
				OpenApi3ReferenceProposalProvider.SCHEMA_EXAMPLE_REGEX))
	}
	

	@Test
	def void link_in_response() {
		assertTrue(
			"/paths/~12.0~1users~1{username}/get/responses/200/links/userRepositories/$ref".matches(
				OpenApi3ReferenceProposalProvider.LINK_REGEX))
		assertTrue(
			"/paths/~12.0~1repositories~1{username}/get/responses/200/links/userRepository/$ref".matches(
				OpenApi3ReferenceProposalProvider.LINK_REGEX))
		assertTrue(
			"/paths/~12.0~1repositories~1{username}~1{slug}/get/responses/200/links/repositoryPullRequests/$ref".matches(
				OpenApi3ReferenceProposalProvider.LINK_REGEX))
		assertTrue(
			"/paths/~12.0~1repositories~1{username}~1{slug}~1pullrequests~1{pid}/get/responses/200/links/pullRequst/$ref".matches(
				OpenApi3ReferenceProposalProvider.LINK_REGEX))
	}	
}
