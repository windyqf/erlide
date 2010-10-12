/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *******************************************************************************/

package org.erlide.ui.eunit.internal.model;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.core.runtime.Assert;
import org.erlide.core.erlang.IErlProject;
import org.erlide.ui.eunit.model.ITestElement;
import org.erlide.ui.eunit.model.ITestElement.FailureTrace;
import org.erlide.ui.eunit.model.ITestElement.ProgressState;
import org.erlide.ui.eunit.model.ITestElement.Result;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

public class TestRunSessionSerializer implements XMLReader {

	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String CDATA = "CDATA"; //$NON-NLS-1$
	private static final Attributes NO_ATTS = new AttributesImpl();

	private final TestRunSession fTestRunSession;
	private ContentHandler fHandler;
	private ErrorHandler fErrorHandler;

	private final NumberFormat timeFormat = new DecimalFormat(
			"0.0##", new DecimalFormatSymbols(Locale.US)); //$NON-NLS-1$ // not localized, parseable by Double.parseDouble(..)

	/**
	 * @param testRunSession
	 *            the test run session to serialize
	 */
	public TestRunSessionSerializer(final TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		fTestRunSession = testRunSession;
	}

	public void parse(final InputSource input) throws IOException, SAXException {
		if (fHandler == null) {
			throw new SAXException("ContentHandler missing"); //$NON-NLS-1$
		}

		fHandler.startDocument();
		handleTestRun();
		fHandler.endDocument();
	}

	private void handleTestRun() throws SAXException {
		final AttributesImpl atts = new AttributesImpl();
		addCDATA(atts, IXMLTags.ATTR_NAME, fTestRunSession.getTestRunName());
		final IErlProject project = fTestRunSession.getLaunchedProject();
		if (project != null) {
			addCDATA(atts, IXMLTags.ATTR_PROJECT, project.getName());
		}
		addCDATA(atts, IXMLTags.ATTR_TESTS, fTestRunSession.getTotalCount());
		addCDATA(atts, IXMLTags.ATTR_STARTED, fTestRunSession.getStartedCount());
		addCDATA(atts, IXMLTags.ATTR_FAILURES,
				fTestRunSession.getFailureCount());
		addCDATA(atts, IXMLTags.ATTR_ERRORS, fTestRunSession.getErrorCount());
		addCDATA(atts, IXMLTags.ATTR_IGNORED, fTestRunSession.getIgnoredCount());
		startElement(IXMLTags.NODE_TESTRUN, atts);

		final TestRoot testRoot = fTestRunSession.getTestRoot();
		final ITestElement[] topSuites = testRoot.getChildren();
		for (int i = 0; i < topSuites.length; i++) {
			handleTestElement(topSuites[i]);
		}

		endElement(IXMLTags.NODE_TESTRUN);
	}

	private void handleTestElement(final ITestElement testElement)
			throws SAXException {
		if (testElement instanceof TestSuiteElement) {
			final TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;

			final AttributesImpl atts = new AttributesImpl();
			addCDATA(atts, IXMLTags.ATTR_NAME,
					testSuiteElement.getSuiteTypeName());
			if (!Double.isNaN(testSuiteElement.getElapsedTimeInSeconds())) {
				addCDATA(atts, IXMLTags.ATTR_TIME,
						timeFormat.format(testSuiteElement
								.getElapsedTimeInSeconds()));
			}
			if (testElement.getProgressState() != ProgressState.COMPLETED
					|| testElement.getTestResult(false) != Result.UNDEFINED) {
				addCDATA(atts, IXMLTags.ATTR_INCOMPLETE,
						Boolean.TRUE.toString());
			}

			startElement(IXMLTags.NODE_TESTSUITE, atts);
			addFailure(testElement);

			final ITestElement[] children = testSuiteElement.getChildren();
			for (int i = 0; i < children.length; i++) {
				handleTestElement(children[i]);
			}
			endElement(IXMLTags.NODE_TESTSUITE);

		} else if (testElement instanceof TestCaseElement) {
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;

			final AttributesImpl atts = new AttributesImpl();
			addCDATA(atts, IXMLTags.ATTR_NAME,
					testCaseElement.getTestFunctionName());
			addCDATA(atts, IXMLTags.ATTR_MODULENAME,
					testCaseElement.getModuleName());
			if (!Double.isNaN(testCaseElement.getElapsedTimeInSeconds())) {
				addCDATA(atts, IXMLTags.ATTR_TIME,
						timeFormat.format(testCaseElement
								.getElapsedTimeInSeconds()));
			}
			if (testElement.getProgressState() != ProgressState.COMPLETED) {
				addCDATA(atts, IXMLTags.ATTR_INCOMPLETE,
						Boolean.TRUE.toString());
			}
			if (testCaseElement.isIgnored()) {
				addCDATA(atts, IXMLTags.ATTR_IGNORED, Boolean.TRUE.toString());
			}

			startElement(IXMLTags.NODE_TESTCASE, atts);
			addFailure(testElement);

			endElement(IXMLTags.NODE_TESTCASE);

		} else {
			throw new IllegalStateException(String.valueOf(testElement));
		}

	}

