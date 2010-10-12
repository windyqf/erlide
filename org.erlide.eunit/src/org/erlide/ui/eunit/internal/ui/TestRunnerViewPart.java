/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Julien Ruaux: jruaux@octo.com see bug 25324 Ability to know when tests are finished [junit]
 *     Vincent Massol: vmassol@octo.com 25324 Ability to know when tests are finished [junit]
 *     Sebastian Davids: sdavids@gmx.de 35762 JUnit View wasting a lot of screen space [JUnit]
 *     Brock Janiczak (brockj@tpg.com.au)
 *         - https://bugs.eclipse.org/bugs/show_bug.cgi?id=102236: [JUnit] display execution time next to each test
 *     Achim Demelt <a.demelt@exxcellent.de> - [junit] Separate UI from non-UI code - https://bugs.eclipse.org/bugs/show_bug.cgi?id=278844
 *     Andrew Eisenberg <andrew@eisenberg.as> - [JUnit] Rerun failed first does not work with JUnit4 - https://bugs.eclipse.org/bugs/show_bug.cgi?id=140392
 *******************************************************************************/
package org.erlide.ui.eunit.internal.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.PageSwitcher;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.eclipse.ui.progress.UIJob;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlElementDelta;
import org.erlide.core.erlang.IErlProject;
import org.erlide.core.erlang.util.ElementChangedEvent;
import org.erlide.core.erlang.util.IElementChangedListener;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.EUnitPreferencesConstants;
import org.erlide.jinterface.util.ErlLogger;
import org.erlide.ui.eunit.internal.launcher.EUnitLaunchConfigurationConstants;
import org.erlide.ui.eunit.internal.model.EUnitModel;
import org.erlide.ui.eunit.internal.model.ITestRunSessionListener;
import org.erlide.ui.eunit.internal.model.ITestSessionListener;
import org.erlide.ui.eunit.internal.model.TestCaseElement;
import org.erlide.ui.eunit.internal.model.TestElement;
import org.erlide.ui.eunit.internal.model.TestRunSession;
import org.erlide.ui.eunit.internal.ui.viewsupport.ViewHistory;
import org.erlide.ui.eunit.model.ITestElement.Result;
import org.erlide.ui.eunit.ui.CounterPanel;
import org.erlide.ui.eunit.ui.EUnitProgressBar;
import org.erlide.ui.eunit.ui.ProgressImages;

/**
 * A ViewPart that shows the results of a test run.
 */
public class TestRunnerViewPart extends ViewPart {

	public static final String NAME = "org.eclipse.jdt.junit.ResultView"; //$NON-NLS-1$

	private static final String RERUN_LAST_COMMAND = "org.eclipse.jdt.junit.junitShortcut.rerunLast"; //$NON-NLS-1$
	private static final String RERUN_FAILED_FIRST_COMMAND = "org.eclipse.jdt.junit.junitShortcut.rerunFailedFirst"; //$NON-NLS-1$

	static final int REFRESH_INTERVAL = 200;

	static final int LAYOUT_FLAT = 0;
	static final int LAYOUT_HIERARCHICAL = 1;

	/**
	 * Whether the output scrolls and reveals tests as they are executed.
	 */
	protected boolean fAutoScroll = true;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or
	 * <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation = VIEW_ORIENTATION_AUTOMATIC;
	/**
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>.
	 */
	private int fCurrentOrientation;
	/**
	 * The current layout mode (LAYOUT_FLAT or LAYOUT_HIERARCHICAL).
	 */
	private int fLayout = LAYOUT_HIERARCHICAL;

	// private boolean fTestIsRunning= false;

	protected EUnitProgressBar fProgressBar;
	protected ProgressImages fProgressImages;
	protected Image fViewImage;
	protected CounterPanel fCounterPanel;
	protected boolean fShowOnErrorOnly = false;
	protected Clipboard fClipboard;
	protected volatile String fInfoMessage;

	private FailureTrace fFailureTrace;

	private TestViewer fTestViewer;
	/**
	 * Is the UI disposed?
	 */
	private boolean fIsDisposed = false;

	/**
	 * Actions
	 */
	private Action fNextAction;
	private Action fPreviousAction;

	private StopAction fStopAction;
	private EUnitCopyAction fCopyAction;

	private Action fRerunLastTestAction;
	private IHandlerActivation fRerunLastActivation;
	private Action fRerunFailedFirstAction;
	private IHandlerActivation fRerunFailedFirstActivation;

	private Action fFailuresOnlyFilterAction;
	private ScrollLockAction fScrollLockAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
	private ShowTestHierarchyAction fShowTestHierarchyAction;
	private ShowTimeAction fShowTimeAction;
	private ActivateOnErrorAction fActivateOnErrorAction;
	private IMenuListener fViewMenuListener;

	private TestRunSession fTestRunSession;
	private TestSessionListener fTestSessionListener;

	private RunnerViewHistory fViewHistory;
	private TestRunSessionListener fTestRunSessionListener;

	final Image fStackViewIcon;
	final Image fTestRunOKIcon;
	final Image fTestRunFailIcon;
	final Image fTestRunOKDirtyIcon;
	final Image fTestRunFailDirtyIcon;

	final Image fTestIcon;
	final Image fTestOkIcon;
	final Image fTestErrorIcon;
	final Image fTestFailIcon;
	final Image fTestRunningIcon;
	final Image fTestIgnoredIcon;

