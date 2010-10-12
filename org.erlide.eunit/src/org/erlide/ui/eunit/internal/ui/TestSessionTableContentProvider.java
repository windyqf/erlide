/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.erlide.ui.eunit.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.erlide.ui.eunit.internal.model.TestCaseElement;
import org.erlide.ui.eunit.internal.model.TestRoot;
import org.erlide.ui.eunit.internal.model.TestSuiteElement;
import org.erlide.ui.eunit.model.ITestElement;

public class TestSessionTableContentProvider implements
		IStructuredContentProvider {

	public void inputChanged(final Viewer viewer, final Object oldInput,
			final Object newInput) {
	}

	public Object[] getElements(final Object inputElement) {
		final List<ITestElement> all = new ArrayList<ITestElement>();
		addAll(all, (TestRoot) inputElement);
		return all.toArray();
	}

	private void addAll(final List<ITestElement> all,
			final TestSuiteElement suite) {
		final ITestElement[] children = suite.getChildren();
		for (int i = 0; i < children.length; i++) {
			final ITestElement element = children[i];
			if (element instanceof TestSuiteElement) {
				if (((TestSuiteElement) element).getSuiteStatus()
						.isErrorOrFailure()) {
					all.add(element); // add failed suite to flat list too
				}
				addAll(all, (TestSuiteElement) element);
			} else if (element instanceof TestCaseElement) {
				all.add(element);
			}
		}
	}

	public void dispose() {
	}
}
