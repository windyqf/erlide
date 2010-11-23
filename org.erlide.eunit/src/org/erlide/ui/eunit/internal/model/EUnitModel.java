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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlProject;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.EUnitPreferencesConstants;
import org.erlide.eunit.TestRunListener;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.eunit.internal.launcher.EUnitEventHandler;
import org.erlide.ui.eunit.internal.launcher.EUnitLaunchConfigurationConstants;
import org.erlide.ui.eunit.internal.launcher.EUnitLaunchConfigurationDelegate;
import org.erlide.ui.eunit.model.ITestRunSession;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * Central registry for EUnit test runs.
 */
public final class EUnitModel {

	private final class EUnitLaunchListener implements ILaunchListener {

		/**
		 * Used to track new launches. We need to do this so that we only attach
		 * a TestRunner once to a launch. Once a test runner is connected, it is
		 * removed from the set.
		 */
		private final HashSet<ILaunch> fTrackedLaunches = new HashSet<ILaunch>(
				20);

		/*
		 * @see ILaunchListener#launchAdded(ILaunch)
		 */
		public void launchAdded(final ILaunch launch) {
			fTrackedLaunches.add(launch);
		}

		/*
		 * @see ILaunchListener#launchRemoved(ILaunch)
		 */
		public void launchRemoved(final ILaunch launch) {
			fTrackedLaunches.remove(launch);
			// TODO: story for removing old test runs?
			// getDisplay().asyncExec(new Runnable() {
			// public void run() {
			// TestRunnerViewPart testRunnerViewPart=
			// findTestRunnerViewPartInActivePage();
			// if (testRunnerViewPart != null && testRunnerViewPart.isCreated()
			// && launch.equals(testRunnerViewPart.getLastLaunch()))
			// testRunnerViewPart.reset();
			// }
			// });
		}

		/*
		 * @see ILaunchListener#launchChanged(ILaunch)
		 */
		public void launchChanged(final ILaunch launch) {
			if (!fTrackedLaunches.contains(launch)) {
				return;
			}

			final ILaunchConfiguration configuration = launch
					.getLaunchConfiguration();
			if (configuration == null) {
				return;
			}

			final Collection<IErlProject> erlProjects = EUnitLaunchConfigurationDelegate
					.getErlProjects(configuration);
			if (erlProjects == null || erlProjects.isEmpty()) {
				return;
			}

			// test whether the launch defines the EUnit attributes
			final String testProjectName = launch
					.getAttribute(EUnitLaunchConfigurationConstants.ATTR_TEST_PROJECT);
			if (testProjectName != null && testProjectName.length() > 0) {
				fTrackedLaunches.remove(launch);
				final IErlProject project = ErlangCore.getModel()
						.getErlangProject(testProjectName);
				connectTestRunner(launch, project, testProjectName);
			}
		}

		private void connectTestRunner(final ILaunch launch,
				final IErlProject erlProject, final String name) {
			final TestRunSession testRunSession = new TestRunSession(launch,
					name, erlProject);
			addTestRunSession(testRunSession);

			final Object[] listeners = EUnitPlugin.getDefault()
					.getTestRunListeners().getListeners();
			for (int i = 0; i < listeners.length; i++) {
				((TestRunListener) listeners[i])
						.sessionLaunched(testRunSession);
			}
		}
	}

	private final ListenerList fTestRunSessionListeners = new ListenerList();
	/**
	 * Active test run sessions, youngest first.
	 */
	private final LinkedList<TestRunSession> fTestRunSessions = new LinkedList<TestRunSession>();
	private final ILaunchListener fLaunchListener = new EUnitLaunchListener();
	private final List<EUnitEventHandler> fEventHandlers = Lists.newArrayList();

