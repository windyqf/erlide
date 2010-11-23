/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     David Saff (saff@mit.edu) - bug 102632: [JUnit] Support for JUnit 4.
 *     Robert Konigsberg <konigsberg@google.com> - [JUnit] Leverage AbstractJavaLaunchConfigurationDelegate.getMainTypeName in JUnitLaunchConfigurationDelegate - https://bugs.eclipse.org/bugs/show_bug.cgi?id=280114
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *******************************************************************************/
package org.erlide.ui.eunit.internal.launcher;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlFunction;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.util.ErlangFunctionCall;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.jinterface.backend.Backend;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.runtime.backend.ErlideBackend;
import org.erlide.runtime.launch.ErlLaunchAttributes;
import org.erlide.runtime.launch.ErlLaunchData;
import org.erlide.runtime.launch.ErlangLaunchConfigurationDelegate;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangList;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangPid;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.google.common.collect.Lists;

import erlang.ErlideEUnit;

/**
 * Launch configuration delegate for a JUnit test as a Java application.
 * 
 * <p>
 * Clients can instantiate and extend this class.
 * </p>
 * 
 * @since 3.3
 */
public class EUnitLaunchConfigurationDelegate extends
		ErlangLaunchConfigurationDelegate {

	private List<ErlangFunctionCall> fTestElements;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.
	 * eclipse.debug.core.ILaunchConfiguration, java.lang.String,
	 * org.eclipse.debug.core.ILaunch,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public synchronized void launch(final ILaunchConfiguration configuration,
			String mode, final ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		final String testProjectName = configuration.getAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT, "");
		launch.setAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT,
				testProjectName);
		monitor.beginTask(
				MessageFormat.format("{0}...", configuration.getName()), 5); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}

		try {
			if (mode.equals(EUnitLaunchConfigurationConstants.MODE_RUN_QUIETLY_MODE)) {
				launch.setAttribute(
						EUnitLaunchConfigurationConstants.ATTR_NO_DISPLAY,
						"true"); //$NON-NLS-1$
				mode = ILaunchManager.RUN_MODE;
			}

			monitor.subTask("Verifying launch attributes...");

			try {
				preLaunchCheck(configuration, launch, new SubProgressMonitor(
						monitor, 2));
			} catch (final CoreException e) {
				if (e.getStatus().getSeverity() == IStatus.CANCEL) {
					monitor.setCanceled(true);
					return;
				}
				throw e;
			}
			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// get the tests
			fTestElements = evaluateTests(configuration, monitor);
			ErlLogger.debug("fTestElements %s", fTestElements);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// done the verification phase
			monitor.worked(1);

			// set the default source locator if required
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			doLaunch(configuration, mode, launch, false, null);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			fTestElements = null;
			monitor.done();
		}
	}

	// private int evaluatePort() throws CoreException {
	// final int port = SocketUtil.findFreePort();
	// if (port == -1) {
	// abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_no_socket,
	// null,
	// IJavaLaunchConfigurationConstants.ERR_NO_SOCKET_AVAILABLE);
	// }
	// return port;
	// }

	/**
	 * Performs a check on the launch configuration's attributes. If an
	 * attribute contains an invalid value, a {@link CoreException} with the
	 * error is thrown.
	 * 
	 * @param configuration
	 *            the launch configuration to verify
	 * @param launch
	 *            the launch to verify
	 * @param monitor
	 *            the progress monitor to use
	 * @throws CoreException
	 *             an exception is thrown when the verification fails
	 */
	protected void preLaunchCheck(final ILaunchConfiguration configuration,
			final ILaunch launch, final IProgressMonitor monitor)
			throws CoreException {
		try {
			final List<IErlProject> projects = getErlProjects(configuration);
			if (projects == null || projects.isEmpty()) {
				abort("no projects found", null,
						DebugException.CONFIGURATION_INVALID);
			}
			// if (!CoreTestSearchEngine.hasTestCaseType(erlProject)) {
			// abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junitnotonpath,
			// null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			// }
			//
			// final ITestKind testKind = getTestRunnerKind(configuration);
			// final boolean isJUnit4Configuration =
			// TestKindRegistry.JUNIT4_TEST_KIND_ID
			// .equals(testKind.getId());
			// if (isJUnit4Configuration
			// && !CoreTestSearchEngine.hasTestAnnotation(erlProject)) {
			// abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_junit4notonpath,
			// null, IJUnitStatusConstants.ERR_JUNIT_NOT_ON_PATH);
			// }
		} finally {
			monitor.done();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate#
	 * verifyMainTypeName(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public String verifyMainTypeName(final ILaunchConfiguration configuration)
			throws CoreException {
		return "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner"; //$NON-NLS-1$
	}

	/**
	 * Evaluates all test elements selected by the given launch configuration.
	 * The elements are of type {@link IErlModule} or {@link IErlFunction}. At
	 * the moment it is only possible to run a single method or a set of types,
	 * but not mixed or more than one method at a time.
	 * 
	 * @param configuration
	 *            the launch configuration to inspect
	 * @param monitor
	 *            the progress monitor
	 * @return returns all types or methods that should be ran
	 * @throws CoreException
	 *             an exception is thrown when the search for tests failed
	 */
	protected List<ErlangFunctionCall> evaluateTests(
			final ILaunchConfiguration configuration,
			final IProgressMonitor monitor) throws CoreException {
		List<IErlProject> erlProjects = getErlProjects(configuration);
		final String testProject = configuration.getAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT, ""); //$NON-NLS-1$
		erlProjects = filterTestProjects(testProject, erlProjects);
		List<ErlangFunctionCall> testFunctions = getTestFunctions(monitor,
				erlProjects);
		final String testModuleName = configuration.getAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_MODULE, ""); //$NON-NLS-1$
		testFunctions = filterTestModule(testModuleName, testFunctions);
		final String testFunctionName = configuration.getAttribute(
				EUnitLaunchConfigurationConstants.ATTR_TEST_FUNCTION, ""); //$NON-NLS-1$
		testFunctions = filterTestFunction(testFunctionName, testFunctions);
		if (testFunctions.isEmpty()) {
			final String msg = MessageFormat.format(
					"No tests found with test runner ''{0}''.", "EUnit");
			abort(msg, null, DebugException.REQUEST_FAILED);// FIXME byt code
		}
		return testFunctions;
	}

	private List<ErlangFunctionCall> filterTestFunction(
			final String testFunctionName,
			final List<ErlangFunctionCall> testFunctions) {
		if (testFunctionName.length() == 0) {
			return testFunctions;
		}
		final List<ErlangFunctionCall> result = Lists.newArrayList();
		for (final ErlangFunctionCall erlangFunctionCall : result) {
			if (erlangFunctionCall.getName().equals(testFunctionName)) {
				result.add(erlangFunctionCall);
			}
		}
		return result;
	}

	private List<ErlangFunctionCall> getTestFunctions(
			final IProgressMonitor monitor, final List<IErlProject> erlProjects)
			throws CoreException {
		final IErlangTestFinder finder = new EUnitTestFinder();
		final List<ErlangFunctionCall> testFunctions = Lists.newArrayList();
		for (final IErlProject erlProject : erlProjects) {
			final List<ErlangFunctionCall> testsInContainer = finder
					.findTestsInContainer(erlProject, erlProject, monitor);
			testFunctions.addAll(testsInContainer);
		}
		return testFunctions;
	}

	private List<ErlangFunctionCall> filterTestModule(
			final String testModuleName,
			final List<ErlangFunctionCall> testFunctions) {
		if (testModuleName.length() == 0) {
			return testFunctions;
		}
		final List<ErlangFunctionCall> result = Lists.newArrayList();
		for (final ErlangFunctionCall erlangFunctionCall : testFunctions) {
			if (erlangFunctionCall.getModule().equals(testModuleName)) {
				result.add(erlangFunctionCall);
			}
		}
		return result;
	}

	private List<IErlProject> filterTestProjects(final String testProjectName,
			final List<IErlProject> erlProjects) {
		if (testProjectName.length() > 0) {
			for (final IErlProject project : erlProjects) {
				if (project.getName().equals(testProjectName)) {
					return Lists.newArrayList(project);
				}
			}
		}
		return erlProjects;
	}

	// private final IErlElement getTestTarget(
	// final ILaunchConfiguration configuration,
	// final IErlProject erlProject) throws CoreException {
	// // final String containerHandle = configuration.getAttribute(
	//		//				EUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, ""); //$NON-NLS-1$
	// // if (containerHandle.length() != 0) {
	// // final IErlElement element = ErlangCore.
	// // JavaCore.create(containerHandle);
	// // if (element == null || !element.exists()) {
	// //
	// abort(JUnitMessages.JUnitLaunchConfigurationDelegate_error_input_element_deosn_not_exist,
	// // null,
	// // IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
	// // }
	// // return element;
	// // }
	// final String testTypeName = getMainTypeName(configuration);
	// if (testTypeName != null && testTypeName.length() != 0) {
	// final IType type = javaProject.findType(testTypeName);
	// if (type != null && type.exists()) {
	// return type;
	// }
	// }
	// abort(JUnitMessages.JUnitLaunchConfigurationDelegate_input_type_does_not_exist,
	// null,
	// IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
	// return null; // not reachable
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jdt.internal.junit.launcher.ITestFindingAbortHandler#abort
	 * (java.lang.String, java.lang.Throwable, int)
	 */
	protected void abort(final String message, final Throwable exception,
			final int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR,
				EUnitPlugin.PLUGIN_ID, code, message, exception));
	}

	public static List<IErlProject> getErlProjects(
			final ILaunchConfiguration configuration) {
		try {
			final String projectNames = configuration.getAttribute(
					ErlLaunchAttributes.PROJECTS, (String) null);
			final String[] names = projectNames.split(";");
			final List<IErlProject> result = new ArrayList<IErlProject>(
					names.length);
			for (final String name : names) {
				result.add(ErlangCore.getModel().getErlangProject(name));
			}
			return result;
		} catch (final CoreException e) {
		}
		return null;
	}

	// FIXME JC is this better done with a backend listener?
	@Override
	protected void postLaunch(final String mode, final ErlLaunchData data,
			final Set<IProject> projects, final ErlideBackend backend,
			final ILaunch launch) throws DebugException {
		backend.getEventDaemon().addHandler(
				new EUnitEventHandler(backend.getEventPid(), launch, backend));
		super.postLaunch(mode, data, projects, backend, launch);
	}

	@Override
	protected void runInitial(final String module, final String function,
			final String args, final Backend backend) {
		final OtpErlangPid jpid = backend.getEventPid();
		final OtpErlangObject elems[] = new OtpErlangObject[fTestElements
				.size()];
		for (int i = 0; i < elems.length; i++) {
			final ErlangFunctionCall erlangFunctionCall = fTestElements.get(i);
			final OtpErlangAtom modA = new OtpErlangAtom(
					erlangFunctionCall.getModule());
			final OtpErlangAtom funA = new OtpErlangAtom(
					erlangFunctionCall.getName());
			elems[i] = new OtpErlangTuple(new OtpErlangObject[] { modA, funA });
		}
		final OtpErlangList tests = new OtpErlangList(elems);
		ErlideEUnit.runTests(backend, tests, jpid);
	}
}