	private void addFailure(final ITestElement testElement) throws SAXException {
		final FailureTrace failureTrace = testElement.getFailureTrace();
		if (failureTrace != null) {
			final AttributesImpl failureAtts = new AttributesImpl();
			// addCDATA(failureAtts, IXMLTags.ATTR_MESSAGE, xx);
			// addCDATA(failureAtts, IXMLTags.ATTR_TYPE, xx);
			final String failureKind = testElement.getTestResult(false) == Result.ERROR ? IXMLTags.NODE_ERROR
					: IXMLTags.NODE_FAILURE;
			startElement(failureKind, failureAtts);
			final String expected = failureTrace.getExpected();
			final String actual = failureTrace.getActual();
			if (expected != null) {
				startElement(IXMLTags.NODE_EXPECTED, NO_ATTS);
				addCharacters(expected);
				endElement(IXMLTags.NODE_EXPECTED);
			}
			if (actual != null) {
				startElement(IXMLTags.NODE_ACTUAL, NO_ATTS);
				addCharacters(actual);
				endElement(IXMLTags.NODE_ACTUAL);
			}
			final String trace = failureTrace.getTrace();
			addCharacters(trace);
			endElement(failureKind);
		}
	}

	private void startElement(final String name, final Attributes atts)
			throws SAXException {
		fHandler.startElement(EMPTY, name, name, atts);
	}

	private void endElement(final String name) throws SAXException {
		fHandler.endElement(EMPTY, name, name);
	}

	private static void addCDATA(final AttributesImpl atts, final String name,
			final int value) {
		addCDATA(atts, name, Integer.toString(value));
	}

	private static void addCDATA(final AttributesImpl atts, final String name,
			final String value) {
		atts.addAttribute(EMPTY, EMPTY, name, CDATA, value);
	}

	private void addCharacters(String string) throws SAXException {
		string = escapeNonUnicodeChars(string);
		fHandler.characters(string.toCharArray(), 0, string.length());
	}

	/**
	 * Replaces all non-Unicode characters in the given string.
	 * 
	 * @param string
	 *            a string
	 * @return string with Java-escapes
	 * @since 3.6
	 */
	private static String escapeNonUnicodeChars(final String string) {
		StringBuffer buf = null;
		for (int i = 0; i < string.length(); i++) {
			final char ch = string.charAt(i);
			if (!(ch == 9 || ch == 10 || ch == 13 || ch >= 32)) {
				if (buf == null) {
					buf = new StringBuffer(string.substring(0, i));
				}
				buf.append("\\u"); //$NON-NLS-1$
				final String hex = Integer.toHexString(ch);
				for (int j = hex.length(); j < 4; j++) {
					buf.append('0');
				}
				buf.append(hex);
			} else if (buf != null) {
				buf.append(ch);
			}
		}
		if (buf != null) {
			return buf.toString();
		}
		return string;
	}

	public void setContentHandler(final ContentHandler handler) {
		fHandler = handler;
	}

	public ContentHandler getContentHandler() {
		return fHandler;
	}

	public void setErrorHandler(final ErrorHandler handler) {
		fErrorHandler = handler;
	}

	public ErrorHandler getErrorHandler() {
		return fErrorHandler;
	}

	// ignored:

	public void parse(final String systemId) throws IOException, SAXException {
	}

	public void setDTDHandler(final DTDHandler handler) {
	}

	public DTDHandler getDTDHandler() {
		return null;
	}

	public void setEntityResolver(final EntityResolver resolver) {
	}

	public EntityResolver getEntityResolver() {
		return null;
	}

	public void setProperty(final java.lang.String name,
			final java.lang.Object value) {
	}

	public Object getProperty(final java.lang.String name) {
		return null;
	}

	public void setFeature(final java.lang.String name, final boolean value) {
	}

	public boolean getFeature(final java.lang.String name) {
		return false;
	}
}
