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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.erlide.core.erlang.ErlModelException;
import org.erlide.core.erlang.IErlElement;
import org.erlide.core.erlang.IErlFunction;
import org.erlide.core.erlang.IErlModule;
import org.erlide.ui.ErlideUIPlugin;
import org.erlide.ui.editors.erl.ErlangEditor;
import org.erlide.ui.util.ErlModelUtils;

import com.google.common.collect.Lists;

/**
 * Shows a dialog with test methods that refer to the selection.
 */
public class GotoReferencedTestAction implements IWorkbenchWindowActionDelegate {
	ISelection fSelection;
	IWorkbenchWindow fWorkbench;

	private void run(final IStructuredSelection selection) {
		final List<IErlElement> elements = getSelectedElements(selection);
		if (elements.isEmpty()) {
			MessageDialog
					.openInformation(getShell(), "Search Referring Tests",
							"Select a module or a function to open tests that refer to them.");
			return;
		}
		try {
			run(elements);
		} catch (final CoreException e) {
			ErrorDialog.openError(getShell(), "Search Referring Tests",
					"Test cannot be found", e.getStatus());
		}
	}

	private void runWithEditorSelection() {
		final ErlangEditor editor = getActiveEditor();
		if (editor == null) {
			return;
		}
		final int offset = editor.getViewer().getSelectedRange().x;
		final IErlElement element = editor.getElementAt(offset, true);
		if (element == null || element.getKind() != IErlElement.Kind.FUNCTION) {
			MessageDialog.openInformation(getShell(), "Search Referring Tests",
					"Selection is not inside a function.");
			return;
		}
	}

	private void run(final List<IErlElement> elements)
			throws PartInitException, ErlModelException {
		// TODO JC search for referring tests (no dialog)
		openElement(elements.get(0));
	}

	private void openElement(final IErlElement result)
			throws ErlModelException, PartInitException {
		ErlModelUtils.openInEditor(result);
	}

	private List<IErlElement> getSelectedElements(
			final IStructuredSelection selection) {
		final List<IErlElement> result = Lists.newArrayList();
		for (final Object object : selection.toList()) {
			if (object instanceof IErlModule || object instanceof IErlFunction) {
				result.add((IErlElement) object);
			}
		}
		return result;
	}

	public void run(final IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			run((IStructuredSelection) fSelection);
		} else {
			runWithEditorSelection();
		}

	}

	public void selectionChanged(final IAction action,
			final ISelection selection) {
		fSelection = selection;
		action.setEnabled(getActiveEditor() != null);
	}

	private Shell getShell() {
		if (fWorkbench != null) {
			return fWorkbench.getShell();
		}
		return ErlideUIPlugin.getActiveWorkbenchShell();
	}

	public void dispose() {
	}

	public void init(final IWorkbenchWindow window) {
		fWorkbench = window;
	}

	private ErlangEditor getActiveEditor() {
		final IEditorPart editor = fWorkbench.getActivePage().getActiveEditor();
		if (editor instanceof ErlangEditor) {
			return (ErlangEditor) editor;
		}
		return null;
	}
}
