/*******************************************************************************
 * Copyright (c) 2019 ModelSolv, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.core.utils;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonPointer;
import com.reprezen.swagedit.core.model.AbstractNode;
import com.reprezen.swagedit.core.model.ObjectNode;

public class ModelUtils {

	public static Optional<AbstractNode> findParentContainingField(AbstractNode node, String fieldName) {
		if (node instanceof ObjectNode) {
			final ObjectNode objectNode = (ObjectNode) node;
			if (objectNode.get(fieldName) != null) {
				return Optional.of(objectNode);
			}
		}

		if (node == null) {
			return Optional.empty();
		}

		return findParentContainingField(node.getParent(), fieldName);
	}

	public static Optional<AbstractNode> findParent(AbstractNode node, JsonPointer pointer) {
		if (node instanceof ObjectNode) {
			final ObjectNode objectNode = (ObjectNode) node;
			if (pointer.equals(objectNode.getType().getPointer())) {
				return Optional.of(objectNode);
			}
		}

		if (node == null) {
			return Optional.empty();
		}

		return findParent(node.getParent(), pointer);
	}

}
