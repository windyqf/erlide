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
package org.erlide.ui.eunit.internal.ui;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlModule;
import org.erlide.core.erlang.IErlProject;
import org.erlide.ui.util.ErlModelUtils;

/**
 * Abstract Action for opening a Java editor.
 */
public abstract class OpenEditorAction extends Action {
	protected String fModuleName;
	protected TestRunnerViewPart fTestRunner;

	// private final boolean fActivate;

	protected OpenEditorAction(final TestRunnerViewPart testRunner,
			final String testClassName) {
		this(testRunner, testClassName, true);
	}

	public OpenEditorAction(final TestRunnerViewPart testRunner,
			final String moduleName, final boolean activate) {
		super("&Go to File");
		fModuleName = moduleName;
		fTestRunner = testRunner;
		// fActivate = activate;
	}

	/*
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		final IErlModule module = findModule(getLaunchedProject(), fModuleName);
		if (module == null) {
			MessageDialog.openError(getShell(), "Cannot Open Editor",
					"Test class not found in selected project");
			return;
		}
		try {
			final ITextEditor editor = (ITextEditor) ErlModelUtils
					.openElement(module);
			reveal(editor);
		} catch (final PartInitException e) {
			e.printStackTrace();
		} catch (final ErlModelException e) {
			e.printStackTrace();
		}
	}

	protected Shell getShell() {
		return fTestRunner.getSite().getShell();
	}

	/**
	 * @return the erlang project, or <code>null</code>
	 */
	protected IErlProject getLaunchedProject() {
		return fTestRunner.getLaunchedProject();
	}

	protected String getClassName() {
		return fModuleName;
	}

	protected abstract void reveal(ITextEditor editor);

	protected final IErlModule findModule(final IErlProject project,
			final String name) {
		try {
			IErlModule module = project.getModule(name);
			if (module == null) {
				module = ErlangCore.getModel().findModule(name);
			}
			return module;
		} catch (final ErlModelException e) {
			return null;
		}
	}

}