	final ImageDescriptor fSuiteIconDescriptor = EUnitPlugin
			.getImageDescriptor("obj16/tsuite.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteOkIconDescriptor = EUnitPlugin
			.getImageDescriptor("obj16/tsuiteok.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteErrorIconDescriptor = EUnitPlugin
			.getImageDescriptor("obj16/tsuiteerror.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteFailIconDescriptor = EUnitPlugin
			.getImageDescriptor("obj16/tsuitefail.gif"); //$NON-NLS-1$
	final ImageDescriptor fSuiteRunningIconDescriptor = EUnitPlugin
			.getImageDescriptor("obj16/tsuiterun.gif"); //$NON-NLS-1$

	final Image fSuiteIcon;
	final Image fSuiteOkIcon;
	final Image fSuiteErrorIcon;
	final Image fSuiteFailIcon;
	final Image fSuiteRunningIcon;

	final List<Image> fImagesToDispose;

	// Persistence tags.
	static final String TAG_PAGE = "page"; //$NON-NLS-1$
	static final String TAG_RATIO = "ratio"; //$NON-NLS-1$
	static final String TAG_TRACEFILTER = "tracefilter"; //$NON-NLS-1$
	static final String TAG_ORIENTATION = "orientation"; //$NON-NLS-1$
	static final String TAG_SCROLL = "scroll"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_LAYOUT = "layout"; //$NON-NLS-1$
	/**
	 * @since 3.2
	 */
	static final String TAG_FAILURES_ONLY = "failuresOnly"; //$NON-NLS-1$
	/**
	 * @since 3.4
	 */
	static final String TAG_SHOW_TIME = "time"; //$NON-NLS-1$

	/**
	 * @since 3.5
	 */
	static final String PREF_LAST_PATH = "lastImportExportPath"; //$NON-NLS-1$

	/**
	 * @since 3.6
	 */
	static final String PREF_LAST_URL = "lastImportURL"; //$NON-NLS-1$

	// orientations
	static final int VIEW_ORIENTATION_VERTICAL = 0;
	static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	static final int VIEW_ORIENTATION_AUTOMATIC = 2;

	private IMemento fMemento;

	Image fOriginalViewImage;
	IElementChangedListener fDirtyListener;

	// private CTabFolder fTabFolder;
	private SashForm fSashForm;

	private Composite fCounterComposite;
	private Composite fParent;

	/**
	 * A Job that periodically updates view description, counters, and progress
	 * bar.
	 */
	private UpdateUIJob fUpdateJob;

	/**
	 * A Job that runs as long as a test run is running. It is used to show
	 * busyness for running jobs in the view (title in italics).
	 */
	private JUnitIsRunningJob fJUnitIsRunningJob;
	private ILock fJUnitIsRunningLock;
	public static final Object FAMILY_JUNIT_RUN = new Object();

	private final IPartListener2 fPartListener = new IPartListener2() {
		public void partActivated(final IWorkbenchPartReference ref) {
		}

		public void partBroughtToTop(final IWorkbenchPartReference ref) {
		}

		public void partInputChanged(final IWorkbenchPartReference ref) {
		}

		public void partClosed(final IWorkbenchPartReference ref) {
		}

		public void partDeactivated(final IWorkbenchPartReference ref) {
		}

		public void partOpened(final IWorkbenchPartReference ref) {
		}

		public void partVisible(final IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = true;
			}
		}

		public void partHidden(final IWorkbenchPartReference ref) {
			if (getSite().getId().equals(ref.getId())) {
				fPartIsVisible = false;
			}
		}
	};

	protected boolean fPartIsVisible = false;

	private class RunnerViewHistory extends ViewHistory {

		@Override
		public void configureHistoryListAction(final IAction action) {
			action.setText("&History...");
		}

		@Override
		public void configureHistoryDropDownAction(final IAction action) {
			action.setToolTipText("Test Run History...");
			EUnitPlugin.setLocalImageDescriptors(action, "history_list.gif"); //$NON-NLS-1$
		}

		@Override
		public Action getClearAction() {
			return new ClearAction();
		}

		@Override
		public String getHistoryListDialogTitle() {
			return "Test Runs";
		}

		@Override
		public String getHistoryListDialogMessage() {
			return "&Select a test run:";
		}

		@Override
		public Shell getShell() {
			return fParent.getShell();
		}

		@Override
		public List<?> getHistoryEntries() {
			return EUnitPlugin.getModel().getTestRunSessions();
		}

		@Override
		public Object getCurrentEntry() {
			return fTestRunSession;
		}

		@Override
		public void setActiveEntry(final Object entry) {
			final TestRunSession deactivatedSession = setActiveTestRunSession((TestRunSession) entry);
			if (deactivatedSession != null) {
				deactivatedSession.swapOut();
			}
		}

		@Override
		public void setHistoryEntries(final List<?> remainingEntries,
				final Object activeEntry) {
			setActiveTestRunSession((TestRunSession) activeEntry);

			final EUnitModel model = EUnitPlugin.getModel();
			final List<TestRunSession> testRunSessions = model
					.getTestRunSessions();
			testRunSessions.removeAll(remainingEntries);
			for (final TestRunSession session : testRunSessions) {
				model.removeTestRunSession(session);
			}
			for (final Iterator<?> iter = remainingEntries.iterator(); iter
					.hasNext();) {
				final TestRunSession remaining = (TestRunSession) iter.next();
				remaining.swapOut();
			}
		}

		@Override
		public ImageDescriptor getImageDescriptor(final Object element) {
			final TestRunSession session = (TestRunSession) element;
			if (session.isStopped()) {
				return fSuiteIconDescriptor;
			}

			if (session.isRunning()) {
				return fSuiteRunningIconDescriptor;
			}

			final Result result = session.getTestResult(true);
			if (result == Result.OK) {
				return fSuiteOkIconDescriptor;
			} else if (result == Result.ERROR) {
				return fSuiteErrorIconDescriptor;
			} else if (result == Result.FAILURE) {
				return fSuiteFailIconDescriptor;
			} else {
				return fSuiteIconDescriptor;
			}
		}

		@Override
		public String getText(final Object element) {
			final TestRunSession session = (TestRunSession) element;
			final String testRunLabel = session.getTestRunName();
			if (session.getStartTime() <= 0) {
				return testRunLabel;
			} else {
				final String startTime = DateFormat.getDateTimeInstance()
						.format(new Date(session.getStartTime()));
				return MessageFormat.format("{0} ({1})", testRunLabel,
						startTime);
			}
		}

		@Override
		public void addMenuEntries(final MenuManager manager) {
			// manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
			// new ImportTestRunSessionAction(fParent.getShell()));
			// manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
			// new ImportTestRunSessionFromURLAction(fParent.getShell()));
			// if (fTestRunSession != null) {
			// manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS,
			// new ExportTestRunSessionAction(fParent.getShell(),
			// fTestRunSession));
			// }
		}

		@Override
		public String getMaxEntriesMessage() {
			return "&Maximum count of remembered test runs:";
		}

		@Override
		public int getMaxEntries() {
			return Platform.getPreferencesService().getInt(
					EUnitPlugin.PLUGIN_ID,
					EUnitPreferencesConstants.MAX_TEST_RUNS, 10, null);
		}

		@Override
		public void setMaxEntries(final int maxEntries) {
			new InstanceScope().getNode(EUnitPlugin.PLUGIN_ID).putInt(
					EUnitPreferencesConstants.MAX_TEST_RUNS, maxEntries);
		}
	}

	// private static class ImportTestRunSessionAction extends Action {
	// private final Shell fShell;
	//
	// public ImportTestRunSessionAction(final Shell shell) {
	// super(
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_name);
	// fShell = shell;
	// }
	//
	// public void run() {
	// final FileDialog importDialog = new FileDialog(fShell, SWT.OPEN);
	// importDialog
	// .setText(JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_title);
	// final IDialogSettings dialogSettings = JUnitPlugin.getDefault()
	// .getDialogSettings();
	// final String lastPath = dialogSettings.get(PREF_LAST_PATH);
	// if (lastPath != null) {
	// importDialog.setFilterPath(lastPath);
	// }
	//			importDialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
	// final String path = importDialog.open();
	// if (path == null) {
	// return;
	// }
	//
	// // TODO: MULTI: getFileNames()
	// final File file = new File(path);
	//
	// try {
	// EUnitModel.importTestRunSession(file);
	// } catch (final CoreException e) {
	// JUnitPlugin.log(e);
	// ErrorDialog
	// .openError(
	// fShell,
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_error_title,
	// e.getStatus().getMessage(), e.getStatus());
	// }
	// }
	// }

	// private static class ImportTestRunSessionFromURLAction extends Action {
	// private static class URLValidator implements IInputValidator {
	// public String isValid(final String newText) {
	// if (newText.length() == 0) {
	// return null;
	// }
	// try {
	// new URL(newText);
	// return null;
	// } catch (final MalformedURLException e) {
	// return
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_invalid_url
	// + e.getLocalizedMessage();
	// }
	// }
	// }
	//
	//		private static final String DIALOG_SETTINGS = "ImportTestRunSessionFromURLAction"; //$NON-NLS-1$
	//
	// private final Shell fShell;
	//
	// public ImportTestRunSessionFromURLAction(final Shell shell) {
	// super(
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_import_from_url);
	// fShell = shell;
	// }
	//
	// public void run() {
	// final String title =
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionAction_title;
	// final String message =
	// JUnitMessages.TestRunnerViewPart_ImportTestRunSessionFromURLAction_url;
	//
	// final IDialogSettings dialogSettings = JUnitPlugin.getDefault()
	// .getDialogSettings();
	// String url = dialogSettings.get(PREF_LAST_URL);
	//
	// final IInputValidator validator = new URLValidator();
	//
	// final InputDialog inputDialog = new InputDialog(fShell, title,
	// message, url, validator) {
	// protected Control createDialogArea(final Composite parent) {
	// final Control dialogArea2 = super.createDialogArea(parent);
	// final Object layoutData = getText().getLayoutData();
	// if (layoutData instanceof GridData) {
	// final GridData gd = (GridData) layoutData;
	// gd.widthHint = convertWidthInCharsToPixels(150);
	// }
	// return dialogArea2;
	// }
	//
	// protected IDialogSettings getDialogBoundsSettings() {
	// IDialogSettings settings = dialogSettings
	// .getSection(DIALOG_SETTINGS);
	// if (settings == null) {
	// settings = dialogSettings
	// .addNewSection(DIALOG_SETTINGS);
	// }
	// return settings;
	// }
	//
	// protected boolean isResizable() {
	// return true;
	// }
	// };
	//
	// final int res = inputDialog.open();
	// if (res == IDialogConstants.OK_ID) {
	// url = inputDialog.getValue();
	// dialogSettings.put(PREF_LAST_URL, url);
	// importTestRunSession(url);
	// }
	// }
	// }