	/**
	 * Starts the model (called by the {@link EUnitPlugin} on startup).
	 */
	public void start() {
		final ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		launchManager.addLaunchListener(fLaunchListener);

		/*
		 * TODO: restore on restart: - only import headers! - only import last n
		 * sessions; remove all other files in historyDirectory
		 */
		// File historyDirectory= JUnitPlugin.getHistoryDirectory();
		// File[] swapFiles= historyDirectory.listFiles();
		// if (swapFiles != null) {
		// Arrays.sort(swapFiles, new Comparator() {
		// public int compare(Object o1, Object o2) {
		// String name1= ((File) o1).getName();
		// String name2= ((File) o2).getName();
		// return name1.compareTo(name2);
		// }
		// });
		// for (int i= 0; i < swapFiles.length; i++) {
		// final File file= swapFiles[i];
		// SafeRunner.run(new ISafeRunnable() {
		// public void run() throws Exception {
		// importTestRunSession(file );
		// }
		// public void handleException(Throwable exception) {
		// JUnitPlugin.log(exception);
		// }
		// });
		// }
		// }

	}

	/**
	 * Stops the model (called by the {@link JUnitCorePlugin} on shutdown).
	 */
	public void stop() {
		final ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		launchManager.removeLaunchListener(fLaunchListener);

		final File historyDirectory = EUnitPlugin.getHistoryDirectory();
		final File[] swapFiles = historyDirectory.listFiles();
		if (swapFiles != null) {
			for (int i = 0; i < swapFiles.length; i++) {
				swapFiles[i].delete();
			}
		}

		// for (Iterator iter= fTestRunSessions.iterator(); iter.hasNext();) {
		// final TestRunSession session= (TestRunSession) iter.next();
		// SafeRunner.run(new ISafeRunnable() {
		// public void run() throws Exception {
		// session.swapOut();
		// }
		// public void handleException(Throwable exception) {
		// JUnitPlugin.log(exception);
		// }
		// });
		// }
	}

	public void addTestRunSessionListener(final ITestRunSessionListener listener) {
		fTestRunSessionListeners.add(listener);
	}

	public void removeTestRunSessionListener(
			final ITestRunSessionListener listener) {
		fTestRunSessionListeners.remove(listener);
	}

	/**
	 * @return a list of active {@link TestRunSession}s. The list is a copy of
	 *         the internal data structure and modifications do not affect the
	 *         global list of active sessions. The list is sorted by age,
	 *         youngest first.
	 */
	public synchronized List<TestRunSession> getTestRunSessions() {
		return new ArrayList<TestRunSession>(fTestRunSessions);
	}

	/**
	 * Adds the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 * 
	 * @param testRunSession
	 *            the session to add
	 */
	public void addTestRunSession(final TestRunSession testRunSession) {
		Assert.isNotNull(testRunSession);
		final ArrayList<TestRunSession> toRemove = new ArrayList<TestRunSession>();

		synchronized (this) {
			Assert.isLegal(!fTestRunSessions.contains(testRunSession));
			fTestRunSessions.addFirst(testRunSession);

			final int maxCount = Platform.getPreferencesService().getInt(
					EUnitPlugin.PLUGIN_ID,
					EUnitPreferencesConstants.MAX_TEST_RUNS, 10, null);
			final int size = fTestRunSessions.size();
			if (size > maxCount) {
				final List<TestRunSession> excess = fTestRunSessions.subList(
						maxCount, size);
				for (final Iterator<TestRunSession> iter = excess.iterator(); iter
						.hasNext();) {
					final TestRunSession oldSession = iter.next();
					if (!(oldSession.isStarting() || oldSession.isRunning() || oldSession
							.isKeptAlive())) {
						toRemove.add(oldSession);
						iter.remove();
					}
				}
			}
		}
		for (final TestRunSession i : toRemove) {
			notifyTestRunSessionRemoved(i);
		}
		notifyTestRunSessionAdded(testRunSession);
	}

