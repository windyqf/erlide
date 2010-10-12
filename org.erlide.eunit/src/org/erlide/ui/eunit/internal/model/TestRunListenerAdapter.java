/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.erlide.ui.eunit.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.TestRunListener;
import org.erlide.ui.eunit.internal.model.TestElement.Status;
import org.erlide.ui.eunit.model.ITestCaseElement;

/**
 * Notifier for the callback listener API {@link TestRunListener}.
 */
public class TestRunListenerAdapter implements ITestSessionListener {

	private final TestRunSession fSession;

	public TestRunListenerAdapter(final TestRunSession session) {
		fSession = session;
	}

	private List<TestRunListener> getListeners() {
		final Object[] listeners = EUnitPlugin.getDefault()
				.getTestRunListeners().getListeners();
		final List<TestRunListener> result = new ArrayList<TestRunListener>(
				listeners.length);
		for (final Object i : listeners) {
			result.add((TestRunListener) i);
		}
		return result;
	}

	private void fireSessionStarted() {
		for (final TestRunListener i : getListeners()) {
			i.sessionStarted(fSession);
		}
	}

	private void fireSessionFinished() {
		for (final TestRunListener i : getListeners()) {
			i.sessionFinished(fSession);
		}
	}

	private void fireTestCaseStarted(final ITestCaseElement testCaseElement) {
		for (final TestRunListener i : getListeners()) {
			i.testCaseStarted(testCaseElement);
		}
	}

	private void fireTestCaseFinished(final ITestCaseElement testCaseElement) {
		for (final TestRunListener i : getListeners()) {
			i.testCaseFinished(testCaseElement);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionStarted
	 * ()
	 */
	public void sessionStarted() {
		// wait until all test are added
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionEnded
	 * (long)
	 */
	public void sessionEnded(final long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionStopped
	 * (long)
	 */
	public void sessionStopped(final long elapsedTime) {
		fireSessionFinished();
		fSession.swapOut();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#sessionTerminated
	 * ()
	 */
	public void sessionTerminated() {
		fSession.swapOut();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#testAdded(org
	 * .eclipse.jdt.internal.junit.model.TestElement)
	 */
	public void testAdded(final TestElement testElement) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#runningBegins()
	 */
	public void runningBegins() {
		fireSessionStarted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#testStarted
	 * (org.eclipse.jdt.internal.junit.model.TestCaseElement)
	 */
	public void testStarted(final TestCaseElement testCaseElement) {
		fireTestCaseStarted(testCaseElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#testEnded(org
	 * .eclipse.jdt.internal.junit.model.TestCaseElement)
	 */
	public void testEnded(final TestCaseElement testCaseElement) {
		fireTestCaseFinished(testCaseElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#testFailed(
	 * org.eclipse.jdt.internal.junit.model.TestElement,
	 * org.eclipse.jdt.internal.junit.model.TestElement.Status,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public void testFailed(final TestElement testElement, final Status status,
			final String trace, final String expected, final String actual) {
		// ignore
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.model.ITestSessionListener#testReran(org
	 * .eclipse.jdt.internal.junit.model.TestCaseElement,
	 * org.eclipse.jdt.internal.junit.model.TestElement.Status,
	 * java.lang.String, java.lang.String, java.lang.String)
	 */
	public void testReran(final TestCaseElement testCaseElement,
			final Status status, final String trace,
			final String expectedResult, final String actualResult) {
		// ignore
	}

	public boolean acceptsSwapToDisk() {
		return true;
	}
}
