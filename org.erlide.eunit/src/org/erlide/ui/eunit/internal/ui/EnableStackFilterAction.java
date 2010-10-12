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
import org.erlide.eunit.EUnitPlugin;
import org.erlide.eunit.EUnitPreferencesConstants;

/**
 * Action to enable/disable stack trace filtering.
 */
public class EnableStackFilterAction extends Action {

	private final FailureTrace fView;

	public EnableStackFilterAction(final FailureTrace view) {
		super("Filter");
		setDescription("Filter the stack trace");
		setToolTipText("Filter Stack Trace");

		setDisabledImageDescriptor(EUnitPlugin
				.getImageDescriptor("dlcl16/cfilter.gif")); //$NON-NLS-1$
		setHoverImageDescriptor(EUnitPlugin
				.getImageDescriptor("elcl16/cfilter.gif")); //$NON-NLS-1$
		setImageDescriptor(EUnitPlugin.getImageDescriptor("elcl16/cfilter.gif")); //$NON-NLS-1$
		// TODO PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
		// IJUnitHelpContextIds.ENABLEFILTER_ACTION);

		fView = view;
		setChecked(EUnitPreferencesConstants.getFilterStack());
	}

	/*
	 * @see Action#actionPerformed
	 */
	@Override
	public void run() {
		EUnitPreferencesConstants.setFilterStack(isChecked());
		fView.refresh();
	}
}