	/**
	 * Imports a test run session from the given file.
	 * 
	 * @param file
	 *            a file containing a test run session transcript
	 * @return the imported test run session
	 * @throws CoreException
	 *             if the import failed
	 */
	public static TestRunSession importTestRunSession(final File file)
			throws CoreException {
		try {
			final SAXParserFactory parserFactory = SAXParserFactory
					.newInstance();
			// parserFactory.setValidating(true); // TODO: add DTD and debug
			// flag
			final SAXParser parser = parserFactory.newSAXParser();
			final TestRunHandler handler = new TestRunHandler();
			parser.parse(file, handler);
			final TestRunSession session = handler.getTestRunSession();
			EUnitPlugin.getModel().addTestRunSession(session);
			return session;
		} catch (final ParserConfigurationException e) {
			throwImportError(file, e);
		} catch (final SAXException e) {
			throwImportError(file, e);
		} catch (final IOException e) {
			throwImportError(file, e);
		}
		return null; // does not happen
	}

	// /**
	// * Imports a test run session from the given URL.
	// *
	// * @param url
	// * an URL to a test run session transcript
	// * @param monitor
	// * a progress monitor for cancellation
	// * @return the imported test run session
	// * @throws InvocationTargetException
	// * wrapping a CoreException if the import failed
	// * @throws InterruptedException
	// * if the import was cancelled
	// * @since 3.6
	// */
	// public static TestRunSession importTestRunSession(final String url,
	// final IProgressMonitor monitor) throws InvocationTargetException,
	// InterruptedException {
	// monitor.beginTask(ModelMessages.JUnitModel_importing_from_url,
	// IProgressMonitor.UNKNOWN);
	// final TestRunHandler handler = new TestRunHandler();
	//
	// final CoreException[] exception = { null };
	// final TestRunSession[] session = { null };
	//
	//		final Thread importThread = new Thread("JUnit URL importer") { //$NON-NLS-1$
	// @Override
	// public void run() {
	// try {
	// final SAXParserFactory parserFactory = SAXParserFactory
	// .newInstance();
	// // parserFactory.setValidating(true); // TODO: add DTD and
	// // debug flag
	// final SAXParser parser = parserFactory.newSAXParser();
	// parser.parse(url, handler);
	// session[0] = handler.getTestRunSession();
	// } catch (final ParserConfigurationException e) {
	// storeImportError(e);
	// } catch (final SAXException e) {
	// storeImportError(e);
	// } catch (final IOException e) {
	// storeImportError(e);
	// }
	// }
	//
	// private void storeImportError(final Exception e) {
	// exception[0] = new CoreException(
	// new org.eclipse.core.runtime.Status(IStatus.ERROR,
	// JUnitCorePlugin.getPluginId(),
	// ModelMessages.JUnitModel_could_not_import, e));
	// }
	// };
	// importThread.start();
	//
	// while (session[0] == null && exception[0] == null
	// && !monitor.isCanceled()) {
	// try {
	// Thread.sleep(100);
	// } catch (final InterruptedException e) {
	// // that's OK
	// }
	// }
	// if (session[0] == null) {
	// if (exception[0] != null) {
	// throw new InvocationTargetException(exception[0]);
	// } else {
	// importThread.interrupt(); // have to kill the thread since we
	// // don't control URLConnection and
	// // XML
	// // parsing
	// throw new InterruptedException();
	// }
	// }
	//
	// JUnitCorePlugin.getModel().addTestRunSession(session[0]);
	// monitor.done();
	// return session[0];
	// }

	public static void importIntoTestRunSession(final File swapFile,
			final TestRunSession testRunSession) throws CoreException {
		try {
			final SAXParserFactory parserFactory = SAXParserFactory
					.newInstance();
			// parserFactory.setValidating(true); // TODO: add DTD and debug
			// flag
			final SAXParser parser = parserFactory.newSAXParser();
			final TestRunHandler handler = new TestRunHandler(testRunSession);
			parser.parse(swapFile, handler);
		} catch (final ParserConfigurationException e) {
			throwImportError(swapFile, e);
		} catch (final SAXException e) {
			throwImportError(swapFile, e);
		} catch (final IOException e) {
			throwImportError(swapFile, e);
		}
	}

