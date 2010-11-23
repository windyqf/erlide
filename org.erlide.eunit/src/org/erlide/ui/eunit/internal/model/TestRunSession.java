/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.erlide.ui.eunit.internal.model;

import java.io.File;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.erlide.core.erlang.IErlProject;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.internal.runner.MessageIds;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.eunit.internal.launcher.EUnitEventHandler;
import org.erlide.ui.eunit.internal.model.TestElement.Status;
import org.erlide.ui.eunit.model.ITestElement;
import org.erlide.ui.eunit.model.ITestElementContainer;
import org.erlide.ui.eunit.model.ITestRunSession;

/**
 * A test run session holds all information about a test run, i.e. launch
 * configuration, launch, test tree (including results).
 */
public class TestRunSession implements ITestRunSession {

	/**
	 * The launch, or <code>null</code> iff this session was run externally.
	 */
	private final ILaunch fLaunch;
	private final String fTestRunName;
	/**
	 * Erlang project, or <code>null</code>.
	 */
	private final IErlProject fProject;

	// private final ITestKind fTestRunnerKind;

	/**
	 * Test runner client or <code>null</code>.
	 */
	// private RemoteTestRunnerClient fTestRunnerClient;

	private final ListenerList/* <ITestSessionListener> */fSessionListeners;

	/**
	 * The model root, or <code>null</code> if swapped to disk.
	 */
	private TestRoot fTestRoot;

	/**
	 * The test run session's cached result, or <code>null</code> if
	 * <code>fTestRoot != null</code>.
	 */
	private Result fTestResult;

	/**
	 * Map from testId to testElement.
	 */
	private HashMap<String, TestElement> fIdToTest;

	/**
	 * The TestSuites for which additional children are expected.
	 */
	private List<IncompleteTestSuite> fIncompleteTestSuites;

	/**
	 * Suite for unrooted test case elements, or <code>null</code>.
	 */
	private TestSuiteElement fUnrootedSuite;

	/**
	 * Number of tests started during this test run.
	 */
	volatile int fStartedCount;
	/**
	 * Number of tests ignored during this test run.
	 */
	volatile int fIgnoredCount;
	/**
	 * Number of errors during this test run.
	 */
	volatile int fErrorCount;
	/**
	 * Number of failures during this test run.
	 */
	volatile int fFailureCount;
	/**
	 * Total number of tests to run.
	 */
	volatile int fTotalCount;
	/**
	 * <ul>
	 * <li>If &gt; 0: Start time in millis</li>
	 * <li>If &lt; 0: Unique identifier for imported test run</li>
	 * <li>If = 0: Session not started yet</li>
	 * </ul>
	 */
	volatile long fStartTime;
	volatile boolean fIsRunning;

	volatile boolean fIsStopped;

