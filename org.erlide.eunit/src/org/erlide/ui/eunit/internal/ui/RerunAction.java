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

import org.eclipse.jface.action.Action;

/**
 * Requests to rerun a test.
 */
public class RerunAction extends Action {
	private final String fTestId;
	private final String fModuleName;
	private final String fTestName;
	private final TestRunnerViewPart fTestRunner;
	private final String fLaunchMode;

	public RerunAction(final String actionName,
			final TestRunnerViewPart runner, final String testId,
			final String moduleName, final String testName,
			final String launchMode) {
		super(actionName);
		// TODO PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IJUnitHelpContextIds.RERUN_ACTION);
		fTestRunner = runner;
		fTestId = testId;
		fModuleName = moduleName;
		fTestName = testName;
		fLaunchMode = launchMode;
	}

	/*
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		fTestRunner.rerunTest(fTestId, fModuleName, fTestName, fLaunchMode);
	}
}