	// private static class ExportTestRunSessionAction extends Action {
	// private final TestRunSession fTestRunSession;
	// private final Shell fShell;
	//
	// public ExportTestRunSessionAction(final Shell shell,
	// final TestRunSession testRunSession) {
	// super(
	// JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_name);
	// fShell = shell;
	// fTestRunSession = testRunSession;
	// }
	//
	// public void run() {
	// final FileDialog exportDialog = new FileDialog(fShell, SWT.SAVE);
	// exportDialog
	// .setText(JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_title);
	// final IDialogSettings dialogSettings = JUnitPlugin.getDefault()
	// .getDialogSettings();
	// final String lastPath = dialogSettings.get(PREF_LAST_PATH);
	// if (lastPath != null) {
	// exportDialog.setFilterPath(lastPath);
	// }
	// exportDialog.setFileName(getFileName());
	//			exportDialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
	// final String path = exportDialog.open();
	// if (path == null) {
	// return;
	// }
	//
	// // TODO: MULTI: getFileNames()
	// final File file = new File(path);
	//
	// try {
	// EUnitModel.exportTestRunSession(fTestRunSession, file);
	// } catch (final CoreException e) {
	// JUnitPlugin.log(e);
	// ErrorDialog
	// .openError(
	// fShell,
	// JUnitMessages.TestRunnerViewPart_ExportTestRunSessionAction_error_title,
	// e.getStatus().getMessage(), e.getStatus());
	// }
	// }
	//
	// private String getFileName() {
	// final String testRunName = fTestRunSession.getTestRunName();
	// final long startTime = fTestRunSession.getStartTime();
	// if (startTime <= 0) {
	// return testRunName;
	// }
	//
	//			final String isoTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date(startTime)); //$NON-NLS-1$
	//			return testRunName + " " + isoTime + ".xml"; //$NON-NLS-1$ //$NON-NLS-2$
	// }
	// }

