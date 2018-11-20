/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.tools.java.parser;

import com.liferay.portal.kernel.util.StringBundler;

import java.util.List;

/**
 * @author Hugo Huijser
 */
public class JavaMethodDefinition extends BaseJavaTerm {

	public void setJavaAnnotations(List<JavaAnnotation> javaAnnotations) {
		_javaAnnotations = javaAnnotations;
	}

	public void setJavaSignature(JavaSignature javaSignature) {
		_javaSignature = javaSignature;
	}

	@Override
	public String toString(
		String indent, String prefix, String suffix, int maxLineLength) {

		StringBundler sb = new StringBundler();

		for (int i = 0; i < _javaAnnotations.size(); i++) {
			if (i == 0) {
				appendNewLine(
					sb, _javaAnnotations.get(i), indent, prefix, "",
					maxLineLength);
			}
			else {
				appendNewLine(
					sb, _javaAnnotations.get(i), indent, maxLineLength);
			}
		}

		appendNewLine(sb, _javaSignature, indent, "", suffix, maxLineLength);

		return sb.toString();
	}

	private List<JavaAnnotation> _javaAnnotations;
	private JavaSignature _javaSignature;

}