	/**
	 * Exports the given test run session.
	 * 
	 * @param testRunSession
	 *            the test run session
	 * @param file
	 *            the destination
	 * @throws CoreException
	 *             if an error occurred
	 */
	public static void exportTestRunSession(
			final TestRunSession testRunSession, final File file)
			throws CoreException {
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			exportTestRunSession(testRunSession, out);

		} catch (final IOException e) {
			throwExportError(file, e);
		} catch (final TransformerConfigurationException e) {
			throwExportError(file, e);
		} catch (final TransformerException e) {
			throwExportError(file, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (final IOException e2) {
					ErlLogger.error(e2);
				}
			}
		}
	}

	public static void exportTestRunSession(
			final TestRunSession testRunSession, final OutputStream out)
			throws TransformerFactoryConfigurationError, TransformerException {

		final Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		final InputSource inputSource = new InputSource();
		final SAXSource source = new SAXSource(new TestRunSessionSerializer(
				testRunSession), inputSource);
		final StreamResult result = new StreamResult(out);
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		/*
		 * Bug in Xalan: Only indents if proprietary property
		 * org.apache.xalan.templates.OutputProperties.S_KEY_INDENT_AMOUNT is
		 * set.
		 * 
		 * Bug in Xalan as shipped with J2SE 5.0: Does not read the
		 * indent-amount property at all >:-(.
		 */
		try {
			transformer.setOutputProperty(
					"{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (final IllegalArgumentException e) {
			// no indentation today...
		}
		transformer.transform(source, result);
	}

	private static void throwExportError(final File file, final Exception e)
			throws CoreException {
		throw new CoreException(new org.eclipse.core.runtime.Status(
				IStatus.ERROR, EUnitPlugin.PLUGIN_ID, MessageFormat.format(
						"The test run could not be written to file ''{0}''.",
						file.toString()), e));
	}

	private static void throwImportError(final File file, final Exception e)
			throws CoreException {
		throw new CoreException(
				new org.eclipse.core.runtime.Status(
						IStatus.ERROR,
						EUnitPlugin.PLUGIN_ID,
						MessageFormat
								.format("The test run could not be imported from file ''{0}''.",
										file.toString()), e));
	}

	/**
	 * Removes the given {@link TestRunSession} and notifies all registered
	 * {@link ITestRunSessionListener}s.
	 * 
	 * @param testRunSession
	 *            the session to remove
	 */
	public void removeTestRunSession(final TestRunSession testRunSession) {
		boolean existed;
		synchronized (this) {
			existed = fTestRunSessions.remove(testRunSession);
		}
		if (existed) {
			notifyTestRunSessionRemoved(testRunSession);
		}
		testRunSession.removeSwapFile();
	}

	private void notifyTestRunSessionRemoved(final TestRunSession testRunSession) {
		testRunSession.stopTestRun();
		final ILaunch launch = testRunSession.getLaunch();
		if (launch != null) {
			final ILaunchManager launchManager = DebugPlugin.getDefault()
					.getLaunchManager();
			launchManager.removeLaunch(launch);
		}

		final Object[] listeners = fTestRunSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestRunSessionListener) listeners[i])
					.sessionRemoved(testRunSession);
		}
	}

	private void notifyTestRunSessionAdded(final TestRunSession testRunSession) {
		final Object[] listeners = fTestRunSessionListeners.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((ITestRunSessionListener) listeners[i])
					.sessionAdded(testRunSession);
		}
	}

	public void addEventHandler(final EUnitEventHandler eventHandler) {
		fEventHandlers.add(eventHandler);
		final ITestRunSession testRunSession = getTestRunSessionForLaunch(eventHandler
				.getLaunch());
		testRunSession.setEventHandler(eventHandler);
	}

	private ITestRunSession getTestRunSessionForLaunch(final ILaunch launch) {
		for (final ITestRunSession testRunSession : fTestRunSessions) {
			if (testRunSession.getLaunch().equals(launch)) {
				return testRunSession;
			}
		}
		return null;
	}

	public EUnitEventHandler getEventHandlerForLaunch(final ILaunch launch) {
		for (final EUnitEventHandler eventHandler : fEventHandlers) {
			if (eventHandler.getLaunch().equals(launch)) {
				return eventHandler;
			}
		}
		return null;
	}

}
