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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.erlide.eunit.EUnitPlugin;
import org.erlide.ui.eunit.internal.model.TestElement;

/**
 * Action to enable/disable stack trace filtering.
 */
public class CompareResultsAction extends Action {

	private final FailureTrace fView;
	private CompareResultDialog fOpenDialog;

	public CompareResultsAction(final FailureTrace view) {
		super("Compare Result");
		setDescription("Compare the actual and expected test result");
		setToolTipText("Compare Actual With Expected Test Result");

		setDisabledImageDescriptor(EUnitPlugin
				.getImageDescriptor("dlcl16/compare.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(EUnitPlugin
				.getImageDescriptor("elcl16/compare.gif")); //$NON-NLS-1$
		setImageDescriptor(EUnitPlugin.getImageDescriptor("elcl16/compare.gif")); //$NON-NLS-1$
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IJUnitHelpContextIds.ENABLEFILTER_ACTION);
		fView = view;
	}

	/*
	 * @see Action#actionPerformed
	 */
	@Override
	public void run() {
		final TestElement failedTest = fView.getFailedTest();
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
			fOpenDialog.getShell().setActive();

		} else {
			fOpenDialog = new CompareResultDialog(fView.getShell(), failedTest);
			fOpenDialog.create();
			fOpenDialog.getShell().addDisposeListener(new DisposeListener() {
				public void widgetDisposed(final DisposeEvent e) {
					fOpenDialog = null;
				}
			});
			fOpenDialog.setBlockOnOpen(false);
			fOpenDialog.open();
		}
	}

	public void updateOpenDialog(final TestElement failedTest) {
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
		}
	}
}
