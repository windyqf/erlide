/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.erlide.ui.eunit.internal.model;

import org.eclipse.core.runtime.Assert;
import org.erlide.ui.eunit.model.ITestCaseElement;

public class TestCaseElement extends TestElement implements ITestCaseElement {

	private boolean fIgnored;

	public TestCaseElement(final TestSuiteElement parent, final String id,
			final String testName) {
		super(parent, id, testName);
		Assert.isNotNull(parent);
	}

	// /**
	// * {@inheritDoc}
	// *
	// * @see org.eclipse.jdt.junit.model.ITestCaseElement#getTestFunctionName()
	// * @see
	// org.eclipse.jdt.internal.junit.runner.MessageIds#TEST_IDENTIFIER_MESSAGE_FORMAT
	// * @see
	// org.eclipse.jdt.internal.junit.runner.MessageIds#IGNORED_TEST_PREFIX
	// */
	// public String getTestFunctionName() {
	// final String testName = getTestName();
	// final String[] mf = testName.split(":");
	// return mf[1];
	// }

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestCaseElement#getTestModuleName()
	 */
	public String getTestModuleName() {
		return getModuleName();
	}

	/*
	 * @see
	 * org.eclipse.jdt.internal.junit.model.TestElement#getTestResult(boolean)
	 * 
	 * @since 3.6
	 */
	@Override
	public Result getTestResult(final boolean includeChildren) {
		if (fIgnored) {
			return Result.IGNORED;
		} else {
			return super.getTestResult(includeChildren);
		}
	}

	public void setIgnored(final boolean ignored) {
		fIgnored = ignored;
	}

	public boolean isIgnored() {
		return fIgnored;
	}

	@Override
	public String toString() {
		return "TestCase: " + getTestModuleName() + "." + getTestFunctionName() + " : " + super.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public String getTestFunctionName() {
		final String mf[] = getTestName().split(":");
		return mf[1];
	}
}