	private class TestRunSessionListener implements ITestRunSessionListener {
		public void sessionAdded(final TestRunSession testRunSession) {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (getSite().getWorkbenchWindow() == EUnitPlugin
							.getActiveWorkbenchWindow()) {
						if (fInfoMessage == null) {
							final String testRunLabel = testRunSession
									.getTestRunName();
							String msg;
							if (testRunSession.getLaunch() != null) {
								msg = MessageFormat.format("Launching {0}...",
										testRunLabel);
							} else {
								msg = testRunLabel;
							}
							registerInfoMessage(msg);
						}

						final TestRunSession deactivatedSession = setActiveTestRunSession(testRunSession);
						if (deactivatedSession != null) {
							deactivatedSession.swapOut();
						}
					}
				}
			});
		}

		public void sessionRemoved(final TestRunSession testRunSession) {
			getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (testRunSession.equals(fTestRunSession)) {
						final List<TestRunSession> testRunSessions = EUnitPlugin
								.getModel().getTestRunSessions();
						TestRunSession deactivatedSession;
						if (!testRunSessions.isEmpty()) {
							deactivatedSession = setActiveTestRunSession(testRunSessions
									.get(0));
						} else {
							deactivatedSession = setActiveTestRunSession(null);
						}
						if (deactivatedSession != null) {
							deactivatedSession.swapOut();
						}
					}
				}
			});
		}
	}

	private class TestSessionListener implements ITestSessionListener {
		public void sessionStarted() {
			fTestViewer.registerViewersRefresh();
			fShowOnErrorOnly = getShowOnErrorOnly();

			startUpdateJobs();

			fStopAction.setEnabled(true);
			fRerunLastTestAction.setEnabled(true);
		}

		public void sessionEnded(final long elapsedTime) {
			deregisterTestSessionListener(false);

			fTestViewer.registerAutoScrollTarget(null);

			final String[] keys = { elapsedTimeAsString(elapsedTime) };
			final String msg = MessageFormat.format(
					" Finished after {0} seconds", new Object[] { keys });
			registerInfoMessage(msg);

			postSyncRunnable(new Runnable() {
				public void run() {
					if (isDisposed()) {
						return;
					}
					fStopAction.setEnabled(lastLaunchIsKeptAlive());
					updateRerunFailedFirstAction();
					processChangesInUI();
					if (hasErrorsOrFailures()) {
						selectFirstFailure();
					}
					if (fDirtyListener == null) {
						fDirtyListener = new DirtyListener();
						ErlangCore.getModelManager().addElementChangedListener(
								fDirtyListener);
					}
					warnOfContentChange();
				}
			});
			stopUpdateJobs();
		}

		public void sessionStopped(final long elapsedTime) {
			deregisterTestSessionListener(false);

			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage(" Stopped");
			handleStopped();
		}

		public void sessionTerminated() {
			deregisterTestSessionListener(true);

			fTestViewer.registerAutoScrollTarget(null);

			registerInfoMessage("Terminated");
			handleStopped();
		}

		public void runningBegins() {
			if (!fShowOnErrorOnly) {
				postShowTestResultsView();
			}
		}

		public void testStarted(final TestCaseElement testCaseElement) {
			fTestViewer.registerAutoScrollTarget(testCaseElement);
			fTestViewer.registerViewerUpdate(testCaseElement);

			final String moduleName = testCaseElement.getModuleName();
			final String function = testCaseElement.getTestFunctionName();
			final String status = MessageFormat.format(" {0}:{1}", moduleName,
					function);
			registerInfoMessage(status);
		}

		public void testFailed(final TestElement testElement,
				final TestElement.Status status, final String trace,
				final String expected, final String actual) {
			if (isAutoScroll()) {
				fTestViewer.registerFailedForAutoScroll(testElement);
			}
			fTestViewer.registerViewerUpdate(testElement);

			// show the view on the first error only
			if (fShowOnErrorOnly && getErrorsPlusFailures() == 1) {
				postShowTestResultsView();
			}

			// TODO:
			// [Bug 35590] JUnit window doesn't report errors from
			// junit.extensions.TestSetup [JUnit]
			// when a failure occurs in test setup then no test is running
			// to update the views we artificially signal the end of a test run
			// if (!fTestIsRunning) {
			// fTestIsRunning= false;
			// testEnded(testCaseElement);
			// }
		}

		public void testEnded(final TestCaseElement testCaseElement) {
			fTestViewer.registerViewerUpdate(testCaseElement);
		}

		public void testReran(final TestCaseElement testCaseElement,
				final TestElement.Status status, final String trace,
				final String expectedResult, final String actualResult) {
			fTestViewer.registerViewerUpdate(testCaseElement); // TODO:
																// autoExpand?
			postSyncProcessChanges();
			showFailure(testCaseElement);
		}

		public void testAdded(final TestElement testElement) {
			fTestViewer.registerTestAdded(testElement);
		}

		public boolean acceptsSwapToDisk() {
			return false;
		}
	}

	private class UpdateUIJob extends UIJob {
		private boolean fRunning = true;

		public UpdateUIJob(final String name) {
			super(name);
			setSystem(true);
		}

		@Override
		public IStatus runInUIThread(final IProgressMonitor monitor) {
			if (!isDisposed()) {
				processChangesInUI();
			}
			schedule(REFRESH_INTERVAL);
			return Status.OK_STATUS;
		}

		public void stop() {
			fRunning = false;
		}

		@Override
		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	private class JUnitIsRunningJob extends Job {
		public JUnitIsRunningJob(final String name) {
			super(name);
			setSystem(true);
		}

		@Override
		public IStatus run(final IProgressMonitor monitor) {
			// wait until the test run terminates
			fJUnitIsRunningLock.acquire();
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(final Object family) {
			return family == TestRunnerViewPart.FAMILY_JUNIT_RUN;
		}
	}

	private class ClearAction extends Action {
		public ClearAction() {
			setText("&Clear Terminated");
			boolean enabled = false;
			final List<TestRunSession> testRunSessions = EUnitPlugin.getModel()
					.getTestRunSessions();
			for (final TestRunSession testRunSession : testRunSessions) {
				if (!testRunSession.isRunning() && !testRunSession.isStarting()) {
					enabled = true;
					break;
				}
			}
			setEnabled(enabled);
		}

		@Override
		public void run() {
			final List<TestRunSession> testRunSessions = getRunningSessions();
			final Object first = testRunSessions.isEmpty() ? null
					: testRunSessions.get(0);
			fViewHistory.setHistoryEntries(testRunSessions, first);
		}

		private List<TestRunSession> getRunningSessions() {
			final List<TestRunSession> testRunSessions = EUnitPlugin.getModel()
					.getTestRunSessions();
			for (final Iterator<TestRunSession> iter = testRunSessions
					.iterator(); iter.hasNext();) {
				final TestRunSession testRunSession = iter.next();
				if (!testRunSession.isRunning() && !testRunSession.isStarting()) {
					iter.remove();
				}
			}
			return testRunSessions;
		}
	}

	private class StopAction extends Action {
		public StopAction() {
			setText("Stop JUnit Test");
			setToolTipText("Stop JUnit Test Run");
			EUnitPlugin.setLocalImageDescriptors(this, "stop.gif"); //$NON-NLS-1$
		}

		@Override
		public void run() {
			stopTest();
			setEnabled(false);
		}
	}

	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText("Rerun Test");
			setToolTipText("Rerun Test");
			EUnitPlugin.setLocalImageDescriptors(this, "relaunch.gif"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_LAST_COMMAND);
		}

		@Override
		public void run() {
			rerunTestRun();
		}
	}

	private class RerunLastFailedFirstAction extends Action {
		public RerunLastFailedFirstAction() {
			setText("Rerun Test - Failures First");
			setToolTipText("Rerun Test - Failures First");
			EUnitPlugin.setLocalImageDescriptors(this, "relaunchf.gif"); //$NON-NLS-1$
			setEnabled(false);
			setActionDefinitionId(RERUN_FAILED_FIRST_COMMAND);
		}

		@Override
		public void run() {
			rerunTestFailedFirst();
		}
	}

	private class ToggleOrientationAction extends Action {
		private final int fActionOrientation;

		public ToggleOrientationAction(final int orientation) {
			super("", AS_RADIO_BUTTON); //$NON-NLS-1$
			if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
				setText("&Horizontal");
				setImageDescriptor(EUnitPlugin
						.getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
				setText("&Vertical");
				setImageDescriptor(EUnitPlugin
						.getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$
			} else if (orientation == TestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
				setText("&Automatic");
				setImageDescriptor(EUnitPlugin
						.getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$
			}
			fActionOrientation = orientation;
			// TODO PlatformUI
			// .getWorkbench()
			// .getHelpSystem()
			// .setHelp(
			// this,
			// IJUnitHelpContextIds.RESULTS_VIEW_TOGGLE_ORIENTATION_ACTION);
		}

		public int getOrientation() {
			return fActionOrientation;
		}

		@Override
		public void run() {
			if (isChecked()) {
				fOrientation = fActionOrientation;
				computeOrientation();
			}
		}
	}

	/**
	 * Listen for for modifications to Java elements
	 */
	private class DirtyListener implements IElementChangedListener {
		public void elementChanged(final ElementChangedEvent event) {
			processDelta(event.getDelta());
		}

		private boolean processDelta(final IErlElementDelta delta) {
			final int kind = delta.getKind();
			final int details = delta.getFlags();
			final IErlElement.Kind elementKind = delta.getElement().getKind();

			switch (elementKind) {
			// Consider containers for class files.
			case MODEL:
			case PROJECT:
				// If we did something different than changing a child we flush
				// the undo / redo stack.
				if (kind != IErlElementDelta.CHANGED
						|| details != IErlElementDelta.F_CHILDREN) {
					codeHasChanged();
					return false;
				}
				break;
			case MODULE:
				// XXX // if we have changed a primary working copy (e.g
				// created,
				// // removed, ...)
				// // then we do nothing.
				// if ((details & IErlElementDelta.F_PRIMARY_WORKING_COPY) != 0)
				// {
				// return true;
				// }
				codeHasChanged();
				return false;

				// case IJavaElement.CLASS_FILE:
				// // Don't examine children of a class file but keep on
				// examining
				// // siblings.
				// return true;
			default:
				codeHasChanged();
				return false;
			}

			final IErlElementDelta[] affectedChildren = delta
					.getChildren(IErlElementDelta.ALL);
			if (affectedChildren == null) {
				return true;
			}

			for (int i = 0; i < affectedChildren.length; i++) {
				if (!processDelta(affectedChildren[i])) {
					return false;
				}
			}
			return true;
		}
	}

	private class FailuresOnlyFilterAction extends Action {
		public FailuresOnlyFilterAction() {
			super("Show &Failures Only", AS_CHECK_BOX);
			setToolTipText("Show &Failures Only");
			setImageDescriptor(EUnitPlugin
					.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			setShowFailuresOnly(isChecked());
		}
	}

	private class ShowTimeAction extends Action {

		public ShowTimeAction() {
			super("Show Execution &Time", IAction.AS_CHECK_BOX);
		}

		@Override
		public void run() {
			setShowExecutionTime(isChecked());
		}
	}

	private class ShowTestHierarchyAction extends Action {

		public ShowTestHierarchyAction() {
			super("Show Tests in &Hierarchy ", IAction.AS_CHECK_BOX);
			setImageDescriptor(EUnitPlugin
					.getImageDescriptor("elcl16/hierarchicalLayout.gif")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			final int mode = isChecked() ? LAYOUT_HIERARCHICAL : LAYOUT_FLAT;
			setLayoutMode(mode);
		}
	}

	private class ActivateOnErrorAction extends Action {
		public ActivateOnErrorAction() {
			super("Activate on &Error/Failure Only", IAction.AS_CHECK_BOX);
			//setImageDescriptor(JUnitPlugin.getImageDescriptor("obj16/failures.gif")); //$NON-NLS-1$
			update();
		}

		public void update() {
			setChecked(getShowOnErrorOnly());
		}

		@Override
		public void run() {
			final boolean checked = isChecked();
			fShowOnErrorOnly = checked;
			new InstanceScope().getNode(EUnitPlugin.PLUGIN_ID).putBoolean(
					EUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, checked);
		}
	}

	public TestRunnerViewPart() {
		fImagesToDispose = new ArrayList<Image>();

		fStackViewIcon = createManagedImage("eview16/stackframe.gif");//$NON-NLS-1$
		fTestRunOKIcon = createManagedImage("eview16/junitsucc.gif"); //$NON-NLS-1$
		fTestRunFailIcon = createManagedImage("eview16/juniterr.gif"); //$NON-NLS-1$
		fTestRunOKDirtyIcon = createManagedImage("eview16/junitsuccq.gif"); //$NON-NLS-1$
		fTestRunFailDirtyIcon = createManagedImage("eview16/juniterrq.gif"); //$NON-NLS-1$

		fTestIcon = createManagedImage("obj16/test.gif"); //$NON-NLS-1$
		fTestOkIcon = createManagedImage("obj16/testok.gif"); //$NON-NLS-1$
		fTestErrorIcon = createManagedImage("obj16/testerr.gif"); //$NON-NLS-1$
		fTestFailIcon = createManagedImage("obj16/testfail.gif"); //$NON-NLS-1$
		fTestRunningIcon = createManagedImage("obj16/testrun.gif"); //$NON-NLS-1$
		fTestIgnoredIcon = createManagedImage("obj16/testignored.gif"); //$NON-NLS-1$

		fSuiteIcon = createManagedImage(fSuiteIconDescriptor);
		fSuiteOkIcon = createManagedImage(fSuiteOkIconDescriptor);
		fSuiteErrorIcon = createManagedImage(fSuiteErrorIconDescriptor);
		fSuiteFailIcon = createManagedImage(fSuiteFailIconDescriptor);
		fSuiteRunningIcon = createManagedImage(fSuiteRunningIconDescriptor);
	}

	private Image createManagedImage(final String path) {
		return createManagedImage(EUnitPlugin.getImageDescriptor(path));
	}

	private Image createManagedImage(final ImageDescriptor descriptor) {
		Image image = descriptor.createImage();
		if (image == null) {
			image = ImageDescriptor.getMissingImageDescriptor().createImage();
		}
		fImagesToDispose.add(image);
		return image;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento)
			throws PartInitException {
		super.init(site, memento);
		fMemento = memento;
		final IWorkbenchSiteProgressService progressService = getProgressService();
		if (progressService != null) {
			progressService
					.showBusyForFamily(TestRunnerViewPart.FAMILY_JUNIT_RUN);
		}
	}

	private IWorkbenchSiteProgressService getProgressService() {
		final Object siteService = getSite().getAdapter(
				IWorkbenchSiteProgressService.class);
		if (siteService != null) {
			return (IWorkbenchSiteProgressService) siteService;
		}
		return null;
	}

	@Override
	public void saveState(final IMemento memento) {
		if (fSashForm == null) {
			// part has not been created
			if (fMemento != null) {
				memento.putMemento(fMemento);
			}
			return;
		}

		// int activePage= fTabFolder.getSelectionIndex();
		// memento.putInteger(TAG_PAGE, activePage);
		memento.putString(TAG_SCROLL,
				fScrollLockAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		final int weigths[] = fSashForm.getWeights();
		final int ratio = weigths[0] * 1000 / (weigths[0] + weigths[1]);
		memento.putInteger(TAG_RATIO, ratio);
		memento.putInteger(TAG_ORIENTATION, fOrientation);

		memento.putString(TAG_FAILURES_ONLY,
				fFailuresOnlyFilterAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
		memento.putInteger(TAG_LAYOUT, fLayout);
		memento.putString(TAG_SHOW_TIME,
				fShowTimeAction.isChecked() ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void restoreLayoutState(final IMemento memento) {
		// Integer page= memento.getInteger(TAG_PAGE);
		// if (page != null) {
		// int p= page.intValue();
		// if (p < fTestRunTabs.size()) { // tab count can decrease if a
		// contributing plug-in is removed
		// fTabFolder.setSelection(p);
		// fActiveRunTab= (TestRunTab)fTestRunTabs.get(p);
		// }
		// }
		final Integer ratio = memento.getInteger(TAG_RATIO);
		if (ratio != null) {
			fSashForm.setWeights(new int[] { ratio.intValue(),
					1000 - ratio.intValue() });
		}
		final Integer orientation = memento.getInteger(TAG_ORIENTATION);
		if (orientation != null) {
			fOrientation = orientation.intValue();
		}
		computeOrientation();
		final String scrollLock = memento.getString(TAG_SCROLL);
		if (scrollLock != null) {
			fScrollLockAction.setChecked(scrollLock.equals("true")); //$NON-NLS-1$
			setAutoScroll(!fScrollLockAction.isChecked());
		}

		final Integer layout = memento.getInteger(TAG_LAYOUT);
		int layoutValue = LAYOUT_HIERARCHICAL;
		if (layout != null) {
			layoutValue = layout.intValue();
		}

		final String failuresOnly = memento.getString(TAG_FAILURES_ONLY);
		boolean showFailuresOnly = false;
		if (failuresOnly != null) {
			showFailuresOnly = failuresOnly.equals("true"); //$NON-NLS-1$
		}

		final String time = memento.getString(TAG_SHOW_TIME);
		boolean showTime = true;
		if (time != null) {
			showTime = time.equals("true"); //$NON-NLS-1$
		}

		setFilterAndLayout(showFailuresOnly, layoutValue);
		setShowExecutionTime(showTime);
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void stopTest() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				setContentDescription("Stopping...");
			}
			fTestRunSession.stopTestRun();
		}
	}

	private void startUpdateJobs() {
		postSyncProcessChanges();

		if (fUpdateJob != null) {
			return;
		}
		fJUnitIsRunningJob = new JUnitIsRunningJob("JUnit Starter Job");
		fJUnitIsRunningLock = Job.getJobManager().newLock();
		// acquire lock while a test run is running
		// the lock is released when the test run terminates
		// the wrapper job will wait on this lock.
		fJUnitIsRunningLock.acquire();
		getProgressService().schedule(fJUnitIsRunningJob);

		fUpdateJob = new UpdateUIJob("Update JUnit");
		fUpdateJob.schedule(REFRESH_INTERVAL);
	}

	private void stopUpdateJobs() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob = null;
		}
		if (fJUnitIsRunningJob != null && fJUnitIsRunningLock != null) {
			fJUnitIsRunningLock.release();
			fJUnitIsRunningJob = null;
		}
		postSyncProcessChanges();
	}

	private void processChangesInUI() {
		if (fSashForm.isDisposed()) {
			return;
		}

		doShowInfoMessage();
		refreshCounters();

		if (!fPartIsVisible) {
			updateViewTitleProgress();
		} else {
			updateViewIcon();
		}
		final boolean hasErrorsOrFailures = hasErrorsOrFailures();
		fNextAction.setEnabled(hasErrorsOrFailures);
		fPreviousAction.setEnabled(hasErrorsOrFailures);

		fTestViewer.processChangesInUI();
	}

	/**
	 * Stops the currently running test and shuts down the RemoteTestRunner
	 */
	public void rerunTestRun() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), "Rerun Test",
					"Terminate currently running tests?")) {
				stopTest(); // TODO: wait for termination
			}
		}

		if (fTestRunSession == null) {
			return;
		}
		final ILaunch launch = fTestRunSession.getLaunch();
		if (launch == null) {
			return;
		}
		final ILaunchConfiguration launchConfiguration = launch
				.getLaunchConfiguration();
		if (launchConfiguration == null) {
			return;
		}

		final ILaunchConfiguration configuration = prepareLaunchConfigForRelaunch(launchConfiguration);
		relaunch(configuration, launch.getLaunchMode());
	}

	private ILaunchConfiguration prepareLaunchConfigForRelaunch(
			final ILaunchConfiguration configuration) {
		try {
			final String attribute = configuration.getAttribute(
					EUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES, ""); //$NON-NLS-1$
			if (attribute.length() != 0) {
				final String configName = MessageFormat.format("Rerun {0}",
						configuration.getName());
				final ILaunchConfigurationWorkingCopy tmp = configuration
						.copy(configName);
				tmp.setAttribute(
						EUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES,
						""); //$NON-NLS-1$
				return tmp;
			}
		} catch (final CoreException e) {
			// fall through
		}
		return configuration;
	}

	public void rerunTestFailedFirst() {
		if (lastLaunchIsKeptAlive()) {
			// prompt for terminating the existing run
			if (MessageDialog.openQuestion(getSite().getShell(), "Rerun Test",
					"Terminate currently running tests?")) {
				if (fTestRunSession != null) {
					fTestRunSession.stopTestRun();
				}
			}
		}
		final ILaunch launch = fTestRunSession.getLaunch();
		if (launch != null && launch.getLaunchConfiguration() != null) {
			final ILaunchConfiguration launchConfiguration = launch
					.getLaunchConfiguration();
			if (launchConfiguration != null) {
				try {
					final String oldName = launchConfiguration.getName();
					final String oldFailuresFilename = launchConfiguration
							.getAttribute(
									EUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES,
									(String) null);
					String configName;
					if (oldFailuresFilename != null) {
						configName = oldName;
					} else {
						configName = MessageFormat.format(
								"{0} (Failed Tests first)", oldName);
					}
					final ILaunchConfigurationWorkingCopy tmp = launchConfiguration
							.copy(configName);
					tmp.setAttribute(
							EUnitLaunchConfigurationConstants.ATTR_FAILURES_NAMES,
							createFailureNamesFile());
					relaunch(tmp, launch.getLaunchMode());
					return;
				} catch (final CoreException e) {
					ErrorDialog.openError(getSite().getShell(),
							"Could not rerun test", e.getMessage(),
							e.getStatus());
				}
			}
			MessageDialog
					.openInformation(
							getSite().getShell(),
							"Rerun Test",
							"To rerun tests they must be launched under the debugger\nand \'Keep JUnit running\' must be set in the launch configuration.");
		}
	}

	private void relaunch(final ILaunchConfiguration configuration,
			final String launchMode) {
		DebugUITools.launch(configuration, launchMode);
	}

	private String createFailureNamesFile() throws CoreException {
		try {
			final File file = File.createTempFile("testFailures", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
			file.deleteOnExit();
			final TestElement[] failures = fTestRunSession
					.getAllFailedTestElements();
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$
				for (int i = 0; i < failures.length; i++) {
					final TestElement testElement = failures[i];
					bw.write(testElement.getTestName());
					bw.newLine();
				}
			} finally {
				if (bw != null) {
					bw.close();
				}
			}
			return file.getAbsolutePath();
		} catch (final IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					EUnitPlugin.PLUGIN_ID, IStatus.ERROR, "", e)); //$NON-NLS-1$
		}
	}

	public void setAutoScroll(final boolean scroll) {
		fAutoScroll = scroll;
	}

	public boolean isAutoScroll() {
		return fAutoScroll;
	}

	public void selectNextFailure() {
		fTestViewer.selectFailure(true);
	}

	public void selectPreviousFailure() {
		fTestViewer.selectFailure(false);
	}

	protected void selectFirstFailure() {
		fTestViewer.selectFirstFailure();
	}

	private boolean hasErrorsOrFailures() {
		return getErrorsPlusFailures() > 0;
	}

	private int getErrorsPlusFailures() {
		if (fTestRunSession == null) {
			return 0;
		} else {
			return fTestRunSession.getErrorCount()
					+ fTestRunSession.getFailureCount();
		}
	}

	private String elapsedTimeAsString(final long runTime) {
		return NumberFormat.getInstance().format((double) runTime / 1000);
	}

	private void handleStopped() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) {
					return;
				}
				resetViewIcon();
				fStopAction.setEnabled(false);
				updateRerunFailedFirstAction();
			}
		});
		stopUpdateJobs();
	}

	private void resetViewIcon() {
		fViewImage = fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewIcon() {
		if (fTestRunSession == null || fTestRunSession.isStopped()
				|| fTestRunSession.isRunning()
				|| fTestRunSession.getStartedCount() == 0) {
			fViewImage = fOriginalViewImage;
		} else if (hasErrorsOrFailures()) {
			fViewImage = fTestRunFailIcon;
		} else {
			fViewImage = fTestRunOKIcon;
		}
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}

	private void updateViewTitleProgress() {
		if (fTestRunSession != null) {
			if (fTestRunSession.isRunning()) {
				final Image progress = fProgressImages.getImage(
						fTestRunSession.getStartedCount(),
						fTestRunSession.getTotalCount(),
						fTestRunSession.getErrorCount(),
						fTestRunSession.getFailureCount());
				if (progress != fViewImage) {
					fViewImage = progress;
					firePropertyChange(IWorkbenchPart.PROP_TITLE);
				}
			} else {
				updateViewIcon();
			}
		} else {
			resetViewIcon();
		}
	}

	/**
	 * @param testRunSession
	 *            new active test run session
	 * @return deactivated session, or <code>null</code> iff no session got
	 *         deactivated
	 */
	private TestRunSession setActiveTestRunSession(
			final TestRunSession testRunSession) {
		/*
		 * - State: fTestRunSession fTestSessionListener Jobs
		 * fTestViewer.processChangesInUI(); - UI: fCounterPanel fProgressBar
		 * setContentDescription / fInfoMessage setTitleToolTip view icons
		 * statusLine fFailureTrace
		 * 
		 * action enablement
		 */
		if (fTestRunSession == testRunSession) {
			return null;
		}

		deregisterTestSessionListener(true);

		final TestRunSession deactivatedSession = fTestRunSession;

		fTestRunSession = testRunSession;
		fTestViewer.registerActiveSession(testRunSession);

		if (fSashForm.isDisposed()) {
			stopUpdateJobs();
			return deactivatedSession;
		}

		if (testRunSession == null) {
			setTitleToolTip(null);
			resetViewIcon();
			clearStatus();
			fFailureTrace.clear();

			registerInfoMessage(" "); //$NON-NLS-1$
			stopUpdateJobs();

			fStopAction.setEnabled(false);
			fRerunFailedFirstAction.setEnabled(false);
			fRerunLastTestAction.setEnabled(false);

		} else {
			if (fTestRunSession.isStarting() || fTestRunSession.isRunning()
					|| fTestRunSession.isKeptAlive()) {
				fTestSessionListener = new TestSessionListener();
				fTestRunSession.addTestSessionListener(fTestSessionListener);
			}
			if (!fTestRunSession.isStarting() && !fShowOnErrorOnly) {
				showTestResultsView();
			}

			setTitleToolTip();

			clearStatus();
			fFailureTrace.clear();
			registerInfoMessage(fTestRunSession.getTestRunName());

			updateRerunFailedFirstAction();
			fRerunLastTestAction
					.setEnabled(fTestRunSession.getLaunch() != null);

			if (fTestRunSession.isRunning()) {
				startUpdateJobs();

				fStopAction.setEnabled(true);

			} else /* old or fresh session: don't want jobs at this stage */{
				stopUpdateJobs();

				fStopAction.setEnabled(fTestRunSession.isKeptAlive());
				fTestViewer.expandFirstLevel();
			}
		}
		return deactivatedSession;
	}

	private void deregisterTestSessionListener(final boolean force) {
		if (fTestRunSession != null && fTestSessionListener != null
				&& (force || !fTestRunSession.isKeptAlive())) {
			fTestRunSession.removeTestSessionListener(fTestSessionListener);
			fTestSessionListener = null;
		}
	}

	private void updateRerunFailedFirstAction() {
		final boolean state = hasErrorsOrFailures()
				&& fTestRunSession.getLaunch() != null;
		fRerunFailedFirstAction.setEnabled(state);
	}

	// /**
	// * @return the display name of the current test run sessions kind, or
	// * <code>null</code>
	// */
	// public String getTestKindDisplayName() {
	// final ITestKind kind = fTestRunSession.getTestRunnerKind();
	// if (!kind.isNull()) {
	// return kind.getDisplayName();
	// }
	// return null;
	// }

	private void setTitleToolTip() {
		// final String testKindDisplayStr = getTestKindDisplayName();

		final String testRunLabel = fTestRunSession.getTestRunName();
		// if (testKindDisplayStr != null) {
		// setTitleToolTip(MessageFormat.format(
		// JUnitMessages.TestRunnerViewPart_titleToolTip,
		// new String[] { testRunLabel, testKindDisplayStr }));
		// } else {
		setTitleToolTip(testRunLabel);
		// }
	}

	@Override
	public synchronized void dispose() {
		fIsDisposed = true;
		if (fTestRunSessionListener != null) {
			EUnitPlugin.getModel().removeTestRunSessionListener(
					fTestRunSessionListener);
		}

		final IHandlerService handlerService = (IHandlerService) getSite()
				.getWorkbenchWindow().getService(IHandlerService.class);
		handlerService.deactivateHandler(fRerunLastActivation);
		handlerService.deactivateHandler(fRerunFailedFirstActivation);
		setActiveTestRunSession(null);

		if (fProgressImages != null) {
			fProgressImages.dispose();
		}
		getViewSite().getPage().removePartListener(fPartListener);

		disposeImages();
		if (fClipboard != null) {
			fClipboard.dispose();
		}
		if (fViewMenuListener != null) {
			getViewSite().getActionBars().getMenuManager()
					.removeMenuListener(fViewMenuListener);
		}
		if (fDirtyListener != null) {
			ErlangCore.getModelManager().removeElementChangedListener(
					fDirtyListener);
			fDirtyListener = null;
		}
	}

	private void disposeImages() {
		for (int i = 0; i < fImagesToDispose.size(); i++) {
			fImagesToDispose.get(i).dispose();
		}
	}

	private void postSyncRunnable(final Runnable r) {
		if (!isDisposed()) {
			getDisplay().syncExec(r);
		}
	}

	private void refreshCounters() {
		// TODO: Inefficient. Either
		// - keep a boolean fHasTestRun and update only on changes, or
		// - improve components to only redraw on changes (once!).

		int startedCount;
		int ignoredCount;
		int totalCount;
		int errorCount;
		int failureCount;
		boolean hasErrorsOrFailures;
		boolean stopped;

		if (fTestRunSession != null) {
			startedCount = fTestRunSession.getStartedCount();
			ignoredCount = fTestRunSession.getIgnoredCount();
			totalCount = fTestRunSession.getTotalCount();
			errorCount = fTestRunSession.getErrorCount();
			failureCount = fTestRunSession.getFailureCount();
			hasErrorsOrFailures = errorCount + failureCount > 0;
			stopped = fTestRunSession.isStopped();
		} else {
			startedCount = 0;
			ignoredCount = 0;
			totalCount = 0;
			errorCount = 0;
			failureCount = 0;
			hasErrorsOrFailures = false;
			stopped = false;
		}

		fCounterPanel.setTotal(totalCount);
		fCounterPanel.setRunValue(startedCount, ignoredCount);
		fCounterPanel.setErrorValue(errorCount);
		fCounterPanel.setFailureValue(failureCount);

		int ticksDone;
		if (startedCount == 0) {
			ticksDone = 0;
		} else if (startedCount == totalCount && !fTestRunSession.isRunning()) {
			ticksDone = totalCount;
		} else {
			ticksDone = startedCount - 1;
		}

		fProgressBar.reset(hasErrorsOrFailures, stopped, ticksDone, totalCount);
	}

	protected void postShowTestResultsView() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) {
					return;
				}
				showTestResultsView();
			}
		});
	}

	public void showTestResultsView() {
		final IWorkbenchWindow window = getSite().getWorkbenchWindow();
		final IWorkbenchPage page = window.getActivePage();
		TestRunnerViewPart testRunner = null;

		if (page != null) {
			try { // show the result view
				testRunner = (TestRunnerViewPart) page
						.findView(TestRunnerViewPart.NAME);
				if (testRunner == null) {
					final IWorkbenchPart activePart = page.getActivePart();
					testRunner = (TestRunnerViewPart) page.showView(
							TestRunnerViewPart.NAME, null,
							IWorkbenchPage.VIEW_VISIBLE);
					// restore focus
					page.activate(activePart);
				} else {
					page.bringToTop(testRunner);
				}
			} catch (final PartInitException pie) {
				ErlLogger.error(pie);
			}
		}
	}

	protected void doShowInfoMessage() {
		if (fInfoMessage != null) {
			setContentDescription(fInfoMessage);
			fInfoMessage = null;
		}
	}

	protected void registerInfoMessage(final String message) {
		fInfoMessage = message;
	}

	private SashForm createSashForm(final Composite parent) {
		fSashForm = new SashForm(parent, SWT.VERTICAL);

		final ViewForm top = new ViewForm(fSashForm, SWT.NONE);

		final Composite empty = new Composite(top, SWT.NONE);
		empty.setLayout(new Layout() {
			@Override
			protected Point computeSize(final Composite composite,
					final int wHint, final int hHint, final boolean flushCache) {
				return new Point(1, 1); // (0, 0) does not work with
										// super-intelligent ViewForm
			}

			@Override
			protected void layout(final Composite composite,
					final boolean flushCache) {
			}
		});
		top.setTopLeft(empty); // makes ViewForm draw the horizontal separator
								// line ...
		fTestViewer = new TestViewer(top, fClipboard, this);
		top.setContent(fTestViewer.getTestViewerControl());

		final ViewForm bottom = new ViewForm(fSashForm, SWT.NONE);

		final CLabel label = new CLabel(bottom, SWT.NONE);
		label.setText("Failure Trace");
		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);
		final ToolBar failureToolBar = new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureTrace = new FailureTrace(bottom, fClipboard, this,
				failureToolBar);
		bottom.setContent(fFailureTrace.getComposite());

		fSashForm.setWeights(new int[] { 50, 50 });
		return fSashForm;
	}

	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}

	@Override
	public void setFocus() {
		if (fTestViewer != null) {
			fTestViewer.getTestViewerControl().setFocus();
		}
	}

	@Override
	public void createPartControl(final Composite parent) {
		fParent = parent;
		addResizeListener(parent);
		fClipboard = new Clipboard(parent.getDisplay());

		final GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		parent.setLayout(gridLayout);

		fViewHistory = new RunnerViewHistory();
		configureToolBar();

		fCounterComposite = createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		final SashForm sashForm = createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

		final IActionBars actionBars = getViewSite().getActionBars();
		fCopyAction = new EUnitCopyAction(fFailureTrace, fClipboard);
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				fCopyAction);
		initPageSwitcher();
		addDropAdapter(parent);

		fOriginalViewImage = getTitleImage();
		fProgressImages = new ProgressImages();
		// TODO PlatformUI.getWorkbench().getHelpSystem()
		// .setHelp(parent, IJUnitHelpContextIds.RESULTS_VIEW);

		getViewSite().getPage().addPartListener(fPartListener);

		setFilterAndLayout(false, LAYOUT_HIERARCHICAL);
		setShowExecutionTime(true);
		if (fMemento != null) {
			restoreLayoutState(fMemento);
		}
		fMemento = null;

		fTestRunSessionListener = new TestRunSessionListener();
		EUnitPlugin.getModel().addTestRunSessionListener(
				fTestRunSessionListener);

		// always show youngest test run in view. simulate "sessionAdded" event
		// to do that
		final List<TestRunSession> testRunSessions = EUnitPlugin.getModel()
				.getTestRunSessions();
		if (!testRunSessions.isEmpty()) {
			fTestRunSessionListener.sessionAdded(testRunSessions.get(0));
		}
	}

	private void addDropAdapter(final Composite parent) {
		final DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE
				| DND.DROP_COPY | DND.DROP_LINK | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { TextTransfer.getInstance() });
		class DropAdapter extends DropTargetAdapter {
			@Override
			public void dragEnter(final DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void dragOver(final DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void dragOperationChanged(final DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
				event.feedback = DND.FEEDBACK_NONE;
			}

			@Override
			public void drop(final DropTargetEvent event) {
				if (TextTransfer.getInstance().isSupportedType(
						event.currentDataType)) {
					// final String url = (String) event.data;
					// XXX importTestRunSession(url);
				}
			}
		}
		dropTarget.addDropListener(new DropAdapter());
	}

	private void initPageSwitcher() {
		new PageSwitcher(this) {
			@Override
			public Object[] getPages() {
				return fViewHistory.getHistoryEntries().toArray();
			}

			@Override
			public String getName(final Object page) {
				return fViewHistory.getText(page);
			}

			@Override
			public ImageDescriptor getImageDescriptor(final Object page) {
				return fViewHistory.getImageDescriptor(page);
			}

			@Override
			public void activatePage(final Object page) {
				fViewHistory.setActiveEntry(page);
			}

			@Override
			public int getCurrentPageIndex() {
				return fViewHistory.getHistoryEntries().indexOf(
						fViewHistory.getCurrentEntry());
			}
		};
	}

	private void addResizeListener(final Composite parent) {
		parent.addControlListener(new ControlListener() {
			public void controlMoved(final ControlEvent e) {
			}

			public void controlResized(final ControlEvent e) {
				computeOrientation();
			}
		});
	}

	void computeOrientation() {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation = fOrientation;
			setOrientation(fCurrentOrientation);
		} else {
			final Point size = fParent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y) {
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				} else {
					setOrientation(VIEW_ORIENTATION_VERTICAL);
				}
			}
		}
	}

	private void configureToolBar() {
		final IActionBars actionBars = getViewSite().getActionBars();
		final IToolBarManager toolBar = actionBars.getToolBarManager();
		final IMenuManager viewMenu = actionBars.getMenuManager();

		fNextAction = new ShowNextFailureAction(this);
		fNextAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.NEXT.getId(),
				fNextAction);

		fPreviousAction = new ShowPreviousFailureAction(this);
		fPreviousAction.setEnabled(false);
		actionBars.setGlobalActionHandler(ActionFactory.PREVIOUS.getId(),
				fPreviousAction);

		fStopAction = new StopAction();
		fStopAction.setEnabled(false);

		fRerunLastTestAction = new RerunLastAction();
		final IHandlerService handlerService = (IHandlerService) getSite()
				.getWorkbenchWindow().getService(IHandlerService.class);
		IHandler handler = new AbstractHandler() {
			public Object execute(final ExecutionEvent event)
					throws ExecutionException {
				fRerunLastTestAction.run();
				return null;
			}

			@Override
			public boolean isEnabled() {
				return fRerunLastTestAction.isEnabled();
			}
		};
		fRerunLastActivation = handlerService.activateHandler(
				RERUN_LAST_COMMAND, handler);

		fRerunFailedFirstAction = new RerunLastFailedFirstAction();
		handler = new AbstractHandler() {
			public Object execute(final ExecutionEvent event)
					throws ExecutionException {
				fRerunFailedFirstAction.run();
				return null;
			}

			@Override
			public boolean isEnabled() {
				return fRerunFailedFirstAction.isEnabled();
			}
		};
		fRerunFailedFirstActivation = handlerService.activateHandler(
				RERUN_FAILED_FIRST_COMMAND, handler);

		fFailuresOnlyFilterAction = new FailuresOnlyFilterAction();

		fScrollLockAction = new ScrollLockAction(this);
		fScrollLockAction.setChecked(!fAutoScroll);

		fToggleOrientationActions = new ToggleOrientationAction[] {
				new ToggleOrientationAction(VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(VIEW_ORIENTATION_AUTOMATIC) };

		fShowTestHierarchyAction = new ShowTestHierarchyAction();
		fShowTimeAction = new ShowTimeAction();

		toolBar.add(fNextAction);
		toolBar.add(fPreviousAction);
		toolBar.add(fFailuresOnlyFilterAction);
		toolBar.add(fScrollLockAction);
		toolBar.add(new Separator());
		toolBar.add(fRerunLastTestAction);
		toolBar.add(fRerunFailedFirstAction);
		toolBar.add(fStopAction);
		toolBar.add(fViewHistory.createHistoryDropDownAction());

		viewMenu.add(fShowTestHierarchyAction);
		viewMenu.add(fShowTimeAction);
		viewMenu.add(new Separator());

		final MenuManager layoutSubMenu = new MenuManager("&Layout");
		for (int i = 0; i < fToggleOrientationActions.length; ++i) {
			layoutSubMenu.add(fToggleOrientationActions[i]);
		}
		viewMenu.add(layoutSubMenu);
		viewMenu.add(new Separator());

		viewMenu.add(fFailuresOnlyFilterAction);

		fActivateOnErrorAction = new ActivateOnErrorAction();
		viewMenu.add(fActivateOnErrorAction);
		fViewMenuListener = new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fActivateOnErrorAction.update();
			}
		};

		viewMenu.addMenuListener(fViewMenuListener);

		actionBars.updateActionBars();
	}

	private IStatusLineManager getStatusLine() {
		// we want to show messages globally hence we
		// have to go through the active part
		final IViewSite site = getViewSite();
		final IWorkbenchPage page = site.getPage();
		final IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof IViewPart) {
			final IViewPart activeViewPart = (IViewPart) activePart;
			final IViewSite activeViewSite = activeViewPart.getViewSite();
			return activeViewSite.getActionBars().getStatusLineManager();
		}

		if (activePart instanceof IEditorPart) {
			final IEditorPart activeEditorPart = (IEditorPart) activePart;
			final IEditorActionBarContributor contributor = activeEditorPart
					.getEditorSite().getActionBarContributor();
			if (contributor instanceof EditorActionBarContributor) {
				return ((EditorActionBarContributor) contributor)
						.getActionBars().getStatusLineManager();
			}
		}
		// no active part
		return getViewSite().getActionBars().getStatusLineManager();
	}

	protected Composite createProgressCountPanel(final Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		setCounterColumns(layout);

		fCounterPanel = new CounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		fProgressBar = new EUnitProgressBar(composite);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}

	public void handleTestSelected(final TestElement test) {
		showFailure(test);
		fCopyAction.handleTestSelected(test);
	}

	private void showFailure(final TestElement test) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					fFailureTrace.showFailure(test);
				}
			}
		});
	}

	/**
	 * @return the erlang project, or <code>null</code>
	 */
	public IErlProject getLaunchedProject() {
		return fTestRunSession == null ? null : fTestRunSession
				.getLaunchedProject();
	}

	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}

	private Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}

	/*
	 * @see IWorkbenchPart#getTitleImage()
	 */
	@Override
	public Image getTitleImage() {
		if (fOriginalViewImage == null) {
			fOriginalViewImage = super.getTitleImage();
		}

		if (fViewImage == null) {
			return super.getTitleImage();
		}
		return fViewImage;
	}

	void codeHasChanged() {
		if (fDirtyListener != null) {
			ErlangCore.getModelManager().removeElementChangedListener(
					fDirtyListener);
			fDirtyListener = null;
		}
		if (fViewImage == fTestRunOKIcon) {
			fViewImage = fTestRunOKDirtyIcon;
		} else if (fViewImage == fTestRunFailIcon) {
			fViewImage = fTestRunFailDirtyIcon;
		}

		final Runnable r = new Runnable() {
			public void run() {
				if (isDisposed()) {
					return;
				}
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		};
		if (!isDisposed()) {
			getDisplay().asyncExec(r);
		}
	}

	public void rerunTest(final String testId, final String className,
			final String testName, final String launchMode) {
		final boolean buildBeforeLaunch = Platform
				.getPreferencesService()
				.getBoolean(IDebugUIConstants.PLUGIN_ID,
						IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, false, null);
		try {
			final boolean couldLaunch = fTestRunSession.rerunTest(testId,
					className, testName, launchMode, buildBeforeLaunch);
			if (!couldLaunch) {
				MessageDialog
						.openInformation(
								getSite().getShell(),
								"Rerun Test",
								"To rerun tests they must be launched under the debugger\nand \'Keep JUnit running\' must be set in the launch configuration.");
			} else if (fTestRunSession.isKeptAlive()) {
				final TestCaseElement testCaseElement = (TestCaseElement) fTestRunSession
						.getTestElement(testId);
				testCaseElement.setStatus(TestElement.Status.RUNNING, null,
						null, null);
				fTestViewer.registerViewerUpdate(testCaseElement);
				postSyncProcessChanges();
			}

		} catch (final CoreException e) {
			ErrorDialog.openError(getSite().getShell(), "Could not rerun test",
					e.getMessage(), e.getStatus());
		}
	}

	private void postSyncProcessChanges() {
		postSyncRunnable(new Runnable() {
			public void run() {
				processChangesInUI();
			}
		});
	}

	public void warnOfContentChange() {
		final IWorkbenchSiteProgressService service = getProgressService();
		if (service != null) {
			service.warnOfContentChange();
		}
	}

	public boolean lastLaunchIsKeptAlive() {
		return fTestRunSession != null && fTestRunSession.isKeptAlive();
	}

	private void setOrientation(final int orientation) {
		if (fSashForm == null || fSashForm.isDisposed()) {
			return;
		}
		final boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
		fSashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		for (int i = 0; i < fToggleOrientationActions.length; ++i) {
			fToggleOrientationActions[i]
					.setChecked(fOrientation == fToggleOrientationActions[i]
							.getOrientation());
		}
		fCurrentOrientation = orientation;
		final GridLayout layout = (GridLayout) fCounterComposite.getLayout();
		setCounterColumns(layout);
		fParent.layout();
	}

	private void setCounterColumns(final GridLayout layout) {
		if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL) {
			layout.numColumns = 2;
		} else {
			layout.numColumns = 1;
		}
	}

	static boolean getShowOnErrorOnly() {
		return Platform.getPreferencesService().getBoolean(
				EUnitPlugin.PLUGIN_ID,
				EUnitPreferencesConstants.SHOW_ON_ERROR_ONLY, false, null);
	}

	// static void importTestRunSession(final String url) {
	// try {
	// PlatformUI.getWorkbench().getProgressService()
	// .busyCursorWhile(new IRunnableWithProgress() {
	// public void run(final IProgressMonitor monitor)
	// throws InvocationTargetException,
	// InterruptedException {
	// EUnitModel.importTestRunSession(url, monitor);
	// }
	// });
	// } catch (final InterruptedException e) {
	// // cancelled
	// } catch (final InvocationTargetException e) {
	// final CoreException ce = (CoreException) e.getCause();
	// StatusManager.getManager().handle(ce.getStatus(),
	// StatusManager.SHOW | StatusManager.LOG);
	// }
	// }

	public FailureTrace getFailureTrace() {
		return fFailureTrace;
	}

	void setShowFailuresOnly(final boolean failuresOnly) {
		setFilterAndLayout(failuresOnly, fLayout);
	}

	private void setLayoutMode(final int mode) {
		setFilterAndLayout(fFailuresOnlyFilterAction.isChecked(), mode);
	}

	private void setFilterAndLayout(final boolean failuresOnly,
			final int layoutMode) {
		fShowTestHierarchyAction.setChecked(layoutMode == LAYOUT_HIERARCHICAL);
		fLayout = layoutMode;
		fFailuresOnlyFilterAction.setChecked(failuresOnly);
		fTestViewer.setShowFailuresOnly(failuresOnly, layoutMode);
	}

	private void setShowExecutionTime(final boolean showTime) {
		fTestViewer.setShowTime(showTime);
		fShowTimeAction.setChecked(showTime);

	}

	TestElement[] getAllFailures() {
		return fTestRunSession.getAllFailedTestElements();
	}
}