	/**
	 * Creates a test run session.
	 * 
	 * @param testRunName
	 *            name of the test run
	 * @param project
	 *            may be <code>null</code>
	 */
	public TestRunSession(final ILaunch launch, final String testRunName,
			final IErlProject project) {
		// TODO: check assumptions about non-null fields

		fLaunch = launch;
		fProject = project;
		fStartTime = -System.currentTimeMillis();

		Assert.isNotNull(testRunName);
		fTestRunName = testRunName;
		// fTestRunnerKind = ITestKind.NULL; // TODO

		fTestRoot = new TestRoot(this);
		fIdToTest = new HashMap<String, TestElement>();

		// fTestRunnerClient = null;

		fSessionListeners = new ListenerList();

		addTestSessionListener(new ITestSessionListener() {

			public void testStarted(final TestCaseElement testCaseElement) {
			}

			public void testReran(final TestCaseElement testCaseElement,
					final Status status, final String trace,
					final String expectedResult, final String actualResult) {
			}

			public void testFailed(final TestElement testElement,
					final Status status, final String trace,
					final String expected, final String actual) {
			}

			public void testEnded(final TestCaseElement testCaseElement) {
			}

			public void testAdded(final TestElement testElement) {
			}

			public void sessionEnded(final long elapsedTime) {
				swapOut();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#
			 * sessionStopped(long)
			 */
			public void sessionStopped(final long elapsedTime) {
				swapOut();
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.jdt.internal.junit.model.ITestSessionListener#
			 * sessionTerminated()
			 */
			public void sessionTerminated() {
				swapOut();
			}

			public void runningBegins() {
			}

			public boolean acceptsSwapToDisk() {
				return true;
			}

			public void sessionStarted() {
			}
		});
	}

	void reset() {
		fStartedCount = 0;
		fFailureCount = 0;
		fErrorCount = 0;
		fIgnoredCount = 0;
		fTotalCount = 0;

		fTestRoot = new TestRoot(this);
		fTestResult = null;
		fIdToTest = new HashMap<String, TestElement>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.ITestRunSession#getProgressState()
	 */
	public ProgressState getProgressState() {
		if (isRunning()) {
			return ProgressState.RUNNING;
		}
		if (isStopped()) {
			return ProgressState.STOPPED;
		}
		return ProgressState.COMPLETED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElement#getTestResult(boolean)
	 */
	public Result getTestResult(final boolean includeChildren) {
		if (fTestRoot != null) {
			return fTestRoot.getTestResult(true);
		} else {
			return fTestResult;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElementContainer#getChildren()
	 */
	public ITestElement[] getChildren() {
		return getTestRoot().getChildren();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElement#getFailureTrace()
	 */
	public FailureTrace getFailureTrace() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElement#getParentContainer()
	 */
	public ITestElementContainer getParentContainer() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElement#getTestRunSession()
	 */
	public ITestRunSession getTestRunSession() {
		return this;
	}

	public synchronized TestRoot getTestRoot() {
		swapIn(); // TODO: TestRoot should stay (e.g. for
					// getTestRoot().getStatus())
		return fTestRoot;
	}

	/*
	 * @see org.eclipse.jdt.junit.model.ITestRunSession#getJavaProject()
	 */
	public IErlProject getLaunchedProject() {
		return fProject;
	}

	// public ITestKind getTestRunnerKind() {
	// return fTestRunnerKind;
	// }

	/**
	 * @return the launch, or <code>null</code> iff this session was run
	 *         externally
	 */
	public ILaunch getLaunch() {
		return fLaunch;
	}

	public String getTestRunName() {
		return fTestRunName;
	}

	public int getErrorCount() {
		return fErrorCount;
	}

	public int getFailureCount() {
		return fFailureCount;
	}

	public int getStartedCount() {
		return fStartedCount;
	}

	public int getIgnoredCount() {
		return fIgnoredCount;
	}

	public int getTotalCount() {
		return fTotalCount;
	}

	public long getStartTime() {
		return fStartTime;
	}

	/**
	 * @return <code>true</code> iff the session has been stopped or terminated
	 */
	public boolean isStopped() {
		return fIsStopped;
	}

	public synchronized void addTestSessionListener(
			final ITestSessionListener listener) {
		swapIn();
		fSessionListeners.add(listener);
	}

	public void removeTestSessionListener(final ITestSessionListener listener) {
		fSessionListeners.remove(listener);
	}

	public synchronized void swapOut() {
		if (fTestRoot == null) {
			return;
		}
		if (isRunning() || isStarting() || isKeptAlive()) {
			return;
		}

		final Object[] listeners = fSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			final ITestSessionListener registered = (ITestSessionListener) listeners[i];
			if (!registered.acceptsSwapToDisk()) {
				return;
			}
		}

		try {
			final File swapFile = getSwapFile();

			EUnitModel.exportTestRunSession(this, swapFile);
			fTestResult = fTestRoot.getTestResult(true);
			fTestRoot = null;
			// fTestRunnerClient = null;
			fIdToTest = new HashMap<String, TestElement>();
			fIncompleteTestSuites = null;
			fUnrootedSuite = null;

		} catch (final IllegalStateException e) {
			ErlLogger.error(e);
		} catch (final CoreException e) {
			ErlLogger.error(e);
		}
	}

	public boolean isStarting() {
		return getStartTime() == 0 && fLaunch != null
				&& !fLaunch.isTerminated();
	}

	public void removeSwapFile() {
		final File swapFile = getSwapFile();
		if (swapFile.exists()) {
			swapFile.delete();
		}
	}

	private File getSwapFile() throws IllegalStateException {
		final File historyDir = EUnitPlugin.getHistoryDirectory();
		final String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS").format(new Date(getStartTime())); //$NON-NLS-1$
		final String swapFileName = isoTime + ".xml"; //$NON-NLS-1$
		return new File(historyDir, swapFileName);
	}

	public synchronized void swapIn() {
		if (fTestRoot != null) {
			return;
		}

		try {
			EUnitModel.importIntoTestRunSession(getSwapFile(), this);
		} catch (final IllegalStateException e) {
			ErlLogger.error(e);
			fTestRoot = new TestRoot(this);
			fTestResult = null;
		} catch (final CoreException e) {
			ErlLogger.error(e);
			fTestRoot = new TestRoot(this);
			fTestResult = null;
		}
	}

	public void stopTestRun() {
		if (isRunning() || !isKeptAlive()) {
			fIsStopped = true;
		}
		// if (fTestRunnerClient != null) {
		// fTestRunnerClient.stopTest();
		// }
	}

	/**
	 * @return <code>true</code> iff the runtime VM of this test session is
	 *         still alive
	 */
	public boolean isKeptAlive() {
		// if (fTestRunnerClient != null && fLaunch != null
		// && fTestRunnerClient.isRunning()
		// && ILaunchManager.DEBUG_MODE.equals(fLaunch.getLaunchMode())) {
		// final ILaunchConfiguration config = fLaunch
		// .getLaunchConfiguration();
		// try {
		// return config != null
		// && config
		// .getAttribute(
		// JUnitLaunchConfigurationConstants.ATTR_KEEPRUNNING,
		// false);
		// } catch (final CoreException e) {
		// return false;
		// }
		//
		// } else {
		// return false;
		// }
		return false;
	}

	/**
	 * @return <code>true</code> iff this session has been started, but not
	 *         ended nor stopped nor terminated
	 */
	public boolean isRunning() {
		return fIsRunning;
	}

	/**
	 * Reruns the given test method.
	 * 
	 * @param testId
	 *            test id
	 * @param className
	 *            test class name
	 * @param testName
	 *            test method name
	 * @param launchMode
	 *            launch mode, see {@link ILaunchManager}
	 * @param buildBeforeLaunch
	 *            whether a build should be done before launch
	 * @return <code>false</code> iff the rerun could not be started
	 * @throws CoreException
	 *             if the launch fails
	 */
	public boolean rerunTest(final String testId, final String className,
			final String testName, final String launchMode,
			final boolean buildBeforeLaunch) throws CoreException {
		if (isKeptAlive()) {
			final Status status = ((TestCaseElement) getTestElement(testId))
					.getStatus();
			if (status == Status.ERROR) {
				fErrorCount--;
			} else if (status == Status.FAILURE) {
				fFailureCount--;
			}
			// XXX fTestRunnerClient.rerunTest(testId, className, testName);
			return true;

		} else if (fLaunch != null) {
			// run the selected test using the previous launch configuration
			final ILaunchConfiguration launchConfiguration = fLaunch
					.getLaunchConfiguration();
			if (launchConfiguration != null) {

				String name = className;
				if (testName != null) {
					name += "." + testName; //$NON-NLS-1$
				}
				final String configName = MessageFormat.format("Rerun {0}",
						name);
				final ILaunchConfigurationWorkingCopy tmp = launchConfiguration
						.copy(configName);
				// XXX // fix for bug: 64838 junit view run single test does not
				// use
				// // correct class [JUnit]
				// tmp.setAttribute(
				// IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
				// className);
				// // reset the container
				// tmp.setAttribute(
				// JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER,
				//						""); //$NON-NLS-1$
				// if (testName != null) {
				// tmp.setAttribute(
				// JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
				// testName);
				// // String args= "-rerun "+testId;
				// //
				// tmp.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
				// // args);
				// }
				tmp.launch(launchMode, null, buildBeforeLaunch);
				return true;
			}
		}

		return false;
	}

	public TestElement getTestElement(final String id) {
		return fIdToTest.get(id);
	}

	private TestElement addTreeEntry(final String treeEntry) {
		// format: testSuite
		// or    : testSuite/testCase
		final String strings[] = treeEntry.split("/");
		final boolean isSuite = strings.length==1;
final mer att final göra här... final eunit skickar inte final suite om man final bara kör ett final enstaka test, så det final måste hanteras
dessutom final verkar JUnit kräva/vilja ha final antalet tester i final en svit, tror final inte EUnit final har det...

if (fIncompleteTestSuites.isEmpty()) {
			return createTestElement(fTestRoot, id, testName, isSuite,
					testCount);
		} else {
			final int suiteIndex = fIncompleteTestSuites.size() - 1;
			final IncompleteTestSuite openSuite = fIncompleteTestSuites
					.get(suiteIndex);
			openSuite.fOutstandingChildren--;
			if (openSuite.fOutstandingChildren <= 0) {
				fIncompleteTestSuites.remove(suiteIndex);
			}
			return createTestElement(openSuite.fTestSuiteElement, id, testName,
					isSuite, testCount);
		}
	}

	public TestElement createTestElement(final TestSuiteElement parent,
			final String id, final String testName, final boolean isSuite,
			final int testCount) {
		TestElement testElement;
		if (isSuite) {
			final TestSuiteElement testSuiteElement = new TestSuiteElement(
					parent, id, testName, testCount);
			testElement = testSuiteElement;
			if (testCount > 0) {
				fIncompleteTestSuites.add(new IncompleteTestSuite(
						testSuiteElement, testCount));
			}
		} else {
			testElement = new TestCaseElement(parent, id, testName);
		}
		fIdToTest.put(id, testElement);
		return testElement;
	}

	/**
	 * Append the test name from <code>s</code> to <code>testName</code>.
	 * 
	 * @param s
	 *            the string to scan
	 * @param start
	 *            the offset of the first character in <code>s</code>
	 * @param testName
	 *            the result
	 * 
	 * @return the index of the next ','
	 */
	private int scanTestName(final String s, final int start,
			final StringBuffer testName) {
		boolean inQuote = false;
		int i = start;
		for (; i < s.length(); i++) {
			final char c = s.charAt(i);
			if (c == '\\' && !inQuote) {
				inQuote = true;
				continue;
			} else if (inQuote) {
				inQuote = false;
				testName.append(c);
			} else if (c == ',') {
				break;
			} else {
				testName.append(c);
			}
		}
		return i;
	}

	/**
	 * An {@link ITestRunListener2} that listens to events from the
	 * {@link RemoteTestRunnerClient} and translates them into high-level model
	 * events (broadcasted to {@link ITestSessionListener}s).
	 */
	private class TestSessionNotifier implements ITestRunListener2 {

		public void testRunStarted(final int testCount) {
			fIncompleteTestSuites = new ArrayList<TestRunSession.IncompleteTestSuite>();

			fStartedCount = 0;
			fIgnoredCount = 0;
			fFailureCount = 0;
			fErrorCount = 0;
			fTotalCount = testCount;

			fStartTime = System.currentTimeMillis();
			fIsRunning = true;

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionStarted();
			}
		}

		public void testRunEnded(final long elapsedTime) {
			fIsRunning = false;

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionEnded(elapsedTime);
			}
		}

		public void testRunStopped(final long elapsedTime) {
			fIsRunning = false;
			fIsStopped = true;

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.sessionStopped(elapsedTime);
			}
		}

		public void testRunTerminated() {
			fIsRunning = false;
			fIsStopped = true;

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).sessionTerminated();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.internal.junit.model.ITestRunListener2#testTreeEntry
		 * (java.lang.String)
		 */
		public void testTreeEntry(final String description) {
			final TestElement testElement = addTreeEntry(description);

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testAdded(testElement);
			}
		}

		private TestElement createUnrootedTestElement(final String testId,
				final String testName) {
			final TestSuiteElement unrootedSuite = getUnrootedSuite();
			final TestElement testElement = createTestElement(unrootedSuite,
					testId, testName, false, 1);

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testAdded(testElement);
			}

			return testElement;
		}

		private TestSuiteElement getUnrootedSuite() {
			if (fUnrootedSuite == null) {
				fUnrootedSuite = (TestSuiteElement) createTestElement(
						fTestRoot, "-2", "Unrooted Tests", true, 0); //$NON-NLS-1$
			}
			return fUnrootedSuite;
		}

		public void testStarted(final String testId, final String testName) {
			if (fStartedCount == 0) {
				final Object[] listeners = fSessionListeners.getListeners();
				for (int i = 0; i < listeners.length; ++i) {
					((ITestSessionListener) listeners[i]).runningBegins();
				}
			}
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;
			setStatus(testCaseElement, Status.RUNNING);

			fStartedCount++;

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.testStarted(testCaseElement);
			}
		}

		public void testEnded(final String testId, final String testName) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;
			if (testName.startsWith(MessageIds.IGNORED_TEST_PREFIX)) {
				testCaseElement.setIgnored(true);
				fIgnoredCount++;
			}

			if (testCaseElement.getStatus() == Status.RUNNING) {
				setStatus(testCaseElement, Status.OK);
			}

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i])
						.testEnded(testCaseElement);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.eclipse.jdt.internal.junit.model.ITestRunListener2#testFailed
		 * (int, java.lang.String, java.lang.String, java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		public void testFailed(final int statusCode, final String testId,
				final String testName, final String trace,
				final String expected, final String actual) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			}

			final Status status = Status.convert(statusCode);
			registerTestFailureStatus(testElement, status, trace,
					nullifyEmpty(expected), nullifyEmpty(actual));

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((ITestSessionListener) listeners[i]).testFailed(testElement,
						status, trace, expected, actual);
			}
		}

		private String nullifyEmpty(final String string) {
			final int length = string.length();
			if (length == 0) {
				return null;
			} else if (string.charAt(length - 1) == '\n') {
				return string.substring(0, length - 1);
			} else {
				return string;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ITestRunListener2#testReran(String, String, String, int, String,
		 * String, String)
		 */
		public void testReran(final String testId, final String className,
				final String testName, final int statusCode,
				final String trace, final String expectedResult,
				final String actualResult) {
			TestElement testElement = getTestElement(testId);
			if (testElement == null) {
				testElement = createUnrootedTestElement(testId, testName);
			} else if (!(testElement instanceof TestCaseElement)) {
				logUnexpectedTest(testId, testElement);
				return;
			}
			final TestCaseElement testCaseElement = (TestCaseElement) testElement;

			final Status status = Status.convert(statusCode);
			registerTestFailureStatus(testElement, status, trace,
					nullifyEmpty(expectedResult), nullifyEmpty(actualResult));

			final Object[] listeners = fSessionListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				// TODO: post old & new status?
				((ITestSessionListener) listeners[i]).testReran(
						testCaseElement, status, trace, expectedResult,
						actualResult);
			}
		}

		private void logUnexpectedTest(final String testId,
				final TestElement testElement) {
			ErlLogger
					.error(new Exception(
							"Unexpected TestElement type for testId '" + testId + "': " + testElement)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class IncompleteTestSuite {
		public TestSuiteElement fTestSuiteElement;
		public int fOutstandingChildren;

		public IncompleteTestSuite(final TestSuiteElement testSuiteElement,
				final int outstandingChildren) {
			fTestSuiteElement = testSuiteElement;
			fOutstandingChildren = outstandingChildren;
		}
	}

	public void registerTestFailureStatus(final TestElement testElement,
			final Status status, final String trace, final String expected,
			final String actual) {
		testElement.setStatus(status, trace, expected, actual);
		if (status.isError()) {
			fErrorCount++;
		} else if (status.isFailure()) {
			fFailureCount++;
		}
	}

	public void registerTestEnded(final TestElement testElement,
			final boolean completed) {
		if (testElement instanceof TestCaseElement) {
			fTotalCount++;
			if (!completed) {
				return;
			}
			fStartedCount++;
			if (((TestCaseElement) testElement).isIgnored()) {
				fIgnoredCount++;
			}
			if (!testElement.getStatus().isErrorOrFailure()) {
				setStatus(testElement, Status.OK);
			}
		}
	}

	private void setStatus(final TestElement testElement, final Status status) {
		testElement.setStatus(status);
	}

	public TestElement[] getAllFailedTestElements() {
		final ArrayList<ITestElement> failures = new ArrayList<ITestElement>();
		addFailures(failures, getTestRoot());
		return failures.toArray(new TestElement[failures.size()]);
	}

	private void addFailures(final ArrayList<ITestElement> failures,
			final ITestElement testElement) {
		final Result testResult = testElement.getTestResult(true);
		if (testResult == Result.ERROR || testResult == Result.FAILURE) {
			failures.add(testElement);
		}
		if (testElement instanceof TestSuiteElement) {
			final TestSuiteElement testSuiteElement = (TestSuiteElement) testElement;
			final ITestElement[] children = testSuiteElement.getChildren();
			for (int i = 0; i < children.length; i++) {
				addFailures(failures, children[i]);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.junit.model.ITestElement#getElapsedTimeInSeconds()
	 */
	public double getElapsedTimeInSeconds() {
		if (fTestRoot == null) {
			return Double.NaN;
		}

		return fTestRoot.getElapsedTimeInSeconds();
	}

	@Override
	public String toString() {
		return fTestRunName
				+ " " + DateFormat.getDateTimeInstance().format(new Date(fStartTime)); //$NON-NLS-1$
	}

	public void setEventHandler(final EUnitEventHandler eventHandler) {
		eventHandler.addListener(new TestSessionNotifier());

		final ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		launchManager.addLaunchListener(new ILaunchesListener2() {
			public void launchesTerminated(final ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					if (eventHandler != null) {
						eventHandler.shutdown();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			public void launchesRemoved(final ILaunch[] launches) {
				if (Arrays.asList(launches).contains(fLaunch)) {
					if (eventHandler != null) {
						eventHandler.shutdown();
					}
					launchManager.removeLaunchListener(this);
				}
			}

			public void launchesChanged(final ILaunch[] launches) {
			}

			public void launchesAdded(final ILaunch[] launches) {
			}
		});

	}
}
