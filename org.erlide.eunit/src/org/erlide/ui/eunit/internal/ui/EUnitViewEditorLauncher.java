/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - bug fixes
 *     Brock Janiczak <brockj@tpg.com.au> - [JUnit] Add context menu action to import junit test results from package explorer - https://bugs.eclipse.org/bugs/show_bug.cgi?id=213786
 *******************************************************************************/
package org.erlide.ui.eunit.internal.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorLauncher;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.internal.util.ExceptionHandler;
import org.erlide.ui.eunit.internal.model.EUnitModel;

public class EUnitViewEditorLauncher implements IEditorLauncher {

	public void open(final IPath file) {
		try {
			EUnitPlugin.getActivePage().showView(TestRunnerViewPart.VIEW_ID);
			EUnitModel.importTestRunSession(file.toFile());
		} catch (final CoreException e) {
			ExceptionHandler.handle(e, "Import Test Run",
					"An error occurred while opening a test run file.");
		}
	}

}
